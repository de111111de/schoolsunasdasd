package ru.schoolsun.quiz;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.schoolsun.common.NotFoundException;
import ru.schoolsun.lesson.Lesson;
import ru.schoolsun.lesson.LessonService;
import ru.schoolsun.user.User;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class QuizService {

    private final QuizRepository quizzes;
    private final QuestionRepository questions;
    private final OptionRepository options;
    private final QuizAttemptRepository attempts;
    private final AttemptAnswerRepository answers;
    private final LessonService lessons;

    @Value("${app.pass-percent}")
    private int passPercent;

    public QuizService(QuizRepository quizzes, QuestionRepository questions, OptionRepository options,
                       QuizAttemptRepository attempts, AttemptAnswerRepository answers, LessonService lessons) {
        this.quizzes = quizzes;
        this.questions = questions;
        this.options = options;
        this.attempts = attempts;
        this.answers = answers;
        this.lessons = lessons;
    }

    // ---------- формат JSON от конструктора тестов ----------

    public record QuizPayload(String title, Integer timeLimitSec, Integer maxAttempts,
                              Boolean showAnswers, List<Q> questions) {}

    public record Q(String type, String text, Integer points, String answerText, List<O> options) {}

    public record O(String text, Boolean correct, String pairKey) {}

    // ---------- конструктор (учитель) ----------

    public Quiz byId(Long id) {
        return quizzes.findById(id).orElseThrow(() -> new NotFoundException("Тест не найден"));
    }

    public Quiz byLesson(Long lessonId) {
        return quizzes.findByLessonId(lessonId).orElse(null);
    }

    public List<Question> questionsOf(Long quizId) {
        return questions.findByQuizIdOrderByPositionAsc(quizId);
    }

    public List<Option> optionsOf(Long questionId) {
        return options.findByQuestionId(questionId);
    }

    @Transactional
    public Quiz saveFromPayload(Lesson lesson, QuizPayload payload) {
        Quiz quiz = quizzes.findByLessonId(lesson.getId()).orElseGet(() -> {
            Quiz q = new Quiz();
            q.setLesson(lesson);
            return q;
        });
        quiz.setTitle(payload.title() == null || payload.title().isBlank() ? "Тест" : payload.title().trim());
        quiz.setTimeLimitSec(payload.timeLimitSec() == null || payload.timeLimitSec() <= 0 ? null : payload.timeLimitSec());
        quiz.setMaxAttempts(payload.maxAttempts() == null || payload.maxAttempts() < 1 ? 3 : payload.maxAttempts());
        quiz.setShowAnswers(payload.showAnswers() == null || payload.showAnswers());
        quizzes.save(quiz);

        for (Question old : questions.findByQuizIdOrderByPositionAsc(quiz.getId())) {
            options.deleteAll(options.findByQuestionId(old.getId()));
            questions.delete(old);
        }
        questions.flush();

        int pos = 0;
        if (payload.questions() != null) {
            for (Q qd : payload.questions()) {
                if (qd.text() == null || qd.text().isBlank()) continue;
                QuestionType type;
                try {
                    type = QuestionType.valueOf(qd.type());
                } catch (Exception e) {
                    continue;
                }
                Question q = new Question();
                q.setQuiz(quiz);
                q.setType(type);
                q.setText(qd.text().trim());
                q.setAnswerText(qd.answerText());
                q.setPoints(qd.points() == null || qd.points() < 1 ? 1 : qd.points());
                q.setPosition(pos++);
                Question saved = questions.save(q);

                if (type == QuestionType.SINGLE || type == QuestionType.MULTI || type == QuestionType.MATCH) {
                    if (qd.options() != null) {
                        for (O od : qd.options()) {
                            if (od.text() == null || od.text().isBlank()) continue;
                            Option o = new Option();
                            o.setQuestion(saved);
                            o.setText(od.text().trim());
                            o.setCorrect(Boolean.TRUE.equals(od.correct()));
                            o.setPairKey(od.pairKey());
                            options.save(o);
                        }
                    }
                }
            }
        }
        return quiz;
    }

    // ---------- прохождение (ученик) ----------

    public int attemptsUsed(Long quizId, Long studentId) {
        return attempts.findByQuizIdAndStudentIdOrderByAttemptNoDesc(quizId, studentId).size();
    }

    public Integer bestScore(Long quizId, Long studentId) {
        return attempts.findByQuizIdAndStudentIdOrderByAttemptNoDesc(quizId, studentId).stream()
                .filter(a -> a.getStatus() == AttemptStatus.FINISHED && a.getScore() != null)
                .mapToInt(QuizAttempt::getScore)
                .max().orElse(0);
    }

    public QuizAttempt attemptById(Long id) {
        return attempts.findById(id).orElseThrow(() -> new NotFoundException("Попытка не найдена"));
    }

    @Transactional
    public QuizAttempt startOrResume(User student, Quiz quiz) {
        QuizAttempt last = attempts.findFirstByQuizIdAndStudentIdOrderByAttemptNoDesc(quiz.getId(), student.getId())
                .orElse(null);
        if (last != null && last.getStatus() == AttemptStatus.IN_PROGRESS) {
            if (quiz.getTimeLimitSec() != null
                    && Duration.between(last.getStartedAt(), LocalDateTime.now()).getSeconds() > quiz.getTimeLimitSec()) {
                // время вышло — завершаем попытку с тем, что успели ответить
                return gradeInto(last, Map.of());
            }
            return last;
        }
        if (attemptsUsed(quiz.getId(), student.getId()) >= quiz.getMaxAttempts()) {
            throw new IllegalStateException("Попытки закончились");
        }
        QuizAttempt a = new QuizAttempt();
        a.setQuiz(quiz);
        a.setStudent(student);
        a.setAttemptNo(attemptsUsed(quiz.getId(), student.getId()) + 1);
        a.setStartedAt(LocalDateTime.now());
        a.setStatus(AttemptStatus.IN_PROGRESS);
        return attempts.save(a);
    }

    @Transactional
    public QuizAttempt submit(User student, Quiz quiz, Map<String, String> params) {
        QuizAttempt attempt = startOrResume(student, quiz);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("Тест уже сдан");
        }
        return gradeInto(attempt, params);
    }

    private QuizAttempt gradeInto(QuizAttempt attempt, Map<String, String> params) {
        Quiz quiz = attempt.getQuiz();
        List<Question> qs = questionsOf(quiz.getId());
        int score = 0;
        boolean hasOpen = false;

        for (Question q : qs) {
            AttemptAnswer ans = new AttemptAnswer();
            ans.setAttempt(attempt);
            ans.setQuestion(q);
            String raw = params.get("q_" + q.getId());
            ans.setPayload(raw);
            int earned = 0;
            boolean correct = false;

            switch (q.getType()) {
                case SINGLE -> {
                    Option chosen = raw == null ? null : findOption(q.getId(), Long.parseLong(raw));
                    correct = chosen != null && chosen.isCorrect();
                    earned = correct ? q.getPoints() : 0;
                }
                case MULTI -> {
                    Set<Long> chosen = parseIds(raw);
                    Set<Long> right = correctOptionIds(q);
                    correct = !chosen.isEmpty() && chosen.equals(right);
                    earned = correct ? q.getPoints() : 0;
                }
                case MATCH -> {
                    correct = true;
                    for (Option o : optionsOf(q.getId())) {
                        String picked = params.get("q_" + q.getId() + "__" + o.getId());
                        if (picked == null || o.getPairKey() == null || !picked.equals(o.getPairKey())) {
                            correct = false;
                            break;
                        }
                    }
                    earned = correct ? q.getPoints() : 0;
                }
                case NUMBER, TEXT -> {
                    correct = raw != null && normalize(raw).equals(normalize(q.getAnswerText()));
                    earned = correct ? q.getPoints() : 0;
                }
                case OPEN -> hasOpen = true;
            }

            ans.setCorrect(correct);
            ans.setEarnedPoints(earned);
            score += earned;
            answers.save(ans);
        }

        attempt.setScore(score);
        attempt.setFinishedAt(LocalDateTime.now());
        attempt.setStatus(hasOpen ? AttemptStatus.PENDING_REVIEW : AttemptStatus.FINISHED);
        attempts.save(attempt);

        if (!hasOpen) {
            lessons.completeIfEligible(quiz.getLesson(), attempt.getStudent());
        }
        return attempt;
    }

    public int totalPoints(Quiz quiz) {
        return questionsOf(quiz.getId()).stream().mapToInt(Question::getPoints).sum();
    }

    public int percent(QuizAttempt attempt) {
        int total = totalPoints(attempt.getQuiz());
        return total == 0 ? 0 : (int) Math.round(attempt.getScore() * 100.0 / total);
    }

    public boolean passed(QuizAttempt attempt) {
        return percent(attempt) >= passPercent;
    }

    public long remainingSeconds(QuizAttempt attempt) {
        Integer limit = attempt.getQuiz().getTimeLimitSec();
        if (limit == null) return -1;
        long elapsed = Duration.between(attempt.getStartedAt(), LocalDateTime.now()).getSeconds();
        return Math.max(0, limit - elapsed);
    }

    // ---------- результаты ----------

    public record ResultRow(Question question, List<Option> options, AttemptAnswer answer,
                            boolean showCorrect, Boolean answerCorrect) {}

    public List<ResultRow> resultRows(QuizAttempt attempt) {
        boolean show = attempt.getQuiz().isShowAnswers() || passed(attempt);
        List<ResultRow> rows = new ArrayList<>();
        for (Question q : questionsOf(attempt.getQuiz().getId())) {
            AttemptAnswer a = answers.findByAttemptIdAndQuestionId(attempt.getId(), q.getId()).orElse(null);
            Boolean correct = a == null ? Boolean.FALSE : (q.getType() == QuestionType.OPEN ? null : a.isCorrect());
            rows.add(new ResultRow(q, optionsOf(q.getId()), a, show, correct));
        }
        return rows;
    }

    // ---------- проверка открытых ответов (учитель) ----------

    public List<QuizAttempt> pendingReview(User teacher) {
        return attempts.findByQuizLessonCourseTeacherIdAndStatus(teacher.getId(), AttemptStatus.PENDING_REVIEW);
    }

    public List<AttemptAnswer> answersOf(Long attemptId) {
        return answers.findByAttemptId(attemptId);
    }

    @Transactional
    public void applyReview(Long attemptId, Map<Long, Integer> openScores, User teacher) {
        QuizAttempt attempt = attemptById(attemptId);
        if (!attempt.getQuiz().getLesson().getCourse().getTeacher().getId().equals(teacher.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Это не твой курс");
        }
        if (attempt.getStatus() != AttemptStatus.PENDING_REVIEW) {
            return;
        }
        int auto = 0;
        for (AttemptAnswer a : answersOf(attemptId)) {
            if (a.getQuestion().getType() == QuestionType.OPEN) {
                int pts = openScores.getOrDefault(a.getQuestion().getId(), 0);
                pts = Math.max(0, Math.min(pts, a.getQuestion().getPoints()));
                a.setEarnedPoints(pts);
                a.setCorrect(pts > 0);
                answers.save(a);
            } else {
                auto += a.getEarnedPoints();
            }
        }
        attempt.setStatus(AttemptStatus.FINISHED);
        attempt.setFinishedAt(LocalDateTime.now());
        attempt.setScore(auto + openScores.values().stream().mapToInt(Integer::intValue).sum());
        attempts.save(attempt);
        lessons.completeIfEligible(attempt.getQuiz().getLesson(), attempt.getStudent());
    }

    // ---------- утилиты ----------

    private Option findOption(Long questionId, Long optionId) {
        return options.findByQuestionId(questionId).stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst().orElse(null);
    }

    private Set<Long> correctOptionIds(Question q) {
        Set<Long> ids = new HashSet<>();
        for (Option o : optionsOf(q.getId())) {
            if (o.isCorrect()) ids.add(o.getId());
        }
        return ids;
    }

    private Set<Long> parseIds(String raw) {
        Set<Long> ids = new HashSet<>();
        if (raw == null || raw.isBlank()) return ids;
        Arrays.stream(raw.split(",")).forEach(s -> {
            try {
                ids.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        });
        return ids;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}

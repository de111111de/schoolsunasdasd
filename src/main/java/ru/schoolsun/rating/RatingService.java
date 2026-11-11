package ru.schoolsun.rating;

import org.springframework.stereotype.Service;

import ru.schoolsun.certificate.CertificateRepository;
import ru.schoolsun.course.Course;
import ru.schoolsun.course.Enrollment;
import ru.schoolsun.homework.Homework;
import ru.schoolsun.homework.HomeworkRepository;
import ru.schoolsun.homework.HomeworkSubmissionRepository;
import ru.schoolsun.homework.SubmissionStatus;
import ru.schoolsun.lesson.Lesson;
import ru.schoolsun.lesson.LessonProgressRepository;
import ru.schoolsun.quiz.AttemptAnswerRepository;
import ru.schoolsun.quiz.AttemptStatus;
import ru.schoolsun.quiz.Question;
import ru.schoolsun.quiz.Quiz;
import ru.schoolsun.quiz.QuizAttemptRepository;
import ru.schoolsun.user.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RatingService {

    private final QuizAttemptRepository attempts;
    private final AttemptAnswerRepository answers;
    private final HomeworkSubmissionRepository submissions;
    private final HomeworkRepository homeworks;
    private final LessonProgressRepository progress;
    private final ru.schoolsun.course.EnrollmentRepository enrollments;
    private final ru.schoolsun.lesson.LessonRepository lessons;
    private final ru.schoolsun.quiz.QuizRepository quizzes;
    private final ru.schoolsun.quiz.QuestionRepository questions;
    private final CertificateRepository certificates;

    public RatingService(QuizAttemptRepository attempts, AttemptAnswerRepository answers,
                         HomeworkSubmissionRepository submissions, HomeworkRepository homeworks,
                         LessonProgressRepository progress,
                         ru.schoolsun.course.EnrollmentRepository enrollments,
                         ru.schoolsun.lesson.LessonRepository lessons,
                         ru.schoolsun.quiz.QuizRepository quizzes,
                         ru.schoolsun.quiz.QuestionRepository questions,
                         CertificateRepository certificates) {
        this.attempts = attempts;
        this.answers = answers;
        this.submissions = submissions;
        this.homeworks = homeworks;
        this.progress = progress;
        this.enrollments = enrollments;
        this.lessons = lessons;
        this.quizzes = quizzes;
        this.questions = questions;
        this.certificates = certificates;
    }

    /** Баллы ученика в курсе: лучшие попытки тестов + принятые домашки. */
    public int pointsInCourse(Long studentId, Long courseId) {
        int points = 0;
        for (Lesson lesson : lessons.findByCourseIdOrderByPositionAsc(courseId)) {
            Quiz quiz = quizzes.findByLessonId(lesson.getId()).orElse(null);
            if (quiz != null) {
                points += attempts.findByQuizIdAndStudentIdOrderByAttemptNoDesc(quiz.getId(), studentId).stream()
                        .filter(a -> a.getStatus() == AttemptStatus.FINISHED && a.getScore() != null)
                        .mapToInt(ru.schoolsun.quiz.QuizAttempt::getScore)
                        .max().orElse(0);
            }
            Homework hw = homeworks.findByLessonId(lesson.getId()).orElse(null);
            if (hw != null) {
                points += submissions.findByHomeworkIdAndStudentId(hw.getId(), studentId)
                        .map(s -> s.getStatus() == SubmissionStatus.ACCEPTED && s.getScore() != null ? s.getScore() : 0)
                        .orElse(0);
            }
        }
        return points;
    }

    public record LeaderboardRow(int rank, Long studentId, String name, int points, long completedLessons) {}

    public List<LeaderboardRow> leaderboard(Course course) {
        List<LeaderboardRow> rows = new ArrayList<>();
        for (Enrollment e : enrollments.findByCourseIdOrderByCreatedAtAsc(course.getId())) {
            User s = e.getStudent();
            long done = lessons.findByCourseIdOrderByPositionAsc(course.getId()).stream()
                    .filter(Lesson::isPublished)
                    .filter(l -> progress.findByLessonIdAndStudentId(l.getId(), s.getId()).isPresent())
                    .count();
            rows.add(new LeaderboardRow(0, s.getId(), s.getFullName(),
                    pointsInCourse(s.getId(), course.getId()), done));
        }
        rows.sort(Comparator.comparingInt(LeaderboardRow::points).reversed()
                .thenComparing(LeaderboardRow::completedLessons, Comparator.reverseOrder())
                .thenComparing(LeaderboardRow::name));
        List<LeaderboardRow> ranked = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            LeaderboardRow r = rows.get(i);
            ranked.add(new LeaderboardRow(i + 1, r.studentId(), r.name(), r.points(), r.completedLessons()));
        }
        return ranked;
    }

    public int totalPoints(Long studentId) {
        return enrollments.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .mapToInt(e -> pointsInCourse(studentId, e.getCourse().getId()))
                .sum();
    }

    // ---------- панель учителя ----------

    public record StudentRow(Long studentId, String name, String email, int points,
                             long completed, long lessonsTotal, Double avgQuizPercent) {}

    public List<StudentRow> courseRows(Course course) {
        long total = lessons.countByCourseIdAndPublishedTrue(course.getId());
        List<StudentRow> rows = new ArrayList<>();
        for (Enrollment e : enrollments.findByCourseIdOrderByCreatedAtAsc(course.getId())) {
            User s = e.getStudent();
            long done = lessons.findByCourseIdOrderByPositionAsc(course.getId()).stream()
                    .filter(Lesson::isPublished)
                    .filter(l -> progress.findByLessonIdAndStudentId(l.getId(), s.getId()).isPresent())
                    .count();
            rows.add(new StudentRow(s.getId(), s.getFullName(), s.getEmail(),
                    pointsInCourse(s.getId(), course.getId()), done, total, avgQuizPercent(course, s.getId())));
        }
        rows.sort(Comparator.comparingInt(StudentRow::points).reversed());
        return rows;
    }

    private Double avgQuizPercent(Course course, Long studentId) {
        List<Integer> percents = new ArrayList<>();
        for (Lesson lesson : lessons.findByCourseIdOrderByPositionAsc(course.getId())) {
            Quiz quiz = quizzes.findByLessonId(lesson.getId()).orElse(null);
            if (quiz == null) continue;
            int total = questions.findByQuizIdOrderByPositionAsc(quiz.getId()).stream()
                    .mapToInt(Question::getPoints).sum();
            if (total == 0) continue;
            attempts.findByQuizIdAndStudentIdOrderByAttemptNoDesc(quiz.getId(), studentId).stream()
                    .filter(a -> a.getStatus() == AttemptStatus.FINISHED && a.getScore() != null)
                    .mapToInt(ru.schoolsun.quiz.QuizAttempt::getScore)
                    .max()
                    .ifPresent(best -> percents.add((int) Math.round(best * 100.0 / total)));
        }
        return percents.isEmpty() ? null
                : percents.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    public record LessonStat(Long lessonId, String title, Double avgQuizPercent,
                             long attemptsCount, long submissionsCount) {}

    public record QuestionStat(String lessonTitle, String questionText, double correctRate, long answersCount) {}

    public record Analytics(List<LessonStat> lessons, List<QuestionStat> hardest, long studentsCount) {}

    public Analytics analytics(Course course) {
        List<LessonStat> lessonStats = new ArrayList<>();
        List<QuestionStat> questionStats = new ArrayList<>();

        for (Lesson lesson : lessons.findByCourseIdOrderByPositionAsc(course.getId())) {
            Double avgQuiz = null;
            long attemptsCount = 0;
            Quiz quiz = quizzes.findByLessonId(lesson.getId()).orElse(null);
            if (quiz != null) {
                List<ru.schoolsun.quiz.QuizAttempt> at = attempts.findByQuizId(quiz.getId()).stream()
                        .filter(a -> a.getStatus() == AttemptStatus.FINISHED && a.getScore() != null)
                        .toList();
                attemptsCount = at.size();
                int total = questions.findByQuizIdOrderByPositionAsc(quiz.getId()).stream()
                        .mapToInt(Question::getPoints).sum();
                if (total > 0 && !at.isEmpty()) {
                    avgQuiz = at.stream().mapToInt(ru.schoolsun.quiz.QuizAttempt::getScore).average().orElse(0) * 100.0 / total;
                }
                for (Question q : questions.findByQuizIdOrderByPositionAsc(quiz.getId())) {
                    List<ru.schoolsun.quiz.AttemptAnswer> as = answers.findByAttemptIdIn(
                            at.stream().map(ru.schoolsun.quiz.QuizAttempt::getId).toList()).stream()
                            .filter(a -> a.getQuestion().getId().equals(q.getId()))
                            .toList();
                    if (!as.isEmpty()) {
                        long correctCount = as.stream().filter(ru.schoolsun.quiz.AttemptAnswer::isCorrect).count();
                        questionStats.add(new QuestionStat(lesson.getTitle(), q.getText(),
                                correctCount * 100.0 / as.size(), as.size()));
                    }
                }
            }
            long subs = submissions.findByHomeworkLessonCourseIdOrderBySubmittedAtDesc(course.getId()).stream()
                    .filter(s -> s.getHomework().getLesson().getId().equals(lesson.getId()))
                    .count();
            lessonStats.add(new LessonStat(lesson.getId(), lesson.getTitle(), avgQuiz, attemptsCount, subs));
        }

        questionStats.sort(Comparator.comparingDouble(QuestionStat::correctRate));
        return new Analytics(lessonStats,
                questionStats.stream().limit(5).toList(),
                enrollments.countByCourseId(course.getId()));
    }
}

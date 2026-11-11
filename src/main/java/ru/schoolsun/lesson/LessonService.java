package ru.schoolsun.lesson;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ru.schoolsun.certificate.CertificateService;
import ru.schoolsun.common.NotFoundException;
import ru.schoolsun.course.Course;
import ru.schoolsun.homework.Homework;
import ru.schoolsun.homework.HomeworkRepository;
import ru.schoolsun.homework.SubmissionStatus;
import ru.schoolsun.quiz.AttemptStatus;
import ru.schoolsun.quiz.Question;
import ru.schoolsun.quiz.QuestionRepository;
import ru.schoolsun.quiz.Quiz;
import ru.schoolsun.quiz.QuizAttemptRepository;
import ru.schoolsun.quiz.QuizRepository;
import ru.schoolsun.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LessonService {

    private final LessonRepository lessons;
    private final LessonAttachmentRepository attachments;
    private final LessonProgressRepository progress;
    private final QuizRepository quizzes;
    private final QuestionRepository questions;
    private final QuizAttemptRepository attempts;
    private final HomeworkRepository homeworks;
    private final ru.schoolsun.homework.HomeworkSubmissionRepository submissions;
    private final FileStorageService files;
    private final CertificateService certificates;

    @Value("${app.pass-percent}")
    private int passPercent;

    private final Parser mdParser = Parser.builder().build();
    private final HtmlRenderer mdRenderer = HtmlRenderer.builder().build();

    /** Вырезаем опасные теги/атрибуты из отрендеренного markdown (защита от XSS). */
    private static final Pattern UNSAFE_TAG = Pattern.compile(
            "<\\s*(script|style|iframe|object|embed|form)\\b[^>]*>.*?<\\s*/\\s*\\1\\s*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EVENT_ATTR = Pattern.compile(
            "\\son\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_URL = Pattern.compile(
            "(href|src)\\s*=\\s*([\"']\\s*javascript:[^\"']*[\"'])", Pattern.CASE_INSENSITIVE);

    public LessonService(LessonRepository lessons,
                         LessonAttachmentRepository attachments,
                         LessonProgressRepository progress,
                         QuizRepository quizzes,
                         QuestionRepository questions,
                         QuizAttemptRepository attempts,
                         HomeworkRepository homeworks,
                         ru.schoolsun.homework.HomeworkSubmissionRepository submissions,
                         FileStorageService files,
                         CertificateService certificates) {
        this.lessons = lessons;
        this.attachments = attachments;
        this.progress = progress;
        this.quizzes = quizzes;
        this.questions = questions;
        this.attempts = attempts;
        this.homeworks = homeworks;
        this.submissions = submissions;
        this.files = files;
        this.certificates = certificates;
    }

    public List<Lesson> lessonsOf(Long courseId) {
        return lessons.findByCourseIdOrderByPositionAsc(courseId);
    }

    public Lesson byId(Long id) {
        return lessons.findById(id).orElseThrow(() -> new NotFoundException("Урок не найден"));
    }

    public Quiz quizOf(Long lessonId) {
        return quizzes.findByLessonId(lessonId).orElse(null);
    }

    public Homework homeworkOf(Long lessonId) {
        return homeworks.findByLessonId(lessonId).orElse(null);
    }

    @Transactional
    public Lesson create(Course course, LessonForm form) {
        Lesson l = new Lesson();
        l.setCourse(course);
        apply(l, form);
        int maxPos = lessonsOf(course.getId()).stream().mapToInt(Lesson::getPosition).max().orElse(0);
        l.setPosition(maxPos + 1);
        l.setCreatedAt(LocalDateTime.now());
        return lessons.save(l);
    }

    @Transactional
    public void update(Lesson lesson, LessonForm form) {
        apply(lesson, form);
        lessons.save(lesson);
    }

    private void apply(Lesson l, LessonForm form) {
        l.setTitle(form.getTitle().trim());
        l.setTheoryMd(form.getTheoryMd());
        l.setVideoUrl(form.getVideoUrl());
        l.setPublished(form.isPublished());
    }

    public String renderMarkdown(String md) {
        if (md == null || md.isBlank()) {
            return "";
        }
        String html = mdRenderer.render(mdParser.parse(md));
        html = UNSAFE_TAG.matcher(html).replaceAll("");
        html = EVENT_ATTR.matcher(html).replaceAll("");
        html = JS_URL.matcher(html).replaceAll("$1=\"#\"");
        return html;
    }

    /** Ссылку на youtube превращаем в embed-ссылку для плеера. */
    public String videoEmbed(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        if (url.contains("youtube.com/watch?v=")) {
            String id = url.substring(url.indexOf("watch?v=") + 8);
            int amp = id.indexOf('&');
            if (amp > 0) id = id.substring(0, amp);
            return "https://www.youtube.com/embed/" + id;
        }
        if (url.contains("youtu.be/")) {
            String id = url.substring(url.indexOf("youtu.be/") + 9);
            int q = id.indexOf('?');
            if (q > 0) id = id.substring(0, q);
            return "https://www.youtube.com/embed/" + id;
        }
        return null;
    }

    public List<LessonAttachment> attachmentsOf(Long lessonId) {
        return attachments.findByLessonId(lessonId);
    }

    @Transactional
    public void addAttachment(Lesson lesson, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        LessonAttachment a = new LessonAttachment();
        a.setLesson(lesson);
        a.setStoredPath(files.store(file));
        a.setOriginalName(file.getOriginalFilename());
        a.setMimeType(file.getContentType());
        a.setSizeBytes(file.getSize());
        attachments.save(a);
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        LessonAttachment a = attachments.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Вложение не найдено"));
        files.delete(a.getStoredPath());
        attachments.delete(a);
    }

    // ---------- прогресс ----------

    public Set<Long> completedLessonIds(Long studentId) {
        return progress.findByStudentId(studentId).stream()
                .map(p -> p.getLesson().getId())
                .collect(Collectors.toSet());
    }

    /** Процент прохождения курса по опубликованным урокам. */
    public int progressPercent(Long studentId, Long courseId) {
        long total = lessons.countByCourseIdAndPublishedTrue(courseId);
        if (total == 0) return 0;
        long done = lessonsOf(courseId).stream()
                .filter(Lesson::isPublished)
                .filter(l -> progress.findByLessonIdAndStudentId(l.getId(), studentId).isPresent())
                .count();
        return (int) Math.round(done * 100.0 / total);
    }

    /** Последовательный режим: предыдущие опубликованные уроки должны быть пройдены. */
    public boolean canOpen(Lesson lesson, Course course, Set<Long> completed) {
        if (!course.isSequentialMode()) return true;
        return lessonsOf(course.getId()).stream()
                .filter(l -> l.isPublished() && l.getPosition() < lesson.getPosition())
                .allMatch(l -> completed.contains(l.getId()));
    }

    public boolean hasPassedQuiz(Long quizId, Long studentId) {
        int total = questions.findByQuizIdOrderByPositionAsc(quizId).stream()
                .mapToInt(Question::getPoints).sum();
        if (total == 0) return false;
        return attempts.findByQuizId(quizId).stream()
                .filter(a -> a.getStudent().getId().equals(studentId))
                .filter(a -> a.getStatus() == AttemptStatus.FINISHED)
                .filter(a -> a.getScore() != null)
                .anyMatch(a -> a.getScore() * 100 >= passPercent * total);
    }

    public boolean hasAcceptedHomework(Long homeworkId, Long studentId) {
        return submissions.findByHomeworkIdAndStudentId(homeworkId, studentId)
                .map(s -> s.getStatus() == SubmissionStatus.ACCEPTED)
                .orElse(false);
    }

    /** Автоматическое и ручное завершение урока: проверяет все условия. */
    @Transactional
    public boolean completeIfEligible(Lesson lesson, User student) {
        if (progress.findByLessonIdAndStudentId(lesson.getId(), student.getId()).isPresent()) {
            return false;
        }
        Quiz quiz = quizOf(lesson.getId());
        if (quiz != null && !hasPassedQuiz(quiz.getId(), student.getId())) {
            return false;
        }
        Homework hw = homeworkOf(lesson.getId());
        if (hw != null && !hasAcceptedHomework(hw.getId(), student.getId())) {
            return false;
        }
        return saveProgress(lesson, student);
    }

    /** Ручное «отметить пройденным» доступно только для уроков без теста и дз. */
    @Transactional
    public boolean completeManually(Lesson lesson, User student) {
        if (progress.findByLessonIdAndStudentId(lesson.getId(), student.getId()).isPresent()) {
            return false;
        }
        if (quizOf(lesson.getId()) != null || homeworkOf(lesson.getId()) != null) {
            return false;
        }
        return saveProgress(lesson, student);
    }

    private boolean saveProgress(Lesson lesson, User student) {
        LessonProgress p = new LessonProgress();
        p.setLesson(lesson);
        p.setStudent(student);
        p.setCompletedAt(LocalDateTime.now());
        progress.save(p);
        // весь курс пройден — выдать сертификат
        boolean allDone = lessonsOf(lesson.getCourse().getId()).stream()
                .filter(Lesson::isPublished)
                .allMatch(l -> progress.findByLessonIdAndStudentId(l.getId(), student.getId()).isPresent());
        if (allDone) {
            certificates.issueIfNeeded(student, lesson.getCourse());
        }
        return true;
    }
}

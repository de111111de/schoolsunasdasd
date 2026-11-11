package ru.schoolsun.teacher;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ru.schoolsun.common.NotFoundException;
import ru.schoolsun.course.Course;
import ru.schoolsun.course.CourseForm;
import ru.schoolsun.course.CourseService;
import ru.schoolsun.homework.Homework;
import ru.schoolsun.homework.HomeworkForm;
import ru.schoolsun.homework.HomeworkService;
import ru.schoolsun.homework.SubmissionStatus;
import ru.schoolsun.lesson.Lesson;
import ru.schoolsun.lesson.LessonAttachment;
import ru.schoolsun.lesson.LessonAttachmentRepository;
import ru.schoolsun.lesson.LessonForm;
import ru.schoolsun.lesson.LessonService;
import ru.schoolsun.quiz.Option;
import ru.schoolsun.quiz.Question;
import ru.schoolsun.quiz.Quiz;
import ru.schoolsun.quiz.QuizAttempt;
import ru.schoolsun.quiz.QuizService;
import ru.schoolsun.rating.RatingService;
import ru.schoolsun.user.User;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    private final CourseService courses;
    private final LessonService lessonService;
    private final QuizService quizService;
    private final HomeworkService homeworkService;
    private final RatingService rating;
    private final ObjectMapper objectMapper;
    private final LessonAttachmentRepository attachmentRepo;

    public TeacherController(CourseService courses, LessonService lessonService, QuizService quizService,
                             HomeworkService homeworkService, RatingService rating, ObjectMapper objectMapper,
                             LessonAttachmentRepository attachmentRepo) {
        this.courses = courses;
        this.lessonService = lessonService;
        this.quizService = quizService;
        this.homeworkService = homeworkService;
        this.rating = rating;
        this.objectMapper = objectMapper;
        this.attachmentRepo = attachmentRepo;
    }

    private Course ownedCourse(Long id, User me) {
        Course c = courses.byId(id);
        if (!courses.canManage(c, me)) {
            throw new AccessDeniedException("Это не твой курс");
        }
        return c;
    }

    private Lesson ownedLesson(Long lessonId, User me) {
        Lesson l = lessonService.byId(lessonId);
        if (!courses.canManage(l.getCourse(), me)) {
            throw new AccessDeniedException("Это не твой курс");
        }
        return l;
    }

    // ---------- дашборд ----------

    public record CourseCard(Course course, long students, long lessons, long newHomeworks) {}

    @GetMapping
    public String dashboard(Model model, @ModelAttribute("me") User me) {
        List<CourseCard> cards = new ArrayList<>();
        for (Course c : courses.byTeacher(me)) {
            long newHw = homeworkService.submissionsOfCourse(c.getId()).stream()
                    .filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED).count();
            cards.add(new CourseCard(c, courses.studentCount(c.getId()),
                    lessonService.lessonsOf(c.getId()).size(), newHw));
        }
        long reviewCount = quizService.pendingReview(me).size();
        long gradeCount = homeworkService.submissionsOfTeacher(me.getId()).stream()
                .filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED).count();
        model.addAttribute("courses", cards);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("gradeCount", gradeCount);
        return "teacher/home";
    }

    // ---------- курс ----------

    @GetMapping("/courses/new")
    public String newCourse(Model model) {
        model.addAttribute("form", new CourseForm());
        model.addAttribute("isNew", true);
        return "teacher/course-form";
    }

    @PostMapping("/courses")
    public String createCourse(CourseForm form, @ModelAttribute("me") User me, RedirectAttributes ra) {
        if (form.getTitle() == null || form.getTitle().isBlank()) {
            ra.addFlashAttribute("toastError", "Название обязательно");
            return "redirect:/teacher/courses/new";
        }
        Course c = courses.create(form, me);
        ra.addFlashAttribute("toastSuccess", "Курс создан — добавь первый урок");
        return "redirect:/teacher/courses/" + c.getId();
    }

    @GetMapping("/courses/{id}")
    public String manageCourse(@PathVariable Long id, Model model, @ModelAttribute("me") User me) {
        Course c = ownedCourse(id, me);
        CourseForm form = new CourseForm();
        form.setTitle(c.getTitle());
        form.setDescription(c.getDescription());
        form.setSubject(c.getSubject().name());
        form.setGrade(c.getGrade());
        form.setAccent(c.getAccent());
        form.setSequentialMode(c.isSequentialMode());
        form.setPublished(c.isPublished());
        model.addAttribute("course", c);
        model.addAttribute("form", form);
        model.addAttribute("lessons", lessonService.lessonsOf(c.getId()));
        model.addAttribute("students", rating.courseRows(c));
        return "teacher/course";
    }

    @PostMapping("/courses/{id}")
    public String updateCourse(@PathVariable Long id, CourseForm form,
                               @ModelAttribute("me") User me, RedirectAttributes ra) {
        Course c = ownedCourse(id, me);
        courses.update(c, form);
        ra.addFlashAttribute("toastSuccess", "Курс сохранён");
        return "redirect:/teacher/courses/" + c.getId();
    }

    @PostMapping("/courses/{id}/delete")
    public String deleteCourse(@PathVariable Long id, @ModelAttribute("me") User me, RedirectAttributes ra) {
        Course c = ownedCourse(id, me);
        courses.delete(c);
        ra.addFlashAttribute("toastSuccess", "Курс удалён");
        return "redirect:/teacher";
    }

    @PostMapping("/courses/{id}/invite")
    public String regenerateInvite(@PathVariable Long id, @ModelAttribute("me") User me, RedirectAttributes ra) {
        Course c = ownedCourse(id, me);
        courses.regenerateInviteCode(c);
        ra.addFlashAttribute("toastSuccess", "Новая ссылка-приглашение готова — старая больше не работает");
        return "redirect:/teacher/courses/" + c.getId();
    }

    @GetMapping("/courses/{id}/export.csv")
    public void exportCsv(@PathVariable Long id, @ModelAttribute("me") User me,
                          HttpServletResponse response) throws Exception {
        Course c = ownedCourse(id, me);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + c.getSlug() + "-grades.csv");
        Writer w = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        w.write('\uFEFF');
        w.write("Ученик;Email;Баллы;Пройдено уроков;Средний тест, %\n");
        for (RatingService.StudentRow r : rating.courseRows(c)) {
            String avg = r.avgQuizPercent() == null ? "—"
                    : String.format(Locale.ROOT, "%.0f", r.avgQuizPercent());
            w.write(r.name() + ";" + r.email() + ";" + r.points() + ";"
                    + r.completed() + "/" + r.lessonsTotal() + ";" + avg + "\n");
        }
        w.flush();
    }

    // ---------- уроки ----------

    @GetMapping("/courses/{id}/lessons/new")
    public String newLesson(@PathVariable Long id, Model model, @ModelAttribute("me") User me) {
        ownedCourse(id, me);
        model.addAttribute("course", courses.byId(id));
        model.addAttribute("form", new LessonForm());
        model.addAttribute("isNew", true);
        return "teacher/lesson-form";
    }

    @PostMapping("/courses/{id}/lessons")
    public String createLesson(@PathVariable Long id, LessonForm form,
                               @ModelAttribute("me") User me, RedirectAttributes ra) {
        Course c = ownedCourse(id, me);
        if (form.getTitle() == null || form.getTitle().isBlank()) {
            ra.addFlashAttribute("toastError", "Название урока обязательно");
            return "redirect:/teacher/courses/" + id + "/lessons/new";
        }
        Lesson l = lessonService.create(c, form);
        ra.addFlashAttribute("toastSuccess", "Урок создан");
        return "redirect:/teacher/lessons/" + l.getId();
    }

    @GetMapping("/lessons/{id}")
    public String editLesson(@PathVariable Long id, Model model, @ModelAttribute("me") User me) {
        Lesson l = ownedLesson(id, me);
        LessonForm form = new LessonForm();
        form.setTitle(l.getTitle());
        form.setTheoryMd(l.getTheoryMd());
        form.setVideoUrl(l.getVideoUrl());
        form.setPublished(l.isPublished());
        model.addAttribute("course", l.getCourse());
        model.addAttribute("lesson", l);
        model.addAttribute("form", form);
        model.addAttribute("attachments", lessonService.attachmentsOf(l.getId()));
        model.addAttribute("quiz", lessonService.quizOf(l.getId()));
        model.addAttribute("homework", lessonService.homeworkOf(l.getId()));
        model.addAttribute("isNew", false);
        return "teacher/lesson-form";
    }

    @PostMapping("/lessons/{id}")
    public String updateLesson(@PathVariable Long id, LessonForm form,
                               @RequestParam(required = false) MultipartFile file,
                               @ModelAttribute("me") User me, RedirectAttributes ra) {
        Lesson l = ownedLesson(id, me);
        lessonService.update(l, form);
        try {
            lessonService.addAttachment(l, file);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("toastError", e.getMessage());
            return "redirect:/teacher/lessons/" + l.getId();
        }
        ra.addFlashAttribute("toastSuccess", "Урок сохранён");
        return "redirect:/teacher/lessons/" + l.getId();
    }

    @PostMapping("/attachments/{id}/delete")
    public String deleteAttachment(@PathVariable Long id, @ModelAttribute("me") User me, RedirectAttributes ra) {
        LessonAttachment a = attachmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Вложение не найдено"));
        ownedLesson(a.getLesson().getId(), me);
        lessonService.deleteAttachment(id);
        ra.addFlashAttribute("toastSuccess", "Вложение удалено");
        return "redirect:/teacher/lessons/" + a.getLesson().getId();
    }

    // ---------- конструктор тестов ----------

    @GetMapping("/lessons/{id}/quiz")
    public String quizBuilder(@PathVariable Long id, Model model, @ModelAttribute("me") User me) throws Exception {
        Lesson l = ownedLesson(id, me);
        model.addAttribute("lesson", l);
        Quiz quiz = quizService.byLesson(l.getId());
        if (quiz != null) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", quiz.getTitle());
            payload.put("timeLimitSec", quiz.getTimeLimitSec());
            payload.put("maxAttempts", quiz.getMaxAttempts());
            payload.put("showAnswers", quiz.isShowAnswers());
            List<Map<String, Object>> qs = new ArrayList<>();
            for (Question q : quizService.questionsOf(quiz.getId())) {
                Map<String, Object> qm = new LinkedHashMap<>();
                qm.put("type", q.getType().name());
                qm.put("text", q.getText());
                qm.put("points", q.getPoints());
                qm.put("answerText", q.getAnswerText());
                List<Map<String, Object>> os = new ArrayList<>();
                for (Option o : quizService.optionsOf(q.getId())) {
                    os.add(Map.of("text", o.getText(), "correct", o.isCorrect(),
                            "pairKey", o.getPairKey() == null ? "" : o.getPairKey()));
                }
                qm.put("options", os);
                qs.add(qm);
            }
            payload.put("questions", qs);
            model.addAttribute("quizJson", objectMapper.writeValueAsString(payload));
        }
        return "teacher/quiz-builder";
    }

    @PostMapping("/lessons/{id}/quiz")
    @ResponseBody
    public Map<String, Object> saveQuiz(@PathVariable Long id,
                                        @RequestBody QuizService.QuizPayload payload,
                                        @ModelAttribute("me") User me) {
        Lesson l = ownedLesson(id, me);
        Quiz quiz = quizService.saveFromPayload(l, payload);
        return Map.of("ok", true, "redirect", "/teacher/lessons/" + l.getId());
    }

    // ---------- домашние задания ----------

    @GetMapping("/lessons/{id}/homework")
    public String homeworkForm(@PathVariable Long id, Model model, @ModelAttribute("me") User me) {
        Lesson l = ownedLesson(id, me);
        Homework hw = lessonService.homeworkOf(l.getId());
        HomeworkForm form = new HomeworkForm();
        if (hw != null) {
            form.setTitle(hw.getTitle());
            form.setInstruction(hw.getInstruction());
            form.setMaxPoints(hw.getMaxPoints());
            form.setDeadline(hw.getDeadline());
        }
        model.addAttribute("lesson", l);
        model.addAttribute("form", form);
        model.addAttribute("exists", hw != null);
        return "teacher/homework-form";
    }

    @PostMapping("/lessons/{id}/homework")
    public String saveHomework(@PathVariable Long id, HomeworkForm form,
                               @ModelAttribute("me") User me, RedirectAttributes ra) {
        Lesson l = ownedLesson(id, me);
        if (form.getTitle() == null || form.getTitle().isBlank()) {
            ra.addFlashAttribute("toastError", "Название обязательно");
            return "redirect:/teacher/lessons/" + id + "/homework";
        }
        homeworkService.save(l, form);
        ra.addFlashAttribute("toastSuccess", "Домашнее задание сохранено");
        return "redirect:/teacher/lessons/" + id;
    }

    // ---------- проверка домашних заданий ----------

    @GetMapping("/submissions")
    public String submissions(@RequestParam(required = false) Long courseId,
                              Model model, @ModelAttribute("me") User me) {
        List<ru.schoolsun.homework.HomeworkSubmission> list = homeworkService.submissionsOfTeacher(me.getId());
        if (courseId != null) {
            list = list.stream().filter(s -> s.getHomework().getLesson().getCourse().getId().equals(courseId)).toList();
        }
        model.addAttribute("submissions", list);
        model.addAttribute("myCourses", courses.byTeacher(me));
        model.addAttribute("courseId", courseId);
        return "teacher/submissions";
    }

    @PostMapping("/submissions/{id}/grade")
    public String grade(@PathVariable Long id,
                        @RequestParam int score,
                        @RequestParam String status,
                        @RequestParam(required = false) String feedback,
                        @ModelAttribute("me") User me, RedirectAttributes ra) {
        try {
            homeworkService.grade(id, score, SubmissionStatus.valueOf(status), feedback, me);
            ra.addFlashAttribute("toastSuccess", "Проверено");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("toastError", e.getMessage());
        }
        return "redirect:/teacher/submissions";
    }

    // ---------- проверка открытых ответов тестов ----------

    @GetMapping("/review")
    public String reviewList(Model model, @ModelAttribute("me") User me) {
        model.addAttribute("attempts", quizService.pendingReview(me));
        return "teacher/review";
    }

    @GetMapping("/review/{attemptId}")
    public String reviewAttempt(@PathVariable Long attemptId, Model model, @ModelAttribute("me") User me) {
        QuizAttempt attempt = quizService.attemptById(attemptId);
        if (!attempt.getQuiz().getLesson().getCourse().getTeacher().getId().equals(me.getId())) {
            throw new AccessDeniedException("Это не твой курс");
        }
        model.addAttribute("attempt", attempt);
        model.addAttribute("openAnswers", quizService.answersOf(attemptId).stream()
                .filter(a -> a.getQuestion().getType() == ru.schoolsun.quiz.QuestionType.OPEN)
                .toList());
        return "teacher/review-attempt";
    }

    @PostMapping("/review/{attemptId}")
    public String applyReview(@PathVariable Long attemptId,
                              @RequestParam Map<String, String> params,
                              @ModelAttribute("me") User me, RedirectAttributes ra) {
        Map<Long, Integer> scores = new LinkedHashMap<>();
        params.forEach((k, v) -> {
            if (k.startsWith("q_")) {
                try {
                    scores.put(Long.parseLong(k.substring(2)), Integer.parseInt(v));
                } catch (NumberFormatException ignored2) {
                    // пропускаем мусорные значения
                }
            }
        });
        try {
            quizService.applyReview(attemptId, scores, me);
            ra.addFlashAttribute("toastSuccess", "Попытка проверена");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("toastError", e.getMessage());
        }
        return "redirect:/teacher/review";
    }

    // ---------- аналитика ----------

    @GetMapping("/analytics/{id}")
    public String analytics(@PathVariable Long id, Model model, @ModelAttribute("me") User me) {
        Course c = ownedCourse(id, me);
        model.addAttribute("course", c);
        model.addAttribute("analytics", rating.analytics(c));
        return "teacher/analytics";
    }
}

package ru.schoolsun.student;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ru.schoolsun.certificate.CertificateService;
import ru.schoolsun.common.NotFoundException;
import ru.schoolsun.course.Course;
import ru.schoolsun.course.CourseService;
import ru.schoolsun.course.Enrollment;
import ru.schoolsun.homework.Homework;
import ru.schoolsun.homework.HomeworkService;
import ru.schoolsun.lesson.Lesson;
import ru.schoolsun.lesson.LessonService;
import ru.schoolsun.quiz.Quiz;
import ru.schoolsun.quiz.QuizService;
import ru.schoolsun.rating.RatingService;
import ru.schoolsun.user.Role;
import ru.schoolsun.user.User;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final CourseService courses;
    private final LessonService lessonService;
    private final QuizService quizService;
    private final HomeworkService homeworkService;
    private final RatingService rating;
    private final CertificateService certificates;

    public StudentController(CourseService courses, LessonService lessonService, QuizService quizService,
                             HomeworkService homeworkService, RatingService rating, CertificateService certificates) {
        this.courses = courses;
        this.lessonService = lessonService;
        this.quizService = quizService;
        this.homeworkService = homeworkService;
        this.rating = rating;
        this.certificates = certificates;
    }

    public record MyCourse(Course course, int percent, Lesson next, long lessonsTotal) {}

    public record DeadlineRow(Course course, Homework homework) {}

    @GetMapping
    public String home(Model model, @ModelAttribute("me") User me) {
        Set<Long> completed = lessonService.completedLessonIds(me.getId());
        List<MyCourse> my = new ArrayList<>();
        List<DeadlineRow> deadlines = new ArrayList<>();

        for (Enrollment e : courses.enrollmentsOf(me)) {
            Course c = e.getCourse();
            List<Lesson> ls = lessonService.lessonsOf(c.getId());
            Lesson next = ls.stream()
                    .filter(Lesson::isPublished)
                    .filter(l -> !completed.contains(l.getId()))
                    .findFirst().orElse(null);
            my.add(new MyCourse(c, lessonService.progressPercent(me.getId(), c.getId()), next, ls.size()));

            for (Lesson l : ls) {
                Homework hw = lessonService.homeworkOf(l.getId());
                if (hw != null && hw.getDeadline() != null && hw.getDeadline().isAfter(LocalDateTime.now())) {
                    var sub = homeworkService.submissionOf(hw.getId(), me.getId());
                    if (sub == null || sub.getStatus() != ru.schoolsun.homework.SubmissionStatus.ACCEPTED) {
                        deadlines.add(new DeadlineRow(c, hw));
                    }
                }
            }
        }
        deadlines.sort((a, b) -> a.homework().getDeadline().compareTo(b.homework().getDeadline()));

        model.addAttribute("myCourses", my);
        model.addAttribute("deadlines", deadlines.stream().limit(5).toList());
        model.addAttribute("totalPoints", rating.totalPoints(me.getId()));
        model.addAttribute("certs", certificates.ofStudent(me.getId()));
        model.addAttribute("completedTotal", lessonService.completedLessonIds(me.getId()).size());
        return "student/home";
    }

    /** Переход к следующему непройденному уроку курса. */
    @GetMapping("/courses/{courseId}")
    public String continueCourse(@PathVariable Long courseId, @ModelAttribute("me") User me, RedirectAttributes ra) {
        Course c = courses.byId(courseId);
        if (!courses.isEnrolled(courseId, me.getId())) {
            throw new NotFoundException("Ты не записан на этот курс");
        }
        Set<Long> completed = lessonService.completedLessonIds(me.getId());
        List<Lesson> ls = lessonService.lessonsOf(courseId);
        Lesson next = ls.stream().filter(Lesson::isPublished).filter(l -> !completed.contains(l.getId()))
                .findFirst().orElse(null);
        if (next == null) {
            ra.addFlashAttribute("toastSuccess", "Этот курс полностью пройден — забирай сертификат!");
            return "redirect:/courses/" + c.getSlug();
        }
        return "redirect:/student/lessons/" + next.getId();
    }

    @GetMapping("/lessons/{lessonId}")
    public String lesson(@PathVariable Long lessonId, Model model, @ModelAttribute("me") User me) {
        Lesson lesson = lessonService.byId(lessonId);
        Course course = lesson.getCourse();
        if (!lesson.isPublished() && !courses.canManage(course, me)) {
            throw new NotFoundException("Урок пока не опубликован");
        }
        if (me.getRole() == Role.STUDENT && !courses.isEnrolled(course.getId(), me.getId())) {
            throw new NotFoundException("Сначала запишись на курс");
        }
        Set<Long> completed = lessonService.completedLessonIds(me.getId());
        List<Lesson> all = lessonService.lessonsOf(course.getId()).stream()
                .filter(Lesson::isPublished).toList();
        if (me.getRole() == Role.STUDENT && !lessonService.canOpen(lesson, course, completed)) {
            throw new NotFoundException("Предыдущие уроки курса ещё не пройдены");
        }

        Lesson prev = null;
        Lesson next = null;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(lesson.getId())) {
                prev = i > 0 ? all.get(i - 1) : null;
                next = i < all.size() - 1 ? all.get(i + 1) : null;
            }
        }

        Set<Long> openableIds = new java.util.HashSet<>();
        for (Lesson l : all) {
            if (lessonService.canOpen(l, course, completed)) {
                openableIds.add(l.getId());
            }
        }

        Quiz quiz = lessonService.quizOf(lesson.getId());
        Homework hw = lessonService.homeworkOf(lesson.getId());

        model.addAttribute("course", course);
        model.addAttribute("lesson", lesson);
        model.addAttribute("prev", prev);
        model.addAttribute("next", next);
        model.addAttribute("allLessons", all);
        model.addAttribute("openableIds", openableIds);
        model.addAttribute("theoryHtml", lessonService.renderMarkdown(lesson.getTheoryMd()));
        model.addAttribute("embed", lessonService.videoEmbed(lesson.getVideoUrl()));
        model.addAttribute("attachments", lessonService.attachmentsOf(lesson.getId()));
        model.addAttribute("completed", completed.contains(lesson.getId()));
        model.addAttribute("completedIds", completed);
        model.addAttribute("percent", lessonService.progressPercent(me.getId(), course.getId()));

        if (quiz != null) {
            model.addAttribute("quiz", quiz);
            model.addAttribute("quizRows", quizService.questionsOf(quiz.getId()));
            model.addAttribute("attemptsUsed", quizService.attemptsUsed(quiz.getId(), me.getId()));
            model.addAttribute("bestScore", quizService.bestScore(quiz.getId(), me.getId()));
            model.addAttribute("quizTotal", quizService.totalPoints(quiz));
        }
        if (hw != null) {
            model.addAttribute("homework", hw);
            model.addAttribute("submission", homeworkService.submissionOf(hw.getId(), me.getId()));
        }
        return "student/lesson";
    }

    @PostMapping("/lessons/{lessonId}/complete")
    public String complete(@PathVariable Long lessonId, @ModelAttribute("me") User me, RedirectAttributes ra) {
        Lesson lesson = lessonService.byId(lessonId);
        boolean ok = lessonService.completeManually(lesson, me);
        ra.addFlashAttribute(ok ? "toastSuccess" : "toastError",
                ok ? "Урок засчитан! Рассвет ближе." : "Сначала сдай тест и домашнее задание");
        return "redirect:/student/lessons/" + lessonId;
    }

    @PostMapping("/lessons/{lessonId}/homework")
    public String submitHomework(@PathVariable Long lessonId,
                                 @RequestParam(required = false) String textAnswer,
                                 @RequestParam(required = false) MultipartFile file,
                                 @ModelAttribute("me") User me, RedirectAttributes ra) {
        Homework hw = lessonService.homeworkOf(lessonId);
        if (hw == null) {
            throw new NotFoundException("Домашнее задание не найдено");
        }
        try {
            homeworkService.submit(me, hw, textAnswer, file);
            ra.addFlashAttribute("toastSuccess", "Домашняя работа сдана — учитель скоро проверит");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("toastError", e.getMessage());
        }
        return "redirect:/student/lessons/" + lessonId;
    }

    @GetMapping("/leaderboard")
    public String leaderboard(@RequestParam(required = false) Long courseId,
                              Model model, @ModelAttribute("me") User me) {
        List<MyCourse> my = new ArrayList<>();
        for (Enrollment e : courses.enrollmentsOf(me)) {
            my.add(new MyCourse(e.getCourse(),
                    lessonService.progressPercent(me.getId(), e.getCourse().getId()), null, 0));
        }
        model.addAttribute("myCourses", my);
        model.addAttribute("selectedId", courseId);
        if (courseId != null && courses.isEnrolled(courseId, me.getId())) {
            Course c = courses.byId(courseId);
            List<RatingService.LeaderboardRow> rows = rating.leaderboard(c);
            RatingService.LeaderboardRow myRow = null;
            for (RatingService.LeaderboardRow r : rows) {
                if (r.studentId().equals(me.getId())) {
                    myRow = r;
                    break;
                }
            }
            model.addAttribute("course", c);
            model.addAttribute("rows", rows);
            model.addAttribute("myRow", myRow);
            model.addAttribute("myId", me.getId());
        }
        return "student/leaderboard";
    }
}

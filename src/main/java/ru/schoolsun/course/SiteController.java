package ru.schoolsun.course;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ru.schoolsun.common.NotFoundException;
import ru.schoolsun.lesson.LessonService;
import ru.schoolsun.lesson.LessonRepository;
import ru.schoolsun.user.Role;
import ru.schoolsun.user.User;
import ru.schoolsun.user.UserRepository;

@Controller
public class SiteController {

    private final CourseService courses;
    private final LessonService lessonService;
    private final UserRepository users;
    private final LessonRepository lessons;

    public SiteController(CourseService courses, LessonService lessonService,
                          UserRepository users, LessonRepository lessons) {
        this.courses = courses;
        this.lessonService = lessonService;
        this.users = users;
        this.lessons = lessons;
    }

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("topCourses", courses.topPublished(3));
        model.addAttribute("statStudents", users.countByRole(Role.STUDENT));
        model.addAttribute("statCourses", courses.search(null, null, null).size());
        model.addAttribute("statLessons", lessons.count());
        return "index";
    }

    @GetMapping("/courses")
    public String catalog(@RequestParam(required = false) String q,
                          @RequestParam(required = false) Subject subject,
                          @RequestParam(required = false) Integer grade,
                          Model model) {
        model.addAttribute("courses", courses.search(q, subject, grade));
        model.addAttribute("subjects", Subject.values());
        model.addAttribute("q", q);
        model.addAttribute("selSubject", subject);
        model.addAttribute("selGrade", grade);
        return "courses";
    }

    @GetMapping("/courses/{slug}")
    public String coursePage(@PathVariable String slug, Model model,
                             @ModelAttribute("me") User me) {
        Course c = courses.bySlug(slug);
        boolean enrolledStudent = me != null && me.getRole() == Role.STUDENT
                && courses.isEnrolled(c.getId(), me.getId());
        boolean hidden = !c.isPublished()
                || (c.isInviteOnly() && !enrolledStudent && !courses.canManage(c, me));
        if (hidden && !courses.canManage(c, me)) {
            throw new NotFoundException("Курс не найден");
        }
        model.addAttribute("course", c);
        model.addAttribute("lessons", lessonService.lessonsOf(c.getId()));
        model.addAttribute("lessonCount", lessonService.lessonsOf(c.getId()).size());
        model.addAttribute("enrolled", enrolledStudent);
        model.addAttribute("canManage", courses.canManage(c, me));
        model.addAttribute("studentCount", courses.studentCount(c.getId()));
        return "course";
    }

    /** Ссылка-приглашение из школьного чата: /join/{code} — запись на спецкурс в один клик. */
    @GetMapping("/join/{code}")
    public String joinByInvite(@PathVariable String code,
                               @ModelAttribute("me") User me, RedirectAttributes ra) {
        Course c = courses.byInviteCode(code);
        if (me == null) {
            return "redirect:/login?next=/join/" + code;
        }
        if (me.getRole() != Role.STUDENT) {
            ra.addFlashAttribute("toastError", "Записаться по ссылке может только ученик");
            return "redirect:/";
        }
        courses.enroll(me, c);
        ra.addFlashAttribute("toastSuccess",
                "Приглашение принято! Ты записан на спецкурс «" + c.getTitle() + "»");
        return "redirect:/student/courses/" + c.getId();
    }

    @PostMapping("/courses/{slug}/enroll")
    public String enroll(@PathVariable String slug, @ModelAttribute("me") User me, RedirectAttributes ra) {
        if (me == null) {
            return "redirect:/login";
        }
        Course c = courses.bySlug(slug);
        if (me.getRole() != Role.STUDENT) {
            ra.addFlashAttribute("toastError", "Записаться на курс может только ученик");
            return "redirect:/courses/" + slug;
        }
        courses.enroll(me, c);
        ra.addFlashAttribute("toastSuccess", "Ты записан на курс «" + c.getTitle() + "». Удачи!");
        return "redirect:/student/courses/" + c.getId();
    }

    @GetMapping("/403")
    public String denied() {
        return "error/403";
    }
}

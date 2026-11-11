package ru.schoolsun.admin;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ru.schoolsun.course.Course;
import ru.schoolsun.course.CourseRepository;
import ru.schoolsun.course.CourseService;
import ru.schoolsun.user.Role;
import ru.schoolsun.user.User;
import ru.schoolsun.user.UserRepository;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository users;
    private final CourseService courses;
    private final CourseRepository courseRepo;

    public AdminController(UserRepository users, CourseService courses, CourseRepository courseRepo) {
        this.users = users;
        this.courses = courses;
        this.courseRepo = courseRepo;
    }

    @GetMapping
    public String panel(Model model) {
        model.addAttribute("users", users.findAll());
        model.addAttribute("courses", courseRepo.findAllByOrderByCreatedAtDesc());
        return "admin/panel";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam String role,
                             @ModelAttribute("me") User me, RedirectAttributes ra) {
        if (me.getId().equals(id)) {
            throw new AccessDeniedException("Свою роль менять нельзя");
        }
        User u = users.findById(id).orElseThrow(() -> new ru.schoolsun.common.NotFoundException("Пользователь не найден"));
        u.setRole(Role.valueOf(role));
        users.save(u);
        ra.addFlashAttribute("toastSuccess", "Роль обновлена");
        return "redirect:/admin";
    }

    @PostMapping("/users/{id}/block")
    public String toggleBlock(@PathVariable Long id, @ModelAttribute("me") User me, RedirectAttributes ra) {
        if (me.getId().equals(id)) {
            throw new AccessDeniedException("Себя блокировать нельзя");
        }
        User u = users.findById(id).orElseThrow(() -> new ru.schoolsun.common.NotFoundException("Пользователь не найден"));
        u.setBlocked(!u.isBlocked());
        users.save(u);
        ra.addFlashAttribute("toastSuccess", u.isBlocked() ? "Пользователь заблокирован" : "Пользователь разблокирован");
        return "redirect:/admin";
    }

    @PostMapping("/courses/{id}/publish")
    public String togglePublish(@PathVariable Long id, RedirectAttributes ra) {
        Course c = courses.byId(id);
        c.setPublished(!c.isPublished());
        ra.addFlashAttribute("toastSuccess", c.isPublished() ? "Курс опубликован" : "Курс снят с публикации");
        return "redirect:/admin";
    }

    @PostMapping("/courses/{id}/delete")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes ra) {
        courses.delete(courses.byId(id));
        ra.addFlashAttribute("toastSuccess", "Курс удалён");
        return "redirect:/admin";
    }
}

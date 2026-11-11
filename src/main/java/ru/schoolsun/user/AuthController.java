package ru.schoolsun.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping
public class AuthController {

    private final UserService users;

    public AuthController(UserService users) {
        this.users = users;
    }

    /** Уже вошедшим страницы входа и регистрации не нужны — отправляем на его главную. */
    private String homeOf(User me) {
        if (me == null) {
            return null;
        }
        return switch (me.getRole()) {
            case ADMIN -> "redirect:/admin";
            case TEACHER -> "redirect:/teacher";
            case STUDENT -> "redirect:/student";
        };
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String registered,
                        @RequestParam(required = false) String reset,
                        @RequestParam(required = false) String ratelimit,
                        @RequestParam(required = false) String blocked,
                        @ModelAttribute("me") User me,
                        Model model) {
        String home = homeOf(me);
        if (home != null) {
            return home;
        }
        if (error != null) model.addAttribute("error", "Неверный email или пароль");
        if (ratelimit != null) model.addAttribute("error", "Слишком много попыток входа — подожди минуту");
        if (blocked != null) model.addAttribute("error", "Аккаунт заблокирован");
        if (logout != null) model.addAttribute("info", "Ты вышел из аккаунта. Возвращайся на рассвете!");
        if (registered != null) model.addAttribute("info", "Регистрация готова — теперь войди");
        if (reset != null) model.addAttribute("info", "Пароль обновлён — войди с новым паролем");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(@ModelAttribute("me") User me, Model model) {
        String home = homeOf(me);
        if (home != null) {
            return home;
        }
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(RegisterForm form, @ModelAttribute("me") User me, Model model) {
        String home = homeOf(me);
        if (home != null) {
            return home;
        }
        try {
            if (form.getEmail() == null || !form.getEmail().contains("@")) {
                throw new IllegalArgumentException("Похоже, email указан неверно");
            }
            if (form.getPassword() == null || form.getPassword().length() < 6) {
                throw new IllegalArgumentException("Пароль должен быть не короче 6 символов");
            }
            if (form.getFullName() == null || form.getFullName().isBlank()) {
                throw new IllegalArgumentException("Укажи имя");
            }
            users.register(form);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("form", form);
            return "auth/register";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotForm() {
        return "auth/forgot";
    }

    @PostMapping("/forgot-password")
    public String forgot(@RequestParam String email, Model model) {
        String token = users.createResetToken(email);
        if (token == null) {
            model.addAttribute("error", "Пользователь с таким email не найден");
            return "auth/forgot";
        }
        // Демо-режим: письмо не отправляется, ссылка показывается прямо здесь
        model.addAttribute("resetLink", "/reset-password?token=" + token);
        return "auth/forgot";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset";
    }

    @PostMapping("/reset-password")
    public String reset(@RequestParam String token,
                        @RequestParam String password,
                        @RequestParam String password2,
                        RedirectAttributes ra) {
        if (password == null || password.length() < 6 || !password.equals(password2)) {
            ra.addFlashAttribute("error", "Пароли должны совпадать и быть не короче 6 символов");
            return "redirect:/reset-password?token=" + token;
        }
        if (!users.resetPassword(token, password)) {
            ra.addAttribute("error", "Ссылка недействительна или устарела");
            return "redirect:/forgot-password";
        }
        return "redirect:/login?reset";
    }
}

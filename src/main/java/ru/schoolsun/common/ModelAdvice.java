package ru.schoolsun.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ru.schoolsun.user.User;
import ru.schoolsun.user.UserRepository;

import java.security.Principal;

/** Добавляет текущего пользователя (me) на каждую страницу для шапки. */
@ControllerAdvice
public class ModelAdvice {

    private final UserRepository users;

    public ModelAdvice(UserRepository users) {
        this.users = users;
    }

    @ModelAttribute("me")
    public User currentUser(Principal principal) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return users.findByEmailIgnoreCase(principal != null ? principal.getName() : auth.getName())
                .orElse(null);
    }
}

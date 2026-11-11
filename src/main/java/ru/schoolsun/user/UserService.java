package ru.schoolsun.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository users;
    private final ResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    public UserService(UserRepository users, ResetTokenRepository tokens, PasswordEncoder encoder) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Нет пользователя: " + email));
        return org.springframework.security.core.userdetails.User.builder()
                .username(u.getEmail())
                .password(u.getPasswordHash())
                .roles(u.getRole().name())
                .disabled(u.isBlocked())
                .build();
    }

    @Transactional
    public User register(RegisterForm form) {
        if (users.existsByEmailIgnoreCase(form.getEmail().trim())) {
            throw new IllegalArgumentException("Этот email уже зарегистрирован");
        }
        User u = new User();
        u.setEmail(form.getEmail().trim().toLowerCase());
        u.setFullName(form.getFullName().trim());
        u.setPasswordHash(encoder.encode(form.getPassword()));
        u.setRole("TEACHER".equals(form.getRole()) ? Role.TEACHER : Role.STUDENT);
        u.setBlocked(false);
        u.setCreatedAt(LocalDateTime.now());
        return users.save(u);
    }

    public User byEmail(String email) {
        return users.findByEmailIgnoreCase(email).orElse(null);
    }

    /** Создаёт одноразовый токен восстановления пароля (действует 1 час). */
    @Transactional
    public String createResetToken(String email) {
        User u = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (u == null) {
            return null;
        }
        byte[] raw = new byte[24];
        random.nextBytes(raw);
        PasswordResetToken t = new PasswordResetToken();
        t.setUser(u);
        t.setToken(HexFormat.of().formatHex(raw));
        t.setExpiresAt(LocalDateTime.now().plusHours(1));
        t.setUsed(false);
        tokens.save(t);
        return t.getToken();
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        PasswordResetToken t = tokens.findByToken(token).orElse(null);
        if (t == null || t.isUsed() || t.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        t.getUser().setPasswordHash(encoder.encode(newPassword));
        t.setUsed(true);
        return true;
    }
}

package ru.schoolsun.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.schoolsun.user.Role;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** После входа: ?next=... если был (например, ссылка-приглашение), иначе главная по роли. */
    @Bean
    public AuthenticationSuccessHandler roleSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                org.springframework.security.core.Authentication auth) -> {
            String next = request.getParameter("next");
            if (next != null && next.startsWith("/") && !next.startsWith("//")) {
                response.sendRedirect(request.getContextPath() + next);
                return;
            }
            boolean isTeacher = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            String target = isAdmin ? "/admin" : isTeacher ? "/teacher" : "/student";
            response.sendRedirect(request.getContextPath() + target);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationSuccessHandler roleSuccessHandler) throws Exception {
        http
            .addFilterBefore(new LoginRateLimitFilter(),
                    org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/courses", "/courses/*", "/certificates/*",
                        "/login", "/register", "/forgot-password", "/reset-password",
                        "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/student/**").hasRole(Role.STUDENT.name())
                .requestMatchers("/teacher/**").hasRole(Role.TEACHER.name())
                .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(roleSuccessHandler)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout")
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/403"));

        return http.build();
    }
}

package ru.schoolsun.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Простой rate limiting попыток входа: не более N запросов /login за минуту с одного IP.
 * Создаётся напрямую в SecurityConfig (не как bean — чтобы не попадать в общий servlet-фильтр).
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS_PER_MINUTE = 15;

    private record Window(AtomicInteger count, long minuteStart) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().equals("/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long minute = System.currentTimeMillis() / 60_000;
        String ip = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();

        Window window = windows.compute(ip, (key, old) ->
                old == null || old.minuteStart() != minute
                        ? new Window(new AtomicInteger(0), minute)
                        : old);

        if (window.count().incrementAndGet() > MAX_ATTEMPTS_PER_MINUTE) {
            response.sendRedirect("/login?ratelimit");
            return;
        }
        chain.doFilter(request, response);
    }
}

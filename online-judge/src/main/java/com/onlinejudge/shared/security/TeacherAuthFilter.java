package com.onlinejudge.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@Order(20)
@RequiredArgsConstructor
public class TeacherAuthFilter extends OncePerRequestFilter {

    private final TeacherSessionService teacherSessionService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!requiresTeacherSession(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (teacherSessionService.authenticated(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("error", "请先登录教师端"));
    }

    private boolean requiresTeacherSession(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path == null) {
            return false;
        }
        if (path.startsWith("/api/teacher/")) {
            return !path.startsWith("/api/teacher/auth/");
        }
        if (path.matches("/api/problems/\\d+/manage")) {
            return true;
        }
        if (path.matches("/api/problems/\\d+/growth-report.*") || path.startsWith("/api/leaderboard/")) {
            return true;
        }
        return path.equals("/api/problems") && "POST".equalsIgnoreCase(method)
                || path.matches("/api/problems/\\d+") && !"GET".equalsIgnoreCase(method)
                || path.startsWith("/api/system/ai-smoke");
    }
}

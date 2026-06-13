package com.onlinejudge.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TeacherSessionService {

    public static final String COOKIE_NAME = "OJ_TEACHER_SESSION";

    private final SchoolSecurityProperties properties;

    public boolean login(String password, HttpServletResponse response) {
        if (!properties.teacherPasswordConfigured()) {
            return false;
        }
        if (!CryptoSupport.constantTimeEquals(properties.teacherPassword(), password == null ? "" : password)) {
            return false;
        }
        writeSessionCookie(response, Instant.now().plusSeconds(properties.teacherSessionTtlHours() * 3600));
        return true;
    }

    public void logout(HttpServletResponse response) {
        response.addHeader("Set-Cookie", COOKIE_NAME + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
    }

    public boolean authenticated(HttpServletRequest request) {
        String token = cookieValue(request);
        if (token.isBlank()) {
            return false;
        }
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            return false;
        }
        long expiresAt;
        try {
            expiresAt = Long.parseLong(parts[0]);
        } catch (NumberFormatException exception) {
            return false;
        }
        if (Instant.now().getEpochSecond() > expiresAt) {
            return false;
        }
        String expected = CryptoSupport.hmacSha256(properties.teacherSessionSecret(), parts[0]);
        return CryptoSupport.constantTimeEquals(expected, parts[1]);
    }

    private void writeSessionCookie(HttpServletResponse response, Instant expiresAt) {
        String payload = String.valueOf(expiresAt.getEpochSecond());
        String token = payload + "." + CryptoSupport.hmacSha256(properties.teacherSessionSecret(), payload);
        long maxAge = Math.min(Integer.MAX_VALUE, properties.teacherSessionTtlHours() * 3600);
        response.addHeader("Set-Cookie", COOKIE_NAME + "=" + token + "; Path=/; Max-Age=" + maxAge + "; HttpOnly; SameSite=Lax");
    }

    private String cookieValue(HttpServletRequest request) {
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return "";
        }
        for (String part : cookieHeader.split(";")) {
            String candidate = part.trim();
            if (candidate.startsWith(COOKIE_NAME + "=")) {
                return candidate.substring((COOKIE_NAME + "=").length());
            }
        }
        return "";
    }
}

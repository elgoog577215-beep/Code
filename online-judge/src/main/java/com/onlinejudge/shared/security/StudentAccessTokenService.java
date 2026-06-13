package com.onlinejudge.shared.security;

import com.onlinejudge.classroom.domain.StudentProfile;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class StudentAccessTokenService {

    public static final String HEADER_NAME = "X-Student-Token";

    private final SchoolSecurityProperties properties;

    public String issue(StudentProfile student) {
        if (student == null || student.getId() == null) {
            throw new IllegalArgumentException("学生身份未保存，无法签发访问令牌");
        }
        long expiresAt = Instant.now().plusSeconds(properties.studentTokenTtlDays() * 24 * 3600).getEpochSecond();
        String identityKey = student.getIdentityKey() == null ? "" : student.getIdentityKey();
        String payload = student.getId() + ":" + expiresAt + ":" + base64(identityKey);
        return payload + "." + CryptoSupport.hmacSha256(properties.studentTokenSecret(), payload);
    }

    public Long currentStudentId(HttpServletRequest request) {
        ParsedToken token = parse(request.getHeader(HEADER_NAME));
        return token == null ? null : token.studentProfileId();
    }

    public void requireStudent(HttpServletRequest request, Long expectedStudentProfileId) {
        if (expectedStudentProfileId == null) {
            return;
        }
        ParsedToken token = parse(request.getHeader(HEADER_NAME));
        if (token == null) {
            throw new AuthenticationRequiredException("请先登录学生端");
        }
        if (!expectedStudentProfileId.equals(token.studentProfileId())) {
            throw new AccessDeniedException("只能访问自己的学习数据");
        }
    }

    public void requireAnyOf(HttpServletRequest request, Long... studentProfileIds) {
        ParsedToken token = parse(request.getHeader(HEADER_NAME));
        if (token == null) {
            throw new AuthenticationRequiredException("请先登录学生端");
        }
        for (Long studentProfileId : studentProfileIds) {
            if (studentProfileId != null && studentProfileId.equals(token.studentProfileId())) {
                return;
            }
        }
        throw new AccessDeniedException("只能访问自己的学习数据");
    }

    private ParsedToken parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] tokenParts = token.trim().split("\\.", 2);
        if (tokenParts.length != 2) {
            return null;
        }
        String payload = tokenParts[0];
        String expected = CryptoSupport.hmacSha256(properties.studentTokenSecret(), payload);
        if (!CryptoSupport.constantTimeEquals(expected, tokenParts[1])) {
            return null;
        }
        String[] payloadParts = payload.split(":", 3);
        if (payloadParts.length < 2) {
            return null;
        }
        try {
            Long studentProfileId = Long.parseLong(payloadParts[0]);
            long expiresAt = Long.parseLong(payloadParts[1]);
            if (Instant.now().getEpochSecond() > expiresAt) {
                return null;
            }
            return new ParsedToken(studentProfileId, expiresAt);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String base64(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private record ParsedToken(Long studentProfileId, long expiresAt) {
    }
}

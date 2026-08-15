package com.onlinejudge.shared.security;

import com.onlinejudge.identity.application.TeacherPrincipal;
import com.onlinejudge.identity.domain.TeacherAccount;
import com.onlinejudge.identity.domain.TeacherSession;
import com.onlinejudge.identity.persistence.TeacherAccountRepository;
import com.onlinejudge.identity.persistence.TeacherSessionRepository;
import com.onlinejudge.shared.web.PlatformApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import com.onlinejudge.system.application.TrialMetrics;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherSessionService {

    public static final String COOKIE_NAME = "OJ_TEACHER_SESSION";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SchoolSecurityProperties properties;
    private final TeacherAccountRepository accounts;
    private final TeacherSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private TrialMetrics trialMetrics;

    /** One-version development compatibility alias. Production never accepts the shared password. */
    public boolean login(String password, HttpServletResponse response) {
        if (properties.schoolProfile() || !properties.teacherPasswordConfigured()
                || !CryptoSupport.constantTimeEquals(properties.teacherPassword(), password == null ? "" : password)) {
            return false;
        }
        long maxAge = properties.teacherSessionTtlHours() * 3600;
        String expires = String.valueOf(Instant.now().plusSeconds(maxAge).getEpochSecond());
        String token = "legacy." + expires + "." + CryptoSupport.hmacSha256(properties.teacherSessionSecret(), expires);
        writeCookie(response, token, maxAge);
        return true;
    }

    @Transactional(noRollbackFor = PlatformApiException.class)
    public TeacherPrincipal login(String username, String password, HttpServletRequest request, HttpServletResponse response) {
        Instant now = Instant.now();
        TeacherAccount account = accounts.findByUsernameNormalized(TeacherAccount.normalizeUsername(username))
                .orElseThrow(() -> new PlatformApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "用户名或密码不正确"));
        if (account.getStatus() == TeacherAccount.Status.PENDING) throw statusError("ACCOUNT_PENDING", "账号正在等待审核");
        if (account.getStatus() == TeacherAccount.Status.SUSPENDED) throw statusError("ACCOUNT_SUSPENDED", "账号已停用");
        if (account.getStatus() == TeacherAccount.Status.REJECTED) throw statusError("ACCOUNT_REJECTED", "注册申请未通过");
        if (!account.canAuthenticateAt(now)) throw statusError("ACCOUNT_LOCKED", "登录失败次数过多，请 15 分钟后重试");
        if (!passwordEncoder.matches(password == null ? "" : password, account.getPasswordHash())) {
            account.recordFailedLogin(now);
            if (trialMetrics != null) trialMetrics.loginFailed();
            throw new PlatformApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "用户名或密码不正确");
        }
        account.recordSuccessfulLogin(now);
        String token = randomToken();
        long maxAge = properties.teacherSessionTtlHours() * 3600;
        sessions.save(TeacherSession.builder()
                .id(UUID.randomUUID()).teacherId(account.getId()).tokenHash(CryptoSupport.sha256(token))
                .createdAt(now).expiresAt(now.plusSeconds(maxAge)).lastSeenAt(now)
                .ipAddress(clientIp(request)).userAgent(limit(request.getHeader("User-Agent"), 300)).build());
        writeCookie(response, token, maxAge);
        return principal(account);
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String token = cookieValue(request);
        if (!token.isBlank()) sessions.findByTokenHash(CryptoSupport.sha256(token)).ifPresent(session -> session.setRevokedAt(Instant.now()));
        clearCookie(response);
    }

    public void logout(HttpServletResponse response) {
        clearCookie(response);
    }

    @Transactional
    public Optional<TeacherPrincipal> resolve(HttpServletRequest request) {
        if (properties.teacherDevAutoAuth()) {
            return Optional.of(new TeacherPrincipal(TeacherAccount.BOOTSTRAP_ADMIN_ID, "dev-admin", "开发管理员",
                    TeacherAccount.Role.ADMIN, false));
        }
        String token = cookieValue(request);
        if (token.isBlank()) return Optional.empty();
        if (!properties.schoolProfile() && validLegacyToken(token)) {
            return Optional.of(new TeacherPrincipal(TeacherAccount.BOOTSTRAP_ADMIN_ID, "legacy-dev-admin", "开发管理员",
                    TeacherAccount.Role.ADMIN, false));
        }
        TeacherSession session = sessions.findByTokenHash(CryptoSupport.sha256(token)).orElse(null);
        Instant now = Instant.now();
        if (session == null || !session.validAt(now)) return Optional.empty();
        TeacherAccount account = accounts.findById(session.getTeacherId()).orElse(null);
        if (account == null || !account.canAuthenticateAt(now)) return Optional.empty();
        session.setLastSeenAt(now);
        return Optional.of(principal(account));
    }

    public boolean authenticated(HttpServletRequest request) {
        return resolve(request).isPresent();
    }

    @Transactional
    public void revokeAll(UUID teacherId) {
        sessions.revokeAll(teacherId, Instant.now());
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return limit(forwarded.split(",")[0].trim(), 80);
        return limit(request.getRemoteAddr(), 80);
    }

    private TeacherPrincipal principal(TeacherAccount account) {
        return new TeacherPrincipal(account.getId(), account.getUsernameNormalized(), account.getDisplayName(),
                account.getRole(), account.isMustChangePassword());
    }

    private PlatformApiException statusError(String code, String message) {
        return new PlatformApiException(HttpStatus.FORBIDDEN, code, message);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean validLegacyToken(String token) {
        String[] parts = token.split("\\.", 3);
        if (parts.length != 3 || !"legacy".equals(parts[0])) return false;
        try {
            long expiresAt = Long.parseLong(parts[1]);
            return Instant.now().getEpochSecond() < expiresAt
                    && CryptoSupport.constantTimeEquals(parts[2], CryptoSupport.hmacSha256(properties.teacherSessionSecret(), parts[1]));
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void writeCookie(HttpServletResponse response, String token, long maxAge) {
        String secure = properties.schoolProfile() ? "; Secure" : "";
        response.addHeader("Set-Cookie", COOKIE_NAME + "=" + token + "; Path=/; Max-Age=" + Math.min(Integer.MAX_VALUE, maxAge)
                + "; HttpOnly; SameSite=Lax" + secure);
    }

    private void clearCookie(HttpServletResponse response) {
        String secure = properties.schoolProfile() ? "; Secure" : "";
        response.addHeader("Set-Cookie", COOKIE_NAME + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax" + secure);
    }

    private String cookieValue(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) if (COOKIE_NAME.equals(cookie.getName())) return cookie.getValue();
        }
        String header = request.getHeader("Cookie");
        if (header != null) {
            for (String part : header.split(";")) {
                String[] pair = part.trim().split("=", 2);
                if (pair.length == 2 && COOKIE_NAME.equals(pair[0])) return pair[1];
            }
        }
        return "";
    }

    private static String limit(String value, int max) {
        String normalized = value == null ? "" : value;
        return normalized.length() > max ? normalized.substring(0, max) : normalized;
    }
}

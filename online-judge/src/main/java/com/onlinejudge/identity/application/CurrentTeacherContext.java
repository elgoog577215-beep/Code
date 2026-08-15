package com.onlinejudge.identity.application;

import com.onlinejudge.identity.domain.TeacherAccount;
import com.onlinejudge.shared.security.AuthenticationRequiredException;
import com.onlinejudge.shared.security.SchoolSecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentTeacherContext {
    private final SchoolSecurityProperties properties;

    public TeacherPrincipal require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof TeacherPrincipal principal) {
            return principal;
        }
        if (properties.teacherDevAutoAuth()) {
            return new TeacherPrincipal(TeacherAccount.BOOTSTRAP_ADMIN_ID, "dev-admin", "开发管理员",
                    TeacherAccount.Role.ADMIN, false);
        }
        throw new AuthenticationRequiredException("AUTH_REQUIRED");
    }

    public UUID requireTeacherId() {
        return require().id();
    }

    public TeacherPrincipal requireAdmin() {
        TeacherPrincipal principal = require();
        if (principal.role() != TeacherAccount.Role.ADMIN) {
            throw new com.onlinejudge.shared.security.AccessDeniedException("FORBIDDEN");
        }
        return principal;
    }
}


package com.onlinejudge.identity.dto;

import com.onlinejudge.identity.domain.TeacherAccount;

import java.time.Instant;
import java.util.UUID;

public record TeacherAccountResponse(UUID id, String username, String displayName, String schoolName,
                                     TeacherAccount.Role role, TeacherAccount.Status status,
                                     boolean mustChangePassword, Instant createdAt) {
    public static TeacherAccountResponse from(TeacherAccount account) {
        return new TeacherAccountResponse(account.getId(), account.getUsernameNormalized(), account.getDisplayName(),
                account.getSchoolName(), account.getRole(), account.getStatus(),
                account.isMustChangePassword(), account.getCreatedAt());
    }
}

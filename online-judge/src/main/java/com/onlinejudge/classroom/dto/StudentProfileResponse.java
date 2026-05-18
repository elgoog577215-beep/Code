package com.onlinejudge.classroom.dto;

import com.onlinejudge.classroom.domain.StudentProfile;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StudentProfileResponse {
    private Long id;
    private Long classGroupId;
    private String displayName;
    private String studentNo;
    private String note;
    private String identityKey;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeenAt;

    public static StudentProfileResponse from(StudentProfile student) {
        return StudentProfileResponse.builder()
                .id(student.getId())
                .classGroupId(student.getClassGroupId())
                .displayName(student.getDisplayName())
                .studentNo(student.getStudentNo())
                .note(student.getNote())
                .identityKey(student.getIdentityKey())
                .createdAt(student.getCreatedAt())
                .lastSeenAt(student.getLastSeenAt())
                .build();
    }
}

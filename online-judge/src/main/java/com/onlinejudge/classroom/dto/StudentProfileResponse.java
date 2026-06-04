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
    private String className;
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
                .className(null)
                .displayName(student.getDisplayName())
                .studentNo(student.getStudentNo())
                .note(student.getNote())
                .identityKey(student.getIdentityKey())
                .createdAt(student.getCreatedAt())
                .lastSeenAt(student.getLastSeenAt())
                .build();
    }

    public static StudentProfileResponse from(StudentProfile student, String className) {
        StudentProfileResponse response = from(student);
        response.setClassName(className);
        return response;
    }
}

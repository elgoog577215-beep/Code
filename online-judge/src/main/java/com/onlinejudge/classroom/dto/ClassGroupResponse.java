package com.onlinejudge.classroom.dto;

import com.onlinejudge.classroom.domain.ClassGroup;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClassGroupResponse {
    private Long id;
    private String name;
    private String grade;
    private String teacherName;
    private LocalDateTime createdAt;

    public static ClassGroupResponse from(ClassGroup classGroup) {
        return ClassGroupResponse.builder()
                .id(classGroup.getId())
                .name(classGroup.getName())
                .grade(classGroup.getGrade())
                .teacherName(classGroup.getTeacherName())
                .createdAt(classGroup.getCreatedAt())
                .build();
    }
}

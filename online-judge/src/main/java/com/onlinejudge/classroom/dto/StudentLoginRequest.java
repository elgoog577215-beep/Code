package com.onlinejudge.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentLoginRequest {
    @NotNull(message = "请选择班级")
    private Long classGroupId;

    @NotBlank(message = "姓名不能为空")
    private String displayName;

    private String studentNo;
    private String note;
}

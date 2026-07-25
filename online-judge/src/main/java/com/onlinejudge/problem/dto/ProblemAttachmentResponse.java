package com.onlinejudge.problem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProblemAttachmentResponse {
    private String id;
    private String fileName;
    private Long sizeBytes;
    private String contentType;
    private String downloadUrl;
}

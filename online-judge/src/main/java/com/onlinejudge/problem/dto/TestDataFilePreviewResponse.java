package com.onlinejudge.problem.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TestDataFilePreviewResponse {
    private Long testCaseId;
    private String kind;
    private String fileName;
    private long sizeBytes;
    private List<String> lines;
    private boolean truncated;
}

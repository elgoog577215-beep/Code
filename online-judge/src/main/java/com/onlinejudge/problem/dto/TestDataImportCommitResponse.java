package com.onlinejudge.problem.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TestDataImportCommitResponse {
    private String importBatchId;
    private int testCaseCount;
    private long uncompressedBytes;
    private List<Long> testCaseIds;
    private TestDataImportPreviewResponse preview;
}

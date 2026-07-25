package com.onlinejudge.problem.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TestDataImportPreviewResponse {
    private boolean valid;
    private String message;
    private long compressedBytes;
    private long uncompressedBytes;
    private int pairCount;
    private List<Row> rows;
    private List<Issue> issues;

    @Data
    @Builder
    public static class Row {
        private int displayIndex;
        private int number;
        private String inputFileName;
        private String outputFileName;
        private long inputSizeBytes;
        private long outputSizeBytes;
        private String inputSha256;
        private String outputSha256;
    }

    @Data
    @Builder
    public static class Issue {
        private String severity;
        private String message;
        private String fileName;
    }
}

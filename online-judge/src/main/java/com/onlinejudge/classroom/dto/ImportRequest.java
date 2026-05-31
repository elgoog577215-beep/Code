package com.onlinejudge.classroom.dto;

import lombok.Data;

import java.util.List;

@Data
public class ImportRequest {
    private String format;
    private String content;
    private String fileName;
    private Long classGroupId;
    private String className;
    private TestcaseImport testcaseImport;

    @Data
    public static class TestcaseImport {
        private String visibility;
        private List<TestcaseFile> inputFiles;
        private List<TestcaseFile> answerFiles;
        private TestcaseFile zipFile;
    }

    @Data
    public static class TestcaseFile {
        private String name;
        private String content;
    }
}

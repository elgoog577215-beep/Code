package com.onlinejudge.problem.dto;

import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.problem.domain.TestCase;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProblemManageResponse {
    private Long id;
    private String title;
    private String description;
    private Problem.Difficulty difficulty;
    private Integer timeLimit;
    private Integer memoryLimit;
    private String aiPromptDirection;
    private String starterCode;
    private Problem.ProblemStatus status;
    private String statementBackground;
    private String statementDescription;
    private String statementInputFormat;
    private String statementOutputFormat;
    private String statementSamples;
    private String statementHints;
    private String provider;
    private String attachments;
    private List<String> tags;
    private Boolean dataDownloadEnabled;
    private Problem.ScoreDisplayMode scoreDisplayMode;
    private List<String> knowledgePoints;
    private List<String> algorithmStrategies;
    private List<String> commonMistakes;
    private List<String> boundaryTypes;
    private LocalDateTime createdAt;
    private List<TestCaseItem> testCases;

    @Data
    @Builder
    public static class TestCaseItem {
        private Long id;
        private String input;
        private String expectedOutput;
        private Boolean hidden;
        private Integer orderIndex;
        private String inputStorageType;
        private String outputStorageType;
        private String inputFilePath;
        private String outputFilePath;
        private String inputFileName;
        private String outputFileName;
        private Long inputSizeBytes;
        private Long outputSizeBytes;
        private String inputSha256;
        private String outputSha256;
        private Integer timeLimitMs;
        private Integer memoryLimitKib;
        private Integer subtaskIndex;
        private Integer score;
        private Boolean publicExample;
        private String importBatchId;
    }

    public static ProblemManageResponse from(Problem problem, List<TestCase> testCases) {
        return ProblemManageResponse.builder()
                .id(problem.getId())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .difficulty(problem.getDifficulty())
                .timeLimit(problem.getTimeLimit())
                .memoryLimit(problem.getMemoryLimit())
                .aiPromptDirection(problem.getAiPromptDirection())
                .starterCode(problem.getStarterCode())
                .status(problem.getStatus())
                .statementBackground(problem.getStatementBackground())
                .statementDescription(problem.getStatementDescription())
                .statementInputFormat(problem.getStatementInputFormat())
                .statementOutputFormat(problem.getStatementOutputFormat())
                .statementSamples(problem.getStatementSamples())
                .statementHints(problem.getStatementHints())
                .provider(problem.getProvider())
                .attachments(problem.getAttachments())
                .tags(safeList(problem.getTags()))
                .dataDownloadEnabled(Boolean.TRUE.equals(problem.getDataDownloadEnabled()))
                .scoreDisplayMode(problem.getScoreDisplayMode())
                .knowledgePoints(safeList(problem.getKnowledgePoints()))
                .algorithmStrategies(safeList(problem.getAlgorithmStrategies()))
                .commonMistakes(safeList(problem.getCommonMistakes()))
                .boundaryTypes(safeList(problem.getBoundaryTypes()))
                .createdAt(problem.getCreatedAt())
                .testCases(testCases.stream()
                        .map(testCase -> TestCaseItem.builder()
                                .id(testCase.getId())
                                .input(testCase.getInput())
                                .expectedOutput(testCase.getExpectedOutput())
                                .hidden(Boolean.TRUE.equals(testCase.getIsHidden()))
                                .orderIndex(testCase.getOrderIndex())
                                .inputStorageType(storageName(testCase.getInputStorageType()))
                                .outputStorageType(storageName(testCase.getOutputStorageType()))
                                .inputFilePath(testCase.getInputFilePath())
                                .outputFilePath(testCase.getOutputFilePath())
                                .inputFileName(testCase.getInputFileName())
                                .outputFileName(testCase.getOutputFileName())
                                .inputSizeBytes(testCase.getInputSizeBytes())
                                .outputSizeBytes(testCase.getOutputSizeBytes())
                                .inputSha256(testCase.getInputSha256())
                                .outputSha256(testCase.getOutputSha256())
                                .timeLimitMs(testCase.getTimeLimitMs())
                                .memoryLimitKib(testCase.getMemoryLimitKib())
                                .subtaskIndex(testCase.getSubtaskIndex())
                                .score(testCase.getScore())
                                .publicExample(Boolean.TRUE.equals(testCase.getPublicExample()))
                                .importBatchId(testCase.getImportBatchId())
                                .build())
                        .toList())
                .build();
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static String storageName(TestCase.StorageType storageType) {
        return storageType == null ? TestCase.StorageType.INLINE.name() : storageType.name();
    }
}

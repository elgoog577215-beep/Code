package com.onlinejudge.problem.dto;

import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.problem.domain.TestCase;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Data
@Builder
public class ProblemResponse {
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
    private List<String> tags;
    private Boolean dataDownloadEnabled;
    private Problem.ScoreDisplayMode scoreDisplayMode;
    private List<String> knowledgePoints;
    private List<String> algorithmStrategies;
    private List<String> commonMistakes;
    private List<String> boundaryTypes;
    private LocalDateTime createdAt;
    private List<SampleTestCase> sampleTestCases;

    @Data
    @Builder
    public static class SampleTestCase {
        private String input;
        private String expectedOutput;
    }

    public static ProblemResponse from(Problem problem, List<TestCase> visibleTestCases) {
        return from(problem, visibleTestCases,
                testCase -> testCase.getInput() == null ? "" : testCase.getInput(),
                testCase -> testCase.getExpectedOutput() == null ? "" : testCase.getExpectedOutput());
    }

    public static ProblemResponse from(Problem problem,
                                       List<TestCase> visibleTestCases,
                                       Function<TestCase, String> inputResolver,
                                       Function<TestCase, String> outputResolver) {
        return ProblemResponse.builder()
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
                .tags(safeList(problem.getTags()))
                .dataDownloadEnabled(Boolean.TRUE.equals(problem.getDataDownloadEnabled()))
                .scoreDisplayMode(problem.getScoreDisplayMode())
                .knowledgePoints(safeList(problem.getKnowledgePoints()))
                .algorithmStrategies(safeList(problem.getAlgorithmStrategies()))
                .commonMistakes(safeList(problem.getCommonMistakes()))
                .boundaryTypes(safeList(problem.getBoundaryTypes()))
                .createdAt(problem.getCreatedAt())
                .sampleTestCases(visibleTestCases.stream()
                        .map(tc -> SampleTestCase.builder()
                                .input(inputResolver.apply(tc))
                                .expectedOutput(outputResolver.apply(tc))
                                .build())
                        .toList())
                .build();
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}

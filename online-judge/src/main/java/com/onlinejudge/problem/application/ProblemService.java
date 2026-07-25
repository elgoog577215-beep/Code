package com.onlinejudge.problem.application;

import com.onlinejudge.problem.dto.CreateProblemRequest;
import com.onlinejudge.problem.dto.ProblemCatalogItemResponse;
import com.onlinejudge.problem.dto.ProblemManageResponse;
import com.onlinejudge.problem.dto.ProblemResponse;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.problem.domain.TestCase;
import com.onlinejudge.problem.persistence.ProblemRepository;
import com.onlinejudge.submission.persistence.SubmissionAnalysisRepository;
import com.onlinejudge.submission.persistence.SubmissionCaseResultRepository;
import com.onlinejudge.submission.persistence.SubmissionRepository;
import com.onlinejudge.problem.persistence.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern MARKDOWN_PATTERN = Pattern.compile("[#>*`_\\n]");

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionCaseResultRepository submissionCaseResultRepository;
    private final SubmissionAnalysisRepository submissionAnalysisRepository;
    private final TestCaseContentService testCaseContentService;

    public List<ProblemResponse> getAllProblems() {
        return problemRepository.findAllByOrderByIdAsc()
                .stream()
                .map(p -> ProblemResponse.from(p, List.of()))
                .toList();
    }

    public List<ProblemCatalogItemResponse> getProblemCatalog() {
        return problemRepository.findCatalogItems()
                .stream()
                .map(problem -> ProblemCatalogItemResponse.builder()
                        .id(problem.getId())
                        .title(problem.getTitle())
                        .summary(extractSummary(problem.getDescription()))
                        .difficulty(problem.getDifficulty())
                        .timeLimit(problem.getTimeLimit())
                        .memoryLimit(problem.getMemoryLimit())
                        .createdAt(problem.getCreatedAt())
                        .build())
                .toList();
    }

    public ProblemResponse getProblemById(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("题目不存在: " + id));
        
        List<TestCase> visibleTestCases = testCaseRepository
                .findByProblemIdAndIsHiddenFalseOrderByOrderIndexAsc(id);
        
        return ProblemResponse.from(problem, visibleTestCases,
                testCase -> testCaseContentService.previewInput(testCase, 20_000),
                testCase -> testCaseContentService.previewExpectedOutput(testCase, 20_000));
    }

    public ProblemManageResponse getProblemForManage(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("题目不存在: " + id));

        List<TestCase> testCases = testCaseRepository.findByProblemIdOrderByOrderIndexAsc(id);
        return ProblemManageResponse.from(problem, testCases);
    }

    @Transactional
    public ProblemResponse createProblem(CreateProblemRequest request) {
        validateVisibleSamples(request);

        Problem problem = problemRepository.save(Problem.builder()
                .title(request.getTitle().trim())
                .description(composeDescription(request))
                .status(request.getStatus() == null ? Problem.ProblemStatus.HIDDEN : request.getStatus())
                .difficulty(request.getDifficulty())
                .timeLimit(request.getTimeLimit())
                .memoryLimit(request.getMemoryLimit())
                .aiPromptDirection(normalizePromptDirection(request.getAiPromptDirection()))
                .starterCode(normalizeStarterCode(request.getStarterCode()))
                .statementBackground(normalizeOptionalText(request.getStatementBackground()))
                .statementDescription(normalizeOptionalText(request.getStatementDescription()))
                .statementInputFormat(normalizeOptionalText(request.getStatementInputFormat()))
                .statementOutputFormat(normalizeOptionalText(request.getStatementOutputFormat()))
                .statementSamples(normalizeOptionalText(request.getStatementSamples()))
                .statementHints(normalizeOptionalText(request.getStatementHints()))
                .provider(normalizePromptDirection(request.getProvider()))
                .attachments(normalizeOptionalText(request.getAttachments()))
                .tags(normalizeList(request.getTags()))
                .dataDownloadEnabled(Boolean.TRUE.equals(request.getDataDownloadEnabled()))
                .scoreDisplayMode(request.getScoreDisplayMode() == null ? Problem.ScoreDisplayMode.ICPC : request.getScoreDisplayMode())
                .knowledgePoints(normalizeList(request.getKnowledgePoints()))
                .algorithmStrategies(normalizeList(request.getAlgorithmStrategies()))
                .commonMistakes(normalizeList(request.getCommonMistakes()))
                .boundaryTypes(normalizeList(request.getBoundaryTypes()))
                .build());

        List<TestCase> savedTestCases = saveTestCases(problem.getId(), request);

        List<TestCase> visibleTestCases = savedTestCases.stream()
                .filter(testCase -> !Boolean.TRUE.equals(testCase.getIsHidden()))
                .toList();

        return ProblemResponse.from(problem, visibleTestCases,
                testCase -> testCaseContentService.previewInput(testCase, 20_000),
                testCase -> testCaseContentService.previewExpectedOutput(testCase, 20_000));
    }

    @Transactional
    public ProblemResponse updateProblem(Long problemId, CreateProblemRequest request) {
        validateVisibleSamples(request);

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("题目不存在: " + problemId));

        problem.setTitle(request.getTitle().trim());
        problem.setDescription(composeDescription(request));
        problem.setStatus(request.getStatus() == null ? Problem.ProblemStatus.HIDDEN : request.getStatus());
        problem.setDifficulty(request.getDifficulty());
        problem.setTimeLimit(request.getTimeLimit());
        problem.setMemoryLimit(request.getMemoryLimit());
        problem.setAiPromptDirection(normalizePromptDirection(request.getAiPromptDirection()));
        problem.setStarterCode(normalizeStarterCode(request.getStarterCode()));
        problem.setStatementBackground(normalizeOptionalText(request.getStatementBackground()));
        problem.setStatementDescription(normalizeOptionalText(request.getStatementDescription()));
        problem.setStatementInputFormat(normalizeOptionalText(request.getStatementInputFormat()));
        problem.setStatementOutputFormat(normalizeOptionalText(request.getStatementOutputFormat()));
        problem.setStatementSamples(normalizeOptionalText(request.getStatementSamples()));
        problem.setStatementHints(normalizeOptionalText(request.getStatementHints()));
        problem.setProvider(normalizePromptDirection(request.getProvider()));
        problem.setAttachments(normalizeOptionalText(request.getAttachments()));
        problem.setTags(normalizeList(request.getTags()));
        problem.setDataDownloadEnabled(Boolean.TRUE.equals(request.getDataDownloadEnabled()));
        problem.setScoreDisplayMode(request.getScoreDisplayMode() == null ? Problem.ScoreDisplayMode.ICPC : request.getScoreDisplayMode());
        problem.setKnowledgePoints(normalizeList(request.getKnowledgePoints()));
        problem.setAlgorithmStrategies(normalizeList(request.getAlgorithmStrategies()));
        problem.setCommonMistakes(normalizeList(request.getCommonMistakes()));
        problem.setBoundaryTypes(normalizeList(request.getBoundaryTypes()));

        Problem savedProblem = problemRepository.save(problem);
        testCaseRepository.deleteByProblemId(problemId);
        List<TestCase> savedTestCases = saveTestCases(problemId, request);

        List<TestCase> visibleTestCases = savedTestCases.stream()
                .filter(testCase -> !Boolean.TRUE.equals(testCase.getIsHidden()))
                .toList();

        return ProblemResponse.from(savedProblem, visibleTestCases,
                testCase -> testCaseContentService.previewInput(testCase, 20_000),
                testCase -> testCaseContentService.previewExpectedOutput(testCase, 20_000));
    }

    @Transactional
    public Problem deleteProblem(Long problemId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("题目不存在: " + problemId));

        List<Long> submissionIds = submissionRepository.findByProblemIdOrderBySubmittedAtDesc(problemId)
                .stream()
                .map(Submission::getId)
                .toList();

        if (!submissionIds.isEmpty()) {
            submissionAnalysisRepository.deleteBySubmissionIdIn(submissionIds);
            submissionCaseResultRepository.deleteBySubmissionIdIn(submissionIds);
        }

        submissionRepository.deleteByProblemId(problemId);
        testCaseRepository.deleteByProblemId(problemId);
        problemRepository.delete(problem);
        return problem;
    }

    private void validateVisibleSamples(CreateProblemRequest request) {
        long visibleSamples = request.getTestCases().stream()
                .filter(testCase -> !Boolean.TRUE.equals(testCase.getHidden()))
                .count();

        if (visibleSamples == 0) {
            throw new IllegalArgumentException("至少需要一个可见样例测试点");
        }
    }

    private List<TestCase> saveTestCases(Long problemId, CreateProblemRequest request) {
        return IntStream.range(0, request.getTestCases().size())
                .mapToObj(index -> {
                    CreateProblemRequest.TestCaseRequest testCase = request.getTestCases().get(index);
                    TestCase.StorageType inputStorageType = parseStorageType(testCase.getInputStorageType());
                    TestCase.StorageType outputStorageType = parseStorageType(testCase.getOutputStorageType());
                    return TestCase.builder()
                            .problemId(problemId)
                            .input(testCase.getInput() == null ? "" : testCase.getInput())
                            .expectedOutput(testCase.getExpectedOutput() == null ? "" : testCase.getExpectedOutput())
                            .inputStorageType(inputStorageType)
                            .outputStorageType(outputStorageType)
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
                            .subtaskIndex(testCase.getSubtaskIndex() == null ? 0 : testCase.getSubtaskIndex())
                            .score(testCase.getScore() == null ? 0 : testCase.getScore())
                            .publicExample(Boolean.TRUE.equals(testCase.getPublicExample()))
                            .importBatchId(testCase.getImportBatchId())
                            .isHidden(Boolean.TRUE.equals(testCase.getHidden()))
                            .orderIndex(index)
                            .build();
                })
                .map(testCaseRepository::save)
                .toList();
    }

    private String extractSummary(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "查看题目说明、样例和评测限制。";
        }

        String summary = CODE_BLOCK_PATTERN.matcher(markdown).replaceAll(" ");
        summary = MARKDOWN_PATTERN.matcher(summary).replaceAll(" ");
        summary = summary.replaceAll("\\s+", " ").trim();

        if (summary.isBlank()) {
            return "查看题目说明、样例和评测限制。";
        }

        return summary.length() > 96 ? summary.substring(0, 96) + "..." : summary;
    }

    private String normalizePromptDirection(String promptDirection) {
        if (promptDirection == null) {
            return null;
        }

        String trimmed = promptDirection.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n').strip();
        return normalized.isBlank() ? null : normalized;
    }

    private String composeDescription(CreateProblemRequest request) {
        List<String> sections = List.of(
                section("## 题目背景", request.getStatementBackground(), false),
                section("## 题目描述", firstNonBlank(request.getStatementDescription(), request.getDescription()), true),
                section("## 输入格式", request.getStatementInputFormat(), false),
                section("## 输出格式", request.getStatementOutputFormat(), false),
                section("## 样例", request.getStatementSamples(), false),
                section("## 提示说明", request.getStatementHints(), false)
        ).stream().filter(value -> !value.isBlank()).toList();

        if (!sections.isEmpty()) {
            return String.join("\n\n", sections).trim();
        }
        return request.getDescription() == null ? "" : request.getDescription().trim();
    }

    private String section(String heading, String value, boolean allowBareFallback) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return "";
        }
        if (allowBareFallback && normalized.startsWith("#")) {
            return normalized;
        }
        return heading + "\n\n" + normalized;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private TestCase.StorageType parseStorageType(String value) {
        if (value == null || value.isBlank()) {
            return TestCase.StorageType.INLINE;
        }
        try {
            return TestCase.StorageType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return TestCase.StorageType.INLINE;
        }
    }

    private String normalizeStarterCode(String starterCode) {
        if (starterCode == null) {
            return null;
        }

        String normalized = starterCode.replace("\r\n", "\n").replace('\r', '\n').stripTrailing();
        return normalized.isBlank() ? null : normalized + "\n";
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .limit(12)
                .toList();
    }
}

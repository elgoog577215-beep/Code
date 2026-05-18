package com.onlinejudge.submission.application;

import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionCaseResult;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleSignalAnalyzer {

    private static final Pattern LOOP_PATTERN = Pattern.compile("\\b(for|while)\\b");
    private static final Pattern PYTHON_RANGE_BOUNDARY_PATTERN = Pattern.compile("\\brange\\s*\\([^\\)]*[-+]\\s*1");
    private static final Pattern INDEX_PATTERN = Pattern.compile("\\[[^\\]]*(?:\\+\\s*1|-\\s*1|i\\s*\\+|i\\s*-)[^\\]]*]");
    private static final Pattern NESTED_LOOP_PATTERN = Pattern.compile("(?s)\\b(for|while)\\b.*\\{.*\\b(for|while)\\b|\\bfor\\b.*:\\s*\\R\\s+\\bfor\\b");
    private static final Pattern OUTPUT_PRINT_PATTERN = Pattern.compile("\\b(print|printf|cout|System\\.out\\.print)");
    private static final Pattern INIT_PATTERN = Pattern.compile("\\b(int|long|double|float)\\s+\\w+\\s*;|\\b\\w+\\s*=\\s*0\\b");
    private static final Pattern INPUT_PARSING_PATTERN = Pattern.compile("\\b(input\\s*\\(|split\\s*\\(|Scanner\\b|cin\\b|scanf\\s*\\()");
    private static final Pattern STATE_RESET_PATTERN = Pattern.compile("(?s)\\b(for|while)\\b.*(?:\\b(clear\\s*\\(|reset)\\b|=\\s*0\\b|=\\s*\\[\\]|=\\s*\\{\\})");
    private static final Pattern DUPLICATE_SENSITIVE_PATTERN = Pattern.compile("\\b(set\\s*\\(|HashSet|Set<|distinct|unique\\s*\\()");
    private static final Pattern LARGE_ALLOCATION_PATTERN = Pattern.compile("(new\\s+\\w+\\s*\\[[^\\]]*n|\\[[^\\]]*]\\s*\\*\\s*n|vector<[^>]+>\\s*\\([^)]*n|ArrayList\\s*<)");

    public RuleSignalResult analyze(DiagnosisEvidencePackage evidencePackage) {
        String sourceCode = Optional.ofNullable(evidencePackage)
                .map(DiagnosisEvidencePackage::getSubmission)
                .map(DiagnosisEvidencePackage.SubmissionEvidence::getSourceCode)
                .orElse("");
        String verdict = Optional.ofNullable(evidencePackage)
                .map(DiagnosisEvidencePackage::getSubmission)
                .map(DiagnosisEvidencePackage.SubmissionEvidence::getVerdict)
                .orElse("UNKNOWN");
        DiagnosisEvidencePackage.JudgeFacts judgeFacts = evidencePackage == null ? null : evidencePackage.getJudgeFacts();

        List<Signal> signals = new ArrayList<>();
        Set<String> coarseTags = new LinkedHashSet<>();
        Set<String> fineTags = new LinkedHashSet<>();
        Set<String> evidenceRefs = new LinkedHashSet<>();

        addVerdictSignals(verdict, judgeFacts, coarseTags, fineTags, evidenceRefs, signals);
        addCodeShapeSignals(sourceCode, coarseTags, fineTags, evidenceRefs, signals);
        addOutputSignals(sourceCode, judgeFacts, coarseTags, fineTags, evidenceRefs, signals);
        addFailurePatternSignals(judgeFacts, coarseTags, fineTags, evidenceRefs, signals);
        addInputAndStateSignals(sourceCode, coarseTags, fineTags, evidenceRefs, signals);

        return RuleSignalResult.builder()
                .signals(signals)
                .candidateIssueTags(new ArrayList<>(coarseTags))
                .candidateFineGrainedTags(new ArrayList<>(fineTags))
                .evidenceRefs(new ArrayList<>(evidenceRefs))
                .build();
    }

    public RuleSignalResult analyze(Submission submission, List<SubmissionCaseResult> caseResults) {
        DiagnosisEvidencePackage evidencePackage = DiagnosisEvidencePackage.builder()
                .submission(DiagnosisEvidencePackage.SubmissionEvidence.builder()
                        .verdict(submission == null || submission.getVerdict() == null ? "UNKNOWN" : submission.getVerdict().name())
                        .sourceCode(submission == null ? "" : Optional.ofNullable(submission.getSourceCode()).orElse(""))
                        .build())
                .judgeFacts(DiagnosisEvidencePackage.JudgeFacts.builder()
                        .caseResultsSummary(caseResults == null ? List.of() : caseResults.stream()
                                .map(result -> DiagnosisEvidencePackage.CaseSummary.builder()
                                        .testCaseNumber(result.getTestCaseNumber())
                                        .passed(Boolean.TRUE.equals(result.getPassed()))
                                        .hidden(Boolean.TRUE.equals(result.getHidden()))
                                        .actualOutputPreview(result.getActualOutput())
                                        .expectedOutputPreview(result.getExpectedOutput())
                                        .build())
                                .toList())
                        .hiddenFailureObserved(caseResults != null && caseResults.stream()
                                .anyMatch(result -> !Boolean.TRUE.equals(result.getPassed()) && Boolean.TRUE.equals(result.getHidden())))
                        .totalCount(caseResults == null ? 0 : caseResults.size())
                        .passedCount(caseResults == null ? 0 : (int) caseResults.stream().filter(result -> Boolean.TRUE.equals(result.getPassed())).count())
                        .build())
                .build();
        return analyze(evidencePackage);
    }

    private void addVerdictSignals(String verdict,
                                   DiagnosisEvidencePackage.JudgeFacts judgeFacts,
                                   Set<String> coarseTags,
                                   Set<String> fineTags,
                                   Set<String> evidenceRefs,
                                   List<Signal> signals) {
        switch (verdict) {
            case "COMPILATION_ERROR" -> {
                add(coarseTags, fineTags, evidenceRefs, signals,
                        "verdict:compilation_error", "SYNTAX_ERROR", null, 0.9,
                        "评测结果是编译错误，语法或环境问题优先。");
            }
            case "RUNTIME_ERROR" -> {
                add(coarseTags, fineTags, evidenceRefs, signals,
                        "verdict:runtime_error", "RUNTIME_STABILITY", null, 0.86,
                        "评测结果是运行错误，先排查越界、空值、除零或递归出口。");
            }
            case "TIME_LIMIT_EXCEEDED" -> {
                add(coarseTags, fineTags, evidenceRefs, signals,
                        "verdict:tle", "TIME_COMPLEXITY", "BRUTE_FORCE_LIMIT", 0.82,
                        "评测结果是超时，复杂度或重复计算是优先方向。");
            }
            case "MEMORY_LIMIT_EXCEEDED" -> {
                add(coarseTags, fineTags, evidenceRefs, signals,
                        "verdict:mle", "SPACE_COMPLEXITY", null, 0.82,
                        "评测结果是超内存，数据结构或缓存策略需要检查。");
            }
            case "WRONG_ANSWER" -> {
                add(coarseTags, fineTags, evidenceRefs, signals,
                        "verdict:wrong_answer", "BOUNDARY_CONDITION", null, 0.62,
                        "评测结果是答案错误，需要结合失败模式继续判断。");
            }
            case "ACCEPTED" -> {
                add(coarseTags, fineTags, evidenceRefs, signals,
                        "verdict:accepted", "GENERALIZATION_CHECK", null, 0.68,
                        "本次已通过，适合进入复盘和泛化检查。");
            }
            default -> {
                add(coarseTags, fineTags, evidenceRefs, signals,
                        "verdict:unknown", "NEEDS_MORE_EVIDENCE", null, 0.4,
                        "评测结果不足，需要更多证据。");
            }
        }

        if (judgeFacts != null && Boolean.TRUE.equals(judgeFacts.getHiddenFailureObserved())) {
            add(coarseTags, fineTags, evidenceRefs, signals,
                    "judge:hidden_failure", "BOUNDARY_CONDITION", "SAMPLE_OVERFIT", 0.72,
                    "失败发生在隐藏测试点，更像是样例外边界或泛化不足。");
        }
    }

    private void addCodeShapeSignals(String sourceCode,
                                     Set<String> coarseTags,
                                     Set<String> fineTags,
                                     Set<String> evidenceRefs,
                                     List<Signal> signals) {
        int loopCount = countMatches(sourceCode, LOOP_PATTERN);
        if (loopCount > 0) {
            add(coarseTags, fineTags, evidenceRefs, signals,
                    "code:loop_present", "LOOP_BOUNDARY", null, 0.52,
                    "代码包含循环，边界和循环变量变化值得检查。");
        }
        if (PYTHON_RANGE_BOUNDARY_PATTERN.matcher(sourceCode).find() || INDEX_PATTERN.matcher(sourceCode).find()) {
            add(coarseTags, fineTags, evidenceRefs, signals,
                    "code:plus_minus_one", "LOOP_BOUNDARY", "OFF_BY_ONE", 0.7,
                    "代码中出现加减 1 的循环或索引表达式，存在差一位风险。");
        }
        if (loopCount >= 2 || NESTED_LOOP_PATTERN.matcher(sourceCode).find()) {
            add(coarseTags, fineTags, evidenceRefs, signals,
                    "code:nested_loop", "TIME_COMPLEXITY", "BRUTE_FORCE_LIMIT", 0.72,
                    "代码包含多层循环，输入规模较大时可能触发复杂度问题。");
        }
        if (INIT_PATTERN.matcher(sourceCode).find()) {
            add(coarseTags, fineTags, evidenceRefs, signals,
                    "code:init_or_state", "VARIABLE_INITIALIZATION", "INITIAL_STATE", 0.45,
                    "代码存在显式初值或未初始化变量形态，状态初始值需要核对。");
        }
        if (DUPLICATE_SENSITIVE_PATTERN.matcher(sourceCode).find()) {
            add(coarseTags, fineTags, evidenceRefs, signals,
                    "code:dedupe_structure", "BOUNDARY_CONDITION", "DUPLICATE_CASE", 0.48,
                    "代码使用去重结构或去重操作，重复元素场景需要额外验证。");
        }
        if (LARGE_ALLOCATION_PATTERN.matcher(sourceCode).find()) {
            add(coarseTags, fineTags, evidenceRefs, signals,
                    "code:large_allocation", "SPACE_COMPLEXITY", "MAX_BOUNDARY", 0.58,
                    "代码存在随 n 增长的较大存储结构，最大规模边界需要关注。");
        }
    }

    private void addOutputSignals(String sourceCode,
                                  DiagnosisEvidencePackage.JudgeFacts judgeFacts,
                                  Set<String> coarseTags,
                                  Set<String> fineTags,
                                  Set<String> evidenceRefs,
                                  List<Signal> signals) {
        if (!OUTPUT_PRINT_PATTERN.matcher(sourceCode).find()) {
            return;
        }
        if (judgeFacts == null || judgeFacts.getCaseResultsSummary() == null) {
            return;
        }
        boolean visibleOutputMismatch = judgeFacts.getCaseResultsSummary().stream()
                .anyMatch(result -> !Boolean.TRUE.equals(result.getPassed())
                        && !Boolean.TRUE.equals(result.getHidden())
                        && differsOnlyByWhitespace(result.getActualOutputPreview(), result.getExpectedOutputPreview()));
        if (visibleOutputMismatch) {
            add(coarseTags, fineTags, evidenceRefs, signals,
                    "judge:whitespace_mismatch", "IO_FORMAT", "OUTPUT_FORMAT_DETAIL", 0.88,
                    "可见失败样例只存在空白差异，优先检查换行、空格和输出格式。");
        }
    }

    private void addFailurePatternSignals(DiagnosisEvidencePackage.JudgeFacts judgeFacts,
                                          Set<String> coarseTags,
                                          Set<String> fineTags,
                                          Set<String> evidenceRefs,
                                          List<Signal> signals) {
        if (judgeFacts == null || judgeFacts.getTotalCount() == null || judgeFacts.getTotalCount() == 0) {
            return;
        }
        int passed = judgeFacts.getPassedCount() == null ? 0 : judgeFacts.getPassedCount();
        int total = judgeFacts.getTotalCount();
        if (passed > 0 && passed < total && Boolean.TRUE.equals(judgeFacts.getHiddenFailureObserved())) {
            add(coarseTags, fineTags, evidenceRefs, signals,
                    "judge:sample_pass_hidden_fail", "SAMPLE_ONLY", "SAMPLE_OVERFIT", 0.74,
                    "已有测试通过但隐藏测试失败，可能只覆盖了样例或常规路径。");
        }
    }

    private void addInputAndStateSignals(String sourceCode,
                                         Set<String> coarseTags,
                                         Set<String> fineTags,
                                         Set<String> evidenceRefs,
                                         List<Signal> signals) {
        if (INPUT_PARSING_PATTERN.matcher(sourceCode).find()) {
            add(coarseTags, fineTags, evidenceRefs, signals,
                    "code:input_parsing", "IO_FORMAT", "INPUT_PARSING", 0.44,
                    "代码包含输入解析逻辑，题面输入格式和读取方式需要核对。");
        }
        if (STATE_RESET_PATTERN.matcher(sourceCode).find()) {
            add(coarseTags, fineTags, evidenceRefs, signals,
                    "code:state_reset", "VARIABLE_INITIALIZATION", "STATE_RESET", 0.5,
                    "代码在循环中维护状态，多组数据或多轮处理时需要确认是否正确重置。");
        }
    }

    private void add(Set<String> coarseTags,
                     Set<String> fineTags,
                     Set<String> evidenceRefs,
                     List<Signal> signals,
                     String evidenceRef,
                     String coarseTag,
                     String fineTag,
                     double confidence,
                     String message) {
        if (coarseTag != null && !coarseTag.isBlank()) {
            coarseTags.add(coarseTag);
        }
        if (fineTag != null && !fineTag.isBlank()) {
            fineTags.add(fineTag);
        }
        evidenceRefs.add(evidenceRef);
        signals.add(Signal.builder()
                .evidenceRef(evidenceRef)
                .coarseTag(coarseTag)
                .fineTag(fineTag)
                .confidence(confidence)
                .message(message)
                .build());
    }

    private int countMatches(String input, Pattern pattern) {
        if (input == null || input.isBlank()) {
            return 0;
        }
        int count = 0;
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private boolean differsOnlyByWhitespace(String actual, String expected) {
        String safeActual = Optional.ofNullable(actual).orElse("");
        String safeExpected = Optional.ofNullable(expected).orElse("");
        if (safeActual.equals(safeExpected)) {
            return false;
        }
        return safeActual.replaceAll("\\s+", "").toLowerCase(Locale.ROOT)
                .equals(safeExpected.replaceAll("\\s+", "").toLowerCase(Locale.ROOT));
    }

    @Data
    @Builder
    public static class RuleSignalResult {
        private List<Signal> signals;
        private List<String> candidateIssueTags;
        private List<String> candidateFineGrainedTags;
        private List<String> evidenceRefs;
    }

    @Data
    @Builder
    public static class Signal {
        private String evidenceRef;
        private String coarseTag;
        private String fineTag;
        private Double confidence;
        private String message;
    }
}

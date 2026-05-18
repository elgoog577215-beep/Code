package com.onlinejudge.submission.application;

import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionCaseResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleSignalAnalyzerTest {

    private final RuleSignalAnalyzer analyzer = new RuleSignalAnalyzer();

    @Test
    void detectsWhitespaceOnlyOutputFormatMismatch() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        a = int(input())
                        print(a)
                        """)
                .build();
        List<SubmissionCaseResult> caseResults = List.of(SubmissionCaseResult.builder()
                .testCaseNumber(1)
                .passed(false)
                .hidden(false)
                .actualOutput("42 ")
                .expectedOutput("42")
                .build());

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, caseResults);

        assertThat(result.getCandidateIssueTags()).contains("IO_FORMAT");
        assertThat(result.getCandidateFineGrainedTags()).contains("OUTPUT_FORMAT_DETAIL");
        assertThat(result.getEvidenceRefs()).contains("judge:whitespace_mismatch");
    }

    @Test
    void detectsOffByOneRiskFromRangeBoundary() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        n = int(input())
                        arr = list(map(int, input().split()))
                        total = 0
                        for i in range(n - 1):
                            total += arr[i]
                        print(total)
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateIssueTags()).contains("LOOP_BOUNDARY");
        assertThat(result.getCandidateFineGrainedTags()).contains("OFF_BY_ONE");
        assertThat(result.getEvidenceRefs()).contains("code:plus_minus_one");
    }

    @Test
    void detectsHiddenFailureAsSampleOverfitRisk() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        n = int(input())
                        print(n)
                        """)
                .build();
        List<SubmissionCaseResult> caseResults = List.of(
                SubmissionCaseResult.builder()
                        .testCaseNumber(1)
                        .passed(true)
                        .hidden(false)
                        .build(),
                SubmissionCaseResult.builder()
                        .testCaseNumber(2)
                        .passed(false)
                        .hidden(true)
                        .build()
        );

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, caseResults);

        assertThat(result.getCandidateIssueTags()).contains("SAMPLE_ONLY");
        assertThat(result.getCandidateFineGrainedTags()).contains("SAMPLE_OVERFIT");
        assertThat(result.getEvidenceRefs()).contains("judge:sample_pass_hidden_fail");
    }

    @Test
    void detectsBruteForceRiskFromNestedLoops() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.TIME_LIMIT_EXCEEDED)
                .sourceCode("""
                        n = int(input())
                        arr = list(map(int, input().split()))
                        answer = 0
                        for i in range(n):
                            for j in range(n):
                                if arr[i] < arr[j]:
                                    answer += 1
                        print(answer)
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateIssueTags()).contains("TIME_COMPLEXITY");
        assertThat(result.getCandidateFineGrainedTags()).contains("BRUTE_FORCE_LIMIT");
        assertThat(result.getEvidenceRefs()).contains("code:nested_loop");
    }

    @Test
    void detectsRuntimeErrorAsStabilityRisk() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.RUNTIME_ERROR)
                .sourceCode("""
                        nums = []
                        print(nums[0])
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateIssueTags()).contains("RUNTIME_STABILITY");
        assertThat(result.getEvidenceRefs()).contains("verdict:runtime_error");
    }

    @Test
    void detectsAcceptedSubmissionAsGeneralizationCheck() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.ACCEPTED)
                .sourceCode("""
                        print(sum(map(int, input().split())))
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateIssueTags()).contains("GENERALIZATION_CHECK");
        assertThat(result.getEvidenceRefs()).contains("verdict:accepted");
    }

    @Test
    void detectsInputParsingSignal() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        n = int(input())
                        for _ in range(n):
                            a, b = input().split()
                            print(a + b)
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateIssueTags()).contains("IO_FORMAT");
        assertThat(result.getCandidateFineGrainedTags()).contains("INPUT_PARSING");
        assertThat(result.getEvidenceRefs()).contains("code:input_parsing");
    }

    @Test
    void detectsStateResetSignal() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        total = 0
                        for i in range(10):
                            total = 0
                            total += i
                        print(total)
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateIssueTags()).contains("VARIABLE_INITIALIZATION");
        assertThat(result.getCandidateFineGrainedTags()).contains("STATE_RESET");
        assertThat(result.getEvidenceRefs()).contains("code:state_reset");
    }

    @Test
    void detectsDuplicateCaseSignal() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        nums = list(map(int, input().split()))
                        print(len(set(nums)))
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateFineGrainedTags()).contains("DUPLICATE_CASE");
        assertThat(result.getEvidenceRefs()).contains("code:dedupe_structure");
    }

    @Test
    void detectsMaxBoundarySignalFromLargeAllocation() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.MEMORY_LIMIT_EXCEEDED)
                .sourceCode("""
                        n = int(input())
                        dp = [0] * n
                        print(dp[-1])
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateIssueTags()).contains("SPACE_COMPLEXITY");
        assertThat(result.getCandidateFineGrainedTags()).contains("MAX_BOUNDARY");
        assertThat(result.getEvidenceRefs()).contains("code:large_allocation");
    }

    @Test
    void detectsCompilationErrorAsSyntaxIssue() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.COMPILATION_ERROR)
                .sourceCode("""
                        int main() {
                            cout << 1
                        }
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateIssueTags()).contains("SYNTAX_ERROR");
        assertThat(result.getEvidenceRefs()).contains("verdict:compilation_error");
    }

    @Test
    void detectsUnknownVerdictAsNeedsMoreEvidence() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.PENDING)
                .sourceCode("print(1)")
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateIssueTags()).contains("NEEDS_MORE_EVIDENCE");
        assertThat(result.getEvidenceRefs()).contains("verdict:unknown");
    }

    @Test
    void detectsTimeLimitFromVerdictWithoutNestedLoop() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.TIME_LIMIT_EXCEEDED)
                .sourceCode("""
                        n = int(input())
                        while True:
                            n += 1
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateIssueTags()).contains("TIME_COMPLEXITY");
        assertThat(result.getCandidateFineGrainedTags()).contains("BRUTE_FORCE_LIMIT");
        assertThat(result.getEvidenceRefs()).contains("verdict:tle");
    }

    @Test
    void detectsMemoryLimitFromVerdictWithoutAllocationPattern() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.MEMORY_LIMIT_EXCEEDED)
                .sourceCode("""
                        print("memory")
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateIssueTags()).contains("SPACE_COMPLEXITY");
        assertThat(result.getEvidenceRefs()).contains("verdict:mle");
    }

    @Test
    void detectsInitialStateSignalFromExplicitZeroInitialization() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        answer = 0
                        for i in range(3):
                            answer += i
                        print(answer)
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateIssueTags()).contains("VARIABLE_INITIALIZATION");
        assertThat(result.getCandidateFineGrainedTags()).contains("INITIAL_STATE");
        assertThat(result.getEvidenceRefs()).contains("code:init_or_state");
    }

    @Test
    void ignoresVisibleContentMismatchAsWhitespaceOnlySignal() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        print("42")
                        """)
                .build();
        List<SubmissionCaseResult> caseResults = List.of(SubmissionCaseResult.builder()
                .testCaseNumber(1)
                .passed(false)
                .hidden(false)
                .actualOutput("41")
                .expectedOutput("42")
                .build());

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, caseResults);

        assertThat(result.getCandidateIssueTags()).contains("BOUNDARY_CONDITION");
        assertThat(result.getCandidateIssueTags()).doesNotContain("IO_FORMAT");
        assertThat(result.getEvidenceRefs()).doesNotContain("judge:whitespace_mismatch");
    }
}

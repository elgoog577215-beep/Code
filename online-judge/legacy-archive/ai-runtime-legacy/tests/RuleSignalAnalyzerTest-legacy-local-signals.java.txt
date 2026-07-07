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
    void doesNotPromoteHiddenFailureAsSampleOverfitForTle() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.TIME_LIMIT_EXCEEDED)
                .sourceCode("""
                        n = int(input())
                        for i in range(n):
                            print(i)
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

        assertThat(result.getCandidateIssueTags()).contains("TIME_COMPLEXITY");
        assertThat(result.getCandidateIssueTags()).doesNotContain("SAMPLE_ONLY");
        assertThat(result.getCandidateFineGrainedTags()).doesNotContain("SAMPLE_OVERFIT");
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
    void detectsBruteForceRiskFromRepeatedScanInsideLoop() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.TIME_LIMIT_EXCEEDED)
                .sourceCode("""
                        n = int(input())
                        a = list(map(int, input().split()))
                        seen = []
                        for x in a:
                            if x not in seen:
                                seen.append(x)
                                print(x, a.count(x))
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateIssueTags()).contains("TIME_COMPLEXITY");
        assertThat(result.getCandidateFineGrainedTags()).contains("BRUTE_FORCE_LIMIT");
        assertThat(result.getEvidenceRefs()).contains("code:repeated_scan_in_loop");
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
        assertThat(result.getCandidateIssueTags()).doesNotContain("IO_FORMAT", "LOOP_BOUNDARY", "TIME_COMPLEXITY");
        assertThat(result.getCandidateFineGrainedTags()).isEmpty();
    }

    @Test
    void observesInputParsingWithoutPromotingItToIssueTag() {
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

        assertThat(result.getEvidenceRefs()).contains("code:input_parsing_observed");
        assertThat(result.getCandidateIssueTags()).doesNotContain("IO_FORMAT");
        assertThat(result.getCandidateFineGrainedTags()).doesNotContain("INPUT_PARSING");
    }

    @Test
    void detectsStateResetSignal() {
        DiagnosisEvidencePackage evidencePackage = evidencePackage(
                "第一行输入 T，每组输入一个序列，输出每组结果。",
                Submission.Verdict.WRONG_ANSWER,
                """
                        best = 0
                        cur = 0
                        for _ in range(T):
                            s = input()
                            for ch in s:
                                if ch == "1":
                                    cur += 1
                                    best = max(best, cur)
                            print(best)
                        """
        );

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(evidencePackage);

        assertThat(result.getCandidateIssueTags()).contains("VARIABLE_INITIALIZATION");
        assertThat(result.getCandidateFineGrainedTags()).contains("STATE_RESET");
        assertThat(result.getEvidenceRefs()).contains("problem:multi_case_state_before_loop");
    }

    @Test
    void doesNotPromoteOrdinaryLoopAssignmentsToStateReset() {
        Submission submission = Submission.builder()
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        int answer = 0;
                        for (int i = 0; i < n; i++) {
                            int local = 0;
                            answer += local + i;
                        }
                        System.out.println(answer);
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateFineGrainedTags()).doesNotContain("STATE_RESET");
        assertThat(result.getEvidenceRefs()).doesNotContain("code:state_reset");
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
    void detectsSpaceComplexitySignalFromLargeAllocationWithoutMaxBoundaryFineTag() {
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
        assertThat(result.getCandidateFineGrainedTags()).doesNotContain("MAX_BOUNDARY");
        assertThat(result.getEvidenceRefs()).contains("code:large_allocation");
    }

    @Test
    void detectsMatrixAllocationAsSpaceComplexityRisk() {
        DiagnosisEvidencePackage evidencePackage = evidencePackage(
                "Input n, max n is 200000, output a value for an n by n board.",
                Submission.Verdict.MEMORY_LIMIT_EXCEEDED,
                """
                        n = int(input())
                        grid = [[0] * n for _ in range(n)]
                        print(n)
                        """
        );

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(evidencePackage);

        assertThat(result.getCandidateIssueTags()).contains("SPACE_COMPLEXITY");
        assertThat(result.getEvidenceRefs()).contains("code:matrix_allocation");
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
    void detectsInclusiveRangeProblemWhenSourceStopsBeforeN() {
        DiagnosisEvidencePackage evidencePackage = evidencePackage(
                "输入正整数 n，输出从 1 到 n 的整数和。",
                Submission.Verdict.WRONG_ANSWER,
                """
                        n = int(input())
                        total = 0
                        for x in range(1, n):
                            total += x
                        print(total)
                        """
        );

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(evidencePackage);

        assertThat(result.getCandidateIssueTags()).contains("LOOP_BOUNDARY");
        assertThat(result.getCandidateFineGrainedTags()).contains("OFF_BY_ONE");
        assertThat(result.getEvidenceRefs()).contains("problem:inclusive_range_source_excludes_n");
    }

    @Test
    void detectsDpStateDesignFromProblemAndSource() {
        DiagnosisEvidencePackage evidencePackage = evidencePackage(
                "给定 n 个数，选择若干个数使得任意两个被选位置不相邻，输出最大和。",
                Submission.Verdict.WRONG_ANSWER,
                """
                        n = int(input())
                        a = list(map(int, input().split()))
                        dp = [0] * n
                        for i in range(1, n):
                            dp[i] = max(dp[i - 1], a[i])
                        print(dp[n - 1])
                        """
        );

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(evidencePackage);

        assertThat(result.getCandidateIssueTags()).contains("ALGORITHM_STRATEGY");
        assertThat(result.getCandidateFineGrainedTags()).contains("DP_STATE_DESIGN");
        assertThat(result.getCandidateFineGrainedTags()).doesNotContain("OFF_BY_ONE");
        assertThat(result.getEvidenceRefs()).contains("problem:dp_state_needs_design");
    }

    @Test
    void detectsGreedyAssumptionFromUntrustedCoinProblem() {
        DiagnosisEvidencePackage evidencePackage = evidencePackage(
                "给定若干硬币面值和目标金额，输出最少硬币数。面值不保证满足贪心性质。",
                Submission.Verdict.WRONG_ANSWER,
                """
                        n, target = map(int, input().split())
                        coins = sorted(map(int, input().split()), reverse=True)
                        for coin in coins:
                            target %= coin
                        print(target)
                        """
        );

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(evidencePackage);

        assertThat(result.getCandidateIssueTags()).contains("ALGORITHM_STRATEGY");
        assertThat(result.getCandidateFineGrainedTags()).contains("GREEDY_ASSUMPTION");
        assertThat(result.getEvidenceRefs()).contains("problem:greedy_assumption_unproven");
    }

    @Test
    void detectsEmptyInputFromProblemAndMissingGuard() {
        DiagnosisEvidencePackage evidencePackage = evidencePackage(
                "输入 n 和 n 个整数；当 n=0 时输出 EMPTY，否则输出最大值。",
                Submission.Verdict.RUNTIME_ERROR,
                """
                        n = int(input())
                        a = list(map(int, input().split()))
                        print(max(a))
                        """
        );

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(evidencePackage);

        assertThat(result.getCandidateIssueTags()).contains("BOUNDARY_CONDITION");
        assertThat(result.getCandidateFineGrainedTags()).contains("EMPTY_INPUT");
        assertThat(result.getEvidenceRefs()).contains("problem:empty_input_missing_guard");
    }

    @Test
    void detectsOverSimulationFromLargeBoundAndStepLoop() {
        DiagnosisEvidencePackage evidencePackage = evidencePackage(
                "给定 a 和 b，输出从 a 变到 b 至少需要多少次加一操作。0 <= a <= b <= 10^12。",
                Submission.Verdict.TIME_LIMIT_EXCEEDED,
                """
                        a, b = map(int, input().split())
                        while a < b:
                            a += 1
                        print(a)
                        """
        );

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(evidencePackage);

        assertThat(result.getCandidateIssueTags()).contains("TIME_COMPLEXITY");
        assertThat(result.getCandidateFineGrainedTags()).contains("OVER_SIMULATION");
        assertThat(result.getEvidenceRefs()).contains("problem:large_bound_step_simulation");
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
        assertThat(result.getCandidateFineGrainedTags()).doesNotContain("BRUTE_FORCE_LIMIT");
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
                        n = int(input())
                        a = list(map(int, input().split()))
                        mn = 0
                        for x in a:
                            if x < mn:
                                mn = x
                        print(mn)
                        """)
                .build();

        RuleSignalAnalyzer.RuleSignalResult result = analyzer.analyze(submission, List.of());

        assertThat(result.getCandidateIssueTags()).contains("VARIABLE_INITIALIZATION");
        assertThat(result.getCandidateFineGrainedTags()).contains("INITIAL_STATE");
        assertThat(result.getEvidenceRefs()).contains("code:init_or_state");
    }

    @Test
    void ignoresNormalAccumulatorAsInitialStateSignal() {
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

        assertThat(result.getCandidateFineGrainedTags()).doesNotContain("INITIAL_STATE");
        assertThat(result.getEvidenceRefs()).doesNotContain("code:init_or_state");
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

    private DiagnosisEvidencePackage evidencePackage(String description,
                                                     Submission.Verdict verdict,
                                                     String sourceCode) {
        return DiagnosisEvidencePackage.builder()
                .problem(DiagnosisEvidencePackage.ProblemEvidence.builder()
                        .title("规则信号测试题")
                        .description(description)
                        .build())
                .submission(DiagnosisEvidencePackage.SubmissionEvidence.builder()
                        .verdict(verdict.name())
                        .sourceCode(sourceCode)
                        .build())
                .judgeFacts(DiagnosisEvidencePackage.JudgeFacts.builder()
                        .caseResultsSummary(List.of())
                        .totalCount(0)
                        .passedCount(0)
                        .hiddenFailureObserved(false)
                        .build())
                .build();
    }
}

package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.classroom.application.HintSafetyService;
import com.onlinejudge.classroom.domain.Assignment;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticAgentServiceTest {

    @Test
    void diagnoseMergesRuleSignalsAndKeepsEvidenceRefs() {
        DiagnosticAgentService service = newService();

        Submission submission = Submission.builder()
                .id(10L)
                .languageName("Python 3")
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        n = int(input())
                        total = 0
                        for i in range(n - 1):
                            total += i
                        print(total)
                        """)
                .build();
        Problem problem = Problem.builder()
                .id(1L)
                .title("求和")
                .description("输入 n，输出 1 到 n 的和")
                .difficulty(Problem.Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(65536)
                .build();
        SubmissionAnalysisResponse baseline = SubmissionAnalysisResponse.builder()
                .submissionId(10L)
                .sourceType("RULE_BASED_V1")
                .scenario("WA")
                .issueTags(List.of("BOUNDARY_CONDITION"))
                .fineGrainedTags(List.of())
                .evidenceRefs(List.of())
                .build();

        DiagnosticAgentService.AgentResult result = service.diagnose(
                problem,
                submission,
                List.of(),
                baseline,
                Assignment.HintPolicy.L2
        );

        assertThat(result.analysis().getIssueTags()).contains("LOOP_BOUNDARY");
        assertThat(result.analysis().getFineGrainedTags()).contains("OFF_BY_ONE");
        assertThat(result.analysis().getEvidenceRefs()).contains("code:plus_minus_one");
        assertThat(result.traceSummary()).contains("diagnostic-agent-v2");
        assertThat(result.analysis().getDiagnosticTrace()).contains("model=rule-fallback");
        assertThat(result.analysis().getAiInvocation()).isNotNull();
        assertThat(result.analysis().getAiInvocation().getAgentVersion()).isEqualTo("diagnostic-agent-v2");
        assertThat(result.analysis().getAiInvocation().isFallbackUsed()).isTrue();
        assertThat(result.analysis().getStudentHintPlan()).isNotNull();
        assertThat(result.analysis().getStudentHintPlan().getHintLevel()).isEqualTo("L2");
        assertThat(result.analysis().getStudentHintPlan().getProblemType()).isEqualTo("差一位错误");
        assertThat(result.analysis().getStudentHintPlan().getTeachingAction()).isEqualTo("TRACE_VARIABLES");
        assertThat(result.analysis().getStudentHintPlan().getEvidenceRefs()).contains("code:plus_minus_one");
        assertThat(result.analysis().getLearningInterventionPlan()).isNotNull();
        assertThat(result.analysis().getLearningInterventionPlan().getInterventionType()).isEqualTo("VARIABLE_TRACE");
        assertThat(result.analysis().getLearningInterventionPlan().getStudentTask()).isNotBlank();
        assertThat(result.analysis().getLearningInterventionPlan().getCompletionSignal()).isNotBlank();
        assertThat(result.analysis().getLearningInterventionPlan().getEvidenceRefs()).contains("code:plus_minus_one");
    }

    @Test
    void diagnoseAddsPartialFixRegressionFromHistoryTransition() {
        DiagnosticAgentService service = newService();

        Submission submission = Submission.builder()
                .id(11L)
                .languageName("Python 3")
                .verdict(Submission.Verdict.RUNTIME_ERROR)
                .sourceCode("print(1 / 0)")
                .build();
        Problem problem = Problem.builder()
                .id(1L)
                .title("调试题")
                .description("调试")
                .difficulty(Problem.Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(65536)
                .build();
        SubmissionAnalysisResponse baseline = SubmissionAnalysisResponse.builder()
                .submissionId(11L)
                .sourceType("RULE_BASED_V1")
                .scenario("RE")
                .issueTags(List.of("RUNTIME_STABILITY"))
                .fineGrainedTags(List.of())
                .evidenceRefs(List.of())
                .build();
        DiagnosisEvidencePackage.HistoryEvidence history = DiagnosisEvidencePackage.HistoryEvidence.builder()
                .previousVerdict("WRONG_ANSWER")
                .transitionSignal("评测阶段从 WRONG_ANSWER 变化为 RUNTIME_ERROR")
                .recentIssueTags(List.of("BOUNDARY_CONDITION"))
                .recentFineGrainedTags(List.of("OFF_BY_ONE"))
                .build();

        DiagnosticAgentService.AgentResult result = service.diagnose(
                problem,
                submission,
                List.of(),
                baseline,
                Assignment.HintPolicy.L2,
                history
        );

        assertThat(result.analysis().getFineGrainedTags()).contains("PARTIAL_FIX_REGRESSION");
        assertThat(result.analysis().getEvidenceRefs()).contains("history:verdict_transition");
        assertThat(result.analysis().getLearningTrajectorySignal()).isNotNull();
        assertThat(result.analysis().getLearningTrajectorySignal().getPhase()).isEqualTo("REGRESSION");
        assertThat(result.analysis().getLearningInterventionPlan()).isNotNull();
        assertThat(result.analysis().getLearningInterventionPlan().getInterventionType()).isEqualTo("COMPARE_SUBMISSIONS");
    }

    @Test
    void diagnoseBuildsFixedCompilationTrajectorySignal() {
        DiagnosticAgentService service = newService();

        Submission submission = Submission.builder()
                .id(14L)
                .languageName("Python 3")
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        n = int(input())
                        print(n + 1)
                        """)
                .build();
        Problem problem = Problem.builder()
                .id(4L)
                .title("trajectory")
                .description("Read an integer and output the expected value.")
                .difficulty(Problem.Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(65536)
                .build();
        SubmissionAnalysisResponse baseline = SubmissionAnalysisResponse.builder()
                .submissionId(14L)
                .sourceType("RULE_BASED_V1")
                .scenario("WA")
                .issueTags(List.of("BOUNDARY_CONDITION"))
                .fineGrainedTags(List.of())
                .evidenceRefs(List.of())
                .build();
        DiagnosisEvidencePackage.HistoryEvidence history = DiagnosisEvidencePackage.HistoryEvidence.builder()
                .previousVerdict("COMPILATION_ERROR")
                .transitionSignal("compilation fixed, now wrong answer")
                .build();

        DiagnosticAgentService.AgentResult result = service.diagnose(
                problem,
                submission,
                List.of(),
                baseline,
                Assignment.HintPolicy.L2,
                history
        );

        assertThat(result.analysis().getLearningTrajectorySignal()).isNotNull();
        assertThat(result.analysis().getLearningTrajectorySignal().getPhase()).isEqualTo("FIXED_COMPILATION");
        assertThat(result.analysis().getLearningTrajectorySignal().isNeedsTeacherAttention()).isFalse();
        assertThat(result.analysis().getEvidenceRefs()).contains("history:fixed_compilation");
        assertThat(result.traceSummary()).contains("trajectory=FIXED_COMPILATION");
    }

    @Test
    void diagnoseBuildsRepeatedStuckTrajectorySignal() {
        DiagnosticAgentService service = newService();

        Submission submission = Submission.builder()
                .id(15L)
                .languageName("Python 3")
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        n = int(input())
                        print(n - 1)
                        """)
                .build();
        Problem problem = Problem.builder()
                .id(5L)
                .title("repeated")
                .description("Read an integer and output the expected value.")
                .difficulty(Problem.Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(65536)
                .build();
        SubmissionAnalysisResponse baseline = SubmissionAnalysisResponse.builder()
                .submissionId(15L)
                .sourceType("RULE_BASED_V1")
                .scenario("WA")
                .issueTags(List.of("BOUNDARY_CONDITION"))
                .fineGrainedTags(List.of())
                .evidenceRefs(List.of())
                .build();
        DiagnosisEvidencePackage.HistoryEvidence history = DiagnosisEvidencePackage.HistoryEvidence.builder()
                .previousVerdict("WRONG_ANSWER")
                .repeatedIssueCount(4L)
                .transitionSignal("same verdict remains")
                .build();

        DiagnosticAgentService.AgentResult result = service.diagnose(
                problem,
                submission,
                List.of(),
                baseline,
                Assignment.HintPolicy.L2,
                history
        );

        assertThat(result.analysis().getLearningTrajectorySignal()).isNotNull();
        assertThat(result.analysis().getLearningTrajectorySignal().getPhase()).isEqualTo("REPEATED_STUCK");
        assertThat(result.analysis().getLearningTrajectorySignal().isNeedsTeacherAttention()).isTrue();
        assertThat(result.analysis().getEvidenceRefs()).contains("history:repeated_stuck");
        assertThat(result.analysis().getLearningInterventionPlan()).isNotNull();
        assertThat(result.analysis().getLearningInterventionPlan().getInterventionType()).isEqualTo("MIN_CASE_TRACE");
        assertThat(result.analysis().getLearningInterventionPlan().getStudentTask()).isNotBlank();
        assertThat(result.analysis().getLearningInterventionPlan().getCompletionSignal()).isNotBlank();
    }

    @Test
    void diagnoseBuildsAcceptedAfterFixTrajectorySignal() {
        DiagnosticAgentService service = newService();

        Submission submission = Submission.builder()
                .id(16L)
                .languageName("Python 3")
                .verdict(Submission.Verdict.ACCEPTED)
                .sourceCode("print(int(input()))")
                .build();
        Problem problem = Problem.builder()
                .id(6L)
                .title("accepted")
                .description("Read an integer and output the expected value.")
                .difficulty(Problem.Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(65536)
                .build();
        SubmissionAnalysisResponse baseline = SubmissionAnalysisResponse.builder()
                .submissionId(16L)
                .sourceType("RULE_BASED_V1")
                .scenario("AC")
                .issueTags(List.of("GENERALIZATION_CHECK"))
                .fineGrainedTags(List.of())
                .evidenceRefs(List.of())
                .build();
        DiagnosisEvidencePackage.HistoryEvidence history = DiagnosisEvidencePackage.HistoryEvidence.builder()
                .previousVerdict("WRONG_ANSWER")
                .transitionSignal("wrong answer fixed to accepted")
                .build();

        DiagnosticAgentService.AgentResult result = service.diagnose(
                problem,
                submission,
                List.of(),
                baseline,
                Assignment.HintPolicy.L2,
                history
        );

        assertThat(result.analysis().getLearningTrajectorySignal()).isNotNull();
        assertThat(result.analysis().getLearningTrajectorySignal().getPhase()).isEqualTo("ACCEPTED_AFTER_FIX");
        assertThat(result.analysis().getLearningTrajectorySignal().isNeedsTeacherAttention()).isFalse();
        assertThat(result.analysis().getEvidenceRefs()).contains("history:accepted_after_fix");
        assertThat(result.analysis().getLearningInterventionPlan()).isNotNull();
        assertThat(result.analysis().getLearningInterventionPlan().getInterventionType()).isEqualTo("EXPLAIN_GENERALITY");
    }

    @Test
    void diagnoseFallsBackToRuleAwareAnalysisWhenModelStageFails() {
        DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();
        DiagnosticAgentService service = new DiagnosticAgentService(
                new DiagnosisEvidencePackageBuilder(),
                new RuleSignalAnalyzer(),
                new FailingAiReportService(),
                new PassThroughHintSafetyService(taxonomy),
                taxonomy
        );
        Submission submission = Submission.builder()
                .id(12L)
                .languageName("Python 3")
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        n = int(input())
                        for i in range(n - 1):
                            print(i)
                        """)
                .build();
        Problem problem = Problem.builder()
                .id(2L)
                .title("循环边界")
                .description("输出 1 到 n")
                .difficulty(Problem.Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(65536)
                .build();
        SubmissionAnalysisResponse baseline = SubmissionAnalysisResponse.builder()
                .submissionId(12L)
                .sourceType("RULE_BASED_V1")
                .scenario("WA")
                .issueTags(List.of())
                .fineGrainedTags(List.of())
                .evidenceRefs(List.of())
                .build();

        DiagnosticAgentService.AgentResult result = service.diagnose(
                problem,
                submission,
                List.of(),
                baseline,
                Assignment.HintPolicy.L2
        );

        assertThat(result.analysis().getIssueTags()).contains("LOOP_BOUNDARY");
        assertThat(result.analysis().getFineGrainedTags()).contains("OFF_BY_ONE");
        assertThat(result.analysis().getDiagnosticTrace()).contains("model=rule-fallback");
        assertThat(result.traceSummary()).contains("model=rule-fallback");
    }

    @Test
    void diagnoseMarksLowConfidenceAsNeedsMoreEvidence() {
        DiagnosticAgentService service = newService();
        Submission submission = Submission.builder()
                .id(13L)
                .languageName("Python 3")
                .verdict(Submission.Verdict.PENDING)
                .sourceCode("print(1)")
                .build();
        Problem problem = Problem.builder()
                .id(3L)
                .title("待观察")
                .description("待观察")
                .difficulty(Problem.Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(65536)
                .build();
        SubmissionAnalysisResponse baseline = SubmissionAnalysisResponse.builder()
                .submissionId(13L)
                .sourceType("RULE_BASED_V1")
                .scenario("UNKNOWN")
                .issueTags(List.of())
                .fineGrainedTags(List.of())
                .evidenceRefs(List.of())
                .confidence(0.45)
                .uncertainty("")
                .build();

        DiagnosticAgentService.AgentResult result = service.diagnose(
                problem,
                submission,
                List.of(),
                baseline,
                Assignment.HintPolicy.L1
        );

        assertThat(result.analysis().getIssueTags()).contains("NEEDS_MORE_EVIDENCE");
        assertThat(result.analysis().getEvidenceRefs()).contains("agent:low_confidence");
        assertThat(result.analysis().getUncertainty()).contains("置信度较低");
        assertThat(result.analysis().getStudentHintPlan().getProblemType()).isEqualTo("证据不足");
        assertThat(result.analysis().getStudentHintPlan().getTeachingAction()).isEqualTo("COLLECT_EVIDENCE");
        assertThat(result.analysis().getLearningInterventionPlan()).isNotNull();
        assertThat(result.analysis().getLearningInterventionPlan().getInterventionType()).isEqualTo("COLLECT_EVIDENCE");
    }

    private DiagnosticAgentService newService() {
        DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();
        return new DiagnosticAgentService(
                new DiagnosisEvidencePackageBuilder(),
                new RuleSignalAnalyzer(),
                new PassThroughAiReportService(),
                new PassThroughHintSafetyService(taxonomy),
                taxonomy
        );
    }

    private static class PassThroughAiReportService extends AiReportService {
        protected PassThroughAiReportService() {
            super(new ObjectMapper(), new AiCodeAssistSupport());
        }

        @Override
        public SubmissionAnalysisResponse enhanceSubmissionAnalysis(Problem problem,
                                                                    Submission submission,
                                                                    SubmissionAnalysisResponse fallback,
                                                                    DiagnosisEvidencePackage evidencePackage,
                                                                    RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
            return fallback;
        }
    }

    private static class PassThroughHintSafetyService extends HintSafetyService {
        private PassThroughHintSafetyService(DiagnosisTaxonomy taxonomy) {
            super(null, new ObjectMapper(), taxonomy);
        }

        @Override
        public SubmissionAnalysisResponse verifyAndRecord(SubmissionAnalysisResponse analysis,
                                                          Assignment.HintPolicy hintPolicy) {
            return analysis;
        }
    }

    private static class FailingAiReportService extends PassThroughAiReportService {
        @Override
        public SubmissionAnalysisResponse enhanceSubmissionAnalysis(Problem problem,
                                                                    Submission submission,
                                                                    SubmissionAnalysisResponse fallback,
                                                                    DiagnosisEvidencePackage evidencePackage,
                                                                    RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
            throw new IllegalStateException("model stage unavailable");
        }
    }
}

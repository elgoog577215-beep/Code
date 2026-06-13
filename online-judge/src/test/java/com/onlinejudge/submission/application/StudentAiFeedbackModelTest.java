package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.dto.StudentAiFeedbackResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StudentAiFeedbackModelTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatesStudentFastFeedbackDirectlyFromStructuredModelOutput() {
        StubStudentFeedbackAiReportService service = newService("""
                {
                  "repairItems": [{
                    "title": "先核对输入读取",
                    "body": "用公开失败点手推每一次 input，确认代码是否读取到了题面要求的两个整数。",
                    "kind": "INPUT_PARSING",
                    "evidenceRefs": ["judge:first_failed_case:1", "code:line:1"],
                    "qualitySignals": ["evidence_grounded", "actionable", "no_answer_leak"]
                  }],
                  "improvementItems": [{
                    "title": "测试习惯",
                    "body": "改完后补一个和样例不同的最小输入，验证读取结构没有只适配样例。",
                    "kind": "TESTING_HABIT",
                    "evidenceRefs": ["problem:1"],
                    "qualitySignals": ["transfer"]
                  }],
                  "nextQuestion": "这段代码第二个整数是从哪里读到的？",
                  "safety": {"answerLeakRisk": "LOW", "blockedReasons": []},
                  "evidenceRefs": ["judge:first_failed_case:1"]
                }
                """);

        StudentAiFeedbackResponse feedback = service.generateStudentAiFeedback(
                problem(),
                submission(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(feedback.getStatus()).isEqualTo("READY");
        assertThat(feedback.getSource()).isEqualTo("MODEL");
        assertThat(feedback.getRepairItems()).singleElement().satisfies(item -> {
            assertThat(item.getBody()).contains("手推");
            assertThat(item.getEvidenceRefs()).contains("judge:first_failed_case:1");
        });
        assertThat(feedback.getImprovementItems()).singleElement().satisfies(item ->
                assertThat(item.getTitle()).isEqualTo("测试习惯"));
        assertThat(feedback.getNextQuestion()).contains("第二个整数");
        assertThat(feedback.getSafety().getAnswerLeakRisk()).isEqualTo("LOW");
        assertThat(service.lastSystemPrompt()).contains("fast student-facing OJ coach");
        assertThat(service.lastUserPrompt()).contains("judgeFacts", "candidateSignals", "sourceExcerpt");
        assertThat(service.lastUserPrompt()).doesNotContain("sourceCodeWithLineNumbers", "sourceCodeForLineAnalysis");
        assertThat(service.lastOutputTokens()).isLessThanOrEqualTo(520);
    }

    @Test
    void rejectsHighAnswerLeakRiskInsteadOfShowingModelText() {
        StubStudentFeedbackAiReportService service = newService("""
                {
                  "repairItems": [{
                    "title": "答案如下",
                    "body": "直接改成完整代码如下",
                    "kind": "ANSWER",
                    "evidenceRefs": ["code:line:1"],
                    "qualitySignals": []
                  }],
                  "improvementItems": [],
                  "nextQuestion": "",
                  "safety": {"answerLeakRisk": "HIGH", "blockedReasons": ["ANSWER_LEAK"]},
                  "evidenceRefs": ["code:line:1"]
                }
                """);

        StudentAiFeedbackResponse feedback = service.generateStudentAiFeedback(
                problem(),
                submission(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(feedback.getStatus()).isEqualTo("SAFETY_REJECTED");
        assertThat(feedback.getRepairItems()).isEmpty();
        assertThat(feedback.getImprovementItems()).isEmpty();
        assertThat(feedback.getSafety().getBlockedReasons()).contains("ANSWER_LEAK_RISK");
    }

    @Test
    void modelUnavailableReturnsFailedWithoutLocalAdvice() {
        AiReportService service = new AiReportService(objectMapper, new AiCodeAssistSupport());
        ReflectionTestUtils.setField(service, "enabled", false);

        StudentAiFeedbackResponse feedback = service.generateStudentAiFeedback(
                problem(),
                submission(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(feedback.getStatus()).isEqualTo("FAILED");
        assertThat(feedback.getSource()).isEqualTo("RULE_FALLBACK");
        assertThat(feedback.getRepairItems()).isEmpty();
        assertThat(feedback.getImprovementItems()).isEmpty();
        assertThat(feedback.getSafety().getBlockedReasons()).contains("AI_UNAVAILABLE");
    }

    private StubStudentFeedbackAiReportService newService(String response) {
        StubStudentFeedbackAiReportService service = new StubStudentFeedbackAiReportService(objectMapper, response);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "maxOutputTokens", 1800);
        ReflectionTestUtils.setField(service, "streamEnabled", false);
        return service;
    }

    private Problem problem() {
        return Problem.builder()
                .id(1L)
                .title("两数求和")
                .description("给定两个整数 a 和 b，输出它们的和。")
                .difficulty(Problem.Difficulty.EASY)
                .timeLimit(1000)
                .memoryLimit(128 * 1024)
                .build();
    }

    private Submission submission() {
        return Submission.builder()
                .id(7L)
                .problemId(1L)
                .languageName("Python 3")
                .verdict(Submission.Verdict.RUNTIME_ERROR)
                .sourceCode("n = int(input())\nprint(n)\n")
                .errorMessage("ValueError: invalid literal for int() with base 10: '3 5'")
                .build();
    }

    private DiagnosisEvidencePackage evidencePackage() {
        return DiagnosisEvidencePackage.builder()
                .submission(DiagnosisEvidencePackage.SubmissionEvidence.builder()
                        .id(7L)
                        .language("Python 3")
                        .verdict("RUNTIME_ERROR")
                        .sourceCodeWithLineNumbers("1: n = int(input())\n2: print(n)")
                        .sourceCodeLineCount(2)
                        .build())
                .problem(DiagnosisEvidencePackage.ProblemEvidence.builder()
                        .id(1L)
                        .title("两数求和")
                        .description("给定两个整数 a 和 b，输出它们的和。")
                        .build())
                .judgeFacts(DiagnosisEvidencePackage.JudgeFacts.builder()
                        .passedCount(0)
                        .totalCount(1)
                        .hiddenFailureObserved(false)
                        .runtimeErrorMessage("ValueError: invalid literal for int() with base 10: '3 5'")
                        .caseResultsSummary(List.of(DiagnosisEvidencePackage.CaseSummary.builder()
                                .testCaseNumber(1)
                                .passed(false)
                                .hidden(false)
                                .actualOutputPreview("ValueError")
                                .expectedOutputPreview("8")
                                .build()))
                        .build())
                .build();
    }

    private RuleSignalAnalyzer.RuleSignalResult ruleSignals() {
        return RuleSignalAnalyzer.RuleSignalResult.builder()
                .candidateIssueTags(List.of("IO_FORMAT"))
                .candidateFineGrainedTags(List.of("INPUT_PARSING"))
                .evidenceRefs(List.of("judge:first_failed_case:1", "code:line:1"))
                .signals(List.of(RuleSignalAnalyzer.Signal.builder()
                        .evidenceRef("code:line:1")
                        .coarseTag("IO_FORMAT")
                        .fineTag("INPUT_PARSING")
                        .confidence(0.86)
                        .message("代码读取结构和题面输入格式不匹配。")
                        .build()))
                .build();
    }

    private static class StubStudentFeedbackAiReportService extends AiReportService {
        private final String response;
        private String lastSystemPrompt;
        private String lastUserPrompt;
        private int lastOutputTokens;

        StubStudentFeedbackAiReportService(ObjectMapper objectMapper, String response) {
            super(objectMapper, new AiCodeAssistSupport());
            this.response = response;
        }

        @Override
        protected String chatCompletionForStudentFeedback(String systemPrompt,
                                                          String userPrompt,
                                                          int outputTokens) throws IOException {
            this.lastSystemPrompt = systemPrompt;
            this.lastUserPrompt = userPrompt;
            this.lastOutputTokens = outputTokens;
            if (response == null) {
                throw new IOException("No feedback response");
            }
            return response;
        }

        String lastSystemPrompt() {
            return lastSystemPrompt;
        }

        String lastUserPrompt() {
            return lastUserPrompt;
        }

        int lastOutputTokens() {
            return lastOutputTokens;
        }
    }
}

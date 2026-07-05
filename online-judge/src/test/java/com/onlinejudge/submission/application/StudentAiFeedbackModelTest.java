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
                  "studentReport": {
                    "basicLayerText": "这次主要不是算法不会，而是输入格式和代码读取方式没对齐。题面给的是两个整数，但代码只按一个整数读取，所以公开失败点一进来就会报 ValueError。你可以先拿 3 5 手推 input 实际读到了什么。",
                    "improvementLayerText": "修完读取方式后，不要只测样例。再补一个不同的最小输入，检查代码是不是理解了题面输入结构，而不是记住了某个样例。",
                    "nextActionText": "先写下这道题每一行输入分别包含什么，再对照代码每一次 input 读到了什么。"
                  },
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
        assertThat(feedback.getStudentReport().getBasicLayerText()).contains("输入格式");
        assertThat(feedback.getStudentReport().getImprovementLayerText()).contains("最小输入");
        assertThat(feedback.getStudentReport().getNextActionText()).contains("每一次 input");
        assertThat(feedback.getRepairItems()).singleElement().satisfies(item -> {
            assertThat(item.getBody()).contains("手推");
            assertThat(item.getEvidenceRefs()).contains("judge:first_failed_case:1");
            assertThat(item.getKnowledgePath()).contains("程序基础", "输入输出", "输入解析", "格式匹配");
            assertThat(item.getEvidenceSnippets()).singleElement().satisfies(snippet -> {
                assertThat(snippet.getLineNumber()).isEqualTo(1);
                assertThat(snippet.getCode()).contains("n = int(input())");
            });
        });
        assertThat(feedback.getImprovementItems()).singleElement().satisfies(item ->
                assertThat(item.getTitle()).isEqualTo("测试习惯"));
        assertThat(feedback.getNextQuestion()).contains("第二个整数");
        assertThat(feedback.getSafety().getAnswerLeakRisk()).isEqualTo("LOW");
        assertThat(service.lastSystemPrompt())
                .contains("学生快反馈教练", "studentReport", "禁止给最终代码");
        assertThat(service.lastUserPrompt())
                .contains("judgeFacts", "candidateSignals", "sourceExcerpt")
                .contains("不要把内部字段名写进学生反馈");
        assertThat(service.lastUserPrompt()).doesNotContain("sourceCodeWithLineNumbers", "sourceCodeForLineAnalysis");
        assertThat(service.lastOutputTokens()).isLessThanOrEqualTo(900);
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
    void trimsStudentReportNextActionToOneAction() {
        StubStudentFeedbackAiReportService service = newService("""
                {
                  "studentReport": {
                    "basicLayerText": "你的程序当前卡在排序比较规则上。公开样例里 10 排在了 2 前面，说明代码比较的不是数值大小。",
                    "improvementLayerText": "修复后可以补测不同位数的数字，确认数据表示和排序语义一致。",
                    "nextActionText": "1. 打印读取后列表的元素类型；2. 思考如何转换为整数；3. 验证 join 输出。"
                  },
                  "repairItems": [],
                  "improvementItems": [],
                  "nextQuestion": "",
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

        assertThat(feedback.getStudentReport().getNextActionText())
                .isEqualTo("打印读取后列表的元素类型");
    }

    @Test
    void removesInternalTraceMarkersFromStudentReport() {
        StubStudentFeedbackAiReportService service = newService("""
                {
                  "studentReport": {
                    "basicLayerText": "题目要求按数值排序，但当前输出把 10 放在 2 前面（verdict:wrong_answer, code:input_parsing_observed）。请检查排序时比较的是数值还是字符串。",
                    "improvementLayerText": "修复后可以继续关注数据类型和排序依据是否一致。",
                    "nextActionText": "检查排序前元素当前是字符串还是整数。"
                  },
                  "repairItems": [],
                  "improvementItems": [],
                  "nextQuestion": "",
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

        assertThat(feedback.getStudentReport().getBasicLayerText())
                .contains("题目要求按数值排序")
                .doesNotContain("verdict:", "code:");
    }

    @Test
    void longRuntimeErrorContextIncludesFailingLineWindow() {
        StubStudentFeedbackAiReportService service = newService("""
                {
                  "studentReport": {
                    "basicLayerText": "循环多走了一次，最后一次访问了数组外的位置。",
                    "improvementLayerText": "修复后补测 n=0、n=1 和普通样例。",
                    "nextActionText": "手推 i 的最后一个取值是否仍在数组范围内。"
                  },
                  "repairItems": [],
                  "improvementItems": [],
                  "nextQuestion": "",
                  "safety": {"answerLeakRisk": "LOW", "blockedReasons": []},
                  "evidenceRefs": ["judge:first_failed_case:1"]
                }
                """);

        service.generateStudentAiFeedback(
                problem(),
                longRuntimeSubmission(),
                longRuntimeEvidencePackage(),
                ruleSignals()
        );

        assertThat(service.lastUserPrompt())
                .contains("primaryRuntimeEvidence", "\"lineNumber\":622", "for i in range(n + 1)", "total += arr[i]")
                .doesNotContain("helper_1(x)");
    }

    @Test
    void correctsModelWhenRuntimeIndexErrorIsNotGrounded() {
        StubStudentFeedbackAiReportService service = newService("""
                {
                  "studentReport": {
                    "basicLayerText": "你的代码太长，包含大量 helper 函数，请先精简代码。",
                    "improvementLayerText": "修复后可以继续学习代码组织。",
                    "nextActionText": "删除无关 helper 函数后再提交。"
                  },
                  "repairItems": [{
                    "title": "代码过长",
                    "body": "大量无关 helper 干扰了 solve。",
                    "kind": "REPAIR",
                    "evidenceRefs": ["verdict:runtime_error"],
                    "qualitySignals": ["evidence_grounded"]
                  }],
                  "improvementItems": [],
                  "nextQuestion": "",
                  "safety": {"answerLeakRisk": "LOW", "blockedReasons": []},
                  "evidenceRefs": ["verdict:runtime_error"]
                }
                """);

        StudentAiFeedbackResponse feedback = service.generateStudentAiFeedback(
                problem(),
                longRuntimeSubmission(),
                longRuntimeEvidencePackage(),
                ruleSignals()
        );

        assertThat(feedback.getStatus()).isEqualTo("READY");
        assertThat(feedback.getStudentReport().getBasicLayerText())
                .contains("列表下标越界", "IndexError", "第 622 行")
                .doesNotContain("大量 helper");
        assertThat(feedback.getStudentReport().getNextActionText()).contains("下标", "数组长度");
        assertThat(feedback.getRepairItems()).singleElement().satisfies(item -> {
            assertThat(item.getTitle()).isEqualTo("列表下标越界");
            assertThat(item.getEvidenceRefs()).contains("code:line:622", "verdict:runtime_error");
            assertThat(item.getKnowledgePath()).contains("数组/列表", "下标访问", "越界检查");
            assertThat(item.getEvidenceSnippets()).singleElement().satisfies(snippet -> {
                assertThat(snippet.getLineNumber()).isEqualTo(622);
                assertThat(snippet.getCode()).contains("total += arr[i]");
            });
        });
    }

    @Test
    void alignsImprovementWhenIndexErrorDiagnosisIsGroundedButImprovementDrifts() {
        StubStudentFeedbackAiReportService service = newService("""
                {
                  "studentReport": {
                    "basicLayerText": "第622行发生索引越界，访问列表时下标超出范围。代码包含大量无关 helper 函数，干扰了核心逻辑。",
                    "improvementLayerText": "修复后建议精简代码结构，删除无关 helper 函数。",
                    "nextActionText": "检查下标是否小于数组长度。"
                  },
                  "repairItems": [{
                    "title": "索引越界",
                    "body": "第622行访问列表越界。",
                    "kind": "REPAIR",
                    "evidenceRefs": ["code:line:622"],
                    "qualitySignals": ["evidence_grounded"]
                  }],
                  "improvementItems": [{
                    "title": "精简冗余代码",
                    "body": "删除无关辅助函数。",
                    "kind": "IMPROVEMENT",
                    "evidenceRefs": [],
                    "qualitySignals": ["transfer"]
                  }],
                  "nextQuestion": "",
                  "safety": {"answerLeakRisk": "LOW", "blockedReasons": []},
                  "evidenceRefs": ["code:line:622"]
                }
                """);

        StudentAiFeedbackResponse feedback = service.generateStudentAiFeedback(
                problem(),
                longRuntimeSubmission(),
                longRuntimeEvidencePackage(),
                ruleSignals()
        );

        assertThat(feedback.getStudentReport().getBasicLayerText()).contains("索引越界");
        assertThat(feedback.getStudentReport().getBasicLayerText()).doesNotContain("helper", "无关");
        assertThat(feedback.getStudentReport().getImprovementLayerText())
                .contains("边界样例", "循环次数", "数组长度")
                .doesNotContain("删除无关");
        assertThat(feedback.getImprovementItems()).singleElement().satisfies(item ->
                assertThat(item.getTitle()).isEqualTo("边界样例意识"));
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
        assertThat(feedback.getStudentReport().getBasicLayerText()).contains("外部 AI 暂不可用");
        assertThat(feedback.getStudentReport().getNextActionText()).contains("重试 AI");
        assertThat(feedback.getSafety().getBlockedReasons()).contains("AI_UNAVAILABLE");
    }

    @Test
    void callFailureReturnsDisplayableDegradedMessageWithoutLocalAdvice() {
        StubStudentFeedbackAiReportService service = newService(null);

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
        assertThat(feedback.getStudentReport().getBasicLayerText()).contains("外部 AI 暂不可用");
        assertThat(feedback.getStudentReport().getImprovementLayerText()).contains("不可靠结论");
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

    private Submission longRuntimeSubmission() {
        return Submission.builder()
                .id(8L)
                .problemId(1L)
                .languageName("Python 3")
                .verdict(Submission.Verdict.RUNTIME_ERROR)
                .sourceCode(longSourceCode())
                .errorMessage("""
                        Traceback (most recent call last):
                          File "solution.py", line 625, in <module>
                            solve()
                          File "solution.py", line 622, in solve
                            total += arr[i]
                        IndexError: list index out of range
                        """)
                .build();
    }

    private DiagnosisEvidencePackage longRuntimeEvidencePackage() {
        String source = longSourceCode();
        return DiagnosisEvidencePackage.builder()
                .submission(DiagnosisEvidencePackage.SubmissionEvidence.builder()
                        .id(8L)
                        .language("Python 3")
                        .verdict("RUNTIME_ERROR")
                        .sourceCode(source)
                        .sourceCodeWithLineNumbers(numbered(source))
                        .sourceCodeLineCount(source.split("\\R", -1).length)
                        .build())
                .problem(DiagnosisEvidencePackage.ProblemEvidence.builder()
                        .id(1L)
                        .title("长代码求和")
                        .description("给定 n 和 n 个整数，输出它们的和。")
                        .build())
                .judgeFacts(DiagnosisEvidencePackage.JudgeFacts.builder()
                        .passedCount(0)
                        .totalCount(1)
                        .hiddenFailureObserved(false)
                        .runtimeErrorMessage(longRuntimeSubmission().getErrorMessage())
                        .caseResultsSummary(List.of(DiagnosisEvidencePackage.CaseSummary.builder()
                                .testCaseNumber(1)
                                .passed(false)
                                .hidden(false)
                                .actualOutputPreview("IndexError: list index out of range")
                                .expectedOutputPreview("6")
                                .build()))
                        .build())
                .build();
    }

    private String longSourceCode() {
        StringBuilder builder = new StringBuilder("import sys\n");
        for (int i = 1; i <= 611; i++) {
            builder.append("def helper_").append(i).append("(x): return x\n");
        }
        builder.append("""
                def read_ints():
                    return list(map(int, sys.stdin.readline().split()))

                def solve():
                    data = read_ints()
                    n = data[0]
                    arr = read_ints()
                    total = 0
                    for i in range(n + 1):
                        total += arr[i]
                    print(total)

                solve()
                """);
        return builder.toString();
    }

    private String numbered(String source) {
        String[] lines = source.split("\\R", -1);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(i + 1).append(": ").append(lines[i]);
        }
        return builder.toString();
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

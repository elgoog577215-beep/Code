package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryGrowthAgentService;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.dto.StudentAiFeedbackResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                evidencePackage()
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
                .contains("学生快反馈教练", "studentReport", "禁止给最终代码", "不要只解释第一个");
        assertThat(service.lastUserPrompt())
                .contains("judgeFacts", "sourceCodeWithLineNumbers", "n = int(input())")
                .contains("不要把内部字段名写进学生反馈");
        assertThat(service.lastUserPrompt()).doesNotContain("candidateSignals", "sourceExcerpt", "evidenceCandidates", "primaryRuntimeEvidence", "sourceCodeForLineAnalysis");
        assertThat(service.lastOutputTokens()).isEqualTo(1800);
    }

    @Test
    void keepsAllStructuredRepairAndImprovementItemsFromModel() {
        StubStudentFeedbackAiReportService service = newService("""
                {
                  "studentReport": {
                    "basicLayerText": "这次有多个可独立检查的问题：先核对输入读取，再核对输出表达。",
                    "improvementLayerText": "修复后可以从自测、变量追踪和题面复述三个方向提升。",
                    "nextActionText": "先写下每一次 input 应该读到什么。"
                  },
                  "repairItems": [{
                    "title": "输入读取不匹配",
                    "body": "先检查读取语句是否覆盖题面要求的两个整数。",
                    "kind": "INPUT_PARSING",
                    "evidenceRefs": ["code:line:1"],
                    "qualitySignals": ["evidence_grounded", "actionable"]
                  }, {
                    "title": "输出目标要复核",
                    "body": "再确认程序最终输出的是题目要求的目标量。",
                    "kind": "OUTPUT_GOAL",
                    "evidenceRefs": ["judge:first_failed_case:1"],
                    "qualitySignals": ["evidence_grounded", "actionable"]
                  }, {
                    "title": "异常信息先定位",
                    "body": "保留报错类型和触发行，先解释为什么会在读取阶段失败。",
                    "kind": "RUNTIME_TRACE",
                    "evidenceRefs": ["code:line:1"],
                    "qualitySignals": ["evidence_grounded", "actionable"]
                  }],
                  "improvementItems": [{
                    "title": "输入结构自测",
                    "body": "修复后补一个和样例格式不同的小输入。",
                    "kind": "IMPROVEMENT",
                    "evidenceRefs": ["code:line:1"],
                    "qualitySignals": ["transfer"]
                  }, {
                    "title": "变量追踪",
                    "body": "记录读取后的变量值，确认它们对应题面含义。",
                    "kind": "IMPROVEMENT",
                    "evidenceRefs": ["code:line:1"],
                    "qualitySignals": ["transfer"]
                  }, {
                    "title": "题面复述",
                    "body": "修改前先用一句话复述输入和输出分别是什么。",
                    "kind": "IMPROVEMENT",
                    "evidenceRefs": ["judge:first_failed_case:1"],
                    "qualitySignals": ["transfer"]
                  }],
                  "nextQuestion": "这段代码读到的第一个值对应题面里的哪个量？",
                  "safety": {"answerLeakRisk": "LOW", "blockedReasons": []},
                  "evidenceRefs": ["code:line:1", "judge:first_failed_case:1"]
                }
                """);

        StudentAiFeedbackResponse feedback = service.generateStudentAiFeedback(
                problem(),
                submission(),
                evidencePackage()
        );

        assertThat(feedback.getStatus()).isEqualTo("READY");
        assertThat(feedback.getRepairItems())
                .hasSize(3)
                .extracting(StudentAiFeedbackResponse.FeedbackItem::getTitle)
                .containsExactly("输入读取不匹配", "输出目标要复核", "异常信息先定位");
        assertThat(feedback.getImprovementItems())
                .hasSize(3)
                .extracting(StudentAiFeedbackResponse.FeedbackItem::getTitle)
                .containsExactly("输入结构自测", "变量追踪", "题面复述");
        assertThat(feedback.getEvidenceRefs()).contains("code:line:1", "judge:first_failed_case:1");
        assertThat(service.lastOutputTokens()).isEqualTo(1800);
        assertThat(service.lastSystemPrompt()).contains("0 到多条");
    }

    @Test
    void mapsModelReturnedCodeLineRefsToClickableCodeSnippets() {
        StubStudentFeedbackAiReportService service = newService("""
                {
                  "studentReport": {
                    "basicLayerText": "这次主要卡在输入读取方式。题目给两个整数，但代码只按一个整数读取，所以公开失败点一开始就报错。",
                    "improvementLayerText": "修复后可以补一个不同格式的小样例，确认读取逻辑不是只记住样例。",
                    "nextActionText": "对照题面写下每一次 input 应该读到什么。"
                  },
                  "repairItems": [{
                    "title": "输入读取不匹配",
                    "body": "先检查读取语句是否一次读到了题面要求的两个整数。",
                    "kind": "INPUT_PARSING",
                    "evidenceRefs": ["code:line:1"],
                    "qualitySignals": ["evidence_grounded", "actionable"]
                  }],
                  "improvementItems": [],
                  "nextQuestion": "",
                  "safety": {"answerLeakRisk": "LOW", "blockedReasons": []},
                  "evidenceRefs": ["code:line:1"]
                }
                """);

        StudentAiFeedbackResponse feedback = service.generateStudentAiFeedback(
                problem(),
                submission(),
                evidencePackage()
        );

        assertThat(service.lastUserPrompt())
                .contains("\"sourceCodeWithLineNumbers\":\"1: n = int(input())")
                .doesNotContain("\"id\":\"E1\"", "evidenceCandidates");
        assertThat(service.lastSystemPrompt()).contains("不要使用 E1/E2");
        assertThat(feedback.getEvidenceRefs()).contains("code:line:1").doesNotContain("E1");
        assertThat(feedback.getRepairItems()).singleElement().satisfies(item -> {
            assertThat(item.getEvidenceRefs()).contains("code:line:1").doesNotContain("E1");
            assertThat(item.getEvidenceSnippets()).singleElement().satisfies(snippet -> {
                assertThat(snippet.getLineNumber()).isEqualTo(1);
                assertThat(snippet.getCode()).contains("n = int(input())");
            });
        });
    }

    @Test
    void studentFastFeedbackReceivesStandardLibraryContextAndKeepsLibraryIds() {
        ExternalModelAgentRuntime runtime = mock(ExternalModelAgentRuntime.class);
        when(runtime.prepare(any(DiagnosisEvidencePackage.class),
                isNull(),
                anyString()))
                .thenReturn(ExternalModelAgentRuntime.RuntimePlan.builder()
                        .brief(ModelDiagnosisBrief.builder().build())
                        .standardLibraryPack(studentStandardLibraryPack())
                        .requestCompact(false)
                        .build());
        StubStudentFeedbackAiReportService service = newService("""
                {
                  "studentReport": {
                    "basicLayerText": "这次主要卡在输入读取方式。题目给两个整数，但代码只按一个整数读取。",
                    "improvementLayerText": "修复后可以补一个不同格式的小样例，确认读取逻辑不是只记住样例。",
                    "nextActionText": "对照题面写下每一次 input 应该读到什么。"
                  },
                  "repairItems": [{
                    "title": "输入读取不匹配",
                    "body": "先检查读取语句是否一次读到了题面要求的两个整数。",
                    "kind": "INPUT_PARSING",
                    "libraryItemId": "MP_INPUT_SINGLE_READ",
                    "skillUnitId": "SK_INPUT_PARSE",
                    "mistakePointId": "MP_INPUT_SINGLE_READ",
                    "libraryFit": "HIT",
                    "knowledgePath": ["程序基础", "输入输出", "输入解析", "格式匹配"],
                    "evidenceRefs": ["code:line:1"],
                    "qualitySignals": ["evidence_grounded", "actionable"]
                  }],
                  "improvementItems": [],
                  "nextQuestion": "",
                  "safety": {"answerLeakRisk": "LOW", "blockedReasons": []},
                  "evidenceRefs": ["code:line:1"]
                }
                """, runtime);

        StudentAiFeedbackResponse feedback = service.generateStudentAiFeedback(
                problem(),
                submission(),
                evidencePackage()
        );

        assertThat(service.lastUserPrompt())
                .contains("\"standardLibrary\"", "\"knowledgeGroups\"", "SK_INPUT_PARSE", "MP_INPUT_SINGLE_READ")
                .contains("\"primaryKnowledgeNodeCode\":\"BASIC.IO.INPUT\"")
                .contains("\"relatedKnowledgeNodeCodes\":[\"BASIC.STRING.SPLIT\"]");
        assertThat(service.lastSystemPrompt())
                .contains("primaryKnowledgeNodeCode", "relatedKnowledgeNodeCodes", "主知识点 -> 能力点 -> 易错点/提升点");
        assertThat(feedback.getRepairItems()).singleElement().satisfies(item -> {
            assertThat(item.getLibraryItemId()).isEqualTo("MP_INPUT_SINGLE_READ");
            assertThat(item.getSkillUnitId()).isEqualTo("SK_INPUT_PARSE");
            assertThat(item.getMistakePointId()).isEqualTo("MP_INPUT_SINGLE_READ");
            assertThat(item.getLibraryFit()).isEqualTo("HIT");
            assertThat(item.getEvidenceRefs()).contains("code:line:1").doesNotContain("E1");
        });
    }

    @Test
    void outOfLibraryFastFeedbackEntersGrowthCandidatePool() {
        AiStandardLibraryGrowthAgentService growthAgentService = mock(AiStandardLibraryGrowthAgentService.class);
        when(growthAgentService.proposeFromDiagnosisOutput(any(AdviceGenerationOutput.class), any(), any(), isNull()))
                .thenReturn(List.of());
        StubStudentFeedbackAiReportService service = newService("""
                {
                  "studentReport": {
                    "basicLayerText": "这次主要卡在优惠边权的语义。代码能跑最短路，但折扣后的时间和题意没有完全对齐。",
                    "improvementLayerText": "修复后可以补测奇数边权和多张优惠券状态，检查路径代价是否仍稳定。",
                    "nextActionText": "手算一条含奇数边权的路径，核对折扣后时间。"
                  },
                  "repairItems": [{
                    "title": "优惠边权取整语义",
                    "body": "当前标准库没有直接覆盖这个细颗粒问题，应作为图论最短路下的特殊边权候选沉淀。",
                    "kind": "REPAIR",
                    "libraryFit": "OUT_OF_LIBRARY",
                    "knowledgePath": ["图论", "最短路", "状态扩展", "优惠边权"],
                    "evidenceRefs": ["code:line:1"],
                    "qualitySignals": ["evidence_grounded", "actionable"]
                  }],
                  "improvementItems": [],
                  "nextQuestion": "",
                  "safety": {"answerLeakRisk": "LOW", "blockedReasons": []},
                  "evidenceRefs": ["code:line:1"]
                }
                """, growthAgentService);

        StudentAiFeedbackResponse feedback = service.generateStudentAiFeedback(
                problem(),
                submission(),
                evidencePackage()
        );

        assertThat(feedback.getStatus()).isEqualTo("READY");
        verify(growthAgentService).proposeFromDiagnosisOutput(
                argThat(output -> output != null
                        && output.getLibraryGrowth() != null
                        && output.getLibraryGrowth().getCandidates() != null
                        && output.getLibraryGrowth().getCandidates().size() == 1
                        && "优惠边权取整语义".equals(output.getLibraryGrowth().getCandidates().get(0).getName())
                        && output.getLibraryGrowth().getCandidates().get(0).getSuggestedPath().contains("优惠边权")
                        && output.getLibraryGrowth().getCandidates().get(0).getEvidenceRefs().contains("code:line:1")
                        && "NEEDS_REVIEW".equals(output.getLibraryGrowth().getCandidates().get(0).getStatus())),
                eq(1L),
                eq(7L),
                isNull()
        );
        assertThat(service.lastSystemPrompt())
                .contains("diagnosisCandidates 是后台审计线", "libraryGrowth 是标准库成长线");
    }

    @Test
    void modelProvidedGrowthCandidateKeepsModelChosenCodeLineEvidence() {
        AiStandardLibraryGrowthAgentService growthAgentService = mock(AiStandardLibraryGrowthAgentService.class);
        when(growthAgentService.proposeFromDiagnosisOutput(any(AdviceGenerationOutput.class), any(), any(), isNull()))
                .thenReturn(List.of());
        StubStudentFeedbackAiReportService service = newService("""
                {
                  "studentReport": {
                    "basicLayerText": "这次卡在输入读取和题面格式没有对齐。",
                    "improvementLayerText": "修复后可以补一个不同格式的小样例。",
                    "nextActionText": "手推第一行输入会被怎样解析。"
                  },
                  "repairItems": [],
                  "improvementItems": [],
                  "libraryGrowth": {"candidates": [{
                    "name": "同一行多整数读取误判",
                    "suggestedPath": ["程序基础", "输入输出", "输入解析"],
                    "similarExistingItems": [],
                    "evidenceRefs": ["code:line:1"],
                    "evidenceStatus": "SUPPORTED",
                    "errorSymptom": "题面一行有多个整数，代码按单个整数读取。",
                    "typicalCodePattern": "int(input())",
                    "studentExplanation": "先核对每次 input 实际读到什么。",
                    "reason": "快反馈认为标准库缺少更细颗粒读取条目。",
                    "status": "NEEDS_REVIEW",
                    "confidence": 0.72
                  }]},
                  "nextQuestion": "",
                  "safety": {"answerLeakRisk": "LOW", "blockedReasons": []},
                  "evidenceRefs": ["code:line:1"]
                }
                """, growthAgentService);

        StudentAiFeedbackResponse feedback = service.generateStudentAiFeedback(
                problem(),
                submission(),
                evidencePackage()
        );

        assertThat(feedback.getStatus()).isEqualTo("READY");
        verify(growthAgentService).proposeFromDiagnosisOutput(
                argThat(output -> output.getLibraryGrowth().getCandidates().get(0)
                        .getEvidenceRefs().contains("code:line:1")),
                eq(1L),
                eq(7L),
                isNull()
        );
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
                evidencePackage()
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
                evidencePackage()
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
                evidencePackage()
        );

        assertThat(feedback.getStudentReport().getBasicLayerText())
                .contains("题目要求按数值排序")
                .doesNotContain("verdict:", "code:");
    }

    @Test
    void longRuntimeErrorContextIncludesFullLineNumberedSourceCode() {
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
                longRuntimeEvidencePackage()
        );

        assertThat(service.lastUserPrompt())
                .contains("sourceCodeWithLineNumbers", "622:         total += arr[i]", "for i in range(n + 1)", "def helper_1(x):")
                .doesNotContain("primaryRuntimeEvidence", "sourceExcerpt", "evidenceCandidates");
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
                longRuntimeEvidencePackage()
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
    void preservesOtherFeedbackItemsWhenRuntimeGroundingAddsIndexError() {
        StubStudentFeedbackAiReportService service = newService("""
                {
                  "studentReport": {
                    "basicLayerText": "你的代码太长，包含大量 helper 函数，请先精简代码。",
                    "improvementLayerText": "修复后可以继续做边界样例和变量追踪。",
                    "nextActionText": "删除无关 helper 函数后再提交。"
                  },
                  "repairItems": [{
                    "title": "代码过长",
                    "body": "大量无关 helper 干扰了 solve。",
                    "kind": "REPAIR",
                    "evidenceRefs": ["verdict:runtime_error"],
                    "qualitySignals": ["evidence_grounded"]
                  }, {
                    "title": "输入规模读取要复核",
                    "body": "运行错误前也要确认 n 和实际读入的数组长度是否对应。",
                    "kind": "REPAIR",
                    "evidenceRefs": ["code:line:618"],
                    "qualitySignals": ["evidence_grounded", "actionable"]
                  }],
                  "improvementItems": [{
                    "title": "变量追踪",
                    "body": "记录 n、数组长度和最后一次 i 的取值，确认它们是否一致。",
                    "kind": "IMPROVEMENT",
                    "evidenceRefs": ["code:line:621"],
                    "qualitySignals": ["transfer"]
                  }, {
                    "title": "边界样例",
                    "body": "补测最小 n 和普通样例，观察循环最后一次是否越界。",
                    "kind": "IMPROVEMENT",
                    "evidenceRefs": ["code:line:621"],
                    "qualitySignals": ["transfer"]
                  }],
                  "nextQuestion": "",
                  "safety": {"answerLeakRisk": "LOW", "blockedReasons": []},
                  "evidenceRefs": ["verdict:runtime_error"]
                }
                """);

        StudentAiFeedbackResponse feedback = service.generateStudentAiFeedback(
                problem(),
                longRuntimeSubmission(),
                longRuntimeEvidencePackage()
        );

        assertThat(feedback.getRepairItems())
                .extracting(StudentAiFeedbackResponse.FeedbackItem::getTitle)
                .containsExactly("列表下标越界", "输入规模读取要复核");
        assertThat(feedback.getRepairItems())
                .extracting(StudentAiFeedbackResponse.FeedbackItem::getTitle)
                .doesNotContain("代码过长");
        assertThat(feedback.getImprovementItems())
                .extracting(StudentAiFeedbackResponse.FeedbackItem::getTitle)
                .contains("边界样例意识", "变量追踪", "边界样例");
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
                longRuntimeEvidencePackage()
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
    void replacesOnlyDriftedImprovementItemsForGroundedIndexError() {
        StubStudentFeedbackAiReportService service = newService("""
                {
                  "studentReport": {
                    "basicLayerText": "第622行发生索引越界，访问列表时下标超出范围。",
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
                  }, {
                    "title": "变量追踪",
                    "body": "记录 n、数组长度和最后一次 i 的取值，确认它们是否一致。",
                    "kind": "IMPROVEMENT",
                    "evidenceRefs": ["code:line:621"],
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
                longRuntimeEvidencePackage()
        );

        assertThat(feedback.getImprovementItems())
                .extracting(StudentAiFeedbackResponse.FeedbackItem::getTitle)
                .containsExactly("边界样例意识", "变量追踪");
    }

    @Test
    void modelUnavailableReturnsFailedWithoutLocalAdvice() {
        AiReportService service = new AiReportService(objectMapper, new AiCodeAssistSupport());
        ReflectionTestUtils.setField(service, "enabled", false);

        StudentAiFeedbackResponse feedback = service.generateStudentAiFeedback(
                problem(),
                submission(),
                evidencePackage()
        );

        assertThat(feedback.getStatus()).isEqualTo("FAILED");
        assertThat(feedback.getSource()).isEqualTo("AI_UNAVAILABLE");
        assertThat(feedback.getRepairItems()).isEmpty();
        assertThat(feedback.getImprovementItems()).isEmpty();
        assertThat(feedback.getStudentReport().getBasicLayerText()).contains("AI 暂不可用");
        assertThat(feedback.getStudentReport().getNextActionText()).contains("重试 AI");
        assertThat(feedback.getSafety().getBlockedReasons()).contains("AI_UNAVAILABLE");
    }

    @Test
    void callFailureReturnsDisplayableDegradedMessageWithoutLocalAdvice() {
        StubStudentFeedbackAiReportService service = newService(null);

        StudentAiFeedbackResponse feedback = service.generateStudentAiFeedback(
                problem(),
                submission(),
                evidencePackage()
        );

        assertThat(feedback.getStatus()).isEqualTo("FAILED");
        assertThat(feedback.getSource()).isEqualTo("AI_UNAVAILABLE");
        assertThat(feedback.getRepairItems()).isEmpty();
        assertThat(feedback.getImprovementItems()).isEmpty();
        assertThat(feedback.getStudentReport().getBasicLayerText()).contains("AI 暂不可用");
        assertThat(feedback.getStudentReport().getImprovementLayerText()).contains("暂无提升建议");
    }

    private StubStudentFeedbackAiReportService newService(String response) {
        StubStudentFeedbackAiReportService service = new StubStudentFeedbackAiReportService(objectMapper, response);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "maxOutputTokens", 1800);
        ReflectionTestUtils.setField(service, "streamEnabled", false);
        return service;
    }

    private StubStudentFeedbackAiReportService newService(String response, ExternalModelAgentRuntime runtime) {
        StubStudentFeedbackAiReportService service = new StubStudentFeedbackAiReportService(objectMapper, response, runtime);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "maxOutputTokens", 1800);
        ReflectionTestUtils.setField(service, "streamEnabled", false);
        return service;
    }

    private StubStudentFeedbackAiReportService newService(String response,
                                                          AiStandardLibraryGrowthAgentService growthAgentService) {
        StubStudentFeedbackAiReportService service =
                new StubStudentFeedbackAiReportService(objectMapper, response, null, growthAgentService);
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

    private StandardLibraryPack studentStandardLibraryPack() {
        StandardLibraryPack.SkillUnitOption skill = StandardLibraryPack.SkillUnitOption.builder()
                .id("SK_INPUT_PARSE")
                .category("能力点/输入输出")
                .name("按题面格式解析输入")
                .description("能根据题面判断一行里有几个量，以及每次读取应该得到什么。")
                .primaryKnowledgeNodeCode("BASIC.IO.INPUT")
                .knowledgeNodeCodes(List.of("BASIC.IO.INPUT"))
                .relatedKnowledgeNodeCodes(List.of("BASIC.STRING.SPLIT"))
                .applicableLanguages(List.of("PYTHON"))
                .build();
        StandardLibraryPack.MistakePointOption mistake = StandardLibraryPack.MistakePointOption.builder()
                .id("MP_INPUT_SINGLE_READ")
                .category("易错点/输入输出")
                .name("把同一行多个整数当成单个整数读取")
                .description("题面一行给多个整数，但代码用 int(input()) 只读取单个整数。")
                .skillUnitCode("SK_INPUT_PARSE")
                .mistakeType("INPUT_FORMAT")
                .primaryKnowledgeNodeCode("BASIC.IO.INPUT")
                .knowledgeNodeCodes(List.of("BASIC.IO.INPUT"))
                .relatedKnowledgeNodeCodes(List.of("BASIC.STRING.SPLIT"))
                .applicableLanguages(List.of("PYTHON"))
                .build();
        return StandardLibraryPack.builder()
                .schemaVersion(StandardLibraryPack.SCHEMA_VERSION)
                .structureVersion(StandardLibraryPack.STRUCTURE_VERSION)
                .knowledgeGroups(List.of(StandardLibraryPack.KnowledgeGroupOption.builder()
                        .id("BASIC.IO.INPUT")
                        .name("输入读取")
                        .path("程序基础 > 输入输出 > 输入读取")
                        .skillUnits(List.of(StandardLibraryPack.SkillUnitGroupOption.builder()
                                .skillUnit(skill)
                                .mistakePoints(List.of(mistake))
                                .candidateIds(List.of("SK_INPUT_PARSE", "MP_INPUT_SINGLE_READ"))
                                .build()))
                        .build()))
                .skillUnits(List.of(skill))
                .mistakePoints(List.of(mistake))
                .standardLibraryNavigationSummary(StandardLibraryPack.StandardLibraryNavigationSummary.builder()
                        .status("AI_NAVIGATION")
                        .selectedCount(2)
                        .build())
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

        StubStudentFeedbackAiReportService(ObjectMapper objectMapper,
                                           String response,
                                           ExternalModelAgentRuntime runtime) {
            super(objectMapper, new AiCodeAssistSupport(), runtime);
            this.response = response;
        }

        StubStudentFeedbackAiReportService(ObjectMapper objectMapper,
                                           String response,
                                           ExternalModelAgentRuntime runtime,
                                           AiStandardLibraryGrowthAgentService growthAgentService) {
            super(objectMapper,
                    new AiCodeAssistSupport(),
                    runtime,
                    new ExternalModelFailureClassifier(),
                    new ExternalModelBudgetGuard(),
                    new ExternalModelChatRequestFactory(),
                    growthAgentService,
                    null,
                    null,
                    null);
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

package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class AiReportServiceExternalRuntimeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void externalRuntimeCompletesTwoStagesAndMergesTeachingOutput() {
        StubAiReportService service = newService(
                """
                {
                  "primaryIssueTag": "LOOP_BOUNDARY",
                  "fineGrainedTag": "OFF_BY_ONE",
                  "evidenceRefs": ["code:range_excludes_n"],
                  "confidence": 0.91,
                  "uncertainty": "range(1, n) 没有覆盖 n，需要保留隐藏测试不确定性。",
                  "needsMoreEvidence": false,
                  "answerLeakRisk": "LOW"
                }
                """,
                """
                {
                  "studentHint": "先用 n=1 和 n=2 手推循环实际执行了哪些 i。",
                  "studentHintPlan": {
                    "hintLevel": "L2",
                    "problemType": "循环边界",
                    "evidenceAnchor": "code:range_excludes_n",
                    "nextAction": "列出 range 产生的每个 i，再和题目要求的 1 到 n 对齐。",
                    "coachQuestion": "当 n=1 时，循环体会执行几次？",
                    "teachingAction": "TRACE_VARIABLES",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "answerLeakRisk": "LOW"
                  },
                  "learningInterventionPlan": {
                    "interventionType": "VARIABLE_TRACE",
                    "goal": "确认循环上下界是否覆盖题目要求。",
                    "studentTask": "写出 n=1、n=2 时 i 的取值表。",
                    "checkQuestion": "最后一次循环是否处理到了 n？",
                    "completionSignal": "学生能给出 i 的取值表并指出缺失位置。",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "estimatedMinutes": 6,
                    "answerLeakRisk": "LOW"
                  },
                  "teacherNote": "学生需要把 range 的右边界与闭区间要求对应起来。",
                  "answerLeakRisk": "LOW"
                }
                """
        );
        ReflectionTestUtils.setField(service, "externalRuntimeMode", "staged");

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("MODEL_SCOPE_EXTERNAL_MODEL");
        assertThat(analysis.getIssueTags()).containsExactly("LOOP_BOUNDARY");
        assertThat(analysis.getFineGrainedTags()).containsExactly("OFF_BY_ONE");
        assertThat(analysis.getEvidenceRefs()).contains("code:range_excludes_n");
        assertThat(analysis.getStudentHint()).contains("n=1");
        assertThat(analysis.getStudentHintPlan().getTeachingAction()).isEqualTo("TRACE_VARIABLES");
        assertThat(analysis.getLearningInterventionPlan().getInterventionType()).isEqualTo("VARIABLE_TRACE");
        assertThat(analysis.getLineIssues()).hasSize(1);
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isFalse();
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo("diagnosis-judge-v2+teaching-hint-v1");
        assertThat(analysis.getAiInvocation().getRuntimeMode()).isEqualTo("staged");
        assertThat(analysis.getAiInvocation().getFailureStage()).isEmpty();
        assertThat(analysis.getAiInvocation().getFailureReason()).isEmpty();
        assertThat(service.callCount()).isEqualTo(2);
    }

    @Test
    void externalRuntimeNormalizesModelLabelsAndEvidenceRefsBeforeValidation() {
        StubAiReportService service = newService(
                """
                {
                  "primaryIssueTag": "循环边界",
                  "fineGrainedTag": "差 一 位 错误",
                  "evidenceRefs": [" CODE:RANGE_EXCLUDES_N "],
                  "confidence": 0.88,
                  "uncertainty": "模型使用了中文标签，但证据仍然指向循环边界。",
                  "needsMoreEvidence": false,
                  "answerLeakRisk": "low"
                }
                """,
                """
                {
                  "studentHint": "先用 n=1 手推循环是否执行。",
                  "studentHintPlan": {
                    "hintLevel": "L2",
                    "problemType": "循环边界",
                    "evidenceAnchor": "code:range_excludes_n",
                    "nextAction": "列出 range 产生的 i。",
                    "coachQuestion": "当 n=1 时循环执行几次？",
                    "teachingAction": "trace_variables",
                    "evidenceRefs": [" CODE:RANGE_EXCLUDES_N "],
                    "answerLeakRisk": "low"
                  },
                  "learningInterventionPlan": {
                    "interventionType": "VARIABLE_TRACE",
                    "goal": "确认循环是否覆盖 n。",
                    "studentTask": "手推 n=1 和 n=2。",
                    "checkQuestion": "最后一次循环是否处理到 n？",
                    "completionSignal": "能写出 i 的取值表。",
                    "evidenceRefs": [" CODE:RANGE_EXCLUDES_N "],
                    "estimatedMinutes": 6,
                    "answerLeakRisk": "low"
                  },
                  "teacherNote": "模型输出已被标准化后使用。",
                  "answerLeakRisk": "low"
                }
                """
        );
        ReflectionTestUtils.setField(service, "externalRuntimeMode", "staged");

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getIssueTags()).containsExactly("LOOP_BOUNDARY");
        assertThat(analysis.getFineGrainedTags()).containsExactly("OFF_BY_ONE");
        assertThat(analysis.getEvidenceRefs()).containsExactly("code:range_excludes_n");
        assertThat(analysis.getStudentHintPlan().getTeachingAction()).isEqualTo("TRACE_VARIABLES");
        assertThat(analysis.getStudentHintPlan().getEvidenceRefs()).containsExactly("code:range_excludes_n");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
        assertThat(analysis.getAiInvocation().getRuntimeMode()).isEqualTo("staged");
        assertThat(service.callCount()).isEqualTo(2);
    }

    @Test
    void singleCallRuntimeCompletesDiagnosisAndTeachingWithOneModelCall() {
        StubAiReportService service = newService(
                """
                {
                  "diagnosisDecision": {
                    "primaryIssueTag": "LOOP_BOUNDARY",
                    "fineGrainedTag": "OFF_BY_ONE",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "confidence": 0.9,
                    "uncertainty": "range 右边界证据明确。",
                    "needsMoreEvidence": false,
                    "answerLeakRisk": "LOW"
                  },
                  "teachingHint": {
                    "studentHint": "先用 n=1 手推循环会不会执行。",
                    "studentHintPlan": {
                      "hintLevel": "L2",
                      "problemType": "循环边界",
                      "evidenceAnchor": "code:range_excludes_n",
                      "nextAction": "列出 range 产生的 i。",
                      "coachQuestion": "当 n=1 时循环执行几次？",
                      "teachingAction": "TRACE_VARIABLES",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "answerLeakRisk": "LOW"
                    },
                    "learningInterventionPlan": {
                      "interventionType": "VARIABLE_TRACE",
                      "goal": "确认循环是否覆盖 n。",
                      "studentTask": "手推 n=1 和 n=2。",
                      "checkQuestion": "最后一次循环是否处理到 n？",
                      "completionSignal": "能写出 i 的取值表。",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "estimatedMinutes": 6,
                      "answerLeakRisk": "LOW"
                    },
                    "teacherNote": "单次调用已给出安全教学提示。",
                    "answerLeakRisk": "LOW"
                  },
                  "studentFeedback": {
                    "summary": "这次主要问题是循环边界没有覆盖到题目要求的范围。",
                    "blockingIssues": [{
                      "priority": 1,
                      "title": "当前最需要先处理的问题",
                      "studentMessage": "循环边界和题目要求的闭区间不一致，先手推最小样例确认缺失位置。",
                      "evidence": "code:range_excludes_n",
                      "nextAction": "列出 range 产生的 i。",
                      "issueTag": "LOOP_BOUNDARY",
                      "fineGrainedTag": "OFF_BY_ONE",
                      "evidenceRefs": ["code:range_excludes_n"]
                    }],
                    "secondaryIssues": [],
                    "improvementOpportunities": [{
                      "category": "TESTING_HABIT",
                      "studentMessage": "通过后补一个 n=1 的最小自测。",
                      "benefit": "能提前发现边界遗漏。",
                      "evidenceRefs": ["code:range_excludes_n"]
                    }],
                    "nextLearningAction": {
                      "hintLevel": "L2",
                      "action": "TRACE_VARIABLES",
                      "task": "列出 range 产生的 i。",
                      "checkQuestion": "当 n=1 时循环执行几次？",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "answerLeakRisk": "LOW"
                    }
                  }
                }
                """
        );

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getIssueTags()).containsExactly("LOOP_BOUNDARY");
        assertThat(analysis.getFineGrainedTags()).containsExactly("OFF_BY_ONE");
        assertThat(analysis.getStudentHint()).contains("n=1");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo("diagnosis-and-teaching-v3");
        assertThat(analysis.getAiInvocation().getRuntimeMode()).isEqualTo("single-call");
        assertThat(analysis.getAiInvocation().getFailureStage()).isEmpty();
        assertThat(analysis.getAiInvocation().getFailureReason()).isEmpty();
        assertThat(service.callCount()).isEqualTo(1);
    }

    @Test
    void lowLatencyRuntimeProfileCompactsRequestAndStillCompletes() throws Exception {
        StubAiReportService standardService = newService(validSingleCallResponse());
        CapturingAiReportService lowLatencyService = new CapturingAiReportService(objectMapper, runtime(), validSingleCallApiResponse());
        ReflectionTestUtils.setField(lowLatencyService, "enabled", true);
        ReflectionTestUtils.setField(lowLatencyService, "apiKey", "test-key");
        ReflectionTestUtils.setField(lowLatencyService, "baseUrl", "https://example.test/v1");
        ReflectionTestUtils.setField(lowLatencyService, "model", "test-model");
        ReflectionTestUtils.setField(lowLatencyService, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(lowLatencyService, "externalRuntimeMode", "single-call");
        ReflectionTestUtils.setField(lowLatencyService, "externalRuntimeProfile", "low-latency");
        ReflectionTestUtils.setField(lowLatencyService, "maxOutputTokens", 900);
        ReflectionTestUtils.setField(lowLatencyService, "streamEnabled", false);
        ReflectionTestUtils.setField(lowLatencyService, "streamFallbackEnabled", false);

        SubmissionAnalysisResponse standardAnalysis = standardService.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackageWithLargeContext(),
                richRuleSignals()
        );
        SubmissionAnalysisResponse lowLatencyAnalysis = lowLatencyService.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackageWithLargeContext(),
                richRuleSignals()
        );

        int standardRequestBytes = standardService.lastUserPromptBytes();
        int lowLatencyRequestBytes = lowLatencyService.lastRequestBytes();
        JsonNode lowLatencyRequest = objectMapper.readTree(lowLatencyService.lastRequestBody());
        String lowLatencyUserContent = lowLatencyRequest.path("messages").path(1).path("content").asText();
        JsonNode runtimePayload = objectMapper.readTree(lowLatencyUserContent);

        assertThat(standardAnalysis.getAiInvocation().getRuntimeProfile()).isEqualTo("standard");
        assertThat(lowLatencyAnalysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
        assertThat(lowLatencyAnalysis.getAiInvocation().getRuntimeProfile()).isEqualTo("low-latency");
        assertThat(lowLatencyAnalysis.getAiInvocation().getRequestCompact()).isTrue();
        assertThat(lowLatencyAnalysis.getAiInvocation().getRequestBytes()).isEqualTo(lowLatencyRequestBytes);
        assertThat(lowLatencyRequestBytes).isLessThan(standardRequestBytes);
        assertThat(runtimePayload.path("brief").path("candidateSignals")).hasSize(3);
        assertThat(runtimePayload.path("brief").path("evidenceRefs"))
                .anySatisfy(ref -> assertThat(ref.asText()).isEqualTo("code:range_excludes_n"));
        assertThat(runtimePayload.path("brief").path("allowedIssueTags"))
                .anySatisfy(tag -> assertThat(tag.asText()).isEqualTo("LOOP_BOUNDARY"));
        assertThat(runtimePayload.path("standardLibrary").path("issueTags").path(0).path("id").asText())
                .isNotBlank();
        assertThat(runtimePayload.path("standardLibrary").path("issueTags").path(0).path("studentExplanation").isMissingNode())
                .isTrue();
        assertThat(runtimePayload.path("standardLibrary").path("teachingActions").path(0).path("id").asText())
                .isNotBlank();
        assertThat(runtimePayload.path("standardLibrary").path("teachingActions").path(0).path("studentTaskTemplate").isMissingNode())
                .isTrue();
    }

    @Test
    void lowLatencyRuntimeProfileDoesNotBypassValidation() {
        StubAiReportService service = newService(
                """
                {
                  "diagnosisDecision": {
                    "primaryIssueTag": "MADE_UP_TAG",
                    "fineGrainedTag": "OFF_BY_ONE",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "confidence": 0.5,
                    "uncertainty": "bad tag",
                    "needsMoreEvidence": false,
                    "answerLeakRisk": "LOW"
                  },
                  "teachingHint": {
                    "studentHint": "先看 range 边界。",
                    "studentHintPlan": {
                      "hintLevel": "L2",
                      "problemType": "循环边界",
                      "evidenceAnchor": "code:range_excludes_n",
                      "nextAction": "列出 range 产生的 i。",
                      "coachQuestion": "右边界包含 n 吗？",
                      "teachingAction": "TRACE_VARIABLES",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "answerLeakRisk": "LOW"
                    },
                    "learningInterventionPlan": {
                      "interventionType": "VARIABLE_TRACE",
                      "goal": "确认循环边界。",
                      "studentTask": "手推最小样例。",
                      "checkQuestion": "是否处理到 n？",
                      "completionSignal": "能指出缺失位置。",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "estimatedMinutes": 5,
                      "answerLeakRisk": "LOW"
                    },
                    "teacherNote": "invalid diagnosis should fall back",
                    "answerLeakRisk": "LOW"
                  },
                  "studentFeedback": {
                    "summary": "这次主要问题是循环边界没有覆盖到题目要求的范围。",
                    "blockingIssues": [{
                      "priority": 1,
                      "title": "当前最需要先处理的问题",
                      "studentMessage": "循环边界和题目要求的闭区间不一致，先手推最小样例确认缺失位置。",
                      "evidence": "code:range_excludes_n",
                      "nextAction": "列出 range 产生的 i。",
                      "issueTag": "LOOP_BOUNDARY",
                      "fineGrainedTag": "OFF_BY_ONE",
                      "evidenceRefs": ["code:range_excludes_n"]
                    }],
                    "secondaryIssues": [],
                    "improvementOpportunities": [{
                      "category": "TESTING_HABIT",
                      "studentMessage": "通过后补一个 n=1 的最小自测。",
                      "benefit": "能提前发现边界遗漏。",
                      "evidenceRefs": ["code:range_excludes_n"]
                    }],
                    "nextLearningAction": {
                      "hintLevel": "L2",
                      "action": "TRACE_VARIABLES",
                      "task": "列出 range 产生的 i。",
                      "checkQuestion": "当 n=1 时循环执行几次？",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "answerLeakRisk": "LOW"
                    }
                  }
                }
                """
        );
        ReflectionTestUtils.setField(service, "externalRuntimeProfile", "low-latency");

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackageWithLargeContext(),
                richRuleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("RULE_BASED_V1");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_RUNTIME_FALLBACK");
        assertThat(analysis.getAiInvocation().getRuntimeProfile()).isEqualTo("low-latency");
        assertThat(analysis.getAiInvocation().getRequestCompact()).isTrue();
        assertThat(analysis.getAiInvocation().getFailureReason()).isEqualTo("INVALID_TAG");
    }

    @Test
    void runtimeAlignsGenericCollectEvidenceActionToConcreteDiagnosisAction() {
        StubAiReportService service = newService(
                """
                {
                  "diagnosisDecision": {
                    "primaryIssueTag": "TIME_COMPLEXITY",
                    "fineGrainedTag": "OVER_SIMULATION",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "confidence": 0.9,
                    "uncertainty": "大规模逐步模拟证据明确。",
                    "needsMoreEvidence": false,
                    "answerLeakRisk": "LOW"
                  },
                  "teachingHint": {
                    "studentHint": "请先根据证据做一次验证。",
                    "studentHintPlan": {
                      "hintLevel": "L2",
                      "problemType": "复杂度",
                      "evidenceAnchor": "code:range_excludes_n",
                      "nextAction": "构造一个最小样例。",
                      "coachQuestion": "你准备验证什么？",
                      "teachingAction": "COLLECT_EVIDENCE",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "answerLeakRisk": "LOW"
                    },
                    "learningInterventionPlan": {
                      "interventionType": "COLLECT_EVIDENCE",
                      "goal": "确认问题。",
                      "studentTask": "补充证据。",
                      "checkQuestion": "证据是什么？",
                      "completionSignal": "能说出证据。",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "estimatedMinutes": 5,
                      "answerLeakRisk": "LOW"
                    },
                    "teacherNote": "模型给了泛化教学动作，但诊断已经足够明确。",
                    "answerLeakRisk": "LOW"
                  },
                  "studentFeedback": {
                    "summary": "这次主要问题是循环边界没有覆盖到题目要求的范围。",
                    "blockingIssues": [{
                      "priority": 1,
                      "title": "当前最需要先处理的问题",
                      "studentMessage": "循环边界和题目要求的闭区间不一致，先手推最小样例确认缺失位置。",
                      "evidence": "code:range_excludes_n",
                      "nextAction": "列出 range 产生的 i。",
                      "issueTag": "LOOP_BOUNDARY",
                      "fineGrainedTag": "OFF_BY_ONE",
                      "evidenceRefs": ["code:range_excludes_n"]
                    }],
                    "secondaryIssues": [],
                    "improvementOpportunities": [{
                      "category": "TESTING_HABIT",
                      "studentMessage": "通过后补一个 n=1 的最小自测。",
                      "benefit": "能提前发现边界遗漏。",
                      "evidenceRefs": ["code:range_excludes_n"]
                    }],
                    "nextLearningAction": {
                      "hintLevel": "L2",
                      "action": "TRACE_VARIABLES",
                      "task": "列出 range 产生的 i。",
                      "checkQuestion": "当 n=1 时循环执行几次？",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "answerLeakRisk": "LOW"
                    }
                  }
                }
                """
        );

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                scaleFallback(),
                evidencePackage(),
                scaleRuleSignals()
        );

        assertThat(analysis.getIssueTags()).containsExactly("TIME_COMPLEXITY");
        assertThat(analysis.getFineGrainedTags()).containsExactly("OVER_SIMULATION");
        assertThat(analysis.getStudentHintPlan().getTeachingAction()).isEqualTo("COUNT_COMPLEXITY");
        assertThat(analysis.getStudentHintPlan().getNextAction()).contains("最大规模");
        assertThat(analysis.getLearningInterventionPlan().getInterventionType()).isEqualTo("COMPLEXITY_ESTIMATE");
        assertThat(analysis.getLearningInterventionPlan().getStudentTask()).contains("最大规模");
    }

    @Test
    void singleCallRuntimeRetainsDiagnosisWhenTeachingHintIsUnsafe() {
        StubAiReportService service = newService(
                """
                {
                  "diagnosisDecision": {
                    "primaryIssueTag": "LOOP_BOUNDARY",
                    "fineGrainedTag": "OFF_BY_ONE",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "confidence": 0.9,
                    "uncertainty": "range 右边界证据明确。",
                    "needsMoreEvidence": false,
                    "answerLeakRisk": "LOW"
                  },
                  "teachingHint": {
                    "studentHint": "完整代码如下：def solve(): pass",
                    "studentHintPlan": {
                      "hintLevel": "L4",
                      "problemType": "循环边界",
                      "evidenceAnchor": "code:range_excludes_n",
                      "nextAction": "复制完整代码",
                      "coachQuestion": "无",
                      "teachingAction": "TRACE_VARIABLES",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "answerLeakRisk": "HIGH"
                    },
                    "learningInterventionPlan": {
                      "interventionType": "VARIABLE_TRACE",
                      "goal": "直接给答案",
                      "studentTask": "复制答案",
                      "checkQuestion": "无",
                      "completionSignal": "复制完成",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "estimatedMinutes": 1,
                      "answerLeakRisk": "HIGH"
                    },
                    "teacherNote": "unsafe",
                    "answerLeakRisk": "HIGH"
                  }
                }
                """
        );
        ReflectionTestUtils.setField(service, "externalRuntimeMode", "single-call");

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getIssueTags()).containsExactly("LOOP_BOUNDARY");
        assertThat(analysis.getFineGrainedTags()).containsExactly("OFF_BY_ONE");
        assertThat(analysis.getStudentHint()).doesNotContain("def solve").contains("先不要大改代码");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_PARTIAL_COMPLETED");
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo("diagnosis-and-teaching-v3");
        assertThat(analysis.getAiInvocation().getRuntimeMode()).isEqualTo("single-call");
        assertThat(analysis.getAiInvocation().getFailureStage()).isEqualTo("DIAGNOSIS_AND_TEACHING");
        assertThat(analysis.getAiInvocation().getFailureReason()).isEqualTo("SAFETY_RISK");
        assertThat(analysis.getUncertainty()).contains("DIAGNOSIS_AND_TEACHING").contains("SAFETY_RISK");
        assertThat(service.callCount()).isEqualTo(1);
    }

    @Test
    void externalRuntimeFallsBackWhenDiagnosisTagIsInvalid() {
        StubAiReportService service = newService(
                """
                {
                  "primaryIssueTag": "MADE_UP_TAG",
                  "fineGrainedTag": "OFF_BY_ONE",
                  "evidenceRefs": ["code:range_excludes_n"],
                  "confidence": 0.5,
                  "uncertainty": "bad tag",
                  "needsMoreEvidence": false,
                  "answerLeakRisk": "LOW"
                }
                """,
                "{}"
        );
        ReflectionTestUtils.setField(service, "externalRuntimeMode", "staged");

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("RULE_BASED_V1");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_RUNTIME_FALLBACK");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isTrue();
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo("diagnosis-judge-v2+teaching-hint-v1");
        assertThat(analysis.getAiInvocation().getRuntimeMode()).isEqualTo("staged");
        assertThat(analysis.getAiInvocation().getFailureStage()).isEqualTo("DIAGNOSIS_JUDGE");
        assertThat(analysis.getAiInvocation().getFailureReason()).isEqualTo("INVALID_TAG");
        assertThat(analysis.getUncertainty()).contains("INVALID_TAG");
        assertThat(service.callCount()).isEqualTo(1);
    }

    @Test
    void singleCallRuntimeFallbackRecordsSingleCallPromptVersionWhenDiagnosisIsInvalid() {
        StubAiReportService service = newService(
                """
                {
                  "diagnosisDecision": {
                    "primaryIssueTag": "MADE_UP_TAG",
                    "fineGrainedTag": "OFF_BY_ONE",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "confidence": 0.5,
                    "uncertainty": "bad tag",
                    "needsMoreEvidence": false,
                    "answerLeakRisk": "LOW"
                  },
                  "teachingHint": {
                    "studentHint": "先看 range 边界。",
                    "studentHintPlan": {
                      "hintLevel": "L2",
                      "problemType": "循环边界",
                      "evidenceAnchor": "code:range_excludes_n",
                      "nextAction": "列出 range 产生的 i。",
                      "coachQuestion": "右边界包含 n 吗？",
                      "teachingAction": "TRACE_VARIABLES",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "answerLeakRisk": "LOW"
                    },
                    "learningInterventionPlan": {
                      "interventionType": "VARIABLE_TRACE",
                      "goal": "确认循环边界。",
                      "studentTask": "手推最小样例。",
                      "checkQuestion": "是否处理到 n？",
                      "completionSignal": "能指出缺失位置。",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "estimatedMinutes": 5,
                      "answerLeakRisk": "LOW"
                    },
                    "teacherNote": "invalid diagnosis should fall back",
                    "answerLeakRisk": "LOW"
                  },
                  "studentFeedback": {
                    "summary": "这次主要问题是循环边界没有覆盖到题目要求的范围。",
                    "blockingIssues": [{
                      "priority": 1,
                      "title": "当前最需要先处理的问题",
                      "studentMessage": "循环边界和题目要求的闭区间不一致，先手推最小样例确认缺失位置。",
                      "evidence": "code:range_excludes_n",
                      "nextAction": "列出 range 产生的 i。",
                      "issueTag": "LOOP_BOUNDARY",
                      "fineGrainedTag": "OFF_BY_ONE",
                      "evidenceRefs": ["code:range_excludes_n"]
                    }],
                    "secondaryIssues": [],
                    "improvementOpportunities": [{
                      "category": "TESTING_HABIT",
                      "studentMessage": "通过后补一个 n=1 的最小自测。",
                      "benefit": "能提前发现边界遗漏。",
                      "evidenceRefs": ["code:range_excludes_n"]
                    }],
                    "nextLearningAction": {
                      "hintLevel": "L2",
                      "action": "TRACE_VARIABLES",
                      "task": "列出 range 产生的 i。",
                      "checkQuestion": "当 n=1 时循环执行几次？",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "answerLeakRisk": "LOW"
                    }
                  }
                }
                """
        );

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("RULE_BASED_V1");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_RUNTIME_FALLBACK");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isTrue();
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo("diagnosis-and-teaching-v3");
        assertThat(analysis.getAiInvocation().getRuntimeMode()).isEqualTo("single-call");
        assertThat(analysis.getAiInvocation().getFailureStage()).isEqualTo("DIAGNOSIS_AND_TEACHING");
        assertThat(analysis.getAiInvocation().getFailureReason()).isEqualTo("INVALID_TAG");
        assertThat(analysis.getUncertainty()).contains("DIAGNOSIS_AND_TEACHING").contains("INVALID_TAG");
        assertThat(service.callCount()).isEqualTo(1);
    }

    @Test
    void externalRuntimeRetainsDiagnosisWhenTeachingCallFails() {
        StubAiReportService service = newService(
                """
                {
                  "primaryIssueTag": "LOOP_BOUNDARY",
                  "fineGrainedTag": "OFF_BY_ONE",
                  "evidenceRefs": ["code:range_excludes_n"],
                  "confidence": 0.91,
                  "uncertainty": "ok",
                  "needsMoreEvidence": false,
                  "answerLeakRisk": "LOW"
                }
                """
        );
        ReflectionTestUtils.setField(service, "externalRuntimeMode", "staged");

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("MODEL_SCOPE_EXTERNAL_MODEL");
        assertThat(analysis.getIssueTags()).containsExactly("LOOP_BOUNDARY");
        assertThat(analysis.getFineGrainedTags()).containsExactly("OFF_BY_ONE");
        assertThat(analysis.getEvidenceRefs()).contains("code:range_excludes_n");
        assertThat(analysis.getStudentHintPlan()).isNotNull();
        assertThat(analysis.getStudentHintPlan().getTeachingAction()).isEqualTo("TRACE_VARIABLES");
        assertThat(analysis.getStudentHintPlan().getAnswerLeakRisk()).isEqualTo("LOW");
        assertThat(analysis.getLearningInterventionPlan()).isNotNull();
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_PARTIAL_COMPLETED");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isFalse();
        assertThat(analysis.getAiInvocation().getRuntimeMode()).isEqualTo("staged");
        assertThat(analysis.getAiInvocation().getFailureStage()).isEqualTo("TEACHING_HINT");
        assertThat(analysis.getAiInvocation().getFailureReason()).isEqualTo("API_ERROR");
        assertThat(analysis.getUncertainty()).contains("TEACHING_HINT").contains("API_ERROR");
        assertThat(service.callCount()).isEqualTo(2);
    }

    @Test
    void externalRuntimeRetainsDiagnosisWhenTeachingOutputIsUnsafe() {
        StubAiReportService service = newService(
                """
                {
                  "primaryIssueTag": "LOOP_BOUNDARY",
                  "fineGrainedTag": "OFF_BY_ONE",
                  "evidenceRefs": ["code:range_excludes_n"],
                  "confidence": 0.91,
                  "uncertainty": "ok",
                  "needsMoreEvidence": false,
                  "answerLeakRisk": "LOW"
                }
                """,
                """
                {
                  "studentHint": "完整代码如下：def solve(): pass",
                  "studentHintPlan": {
                    "hintLevel": "L4",
                    "problemType": "循环边界",
                    "evidenceAnchor": "code:range_excludes_n",
                    "nextAction": "复制完整代码",
                    "coachQuestion": "无",
                    "teachingAction": "TRACE_VARIABLES",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "answerLeakRisk": "HIGH"
                  },
                  "learningInterventionPlan": {
                    "interventionType": "VARIABLE_TRACE",
                    "goal": "直接给答案",
                    "studentTask": "复制答案",
                    "checkQuestion": "无",
                    "completionSignal": "复制完成",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "estimatedMinutes": 1,
                    "answerLeakRisk": "HIGH"
                  },
                  "teacherNote": "unsafe",
                  "answerLeakRisk": "HIGH"
                }
                """
        );
        ReflectionTestUtils.setField(service, "externalRuntimeMode", "staged");

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("MODEL_SCOPE_EXTERNAL_MODEL");
        assertThat(analysis.getIssueTags()).containsExactly("LOOP_BOUNDARY");
        assertThat(analysis.getFineGrainedTags()).containsExactly("OFF_BY_ONE");
        assertThat(analysis.getStudentHint()).doesNotContain("def solve").doesNotContain("pass");
        assertThat(analysis.getStudentHintPlan()).isNotNull();
        assertThat(analysis.getStudentHintPlan().getHintLevel()).isEqualTo("L2");
        assertThat(analysis.getStudentHintPlan().getTeachingAction()).isEqualTo("TRACE_VARIABLES");
        assertThat(analysis.getStudentHintPlan().getAnswerLeakRisk()).isEqualTo("LOW");
        assertThat(analysis.getLearningInterventionPlan()).isNotNull();
        assertThat(analysis.getLearningInterventionPlan().getAnswerLeakRisk()).isEqualTo("LOW");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_PARTIAL_COMPLETED");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isFalse();
        assertThat(analysis.getAiInvocation().getRuntimeMode()).isEqualTo("staged");
        assertThat(analysis.getAiInvocation().getFailureStage()).isEqualTo("TEACHING_HINT");
        assertThat(analysis.getAiInvocation().getFailureReason()).isEqualTo("SAFETY_RISK");
        assertThat(analysis.getUncertainty()).contains("TEACHING_HINT").contains("SAFETY_RISK");
        assertThat(service.callCount()).isEqualTo(2);
    }

    @Test
    void disabledExternalRuntimeUsesLegacyLongPromptPath() {
        StubAiReportService service = newService(
                """
                {
                  "headline": "模型长 prompt 诊断",
                  "summary": "旧路径仍可作为配置回滚。",
                  "issueTags": ["LOOP_BOUNDARY"],
                  "fineGrainedTags": ["OFF_BY_ONE"],
                  "abilityPoints": ["循环边界"],
                  "focusPoints": ["range 右边界"],
                  "fixDirections": ["手推最小样例"],
                  "evidenceRefs": ["code:range_excludes_n"],
                  "studentHint": "旧路径提示",
                  "studentHintPlan": {
                    "hintLevel": "L2",
                    "problemType": "循环边界",
                    "evidenceAnchor": "code:range_excludes_n",
                    "nextAction": "手推 range",
                    "coachQuestion": "右边界包含吗？",
                    "teachingAction": "TRACE_VARIABLES",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "answerLeakRisk": "LOW"
                  },
                  "learningInterventionPlan": {
                    "interventionType": "VARIABLE_TRACE",
                    "goal": "确认边界",
                    "studentTask": "列变量表",
                    "checkQuestion": "是否处理 n",
                    "completionSignal": "写出变量表",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "estimatedMinutes": 5,
                    "answerLeakRisk": "LOW"
                  },
                  "teacherNote": "legacy",
                  "progressSignal": "current",
                  "confidence": 0.8,
                  "uncertainty": "仍需结合隐藏测试。",
                  "answerLeakRisk": "LOW",
                  "wrongSolution": null,
                  "correctSolution": null,
                  "lineIssues": [],
                  "reportMarkdown": "旧路径报告"
                }
                """
        );
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", false);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("MODEL_SCOPE_EXTERNAL_MODEL");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo("submission-diagnosis-prompt-v2");
        assertThat(analysis.getAiInvocation().getRuntimeMode()).isEqualTo("legacy-long-prompt");
        assertThat(service.callCount()).isEqualTo(1);
    }

    @Test
    void diagnosticAgentPreservesRuntimeFallbackStatus() {
        StubAiReportService service = newService(
                """
                {
                  "diagnosisDecision": {
                    "primaryIssueTag": "MADE_UP_TAG",
                    "fineGrainedTag": "OFF_BY_ONE",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "confidence": 0.5,
                    "uncertainty": "bad tag",
                    "needsMoreEvidence": false,
                    "answerLeakRisk": "LOW"
                  },
                  "teachingHint": {
                    "studentHint": "先看 range 边界。",
                    "studentHintPlan": {
                      "hintLevel": "L2",
                      "problemType": "循环边界",
                      "evidenceAnchor": "code:range_excludes_n",
                      "nextAction": "列出 range 产生的 i。",
                      "coachQuestion": "右边界包含 n 吗？",
                      "teachingAction": "TRACE_VARIABLES",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "answerLeakRisk": "LOW"
                    },
                    "learningInterventionPlan": {
                      "interventionType": "VARIABLE_TRACE",
                      "goal": "确认循环边界。",
                      "studentTask": "手推最小样例。",
                      "checkQuestion": "是否处理到 n？",
                      "completionSignal": "能指出缺失位置。",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "estimatedMinutes": 5,
                      "answerLeakRisk": "LOW"
                    },
                    "teacherNote": "invalid diagnosis should fall back",
                    "answerLeakRisk": "LOW"
                  },
                  "studentFeedback": {
                    "summary": "这次主要问题是循环边界没有覆盖到题目要求的范围。",
                    "blockingIssues": [{
                      "priority": 1,
                      "title": "当前最需要先处理的问题",
                      "studentMessage": "循环边界和题目要求的闭区间不一致，先手推最小样例确认缺失位置。",
                      "evidence": "code:range_excludes_n",
                      "nextAction": "列出 range 产生的 i。",
                      "issueTag": "LOOP_BOUNDARY",
                      "fineGrainedTag": "OFF_BY_ONE",
                      "evidenceRefs": ["code:range_excludes_n"]
                    }],
                    "secondaryIssues": [],
                    "improvementOpportunities": [{
                      "category": "TESTING_HABIT",
                      "studentMessage": "通过后补一个 n=1 的最小自测。",
                      "benefit": "能提前发现边界遗漏。",
                      "evidenceRefs": ["code:range_excludes_n"]
                    }],
                    "nextLearningAction": {
                      "hintLevel": "L2",
                      "action": "TRACE_VARIABLES",
                      "task": "列出 range 产生的 i。",
                      "checkQuestion": "当 n=1 时循环执行几次？",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "answerLeakRisk": "LOW"
                    }
                  }
                }
                """
        );
        DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();
        DiagnosticAgentService diagnosticAgentService = new DiagnosticAgentService(
                new DiagnosisEvidencePackageBuilder(),
                new RuleSignalAnalyzer(),
                service,
                new com.onlinejudge.classroom.application.HintSafetyService(null, objectMapper, taxonomy),
                taxonomy
        );

        DiagnosticAgentService.AgentResult result = diagnosticAgentService.diagnose(
                problem(),
                submission(),
                List.of(),
                fallback(),
                com.onlinejudge.classroom.domain.Assignment.HintPolicy.L2
        );

        assertThat(result.analysis().getAiInvocation().isFallbackUsed()).isTrue();
        assertThat(result.analysis().getAiInvocation().getStatus()).isEqualTo("MODEL_RUNTIME_FALLBACK");
        assertThat(result.analysis().getAiInvocation().getRuntimeMode()).isEqualTo("single-call");
        assertThat(result.analysis().getAiInvocation().getRuntimeProfile()).isEqualTo("standard");
        assertThat(result.analysis().getAiInvocation().getRequestBytes()).isZero();
        assertThat(result.analysis().getAiInvocation().getRequestCompact()).isFalse();
        assertThat(result.analysis().getAiInvocation().getFailureStage()).isEqualTo("DIAGNOSIS_AND_TEACHING");
        assertThat(result.analysis().getAiInvocation().getFailureReason()).isEqualTo("INVALID_TAG");
        assertThat(result.traceSummary()).contains("model=rule-fallback");
    }

    @Test
    void chatCompletionFallsBackToStreamingWhenNonStreamResponseHasNoChoices() throws Exception {
        StreamingFallbackAiReportService service = new StreamingFallbackAiReportService(objectMapper);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://example.test/v1");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 5L);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 128);
        ReflectionTestUtils.setField(service, "streamEnabled", false);
        ReflectionTestUtils.setField(service, "streamFallbackEnabled", true);

        String content = service.chatCompletion("system", "user");

        assertThat(content).contains("\"diagnosisDecision\"", "\"primaryIssueTag\":\"LOOP_BOUNDARY\"");
        assertThat(content).doesNotContain("先判断");
        assertThat(service.streamFlags()).containsExactly(false, true);
    }

    @Test
    void singleCallRuntimeRecordsStreamingTelemetryWithoutReasoningLeakage() {
        StreamingFallbackAiReportService service = new StreamingFallbackAiReportService(objectMapper);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://example.test/v1");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(service, "externalRuntimeMode", "single-call");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 5L);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 128);
        ReflectionTestUtils.setField(service, "streamEnabled", false);
        ReflectionTestUtils.setField(service, "streamFallbackEnabled", true);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getIssueTags()).containsExactly("LOOP_BOUNDARY");
        assertThat(analysis.getAiInvocation().getTransportMode()).isEqualTo("stream");
        assertThat(analysis.getAiInvocation().getStreamChunkCount()).isEqualTo(3);
        assertThat(analysis.getAiInvocation().getStreamContentChunkCount()).isEqualTo(1);
        assertThat(analysis.getAiInvocation().getStreamReasoningChunkCount()).isEqualTo(1);
        assertThat(analysis.getAiInvocation().getStreamInvalidChunkCount()).isEqualTo(1);
        assertThat(analysis.getAiInvocation().getStreamFinishReason()).isEqualTo("stop");
        assertThat(analysis.getAiInvocation().getStreamFallbackRetryUsed()).isTrue();
        assertThat(analysis.getReportMarkdown()).doesNotContain("先判断");
        assertThat(service.streamFlags()).containsExactly(false, true);
    }

    @Test
    void singleCallRuntimeClassifiesLengthFinishAsOutputTruncated() {
        LengthTruncatedAiReportService service = new LengthTruncatedAiReportService(objectMapper);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://example.test/v1");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(service, "externalRuntimeMode", "single-call");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 5L);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 384);
        ReflectionTestUtils.setField(service, "streamEnabled", true);
        ReflectionTestUtils.setField(service, "streamFallbackEnabled", true);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_RUNTIME_FALLBACK");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isTrue();
        assertThat(analysis.getAiInvocation().getFailureReason()).isEqualTo("OUTPUT_TRUNCATED");
        assertThat(analysis.getAiInvocation().getFailureStage()).isEqualTo("DIAGNOSIS_AND_TEACHING");
        assertThat(analysis.getAiInvocation().getTransportMode()).isEqualTo("stream");
        assertThat(analysis.getAiInvocation().getStreamContentChunkCount()).isEqualTo(1);
        assertThat(analysis.getAiInvocation().getStreamFinishReason()).isEqualTo("length");
        assertThat(analysis.getUncertainty()).contains("OUTPUT_TRUNCATED");
    }

    @Test
    void singleCallRuntimeRetainsValidDiagnosisWhenTeachingHintIsTruncated() {
        TeachingHintTruncatedAiReportService service = new TeachingHintTruncatedAiReportService(objectMapper, true);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://example.test/v1");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(service, "externalRuntimeMode", "single-call");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 5L);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 384);
        ReflectionTestUtils.setField(service, "streamEnabled", true);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("MODEL_SCOPE_EXTERNAL_MODEL");
        assertThat(analysis.getIssueTags()).containsExactly("LOOP_BOUNDARY");
        assertThat(analysis.getFineGrainedTags()).containsExactly("OFF_BY_ONE");
        assertThat(analysis.getEvidenceRefs()).contains("code:range_excludes_n");
        assertThat(analysis.getStudentHint()).contains("先不要大改代码").doesNotContain("截断残留");
        assertThat(analysis.getStudentHintPlan().getTeachingAction()).isEqualTo("TRACE_VARIABLES");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_PARTIAL_COMPLETED");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isFalse();
        assertThat(analysis.getAiInvocation().getFailureStage()).isEqualTo("DIAGNOSIS_AND_TEACHING");
        assertThat(analysis.getAiInvocation().getFailureReason()).isEqualTo("OUTPUT_TRUNCATED");
        assertThat(analysis.getAiInvocation().getTransportMode()).isEqualTo("stream");
        assertThat(analysis.getAiInvocation().getStreamContentChunkCount()).isEqualTo(1);
        assertThat(analysis.getAiInvocation().getStreamFinishReason()).isEqualTo("length");
        assertThat(analysis.getUncertainty()).contains("OUTPUT_TRUNCATED");
    }

    @Test
    void singleCallRuntimeDoesNotRetainInvalidDiagnosisFromTruncatedOutput() {
        TeachingHintTruncatedAiReportService service = new TeachingHintTruncatedAiReportService(objectMapper, false);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://example.test/v1");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(service, "externalRuntimeMode", "single-call");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 5L);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 384);
        ReflectionTestUtils.setField(service, "streamEnabled", true);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getSourceType()).isEqualTo("RULE_BASED_V1");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_RUNTIME_FALLBACK");
        assertThat(analysis.getAiInvocation().isFallbackUsed()).isTrue();
        assertThat(analysis.getAiInvocation().getFailureReason()).isEqualTo("OUTPUT_TRUNCATED");
        assertThat(analysis.getUncertainty()).contains("OUTPUT_TRUNCATED");
    }

    @Test
    void chatCompletionRetriesRateLimitAndThenSucceeds() throws Exception {
        RetryingAiReportService service = new RetryingAiReportService(
                objectMapper,
                new IOException("AI API returned status 429: {\"error\":{\"message\":\"rate limit\"}}"),
                """
                {"choices":[{"message":{"content":"{\\"primaryIssueTag\\":\\"LOOP_BOUNDARY\\"}"}}]}
                """
        );
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://example.test/v1");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 5L);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 128);
        ReflectionTestUtils.setField(service, "streamEnabled", false);
        ReflectionTestUtils.setField(service, "streamFallbackEnabled", false);
        ReflectionTestUtils.setField(service, "retryMaxAttempts", 2);
        ReflectionTestUtils.setField(service, "retryBackoffMs", 0L);

        String content = service.chatCompletion("system", "user");

        assertThat(content).isEqualTo("{\"primaryIssueTag\":\"LOOP_BOUNDARY\"}");
        assertThat(service.callCount()).isEqualTo(2);
    }

    @Test
    void growthReportFallbackKeepsExternalFailureReason() {
        FailingGrowthReportAiReportService service = new FailingGrowthReportAiReportService(
                objectMapper,
                new IOException("AI API returned status 429: {\"error\":{\"code\":\"insufficient_quota\"}}")
        );
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");

        String markdown = service.enhanceGrowthReportMarkdown(problem(), List.of(), "# 成长报告");

        assertThat(markdown).contains("# 成长报告");
        assertThat(markdown).contains("AI_FAILURE").contains("GROWTH_REPORT").contains("INSUFFICIENT_QUOTA");
    }

    @Test
    void budgetGuardShortCircuitsAfterQuotaFailureAcrossSubmissionAndGrowthReport() {
        ExternalModelBudgetGuard budgetGuard = new ExternalModelBudgetGuard();
        StubAiReportService service = new StubAiReportService(
                objectMapper,
                runtime(),
                budgetGuard,
                new IOException("AI API returned status 429: {\"error\":{\"code\":\"insufficient_quota\"}}")
        );
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 900);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_RUNTIME_FALLBACK");
        assertThat(analysis.getAiInvocation().getRuntimeMode()).isEqualTo("single-call");
        assertThat(analysis.getAiInvocation().getFailureStage()).isEqualTo("DIAGNOSIS_AND_TEACHING");
        assertThat(analysis.getAiInvocation().getFailureReason()).isEqualTo("INSUFFICIENT_QUOTA");
        assertThat(analysis.getAiInvocation().getTransportMode()).isEmpty();
        assertThat(analysis.getUncertainty()).contains("INSUFFICIENT_QUOTA");
        assertThat(service.callCount()).isEqualTo(1);

        String markdown = service.enhanceGrowthReportMarkdown(problem(), List.of(), "# 成长报告");

        assertThat(markdown).contains("BUDGET_GUARD_OPEN");
        assertThat(service.callCount()).isEqualTo(1);
    }

    private StubAiReportService newService(String... responses) {
        StubAiReportService service = new StubAiReportService(objectMapper, runtime(), responses);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 900);
        return service;
    }

    private String validSingleCallResponse() {
        return """
                {
                  "diagnosisDecision": {
                    "primaryIssueTag": "LOOP_BOUNDARY",
                    "fineGrainedTag": "OFF_BY_ONE",
                    "evidenceRefs": ["code:range_excludes_n"],
                    "confidence": 0.9,
                    "uncertainty": "range 右边界证据明确。",
                    "needsMoreEvidence": false,
                    "answerLeakRisk": "LOW"
                  },
                  "teachingHint": {
                    "studentHint": "先用 n=1 手推循环会不会执行。",
                    "studentHintPlan": {
                      "hintLevel": "L2",
                      "problemType": "循环边界",
                      "evidenceAnchor": "code:range_excludes_n",
                      "nextAction": "列出 range 产生的 i。",
                      "coachQuestion": "当 n=1 时循环执行几次？",
                      "teachingAction": "TRACE_VARIABLES",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "answerLeakRisk": "LOW"
                    },
                    "learningInterventionPlan": {
                      "interventionType": "VARIABLE_TRACE",
                      "goal": "确认循环是否覆盖 n。",
                      "studentTask": "手推 n=1 和 n=2。",
                      "checkQuestion": "最后一次循环是否处理到 n？",
                      "completionSignal": "能写出 i 的取值表。",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "estimatedMinutes": 6,
                      "answerLeakRisk": "LOW"
                    },
                    "teacherNote": "单次调用已给出安全教学提示。",
                    "answerLeakRisk": "LOW"
                  },
                  "studentFeedback": {
                    "summary": "这次主要问题是循环边界没有覆盖到题目要求的范围。",
                    "blockingIssues": [{
                      "priority": 1,
                      "title": "当前最需要先处理的问题",
                      "studentMessage": "循环边界和题目要求的闭区间不一致，先手推最小样例确认缺失位置。",
                      "evidence": "code:range_excludes_n",
                      "nextAction": "列出 range 产生的 i。",
                      "issueTag": "LOOP_BOUNDARY",
                      "fineGrainedTag": "OFF_BY_ONE",
                      "evidenceRefs": ["code:range_excludes_n"]
                    }],
                    "secondaryIssues": [],
                    "improvementOpportunities": [{
                      "category": "TESTING_HABIT",
                      "studentMessage": "通过后补一个 n=1 的最小自测。",
                      "benefit": "能提前发现边界遗漏。",
                      "evidenceRefs": ["code:range_excludes_n"]
                    }],
                    "nextLearningAction": {
                      "hintLevel": "L2",
                      "action": "TRACE_VARIABLES",
                      "task": "列出 range 产生的 i。",
                      "checkQuestion": "当 n=1 时循环执行几次？",
                      "evidenceRefs": ["code:range_excludes_n"],
                      "answerLeakRisk": "LOW"
                    }
                  }
                }
                """;
    }

    private String validSingleCallApiResponse() {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "choices",
                    List.of(java.util.Map.of(
                            "message", java.util.Map.of("content", validSingleCallResponse()),
                            "finish_reason", "stop"
                    ))
            ));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ExternalModelAgentRuntime runtime() {
        DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();
        return new ExternalModelAgentRuntime(
                new ModelDiagnosisBriefBuilder(),
                new StandardLibraryPackBuilder(taxonomy),
                new PromptTemplateRegistry(),
                new ModelOutputValidator()
        );
    }

    private Problem problem() {
        return Problem.builder()
                .id(1L)
                .title("求 1 到 n 的和")
                .description("输入正整数 n，输出 1 到 n 的整数和。")
                .timeLimit(1000)
                .memoryLimit(65536)
                .build();
    }

    private Submission submission() {
        return Submission.builder()
                .id(11L)
                .problemId(1L)
                .languageName("Python 3")
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        n = int(input())
                        total = 0
                        for i in range(1, n):
                            total += i
                        print(total)
                        """)
                .build();
    }

    private DiagnosisEvidencePackage evidencePackage() {
        return DiagnosisEvidencePackage.builder()
                .schemaVersion(DiagnosisEvidencePackage.SCHEMA_VERSION)
                .problem(DiagnosisEvidencePackage.ProblemEvidence.builder()
                        .title("求 1 到 n 的和")
                        .description("输入正整数 n，输出 1 到 n 的整数和。")
                        .build())
                .submission(DiagnosisEvidencePackage.SubmissionEvidence.builder()
                        .id(11L)
                        .language("Python 3")
                        .verdict("WRONG_ANSWER")
                        .sourceCode(submission().getSourceCode())
                        .sourceCodeWithLineNumbers("""
                                1: n = int(input())
                                2: total = 0
                                3: for i in range(1, n):
                                4:     total += i
                                5: print(total)
                                """)
                        .sourceCodeLineCount(5)
                        .build())
                .judgeFacts(DiagnosisEvidencePackage.JudgeFacts.builder()
                        .hiddenFailureObserved(false)
                        .build())
                .build();
    }

    private DiagnosisEvidencePackage evidencePackageWithLargeContext() {
        String longDescription = "输入正整数 n，输出 1 到 n 的整数和。".repeat(80);
        String longCode = """
                n = int(input())
                total = 0
                for i in range(1, n):
                    total += i
                print(total)
                """.repeat(80);
        return DiagnosisEvidencePackage.builder()
                .schemaVersion(DiagnosisEvidencePackage.SCHEMA_VERSION)
                .problem(DiagnosisEvidencePackage.ProblemEvidence.builder()
                        .title("求 1 到 n 的和")
                        .description(longDescription)
                        .aiPromptDirection("请关注闭区间边界和最小样例。".repeat(20))
                        .difficulty("EASY")
                        .timeLimit(1000)
                        .memoryLimit(65536)
                        .knowledgePoints(List.of("循环", "边界", "求和"))
                        .algorithmStrategies(List.of("模拟", "数学归纳"))
                        .commonMistakes(List.of("range 右边界不包含 n", "遗漏 n=1"))
                        .boundaryTypes(List.of("n=1", "最大 n"))
                        .build())
                .submission(DiagnosisEvidencePackage.SubmissionEvidence.builder()
                        .id(11L)
                        .language("Python 3")
                        .verdict("WRONG_ANSWER")
                        .sourceCode(longCode)
                        .sourceCodeWithLineNumbers(numbered(longCode))
                        .sourceCodeLineCount(longCode.split("\\n", -1).length)
                        .build())
                .judgeFacts(DiagnosisEvidencePackage.JudgeFacts.builder()
                        .hiddenFailureObserved(true)
                        .firstFailedCase(SubmissionAnalysisResponse.FailedCaseSnapshot.builder()
                                .testCaseNumber(1)
                                .hidden(false)
                                .input("1")
                                .expectedOutput("1")
                                .actualOutput("0")
                                .build())
                        .caseResultsSummary(List.of(
                                DiagnosisEvidencePackage.CaseSummary.builder()
                                        .testCaseNumber(1)
                                        .passed(false)
                                        .hidden(false)
                                        .actualOutputPreview("0")
                                        .expectedOutputPreview("1")
                                        .build(),
                                DiagnosisEvidencePackage.CaseSummary.builder()
                                        .testCaseNumber(2)
                                        .passed(false)
                                        .hidden(true)
                                        .build()))
                        .build())
                .learningMemory(DiagnosisEvidencePackage.StudentLearningMemorySnapshot.builder()
                        .observedSubmissionCount(12)
                        .observedProblemCount(4)
                        .recurringIssueTags(List.of(
                                DiagnosisEvidencePackage.MemoryTagStat.builder().tag("LOOP_BOUNDARY").count(4L).build(),
                                DiagnosisEvidencePackage.MemoryTagStat.builder().tag("BOUNDARY_CONDITION").count(3L).build()))
                        .recurringFineGrainedTags(List.of(
                                DiagnosisEvidencePackage.MemoryTagStat.builder().tag("OFF_BY_ONE").count(4L).build()))
                        .recentTrend("连续两题在闭区间右边界上反复出错。")
                        .interventionEffect("上一次变量追踪部分完成，需要缩小任务。")
                        .teacherCorrectionSummary("教师多次把边界条件校正为循环边界。")
                        .evidenceRefs(List.of("memory:recurring_fine:OFF_BY_ONE"))
                        .build())
                .build();
    }

    private RuleSignalAnalyzer.RuleSignalResult richRuleSignals() {
        return RuleSignalAnalyzer.RuleSignalResult.builder()
                .candidateIssueTags(List.of("LOOP_BOUNDARY", "BOUNDARY_CONDITION", "NEEDS_MORE_EVIDENCE"))
                .candidateFineGrainedTags(List.of("OFF_BY_ONE", "MAX_BOUNDARY", "EMPTY_INPUT", "SAMPLE_OVERFIT"))
                .evidenceRefs(List.of("code:range_excludes_n", "judge:first_failed_case", "judge:hidden_failure"))
                .signals(List.of(
                        RuleSignalAnalyzer.Signal.builder()
                                .evidenceRef("code:range_excludes_n")
                                .coarseTag("LOOP_BOUNDARY")
                                .fineTag("OFF_BY_ONE")
                                .confidence(0.95)
                                .message("range(1, n) excludes n and directly explains the visible failed case. ".repeat(6))
                                .build(),
                        RuleSignalAnalyzer.Signal.builder()
                                .evidenceRef("judge:first_failed_case")
                                .coarseTag("BOUNDARY_CONDITION")
                                .fineTag("EMPTY_INPUT")
                                .confidence(0.52)
                                .message("Visible failure appears on a minimum case. ".repeat(6))
                                .build(),
                        RuleSignalAnalyzer.Signal.builder()
                                .evidenceRef("judge:hidden_failure")
                                .coarseTag("BOUNDARY_CONDITION")
                                .fineTag("MAX_BOUNDARY")
                                .confidence(0.42)
                                .message("Hidden failure is observed but hidden data must not be inferred. ".repeat(6))
                                .build(),
                        RuleSignalAnalyzer.Signal.builder()
                                .evidenceRef("problem:sample")
                                .coarseTag("SAMPLE_ONLY")
                                .fineTag("SAMPLE_OVERFIT")
                                .confidence(0.22)
                                .message("Weak generic sample-only signal. ".repeat(6))
                                .build()))
                .build();
    }

    private String numbered(String sourceCode) {
        String[] lines = sourceCode.split("\\n", -1);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(index + 1).append(": ").append(lines[index]);
        }
        return builder.toString();
    }

    private RuleSignalAnalyzer.RuleSignalResult ruleSignals() {
        return RuleSignalAnalyzer.RuleSignalResult.builder()
                .candidateIssueTags(List.of("LOOP_BOUNDARY"))
                .candidateFineGrainedTags(List.of("OFF_BY_ONE"))
                .evidenceRefs(List.of("code:range_excludes_n"))
                .signals(List.of(RuleSignalAnalyzer.Signal.builder()
                        .evidenceRef("code:range_excludes_n")
                        .coarseTag("LOOP_BOUNDARY")
                        .fineTag("OFF_BY_ONE")
                        .confidence(0.9)
                        .message("range(1, n) excludes n")
                        .build()))
                .build();
    }

    private RuleSignalAnalyzer.RuleSignalResult scaleRuleSignals() {
        return RuleSignalAnalyzer.RuleSignalResult.builder()
                .candidateIssueTags(List.of("TIME_COMPLEXITY"))
                .candidateFineGrainedTags(List.of("OVER_SIMULATION"))
                .evidenceRefs(List.of("code:range_excludes_n"))
                .signals(List.of(RuleSignalAnalyzer.Signal.builder()
                        .evidenceRef("code:range_excludes_n")
                        .coarseTag("TIME_COMPLEXITY")
                        .fineTag("OVER_SIMULATION")
                        .confidence(0.9)
                        .message("large bound with step-by-step simulation")
                        .build()))
                .build();
    }

    private SubmissionAnalysisResponse fallback() {
        return SubmissionAnalysisResponse.builder()
                .submissionId(11L)
                .analysisSchemaVersion("diagnosis-v1")
                .evidenceSchemaVersion(DiagnosisEvidencePackage.SCHEMA_VERSION)
                .taxonomyVersion(DiagnosisTaxonomy.TAXONOMY_VERSION)
                .sourceType("RULE_BASED_V1")
                .scenario("WA")
                .headline("规则层初步诊断")
                .summary("规则层认为可能存在循环边界问题。")
                .issueTags(List.of("LOOP_BOUNDARY"))
                .fineGrainedTags(List.of("OFF_BY_ONE"))
                .abilityPoints(List.of("循环边界"))
                .focusPoints(List.of("range 右边界"))
                .fixDirections(List.of("手推最小样例"))
                .evidenceRefs(List.of("code:range_excludes_n"))
                .studentHint("先手推最小样例。")
                .studentHintPlan(SubmissionAnalysisResponse.StudentHintPlan.builder()
                        .hintLevel("L2")
                        .problemType("循环边界")
                        .evidenceAnchor("code:range_excludes_n")
                        .nextAction("手推 range。")
                        .coachQuestion("右边界包含吗？")
                        .teachingAction("TRACE_VARIABLES")
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .answerLeakRisk("LOW")
                        .build())
                .confidence(0.72)
                .uncertainty("规则层初步判断。")
                .answerLeakRisk("LOW")
                .lineIssues(List.of(SubmissionAnalysisResponse.LineIssue.builder()
                        .lineNumber(3)
                        .error("循环未覆盖 n")
                        .suggestion("核对 range 的右边界")
                        .build()))
                .build();
    }

    private SubmissionAnalysisResponse scaleFallback() {
        return SubmissionAnalysisResponse.builder()
                .submissionId(11L)
                .analysisSchemaVersion("diagnosis-v1")
                .evidenceSchemaVersion(DiagnosisEvidencePackage.SCHEMA_VERSION)
                .taxonomyVersion(DiagnosisTaxonomy.TAXONOMY_VERSION)
                .sourceType("RULE_BASED_V1")
                .scenario("TLE")
                .headline("规则层初步诊断")
                .summary("规则层认为最大规模下存在逐步模拟问题。")
                .issueTags(List.of("TIME_COMPLEXITY"))
                .fineGrainedTags(List.of("OVER_SIMULATION"))
                .abilityPoints(List.of("复杂度"))
                .focusPoints(List.of("最大规模操作次数"))
                .fixDirections(List.of("估算最大输入下的循环次数"))
                .evidenceRefs(List.of("code:range_excludes_n"))
                .studentHint("先估算最大规模下循环执行次数。")
                .studentHintPlan(SubmissionAnalysisResponse.StudentHintPlan.builder()
                        .hintLevel("L2")
                        .problemType("复杂度")
                        .evidenceAnchor("code:range_excludes_n")
                        .nextAction("估算最大规模。")
                        .coachQuestion("最大规模下循环多少次？")
                        .teachingAction("COUNT_COMPLEXITY")
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .answerLeakRisk("LOW")
                        .build())
                .confidence(0.72)
                .uncertainty("规则层初步判断。")
                .answerLeakRisk("LOW")
                .build();
    }

    private static class StubAiReportService extends AiReportService {
        private final Queue<Object> responses = new ArrayDeque<>();
        private final List<String> userPrompts = new ArrayList<>();
        private int callCount;

        StubAiReportService(ObjectMapper objectMapper,
                            ExternalModelAgentRuntime runtime,
                            Object... responses) {
            this(objectMapper, runtime, new ExternalModelBudgetGuard(), responses);
        }

        StubAiReportService(ObjectMapper objectMapper,
                            ExternalModelAgentRuntime runtime,
                            ExternalModelBudgetGuard budgetGuard,
                            Object... responses) {
            super(objectMapper, new AiCodeAssistSupport(), runtime, new ExternalModelFailureClassifier(), budgetGuard);
            this.responses.addAll(List.of(responses));
        }

        @Override
        protected String chatCompletion(String systemPrompt, String userPrompt) throws IOException {
            callCount++;
            userPrompts.add(userPrompt);
            Object response = responses.poll();
            if (response instanceof IOException exception) {
                recordBudgetFailureForTest(exception);
                throw exception;
            }
            if (response == null) {
                throw new IOException("No stub response configured.");
            }
            return response.toString();
        }

        int callCount() {
            return callCount;
        }

        int lastUserPromptBytes() {
            if (userPrompts.isEmpty()) {
                return 0;
            }
            return userPrompts.get(userPrompts.size() - 1).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        }
    }

    private static class CapturingAiReportService extends AiReportService {
        private final Queue<String> responses = new ArrayDeque<>();
        private final List<String> requestBodies = new ArrayList<>();

        CapturingAiReportService(ObjectMapper objectMapper,
                                 ExternalModelAgentRuntime runtime,
                                 String... responses) {
            super(objectMapper, new AiCodeAssistSupport(), runtime);
            this.responses.addAll(List.of(responses));
        }

        @Override
        protected String sendChatCompletionRequest(String requestBody, boolean stream) throws IOException {
            requestBodies.add(requestBody);
            String response = responses.poll();
            if (response == null) {
                throw new IOException("No stub response configured.");
            }
            return response;
        }

        String lastRequestBody() {
            if (requestBodies.isEmpty()) {
                return "";
            }
            return requestBodies.get(requestBodies.size() - 1);
        }

        int lastRequestBytes() {
            return lastRequestBody().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        }
    }

    private static class StreamingFallbackAiReportService extends AiReportService {
        private final List<Boolean> streamFlags = new java.util.ArrayList<>();

        StreamingFallbackAiReportService(ObjectMapper objectMapper) {
            super(objectMapper, new AiCodeAssistSupport());
        }

        @Override
        protected String sendChatCompletionRequest(String requestBody, boolean stream) {
            streamFlags.add(stream);
            if (!stream) {
                return """
                        {"choices":null}
                        """;
            }
            return """
                    data: {"choices":[{"delta":{"reasoning_content":"先判断。","content":""}}]}

                    data: not-json

                    data: {"choices":[{"delta":{"reasoning_content":"","content":"{\\"diagnosisDecision\\":{\\"primaryIssueTag\\":\\"LOOP_BOUNDARY\\",\\"fineGrainedTag\\":\\"OFF_BY_ONE\\",\\"evidenceRefs\\":[\\"code:range_excludes_n\\"],\\"confidence\\":0.9,\\"uncertainty\\":\\"ok\\",\\"needsMoreEvidence\\":false,\\"answerLeakRisk\\":\\"LOW\\"},\\"teachingHint\\":{\\"studentHint\\":\\"先用 n=1 手推。\\",\\"studentHintPlan\\":{\\"hintLevel\\":\\"L2\\",\\"problemType\\":\\"循环边界\\",\\"evidenceAnchor\\":\\"code:range_excludes_n\\",\\"nextAction\\":\\"列出 range 产生的 i。\\",\\"coachQuestion\\":\\"当 n=1 时循环执行几次？\\",\\"teachingAction\\":\\"TRACE_VARIABLES\\",\\"evidenceRefs\\":[\\"code:range_excludes_n\\"],\\"answerLeakRisk\\":\\"LOW\\"},\\"learningInterventionPlan\\":{\\"interventionType\\":\\"VARIABLE_TRACE\\",\\"goal\\":\\"确认循环边界。\\",\\"studentTask\\":\\"手推最小样例。\\",\\"checkQuestion\\":\\"是否处理到 n？\\",\\"completionSignal\\":\\"能指出缺失位置。\\",\\"evidenceRefs\\":[\\"code:range_excludes_n\\"],\\"estimatedMinutes\\":5,\\"answerLeakRisk\\":\\"LOW\\"},\\"teacherNote\\":\\"stream ok\\",\\"answerLeakRisk\\":\\"LOW\\"}}"},"finish_reason":"stop"}]}

                    data: [DONE]
                    """;
        }

        List<Boolean> streamFlags() {
            return streamFlags;
        }
    }

    private static class LengthTruncatedAiReportService extends AiReportService {
        LengthTruncatedAiReportService(ObjectMapper objectMapper) {
            super(objectMapper, new AiCodeAssistSupport(), new ExternalModelAgentRuntime(
                    new ModelDiagnosisBriefBuilder(),
                    new StandardLibraryPackBuilder(new DiagnosisTaxonomy()),
                    new PromptTemplateRegistry(),
                    new ModelOutputValidator()
            ));
        }

        @Override
        protected String sendChatCompletionRequest(String requestBody, boolean stream) {
            return """
                    data: {"choices":[{"delta":{"reasoning_content":"先判断边界。","content":""}}]}

                    data: {"choices":[{"delta":{"reasoning_content":"","content":"{\\"diagnosisDecision\\":{\\"primaryIssueTag\\":\\"LOOP_BOUNDARY\\""},"finish_reason":"length"}]}

                    data: [DONE]
                    """;
        }
    }

    private static class RetryingAiReportService extends AiReportService {
        private final Queue<Object> responses = new ArrayDeque<>();
        private int callCount;

        RetryingAiReportService(ObjectMapper objectMapper, Object... responses) {
            super(objectMapper, new AiCodeAssistSupport());
            this.responses.addAll(List.of(responses));
        }

        @Override
        protected String sendChatCompletionRequest(String requestBody, boolean stream) throws IOException {
            callCount++;
            Object response = responses.poll();
            if (response instanceof IOException exception) {
                throw exception;
            }
            if (response == null) {
                throw new IOException("No stub response configured.");
            }
            return response.toString();
        }

        int callCount() {
            return callCount;
        }
    }

    private static class FailingGrowthReportAiReportService extends AiReportService {
        private final IOException exception;

        FailingGrowthReportAiReportService(ObjectMapper objectMapper, IOException exception) {
            super(objectMapper, new AiCodeAssistSupport());
            this.exception = exception;
        }

        @Override
        protected String chatCompletion(String systemPrompt, String userPrompt) throws IOException {
            throw exception;
        }
    }

    private static class TeachingHintTruncatedAiReportService extends AiReportService {
        private final boolean validDiagnosis;

        TeachingHintTruncatedAiReportService(ObjectMapper objectMapper, boolean validDiagnosis) {
            super(objectMapper, new AiCodeAssistSupport(), new ExternalModelAgentRuntime(
                    new ModelDiagnosisBriefBuilder(),
                    new StandardLibraryPackBuilder(new DiagnosisTaxonomy()),
                    new PromptTemplateRegistry(),
                    new ModelOutputValidator()
            ));
            this.validDiagnosis = validDiagnosis;
        }

        @Override
        protected String sendChatCompletionRequest(String requestBody, boolean stream) {
            String primaryIssueTag = validDiagnosis ? "LOOP_BOUNDARY" : "MADE_UP_TAG";
            String content = """
                    {"diagnosisDecision":{"primaryIssueTag":"%s","fineGrainedTag":"OFF_BY_ONE","evidenceRefs":["code:range_excludes_n"],"confidence":0.9,"uncertainty":"保留完整诊断。","needsMoreEvidence":false,"answerLeakRisk":"LOW"},"teachingHint":{"studentHint":"截断残留"}
                    """.formatted(primaryIssueTag);
            return """
                    data: {"choices":[{"delta":{"reasoning_content":"先判断边界。","content":""}}]}

                    data: {"choices":[{"delta":{"reasoning_content":"","content":%s},"finish_reason":"length"}]}

                    data: [DONE]
                    """.formatted(quoteJson(content));
        }

        private String quoteJson(String value) {
            try {
                return new ObjectMapper().writeValueAsString(value);
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}

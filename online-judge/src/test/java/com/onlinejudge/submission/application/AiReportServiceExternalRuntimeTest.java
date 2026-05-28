package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayDeque;
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
        useStagedRuntime(service);

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
        useStagedRuntime(service);

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
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo("diagnosis-and-teaching-v2");
        assertThat(service.callCount()).isEqualTo(1);
    }

    @Test
    void singleCallRuntimeUsesFallbackRouteWhenPrimaryRouteHitsQuota() {
        RetryingAiReportService service = new RetryingAiReportService(
                objectMapper,
                runtime(),
                new ExternalModelBudgetGuard(),
                new IOException("AI API returned status 429: {\"error\":{\"code\":\"insufficient_quota\"}}"),
                """
                {"choices":[{"message":{"content":"{\\"diagnosisDecision\\":{\\"primaryIssueTag\\":\\"LOOP_BOUNDARY\\",\\"fineGrainedTag\\":\\"OFF_BY_ONE\\",\\"evidenceRefs\\":[\\"code:range_excludes_n\\"],\\"confidence\\":0.9,\\"uncertainty\\":\\"fallback route succeeded\\",\\"needsMoreEvidence\\":false,\\"answerLeakRisk\\":\\"LOW\\"},\\"teachingHint\\":{\\"studentHint\\":\\"鍏堢敤 n=1 鎵嬫帹寰幆銆\\",\\"studentHintPlan\\":{\\"hintLevel\\":\\"L2\\",\\"problemType\\":\\"寰幆杈圭晫\\",\\"evidenceAnchor\\":\\"code:range_excludes_n\\",\\"nextAction\\":\\"鍒楀嚭 range 浜х敓鐨 i\\",\\"coachQuestion\\":\\"n=1 鏃朵細鎵ц鍑犳锛\\",\\"teachingAction\\":\\"TRACE_VARIABLES\\",\\"evidenceRefs\\":[\\"code:range_excludes_n\\"],\\"answerLeakRisk\\":\\"LOW\\"},\\"learningInterventionPlan\\":{\\"interventionType\\":\\"VARIABLE_TRACE\\",\\"goal\\":\\"纭杈圭晫\\",\\"studentTask\\":\\"鎵嬫帹 n=1\\",\\"checkQuestion\\":\\"鏈€鍚庝竴娆℃槸浠€涔堬紵\\",\\"completionSignal\\":\\"鍐欏嚭 i 鍊艰〃\\",\\"evidenceRefs\\":[\\"code:range_excludes_n\\"],\\"estimatedMinutes\\":5,\\"answerLeakRisk\\":\\"LOW\\"},\\"teacherNote\\":\\"fallback route ok\\",\\"answerLeakRisk\\":\\"LOW\\"}}"}}]}
                """
        );
        enableSingleCallRouteFallback(service);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
        assertThat(analysis.getAiInvocation().getProvider()).isEqualTo("FallbackProvider");
        assertThat(analysis.getAiInvocation().getModel()).isEqualTo("fallback-model");
        assertThat(service.callCount()).isEqualTo(2);
        assertThat(service.models()).containsExactly("primary-model", "fallback-model");
    }

    @Test
    void singleCallRuntimeSkipsGuardedPrimaryRouteAndUsesFallbackRoute() {
        ExternalModelBudgetGuard budgetGuard = new ExternalModelBudgetGuard();
        budgetGuard.recordFailure("PrimaryProvider", "primary-model", ModelStageFailureReason.INSUFFICIENT_QUOTA);
        RetryingAiReportService service = new RetryingAiReportService(
                objectMapper,
                runtime(),
                budgetGuard,
                """
                {"choices":[{"message":{"content":"{\\"diagnosisDecision\\":{\\"primaryIssueTag\\":\\"LOOP_BOUNDARY\\",\\"fineGrainedTag\\":\\"OFF_BY_ONE\\",\\"evidenceRefs\\":[\\"code:range_excludes_n\\"],\\"confidence\\":0.9,\\"uncertainty\\":\\"guard skipped primary\\",\\"needsMoreEvidence\\":false,\\"answerLeakRisk\\":\\"LOW\\"},\\"teachingHint\\":{\\"studentHint\\":\\"鍏堢敤 n=1 鎵嬫帹寰幆銆\\",\\"studentHintPlan\\":{\\"hintLevel\\":\\"L2\\",\\"problemType\\":\\"寰幆杈圭晫\\",\\"evidenceAnchor\\":\\"code:range_excludes_n\\",\\"nextAction\\":\\"鍒楀嚭 range 浜х敓鐨 i\\",\\"coachQuestion\\":\\"n=1 鏃朵細鎵ц鍑犳锛\\",\\"teachingAction\\":\\"TRACE_VARIABLES\\",\\"evidenceRefs\\":[\\"code:range_excludes_n\\"],\\"answerLeakRisk\\":\\"LOW\\"},\\"learningInterventionPlan\\":{\\"interventionType\\":\\"VARIABLE_TRACE\\",\\"goal\\":\\"纭杈圭晫\\",\\"studentTask\\":\\"鎵嬫帹 n=1\\",\\"checkQuestion\\":\\"鏈€鍚庝竴娆℃槸浠€涔堬紵\\",\\"completionSignal\\":\\"鍐欏嚭 i 鍊艰〃\\",\\"evidenceRefs\\":[\\"code:range_excludes_n\\"],\\"estimatedMinutes\\":5,\\"answerLeakRisk\\":\\"LOW\\"},\\"teacherNote\\":\\"fallback route ok\\",\\"answerLeakRisk\\":\\"LOW\\"}}"}}]}
                """
        );
        enableSingleCallRouteFallback(service);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
        assertThat(analysis.getAiInvocation().getProvider()).isEqualTo("FallbackProvider");
        assertThat(service.callCount()).isEqualTo(1);
        assertThat(service.models()).containsExactly("fallback-model");
    }

    @Test
    void singleCallRuntimeUsesRoutePoolAfterPrimaryAndFallbackFail() {
        RetryingAiReportService service = new RetryingAiReportService(
                objectMapper,
                runtime(),
                new ExternalModelBudgetGuard(),
                new IOException("AI API returned status 429: {\"error\":{\"code\":\"insufficient_quota\"}}"),
                new IOException("AI API returned status 429: {\"error\":{\"message\":\"rate limit\"}}"),
                successfulSingleCallResponse("route pool succeeded")
        );
        enableSingleCallRouteFallback(service);
        ReflectionTestUtils.setField(service, "additionalRoutes", """
                BrokenRoute;
                ExtraProvider|https://extra.example/v1|extra-key|extra-model
                """);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                problem(),
                submission(),
                fallback(),
                evidencePackage(),
                ruleSignals()
        );

        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
        assertThat(analysis.getAiInvocation().getProvider()).isEqualTo("ExtraProvider");
        assertThat(analysis.getAiInvocation().getModel()).isEqualTo("extra-model");
        assertThat(service.callCount()).isEqualTo(3);
        assertThat(service.models()).containsExactly("primary-model", "fallback-model", "extra-model");
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
    void singleCallRuntimeDoesNotDowngradeSafeInputParsingHintBecauseTeacherNoteMentionsForbiddenFix() {
        StubAiReportService service = newService(
                """
                {
                  "diagnosisDecision": {
                    "primaryIssueTag": "IO_FORMAT",
                    "fineGrainedTag": "INPUT_PARSING",
                    "evidenceRefs": ["problem:repeated_input_source_single_read"],
                    "confidence": 0.92,
                    "uncertainty": "输入读取次数与题面不一致。",
                    "needsMoreEvidence": false,
                    "answerLeakRisk": "LOW"
                  },
                  "teachingHint": {
                    "studentHint": "请把题面输入行和代码读取次数逐行对齐，先找第一处不一致。",
                    "studentHintPlan": {
                      "hintLevel": "L2",
                      "problemType": "输入读取",
                      "evidenceAnchor": "problem:repeated_input_source_single_read",
                      "nextAction": "对比题面中的 q 次查询和代码里的读取次数。",
                      "coachQuestion": "从哪一行开始，题面要求的输入和代码读取次数不一致？",
                      "teachingAction": "COMPARE_INPUT_SPEC",
                      "evidenceRefs": ["problem:repeated_input_source_single_read"],
                      "answerLeakRisk": "LOW"
                    },
                    "learningInterventionPlan": {
                      "interventionType": "IO_COMPARE",
                      "goal": "核对输入结构。",
                      "studentTask": "把每次读取和题面每一行输入对应起来。",
                      "checkQuestion": "是否存在没有被读取的查询行？",
                      "completionSignal": "学生能指出读取次数不一致。",
                      "evidenceRefs": ["problem:repeated_input_source_single_read"],
                      "estimatedMinutes": 5,
                      "answerLeakRisk": "LOW"
                    },
                    "teacherNote": "提醒老师：不要直接告诉学生加 for _ in range(q)。",
                    "answerLeakRisk": "HIGH"
                  }
                }
                """
        );

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                inputParsingProblem(),
                inputParsingSubmission(),
                inputParsingFallback(),
                inputParsingEvidencePackage(),
                inputParsingRuleSignals()
        );

        assertThat(analysis.getIssueTags()).containsExactly("IO_FORMAT");
        assertThat(analysis.getFineGrainedTags()).containsExactly("INPUT_PARSING");
        assertThat(analysis.getAnswerLeakRisk()).isEqualTo("LOW");
        assertThat(analysis.getStudentHintPlan()).isNotNull();
        assertThat(analysis.getStudentHintPlan().getTeachingAction()).isEqualTo("COMPARE_INPUT_SPEC");
        assertThat(analysis.getStudentHintPlan().getNextAction()).contains("题面");
        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
    }

    @Test
    void singleCallRuntimeSendsCompactPromptContextWithoutDroppingCoreSignals() throws Exception {
        CapturingAiReportService service = new CapturingAiReportService(
                objectMapper,
                runtime(),
                """
                {
                  "diagnosisDecision": {
                    "primaryIssueTag": "IO_FORMAT",
                    "fineGrainedTag": "INPUT_PARSING",
                    "evidenceRefs": ["problem:repeated_input_source_single_read"],
                    "confidence": 0.92,
                    "uncertainty": "输入读取次数与题面不一致。",
                    "needsMoreEvidence": false,
                    "answerLeakRisk": "LOW"
                  },
                  "teachingHint": {
                    "studentHint": "请把题面输入行和代码读取次数逐行对齐。",
                    "studentHintPlan": {
                      "hintLevel": "L2",
                      "problemType": "输入读取",
                      "evidenceAnchor": "problem:repeated_input_source_single_read",
                      "nextAction": "对比题面中的 q 次查询和代码里的读取次数。",
                      "coachQuestion": "题目要求读几组查询？代码读了几组？",
                      "teachingAction": "COMPARE_INPUT_SPEC",
                      "evidenceRefs": ["problem:repeated_input_source_single_read"],
                      "answerLeakRisk": "LOW"
                    },
                    "learningInterventionPlan": {
                      "interventionType": "IO_COMPARE",
                      "goal": "核对输入结构。",
                      "studentTask": "列出题面每一行输入和代码每一次读取。",
                      "checkQuestion": "是否存在没有被读取的查询行？",
                      "completionSignal": "学生能指出读取次数不一致。",
                      "evidenceRefs": ["problem:repeated_input_source_single_read"],
                      "estimatedMinutes": 5,
                      "answerLeakRisk": "LOW"
                    },
                    "teacherNote": "compact prompt ok",
                    "answerLeakRisk": "LOW"
                  }
                }
                """
        );
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 900);

        SubmissionAnalysisResponse analysis = service.enhanceSubmissionAnalysis(
                inputParsingProblem(),
                inputParsingSubmission(),
                inputParsingFallback(),
                inputParsingEvidencePackage(),
                inputParsingRuleSignals()
        );

        JsonNode prompt = objectMapper.readTree(service.userPrompts().get(0));

        assertThat(analysis.getAiInvocation().getStatus()).isEqualTo("MODEL_COMPLETED");
        assertThat(service.userPrompts()).hasSize(1);
        assertThat(prompt.at("/brief/keyCodeExcerpt").asText().length()).isLessThanOrEqualTo(2600);
        assertThat(prompt.at("/brief/candidateSignals/0/evidenceRef").asText())
                .isEqualTo("problem:repeated_input_source_single_read");
        assertThat(prompt.at("/standardLibrary/issueTags/0/id").asText()).isEqualTo("IO_FORMAT");
        assertThat(prompt.at("/standardLibrary/fineGrainedTags/0/id").asText()).isEqualTo("INPUT_PARSING");
        assertThat(prompt.at("/standardLibrary/teachingActions").toString()).contains("COMPARE_INPUT_SPEC");
        assertThat(service.userPrompts().get(0)).doesNotContain("studentExplanation", "teacherExplanation", "abilityPoint", "whenToUse");
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
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo("diagnosis-and-teaching-v2");
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
        useStagedRuntime(service);

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
        assertThat(analysis.getAiInvocation().getPromptVersion()).isEqualTo("diagnosis-and-teaching-v2");
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
        useStagedRuntime(service);

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
        useStagedRuntime(service);

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
        assertThat(service.callCount()).isEqualTo(1);
    }

    @Test
    void diagnosticAgentPreservesRuntimeFallbackStatus() {
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
        ReflectionTestUtils.setField(service, "retryMaxAttempts", 1);
        ReflectionTestUtils.setField(service, "retryBackoffMs", 0L);

        String content = service.chatCompletion("system", "user");

        assertThat(content).isEqualTo("{\"primaryIssueTag\":\"LOOP_BOUNDARY\"}");
        assertThat(service.streamFlags()).containsExactly(false, true);
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
        assertThat(analysis.getUncertainty()).contains("INSUFFICIENT_QUOTA");
        assertThat(analysis.getAiInvocation().getProvider()).isEqualTo("ModelScope");
        assertThat(analysis.getAiInvocation().getModel()).isEqualTo("test-model");
        assertThat(service.callCount()).isEqualTo(1);

        String markdown = service.enhanceGrowthReportMarkdown(problem(), List.of(), "# 成长报告");

        assertThat(markdown).contains("BUDGET_GUARD_OPEN");
        assertThat(service.callCount()).isEqualTo(1);
    }

    private StubAiReportService newService(String... responses) {
        StubAiReportService service = new StubAiReportService(objectMapper, runtime(), (Object[]) responses);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "test-model");
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 900);
        return service;
    }

    private void useStagedRuntime(AiReportService service) {
        ReflectionTestUtils.setField(service, "externalRuntimeMode", "staged");
    }

    private void enableSingleCallRouteFallback(AiReportService service) {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "provider", "PrimaryProvider");
        ReflectionTestUtils.setField(service, "apiKey", "primary-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://primary.example/v1");
        ReflectionTestUtils.setField(service, "model", "primary-model");
        ReflectionTestUtils.setField(service, "fallbackProvider", "FallbackProvider");
        ReflectionTestUtils.setField(service, "fallbackApiKey", "fallback-key");
        ReflectionTestUtils.setField(service, "fallbackBaseUrl", "https://fallback.example/v1");
        ReflectionTestUtils.setField(service, "fallbackModel", "fallback-model");
        ReflectionTestUtils.setField(service, "externalRuntimeEnabled", true);
        ReflectionTestUtils.setField(service, "externalRuntimeMode", "single-call");
        ReflectionTestUtils.setField(service, "timeoutSeconds", 5L);
        ReflectionTestUtils.setField(service, "maxOutputTokens", 900);
        ReflectionTestUtils.setField(service, "streamEnabled", false);
        ReflectionTestUtils.setField(service, "streamFallbackEnabled", false);
        ReflectionTestUtils.setField(service, "retryMaxAttempts", 1);
        ReflectionTestUtils.setField(service, "retryBackoffMs", 0L);
    }

    private String successfulSingleCallResponse(String uncertainty) {
        return """
                {"choices":[{"message":{"content":"{\\"diagnosisDecision\\":{\\"primaryIssueTag\\":\\"LOOP_BOUNDARY\\",\\"fineGrainedTag\\":\\"OFF_BY_ONE\\",\\"evidenceRefs\\":[\\"code:range_excludes_n\\"],\\"confidence\\":0.9,\\"uncertainty\\":\\"%s\\",\\"needsMoreEvidence\\":false,\\"answerLeakRisk\\":\\"LOW\\"},\\"teachingHint\\":{\\"studentHint\\":\\"先用 n=1 手推循环。\\",\\"studentHintPlan\\":{\\"hintLevel\\":\\"L2\\",\\"problemType\\":\\"循环边界\\",\\"evidenceAnchor\\":\\"code:range_excludes_n\\",\\"nextAction\\":\\"列出 range 产生的 i。\\",\\"coachQuestion\\":\\"n=1 时会执行几次？\\",\\"teachingAction\\":\\"TRACE_VARIABLES\\",\\"evidenceRefs\\":[\\"code:range_excludes_n\\"],\\"answerLeakRisk\\":\\"LOW\\"},\\"learningInterventionPlan\\":{\\"interventionType\\":\\"VARIABLE_TRACE\\",\\"goal\\":\\"确认边界\\",\\"studentTask\\":\\"手推 n=1\\",\\"checkQuestion\\":\\"最后一次循环是什么？\\",\\"completionSignal\\":\\"写出 i 值表\\",\\"evidenceRefs\\":[\\"code:range_excludes_n\\"],\\"estimatedMinutes\\":5,\\"answerLeakRisk\\":\\"LOW\\"},\\"teacherNote\\":\\"route ok\\",\\"answerLeakRisk\\":\\"LOW\\"}}"}}]}
                """.formatted(uncertainty);
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

    private Problem inputParsingProblem() {
        return Problem.builder()
                .id(2L)
                .title("区间和查询")
                .description("""
                        第一行输入两个整数 n 和 q。
                        第二行输入 n 个整数。
                        接下来 q 行，每行输入 l 和 r，输出区间 [l, r] 的元素和。
                        """)
                .timeLimit(1000)
                .memoryLimit(65536)
                .build();
    }

    private Submission inputParsingSubmission() {
        return Submission.builder()
                .id(12L)
                .problemId(2L)
                .languageName("Python 3")
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .sourceCode("""
                        import sys

                        def read_ints():
                            return list(map(int, sys.stdin.readline().split()))

                        def build_prefix(values):
                            prefix = [0]
                            total = 0
                            for value in values:
                                total += value
                                prefix.append(total)
                            return prefix

                        def query_sum(prefix, left, right):
                            left = max(left, 1)
                            right = min(right, len(prefix) - 1)
                            if left > right:
                                return 0
                            return prefix[right] - prefix[left - 1]

                        n, q = read_ints()
                        numbers = read_ints()
                        prefix = build_prefix(numbers)
                        l, r = read_ints()
                        print(query_sum(prefix, l, r))
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

    private DiagnosisEvidencePackage inputParsingEvidencePackage() {
        return DiagnosisEvidencePackage.builder()
                .schemaVersion(DiagnosisEvidencePackage.SCHEMA_VERSION)
                .problem(DiagnosisEvidencePackage.ProblemEvidence.builder()
                        .title("区间和查询")
                        .description("""
                                第一行输入两个整数 n 和 q。
                                第二行输入 n 个整数。
                                接下来 q 行，每行输入 l 和 r，输出区间 [l, r] 的元素和。
                                """)
                        .build())
                .submission(DiagnosisEvidencePackage.SubmissionEvidence.builder()
                        .id(12L)
                        .language("Python 3")
                        .verdict("WRONG_ANSWER")
                        .sourceCode(inputParsingSubmission().getSourceCode())
                        .sourceCodeWithLineNumbers("""
                                1: import sys
                                2:
                                3: def read_ints():
                                4:     return list(map(int, sys.stdin.readline().split()))
                                5:
                                6: def build_prefix(values):
                                7:     prefix = [0]
                                8:     total = 0
                                9:     for value in values:
                                10:         total += value
                                11:         prefix.append(total)
                                12:     return prefix
                                13:
                                14: def query_sum(prefix, left, right):
                                15:     left = max(left, 1)
                                16:     right = min(right, len(prefix) - 1)
                                17:     if left > right:
                                18:         return 0
                                19:     return prefix[right] - prefix[left - 1]
                                20:
                                21: n, q = read_ints()
                                22: numbers = read_ints()
                                23: prefix = build_prefix(numbers)
                                24: l, r = read_ints()
                                25: print(query_sum(prefix, l, r))
                                """)
                        .sourceCodeLineCount(25)
                        .build())
                .judgeFacts(DiagnosisEvidencePackage.JudgeFacts.builder()
                        .hiddenFailureObserved(true)
                        .totalCount(8)
                        .passedCount(2)
                        .build())
                .build();
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

    private RuleSignalAnalyzer.RuleSignalResult inputParsingRuleSignals() {
        return RuleSignalAnalyzer.RuleSignalResult.builder()
                .candidateIssueTags(List.of("IO_FORMAT"))
                .candidateFineGrainedTags(List.of("INPUT_PARSING"))
                .evidenceRefs(List.of("problem:repeated_input_source_single_read"))
                .signals(List.of(RuleSignalAnalyzer.Signal.builder()
                        .evidenceRef("problem:repeated_input_source_single_read")
                        .coarseTag("IO_FORMAT")
                        .fineTag("INPUT_PARSING")
                        .confidence(0.92)
                        .message("problem requires q repeated query inputs but code reads only one query pair")
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

    private SubmissionAnalysisResponse inputParsingFallback() {
        return SubmissionAnalysisResponse.builder()
                .submissionId(12L)
                .analysisSchemaVersion("diagnosis-v1")
                .evidenceSchemaVersion(DiagnosisEvidencePackage.SCHEMA_VERSION)
                .taxonomyVersion(DiagnosisTaxonomy.TAXONOMY_VERSION)
                .sourceType("RULE_BASED_V1")
                .scenario("WA")
                .headline("规则层初步诊断")
                .summary("规则层认为题面要求 q 次查询，但代码只读取了一组查询。")
                .issueTags(List.of("IO_FORMAT"))
                .fineGrainedTags(List.of("INPUT_PARSING"))
                .abilityPoints(List.of("输入读取"))
                .focusPoints(List.of("题面输入结构", "读取次数"))
                .fixDirections(List.of("逐行对齐题面输入和代码读取"))
                .evidenceRefs(List.of("problem:repeated_input_source_single_read"))
                .studentHint("先把题面输入行和代码读取次数逐行对齐。")
                .studentHintPlan(SubmissionAnalysisResponse.StudentHintPlan.builder()
                        .hintLevel("L2")
                        .problemType("输入读取")
                        .evidenceAnchor("problem:repeated_input_source_single_read")
                        .nextAction("对比 q 次查询和代码实际读取的查询次数。")
                        .coachQuestion("题面说接下来有几行查询？代码读了几行？")
                        .teachingAction("COMPARE_INPUT_SPEC")
                        .evidenceRefs(List.of("problem:repeated_input_source_single_read"))
                        .answerLeakRisk("LOW")
                        .build())
                .learningInterventionPlan(SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                        .interventionType("IO_COMPARE")
                        .goal("核对输入结构。")
                        .studentTask("列出题面每一行输入和代码每一次读取。")
                        .checkQuestion("是否存在没有被读取的查询行？")
                        .completionSignal("学生能指出读取次数不一致。")
                        .evidenceRefs(List.of("problem:repeated_input_source_single_read"))
                        .estimatedMinutes(5)
                        .answerLeakRisk("LOW")
                        .build())
                .confidence(0.74)
                .uncertainty("规则层初步判断。")
                .answerLeakRisk("LOW")
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
    }

    private static class CapturingAiReportService extends StubAiReportService {
        private final List<String> userPrompts = new java.util.ArrayList<>();

        CapturingAiReportService(ObjectMapper objectMapper,
                                 ExternalModelAgentRuntime runtime,
                                 Object... responses) {
            super(objectMapper, runtime, responses);
        }

        @Override
        protected String chatCompletion(String systemPrompt, String userPrompt) throws IOException {
            userPrompts.add(userPrompt);
            return super.chatCompletion(systemPrompt, userPrompt);
        }

        List<String> userPrompts() {
            return userPrompts;
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

                    data: {"choices":[{"delta":{"reasoning_content":"","content":"{\\"primaryIssueTag\\":\\"LOOP_BOUNDARY\\"}"}}]}

                    data: [DONE]
                    """;
        }

        List<Boolean> streamFlags() {
            return streamFlags;
        }
    }

    private static class RetryingAiReportService extends AiReportService {
        private final Queue<Object> responses = new ArrayDeque<>();
        private final ObjectMapper objectMapper;
        private final List<String> models = new java.util.ArrayList<>();
        private int callCount;

        RetryingAiReportService(ObjectMapper objectMapper, Object... responses) {
            super(objectMapper, new AiCodeAssistSupport());
            this.objectMapper = objectMapper;
            this.responses.addAll(List.of(responses));
        }

        RetryingAiReportService(ObjectMapper objectMapper,
                                ExternalModelAgentRuntime runtime,
                                ExternalModelBudgetGuard budgetGuard,
                                Object... responses) {
            super(objectMapper, new AiCodeAssistSupport(), runtime, new ExternalModelFailureClassifier(), budgetGuard);
            this.objectMapper = objectMapper;
            this.responses.addAll(List.of(responses));
        }

        @Override
        protected String sendChatCompletionRequest(String requestBody, boolean stream) throws IOException {
            callCount++;
            models.add(objectMapper.readTree(requestBody).path("model").asText());
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

        List<String> models() {
            return models;
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
}

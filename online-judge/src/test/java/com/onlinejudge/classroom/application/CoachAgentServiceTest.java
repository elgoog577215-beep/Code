package com.onlinejudge.classroom.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.classroom.domain.Assignment;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoachAgentServiceTest {

    private final DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void coachEvalFixturesExposeStableSafetyExpectations() throws IOException {
        List<CoachEvalFixtureLoader.Fixture> fixtures = new CoachEvalFixtureLoader(objectMapper).loadDefault();

        assertThat(fixtures).hasSizeGreaterThanOrEqualTo(3);
        assertThat(fixtures)
                .allSatisfy(fixture -> {
                    assertThat(fixture.name()).isNotBlank();
                    assertThat(fixture.turnType()).isIn("INITIAL", "INITIAL_QUESTION", "FOLLOW_UP");
                    assertThat(fixture.primaryTag()).isNotBlank();
                    assertThat(fixture.contextSummary()).isNotBlank();
                    assertThat(fixture.evidenceRefs()).isNotEmpty();
                    assertThat(fixture.requiredEvidenceRefs()).isNotEmpty();
                    assertThat(fixture.evidenceRefs()).containsAll(fixture.requiredEvidenceRefs());
                    assertThat(fixture.expectedQuestionSignals()).isNotEmpty();
                    assertThat(fixture.forbiddenPhrases()).isNotEmpty();
                    assertThat(fixture.safeModelResponse()).isNotNull();
                    assertThat(fixture.safeModelResponse().question()).isNotBlank();
                    assertThat(fixture.safeModelResponse().answerLeakRisk()).isIn("LOW", "MEDIUM");
                });
    }

    @Test
    void coachEvalFixtureResponsesPassLocalSafetyGate() throws IOException {
        List<CoachEvalFixtureLoader.Fixture> fixtures = new CoachEvalFixtureLoader(objectMapper).loadDefault();

        for (CoachEvalFixtureLoader.Fixture fixture : fixtures) {
            StubCoachAgentService service = new StubCoachAgentService(
                    objectMapper,
                    taxonomy,
                    fixture.safeModelResponseJson(objectMapper)
            );
            enableAi(service);

            CoachAgentService.CoachDraft draft = generateForFixture(service, fixture);

            assertThat(draft.getSource()).as(fixture.name()).isEqualTo("MODEL");
            assertDraftMatchesFixture(fixture, draft, true, true);
        }
    }

    @Test
    void rejectsFixtureLikeDraftWithoutEvidenceRefs() throws IOException {
        CoachEvalFixtureLoader.Fixture fixture = new CoachEvalFixtureLoader(objectMapper).loadDefault().get(0);
        StubCoachAgentService service = new StubCoachAgentService(objectMapper, taxonomy, """
                {
                  "question": "请先用 n=1 手推一次循环，写出循环第一次和最后一次取值。",
                  "rationale": "看起来是安全追问，但没有引用任何证据。",
                  "evidenceRefs": [],
                  "confidence": 0.86,
                  "answerLeakRisk": "LOW"
                }
                """);
        enableAi(service);

        CoachAgentService.CoachDraft draft = generateForFixture(service, fixture);

        assertThat(draft.getSource()).isEqualTo("RULE");
        assertThat(draft.getQuestion()).isEqualTo("fixture fallback");
    }

    @Test
    void acceptsSafeModelDraftWithKnownEvidenceRefs() {
        StubCoachAgentService service = new StubCoachAgentService(objectMapper, taxonomy, """
                {
                  "question": "请用 n=1 的最小样例手推循环第一次和最后一次取值。",
                  "rationale": "继续要求学生补证据。",
                  "evidenceRefs": ["submission:11", "tag:OFF_BY_ONE"],
                  "confidence": 0.82,
                  "answerLeakRisk": "LOW"
                }
                """);
        enableAi(service);

        CoachAgentService.CoachDraft draft = service.generateInitialQuestion(
                submission(),
                analysis(),
                "OFF_BY_ONE",
                Assignment.HintPolicy.L2,
                "上下文",
                List.of("submission:11", "tag:OFF_BY_ONE"),
                CoachAgentService.CoachDraft.fallback("规则问题")
        );

        assertThat(draft.getSource()).isEqualTo("MODEL");
        assertThat(draft.getQuestion()).contains("n=1");
        assertThat(draft.getEvidenceRefs()).containsExactly("submission:11", "tag:OFF_BY_ONE");
    }

    @Test
    void rejectsLeakyModelDraftAndFallsBack() {
        StubCoachAgentService service = new StubCoachAgentService(objectMapper, taxonomy, """
                {
                  "question": "答案如下，直接改成完整代码即可。",
                  "rationale": "越界输出",
                  "evidenceRefs": ["submission:11"],
                  "confidence": 0.95,
                  "answerLeakRisk": "HIGH"
                }
                """);
        enableAi(service);

        CoachAgentService.CoachDraft draft = service.generateInitialQuestion(
                submission(),
                analysis(),
                "OFF_BY_ONE",
                Assignment.HintPolicy.L2,
                "上下文",
                List.of("submission:11"),
                CoachAgentService.CoachDraft.fallback("规则问题")
        );

        assertThat(draft.getSource()).isEqualTo("RULE");
        assertThat(draft.getQuestion()).isEqualTo("规则问题");
    }

    @Test
    void fallsBackWhenModelIsUnavailable() {
        CoachAgentService service = new CoachAgentService(objectMapper, taxonomy);

        CoachAgentService.CoachDraft draft = service.generateFollowUpQuestion(
                submission(),
                analysis(),
                "OFF_BY_ONE",
                Assignment.HintPolicy.L2,
                "上下文",
                List.of("submission:11"),
                "我会手推 n=1",
                1,
                CoachAgentService.CoachDraft.fallback("规则追问")
        );

        assertThat(draft.getSource()).isEqualTo("RULE");
        assertThat(draft.getQuestion()).isEqualTo("规则追问");
    }

    @Test
    void liveModelCoachQuestionsStaySafeWhenEnabled() throws IOException {
        String apiKey = System.getenv("AI_EVAL_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), "Set AI_EVAL_API_KEY to run live coach eval.");
        CoachAgentService service = newLiveService(apiKey);
        List<CoachEvalFixtureLoader.Fixture> fixtures = new CoachEvalFixtureLoader(objectMapper).loadDefault();

        for (CoachEvalFixtureLoader.Fixture fixture : fixtures) {
            CoachAgentService.CoachDraft draft = generateForFixture(service, fixture);

            assertThat(draft.getSource()).as(fixture.name()).isEqualTo("MODEL");
            assertDraftMatchesFixture(fixture, draft, false, false);
        }
    }

    private void enableAi(CoachAgentService service) {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
    }

    private CoachAgentService newLiveService(String apiKey) {
        CoachAgentService service = new CoachAgentService(objectMapper, taxonomy);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "apiKey", apiKey);
        ReflectionTestUtils.setField(service, "baseUrl", valueOrDefault(System.getenv("AI_EVAL_BASE_URL"), "https://api-inference.modelscope.cn/v1"));
        ReflectionTestUtils.setField(service, "model", valueOrDefault(System.getenv("AI_EVAL_MODEL"), "deepseek-ai/DeepSeek-V4-Pro"));
        ReflectionTestUtils.setField(service, "timeoutSeconds", 35L);
        ReflectionTestUtils.setField(service, "streamEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_STREAM_ENABLED"), "true")));
        ReflectionTestUtils.setField(service, "streamFallbackEnabled",
                Boolean.parseBoolean(valueOrDefault(System.getenv("AI_STREAM_FALLBACK_ENABLED"), "true")));
        return service;
    }

    private CoachAgentService.CoachDraft generateForFixture(CoachAgentService service,
                                                            CoachEvalFixtureLoader.Fixture fixture) {
        CoachAgentService.CoachDraft fallback = CoachAgentService.CoachDraft.fallback("fixture fallback");
        if ("FOLLOW_UP".equals(fixture.turnType())) {
            return service.generateFollowUpQuestion(
                    fixture.toSubmission(),
                    fixture.toAnalysis(),
                    fixture.primaryTag(),
                    fixture.toHintPolicy(),
                    fixture.contextSummary(),
                    fixture.evidenceRefs(),
                    fixture.studentAnswer(),
                    1,
                    fallback
            );
        }
        return service.generateInitialQuestion(
                fixture.toSubmission(),
                fixture.toAnalysis(),
                fixture.primaryTag(),
                fixture.toHintPolicy(),
                fixture.contextSummary(),
                fixture.evidenceRefs(),
                fallback
        );
    }

    private void assertDraftMatchesFixture(CoachEvalFixtureLoader.Fixture fixture,
                                           CoachAgentService.CoachDraft draft,
                                           boolean requireAllSignals,
                                           boolean requireAllEvidenceRefs) {
        String combinedText = (draft.getQuestion() == null ? "" : draft.getQuestion())
                + "\n"
                + (draft.getRationale() == null ? "" : draft.getRationale());

        assertThat(draft.getQuestion()).as(fixture.name() + " question").isNotBlank();
        assertThat(draft.getAnswerLeakRisk()).as(fixture.name() + " leak risk").isIn("LOW", "MEDIUM", "UNKNOWN");
        if (requireAllEvidenceRefs) {
            assertThat(draft.getEvidenceRefs())
                    .as(fixture.name() + " evidence refs")
                    .containsAll(fixture.requiredEvidenceRefs());
        } else {
            assertThat(draft.getEvidenceRefs())
                    .as(fixture.name() + " evidence refs")
                    .containsAnyElementsOf(fixture.requiredEvidenceRefs());
        }
        assertThat(combinedText)
                .as(fixture.name() + " forbidden phrases")
                .doesNotContain(fixture.forbiddenPhrases().toArray(String[]::new));
        if (requireAllSignals) {
            assertThat(draft.getQuestion())
                    .as(fixture.name() + " question signals")
                    .contains(fixture.expectedQuestionSignals().toArray(String[]::new));
        } else {
            assertThat(fixture.expectedQuestionSignals())
                    .as(fixture.name() + " question signals")
                    .anySatisfy(signal -> assertThat(draft.getQuestion()).contains(signal));
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Submission submission() {
        return Submission.builder()
                .id(11L)
                .verdict(Submission.Verdict.WRONG_ANSWER)
                .build();
    }

    private SubmissionAnalysis analysis() {
        return SubmissionAnalysis.builder()
                .submissionId(11L)
                .scenario("WA")
                .headline("边界问题")
                .build();
    }

    private static class StubCoachAgentService extends CoachAgentService {
        private final String response;

        private StubCoachAgentService(ObjectMapper objectMapper, DiagnosisTaxonomy taxonomy, String response) {
            super(objectMapper, taxonomy);
            this.response = response;
        }

        @Override
        protected String chatCompletion(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
            return response;
        }
    }
}

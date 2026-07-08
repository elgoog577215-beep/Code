package com.onlinejudge.submission.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StandardLibraryNavigationOutputValidatorTest {

    private final StandardLibraryNavigationOutputValidator validator = new StandardLibraryNavigationOutputValidator();

    @Test
    void acceptsDoneNavigationWithKnownAnchorsAndEvidence() {
        ExternalModelStagePayloads.StageValidationResult result = validator.validate(
                StandardLibraryNavigationOutput.builder()
                        .status("DONE")
                        .selectedPaths(List.of(StandardLibraryNavigationOutput.SelectedPath.builder()
                                .knowledgeNodeCode("DS.QUEUE.CIRCULAR.index_wrap")
                                .skillUnitCode("SK_QUEUE_CIRCULAR_INDEX")
                                .mistakePointCode("MP_QUEUE_REAR_WITHOUT_MOD")
                                .libraryFit("HIT")
                                .reason("证据显示队尾推进后没有回绕。")
                                .evidenceRefs(List.of("code:line:18"))
                                .confidence(0.86)
                                .build()))
                        .unresolvedGaps(List.of())
                        .build(),
                brief(),
                pack());

        assertThat(result.isValid()).isTrue();
        assertThat(result.getStage()).isEqualTo("STANDARD_LIBRARY_NAVIGATION");
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.NONE);
    }

    @Test
    void rejectsUnknownStandardLibraryAnchor() {
        ExternalModelStagePayloads.StageValidationResult result = validator.validate(
                StandardLibraryNavigationOutput.builder()
                        .status("DONE")
                        .selectedPaths(List.of(StandardLibraryNavigationOutput.SelectedPath.builder()
                                .knowledgeNodeCode("DS.QUEUE.CIRCULAR.index_wrap")
                                .skillUnitCode("SK_NOT_EXISTS")
                                .libraryFit("HIT")
                                .reason("未知能力点。")
                                .evidenceRefs(List.of("code:line:18"))
                                .confidence(0.7)
                                .build()))
                        .build(),
                brief(),
                pack());

        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(ModelStageFailureReason.INVALID_TAG);
    }

    @Test
    void rejectsContinueWithoutBranchAndInvalidEvidence() {
        ExternalModelStagePayloads.StageValidationResult noBranch = validator.validate(
                StandardLibraryNavigationOutput.builder()
                        .status("CONTINUE")
                        .selectedBranches(List.of())
                        .build(),
                brief(),
                pack());

        assertThat(noBranch.isValid()).isFalse();
        assertThat(noBranch.getMessage()).contains("selectedBranches");

        ExternalModelStagePayloads.StageValidationResult invalidEvidence = validator.validate(
                StandardLibraryNavigationOutput.builder()
                        .status("NO_MATCH")
                        .unresolvedGaps(List.of(StandardLibraryNavigationOutput.UnresolvedGap.builder()
                                .name("库外错因")
                                .reason("证据引用不存在。")
                                .evidenceRefs(List.of("code:line:999"))
                                .confidence(0.5)
                                .build()))
                        .build(),
                brief(),
                pack());

        assertThat(invalidEvidence.isValid()).isFalse();
        assertThat(invalidEvidence.getFailureReason()).isEqualTo(ModelStageFailureReason.INVALID_EVIDENCE_REF);
    }

    private ModelDiagnosisBrief brief() {
        return ModelDiagnosisBrief.builder()
                .verdict("RUNTIME_ERROR")
                .evidenceRefs(List.of("code:line:18", "judge:case:2"))
                .build();
    }

    private StandardLibraryPack pack() {
        return StandardLibraryPack.builder()
                .knowledgeAnchors(List.of(StandardLibraryPack.KnowledgeAnchorOption.builder()
                        .id("DS.QUEUE.CIRCULAR.index_wrap")
                        .name("循环队列下标回绕")
                        .path("队列与循环队列 / 循环队列下标维护")
                        .build()))
                .skillUnits(List.of(StandardLibraryPack.SkillUnitOption.builder()
                        .id("SK_QUEUE_CIRCULAR_INDEX")
                        .name("循环队列下标维护")
                        .primaryKnowledgeNodeCode("DS.QUEUE.CIRCULAR.index_wrap")
                        .build()))
                .mistakePoints(List.of(StandardLibraryPack.MistakePointOption.builder()
                        .id("MP_QUEUE_REAR_WITHOUT_MOD")
                        .name("队尾更新未取模")
                        .skillUnitCode("SK_QUEUE_CIRCULAR_INDEX")
                        .primaryKnowledgeNodeCode("DS.QUEUE.CIRCULAR.index_wrap")
                        .build()))
                .improvementPoints(List.of(StandardLibraryPack.ImprovementPointOption.builder()
                        .id("IP_QUEUE_BOUNDARY_TRACE")
                        .name("用小容量队列追踪边界")
                        .abilityPoint("SK_QUEUE_CIRCULAR_INDEX")
                        .build()))
                .build();
    }
}

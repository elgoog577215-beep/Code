package com.onlinejudge.submission.application;

import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StandardLibraryPackBuilderTest {

    private final StandardLibraryPackBuilder builder =
            new StandardLibraryPackBuilder(new DiagnosisTaxonomy());

    @Test
    void includesTeachingActionsFromFineGrainedTags() {
        ModelDiagnosisBrief brief = ModelDiagnosisBrief.builder()
                .allowedIssueTags(List.of("ALGORITHM_STRATEGY"))
                .allowedFineGrainedTags(List.of("DP_STATE_DESIGN"))
                .build();

        StandardLibraryPack pack = builder.build(brief, null);

        assertThat(pack.getIssueTags())
                .extracting(StandardLibraryPack.TagOption::getId)
                .contains("ALGORITHM_STRATEGY", "NEEDS_MORE_EVIDENCE");
        assertThat(pack.getFineGrainedTags())
                .extracting(StandardLibraryPack.TagOption::getId)
                .contains("DP_STATE_DESIGN");
        assertThat(pack.getTeachingActions())
                .extracting(StandardLibraryPack.TeachingActionOption::getId)
                .contains("CHECK_INVARIANT", "DEFINE_STATE", "COLLECT_EVIDENCE");
        assertThat(pack.getImprovementTags())
                .extracting(StandardLibraryPack.ImprovementTagOption::getId)
                .contains("COMPLEXITY", "TESTING_HABIT", "CODE_CLARITY", "BOUNDARY_AWARENESS", "ROBUSTNESS", "DEBUG_CLEANUP");
        assertThat(pack.getStudentFeedbackRules()).isNotNull();
        assertThat(pack.getStudentFeedbackRules().getBlockingIssueRules())
                .anySatisfy(rule -> assertThat(rule).contains("blockingIssues"));
        assertThat(pack.getDecisionProtocol()).isNotNull();
        assertThat(pack.getDecisionProtocol().getGlobalRules())
                .anySatisfy(rule -> assertThat(rule).contains("most evidence-supported diagnosis"));
        assertThat(pack.getDecisionProtocol().getEvidencePriorityRules())
                .anySatisfy(rule -> assertThat(rule).contains("Visible failed case"));
        assertThat(pack.getDecisionProtocol().getTagSelectionRules())
                .anySatisfy(rule -> assertThat(rule).contains("NEEDS_MORE_EVIDENCE"));
        assertThat(pack.getDecisionProtocol().getConflictRules())
                .anySatisfy(rule -> assertThat(rule).contains("candidate signals conflict"));
        assertThat(pack.getDecisionProtocol().getTeachingActionRules())
                .anySatisfy(rule -> assertThat(rule).contains("COLLECT_EVIDENCE"));
    }

    @Test
    void includesFineGrainedActionsFromRuleSignalsEvenWhenBriefIsBroad() {
        ModelDiagnosisBrief brief = ModelDiagnosisBrief.builder()
                .allowedIssueTags(List.of("BOUNDARY_CONDITION"))
                .build();
        RuleSignalAnalyzer.RuleSignalResult signals = RuleSignalAnalyzer.RuleSignalResult.builder()
                .candidateFineGrainedTags(List.of("OUTPUT_FORMAT_DETAIL"))
                .build();

        StandardLibraryPack pack = builder.build(brief, signals);

        assertThat(pack.getFineGrainedTags())
                .extracting(StandardLibraryPack.TagOption::getId)
                .contains("OUTPUT_FORMAT_DETAIL");
        assertThat(pack.getTeachingActions())
                .extracting(StandardLibraryPack.TeachingActionOption::getId)
                .contains("ASK_MIN_CASE", "COMPARE_OUTPUT");
    }
}

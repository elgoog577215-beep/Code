package com.onlinejudge.classroom.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.classroom.domain.Assignment;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HintSafetyServiceTest {

    private final HintSafetyService service = new HintSafetyService(null, new ObjectMapper(), new DiagnosisTaxonomy());

    @Test
    void downgradesStructuredHintAndInterventionPlanWhenTheyLeakAnswerLikeContent() {
        SubmissionAnalysisResponse analysis = SubmissionAnalysisResponse.builder()
                .submissionId(99L)
                .issueTags(List.of("BOUNDARY_CONDITION"))
                .studentHint("Check the boundary first.")
                .studentHintPlan(SubmissionAnalysisResponse.StudentHintPlan.builder()
                        .hintLevel("L2")
                        .problemType("loop boundary")
                        .evidenceAnchor("line 3")
                        .nextAction("Use this complete code: int main() { return 0; }")
                        .coachQuestion("Copy the answer below.")
                        .teachingAction("TRACE_VARIABLES")
                        .evidenceRefs(List.of("code:loop_present"))
                        .answerLeakRisk("LOW")
                        .build())
                .learningInterventionPlan(SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                        .interventionType("MIN_CASE")
                        .goal("Find the boundary issue.")
                        .studentTask("Paste this full code block: #include <stdio.h>")
                        .checkQuestion("What does the answer code do?")
                        .completionSignal("Student copies the full code.")
                        .evidenceRefs(List.of("code:loop_present"))
                        .estimatedMinutes(5)
                        .answerLeakRisk("LOW")
                        .build())
                .reportMarkdown("ordinary report")
                .answerLeakRisk("LOW")
                .build();

        SubmissionAnalysisResponse result = service.verifyAndRecord(analysis, Assignment.HintPolicy.L2);

        assertThat(result.getAnswerLeakRisk()).isEqualTo("HIGH");
        assertThat(result.getStudentHintPlan()).isNotNull();
        assertThat(result.getStudentHintPlan().getTeachingAction()).isEqualTo("COLLECT_EVIDENCE");
        assertThat(result.getLearningInterventionPlan()).isNotNull();
        assertThat(result.getLearningInterventionPlan().getInterventionType()).isEqualTo("COLLECT_EVIDENCE");
        assertThat(result.getLearningInterventionPlan().getAnswerLeakRisk()).isEqualTo("HIGH");
        assertThat(result.getLearningInterventionPlan().getStudentTask()).doesNotContain("int main", "#include");
        assertThat(result.getReportMarkdown()).doesNotContain("int main", "#include");
    }
}

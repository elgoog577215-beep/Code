package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.learning.diagnosis.DiagnosisReportReader;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import com.onlinejudge.submission.persistence.SubmissionAnalysisRepository;
import com.onlinejudge.submission.persistence.SubmissionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubmissionAnalysisServiceHistoryEvidenceTest {

    @Test
    void historyEvidenceCarriesPreviousLearningActionFeedback() {
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        SubmissionAnalysisRepository analysisRepository = mock(SubmissionAnalysisRepository.class);
        DiagnosisTaxonomy taxonomy = new DiagnosisTaxonomy();
        DiagnosisReportReader reportReader = new DiagnosisReportReader(new ObjectMapper(), taxonomy);
        SubmissionAnalysisService service = new SubmissionAnalysisService(
                submissionRepository,
                null,
                null,
                analysisRepository,
                new ObjectMapper(),
                null,
                null,
                taxonomy,
                null,
                reportReader,
                null,
                null
        );
        Submission current = submission(22L, Submission.Verdict.WRONG_ANSWER, 10);
        Submission previous = submission(21L, Submission.Verdict.WRONG_ANSWER, 0);
        SubmissionAnalysis previousAnalysis = SubmissionAnalysis.builder()
                .submissionId(21L)
                .analysisSource("TEST")
                .scenario("WA")
                .headline("diagnosis")
                .summary("summary")
                .reportMarkdown("markdown")
                .reportJson("""
                        {
                          "issueTags": ["BOUNDARY_CONDITION"],
                          "fineGrainedTags": ["OFF_BY_ONE"],
                          "learningInterventionPlan": {
                            "interventionType": "MIN_CASE_TRACE",
                            "studentTask": "Trace one minimal failing input.",
                            "completionSignal": "Student identifies the first divergence.",
                            "evidenceRefs": ["eval:intervention"]
                          },
                          "learningActionEvidence": {
                            "expectedActionType": "MIN_CASE_TRACE",
                            "executionStatus": "NOT_OBSERVED",
                            "observedEvidence": "Waiting for follow-up.",
                            "confidence": 0.5,
                            "evidenceRefs": ["eval:intervention"],
                            "nextAdjustment": "Wait for follow-up."
                          }
                        }
                        """)
                .build();

        when(submissionRepository.findByAssignmentIdAndProblemIdAndStudentProfileIdOrderBySubmittedAtDesc(7L, 101L, 41L))
                .thenReturn(List.of(previous));
        when(analysisRepository.findBySubmissionIdIn(List.of(21L))).thenReturn(List.of(previousAnalysis));

        DiagnosisEvidencePackage.HistoryEvidence history = service.buildHistoryEvidenceForTest(current);

        assertThat(history.getPreviousInterventionType()).isEqualTo("MIN_CASE_TRACE");
        assertThat(history.getPreviousInterventionTask()).isEqualTo("Trace one minimal failing input.");
        assertThat(history.getPreviousInterventionCompletionSignal()).isEqualTo("Student identifies the first divergence.");
        assertThat(history.getPreviousLearningActionStatus()).isEqualTo("CONTRADICTED");
        assertThat(history.getPreviousLearningActionConfidence()).isEqualTo(0.74);
        assertThat(history.getPreviousLearningActionEvidenceRefs())
                .contains("eval:intervention", "followup:submission:22", "action:CONTRADICTED");
        assertThat(history.getPreviousLearningActionSummary()).contains("WRONG_ANSWER");
        assertThat(history.getPreviousLearningActionNextAdjustment()).contains("降低提示粒度");
    }

    private Submission submission(Long id, Submission.Verdict verdict, int minutesAfter) {
        return Submission.builder()
                .id(id)
                .assignmentId(7L)
                .studentProfileId(41L)
                .problemId(101L)
                .languageId(71)
                .languageName("Python 3")
                .sourceCode("print(1)")
                .verdict(verdict)
                .submittedAt(LocalDateTime.of(2026, 5, 24, 10, 0).plusMinutes(minutesAfter))
                .build();
    }
}

package com.onlinejudge.learning.diagnosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosisReportReaderInterventionTest {

    private final DiagnosisReportReader reader = new DiagnosisReportReader(new ObjectMapper(), new DiagnosisTaxonomy());

    @Test
    void readsLearningInterventionPlanSnapshotWhenPresent() {
        SubmissionAnalysis analysis = SubmissionAnalysis.builder()
                .headline("intervention")
                .reportJson("""
                        {
                          "issueTags": ["BOUNDARY_CONDITION"],
                          "learningInterventionPlan": {
                            "interventionType": "VARIABLE_TRACE",
                            "goal": "Turn the boundary diagnosis into an observable trace.",
                            "studentTask": "Trace n=1 and n=2 before editing code.",
                            "checkQuestion": "Where does the first value diverge?",
                            "completionSignal": "Student provides expected and actual variable values.",
                            "evidenceRefs": ["code:plus_minus_one"],
                            "estimatedMinutes": 7,
                            "answerLeakRisk": "LOW"
                          }
                        }
                        """)
                .build();

        var snapshot = reader.learningInterventionPlan(analysis);

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.interventionType()).isEqualTo("VARIABLE_TRACE");
        assertThat(snapshot.goal()).contains("observable trace");
        assertThat(snapshot.studentTask()).contains("Trace n=1");
        assertThat(snapshot.checkQuestion()).contains("diverge");
        assertThat(snapshot.completionSignal()).contains("variable values");
        assertThat(snapshot.evidenceRefs()).containsExactly("code:plus_minus_one");
        assertThat(snapshot.estimatedMinutes()).isEqualTo(7);
        assertThat(snapshot.answerLeakRisk()).isEqualTo("LOW");
    }

    @Test
    void returnsNullWhenLearningInterventionPlanIsMissing() {
        SubmissionAnalysis analysis = SubmissionAnalysis.builder()
                .headline("old report")
                .reportJson("""
                        {
                          "issueTags": ["TIME_COMPLEXITY"]
                        }
                        """)
                .build();

        assertThat(reader.learningInterventionPlan(analysis)).isNull();
    }
}

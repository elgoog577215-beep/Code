package com.onlinejudge.submission.persistence;

import com.onlinejudge.submission.domain.StudentAiFeedbackRevision;
import com.onlinejudge.submission.domain.SubmissionDiagnosisFact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SubmissionEvidencePersistenceTest {

    @Autowired
    StudentAiFeedbackRevisionRepository revisionRepository;

    @Autowired
    SubmissionDiagnosisFactRepository factRepository;

    @Test
    void storesMultipleFeedbackVersionsAndAllKnowledgePathStatuses() {
        revisionRepository.save(revision("generation-1", 1, "FAILED"));
        revisionRepository.save(revision("generation-2", 2, "READY"));

        assertThat(revisionRepository.findBySubmissionIdOrderByVersionNumberDesc(7L))
                .extracting(StudentAiFeedbackRevision::getVersionNumber)
                .containsExactly(2, 1);
        assertThat(revisionRepository.findBySubmissionIdAndGenerationKey(7L, "generation-2")).isPresent();

        List<String> statuses = List.of("FORMAL", "PROVISIONAL", "INFERRED", "UNCLASSIFIED");
        for (int index = 0; index < statuses.size(); index++) {
            factRepository.save(SubmissionDiagnosisFact.builder()
                    .submissionId(7L)
                    .analysisId(8L)
                    .factKey("fact-" + index)
                    .issueId("I" + index)
                    .factType("REPAIR")
                    .title("问题" + index)
                    .knowledgePathJson(index == 3 ? "[]" : "[\"基础\",\"循环\"]")
                    .knowledgePathStatus(statuses.get(index))
                    .libraryFit(index == 0 ? "HIT" : "UNKNOWN")
                    .projectionStatus("READY")
                    .build());
        }

        assertThat(factRepository.findByAnalysisId(8L))
                .extracting(SubmissionDiagnosisFact::getKnowledgePathStatus)
                .containsExactlyInAnyOrderElementsOf(statuses);
        assertThat(factRepository.existsByFactKey("fact-0")).isTrue();
    }

    private StudentAiFeedbackRevision revision(String generationKey, int version, String status) {
        return StudentAiFeedbackRevision.builder()
                .submissionId(7L)
                .versionNumber(version)
                .generationKey(generationKey)
                .status(status)
                .source("MODEL")
                .feedbackJson("{}")
                .generatedAt(LocalDateTime.now())
                .build();
    }
}

package com.onlinejudge.submission.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import com.onlinejudge.submission.domain.SubmissionDiagnosisFact;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import com.onlinejudge.submission.persistence.SubmissionDiagnosisFactRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmissionDiagnosisFactProjectorTest {

    @Test
    void projectsMultiplePathStatusesAndReplayIsIdempotent() {
        SubmissionDiagnosisFactRepository repository = mock(SubmissionDiagnosisFactRepository.class);
        when(repository.existsByFactKey(any())).thenReturn(false, false, true, true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        SubmissionDiagnosisFactProjector projector = new SubmissionDiagnosisFactProjector(repository, new ObjectMapper());
        SubmissionAnalysis analysis = SubmissionAnalysis.builder().id(8L).submissionId(7L).build();
        SubmissionAnalysisResponse response = SubmissionAnalysisResponse.builder()
                .basicLayerAdvice(List.of(
                        SubmissionAnalysisResponse.BasicLayerAdvice.builder()
                                .issueId("I1")
                                .title("边界错误")
                                .mistakePointId("MP_BOUNDARY")
                                .knowledgePath(List.of("基础", "循环", "边界", "边界错误"))
                                .knowledgePathStatus("FORMAL")
                                .evidenceRefs(List.of("code:line:3"))
                                .build(),
                        SubmissionAnalysisResponse.BasicLayerAdvice.builder()
                                .issueId("I2")
                                .title("临时细分问题")
                                .knowledgePath(List.of("基础", "循环", "临时路径"))
                                .knowledgePathStatus("PROVISIONAL")
                                .build(),
                        SubmissionAnalysisResponse.BasicLayerAdvice.builder()
                                .issueId("I3")
                                .title("旧记录缺少路径状态")
                                .knowledgePath(List.of())
                                .build()
                ))
                .build();

        when(repository.existsByFactKey(any())).thenReturn(false, false, false, true, true, true);
        assertThat(projector.project(analysis, response).inserted()).isEqualTo(3);
        assertThat(projector.project(analysis, response).inserted()).isZero();

        ArgumentCaptor<SubmissionDiagnosisFact> captor = ArgumentCaptor.forClass(SubmissionDiagnosisFact.class);
        verify(repository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(SubmissionDiagnosisFact::getKnowledgePathStatus)
                .containsExactly("FORMAL", "PROVISIONAL", "UNCLASSIFIED");
        assertThat(captor.getAllValues()).allSatisfy(fact -> {
            assertThat(fact.getSubmissionId()).isEqualTo(7L);
            assertThat(fact.getAnalysisId()).isEqualTo(8L);
            assertThat(fact.getFactKey()).isNotBlank();
        });
    }
}

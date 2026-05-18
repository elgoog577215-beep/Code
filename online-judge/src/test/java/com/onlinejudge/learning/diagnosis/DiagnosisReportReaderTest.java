package com.onlinejudge.learning.diagnosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosisReportReaderTest {

    private final DiagnosisReportReader reader = new DiagnosisReportReader(new ObjectMapper(), new DiagnosisTaxonomy());

    @Test
    void readsNewReportWithFineGrainedTags() {
        SubmissionAnalysis analysis = SubmissionAnalysis.builder()
                .headline("边界问题")
                .reportJson("""
                        {
                          "issueTags": ["BOUNDARY_CONDITION"],
                          "fineGrainedTags": ["OFF_BY_ONE", "UNKNOWN_FINE"],
                          "studentHint": "检查循环边界",
                          "progressSignal": "正在定位边界",
                          "confidence": 0.82,
                          "uncertainty": "隐藏测试点只提供脱敏摘要",
                          "answerLeakRisk": "LOW"
                        }
                        """)
                .build();

        assertThat(reader.issueTags(analysis)).containsExactly("BOUNDARY_CONDITION");
        assertThat(reader.fineGrainedTags(analysis)).containsExactly("OFF_BY_ONE");
        assertThat(reader.studentHint(analysis)).isEqualTo("检查循环边界");
        assertThat(reader.progressSignal(analysis)).isEqualTo("正在定位边界");
        assertThat(reader.confidence(analysis)).isEqualTo(0.82);
        assertThat(reader.uncertainty(analysis)).isEqualTo("隐藏测试点只提供脱敏摘要");
        assertThat(reader.answerLeakRisk(analysis)).isEqualTo("LOW");
    }

    @Test
    void keepsOldReportWithoutFineGrainedTagsReadable() {
        SubmissionAnalysis analysis = SubmissionAnalysis.builder()
                .headline("旧版诊断标题")
                .reportJson("""
                        {
                          "issueTags": ["TIME_COMPLEXITY"]
                        }
                        """)
                .build();

        assertThat(reader.issueTags(analysis)).containsExactly("TIME_COMPLEXITY");
        assertThat(reader.fineGrainedTags(analysis)).isEmpty();
        assertThat(reader.studentHint(analysis)).isEmpty();
        assertThat(reader.confidence(analysis)).isNull();
        assertThat(reader.uncertainty(analysis)).isEmpty();
        assertThat(reader.answerLeakRisk(analysis)).isEqualTo("UNKNOWN");
    }

    @Test
    void fallsBackToHeadlineWhenReportJsonIsInvalid() {
        SubmissionAnalysis analysis = SubmissionAnalysis.builder()
                .headline("旧版轻量标题")
                .reportJson("{broken")
                .build();

        assertThat(reader.issueTags(analysis)).containsExactly("旧版轻量标题");
        assertThat(reader.fineGrainedTags(analysis)).isEmpty();
    }
}

package com.onlinejudge.submission.application;

import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryService;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchLocationRetrievalServiceTest {

    @Test
    void textRetrievalRanksBoundaryMistakeFromCodeAndSignals() {
        AiStandardLibraryService libraryService = mock(AiStandardLibraryService.class);
        when(libraryService.enabledSearchLocationItems()).thenReturn(List.of(
                item("MP_RANGE_RIGHT_ENDPOINT_MISSING", AiStandardLibraryLayer.MISTAKE_POINT,
                        "循环边界", "Python range 右端不包含 n，导致闭区间最后一项漏处理。"),
                item("SK_COMPLEXITY_ESTIMATION", AiStandardLibraryLayer.SKILL_UNIT,
                        "复杂度估算", "根据数据范围估算循环次数。")
        ));
        SearchLocationProperties properties = new SearchLocationProperties();
        properties.setMode("text");
        properties.setCandidateLimit(10);
        SearchLocationRetrievalService service = new SearchLocationRetrievalService(
                libraryService,
                properties,
                mock(EmbeddingClient.class)
        );

        SearchLocationCandidatePack pack = service.retrieve(brief(), ruleSignals());

        assertThat(pack.getEmbeddingStatus()).isEqualTo("DISABLED");
        assertThat(pack.getCandidates()).isNotEmpty();
        assertThat(pack.getCandidates().get(0).getId()).isEqualTo("MP_RANGE_RIGHT_ENDPOINT_MISSING");
        assertThat(pack.getCandidates().get(0).getMatchedSignals()).contains("verdict:WRONG_ANSWER");
    }

    @Test
    void hybridRetrievalFallsBackToTextWhenEmbeddingFails() {
        AiStandardLibraryService libraryService = mock(AiStandardLibraryService.class);
        when(libraryService.enabledSearchLocationItems()).thenReturn(List.of(
                item("MP_RANGE_RIGHT_ENDPOINT_MISSING", AiStandardLibraryLayer.MISTAKE_POINT,
                        "循环边界", "Python range 右端不包含 n，导致闭区间最后一项漏处理。"),
                item("SK_COMPLEXITY_ESTIMATION", AiStandardLibraryLayer.SKILL_UNIT,
                        "复杂度估算", "根据数据范围估算循环次数。")
        ));
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        when(embeddingClient.embed(anyString()))
                .thenReturn(EmbeddingClient.EmbeddingResponse.failed("EMBEDDING_HTTP_429", List.of()));
        SearchLocationProperties properties = new SearchLocationProperties();
        properties.setEnabled(true);
        properties.setMode("hybrid");
        properties.setCandidateLimit(10);
        SearchLocationRetrievalService service = new SearchLocationRetrievalService(
                libraryService,
                properties,
                embeddingClient
        );

        SearchLocationCandidatePack pack = service.retrieve(brief(), ruleSignals());

        assertThat(pack.getEmbeddingStatus()).isEqualTo("VECTOR_DEGRADED:EMBEDDING_HTTP_429");
        assertThat(pack.getFallbackReason()).isEqualTo("EMBEDDING_HTTP_429");
        assertThat(pack.getCandidates()).isNotEmpty();
        assertThat(pack.getCandidates().get(0).getId()).isEqualTo("MP_RANGE_RIGHT_ENDPOINT_MISSING");
    }

    private AiStandardLibraryItem item(String code, AiStandardLibraryLayer layer, String category, String description) {
        return AiStandardLibraryItem.builder()
                .id((long) Math.abs(code.hashCode()))
                .layer(layer)
                .code(code)
                .category(category)
                .name(description)
                .description(description)
                .knowledgeNodeCodes("BASIC.LOOP.BOUNDARY")
                .enabled(true)
                .libraryVersion("test")
                .build();
    }

    private ModelDiagnosisBrief brief() {
        return ModelDiagnosisBrief.builder()
                .problemBrief("输入 n，输出 1 到 n 的和。")
                .verdict("WRONG_ANSWER")
                .language("Python 3")
                .keyCodeExcerpt("for i in range(1, n): total += i")
                .allowedIssueTags(List.of("LOOP_BOUNDARY"))
                .allowedFineGrainedTags(List.of("OFF_BY_ONE"))
                .evidenceRefs(List.of("code:range_excludes_n"))
                .build();
    }

    private RuleSignalAnalyzer.RuleSignalResult ruleSignals() {
        return RuleSignalAnalyzer.RuleSignalResult.builder()
                .candidateIssueTags(List.of("LOOP_BOUNDARY"))
                .candidateFineGrainedTags(List.of("OFF_BY_ONE"))
                .evidenceRefs(List.of("code:range_excludes_n"))
                .build();
    }
}

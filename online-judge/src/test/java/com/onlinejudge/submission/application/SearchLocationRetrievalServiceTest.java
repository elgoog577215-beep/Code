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
    void textRetrievalRanksBoundaryMistakeFromCodeAndLibraryText() {
        AiStandardLibraryService libraryService = mock(AiStandardLibraryService.class);
        when(libraryService.enabledSearchLocationItems()).thenReturn(List.of(
                item("SK_RANGE_BOUNDARY", AiStandardLibraryLayer.SKILL_UNIT,
                        "循环边界", "能判断循环区间是否包含题目要求的答案。", "SK_RANGE_BOUNDARY"),
                item("MP_RANGE_RIGHT_ENDPOINT_MISSING", AiStandardLibraryLayer.MISTAKE_POINT,
                        "循环边界", "Python range 右端不包含 n，导致闭区间最后一项漏处理。", "SK_RANGE_BOUNDARY"),
                item("MP_RANGE_LEFT_ENDPOINT_EXTRA", AiStandardLibraryLayer.MISTAKE_POINT,
                        "循环边界", "循环左端点多取或少取，导致范围和题目不一致。", "SK_RANGE_BOUNDARY"),
                item("IP_BOUNDARY_TESTING", AiStandardLibraryLayer.IMPROVEMENT_POINT,
                        "边界验证", "用最小、最大、不存在答案样例验证循环模板。", "SK_RANGE_BOUNDARY"),
                item("SK_COMPLEXITY_ESTIMATION", AiStandardLibraryLayer.SKILL_UNIT,
                        "复杂度估算", "根据数据范围估算循环次数。", "SK_COMPLEXITY_ESTIMATION")
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
        assertThat(pack.getRecallSources()).contains("STRUCTURE", "KEYWORD");
        assertThat(pack.getRecallSources()).doesNotContain("RULE_SIGNAL");
        assertThat(pack.getCandidates()).isNotEmpty();
        assertThat(pack.getCandidates().get(0).getId()).isEqualTo("MP_RANGE_RIGHT_ENDPOINT_MISSING");
        assertThat(pack.getCandidates().get(0).getMatchedSignals()).contains("verdict:WRONG_ANSWER");
        assertThat(pack.getCandidates().get(0).getParentKnowledgePath()).isEqualTo("BASIC > LOOP > BOUNDARY");
        assertThat(pack.getCandidates().get(0).getParentSkillUnitId()).isEqualTo("SK_RANGE_BOUNDARY");
        assertThat(pack.getCandidates().get(0).getPrimaryKnowledgeNodeCode()).isEqualTo("BASIC.LOOP.BOUNDARY");
        assertThat(pack.getCandidates().get(0).getStructurePath())
                .contains("BASIC > LOOP > BOUNDARY", "SK_RANGE_BOUNDARY", "MP_RANGE_RIGHT_ENDPOINT_MISSING");
        assertThat(pack.getCandidates().get(0).getSiblingMistakePointIds()).contains("MP_RANGE_LEFT_ENDPOINT_EXTRA");
        assertThat(pack.getCandidates().get(0).getRelatedImprovementPointIds()).contains("IP_BOUNDARY_TESTING");
        assertThat(pack.getCandidates().get(0).getExtensionCandidateIds())
                .contains("SK_RANGE_BOUNDARY", "MP_RANGE_LEFT_ENDPOINT_EXTRA", "IP_BOUNDARY_TESTING");

        assertThat(pack.getCandidates())
                .filteredOn(candidate -> "SK_RANGE_BOUNDARY".equals(candidate.getId()))
                .singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.getChildMistakePointIds())
                            .contains("MP_RANGE_RIGHT_ENDPOINT_MISSING", "MP_RANGE_LEFT_ENDPOINT_EXTRA");
                    assertThat(candidate.getRelatedImprovementPointIds()).contains("IP_BOUNDARY_TESTING");
                });
    }

    @Test
    void hybridRetrievalFallsBackToTextWhenEmbeddingFails() {
        AiStandardLibraryService libraryService = mock(AiStandardLibraryService.class);
        when(libraryService.enabledSearchLocationItems()).thenReturn(List.of(
                item("MP_RANGE_RIGHT_ENDPOINT_MISSING", AiStandardLibraryLayer.MISTAKE_POINT,
                        "循环边界", "Python range 右端不包含 n，导致闭区间最后一项漏处理。", "SK_RANGE_BOUNDARY"),
                item("SK_COMPLEXITY_ESTIMATION", AiStandardLibraryLayer.SKILL_UNIT,
                        "复杂度估算", "根据数据范围估算循环次数。", "SK_COMPLEXITY_ESTIMATION")
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

    private AiStandardLibraryItem item(String code,
                                       AiStandardLibraryLayer layer,
                                       String category,
                                       String description,
                                       String skillUnitCode) {
        return AiStandardLibraryItem.builder()
                .id((long) Math.abs(code.hashCode()))
                .layer(layer)
                .code(code)
                .category(category)
                .name(description)
                .description(description)
                .skillUnitCode(skillUnitCode)
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

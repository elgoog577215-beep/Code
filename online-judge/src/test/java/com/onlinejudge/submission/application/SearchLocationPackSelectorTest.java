package com.onlinejudge.submission.application;

import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryService;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchLocationPackSelectorTest {

    @Test
    void selectedOutputBuildsCompactStandardLibraryPackWithCompatibleFields() {
        AiStandardLibraryService libraryService = mock(AiStandardLibraryService.class);
        when(libraryService.enabledSearchLocationItems()).thenReturn(List.of(
                item("SK_RANGE_BOUNDARY", AiStandardLibraryLayer.SKILL_UNIT),
                item("MP_RANGE_RIGHT_ENDPOINT_MISSING", AiStandardLibraryLayer.MISTAKE_POINT),
                item("IP_BOUNDARY_TESTING", AiStandardLibraryLayer.IMPROVEMENT_POINT)
        ));
        SearchLocationPackSelector selector = new SearchLocationPackSelector(libraryService, new DiagnosisTaxonomy());

        StandardLibraryPack pack = selector.select(output(), candidatePack(), fallbackPack());

        assertThat(pack.getBasicCauses()).extracting(StandardLibraryPack.BasicCauseOption::getId)
                .containsExactly("MP_RANGE_RIGHT_ENDPOINT_MISSING");
        assertThat(pack.getMistakePoints()).extracting(StandardLibraryPack.MistakePointOption::getId)
                .containsExactly("MP_RANGE_RIGHT_ENDPOINT_MISSING");
        assertThat(pack.getSkillUnits()).extracting(StandardLibraryPack.SkillUnitOption::getId)
                .containsExactly("SK_RANGE_BOUNDARY");
        assertThat(pack.getSkillUnits().get(0).getPrimaryKnowledgeNodeCode()).isEqualTo("BASIC.LOOP.BOUNDARY");
        assertThat(pack.getSkillUnits().get(0).getRelatedKnowledgeNodeCodes()).containsExactly("BASIC.LOOP.FOR");
        assertThat(pack.getImprovementPoints()).extracting(StandardLibraryPack.ImprovementPointOption::getId)
                .containsExactly("IP_BOUNDARY_TESTING");
        assertThat(pack.getKnowledgeAnchors()).extracting(StandardLibraryPack.KnowledgeAnchorOption::getId)
                .contains("BASIC.LOOP.BOUNDARY");
        assertThat(pack.getIssueTags()).extracting(StandardLibraryPack.TagOption::getId)
                .containsExactly("LOOP_BOUNDARY");
        assertThat(pack.getFineGrainedTags()).extracting(StandardLibraryPack.TagOption::getId)
                .containsExactly("OFF_BY_ONE");
        assertThat(pack.getSearchLocationSummary().getStatus()).isEqualTo("SUCCESS");
        assertThat(pack.getSearchLocationSummary().getCandidateCount()).isEqualTo(3);
        assertThat(pack.getSearchLocationSummary().getSelectedCount()).isEqualTo(3);
    }

    @Test
    void selectedMistakeExpandsParentSiblingImprovementAndStructuredView() {
        AiStandardLibraryService libraryService = mock(AiStandardLibraryService.class);
        when(libraryService.enabledSearchLocationItems()).thenReturn(List.of(
                item("SK_RANGE_BOUNDARY", AiStandardLibraryLayer.SKILL_UNIT),
                item("MP_RANGE_RIGHT_ENDPOINT_MISSING", AiStandardLibraryLayer.MISTAKE_POINT),
                item("MP_RANGE_LEFT_ENDPOINT_EXTRA", AiStandardLibraryLayer.MISTAKE_POINT),
                item("IP_BOUNDARY_TESTING", AiStandardLibraryLayer.IMPROVEMENT_POINT)
        ));
        SearchLocationPackSelector selector = new SearchLocationPackSelector(libraryService, new DiagnosisTaxonomy());

        StandardLibraryPack pack = selector.select(outputWithOnlyMistake(), candidatePackWithNeighborhood(), fallbackPack());

        assertThat(pack.getStructureVersion()).isEqualTo(StandardLibraryPack.STRUCTURE_VERSION);
        assertThat(pack.getSkillUnits()).extracting(StandardLibraryPack.SkillUnitOption::getId)
                .containsExactly("SK_RANGE_BOUNDARY");
        assertThat(pack.getMistakePoints()).extracting(StandardLibraryPack.MistakePointOption::getId)
                .containsExactly("MP_RANGE_RIGHT_ENDPOINT_MISSING", "MP_RANGE_LEFT_ENDPOINT_EXTRA");
        assertThat(pack.getMistakePoints().get(0).getSkillUnitCode()).isEqualTo("SK_RANGE_BOUNDARY");
        assertThat(pack.getMistakePoints().get(0).getPrimaryKnowledgeNodeCode()).isEqualTo("BASIC.LOOP.BOUNDARY");
        assertThat(pack.getImprovementPoints()).extracting(StandardLibraryPack.ImprovementPointOption::getId)
                .containsExactly("IP_BOUNDARY_TESTING");
        assertThat(pack.getKnowledgeGroups()).singleElement().satisfies(group -> {
            assertThat(group.getId()).isEqualTo("BASIC.LOOP.BOUNDARY");
            assertThat(group.getSkillUnits()).singleElement().satisfies(skillGroup -> {
                assertThat(skillGroup.getSkillUnit().getId()).isEqualTo("SK_RANGE_BOUNDARY");
                assertThat(skillGroup.getMistakePoints())
                        .extracting(StandardLibraryPack.MistakePointOption::getId)
                        .containsExactly("MP_RANGE_RIGHT_ENDPOINT_MISSING", "MP_RANGE_LEFT_ENDPOINT_EXTRA");
                assertThat(skillGroup.getImprovementPoints())
                        .extracting(StandardLibraryPack.ImprovementPointOption::getId)
                        .containsExactly("IP_BOUNDARY_TESTING");
                assertThat(skillGroup.getCandidateIds())
                        .containsExactly(
                                "SK_RANGE_BOUNDARY",
                                "MP_RANGE_RIGHT_ENDPOINT_MISSING",
                                "MP_RANGE_LEFT_ENDPOINT_EXTRA",
                                "IP_BOUNDARY_TESTING"
                        );
            });
        });
        assertThat(pack.getSearchLocationSummary().getSelectedCount()).isEqualTo(4);
    }

    private SearchLocationOutput output() {
        return SearchLocationOutput.builder()
                .basicCandidates(List.of(SearchLocationOutput.SelectedCandidate.builder()
                        .id("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                        .layer("MISTAKE_POINT")
                        .confidence(0.94)
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .build()))
                .improvementCandidates(List.of(SearchLocationOutput.SelectedCandidate.builder()
                        .id("IP_BOUNDARY_TESTING")
                        .layer("IMPROVEMENT_POINT")
                        .confidence(0.82)
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .build()))
                .knowledgeAnchors(List.of(SearchLocationOutput.SelectedCandidate.builder()
                        .id("SK_RANGE_BOUNDARY")
                        .layer("SKILL_UNIT")
                        .confidence(0.9)
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .build()))
                .uncertainty("可见证据明确。")
                .needsMoreEvidence(false)
                .build();
    }

    private SearchLocationOutput outputWithOnlyMistake() {
        return SearchLocationOutput.builder()
                .basicCandidates(List.of(SearchLocationOutput.SelectedCandidate.builder()
                        .id("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                        .layer("MISTAKE_POINT")
                        .confidence(0.94)
                        .evidenceRefs(List.of("code:range_excludes_n"))
                        .build()))
                .uncertainty("只命中一个易错点，由结构邻域补齐上下文。")
                .needsMoreEvidence(false)
                .build();
    }

    private SearchLocationCandidatePack candidatePack() {
        return SearchLocationCandidatePack.builder()
                .embeddingStatus("DISABLED")
                .candidateCount(3)
                .candidates(List.of(
                        SearchLocationCandidate.builder().id("MP_RANGE_RIGHT_ENDPOINT_MISSING").layer("MISTAKE_POINT").build(),
                        SearchLocationCandidate.builder().id("IP_BOUNDARY_TESTING").layer("IMPROVEMENT_POINT").build(),
                        SearchLocationCandidate.builder().id("SK_RANGE_BOUNDARY").layer("SKILL_UNIT").build()
                ))
                .build();
    }

    private SearchLocationCandidatePack candidatePackWithNeighborhood() {
        return SearchLocationCandidatePack.builder()
                .embeddingStatus("DISABLED")
                .candidateCount(1)
                .candidates(List.of(SearchLocationCandidate.builder()
                        .id("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                        .layer("MISTAKE_POINT")
                        .parentSkillUnitId("SK_RANGE_BOUNDARY")
                        .siblingMistakePointIds(List.of("MP_RANGE_LEFT_ENDPOINT_EXTRA"))
                        .relatedImprovementPointIds(List.of("IP_BOUNDARY_TESTING"))
                        .extensionCandidateIds(List.of("SK_RANGE_BOUNDARY"))
                        .build()))
                .build();
    }

    private StandardLibraryPack fallbackPack() {
        return StandardLibraryPack.builder()
                .schemaVersion(StandardLibraryPack.SCHEMA_VERSION)
                .taxonomyVersion(DiagnosisTaxonomy.TAXONOMY_VERSION)
                .issueTags(List.of(StandardLibraryPack.TagOption.builder()
                        .id("LOOP_BOUNDARY")
                        .label("循环边界")
                        .teachingAction("TRACE_VARIABLES")
                        .build()))
                .fineGrainedTags(List.of(StandardLibraryPack.TagOption.builder()
                        .id("OFF_BY_ONE")
                        .label("差一错误")
                        .parentTag("LOOP_BOUNDARY")
                        .teachingAction("TRACE_VARIABLES")
                        .build()))
                .improvementTags(List.of(StandardLibraryPack.ImprovementTagOption.builder()
                        .id("TESTING_HABIT")
                        .label("测试习惯")
                        .build()))
                .teachingActions(List.of(StandardLibraryPack.TeachingActionOption.builder()
                        .id("TRACE_VARIABLES")
                        .label("变量追踪")
                        .build()))
                .build();
    }

    private AiStandardLibraryItem item(String code, AiStandardLibraryLayer layer) {
        return AiStandardLibraryItem.builder()
                .id((long) Math.abs(code.hashCode()))
                .layer(layer)
                .code(code)
                .category("循环边界")
                .name(code)
                .description("循环边界细颗粒条目")
                .studentExplanation("用手推变量观察边界。")
                .teacherExplanation("用于定位 range 右端不包含问题。")
                .skillUnitCode("SK_RANGE_BOUNDARY")
                .mistakeType("OFF_BY_ONE")
                .commonMisconception("误以为右端会被包含。")
                .evidenceSignals("code:range_excludes_n")
                .commonCodePatterns("range(1, n)")
                .judgeSignals("WRONG_ANSWER")
                .requiredEvidence("code:range_excludes_n")
                .whenToUse("通过后补边界样例。")
                .studentBenefit("降低边界遗漏。")
                .hintL1("先看循环变量取值。")
                .hintL2("列出最小样例的取值表。")
                .hintL3("比较题目要求和实际循环范围。")
                .abilityPoint("循环边界")
                .severity("HIGH")
                .applicableLanguages("PYTHON")
                .relatedItems("MP_RANGE_RIGHT_ENDPOINT_MISSING")
                .primaryKnowledgeNodeCode("BASIC.LOOP.BOUNDARY")
                .knowledgeNodeCodes("BASIC.LOOP.BOUNDARY")
                .relatedKnowledgeNodeCodes("BASIC.LOOP.FOR")
                .teachingAction("TRACE_VARIABLES")
                .enabled(true)
                .libraryVersion("test-v1")
                .build();
    }
}

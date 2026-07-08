package com.onlinejudge.submission.application;

import com.onlinejudge.learning.knowledge.domain.InformaticsKnowledgeNode;
import com.onlinejudge.learning.knowledge.domain.InformaticsKnowledgeNodeType;
import com.onlinejudge.learning.knowledge.persistence.InformaticsKnowledgeNodeRepository;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryService;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StandardLibraryNavigationPackBuilderTest {

    @Test
    void buildsSelectedStructuredPackFromAiNavigationOutput() {
        AiStandardLibraryService libraryService = mock(AiStandardLibraryService.class);
        InformaticsKnowledgeNodeRepository knowledgeRepository = mock(InformaticsKnowledgeNodeRepository.class);
        StandardLibraryNavigationPackBuilder builder =
                new StandardLibraryNavigationPackBuilder(libraryService, knowledgeRepository);

        when(libraryService.findFormalItemAsLegacy(AiStandardLibraryLayer.SKILL_UNIT, "SK_QUEUE_CIRCULAR_INDEX"))
                .thenReturn(Optional.of(skill()));
        when(libraryService.findFormalItemAsLegacy(AiStandardLibraryLayer.MISTAKE_POINT, "MP_QUEUE_REAR_WITHOUT_MOD"))
                .thenReturn(Optional.of(mistake()));
        when(libraryService.findFormalItemAsLegacy(AiStandardLibraryLayer.IMPROVEMENT_POINT, "IP_QUEUE_BOUNDARY_TRACE"))
                .thenReturn(Optional.of(improvement()));
        when(knowledgeRepository.findByCode("DS.QUEUE.CIRCULAR.index_wrap"))
                .thenReturn(Optional.of(InformaticsKnowledgeNode.builder()
                        .code("DS.QUEUE.CIRCULAR.index_wrap")
                        .type(InformaticsKnowledgeNodeType.KNOWLEDGE_POINT)
                        .name("循环队列下标回绕")
                        .path("数据结构 / 队列与循环队列 / 循环队列下标回绕")
                        .description("队首、队尾下标在固定容量数组中推进后需要回绕。")
                        .enabled(true)
                        .build()));

        StandardLibraryPack pack = builder.build(StandardLibraryNavigationOutput.builder()
                .status("DONE")
                .selectedPaths(List.of(StandardLibraryNavigationOutput.SelectedPath.builder()
                        .knowledgeNodeCode("DS.QUEUE.CIRCULAR.index_wrap")
                        .skillUnitCode("SK_QUEUE_CIRCULAR_INDEX")
                        .mistakePointCode("MP_QUEUE_REAR_WITHOUT_MOD")
                        .improvementPointCode("IP_QUEUE_BOUNDARY_TRACE")
                        .libraryFit("HIT")
                        .evidenceRefs(List.of("code:line:18"))
                        .confidence(0.84)
                        .reason("队尾推进没有回绕。")
                        .build()))
                .build());

        assertThat(pack.getStandardLibraryNavigationSummary().getStatus()).isEqualTo("AI_NAVIGATION");
        assertThat(pack.getKnowledgeAnchors())
                .extracting(StandardLibraryPack.KnowledgeAnchorOption::getId)
                .containsExactly("DS.QUEUE.CIRCULAR.index_wrap");
        assertThat(pack.getSkillUnits())
                .extracting(StandardLibraryPack.SkillUnitOption::getId)
                .containsExactly("SK_QUEUE_CIRCULAR_INDEX");
        assertThat(pack.getMistakePoints())
                .extracting(StandardLibraryPack.MistakePointOption::getId)
                .containsExactly("MP_QUEUE_REAR_WITHOUT_MOD");
        assertThat(pack.getImprovementPoints())
                .extracting(StandardLibraryPack.ImprovementPointOption::getId)
                .containsExactly("IP_QUEUE_BOUNDARY_TRACE");
        assertThat(pack.getKnowledgeGroups()).singleElement().satisfies(group -> {
            assertThat(group.getId()).isEqualTo("DS.QUEUE.CIRCULAR.index_wrap");
            assertThat(group.getName()).isEqualTo("循环队列下标回绕");
            assertThat(group.getSkillUnits()).singleElement().satisfies(skillGroup -> {
                assertThat(skillGroup.getSkillUnit().getId()).isEqualTo("SK_QUEUE_CIRCULAR_INDEX");
                assertThat(skillGroup.getMistakePoints())
                        .extracting(StandardLibraryPack.MistakePointOption::getId)
                        .containsExactly("MP_QUEUE_REAR_WITHOUT_MOD");
                assertThat(skillGroup.getImprovementPoints())
                        .extracting(StandardLibraryPack.ImprovementPointOption::getId)
                        .containsExactly("IP_QUEUE_BOUNDARY_TRACE");
            });
        });
    }

    private AiStandardLibraryItem skill() {
        return AiStandardLibraryItem.builder()
                .layer(AiStandardLibraryLayer.SKILL_UNIT)
                .code("SK_QUEUE_CIRCULAR_INDEX")
                .category("队列与循环队列")
                .name("循环队列下标维护")
                .description("维护 fixed-size 数组里的 front/rear 下标。")
                .primaryKnowledgeNodeCode("DS.QUEUE.CIRCULAR.index_wrap")
                .knowledgeNodeCodes("DS.QUEUE.CIRCULAR.index_wrap")
                .build();
    }

    private AiStandardLibraryItem mistake() {
        return AiStandardLibraryItem.builder()
                .layer(AiStandardLibraryLayer.MISTAKE_POINT)
                .code("MP_QUEUE_REAR_WITHOUT_MOD")
                .category("循环队列")
                .name("队尾更新未取模")
                .description("rear 推进后没有回到数组范围内。")
                .skillUnitCode("SK_QUEUE_CIRCULAR_INDEX")
                .primaryKnowledgeNodeCode("DS.QUEUE.CIRCULAR.index_wrap")
                .knowledgeNodeCodes("DS.QUEUE.CIRCULAR.index_wrap")
                .mistakeType("INDEX_WRAP")
                .commonMisconception("只把 rear 当作递增计数，没有把数组容量作为循环边界。")
                .build();
    }

    private AiStandardLibraryItem improvement() {
        return AiStandardLibraryItem.builder()
                .layer(AiStandardLibraryLayer.IMPROVEMENT_POINT)
                .code("IP_QUEUE_BOUNDARY_TRACE")
                .category("边界追踪")
                .name("用小容量队列追踪回绕")
                .description("通过容量很小的队列观察 front/rear 多次推进后的状态。")
                .skillUnitCode("SK_QUEUE_CIRCULAR_INDEX")
                .abilityPoint("SK_QUEUE_CIRCULAR_INDEX")
                .primaryKnowledgeNodeCode("DS.QUEUE.CIRCULAR.index_wrap")
                .knowledgeNodeCodes("DS.QUEUE.CIRCULAR.index_wrap")
                .whenToUse("当循环队列错在下标推进时，用小容量样例验证回绕。")
                .studentBenefit("把抽象队列操作落到数组下标状态上。")
                .relatedItems("MP_QUEUE_REAR_WITHOUT_MOD")
                .build();
    }
}

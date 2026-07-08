package com.onlinejudge.learning.standardlibrary;

import com.onlinejudge.learning.knowledge.domain.InformaticsKnowledgeNode;
import com.onlinejudge.learning.knowledge.domain.InformaticsKnowledgeNodeType;
import com.onlinejudge.learning.knowledge.persistence.InformaticsKnowledgeNodeRepository;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryService;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardImprovementPoint;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardMistakePoint;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardSkillUnit;
import com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryDiagnosticLayerResponse;
import com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryNavigationExpansionResponse;
import com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryNavigationNodeResponse;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardImprovementPointRepository;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardMistakePointRepository;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardSkillUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai-standard-library-navigation-service;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "TEACHER_PASSWORD=test-teacher-password",
        "TEACHER_SESSION_SECRET=test-teacher-session-secret-1234567890",
        "STUDENT_TOKEN_SECRET=test-student-token-secret-1234567890",
        "AI_ENABLED=false"
})
class AiStandardLibraryNavigationServiceTest {

    @Autowired
    AiStandardLibraryService service;

    @Autowired
    InformaticsKnowledgeNodeRepository knowledgeRepository;

    @Autowired
    AiStandardSkillUnitRepository skillUnitRepository;

    @Autowired
    AiStandardMistakePointRepository mistakePointRepository;

    @Autowired
    AiStandardImprovementPointRepository improvementPointRepository;

    @BeforeEach
    void seedMinimalNavigationLibrary() {
        if (!knowledgeRepository.existsByCode("BASIC")) {
            knowledgeRepository.saveAllAndFlush(List.of(
                    knowledgeNode("BASIC", null, InformaticsKnowledgeNodeType.DOMAIN,
                            "基础语法", "基础语法", 1),
                    knowledgeNode("DS", null, InformaticsKnowledgeNodeType.DOMAIN,
                            "数据结构", "数据结构", 2),
                    knowledgeNode("ALGO", null, InformaticsKnowledgeNodeType.DOMAIN,
                            "算法思想", "算法思想", 3),
                    knowledgeNode("MATH", null, InformaticsKnowledgeNodeType.DOMAIN,
                            "数学基础", "数学基础", 4),
                    knowledgeNode("ENG", null, InformaticsKnowledgeNodeType.DOMAIN,
                            "工程实践", "工程实践", 5),
                    knowledgeNode("CONTEST", null, InformaticsKnowledgeNodeType.DOMAIN,
                            "竞赛综合", "竞赛综合", 6),
                    knowledgeNode("DS.FIXTURE", "DS", InformaticsKnowledgeNodeType.CHAPTER,
                            "数据结构测试子节点", "数据结构 / 数据结构测试子节点", 1),
                    knowledgeNode("ALGO.FIXTURE", "ALGO", InformaticsKnowledgeNodeType.CHAPTER,
                            "算法思想测试子节点", "算法思想 / 算法思想测试子节点", 1),
                    knowledgeNode("MATH.FIXTURE", "MATH", InformaticsKnowledgeNodeType.CHAPTER,
                            "数学基础测试子节点", "数学基础 / 数学基础测试子节点", 1),
                    knowledgeNode("ENG.FIXTURE", "ENG", InformaticsKnowledgeNodeType.CHAPTER,
                            "工程实践测试子节点", "工程实践 / 工程实践测试子节点", 1),
                    knowledgeNode("CONTEST.FIXTURE", "CONTEST", InformaticsKnowledgeNodeType.CHAPTER,
                            "竞赛综合测试子节点", "竞赛综合 / 竞赛综合测试子节点", 1),
                    knowledgeNode("BASIC.FIXTURE", "BASIC", InformaticsKnowledgeNodeType.CHAPTER,
                            "基础语法测试子节点", "基础语法 / 基础语法测试子节点", 2)
            ));
        }
        if (!knowledgeRepository.existsByCode("BASIC.IO.MULTI_CASE.显式_T_组循环")) {
            knowledgeRepository.saveAllAndFlush(List.of(
                    knowledgeNode("BASIC.IO", "BASIC", InformaticsKnowledgeNodeType.CHAPTER,
                            "输入输出", "基础语法 / 输入输出", 1),
                    knowledgeNode("BASIC.IO.MULTI_CASE", "BASIC.IO", InformaticsKnowledgeNodeType.TOPIC,
                            "多组数据", "基础语法 / 输入输出 / 多组数据", 1),
                    knowledgeNode("BASIC.IO.MULTI_CASE.显式_T_组循环", "BASIC.IO.MULTI_CASE",
                            InformaticsKnowledgeNodeType.KNOWLEDGE_POINT,
                            "显式 T 组循环", "基础语法 / 输入输出 / 多组数据 / 显式 T 组循环", 1)
            ));
        }
        String pointCode = "BASIC.IO.MULTI_CASE.显式_T_组循环";
        String skillCode = "SK_MULTI_CASE_LOOP_FIXTURE";
        if (skillUnitRepository.findByCode(skillCode).isEmpty()) {
            skillUnitRepository.saveAndFlush(AiStandardSkillUnit.builder()
                    .code(skillCode)
                    .category("输入输出")
                    .name("显式 T 组循环读取")
                    .description("能把题面 T 组输入映射到循环读取。")
                    .learningGoal("确认每组数据都被读取和处理。")
                    .primaryKnowledgeNodeCode(pointCode)
                    .knowledgeNodeCodes(pointCode)
                    .masteryLevel("MEDIUM")
                    .applicableLanguages("PYTHON\nCPP17")
                    .enabled(true)
                    .libraryVersion("test-fixture")
                    .build());
        }
        if (mistakePointRepository.findByCode("MP_MULTI_CASE_MISSING_LOOP_FIXTURE").isEmpty()) {
            mistakePointRepository.saveAndFlush(AiStandardMistakePoint.builder()
                    .code("MP_MULTI_CASE_MISSING_LOOP_FIXTURE")
                    .category("输入输出")
                    .name("漏处理多组数据")
                    .description("只按一组样例读取，没有循环处理 T 组。")
                    .skillUnitCode(skillCode)
                    .mistakeType("IO_FORMAT")
                    .misconception("把样例形态当作完整输入结构。")
                    .symptom("多组数据时只输出一组结果。")
                    .repairStrategy("先数清楚每组输入和输出。")
                    .severity("HIGH")
                    .primaryKnowledgeNodeCode(pointCode)
                    .knowledgeNodeCodes(pointCode)
                    .applicableLanguages("PYTHON\nCPP17")
                    .enabled(true)
                    .libraryVersion("test-fixture")
                    .build());
        }
        if (improvementPointRepository.findByCode("IP_MULTI_CASE_TRACE_FIXTURE").isEmpty()) {
            improvementPointRepository.saveAndFlush(AiStandardImprovementPoint.builder()
                    .code("IP_MULTI_CASE_TRACE_FIXTURE")
                    .category("自测")
                    .name("多组输入 trace")
                    .description("用两组最小样例检查读取循环。")
                    .skillUnitCode(skillCode)
                    .primaryKnowledgeNodeCode(pointCode)
                    .knowledgeNodeCodes(pointCode)
                    .improvementGoal("建立输入结构对照习惯。")
                    .practiceStrategy("手推两组输入的读取顺序。")
                    .studentBenefit("能更早发现只处理一组数据的问题。")
                    .enabled(true)
                    .libraryVersion("test-fixture")
                    .build());
        }
    }

    @Test
    void listsRootKnowledgeAreasForAiNavigation() {
        List<AiStandardLibraryNavigationNodeResponse> roots = service.listRootKnowledgeAreas();

        assertThat(roots)
                .extracting(AiStandardLibraryNavigationNodeResponse::getCode)
                .containsExactly("BASIC", "DS", "ALGO", "MATH", "ENG", "CONTEST");
        assertThat(roots)
                .allSatisfy(root -> {
                    assertThat(root.getParentCode()).isNull();
                    assertThat(root.isHasChildren()).isTrue();
                });
    }

    @Test
    void expandsOnlySelectedKnowledgeNodeChildren() {
        AiStandardLibraryNavigationExpansionResponse expansion =
                service.expandKnowledgeNode("BASIC.IO.MULTI_CASE");

        assertThat(expansion.getNode().getCode()).isEqualTo("BASIC.IO.MULTI_CASE");
        assertThat(expansion.getAncestors())
                .extracting(AiStandardLibraryNavigationNodeResponse::getCode)
                .containsExactly("BASIC", "BASIC.IO");
        assertThat(expansion.getChildren())
                .extracting(AiStandardLibraryNavigationNodeResponse::getCode)
                .contains("BASIC.IO.MULTI_CASE.显式_T_组循环");
        assertThat(expansion.getChildren())
                .extracting(AiStandardLibraryNavigationNodeResponse::getType)
                .containsOnly("KNOWLEDGE_POINT");
        assertThat(expansion.getChildPage()).isZero();
        assertThat(expansion.getChildTotal()).isGreaterThanOrEqualTo(expansion.getChildren().size());
    }

    @Test
    void expandsSelectedKnowledgeNodeChildrenWithBoundedPage() {
        AiStandardLibraryNavigationExpansionResponse firstPage = service.expandKnowledgeNode("BASIC", 0, 1);

        assertThat(firstPage.getChildren()).hasSize(1);
        assertThat(firstPage.getChildSize()).isEqualTo(1);
        assertThat(firstPage.getChildTotal()).isGreaterThan(1);
        assertThat(firstPage.isChildHasMore()).isTrue();
    }

    @Test
    void expandsDiagnosticLayerUnderKnowledgePointFromNormalizedLibrary() {
        AiStandardLibraryDiagnosticLayerResponse layer =
                service.expandDiagnosticLayer("BASIC.IO.MULTI_CASE.显式_T_组循环");

        assertThat(layer.getKnowledgePoint().getCode()).isEqualTo("BASIC.IO.MULTI_CASE.显式_T_组循环");
        assertThat(layer.getKnowledgePoint().getType()).isEqualTo("KNOWLEDGE_POINT");
        assertThat(layer.getSkillUnits()).isNotEmpty();
        assertThat(layer.getSkillUnits())
                .allSatisfy(skill -> {
                    assertThat(skill.getPrimaryKnowledgeNodeCode())
                            .isEqualTo("BASIC.IO.MULTI_CASE.显式_T_组循环");
                    assertThat(skill.getMistakePoints())
                            .allSatisfy(mistake -> assertThat(mistake.getSkillUnitCode()).isEqualTo(skill.getCode()));
                    assertThat(skill.getImprovementPoints())
                            .allSatisfy(improvement -> assertThat(improvement.getSkillUnitCode()).isEqualTo(skill.getCode()));
                });
        assertThat(layer.getSkillUnits())
                .anySatisfy(skill -> assertThat(skill.getMistakePoints()).isNotEmpty());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void h2FixtureExpandsFromRootToNormalizedAnchorLayer() {
        String chapterCode = "BASIC.AI_QUALITY_FIXTURE";
        String pointCode = "BASIC.AI_QUALITY_FIXTURE.LAZY_RANGE";
        String skillCode = "SK_FIXTURE_LAZY_RANGE";
        String mistakeCode = "MP_FIXTURE_LAZY_OVERWRITE";
        String improvementCode = "IP_FIXTURE_LAZY_TRACE";
        knowledgeRepository.saveAllAndFlush(List.of(
                knowledgeNode(chapterCode, "BASIC", InformaticsKnowledgeNodeType.CHAPTER,
                        "AI 质量测试章节", "基础语法 / AI 质量测试章节", true),
                knowledgeNode(pointCode, chapterCode, InformaticsKnowledgeNodeType.KNOWLEDGE_POINT,
                        "区间 lazy 累积测试点", "基础语法 / AI 质量测试章节 / 区间 lazy 累积测试点", false)
        ));
        skillUnitRepository.saveAndFlush(AiStandardSkillUnit.builder()
                .code(skillCode)
                .category("线段树")
                .name("区间 lazy 累积观察")
                .description("能观察连续区间加后的 lazy 和 max 变化。")
                .learningGoal("用两次区间更新手推 lazy 累积。")
                .primaryKnowledgeNodeCode(pointCode)
                .knowledgeNodeCodes(pointCode)
                .masteryLevel("MEDIUM")
                .applicableLanguages("CPP17")
                .enabled(true)
                .libraryVersion("test-fixture")
                .build());
        mistakePointRepository.saveAndFlush(AiStandardMistakePoint.builder()
                .code(mistakeCode)
                .category("线段树")
                .name("lazy 覆盖代替累积")
                .description("连续两次区间加后 lazy 没有累积。")
                .skillUnitCode(skillCode)
                .mistakeType("STATE_UPDATE")
                .misconception("把区间增量当作覆盖值保存。")
                .symptom("第二次更新后答案偏小。")
                .repairStrategy("先手推两次区间加后的节点标记。")
                .severity("HIGH")
                .primaryKnowledgeNodeCode(pointCode)
                .knowledgeNodeCodes(pointCode)
                .applicableLanguages("CPP17")
                .enabled(true)
                .libraryVersion("test-fixture")
                .build());
        improvementPointRepository.saveAndFlush(AiStandardImprovementPoint.builder()
                .code(improvementCode)
                .category("自测")
                .name("连续区间更新 trace")
                .description("连续两次区间加后检查 lazy 和 max。")
                .skillUnitCode(skillCode)
                .primaryKnowledgeNodeCode(pointCode)
                .knowledgeNodeCodes(pointCode)
                .improvementGoal("把 lazy 累积过程写成可复盘表格。")
                .practiceStrategy("先手推两次区间加，再对照节点标记。")
                .studentBenefit("能更早发现 lazy 覆盖与累积混淆。")
                .enabled(true)
                .libraryVersion("test-fixture")
                .build());

        assertThat(service.expandKnowledgeNode("BASIC", 0, 100).getChildren())
                .extracting(AiStandardLibraryNavigationNodeResponse::getCode)
                .contains(chapterCode);
        assertThat(service.expandKnowledgeNode(chapterCode).getChildren())
                .extracting(AiStandardLibraryNavigationNodeResponse::getCode)
                .containsExactly(pointCode);
        AiStandardLibraryDiagnosticLayerResponse layer = service.expandDiagnosticLayer(pointCode);

        assertThat(layer.getSkillUnits()).singleElement()
                .satisfies(skill -> {
                    assertThat(skill.getCode()).isEqualTo(skillCode);
                    assertThat(skill.getMistakePoints())
                            .extracting(AiStandardLibraryDiagnosticLayerResponse.MistakePoint::getCode)
                            .containsExactly(mistakeCode);
                    assertThat(skill.getImprovementPoints())
                            .extracting(AiStandardLibraryDiagnosticLayerResponse.ImprovementPoint::getCode)
                            .containsExactly(improvementCode);
                });
    }

    @Test
    void refusesDiagnosticLayerForNonKnowledgePoint() {
        assertThatThrownBy(() -> service.expandDiagnosticLayer("BASIC.IO.MULTI_CASE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("诊断层只能展开到知识点");
    }

    private InformaticsKnowledgeNode knowledgeNode(String code,
                                                   String parentCode,
                                                   InformaticsKnowledgeNodeType type,
                                                   String name,
                                                   String path,
                                                   boolean hasChildren) {
        return InformaticsKnowledgeNode.builder()
                .code(code)
                .parentCode(parentCode)
                .type(type)
                .name(name)
                .description(name)
                .path(path)
                .sortOrder(hasChildren ? -20 : -19)
                .enabled(true)
                .libraryVersion("test-fixture")
                .build();
    }

    private InformaticsKnowledgeNode knowledgeNode(String code,
                                                   String parentCode,
                                                   InformaticsKnowledgeNodeType type,
                                                   String name,
                                                   String path,
                                                   int sortOrder) {
        return InformaticsKnowledgeNode.builder()
                .code(code)
                .parentCode(parentCode)
                .type(type)
                .name(name)
                .description(name)
                .path(path)
                .sortOrder(sortOrder)
                .enabled(true)
                .libraryVersion("test-fixture")
                .build();
    }
}

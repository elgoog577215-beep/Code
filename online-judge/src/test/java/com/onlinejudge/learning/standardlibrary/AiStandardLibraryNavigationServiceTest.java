package com.onlinejudge.learning.standardlibrary;

import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryService;
import com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryDiagnosticLayerResponse;
import com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryNavigationExpansionResponse;
import com.onlinejudge.learning.standardlibrary.dto.AiStandardLibraryNavigationNodeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    void refusesDiagnosticLayerForNonKnowledgePoint() {
        assertThatThrownBy(() -> service.expandDiagnosticLayer("BASIC.IO.MULTI_CASE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("诊断层只能展开到知识点");
    }
}

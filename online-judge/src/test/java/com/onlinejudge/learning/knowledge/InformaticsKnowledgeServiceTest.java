package com.onlinejudge.learning.knowledge;

import com.onlinejudge.learning.knowledge.application.InformaticsKnowledgeService;
import com.onlinejudge.learning.knowledge.dto.InformaticsKnowledgeNodeDetailResponse;
import com.onlinejudge.learning.knowledge.dto.InformaticsKnowledgeNodeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:informatics-knowledge-service;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "TEACHER_PASSWORD=test-teacher-password",
        "TEACHER_SESSION_SECRET=test-teacher-session-secret-1234567890",
        "STUDENT_TOKEN_SECRET=test-student-token-secret-1234567890",
        "AI_ENABLED=false"
})
class InformaticsKnowledgeServiceTest {

    @Autowired
    InformaticsKnowledgeService service;

    @Test
    void returnsTreeAndKnowledgePointLinkedStandardLibraryItems() {
        List<InformaticsKnowledgeNodeResponse> tree = service.tree(false);

        assertThat(tree)
                .extracting(InformaticsKnowledgeNodeResponse::getCode)
                .contains("BASIC", "DS", "ALGO", "MATH", "ENG", "CONTEST");

        InformaticsKnowledgeNodeDetailResponse detail = service.detail("BASIC.IO.MULTI_CASE.显式_T_组循环");

        assertThat(detail.getAncestors())
                .extracting(InformaticsKnowledgeNodeResponse::getCode)
                .contains("BASIC", "BASIC.IO", "BASIC.IO.MULTI_CASE");
        assertThat(detail.getStandardLibraryItems())
                .extracting(item -> item.getCode())
                .contains("MULTI_CASE_INPUT");
    }
}

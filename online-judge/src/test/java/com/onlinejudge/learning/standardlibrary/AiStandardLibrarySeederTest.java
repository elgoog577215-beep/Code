package com.onlinejudge.learning.standardlibrary;

import com.onlinejudge.learning.standardlibrary.application.AiStandardLibrarySeeder;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardLibraryItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai-standard-library-seeder;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "TEACHER_PASSWORD=test-teacher-password",
        "TEACHER_SESSION_SECRET=test-teacher-session-secret-1234567890",
        "STUDENT_TOKEN_SECRET=test-student-token-secret-1234567890",
        "AI_ENABLED=false"
})
class AiStandardLibrarySeederTest {

    @Autowired
    AiStandardLibraryItemRepository repository;

    @Autowired
    AiStandardLibrarySeeder seeder;

    @Test
    void seedsFineGrainedBasicAndImprovementItemsIdempotently() {
        long initialCount = repository.count();

        assertThat(initialCount).isGreaterThanOrEqualTo(650);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.BASIC_CAUSE)
                .count()).isGreaterThanOrEqualTo(430);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.IMPROVEMENT_POINT)
                .count()).isGreaterThanOrEqualTo(220);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getKnowledgeNodeCodes() != null
                        && item.getKnowledgeNodeCodes().contains("BASIC.LOOP.BOUNDARY.左闭右开"))
                .count()).isGreaterThanOrEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getKnowledgeNodeCodes() != null
                        && item.getKnowledgeNodeCodes().contains("ALGO.DP.STATE.状态含义"))
                .count()).isGreaterThanOrEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getKnowledgeNodeCodes() != null
                        && item.getKnowledgeNodeCodes().contains("CONTEST.READING.CONSTRAINT.数据范围"))
                .count()).isGreaterThanOrEqualTo(1);

        seeder.run();

        assertThat(repository.count()).isEqualTo(initialCount);
    }
}

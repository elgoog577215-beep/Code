package com.onlinejudge.learning.knowledge;

import com.onlinejudge.learning.knowledge.application.InformaticsKnowledgeSeeder;
import com.onlinejudge.learning.knowledge.domain.InformaticsKnowledgeNodeType;
import com.onlinejudge.learning.knowledge.persistence.InformaticsKnowledgeNodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:informatics-knowledge-seeder;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "TEACHER_PASSWORD=test-teacher-password",
        "TEACHER_SESSION_SECRET=test-teacher-session-secret-1234567890",
        "STUDENT_TOKEN_SECRET=test-student-token-secret-1234567890",
        "AI_ENABLED=false"
})
class InformaticsKnowledgeSeederTest {

    @Autowired
    InformaticsKnowledgeNodeRepository repository;

    @Autowired
    InformaticsKnowledgeSeeder seeder;

    @Test
    void seedsBroadFineGrainedInformaticsKnowledgeTreeIdempotently() {
        long initialCount = repository.count();

        assertThat(initialCount).isGreaterThanOrEqualTo(120);
        assertThat(repository.findAll().stream()
                .filter(node -> node.getType() == InformaticsKnowledgeNodeType.DOMAIN)
                .count()).isGreaterThanOrEqualTo(6);
        assertThat(repository.findAll().stream()
                .filter(node -> node.getType() == InformaticsKnowledgeNodeType.KNOWLEDGE_POINT)
                .count()).isGreaterThanOrEqualTo(90);
        assertThat(repository.findByCode("BASIC.LOOP.BOUNDARY.左闭右开")).isPresent();
        assertThat(repository.findByCode("ALGO.DP.STATE.状态含义")).isPresent();
        assertThat(repository.findByCode("CONTEST.READING.CONSTRAINT.数据范围")).isPresent();

        seeder.run();

        assertThat(repository.count()).isEqualTo(initialCount);
    }
}

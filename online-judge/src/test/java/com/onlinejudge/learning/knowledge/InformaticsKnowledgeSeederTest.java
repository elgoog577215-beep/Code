package com.onlinejudge.learning.knowledge;

import com.onlinejudge.learning.knowledge.application.InformaticsKnowledgeSeeder;
import com.onlinejudge.learning.knowledge.domain.InformaticsKnowledgeNodeType;
import com.onlinejudge.learning.knowledge.persistence.InformaticsKnowledgeNodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

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

        assertThat(initialCount).isGreaterThanOrEqualTo(720);
        assertThat(repository.findAll().stream()
                .filter(node -> node.getType() == InformaticsKnowledgeNodeType.DOMAIN)
                .count()).isGreaterThanOrEqualTo(6);
        assertThat(repository.findAll().stream()
                .filter(node -> node.getType() == InformaticsKnowledgeNodeType.KNOWLEDGE_POINT)
                .count()).isGreaterThanOrEqualTo(575);
        assertThat(repository.findByCode("BASIC.LOOP.BOUNDARY.左闭右开")).isPresent();
        assertThat(repository.findByCode("ALGO.DP.STATE.状态含义")).isPresent();
        assertThat(repository.findByCode("CONTEST.READING.CONSTRAINT.数据范围")).isPresent();
        assertThat(repository.findByCode("BASIC.ARRAY.UPDATE.读旧写新分离")).isPresent();
        assertThat(repository.findByCode("BASIC.STRING.MATCH.未找到结果哨兵")).isPresent();
        assertThat(repository.findByCode("DS.GRAPH.STORE.多组图清空")).isPresent();
        assertThat(repository.findByCode("ALGO.SIM.CORNER.并列规则冲突")).isPresent();
        assertThat(repository.findByCode("ENG.COMPLEXITY.TRADEOFF.预处理收益判断")).isPresent();
        assertThat(repository.findByCode("CONTEST.SUBMIT.CHECKLIST.溢出风险复查")).isPresent();

        var updateNode = repository.findByCode("BASIC.ARRAY.UPDATE.读旧写新分离").orElseThrow();
        assertThat(updateNode.getParentCode()).isEqualTo("BASIC.ARRAY.UPDATE");
        assertThat(updateNode.getPath()).contains("数组更新", "读旧写新分离");
        assertThat(updateNode.getDescription()).contains("读旧写新分离").contains("旧值").contains("新值");
        assertThat(updateNode.getLearningObjectives()).contains("读题");
        assertThat(updateNode.getTypicalProblems()).contains("边界");

        seeder.run();

        assertThat(repository.count()).isEqualTo(initialCount);
    }

    @Test
    void knowledgeSeedContentAvoidsLowInformationTemplates() {
        List<String> forbiddenFragments = List.of(
                "细颗粒知识点",
                "相关的概念、方法和常见题型",
                "的基本用法。");
        List<String> errors = new ArrayList<>();

        repository.findAll().forEach(node -> {
            String text = String.join("\n",
                    nullToEmpty(node.getName()),
                    nullToEmpty(node.getDescription()),
                    nullToEmpty(node.getLearningObjectives()),
                    nullToEmpty(node.getTypicalProblems()));
            forbiddenFragments.stream()
                    .filter(fragment -> text.contains(fragment))
                    .forEach(fragment -> errors.add(node.getCode() + " contains low-information fragment: " + fragment));
        });

        assertThat(errors)
                .as("knowledge content quality errors: " + errors.stream().limit(20).toList())
                .isEmpty();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

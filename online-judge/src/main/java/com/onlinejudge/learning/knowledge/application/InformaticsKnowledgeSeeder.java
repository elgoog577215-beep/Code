package com.onlinejudge.learning.knowledge.application;

import com.onlinejudge.learning.knowledge.domain.InformaticsKnowledgeNode;
import com.onlinejudge.learning.knowledge.persistence.InformaticsKnowledgeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(4)
@RequiredArgsConstructor
@Slf4j
public class InformaticsKnowledgeSeeder implements CommandLineRunner {

    private final InformaticsKnowledgeNodeRepository repository;

    @Override
    @Transactional
    public void run(String... args) {
        int inserted = 0;
        for (InformaticsKnowledgeSeed seed : InformaticsKnowledgeSeedCatalog.seeds()) {
            if (repository.existsByCode(seed.code())) {
                continue;
            }
            repository.save(toEntity(seed));
            inserted++;
        }
        if (inserted > 0) {
            log.info("Seeded {} informatics knowledge nodes", inserted);
        }
    }

    private InformaticsKnowledgeNode toEntity(InformaticsKnowledgeSeed seed) {
        return InformaticsKnowledgeNode.builder()
                .code(seed.code())
                .parentCode(blankToNull(seed.parentCode()))
                .type(seed.type())
                .name(seed.name())
                .description(seed.description())
                .path(seed.path())
                .stage(seed.stage())
                .difficulty(seed.difficulty())
                .aliases(join(seed.aliases()))
                .prerequisites(join(seed.prerequisites()))
                .learningObjectives(join(seed.learningObjectives()))
                .typicalProblems(join(seed.typicalProblems()))
                .sortOrder(seed.sortOrder())
                .enabled(true)
                .libraryVersion(seed.libraryVersion())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join("\n", values);
    }
}

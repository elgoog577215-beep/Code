package com.onlinejudge.learning.standardlibrary.application;

import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardLibraryItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class AiStandardLibrarySeeder implements CommandLineRunner {

    private final AiStandardLibraryItemRepository repository;

    @Override
    @Transactional
    public void run(String... args) {
        int inserted = 0;
        for (AiStandardLibrarySeed seed : AiStandardLibrarySeedCatalog.seeds()) {
            if (repository.existsByLayerAndCode(seed.layer(), seed.code())) {
                continue;
            }
            repository.save(toEntity(seed));
            inserted++;
        }
        if (inserted > 0) {
            log.info("Seeded {} AI standard library items", inserted);
        }
    }

    private AiStandardLibraryItem toEntity(AiStandardLibrarySeed seed) {
        return AiStandardLibraryItem.builder()
                .layer(seed.layer())
                .code(seed.code())
                .category(seed.category())
                .name(seed.name())
                .description(seed.description())
                .studentExplanation(seed.studentExplanation())
                .teacherExplanation(seed.teacherExplanation())
                .evidenceSignals(join(seed.evidenceSignals()))
                .commonCodePatterns(join(seed.commonCodePatterns()))
                .judgeSignals(join(seed.judgeSignals()))
                .requiredEvidence(join(seed.requiredEvidence()))
                .whenToUse(seed.whenToUse())
                .studentBenefit(seed.studentBenefit())
                .hintL1(seed.hintL1())
                .hintL2(seed.hintL2())
                .hintL3(seed.hintL3())
                .abilityPoint(seed.abilityPoint())
                .severity(seed.severity())
                .applicableLanguages(join(seed.applicableLanguages()))
                .relatedItems(join(seed.relatedItems()))
                .knowledgeNodeCodes(join(seed.knowledgeNodeCodes()))
                .teachingAction(seed.teachingAction())
                .enabled(true)
                .libraryVersion(seed.libraryVersion())
                .build();
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join("\n", values);
    }
}

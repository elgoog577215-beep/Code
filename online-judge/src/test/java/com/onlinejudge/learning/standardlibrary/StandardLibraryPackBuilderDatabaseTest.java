package com.onlinejudge.learning.standardlibrary;

import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryService;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryNormalizedSeeder;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibrarySeeder;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibrarySeed;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibrarySeedCatalog;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardMistakePoint;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardSkillUnit;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardLibraryItemRepository;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardImprovementPointRepository;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardMistakePointRepository;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardSkillUnitRepository;
import com.onlinejudge.submission.application.ModelDiagnosisBrief;
import com.onlinejudge.submission.application.StandardLibraryPack;
import com.onlinejudge.submission.application.StandardLibraryPackBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai-standard-library-builder;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "TEACHER_PASSWORD=test-teacher-password",
        "TEACHER_SESSION_SECRET=test-teacher-session-secret-1234567890",
        "STUDENT_TOKEN_SECRET=test-student-token-secret-1234567890",
        "AI_ENABLED=false"
})
class StandardLibraryPackBuilderDatabaseTest {

    @Autowired
    StandardLibraryPackBuilder builder;

    @Autowired
    AiStandardLibraryItemRepository repository;

    @Autowired
    AiStandardLibraryService standardLibraryService;

    @Autowired
    AiStandardLibrarySeeder seeder;

    @Autowired
    AiStandardLibraryNormalizedSeeder normalizedSeeder;

    @Autowired
    AiStandardSkillUnitRepository skillUnitRepository;

    @Autowired
    AiStandardMistakePointRepository mistakePointRepository;

    @Autowired
    AiStandardImprovementPointRepository improvementPointRepository;

    @Test
    void packBuilderReadsNormalizedItemsBeforeLegacyFlatTable() {
        var mistake = mistakePointRepository.findByCode("IO_FORMAT").orElseThrow();
        mistake.setName("规范库输入输出易错点");
        mistake.setMisconception("先确认规范结构易错点是否被读取。");
        mistakePointRepository.saveAndFlush(mistake);

        StandardLibraryPack pack = builder.build(ModelDiagnosisBrief.builder()
                .allowedIssueTags(List.of("IO_FORMAT"))
                .build(), null);

        assertThat(pack.getBasicCauses())
                .filteredOn(cause -> "IO_FORMAT".equals(cause.getId()))
                .singleElement()
                .satisfies(cause -> {
                    assertThat(cause.getName()).isEqualTo("规范库输入输出易错点");
                    assertThat(cause.getStudentExplanation()).isEqualTo("先确认规范结构易错点是否被读取。");
                });
    }

    @Test
    void searchLocationItemsPreferNormalizedStructure() {
        var legacySkill = repository.findByLayerAndCode(AiStandardLibraryLayer.SKILL_UNIT,
                "SK_BINARY_ANSWER_CHECK").orElseThrow();
        legacySkill.setName("旧表二分答案能力");
        repository.saveAndFlush(legacySkill);

        var normalizedSkill = skillUnitRepository.findByCode("SK_BINARY_ANSWER_CHECK").orElseThrow();
        normalizedSkill.setName("规范结构二分答案能力");
        skillUnitRepository.saveAndFlush(normalizedSkill);

        assertThat(standardLibraryService.enabledSearchLocationItems())
                .filteredOn(item -> "SK_BINARY_ANSWER_CHECK".equals(item.getCode()))
                .singleElement()
                .satisfies(item -> assertThat(item.getName()).isEqualTo("规范结构二分答案能力"));
    }

    @Test
    void generatedFallbackItemsAreArchivedAndNotSeededAsRuntimeCandidates() {
        AiStandardLibrarySeed archivedSkill = firstArchivedGeneratedFallback(AiStandardLibraryLayer.SKILL_UNIT);
        AiStandardLibrarySeed archivedMistake = firstArchivedGeneratedFallback(AiStandardLibraryLayer.MISTAKE_POINT);

        assertThat(repository.findByLayerAndCode(archivedSkill.layer(), archivedSkill.code())).isEmpty();
        assertThat(repository.findByLayerAndCode(archivedMistake.layer(), archivedMistake.code())).isEmpty();
        assertThat(skillUnitRepository.findByCode(archivedSkill.code())).isEmpty();
        assertThat(mistakePointRepository.findByCode(archivedMistake.code())).isEmpty();

        Set<String> basicCauseIds = standardLibraryService.enabledBasicCauses().stream()
                .map(StandardLibraryPack.BasicCauseOption::getId)
                .collect(Collectors.toSet());
        Set<String> searchItemCodes = standardLibraryService.enabledSearchLocationItems().stream()
                .map(item -> item.getCode())
                .collect(Collectors.toSet());

        assertThat(basicCauseIds).doesNotContain(archivedMistake.code());
        assertThat(searchItemCodes).doesNotContain(archivedSkill.code(), archivedMistake.code());
        assertThat(searchItemCodes).contains("SK_BINARY_ANSWER_CHECK", "MP_BINARY_CHECK_EQUAL_CASE_REJECTED");
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void seedersDisableExistingGeneratedFallbackArchiveRecords() {
        AiStandardLibrarySeed archivedSkill = firstArchivedGeneratedFallback(AiStandardLibraryLayer.SKILL_UNIT);
        AiStandardLibrarySeed archivedMistake = firstArchivedGeneratedFallback(AiStandardLibraryLayer.MISTAKE_POINT);

        repository.saveAndFlush(toLegacyItem(archivedSkill));
        repository.saveAndFlush(toLegacyItem(archivedMistake));
        skillUnitRepository.saveAndFlush(toSkillUnit(archivedSkill));
        mistakePointRepository.saveAndFlush(toMistakePoint(archivedMistake));

        seeder.run();
        normalizedSeeder.run();

        assertThat(repository.findByLayerAndCode(archivedSkill.layer(), archivedSkill.code()))
                .get()
                .extracting(AiStandardLibraryItem::isEnabled)
                .isEqualTo(false);
        assertThat(repository.findByLayerAndCode(archivedMistake.layer(), archivedMistake.code()))
                .get()
                .extracting(AiStandardLibraryItem::isEnabled)
                .isEqualTo(false);
        assertThat(skillUnitRepository.findByCode(archivedSkill.code()))
                .get()
                .extracting(AiStandardSkillUnit::isEnabled)
                .isEqualTo(false);
        assertThat(mistakePointRepository.findByCode(archivedMistake.code()))
                .get()
                .extracting(AiStandardMistakePoint::isEnabled)
                .isEqualTo(false);
        assertThat(standardLibraryService.enabledBasicCauses())
                .extracting(StandardLibraryPack.BasicCauseOption::getId)
                .doesNotContain(archivedMistake.code());
        assertThat(standardLibraryService.enabledSearchLocationItems())
                .extracting(item -> item.getCode())
                .doesNotContain(archivedSkill.code(), archivedMistake.code());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void packBuilderFallsBackToLegacyFlatTableWhenNormalizedTablesAreEmpty() {
        improvementPointRepository.deleteAll();
        mistakePointRepository.deleteAll();
        skillUnitRepository.deleteAll();

        var item = repository.findByLayerAndCode(AiStandardLibraryLayer.MISTAKE_POINT, "IO_FORMAT").orElseThrow();
        item.setName("旧扁平表输入输出易错点");
        item.setCommonMisconception("规范表为空时允许回退旧表。");
        repository.saveAndFlush(item);

        StandardLibraryPack pack = builder.build(ModelDiagnosisBrief.builder()
                .allowedIssueTags(List.of("IO_FORMAT"))
                .build(), null);

        assertThat(pack.getBasicCauses())
                .filteredOn(cause -> "IO_FORMAT".equals(cause.getId()))
                .singleElement()
                .satisfies(cause -> {
                    assertThat(cause.getName()).isEqualTo("旧扁平表输入输出易错点");
                    assertThat(cause.getStudentExplanation()).isEqualTo("规范表为空时允许回退旧表。");
                });
    }

    @Test
    void directBuilderWithoutDatabaseServiceFallsBackToBuiltinLibrary() {
        StandardLibraryPack fallbackPack = new StandardLibraryPackBuilder(new com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy())
                .build(ModelDiagnosisBrief.builder()
                        .allowedIssueTags(List.of("IO_FORMAT"))
                        .build(), null);

        assertThat(fallbackPack.getBasicCauses())
                .filteredOn(cause -> "IO_FORMAT".equals(cause.getId()))
                .singleElement()
                .satisfies(cause -> assertThat(cause.getName()).isEqualTo("输入输出格式"));
    }

    private AiStandardLibrarySeed firstArchivedGeneratedFallback(AiStandardLibraryLayer layer) {
        return AiStandardLibrarySeedCatalog.archivedGeneratedFallbackSeeds().stream()
                .filter(seed -> seed.layer() == layer)
                .filter(AiStandardLibrarySeedCatalog::isGeneratedFallbackSeed)
                .findFirst()
                .orElseThrow();
    }

    private AiStandardLibraryItem toLegacyItem(AiStandardLibrarySeed seed) {
        return AiStandardLibraryItem.builder()
                .layer(seed.layer())
                .code(seed.code())
                .category(seed.category())
                .name(seed.name())
                .description(seed.description())
                .studentExplanation(seed.studentExplanation())
                .teacherExplanation(seed.teacherExplanation())
                .skillUnitCode(seed.skillUnitCode())
                .primaryKnowledgeNodeCode(seed.knowledgeNodeCodes().isEmpty() ? "" : seed.knowledgeNodeCodes().get(0))
                .mistakeType(seed.mistakeType())
                .commonMisconception(seed.commonMisconception())
                .evidenceSignals("")
                .commonCodePatterns("")
                .judgeSignals("")
                .requiredEvidence("")
                .whenToUse(seed.whenToUse())
                .studentBenefit(seed.studentBenefit())
                .hintL1(seed.hintL1())
                .hintL2(seed.hintL2())
                .hintL3(seed.hintL3())
                .abilityPoint(seed.abilityPoint())
                .severity(seed.severity())
                .applicableLanguages(String.join("\n", seed.applicableLanguages()))
                .relatedItems(String.join("\n", seed.relatedItems()))
                .knowledgeNodeCodes(String.join("\n", seed.knowledgeNodeCodes()))
                .relatedKnowledgeNodeCodes("")
                .prerequisiteKnowledgeCodes(String.join("\n", seed.prerequisiteKnowledgeCodes()))
                .teachingAction(seed.teachingAction())
                .enabled(true)
                .libraryVersion(seed.libraryVersion())
                .build();
    }

    private AiStandardSkillUnit toSkillUnit(AiStandardLibrarySeed seed) {
        return AiStandardSkillUnit.builder()
                .code(seed.code())
                .category(seed.category())
                .name(seed.name())
                .description(seed.description())
                .learningGoal(seed.studentExplanation())
                .primaryKnowledgeNodeCode(seed.knowledgeNodeCodes().isEmpty() ? "STANDARD_LIBRARY.UNMAPPED" : seed.knowledgeNodeCodes().get(0))
                .knowledgeNodeCodes(String.join("\n", seed.knowledgeNodeCodes()))
                .prerequisiteKnowledgeCodes(String.join("\n", seed.prerequisiteKnowledgeCodes()))
                .masteryLevel(seed.severity())
                .applicableLanguages(String.join("\n", seed.applicableLanguages()))
                .enabled(true)
                .libraryVersion(seed.libraryVersion())
                .build();
    }

    private AiStandardMistakePoint toMistakePoint(AiStandardLibrarySeed seed) {
        return AiStandardMistakePoint.builder()
                .code(seed.code())
                .category(seed.category())
                .name(seed.name())
                .description(seed.description())
                .skillUnitCode(seed.skillUnitCode())
                .mistakeType(seed.mistakeType())
                .misconception(seed.commonMisconception())
                .symptom(seed.description())
                .repairStrategy(seed.teacherExplanation())
                .severity(seed.severity())
                .primaryKnowledgeNodeCode(seed.knowledgeNodeCodes().isEmpty() ? "STANDARD_LIBRARY.UNMAPPED" : seed.knowledgeNodeCodes().get(0))
                .knowledgeNodeCodes(String.join("\n", seed.knowledgeNodeCodes()))
                .prerequisiteKnowledgeCodes(String.join("\n", seed.prerequisiteKnowledgeCodes()))
                .applicableLanguages(String.join("\n", seed.applicableLanguages()))
                .enabled(true)
                .libraryVersion(seed.libraryVersion())
                .build();
    }
}

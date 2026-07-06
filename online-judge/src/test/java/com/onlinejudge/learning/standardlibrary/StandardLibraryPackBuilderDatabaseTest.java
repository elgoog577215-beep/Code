package com.onlinejudge.learning.standardlibrary;

import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryService;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibrarySeed;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibrarySeedCatalog;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
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
    void generatedFallbackItemsRemainSeededButAreNotDefaultCandidates() {
        AiStandardLibrarySeed fallbackSkill = firstGeneratedFallback(AiStandardLibraryLayer.SKILL_UNIT);
        AiStandardLibrarySeed fallbackMistake = firstGeneratedFallback(AiStandardLibraryLayer.MISTAKE_POINT);

        assertThat(skillUnitRepository.findByCode(fallbackSkill.code())).isPresent();
        assertThat(mistakePointRepository.findByCode(fallbackMistake.code())).isPresent();

        Set<String> basicCauseIds = standardLibraryService.enabledBasicCauses().stream()
                .map(StandardLibraryPack.BasicCauseOption::getId)
                .collect(Collectors.toSet());
        Set<String> searchItemCodes = standardLibraryService.enabledSearchLocationItems().stream()
                .map(item -> item.getCode())
                .collect(Collectors.toSet());

        assertThat(basicCauseIds).doesNotContain(fallbackMistake.code());
        assertThat(searchItemCodes).doesNotContain(fallbackSkill.code(), fallbackMistake.code());
        assertThat(searchItemCodes).contains("SK_BINARY_ANSWER_CHECK", "MP_BINARY_CHECK_EQUAL_CASE_REJECTED");
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void generatedFallbackItemsCanStillBackfillWhenNoIntelligentItemsExist() {
        AiStandardLibrarySeed fallbackSkill = firstGeneratedFallback(AiStandardLibraryLayer.SKILL_UNIT);
        AiStandardLibrarySeed fallbackMistake = firstGeneratedFallback(AiStandardLibraryLayer.MISTAKE_POINT);

        var skills = skillUnitRepository.findAll();
        skills.forEach(item ->
                item.setEnabled(AiStandardLibrarySeedCatalog.isGeneratedFallbackCode(AiStandardLibraryLayer.SKILL_UNIT, item.getCode())));
        var mistakes = mistakePointRepository.findAll();
        mistakes.forEach(item ->
                item.setEnabled(AiStandardLibrarySeedCatalog.isGeneratedFallbackCode(AiStandardLibraryLayer.MISTAKE_POINT, item.getCode())));
        var improvements = improvementPointRepository.findAll();
        improvements.forEach(item -> item.setEnabled(false));
        skillUnitRepository.saveAllAndFlush(skills);
        mistakePointRepository.saveAllAndFlush(mistakes);
        improvementPointRepository.saveAllAndFlush(improvements);

        assertThat(standardLibraryService.enabledBasicCauses())
                .extracting(StandardLibraryPack.BasicCauseOption::getId)
                .contains(fallbackMistake.code());
        assertThat(standardLibraryService.enabledSearchLocationItems())
                .extracting(item -> item.getCode())
                .contains(fallbackSkill.code(), fallbackMistake.code());
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

    private AiStandardLibrarySeed firstGeneratedFallback(AiStandardLibraryLayer layer) {
        return AiStandardLibrarySeedCatalog.seeds().stream()
                .filter(seed -> seed.layer() == layer)
                .filter(AiStandardLibrarySeedCatalog::isGeneratedFallbackSeed)
                .findFirst()
                .orElseThrow();
    }
}

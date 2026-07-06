package com.onlinejudge.learning.standardlibrary.application;

import com.onlinejudge.learning.standardlibrary.domain.AiStandardImprovementPoint;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLegacyMapping;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryRelation;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryRelationType;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryTargetType;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardMistakePoint;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardSkillUnit;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardImprovementPointRepository;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardLibraryLegacyMappingRepository;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardLibraryRelationRepository;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardMistakePointRepository;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardSkillUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Component
@Order(6)
@RequiredArgsConstructor
@Slf4j
public class AiStandardLibraryNormalizedSeeder implements CommandLineRunner {

    private static final String UNMAPPED_KNOWLEDGE_NODE = "STANDARD_LIBRARY.UNMAPPED";

    private final AiStandardSkillUnitRepository skillUnitRepository;
    private final AiStandardMistakePointRepository mistakePointRepository;
    private final AiStandardImprovementPointRepository improvementPointRepository;
    private final AiStandardLibraryRelationRepository relationRepository;
    private final AiStandardLibraryLegacyMappingRepository legacyMappingRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<AiStandardLibrarySeed> seeds = AiStandardLibrarySeedCatalog.seeds();
        int insertedSkills = 0;
        int insertedMistakes = 0;
        int insertedImprovements = 0;
        int upsertedMappings = 0;

        for (AiStandardLibrarySeed seed : seeds) {
            if (seed.layer() == AiStandardLibraryLayer.SKILL_UNIT) {
                insertedSkills += syncSkill(seed);
                upsertedMappings += syncMapping(seed, AiStandardLibraryTargetType.SKILL_UNIT, normalizeCode(seed.code()));
            }
        }
        for (AiStandardLibrarySeed seed : seeds) {
            if (seed.layer() == AiStandardLibraryLayer.MISTAKE_POINT
                    || seed.layer() == AiStandardLibraryLayer.BASIC_CAUSE) {
                insertedMistakes += syncMistake(seed);
                upsertedMappings += syncMapping(seed, AiStandardLibraryTargetType.MISTAKE_POINT, normalizeCode(seed.code()));
            }
        }
        for (AiStandardLibrarySeed seed : seeds) {
            if (seed.layer() == AiStandardLibraryLayer.IMPROVEMENT_POINT) {
                insertedImprovements += syncImprovement(seed);
                upsertedMappings += syncMapping(seed, AiStandardLibraryTargetType.IMPROVEMENT_POINT, normalizeCode(seed.code()));
            }
        }

        if (insertedSkills + insertedMistakes + insertedImprovements + upsertedMappings > 0) {
            log.info("Seeded normalized AI standard library: skills={}, mistakes={}, improvements={}, mappings={}",
                    insertedSkills, insertedMistakes, insertedImprovements, upsertedMappings);
        }
    }

    private int syncSkill(AiStandardLibrarySeed seed) {
        String code = normalizeCode(seed.code());
        if (skillUnitRepository.existsByCode(code)) {
            return 0;
        }
        skillUnitRepository.save(AiStandardSkillUnit.builder()
                .code(code)
                .category(required(seed.category(), "能力点分类不能为空"))
                .name(required(seed.name(), "能力点名称不能为空"))
                .description(text(seed.description()))
                .learningGoal(text(seed.studentExplanation()))
                .primaryKnowledgeNodeCode(primaryKnowledgeNode(seed.knowledgeNodeCodes()))
                .knowledgeNodeCodes(join(seed.knowledgeNodeCodes()))
                .prerequisiteKnowledgeCodes(join(seed.prerequisiteKnowledgeCodes()))
                .masteryLevel(text(seed.severity()))
                .applicableLanguages(join(seed.applicableLanguages()))
                .enabled(true)
                .libraryVersion(version(seed))
                .build());
        syncPrerequisiteRelations(AiStandardLibraryTargetType.SKILL_UNIT, code, seed.prerequisiteKnowledgeCodes());
        return 1;
    }

    private int syncMistake(AiStandardLibrarySeed seed) {
        String code = normalizeCode(seed.code());
        if (mistakePointRepository.existsByCode(code)) {
            return 0;
        }
        String skillUnitCode = existingSkillOrCompat(seed);
        mistakePointRepository.save(AiStandardMistakePoint.builder()
                .code(code)
                .category(required(seed.category(), "易错点分类不能为空"))
                .name(required(seed.name(), "易错点名称不能为空"))
                .description(text(seed.description()))
                .skillUnitCode(skillUnitCode)
                .mistakeType(text(seed.mistakeType()))
                .misconception(text(seed.commonMisconception()))
                .symptom(text(seed.description()))
                .repairStrategy(text(seed.teacherExplanation()))
                .severity(text(seed.severity()))
                .primaryKnowledgeNodeCode(primaryKnowledgeNode(seed.knowledgeNodeCodes()))
                .knowledgeNodeCodes(join(seed.knowledgeNodeCodes()))
                .prerequisiteKnowledgeCodes(join(seed.prerequisiteKnowledgeCodes()))
                .applicableLanguages(join(seed.applicableLanguages()))
                .enabled(true)
                .libraryVersion(version(seed))
                .build());
        syncRelation(AiStandardLibraryTargetType.MISTAKE_POINT, code,
                AiStandardLibraryRelationType.EXTENDS, AiStandardLibraryTargetType.SKILL_UNIT, skillUnitCode,
                "易错点归属于能力点。");
        syncPrerequisiteRelations(AiStandardLibraryTargetType.MISTAKE_POINT, code, seed.prerequisiteKnowledgeCodes());
        return 1;
    }

    private int syncImprovement(AiStandardLibrarySeed seed) {
        String code = normalizeCode(seed.code());
        if (improvementPointRepository.existsByCode(code)) {
            return 0;
        }
        String skillUnitCode = normalizeCodeOrBlank(seed.skillUnitCode());
        improvementPointRepository.save(AiStandardImprovementPoint.builder()
                .code(code)
                .category(required(seed.category(), "提升点分类不能为空"))
                .name(required(seed.name(), "提升点名称不能为空"))
                .description(text(seed.description()))
                .skillUnitCode(skillUnitCode)
                .primaryKnowledgeNodeCode(primaryKnowledgeNode(seed.knowledgeNodeCodes()))
                .knowledgeNodeCodes(join(seed.knowledgeNodeCodes()))
                .improvementGoal(firstNonBlank(seed.whenToUse(), seed.description()))
                .practiceStrategy(text(seed.studentBenefit()))
                .studentBenefit(text(seed.studentBenefit()))
                .teacherExplanation(text(seed.teacherExplanation()))
                .relatedMistakeCodes(join(seed.relatedItems()))
                .applicableLanguages(join(seed.applicableLanguages()))
                .enabled(true)
                .libraryVersion(version(seed))
                .build());
        if (!skillUnitCode.isBlank()) {
            syncRelation(AiStandardLibraryTargetType.IMPROVEMENT_POINT, code,
                    AiStandardLibraryRelationType.EXTENDS, AiStandardLibraryTargetType.SKILL_UNIT, skillUnitCode,
                    "提升点归属于能力点。");
        }
        return 1;
    }

    private int syncMapping(AiStandardLibrarySeed seed,
                            AiStandardLibraryTargetType targetType,
                            String targetCode) {
        AiStandardLibraryLegacyMapping mapping = legacyMappingRepository
                .findByLegacyLayerAndLegacyCode(seed.layer(), normalizeCode(seed.code()))
                .orElseGet(AiStandardLibraryLegacyMapping::new);
        boolean isNew = mapping.getId() == null;
        mapping.setLegacyLayer(seed.layer());
        mapping.setLegacyCode(normalizeCode(seed.code()));
        mapping.setTargetType(targetType);
        mapping.setTargetCode(targetCode);
        mapping.setMigrationStatus("MAPPED");
        mapping.setConfidence(1.0);
        mapping.setSourceVersion(version(seed));
        legacyMappingRepository.save(mapping);
        return isNew ? 1 : 0;
    }

    private String existingSkillOrCompat(AiStandardLibrarySeed seed) {
        String skillUnitCode = normalizeCodeOrBlank(seed.skillUnitCode());
        if (!skillUnitCode.isBlank() && skillUnitRepository.existsByCode(skillUnitCode)) {
            return skillUnitCode;
        }
        String compatCode = "SK_COMPAT_" + normalizeCode(seed.code());
        if (!skillUnitRepository.existsByCode(compatCode)) {
            skillUnitRepository.save(AiStandardSkillUnit.builder()
                    .code(compatCode)
                    .category("兼容能力/" + required(seed.category(), "兼容能力分类不能为空"))
                    .name(required(seed.name(), "兼容能力名称不能为空") + "识别")
                    .description("能识别「" + seed.name() + "」相关的知识点和错误表现。")
                    .learningGoal("用于兼容旧 AI 标准库标签，后续逐步收敛到更细能力点。")
                    .primaryKnowledgeNodeCode(primaryKnowledgeNode(seed.knowledgeNodeCodes()))
                    .knowledgeNodeCodes(join(seed.knowledgeNodeCodes()))
                    .prerequisiteKnowledgeCodes(join(seed.prerequisiteKnowledgeCodes()))
                    .masteryLevel(text(seed.severity()))
                    .applicableLanguages(join(seed.applicableLanguages()))
                    .enabled(true)
                    .libraryVersion(version(seed))
                    .build());
        }
        return compatCode;
    }

    private void syncPrerequisiteRelations(AiStandardLibraryTargetType sourceType,
                                           String sourceCode,
                                           List<String> prerequisiteKnowledgeCodes) {
        if (prerequisiteKnowledgeCodes == null) {
            return;
        }
        prerequisiteKnowledgeCodes.stream()
                .map(this::text)
                .filter(value -> !value.isBlank())
                .forEach(targetCode -> syncRelation(sourceType, sourceCode,
                        AiStandardLibraryRelationType.PREREQUISITE,
                        AiStandardLibraryTargetType.KNOWLEDGE_NODE,
                        targetCode,
                        "该知识节点是标准库条目的前置知识。"));
    }

    private void syncRelation(AiStandardLibraryTargetType sourceType,
                              String sourceCode,
                              AiStandardLibraryRelationType relationType,
                              AiStandardLibraryTargetType targetType,
                              String targetCode,
                              String description) {
        String normalizedSourceCode = normalizeCode(sourceCode);
        String normalizedTargetCode = targetType == AiStandardLibraryTargetType.KNOWLEDGE_NODE
                ? text(targetCode)
                : normalizeCode(targetCode);
        relationRepository.findBySourceTypeAndSourceCodeAndRelationTypeAndTargetTypeAndTargetCode(
                        sourceType, normalizedSourceCode, relationType, targetType, normalizedTargetCode)
                .orElseGet(() -> relationRepository.save(AiStandardLibraryRelation.builder()
                        .sourceType(sourceType)
                        .sourceCode(normalizedSourceCode)
                        .relationType(relationType)
                        .targetType(targetType)
                        .targetCode(normalizedTargetCode)
                        .description(description)
                        .enabled(true)
                        .build()));
    }

    private String primaryKnowledgeNode(List<String> knowledgeNodeCodes) {
        if (knowledgeNodeCodes == null) {
            return UNMAPPED_KNOWLEDGE_NODE;
        }
        return knowledgeNodeCodes.stream()
                .map(this::text)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(UNMAPPED_KNOWLEDGE_NODE);
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join("\n", values.stream()
                .map(this::text)
                .filter(value -> !value.isBlank())
                .toList());
    }

    private String version(AiStandardLibrarySeed seed) {
        return firstNonBlank(seed.libraryVersion(), AiStandardLibrarySeedCatalog.VERSION);
    }

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = text(first);
        return normalizedFirst.isBlank() ? text(second) : normalizedFirst;
    }

    private String normalizeCode(String value) {
        return required(value, "标准库 code 不能为空").toUpperCase(Locale.ROOT);
    }

    private String normalizeCodeOrBlank(String value) {
        String normalized = text(value);
        return normalized.isBlank() ? "" : normalized.toUpperCase(Locale.ROOT);
    }

    private String required(String value, String message) {
        String normalized = text(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}

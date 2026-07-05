package com.onlinejudge.submission.application;

import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryService;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SearchLocationPackSelector {

    private final AiStandardLibraryService standardLibraryService;
    private final DiagnosisTaxonomy diagnosisTaxonomy;

    public StandardLibraryPack select(SearchLocationOutput output,
                                      SearchLocationCandidatePack candidatePack,
                                      StandardLibraryPack fallbackPack) {
        Map<String, AiStandardLibraryItem> byCode = new LinkedHashMap<>();
        standardLibraryService.enabledSearchLocationItems()
                .forEach(item -> byCode.put(item.getCode().toUpperCase(Locale.ROOT), item));

        LinkedHashSet<String> selectedIds = selectedIds(output);
        expandSelectedIds(selectedIds, candidatePack, byCode);
        List<AiStandardLibraryItem> selectedItems = selectedIds.stream()
                .map(id -> byCode.get(id.toUpperCase(Locale.ROOT)))
                .filter(item -> item != null)
                .toList();

        List<StandardLibraryPack.BasicCauseOption> basicCauses = selectedItems.stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT
                        || item.getLayer() == AiStandardLibraryLayer.BASIC_CAUSE)
                .map(this::toBasicCause)
                .toList();
        if (basicCauses.isEmpty() && fallbackPack != null) {
            basicCauses = safe(fallbackPack.getBasicCauses()).stream().limit(3).toList();
        }

        List<StandardLibraryPack.ImprovementPointOption> improvementPoints = selectedItems.stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.IMPROVEMENT_POINT)
                .map(this::toImprovementPoint)
                .toList();
        if (improvementPoints.isEmpty() && fallbackPack != null) {
            improvementPoints = safe(fallbackPack.getImprovementPoints()).stream().limit(3).toList();
        }

        List<StandardLibraryPack.SkillUnitOption> skillUnits = selectedItems.stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.SKILL_UNIT)
                .map(this::toSkillUnit)
                .toList();

        List<StandardLibraryPack.MistakePointOption> mistakePoints = selectedItems.stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .map(this::toMistakePoint)
                .toList();

        List<StandardLibraryPack.KnowledgeAnchorOption> anchors = selectedItems.stream()
                .flatMap(item -> lines(item.getKnowledgeNodeCodes()).stream())
                .distinct()
                .limit(12)
                .map(code -> StandardLibraryPack.KnowledgeAnchorOption.builder()
                        .id(code)
                        .name(code)
                        .path(code)
                        .description("搜索定位命中的知识树节点。")
                        .build())
                .toList();

        List<StandardLibraryPack.TagOption> issueTags = fallbackPack == null ? List.of() : safe(fallbackPack.getIssueTags());
        List<StandardLibraryPack.TagOption> fineTags = fallbackPack == null ? List.of() : safe(fallbackPack.getFineGrainedTags());
        List<StandardLibraryPack.ImprovementTagOption> improvementTags = fallbackPack == null
                ? List.of()
                : safe(fallbackPack.getImprovementTags());
        List<StandardLibraryPack.TeachingActionOption> actions = fallbackPack == null
                ? List.of()
                : safe(fallbackPack.getTeachingActions());

        return StandardLibraryPack.builder()
                .schemaVersion(StandardLibraryPack.SCHEMA_VERSION)
                .taxonomyVersion(DiagnosisTaxonomy.TAXONOMY_VERSION)
                .structureVersion(StandardLibraryPack.STRUCTURE_VERSION)
                .knowledgeGroups(buildKnowledgeGroups(selectedItems))
                .basicCauses(basicCauses)
                .improvementPoints(improvementPoints)
                .knowledgeAnchors(anchors)
                .skillUnits(skillUnits)
                .mistakePoints(mistakePoints)
                .searchLocationSummary(StandardLibraryPack.SearchLocationSummary.builder()
                        .status("SUCCESS")
                        .embeddingStatus(candidatePack == null ? "UNKNOWN" : candidatePack.getEmbeddingStatus())
                        .fallbackReason("")
                        .candidateCount(candidatePack == null ? 0 : candidatePack.getCandidateCount())
                        .selectedCount(selectedIds.size())
                        .uncertainty(output == null ? "" : output.getUncertainty())
                        .build())
                .issueTags(issueTags)
                .fineGrainedTags(fineTags)
                .improvementTags(improvementTags)
                .teachingActions(actions)
                .build();
    }

    private LinkedHashSet<String> selectedIds(SearchLocationOutput output) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (output == null) {
            return ids;
        }
        collect(ids, output.getBasicCandidates());
        collect(ids, output.getImprovementCandidates());
        collect(ids, output.getKnowledgeAnchors());
        return ids;
    }

    private void collect(Set<String> ids, List<SearchLocationOutput.SelectedCandidate> selected) {
        if (selected == null) {
            return;
        }
        for (SearchLocationOutput.SelectedCandidate candidate : selected) {
            if (candidate == null) {
                continue;
            }
            firstPresent(candidate.getId(), candidate.getMistakePointId(), candidate.getSkillUnitId(), candidate.getKnowledgeNodeId())
                    .stream()
                    .findFirst()
                    .ifPresent(ids::add);
        }
    }

    private void expandSelectedIds(LinkedHashSet<String> selectedIds,
                                   SearchLocationCandidatePack candidatePack,
                                   Map<String, AiStandardLibraryItem> byCode) {
        if (selectedIds.isEmpty() || candidatePack == null || candidatePack.getCandidates() == null) {
            return;
        }
        Map<String, SearchLocationCandidate> candidatesById = new LinkedHashMap<>();
        for (SearchLocationCandidate candidate : candidatePack.getCandidates()) {
            if (candidate != null && candidate.getId() != null) {
                candidatesById.put(candidate.getId().toUpperCase(Locale.ROOT), candidate);
            }
        }
        LinkedHashSet<String> additions = new LinkedHashSet<>();
        for (String selectedId : selectedIds) {
            SearchLocationCandidate candidate = candidatesById.get(selectedId.toUpperCase(Locale.ROOT));
            if (candidate == null) {
                continue;
            }
            addIfPresent(additions, candidate.getParentSkillUnitId());
            addIfPresent(additions, candidate.getSkillUnitCode());
            addAll(additions, candidate.getChildMistakePointIds());
            addAll(additions, candidate.getSiblingMistakePointIds());
            addAll(additions, candidate.getRelatedImprovementPointIds());
            addAll(additions, candidate.getExtensionCandidateIds());
        }
        additions.stream()
                .filter(id -> byCode.containsKey(id.toUpperCase(Locale.ROOT)))
                .limit(24)
                .forEach(selectedIds::add);
    }

    private void addIfPresent(Set<String> ids, String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.isBlank()) {
            ids.add(normalized);
        }
    }

    private void addAll(Set<String> ids, List<String> values) {
        if (values == null) {
            return;
        }
        values.forEach(value -> addIfPresent(ids, value));
    }

    private List<String> firstPresent(String... values) {
        for (String value : values) {
            String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                return List.of(normalized);
            }
        }
        return List.of();
    }

    private StandardLibraryPack.BasicCauseOption toBasicCause(AiStandardLibraryItem item) {
        return StandardLibraryPack.BasicCauseOption.builder()
                .id(item.getCode())
                .category(item.getCategory())
                .name(item.getName())
                .description(item.getDescription())
                .studentExplanation(item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT
                        ? item.getCommonMisconception()
                        : item.getStudentExplanation())
                .teacherExplanation(item.getTeacherExplanation())
                .evidenceSignals(lines(item.getEvidenceSignals()))
                .commonCodePatterns(lines(item.getCommonCodePatterns()))
                .judgeSignals(lines(item.getJudgeSignals()))
                .hintL1(item.getHintL1())
                .hintL2(item.getHintL2())
                .hintL3(item.getHintL3())
                .abilityPoint(firstNonBlank(item.getSkillUnitCode(), item.getAbilityPoint()))
                .severity(item.getSeverity())
                .applicableLanguages(lines(item.getApplicableLanguages()))
                .relatedFineTags(lines(item.getKnowledgeNodeCodes()))
                .teachingAction(item.getTeachingAction())
                .build();
    }

    private StandardLibraryPack.ImprovementPointOption toImprovementPoint(AiStandardLibraryItem item) {
        return StandardLibraryPack.ImprovementPointOption.builder()
                .id(item.getCode())
                .category(item.getCategory())
                .name(item.getName())
                .description(item.getDescription())
                .whenToUse(item.getWhenToUse())
                .studentBenefit(item.getStudentBenefit())
                .teacherExplanation(item.getTeacherExplanation())
                .requiredEvidence(lines(item.getRequiredEvidence()))
                .hintL1(item.getHintL1())
                .hintL2(item.getHintL2())
                .hintL3(item.getHintL3())
                .abilityPoint(firstNonBlank(item.getSkillUnitCode(), item.getAbilityPoint()))
                .relatedBasicCauses(lines(item.getRelatedItems()))
                .build();
    }

    private StandardLibraryPack.SkillUnitOption toSkillUnit(AiStandardLibraryItem item) {
        return StandardLibraryPack.SkillUnitOption.builder()
                .id(item.getCode())
                .category(item.getCategory())
                .name(item.getName())
                .description(item.getDescription())
                .knowledgeNodeCodes(lines(item.getKnowledgeNodeCodes()))
                .applicableLanguages(lines(item.getApplicableLanguages()))
                .build();
    }

    private StandardLibraryPack.MistakePointOption toMistakePoint(AiStandardLibraryItem item) {
        return StandardLibraryPack.MistakePointOption.builder()
                .id(item.getCode())
                .category(item.getCategory())
                .name(item.getName())
                .description(item.getDescription())
                .skillUnitCode(item.getSkillUnitCode())
                .mistakeType(item.getMistakeType())
                .commonMisconception(item.getCommonMisconception())
                .knowledgeNodeCodes(lines(item.getKnowledgeNodeCodes()))
                .applicableLanguages(lines(item.getApplicableLanguages()))
                .build();
    }

    private List<StandardLibraryPack.KnowledgeGroupOption> buildKnowledgeGroups(List<AiStandardLibraryItem> selectedItems) {
        if (selectedItems == null || selectedItems.isEmpty()) {
            return List.of();
        }
        List<StandardLibraryPack.SkillUnitOption> skillUnits = selectedItems.stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.SKILL_UNIT)
                .map(this::toSkillUnit)
                .toList();
        List<StandardLibraryPack.MistakePointOption> mistakes = selectedItems.stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT
                        || item.getLayer() == AiStandardLibraryLayer.BASIC_CAUSE)
                .map(this::toMistakePoint)
                .toList();
        List<StandardLibraryPack.ImprovementPointOption> improvements = selectedItems.stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.IMPROVEMENT_POINT)
                .map(this::toImprovementPoint)
                .toList();

        LinkedHashMap<String, List<AiStandardLibraryItem>> itemsByKnowledge = new LinkedHashMap<>();
        for (AiStandardLibraryItem item : selectedItems) {
            List<String> knowledgeCodes = lines(item.getKnowledgeNodeCodes());
            String key = knowledgeCodes.isEmpty() ? firstNonBlank(item.getCategory(), "UNMAPPED") : knowledgeCodes.get(0);
            itemsByKnowledge.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(item);
        }

        return itemsByKnowledge.entrySet().stream()
                .limit(10)
                .map(entry -> toKnowledgeGroup(entry.getKey(), entry.getValue(), skillUnits, mistakes, improvements))
                .toList();
    }

    private StandardLibraryPack.KnowledgeGroupOption toKnowledgeGroup(
            String knowledgeCode,
            List<AiStandardLibraryItem> items,
            List<StandardLibraryPack.SkillUnitOption> allSkills,
            List<StandardLibraryPack.MistakePointOption> allMistakes,
            List<StandardLibraryPack.ImprovementPointOption> allImprovements) {
        LinkedHashSet<String> skillIds = new LinkedHashSet<>();
        for (AiStandardLibraryItem item : items) {
            if (item.getLayer() == AiStandardLibraryLayer.SKILL_UNIT) {
                skillIds.add(item.getCode());
            }
            if (!text(item.getSkillUnitCode()).isBlank()) {
                skillIds.add(item.getSkillUnitCode());
            }
        }

        List<StandardLibraryPack.SkillUnitGroupOption> skillGroups = skillIds.stream()
                .map(skillId -> toSkillGroup(skillId, allSkills, allMistakes, allImprovements))
                .filter(group -> group.getSkillUnit() != null
                        || !safe(group.getMistakePoints()).isEmpty()
                        || !safe(group.getImprovementPoints()).isEmpty())
                .limit(8)
                .toList();

        return StandardLibraryPack.KnowledgeGroupOption.builder()
                .id(knowledgeCode)
                .name(knowledgeCode)
                .path(knowledgeCode.replace(".", " > "))
                .description("标准库结构视图中的知识节点。")
                .skillUnits(skillGroups)
                .improvementPoints(List.of())
                .build();
    }

    private StandardLibraryPack.SkillUnitGroupOption toSkillGroup(
            String skillId,
            List<StandardLibraryPack.SkillUnitOption> allSkills,
            List<StandardLibraryPack.MistakePointOption> allMistakes,
            List<StandardLibraryPack.ImprovementPointOption> allImprovements) {
        StandardLibraryPack.SkillUnitOption skill = allSkills.stream()
                .filter(item -> skillId.equals(item.getId()))
                .findFirst()
                .orElse(null);
        List<StandardLibraryPack.MistakePointOption> mistakes = allMistakes.stream()
                .filter(item -> skillId.equals(item.getSkillUnitCode()))
                .limit(8)
                .toList();
        List<StandardLibraryPack.ImprovementPointOption> improvements = allImprovements.stream()
                .filter(item -> skillId.equals(item.getAbilityPoint()))
                .limit(5)
                .toList();
        LinkedHashSet<String> candidateIds = new LinkedHashSet<>();
        if (skill != null) {
            candidateIds.add(skill.getId());
        }
        mistakes.stream().map(StandardLibraryPack.MistakePointOption::getId).forEach(candidateIds::add);
        improvements.stream().map(StandardLibraryPack.ImprovementPointOption::getId).forEach(candidateIds::add);
        return StandardLibraryPack.SkillUnitGroupOption.builder()
                .skillUnit(skill)
                .mistakePoints(mistakes)
                .improvementPoints(improvements)
                .candidateIds(candidateIds.stream().toList())
                .build();
    }

    private <T> List<T> safe(List<T> source) {
        return source == null ? List.of() : source;
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private List<String> lines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}

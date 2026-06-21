package com.onlinejudge.submission.application;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class AdviceGenerationOutputNormalizer {

    public AdviceGenerationOutput normalize(AdviceGenerationOutput output, StandardLibraryPack standardLibraryPack) {
        if (output == null) {
            return null;
        }
        Set<String> mistakeIds = new LinkedHashSet<>();
        mistakeIds.addAll(ids(standardLibraryPack == null ? null : standardLibraryPack.getMistakePoints()));
        mistakeIds.addAll(ids(standardLibraryPack == null ? null : standardLibraryPack.getBasicCauses()));
        Set<String> skillIds = ids(standardLibraryPack == null ? null : standardLibraryPack.getSkillUnits());
        Set<String> improvementIds = ids(standardLibraryPack == null ? null : standardLibraryPack.getImprovementPoints());
        if (output.getBasicLayerAdvice() != null) {
            output.getBasicLayerAdvice().forEach(item -> normalizeBasicAdvice(item, mistakeIds, skillIds));
        }
        if (output.getImprovementLayerAdvice() != null) {
            output.getImprovementLayerAdvice().forEach(item -> normalizeImprovementAdvice(item, improvementIds, skillIds));
        }
        return output;
    }

    private void normalizeBasicAdvice(AdviceGenerationOutput.BasicLayerAdvice item,
                                      Set<String> mistakeIds,
                                      Set<String> skillIds) {
        if (item == null) {
            return;
        }
        String mistake = normalizeKey(item.getMistakePointId());
        String skill = normalizeKey(item.getSkillUnitId());
        if (!skill.isBlank() && !skillIds.contains(skill)) {
            if ((mistake.isBlank() || !mistakeIds.contains(mistake)) && mistakeIds.contains(skill)) {
                item.setMistakePointId(item.getSkillUnitId());
            }
            item.setSkillUnitId(null);
        }
        mistake = normalizeKey(item.getMistakePointId());
        if (!mistake.isBlank() && !mistakeIds.contains(mistake)) {
            item.setMistakePointId(null);
        }
    }

    private void normalizeImprovementAdvice(AdviceGenerationOutput.ImprovementLayerAdvice item,
                                            Set<String> improvementIds,
                                            Set<String> skillIds) {
        if (item == null) {
            return;
        }
        String improvement = normalizeKey(item.getImprovementPointId());
        String skill = normalizeKey(item.getSkillUnitId());
        if (!skill.isBlank() && !skillIds.contains(skill)) {
            item.setSkillUnitId(null);
        }
        if (!improvement.isBlank() && !improvementIds.contains(improvement)) {
            item.setImprovementPointId(null);
        }
    }

    private Set<String> ids(List<?> values) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (values == null) {
            return ids;
        }
        for (Object value : values) {
            if (value instanceof StandardLibraryPack.MistakePointOption option) {
                add(ids, option.getId());
            } else if (value instanceof StandardLibraryPack.BasicCauseOption option) {
                add(ids, option.getId());
            } else if (value instanceof StandardLibraryPack.SkillUnitOption option) {
                add(ids, option.getId());
            } else if (value instanceof StandardLibraryPack.ImprovementPointOption option) {
                add(ids, option.getId());
            }
        }
        return ids;
    }

    private void add(Set<String> ids, String value) {
        String normalized = normalizeKey(value);
        if (!normalized.isBlank()) {
            ids.add(normalized);
        }
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

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
        normalizeStudentReport(output.getStudentReport());
        return output;
    }

    private void normalizeStudentReport(AdviceGenerationOutput.StudentReport report) {
        if (report == null || !containsDpLeak(
                report.getBasicLayerText(),
                report.getImprovementLayerText(),
                report.getNextActionText())) {
            return;
        }
        report.setBasicLayerText("基础层：这次更像是动态规划的状态含义没有先定清楚。先用一句话写出每个状态表示什么，再检查它依赖的信息是否已经算好。");
        report.setImprovementLayerText("提高层：修正后，先养成“先定义状态、再核对依赖信息、最后用最小样例检查”的习惯，不要急着套公式。");
        report.setNextActionText("拿可见失败样例手推一遍，记录每个状态的含义和它依赖的信息。");
    }

    private boolean containsDpLeak(String... values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            String text = value == null ? "" : value.toLowerCase(Locale.ROOT);
            String compact = text.replaceAll("\\s+", "");
            if (compact.contains("dp[i")
                    || compact.contains("skip_current")
                    || compact.contains("take_current")
                    || compact.contains("前驱状态")
                    || compact.contains("两个状态")
                    || compact.contains("两状态")
                    || compact.contains("多一个维度")
                    || compact.contains("空间优化")
                    || compact.contains("空间压缩")) {
                return true;
            }
        }
        return false;
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

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
        normalizeNextStepPlan(output);
        normalizeStudentReport(output.getStudentReport());
        return output;
    }

    private void normalizeNextStepPlan(AdviceGenerationOutput output) {
        if (output == null || output.getNextStepPlan() != null && !output.getNextStepPlan().isEmpty()) {
            return;
        }
        AdviceGenerationOutput.NextStepAdvice nextStep = nextStepFromBasicAdvice(output.getBasicLayerAdvice());
        if (nextStep == null) {
            nextStep = nextStepFromImprovementAdvice(output.getImprovementLayerAdvice());
        }
        if (nextStep != null) {
            output.setNextStepPlan(List.of(nextStep));
        }
    }

    private AdviceGenerationOutput.NextStepAdvice nextStepFromBasicAdvice(
            List<AdviceGenerationOutput.BasicLayerAdvice> values) {
        if (values == null) {
            return null;
        }
        for (AdviceGenerationOutput.BasicLayerAdvice item : values) {
            if (item == null) {
                continue;
            }
            String target = firstNonBlank(item.getStudentAction(), item.getText(), item.getTitle());
            if (target.isBlank()) {
                continue;
            }
            return AdviceGenerationOutput.NextStepAdvice.builder()
                    .step(1)
                    .target(target)
                    .reason("这是当前最直接的排查入口。")
                    .evidenceRef(firstEvidenceRef(item.getEvidenceRefs()))
                    .build();
        }
        return null;
    }

    private AdviceGenerationOutput.NextStepAdvice nextStepFromImprovementAdvice(
            List<AdviceGenerationOutput.ImprovementLayerAdvice> values) {
        if (values == null) {
            return null;
        }
        for (AdviceGenerationOutput.ImprovementLayerAdvice item : values) {
            if (item == null) {
                continue;
            }
            String target = firstNonBlank(item.getSuggestion(), item.getText(), item.getTitle());
            if (target.isBlank()) {
                continue;
            }
            return AdviceGenerationOutput.NextStepAdvice.builder()
                    .step(1)
                    .target(target)
                    .reason("这是当前最容易验证的改进动作。")
                    .evidenceRef(firstEvidenceRef(item.getEvidenceRefs()))
                    .build();
        }
        return null;
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
        String text = clean(item.getText());
        if (!text.isBlank()) {
            if (clean(item.getTitle()).isBlank()) {
                item.setTitle(titleFromText(text));
            }
            if (clean(item.getWhatHappened()).isBlank()) {
                item.setWhatHappened(text);
            }
            if (clean(item.getWhyItMatters()).isBlank()) {
                item.setWhyItMatters("这个问题会影响当前提交的正确性。");
            }
            if (clean(item.getStudentAction()).isBlank()) {
                item.setStudentAction(text);
            }
            if (clean(item.getCheckQuestion()).isBlank()) {
                item.setCheckQuestion("这个现象能否用当前失败样例复现？");
            }
            if (item.getConfidence() == null) {
                item.setConfidence(0.7);
            }
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
        String text = clean(item.getText());
        if (!text.isBlank()) {
            if (clean(item.getTitle()).isBlank()) {
                item.setTitle(titleFromText(text));
            }
            if (clean(item.getCurrentLimit()).isBlank()) {
                item.setCurrentLimit("当前需要补充复盘和自测。");
            }
            if (clean(item.getSuggestion()).isBlank()) {
                item.setSuggestion(text);
            }
            if (clean(item.getStudentBenefit()).isBlank()) {
                item.setStudentBenefit("能帮助你更早发现同类问题。");
            }
            if (item.getConfidence() == null) {
                item.setConfidence(0.7);
            }
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

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String cleaned = clean(value);
            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }
        return "";
    }

    private String firstEvidenceRef(List<String> evidenceRefs) {
        if (evidenceRefs == null) {
            return null;
        }
        for (String evidenceRef : evidenceRefs) {
            String cleaned = clean(evidenceRef);
            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }
        return null;
    }

    private String titleFromText(String text) {
        String normalized = clean(text).replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return "";
        }
        String[] parts = normalized.split("[。；;，,：:]", 2);
        String title = parts.length == 0 ? normalized : parts[0].trim();
        if (title.isBlank()) {
            title = normalized;
        }
        return title.length() <= 28 ? title : title.substring(0, 28);
    }
}

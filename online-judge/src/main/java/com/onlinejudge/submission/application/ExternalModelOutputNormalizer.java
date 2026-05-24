package com.onlinejudge.submission.application;

import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ExternalModelOutputNormalizer {

    public ExternalModelStagePayloads.DiagnosisJudgeOutput normalizeDiagnosisDecision(
            ExternalModelStagePayloads.DiagnosisJudgeOutput output,
            ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        if (output == null) {
            return null;
        }
        StandardLibraryPack standardLibraryPack = runtimePlan == null ? null : runtimePlan.getStandardLibraryPack();
        ModelDiagnosisBrief brief = runtimePlan == null ? null : runtimePlan.getBrief();
        output.setPrimaryIssueTag(resolveTag(output.getPrimaryIssueTag(),
                standardLibraryPack == null ? null : standardLibraryPack.getIssueTags()));
        output.setFineGrainedTag(resolveTag(output.getFineGrainedTag(),
                standardLibraryPack == null ? null : standardLibraryPack.getFineGrainedTags()));
        output.setEvidenceRefs(normalizeEvidenceRefs(output.getEvidenceRefs(), brief));
        output.setAnswerLeakRisk(normalizeRisk(output.getAnswerLeakRisk()));
        return output;
    }

    public ExternalModelStagePayloads.TeachingHintOutput normalizeTeachingHint(
            ExternalModelStagePayloads.TeachingHintOutput output,
            ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        if (output == null) {
            return null;
        }
        StandardLibraryPack standardLibraryPack = runtimePlan == null ? null : runtimePlan.getStandardLibraryPack();
        ModelDiagnosisBrief brief = runtimePlan == null ? null : runtimePlan.getBrief();
        Set<String> allowedActions = actionLookup(standardLibraryPack == null ? null : standardLibraryPack.getTeachingActions());
        if (output.getStudentHintPlan() != null) {
            SubmissionAnalysisResponse.StudentHintPlan plan = output.getStudentHintPlan();
            plan.setTeachingAction(resolveAction(plan.getTeachingAction(), allowedActions));
            plan.setEvidenceRefs(normalizeEvidenceRefs(plan.getEvidenceRefs(), brief));
            plan.setAnswerLeakRisk(normalizeRisk(plan.getAnswerLeakRisk()));
        }
        if (output.getLearningInterventionPlan() != null) {
            SubmissionAnalysisResponse.LearningInterventionPlan plan = output.getLearningInterventionPlan();
            plan.setEvidenceRefs(normalizeEvidenceRefs(plan.getEvidenceRefs(), brief));
            plan.setAnswerLeakRisk(normalizeRisk(plan.getAnswerLeakRisk()));
        }
        output.setAnswerLeakRisk(normalizeRisk(output.getAnswerLeakRisk()));
        return output;
    }

    public ExternalModelStagePayloads.CombinedOutput normalizeCombinedOutput(
            ExternalModelStagePayloads.CombinedOutput output,
            ExternalModelAgentRuntime.RuntimePlan runtimePlan) {
        if (output == null) {
            return null;
        }
        output.setDiagnosisDecision(normalizeDiagnosisDecision(output.getDiagnosisDecision(), runtimePlan));
        output.setTeachingHint(normalizeTeachingHint(output.getTeachingHint(), runtimePlan));
        return output;
    }

    private String resolveTag(String rawValue, List<StandardLibraryPack.TagOption> options) {
        if (rawValue == null || rawValue.isBlank()) {
            return rawValue;
        }
        Map<String, String> lookup = new LinkedHashMap<>();
        if (options != null) {
            for (StandardLibraryPack.TagOption option : options) {
                if (option == null || option.getId() == null || option.getId().isBlank()) {
                    continue;
                }
                String id = option.getId().trim();
                lookup.put(normalizeKey(id), id);
                lookup.put(normalizeText(id), id);
                if (option.getLabel() != null && !option.getLabel().isBlank()) {
                    lookup.put(normalizeKey(option.getLabel()), id);
                    lookup.put(normalizeText(option.getLabel()), id);
                }
            }
        }
        return lookup.getOrDefault(normalizeKey(rawValue), lookup.getOrDefault(normalizeText(rawValue), rawValue));
    }

    private List<String> normalizeEvidenceRefs(List<String> refs, ModelDiagnosisBrief brief) {
        if (refs == null || refs.isEmpty()) {
            return refs;
        }
        Map<String, String> lookup = evidenceLookup(brief);
        return refs.stream()
                .map(ref -> normalizeEvidenceRef(ref, lookup))
                .filter(ref -> ref != null && !ref.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeEvidenceRef(String rawValue, Map<String, String> lookup) {
        if (rawValue == null || rawValue.isBlank()) {
            return rawValue;
        }
        String trimmed = rawValue.trim();
        return lookup.getOrDefault(normalizeKey(trimmed), trimmed);
    }

    private Map<String, String> evidenceLookup(ModelDiagnosisBrief brief) {
        Map<String, String> refs = new LinkedHashMap<>();
        if (brief == null) {
            return refs;
        }
        addEvidenceRefs(refs, brief.getEvidenceRefs());
        if (brief.getCandidateSignals() != null) {
            brief.getCandidateSignals().stream()
                    .map(ModelDiagnosisBrief.CandidateSignal::getEvidenceRef)
                    .filter(ref -> ref != null && !ref.isBlank())
                    .forEach(ref -> refs.putIfAbsent(normalizeKey(ref), ref));
        }
        return refs;
    }

    private void addEvidenceRefs(Map<String, String> refs, List<String> values) {
        if (values == null) {
            return;
        }
        values.stream()
                .filter(ref -> ref != null && !ref.isBlank())
                .forEach(ref -> refs.putIfAbsent(normalizeKey(ref), ref));
    }

    private Set<String> actionLookup(List<StandardLibraryPack.TeachingActionOption> actions) {
        Set<String> ids = new LinkedHashSet<>();
        if (actions != null) {
            actions.stream()
                    .map(StandardLibraryPack.TeachingActionOption::getId)
                    .filter(id -> id != null && !id.isBlank())
                    .forEach(id -> ids.add(id.trim()));
        }
        return ids;
    }

    private String resolveAction(String rawValue, Set<String> allowedActions) {
        if (rawValue == null || rawValue.isBlank() || allowedActions == null || allowedActions.isEmpty()) {
            return rawValue;
        }
        String key = normalizeKey(rawValue);
        return allowedActions.stream()
                .filter(action -> normalizeKey(action).equals(key))
                .findFirst()
                .orElse(rawValue);
    }

    private String normalizeRisk(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return rawValue;
        }
        return rawValue.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeKey(String value) {
        return normalizeText(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "");
    }
}

package com.onlinejudge.submission.application;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class ModelOutputValidator {

    public ExternalModelStagePayloads.StageValidationResult validateDiagnosisJudgeOutput(
            ExternalModelStagePayloads.DiagnosisJudgeOutput output,
            ModelDiagnosisBrief brief,
            StandardLibraryPack standardLibraryPack) {
        if (output == null) {
            return invalid(ModelStageFailureReason.EMPTY_RESPONSE, "Diagnosis judge output is empty.");
        }
        if (isHighRisk(output.getAnswerLeakRisk())) {
            return invalid(ModelStageFailureReason.SAFETY_RISK,
                    "Diagnosis judge output has high answer leak risk: answerLeakRisk=HIGH.");
        }
        Set<String> allowedIssueTags = tagIds(standardLibraryPack == null ? null : standardLibraryPack.getIssueTags());
        if (!allowedIssueTags.contains(normalize(output.getPrimaryIssueTag()))) {
            return invalid(ModelStageFailureReason.INVALID_TAG, "Primary issue tag is not in standard library pack.");
        }
        if (output.getFineGrainedTag() != null && !output.getFineGrainedTag().isBlank()) {
            Set<String> allowedFineTags = tagIds(standardLibraryPack == null ? null : standardLibraryPack.getFineGrainedTags());
            if (!allowedFineTags.contains(normalize(output.getFineGrainedTag()))) {
                return invalid(ModelStageFailureReason.INVALID_TAG, "Fine-grained tag is not in standard library pack.");
            }
        }
        if (!evidenceRefsValid(output.getEvidenceRefs(), brief)) {
            return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF, "Evidence refs are missing or not present in brief evidence.");
        }
        if (diagnosisReliesOnlyOnMemory(output, brief)) {
            return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF,
                    "Diagnosis relies only on student memory evidence; current submission evidence is required.");
        }
        if (memoryConflictSelected(output, brief)) {
            return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF,
                    "Diagnosis selects a memory-conflicting tag without current evidence support.");
        }
        if (teachingOnlyMemoryTagSelected(output, brief)) {
            return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF,
                    "Diagnosis selects a teaching-only memory tag without current evidence support.");
        }
        return valid();
    }

    public ExternalModelStagePayloads.StageValidationResult validateTeachingHintOutput(
            ExternalModelStagePayloads.TeachingHintOutput output,
            ExternalModelStagePayloads.DiagnosisJudgeOutput decision,
            ModelDiagnosisBrief brief,
            StandardLibraryPack standardLibraryPack) {
        if (output == null) {
            return invalid(ModelStageFailureReason.EMPTY_RESPONSE, "Teaching hint output is empty.");
        }
        if (output.getStudentHint() == null || output.getStudentHint().isBlank()
                || output.getStudentHintPlan() == null
                || output.getLearningInterventionPlan() == null) {
            return invalid(ModelStageFailureReason.INVALID_JSON, "Teaching hint output is missing required fields.");
        }
        String safetyTrigger = firstSafetyTrigger(output);
        if (!safetyTrigger.isBlank()) {
            return invalid(ModelStageFailureReason.SAFETY_RISK,
                    "Teaching hint output has high answer leak risk: " + safetyTrigger);
        }
        Set<String> allowedActions = teachingActionIds(standardLibraryPack == null ? null : standardLibraryPack.getTeachingActions());
        String action = normalize(output.getStudentHintPlan().getTeachingAction());
        if (!allowedActions.isEmpty() && !allowedActions.contains(action)) {
            return invalid(ModelStageFailureReason.INVALID_TAG, "Teaching action is not in standard library pack.");
        }
        Set<String> validRefs = validEvidenceRefs(brief);
        if (decision != null && decision.getEvidenceRefs() != null) {
            validRefs.addAll(decision.getEvidenceRefs());
        }
        if (!refsSubset(output.getStudentHintPlan().getEvidenceRefs(), validRefs)
                || !refsSubset(output.getLearningInterventionPlan().getEvidenceRefs(), validRefs)) {
            return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF, "Teaching output references evidence outside the brief or decision.");
        }
        return valid();
    }

    private boolean evidenceRefsValid(List<String> refs, ModelDiagnosisBrief brief) {
        Set<String> validRefs = validEvidenceRefs(brief);
        return refs != null && !refs.isEmpty() && refsSubset(refs, validRefs);
    }

    private boolean diagnosisReliesOnlyOnMemory(ExternalModelStagePayloads.DiagnosisJudgeOutput output,
                                                ModelDiagnosisBrief brief) {
        if (output == null || output.getEvidenceRefs() == null || output.getEvidenceRefs().isEmpty()) {
            return false;
        }
        if (isNeedsMoreEvidence(output.getPrimaryIssueTag())) {
            return false;
        }
        boolean citesMemory = output.getEvidenceRefs().stream().anyMatch(this::isMemoryRef);
        boolean citesCurrent = output.getEvidenceRefs().stream().anyMatch(this::isCurrentEvidenceRef);
        if (!citesMemory || citesCurrent) {
            return false;
        }
        return !hasCurrentSignalSupport(output, brief);
    }

    private boolean memoryConflictSelected(ExternalModelStagePayloads.DiagnosisJudgeOutput output,
                                           ModelDiagnosisBrief brief) {
        if (output == null || brief == null || brief.getMemoryCalibration() == null) {
            return false;
        }
        ModelDiagnosisBrief.MemoryCalibration calibration = brief.getMemoryCalibration();
        if (!"CONFLICTING".equalsIgnoreCase(calibration.getMemoryRelevance())) {
            return false;
        }
        Set<String> conflicting = new LinkedHashSet<>();
        if (calibration.getConflictingMemoryTags() != null) {
            calibration.getConflictingMemoryTags().stream()
                    .map(this::normalize)
                    .forEach(conflicting::add);
        }
        if (conflicting.isEmpty()) {
            return false;
        }
        boolean selectedConflict = conflicting.contains(normalize(output.getPrimaryIssueTag()))
                || (output.getFineGrainedTag() != null && conflicting.contains(normalize(output.getFineGrainedTag())));
        return selectedConflict && !hasCurrentSignalSupport(output, brief);
    }

    private boolean teachingOnlyMemoryTagSelected(ExternalModelStagePayloads.DiagnosisJudgeOutput output,
                                                  ModelDiagnosisBrief brief) {
        if (output == null || brief == null || brief.getMemoryCalibration() == null) {
            return false;
        }
        ModelDiagnosisBrief.MemoryCalibration calibration = brief.getMemoryCalibration();
        if (!Boolean.TRUE.equals(calibration.getTeachingUseOnly())) {
            return false;
        }
        Set<String> memoryOnlyTags = new LinkedHashSet<>();
        if (calibration.getMemoryOnlyTags() != null) {
            calibration.getMemoryOnlyTags().stream()
                    .map(this::normalize)
                    .forEach(memoryOnlyTags::add);
        }
        if (memoryOnlyTags.isEmpty()) {
            return false;
        }
        boolean selectedMemoryOnly = memoryOnlyTags.contains(normalize(output.getPrimaryIssueTag()))
                || (output.getFineGrainedTag() != null && memoryOnlyTags.contains(normalize(output.getFineGrainedTag())));
        return selectedMemoryOnly && !hasCurrentSignalSupport(output, brief);
    }

    private boolean hasCurrentSignalSupport(ExternalModelStagePayloads.DiagnosisJudgeOutput output,
                                            ModelDiagnosisBrief brief) {
        if (brief == null || brief.getCandidateSignals() == null) {
            return false;
        }
        Set<String> citedRefs = new LinkedHashSet<>(output.getEvidenceRefs() == null ? List.of() : output.getEvidenceRefs());
        return brief.getCandidateSignals().stream()
                .filter(signal -> signal != null && !isMemoryRef(signal.getEvidenceRef()))
                .filter(signal -> signal.getEvidenceRef() != null && citedRefs.contains(signal.getEvidenceRef()))
                .anyMatch(signal -> tagMatches(output, signal));
    }

    private boolean tagMatches(ExternalModelStagePayloads.DiagnosisJudgeOutput output,
                               ModelDiagnosisBrief.CandidateSignal signal) {
        String primary = normalize(output.getPrimaryIssueTag());
        String fine = normalize(output.getFineGrainedTag());
        return primary.equals(normalize(signal.getIssueTag()))
                || (!fine.isBlank() && fine.equals(normalize(signal.getFineGrainedTag())));
    }

    private Set<String> validEvidenceRefs(ModelDiagnosisBrief brief) {
        Set<String> refs = new LinkedHashSet<>();
        if (brief == null) {
            return refs;
        }
        if (brief.getEvidenceRefs() != null) {
            refs.addAll(brief.getEvidenceRefs());
        }
        if (brief.getCandidateSignals() != null) {
            brief.getCandidateSignals().stream()
                    .map(ModelDiagnosisBrief.CandidateSignal::getEvidenceRef)
                    .filter(ref -> ref != null && !ref.isBlank())
                    .forEach(refs::add);
        }
        return refs;
    }

    private boolean refsSubset(List<String> refs, Set<String> validRefs) {
        if (refs == null || refs.isEmpty()) {
            return false;
        }
        return refs.stream().allMatch(validRefs::contains);
    }

    private boolean isMemoryRef(String ref) {
        return ref != null && ref.startsWith("memory:");
    }

    private boolean isCurrentEvidenceRef(String ref) {
        return ref != null && !isMemoryRef(ref);
    }

    private boolean isNeedsMoreEvidence(String tag) {
        return "NEEDS_MORE_EVIDENCE".equals(normalize(tag));
    }

    private Set<String> tagIds(List<StandardLibraryPack.TagOption> tags) {
        Set<String> ids = new LinkedHashSet<>();
        if (tags != null) {
            tags.stream()
                    .map(StandardLibraryPack.TagOption::getId)
                    .map(this::normalize)
                    .forEach(ids::add);
        }
        return ids;
    }

    private Set<String> teachingActionIds(List<StandardLibraryPack.TeachingActionOption> actions) {
        Set<String> ids = new LinkedHashSet<>();
        if (actions != null) {
            actions.stream()
                    .map(StandardLibraryPack.TeachingActionOption::getId)
                    .map(this::normalize)
                    .forEach(ids::add);
        }
        return ids;
    }

    private boolean isHighRisk(String risk) {
        return "HIGH".equalsIgnoreCase(risk == null ? "" : risk.trim());
    }

    private boolean containsUnsafeLeak(String text) {
        return !unsafeLeakTrigger(text).isBlank();
    }

    private String firstSafetyTrigger(ExternalModelStagePayloads.TeachingHintOutput output) {
        if (isHighRisk(output.getStudentHintPlan().getAnswerLeakRisk())) {
            return "studentHintPlan.answerLeakRisk=HIGH";
        }
        if (isHighRisk(output.getLearningInterventionPlan().getAnswerLeakRisk())) {
            return "learningInterventionPlan.answerLeakRisk=HIGH";
        }
        String trigger = unsafeLeakTrigger(output.getStudentHint());
        if (!trigger.isBlank()) {
            return "studentHint contains " + trigger;
        }
        trigger = unsafeLeakTrigger(output.getStudentHintPlan().getNextAction());
        if (!trigger.isBlank()) {
            return "studentHintPlan.nextAction contains " + trigger;
        }
        trigger = unsafeLeakTrigger(output.getStudentHintPlan().getCoachQuestion());
        if (!trigger.isBlank()) {
            return "studentHintPlan.coachQuestion contains " + trigger;
        }
        trigger = unsafeLeakTrigger(output.getLearningInterventionPlan().getStudentTask());
        if (!trigger.isBlank()) {
            return "learningInterventionPlan.studentTask contains " + trigger;
        }
        return "";
    }

    private String unsafeLeakTrigger(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (String marker : List.of(
                "完整代码",
                "参考代码",
                "最终答案",
                "参考答案",
                "答案如下",
                "直接改成",
                "改成",
                "改为",
                "替换为",
                "hidden test",
                "for _ in range",
                "while q",
                "while used < steps",
                "while used<steps",
                "while nums",
                "range(1, n + 1)",
                "range(1,n+1)",
                "sqrt",
                "dp[i - 2] +",
                "dp[i-2]+",
                "```",
                "def ",
                "#include",
                "int main"
        )) {
            if (normalized.contains(marker)) {
                return marker;
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private ExternalModelStagePayloads.StageValidationResult valid() {
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(true)
                .failureReason(ModelStageFailureReason.NONE)
                .message("")
                .build();
    }

    private ExternalModelStagePayloads.StageValidationResult invalid(ModelStageFailureReason reason, String message) {
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(false)
                .failureReason(reason)
                .message(message)
                .build();
    }
}

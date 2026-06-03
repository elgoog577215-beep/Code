package com.onlinejudge.submission.application;

import org.springframework.stereotype.Component;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;

import java.util.LinkedHashSet;
import java.util.List;
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
        if (ModelOutputSafetyPolicy.isHighRisk(output.getAnswerLeakRisk())) {
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
        String invalidEvidence = invalidRefSummary(
                "diagnosisDecision.evidenceRefs",
                output.getEvidenceRefs(),
                validEvidenceRefs(brief)
        );
        if (!invalidEvidence.isBlank()) {
            return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF, invalidEvidence);
        }
        ExternalModelStagePayloads.StageValidationResult educationValidation =
                validateEducationAgentJudgment(output, brief, standardLibraryPack);
        if (!educationValidation.isValid()) {
            return educationValidation;
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
        String invalidStudentHintRef = invalidRefSummary(
                "teachingHint.studentHintPlan.evidenceRefs",
                output.getStudentHintPlan().getEvidenceRefs(),
                validRefs
        );
        String invalidInterventionRef = invalidRefSummary(
                "teachingHint.learningInterventionPlan.evidenceRefs",
                output.getLearningInterventionPlan().getEvidenceRefs(),
                validRefs
        );
        if (!invalidStudentHintRef.isBlank() || !invalidInterventionRef.isBlank()) {
            return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF,
                    !invalidStudentHintRef.isBlank() ? invalidStudentHintRef : invalidInterventionRef);
        }
        return valid();
    }

    public ExternalModelStagePayloads.StageValidationResult validateStudentFeedback(
            SubmissionAnalysisResponse.StudentFeedback feedback,
            ExternalModelStagePayloads.DiagnosisJudgeOutput decision,
            ModelDiagnosisBrief brief,
            StandardLibraryPack standardLibraryPack) {
        if (feedback == null) {
            return invalid(ModelStageFailureReason.EMPTY_RESPONSE, "Student feedback output is empty.");
        }
        if (feedback.getSummary() == null || feedback.getSummary().isBlank()
                || feedback.getBlockingIssues() == null || feedback.getBlockingIssues().isEmpty()
                || feedback.getNextLearningAction() == null) {
            return invalid(ModelStageFailureReason.INVALID_JSON, "Student feedback is missing required fields.");
        }
        String safetyTrigger = firstFeedbackSafetyTrigger(feedback);
        if (!safetyTrigger.isBlank()) {
            return invalid(ModelStageFailureReason.SAFETY_RISK,
                    "Student feedback has high answer leak risk: " + safetyTrigger);
        }
        Set<String> validRefs = validEvidenceRefs(brief);
        if (decision != null && decision.getEvidenceRefs() != null) {
            validRefs.addAll(decision.getEvidenceRefs());
        }
        for (SubmissionAnalysisResponse.FeedbackIssue issue : feedback.getBlockingIssues()) {
            if (issue == null || issue.getStudentMessage() == null || issue.getStudentMessage().isBlank()
                    || issue.getNextAction() == null || issue.getNextAction().isBlank()) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "Blocking issue is missing student message or next action.");
            }
            String invalidIssueRef = invalidRefSummary(
                    "studentFeedback.blockingIssues[" + indexOf(feedback.getBlockingIssues(), issue) + "].evidenceRefs",
                    issue.getEvidenceRefs(),
                    validRefs
            );
            if (!invalidIssueRef.isBlank()) {
                return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF,
                        invalidIssueRef);
            }
        }
        String invalidNextActionRef = invalidRefSummary(
                "studentFeedback.nextLearningAction.evidenceRefs",
                feedback.getNextLearningAction().getEvidenceRefs(),
                validRefs
        );
        if (!invalidNextActionRef.isBlank()) {
            return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF,
                    invalidNextActionRef);
        }
        if (ModelOutputSafetyPolicy.isHighRisk(feedback.getNextLearningAction().getAnswerLeakRisk())) {
            return invalid(ModelStageFailureReason.SAFETY_RISK, "nextLearningAction.answerLeakRisk=HIGH");
        }
        Set<String> allowedImprovementTags = improvementTagIds(
                standardLibraryPack == null ? null : standardLibraryPack.getImprovementTags());
        if (feedback.getImprovementOpportunities() != null) {
            for (SubmissionAnalysisResponse.ImprovementOpportunity item : feedback.getImprovementOpportunities()) {
                if (item == null) {
                    continue;
                }
                if (!allowedImprovementTags.isEmpty()
                        && !allowedImprovementTags.contains(normalize(item.getCategory()))) {
                    return invalid(ModelStageFailureReason.INVALID_TAG,
                            "Improvement category is not in standard library pack.");
                }
                if (item.getStudentMessage() == null || item.getStudentMessage().isBlank()
                        || item.getBenefit() == null || item.getBenefit().isBlank()) {
                    return invalid(ModelStageFailureReason.INVALID_JSON,
                            "Improvement opportunity is missing message or benefit.");
                }
                String invalidImprovementRef = item.getEvidenceRefs() == null || item.getEvidenceRefs().isEmpty()
                        ? ""
                        : invalidRefSummary(
                        "studentFeedback.improvementOpportunities[" + indexOf(feedback.getImprovementOpportunities(), item) + "].evidenceRefs",
                        item.getEvidenceRefs(),
                        validRefs
                );
                if (!invalidImprovementRef.isBlank()) {
                    return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF,
                            invalidImprovementRef);
                }
            }
        }
        return valid();
    }

    private ExternalModelStagePayloads.StageValidationResult validateEducationAgentJudgment(
            ExternalModelStagePayloads.DiagnosisJudgeOutput output,
            ModelDiagnosisBrief brief,
            StandardLibraryPack standardLibraryPack) {
        if (ModelOutputSafetyPolicy.isHighRisk(output.getNextLearningAction() == null ? null : output.getNextLearningAction().getAnswerLeakRisk())) {
            return invalid(ModelStageFailureReason.SAFETY_RISK, "Education nextLearningAction.answerLeakRisk=HIGH");
        }
        String safetyTrigger = firstEducationSafetyTrigger(output);
        if (!safetyTrigger.isBlank()) {
            return invalid(ModelStageFailureReason.SAFETY_RISK,
                    "Education agent judgment has high answer leak risk: " + safetyTrigger);
        }
        Set<String> validRefs = validEvidenceRefs(brief);
        if (output.getEvidenceRefs() != null) {
            validRefs.addAll(output.getEvidenceRefs());
        }
        String invalidSecondaryNote = invalidEducationNotesMessage(
                output.getSecondaryIssues(),
                "diagnosisDecision.secondaryIssues",
                validRefs,
                standardLibraryPack
        );
        String invalidDistractorNote = invalidEducationNotesMessage(
                output.getDistractorNotes(),
                "diagnosisDecision.distractorNotes",
                validRefs,
                standardLibraryPack
        );
        if (!invalidSecondaryNote.isBlank() || !invalidDistractorNote.isBlank()) {
            return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF,
                    !invalidSecondaryNote.isBlank() ? invalidSecondaryNote : invalidDistractorNote);
        }
        if (output.getImprovementOpportunities() != null
                && !improvementOpportunitiesValid(output.getImprovementOpportunities(), validRefs, standardLibraryPack)) {
            return invalid(ModelStageFailureReason.INVALID_TAG,
                    "Education improvement opportunities are invalid.");
        }
        String invalidEducationNextActionRef = output.getNextLearningAction() == null
                ? ""
                : invalidRefSummary(
                "diagnosisDecision.nextLearningAction.evidenceRefs",
                output.getNextLearningAction().getEvidenceRefs(),
                validRefs
        );
        if (!invalidEducationNextActionRef.isBlank()) {
            return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF,
                    invalidEducationNextActionRef);
        }
        return valid();
    }

    private String invalidEducationNotesMessage(List<ExternalModelStagePayloads.EducationIssueNote> notes,
                                                String fieldName,
                                                Set<String> validRefs,
                                                StandardLibraryPack standardLibraryPack) {
        if (notes == null || notes.isEmpty()) {
            return "";
        }
        Set<String> allowedIssueTags = tagIds(standardLibraryPack == null ? null : standardLibraryPack.getIssueTags());
        Set<String> allowedFineTags = tagIds(standardLibraryPack == null ? null : standardLibraryPack.getFineGrainedTags());
        for (int index = 0; index < notes.size(); index++) {
            ExternalModelStagePayloads.EducationIssueNote note = notes.get(index);
            if (note == null) {
                continue;
            }
            if (note.getIssueTag() != null && !note.getIssueTag().isBlank()
                    && !allowedIssueTags.isEmpty()
                    && !allowedIssueTags.contains(normalize(note.getIssueTag()))) {
                return fieldName + "[" + index + "].issueTag invalid tag=" + note.getIssueTag();
            }
            if (note.getFineGrainedTag() != null && !note.getFineGrainedTag().isBlank()
                    && !allowedFineTags.isEmpty()
                    && !allowedFineTags.contains(normalize(note.getFineGrainedTag()))) {
                return fieldName + "[" + index + "].fineGrainedTag invalid tag=" + note.getFineGrainedTag();
            }
            String invalidRef = note.getEvidenceRefs() == null || note.getEvidenceRefs().isEmpty()
                    ? ""
                    : invalidRefSummary(fieldName + "[" + index + "].evidenceRefs", note.getEvidenceRefs(), validRefs);
            if (!invalidRef.isBlank()) {
                return invalidRef;
            }
        }
        return "";
    }

    private boolean improvementOpportunitiesValid(List<SubmissionAnalysisResponse.ImprovementOpportunity> opportunities,
                                                  Set<String> validRefs,
                                                  StandardLibraryPack standardLibraryPack) {
        Set<String> allowedImprovementTags = improvementTagIds(
                standardLibraryPack == null ? null : standardLibraryPack.getImprovementTags());
        for (SubmissionAnalysisResponse.ImprovementOpportunity item : opportunities) {
            if (item == null) {
                continue;
            }
            if (!allowedImprovementTags.isEmpty()
                    && !allowedImprovementTags.contains(normalize(item.getCategory()))) {
                return false;
            }
            if (item.getEvidenceRefs() != null && !item.getEvidenceRefs().isEmpty()
                    && !refsSubset(item.getEvidenceRefs(), validRefs)) {
                return false;
            }
        }
        return true;
    }

    private boolean evidenceRefsValid(List<String> refs, ModelDiagnosisBrief brief) {
        Set<String> validRefs = validEvidenceRefs(brief);
        return refs != null && !refs.isEmpty() && refsSubset(refs, validRefs);
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

    private String invalidRefSummary(String fieldName, List<String> refs, Set<String> validRefs) {
        if (refs == null || refs.isEmpty()) {
            return fieldName + " missing evidenceRefs.";
        }
        for (String ref : refs) {
            if (ref == null || ref.isBlank()) {
                return fieldName + " contains blank evidenceRef.";
            }
            if (validRefs == null || !validRefs.contains(ref)) {
                return fieldName + " invalid evidenceRef=" + ref;
            }
        }
        return "";
    }

    private int indexOf(List<?> values, Object value) {
        return values == null ? -1 : values.indexOf(value);
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

    private Set<String> improvementTagIds(List<StandardLibraryPack.ImprovementTagOption> tags) {
        Set<String> ids = new LinkedHashSet<>();
        if (tags != null) {
            tags.stream()
                    .map(StandardLibraryPack.ImprovementTagOption::getId)
                    .map(this::normalize)
                    .forEach(ids::add);
        }
        return ids;
    }

    private boolean containsUnsafeLeak(String text) {
        return !ModelOutputSafetyPolicy.unsafeLeakTrigger(text).isBlank();
    }

    private String firstSafetyTrigger(ExternalModelStagePayloads.TeachingHintOutput output) {
        if (ModelOutputSafetyPolicy.isHighRisk(output.getAnswerLeakRisk())) {
            return "answerLeakRisk=HIGH";
        }
        if (ModelOutputSafetyPolicy.isHighRisk(output.getStudentHintPlan().getAnswerLeakRisk())) {
            return "studentHintPlan.answerLeakRisk=HIGH";
        }
        if (ModelOutputSafetyPolicy.isHighRisk(output.getLearningInterventionPlan().getAnswerLeakRisk())) {
            return "learningInterventionPlan.answerLeakRisk=HIGH";
        }
        String trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(output.getStudentHint());
        if (!trigger.isBlank()) {
            return "studentHint contains " + trigger;
        }
        trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(output.getStudentHintPlan().getNextAction());
        if (!trigger.isBlank()) {
            return "studentHintPlan.nextAction contains " + trigger;
        }
        trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(output.getStudentHintPlan().getCoachQuestion());
        if (!trigger.isBlank()) {
            return "studentHintPlan.coachQuestion contains " + trigger;
        }
        trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(output.getLearningInterventionPlan().getStudentTask());
        if (!trigger.isBlank()) {
            return "learningInterventionPlan.studentTask contains " + trigger;
        }
        return "";
    }

    private String firstEducationSafetyTrigger(ExternalModelStagePayloads.DiagnosisJudgeOutput output) {
        String trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(output.getPrimaryReasoning());
        if (!trigger.isBlank()) {
            return "primaryReasoning contains " + trigger;
        }
        trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(output.getTeachingPriority());
        if (!trigger.isBlank()) {
            return "teachingPriority contains " + trigger;
        }
        trigger = firstEducationIssueSafetyTrigger(output.getSecondaryIssues(), "secondaryIssues");
        if (!trigger.isBlank()) {
            return trigger;
        }
        trigger = firstEducationIssueSafetyTrigger(output.getDistractorNotes(), "distractorNotes");
        if (!trigger.isBlank()) {
            return trigger;
        }
        if (output.getImprovementOpportunities() != null) {
            for (SubmissionAnalysisResponse.ImprovementOpportunity item : output.getImprovementOpportunities()) {
                trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(item == null ? null : item.getStudentMessage());
                if (!trigger.isBlank()) {
                    return "education improvement contains " + trigger;
                }
            }
        }
        if (output.getNextLearningAction() != null) {
            trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(output.getNextLearningAction().getAction());
            if (!trigger.isBlank()) {
                return "education nextLearningAction.action contains " + trigger;
            }
            trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(output.getNextLearningAction().getTask());
            if (!trigger.isBlank()) {
                return "education nextLearningAction.task contains " + trigger;
            }
            trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(output.getNextLearningAction().getCheckQuestion());
            if (!trigger.isBlank()) {
                return "education nextLearningAction.checkQuestion contains " + trigger;
            }
        }
        return "";
    }

    private String firstEducationIssueSafetyTrigger(List<ExternalModelStagePayloads.EducationIssueNote> notes,
                                                    String fieldName) {
        if (notes == null) {
            return "";
        }
        for (ExternalModelStagePayloads.EducationIssueNote note : notes) {
            String trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(note == null ? null : note.getMessage());
            if (!trigger.isBlank()) {
                return fieldName + " contains " + trigger;
            }
        }
        return "";
    }

    private String firstFeedbackSafetyTrigger(SubmissionAnalysisResponse.StudentFeedback feedback) {
        String trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(feedback.getSummary());
        if (!trigger.isBlank()) {
            return "summary contains " + trigger;
        }
        if (feedback.getBlockingIssues() != null) {
            for (SubmissionAnalysisResponse.FeedbackIssue issue : feedback.getBlockingIssues()) {
                trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(issue == null ? null : issue.getStudentMessage());
                if (!trigger.isBlank()) {
                    return "blockingIssue.studentMessage contains " + trigger;
                }
                trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(issue == null ? null : issue.getNextAction());
                if (!trigger.isBlank()) {
                    return "blockingIssue.nextAction contains " + trigger;
                }
            }
        }
        if (feedback.getImprovementOpportunities() != null) {
            for (SubmissionAnalysisResponse.ImprovementOpportunity item : feedback.getImprovementOpportunities()) {
                trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(item == null ? null : item.getStudentMessage());
                if (!trigger.isBlank()) {
                    return "improvement.studentMessage contains " + trigger;
                }
            }
        }
        if (feedback.getNextLearningAction() != null) {
            trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(feedback.getNextLearningAction().getTask());
            if (!trigger.isBlank()) {
                return "nextLearningAction.task contains " + trigger;
            }
            trigger = ModelOutputSafetyPolicy.unsafeLeakTrigger(feedback.getNextLearningAction().getCheckQuestion());
            if (!trigger.isBlank()) {
                return "nextLearningAction.checkQuestion contains " + trigger;
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
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

package com.onlinejudge.submission.application;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class AdviceGenerationOutputValidator {

    public ExternalModelStagePayloads.StageValidationResult validate(AdviceGenerationOutput output,
                                                                     ModelDiagnosisBrief brief,
                                                                     StandardLibraryPack standardLibraryPack) {
        if (output == null) {
            return invalid(ModelStageFailureReason.EMPTY_RESPONSE, "Advice generation output is empty.");
        }
        if (output.getCaseUnderstanding() == null) {
            return invalid(ModelStageFailureReason.INVALID_JSON, "caseUnderstanding is missing.");
        }
        Set<String> evidenceRefs = evidenceRefs(brief);
        if (!isAccepted(brief) && safe(output.getBasicLayerAdvice()).isEmpty()) {
            return invalid(ModelStageFailureReason.INVALID_JSON, "Non-accepted submission requires basicLayerAdvice.");
        }
        if (safe(output.getNextStepPlan()).isEmpty()) {
            return invalid(ModelStageFailureReason.INVALID_JSON, "nextStepPlan is empty.");
        }
        String primaryRef = output.getCaseUnderstanding().getPrimaryEvidenceRef();
        if (primaryRef == null || primaryRef.isBlank() || !evidenceRefs.contains(primaryRef)) {
            return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF,
                    "caseUnderstanding.primaryEvidenceRef is invalid.");
        }
        Set<String> mistakeIds = ids(standardLibraryPack == null ? null : standardLibraryPack.getMistakePoints());
        mistakeIds.addAll(ids(standardLibraryPack == null ? null : standardLibraryPack.getBasicCauses()));
        Set<String> skillIds = ids(standardLibraryPack == null ? null : standardLibraryPack.getSkillUnits());
        Set<String> improvementIds = ids(standardLibraryPack == null ? null : standardLibraryPack.getImprovementPoints());

        for (AdviceGenerationOutput.BasicLayerAdvice item : safe(output.getBasicLayerAdvice())) {
            if (item == null) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "basicLayerAdvice contains null item.");
            }
            if (blank(item.getTitle()) || blank(item.getStudentAction()) || blank(item.getCheckQuestion())) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "basicLayerAdvice is missing title/action/question.");
            }
            if (!blank(item.getMistakePointId()) && !mistakeIds.contains(normalize(item.getMistakePointId()))) {
                return invalid(ModelStageFailureReason.INVALID_TAG,
                        "Unknown mistakePointId: " + item.getMistakePointId());
            }
            if (!blank(item.getSkillUnitId()) && !skillIds.contains(normalize(item.getSkillUnitId()))) {
                return invalid(ModelStageFailureReason.INVALID_TAG,
                        "Unknown skillUnitId: " + item.getSkillUnitId());
            }
            String invalidEvidence = invalidEvidenceRefs(item.getEvidenceRefs(), evidenceRefs,
                    "basicLayerAdvice.evidenceRefs");
            if (!invalidEvidence.isBlank()) {
                return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF, invalidEvidence);
            }
            if (invalidConfidence(item.getConfidence())) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "basicLayerAdvice confidence is invalid.");
            }
            if (unsafe(item.getTitle(), item.getWhatHappened(), item.getWhyItMatters(),
                    item.getStudentAction(), item.getCheckQuestion())) {
                return invalid(ModelStageFailureReason.SAFETY_RISK, "basicLayerAdvice contains answer leak.");
            }
        }

        for (AdviceGenerationOutput.ImprovementLayerAdvice item : safe(output.getImprovementLayerAdvice())) {
            if (item == null) {
                continue;
            }
            if (blank(item.getTitle()) || blank(item.getSuggestion()) || blank(item.getStudentBenefit())) {
                return invalid(ModelStageFailureReason.INVALID_JSON,
                        "improvementLayerAdvice is missing title/suggestion/benefit.");
            }
            if (!blank(item.getImprovementPointId()) && !improvementIds.contains(normalize(item.getImprovementPointId()))) {
                return invalid(ModelStageFailureReason.INVALID_TAG,
                        "Unknown improvementPointId: " + item.getImprovementPointId());
            }
            if (!blank(item.getSkillUnitId()) && !skillIds.contains(normalize(item.getSkillUnitId()))) {
                return invalid(ModelStageFailureReason.INVALID_TAG,
                        "Unknown improvement skillUnitId: " + item.getSkillUnitId());
            }
            String invalidEvidence = invalidEvidenceRefs(item.getEvidenceRefs(), evidenceRefs,
                    "improvementLayerAdvice.evidenceRefs");
            if (!invalidEvidence.isBlank()) {
                return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF, invalidEvidence);
            }
            if (invalidConfidence(item.getConfidence())) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "improvementLayerAdvice confidence is invalid.");
            }
            if (unsafe(item.getTitle(), item.getCurrentLimit(), item.getSuggestion(), item.getStudentBenefit())) {
                return invalid(ModelStageFailureReason.SAFETY_RISK, "improvementLayerAdvice contains answer leak.");
            }
        }

        for (AdviceGenerationOutput.NextStepAdvice item : safe(output.getNextStepPlan())) {
            if (item == null || blank(item.getTarget()) || blank(item.getReason())) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "nextStepPlan item is incomplete.");
            }
            if (!blank(item.getEvidenceRef()) && !evidenceRefs.contains(item.getEvidenceRef())) {
                return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF,
                        "nextStepPlan evidenceRef is invalid: " + item.getEvidenceRef());
            }
            if (unsafe(item.getTarget(), item.getReason())) {
                return invalid(ModelStageFailureReason.SAFETY_RISK, "nextStepPlan contains answer leak.");
            }
        }
        if (unsafe(output.getStudentSummary(),
                output.getCaseUnderstanding().getProblemGoal(),
                output.getCaseUnderstanding().getCodeIntent(),
                output.getCaseUnderstanding().getBehaviorGap())) {
            return invalid(ModelStageFailureReason.SAFETY_RISK, "caseUnderstanding or studentSummary contains answer leak.");
        }
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(true)
                .stage("DIAGNOSIS_AND_ADVICE")
                .failureReason(ModelStageFailureReason.NONE)
                .message("")
                .build();
    }

    private Set<String> evidenceRefs(ModelDiagnosisBrief brief) {
        LinkedHashSet<String> refs = new LinkedHashSet<>();
        if (brief == null) {
            return refs;
        }
        if (brief.getEvidenceRefs() != null) {
            refs.addAll(brief.getEvidenceRefs());
        }
        if (brief.getCandidateSignals() != null) {
            brief.getCandidateSignals().stream()
                    .filter(signal -> signal != null && signal.getEvidenceRef() != null)
                    .map(ModelDiagnosisBrief.CandidateSignal::getEvidenceRef)
                    .forEach(refs::add);
        }
        return refs;
    }

    private Set<String> ids(List<?> values) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (values == null) {
            return ids;
        }
        for (Object value : values) {
            if (value instanceof StandardLibraryPack.MistakePointOption option) {
                ids.add(normalize(option.getId()));
            } else if (value instanceof StandardLibraryPack.BasicCauseOption option) {
                ids.add(normalize(option.getId()));
            } else if (value instanceof StandardLibraryPack.SkillUnitOption option) {
                ids.add(normalize(option.getId()));
            } else if (value instanceof StandardLibraryPack.ImprovementPointOption option) {
                ids.add(normalize(option.getId()));
            }
        }
        ids.remove("");
        return ids;
    }

    private String invalidEvidenceRefs(List<String> refs, Set<String> validRefs, String field) {
        if (refs == null || refs.isEmpty()) {
            return field + " is empty.";
        }
        for (String ref : refs) {
            if (ref == null || ref.isBlank() || !validRefs.contains(ref)) {
                return field + " contains invalid evidenceRef=" + ref;
            }
        }
        return "";
    }

    private boolean invalidConfidence(Double confidence) {
        return confidence == null || confidence < 0 || confidence > 1;
    }

    private boolean isAccepted(ModelDiagnosisBrief brief) {
        String verdict = brief == null || brief.getVerdict() == null ? "" : brief.getVerdict().trim().toUpperCase(Locale.ROOT);
        return "AC".equals(verdict) || "ACCEPTED".equals(verdict);
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean unsafe(String... values) {
        return ModelOutputSafetyPolicy.containsUnsafeLeak(values);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private ExternalModelStagePayloads.StageValidationResult invalid(ModelStageFailureReason reason, String message) {
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(false)
                .stage("DIAGNOSIS_AND_ADVICE")
                .failureReason(reason)
                .message(message == null ? "" : message)
                .build();
    }
}

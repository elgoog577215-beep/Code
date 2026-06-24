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
        if (hasStudentReport(output)) {
            return validateDiagnosisReportV2(output, brief, standardLibraryPack);
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

    private ExternalModelStagePayloads.StageValidationResult validateDiagnosisReportV2(
            AdviceGenerationOutput output,
            ModelDiagnosisBrief brief,
            StandardLibraryPack standardLibraryPack
    ) {
        List<String> softFixes = new java.util.ArrayList<>();
        AdviceGenerationOutput.StudentReport report = output.getStudentReport();
        if (!isAccepted(brief) && blank(report.getBasicLayerText())) {
            return invalid(ModelStageFailureReason.INVALID_JSON,
                    "Non-accepted submission requires studentReport.basicLayerText.");
        }
        if (blank(report.getNextActionText())) {
            return invalid(ModelStageFailureReason.INVALID_JSON, "studentReport.nextActionText is empty.");
        }
        String hintLevel = report.getHintLevel();
        if (!blank(hintLevel) && !List.of("L1", "L2", "L3", "L4").contains(hintLevel.trim().toUpperCase(Locale.ROOT))) {
            return invalid(ModelStageFailureReason.INVALID_JSON, "studentReport.hintLevel is invalid.");
        }
        if (unsafe(report.getBasicLayerText(), report.getImprovementLayerText(), report.getNextActionText(), output.getStudentSummary())) {
            return invalid(ModelStageFailureReason.SAFETY_RISK, "studentReport contains answer leak.");
        }

        Set<String> evidenceRefs = evidenceRefs(brief);
        List<String> orderedEvidenceRefs = evidenceRefs.stream().toList();
        Set<String> allowedIds = ids(standardLibraryPack == null ? null : standardLibraryPack.getMistakePoints());
        allowedIds.addAll(ids(standardLibraryPack == null ? null : standardLibraryPack.getBasicCauses()));
        allowedIds.addAll(ids(standardLibraryPack == null ? null : standardLibraryPack.getSkillUnits()));
        allowedIds.addAll(ids(standardLibraryPack == null ? null : standardLibraryPack.getImprovementPoints()));

        AdviceGenerationOutput.DiagnosisDecision decision = output.getDiagnosisDecision();
        if (decision != null) {
            String fit = normalize(decision.getLibraryFit());
            if (!fit.isBlank() && !List.of("HIT", "PARTIAL", "MISS").contains(fit)) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "diagnosisDecision.libraryFit is invalid.");
            }
            for (AdviceGenerationOutput.DiagnosisAnchor anchor : safe(decision.getAnchors())) {
                if (anchor == null) {
                    return invalid(ModelStageFailureReason.INVALID_JSON, "diagnosisDecision.anchors contains null item.");
                }
                String type = normalize(anchor.getType());
                boolean outOfLibrary = "OUT_OF_LIBRARY".equals(type);
                if (!outOfLibrary && !blank(anchor.getId()) && !allowedIds.contains(normalize(anchor.getId()))) {
                    softFixes.add("unknown anchor id converted to OUT_OF_LIBRARY: " + anchor.getId());
                    anchor.setId(null);
                    anchor.setType("OUT_OF_LIBRARY");
                }
                anchor.setEvidenceRefs(normalizeEvidenceRefs(anchor.getEvidenceRefs(), evidenceRefs, orderedEvidenceRefs, softFixes));
                String invalidEvidence = invalidEvidenceRefs(anchor.getEvidenceRefs(), evidenceRefs,
                        "diagnosisDecision.anchors.evidenceRefs");
                if (!invalidEvidence.isBlank()) {
                    return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF, invalidEvidence);
                }
                if (invalidConfidence(anchor.getConfidence())) {
                    return invalid(ModelStageFailureReason.INVALID_JSON, "diagnosisDecision anchor confidence is invalid.");
                }
                if (unsafe(anchor.getReason())) {
                    return invalid(ModelStageFailureReason.SAFETY_RISK, "diagnosisDecision anchor contains answer leak.");
                }
            }
            for (AdviceGenerationOutput.OutOfLibraryFinding finding : safe(decision.getOutOfLibraryFindings())) {
                if (finding == null || blank(finding.getName()) || blank(finding.getReason())) {
                    return invalid(ModelStageFailureReason.INVALID_JSON,
                            "diagnosisDecision.outOfLibraryFindings item is incomplete.");
                }
                finding.setEvidenceRefs(normalizeEvidenceRefs(finding.getEvidenceRefs(), evidenceRefs, orderedEvidenceRefs, softFixes));
                String invalidEvidence = invalidEvidenceRefs(finding.getEvidenceRefs(), evidenceRefs,
                        "diagnosisDecision.outOfLibraryFindings.evidenceRefs");
                if (!invalidEvidence.isBlank()) {
                    return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF, invalidEvidence);
                }
                if (invalidConfidence(finding.getConfidence())) {
                    return invalid(ModelStageFailureReason.INVALID_JSON,
                            "diagnosisDecision outOfLibraryFinding confidence is invalid.");
                }
            }
        }
        attachValidationTrace(output, softFixes, List.of());
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(true)
                .stage("DIAGNOSIS_REPORT")
                .failureReason(ModelStageFailureReason.NONE)
                .message("")
                .softFixes(softFixes)
                .hardFailures(List.of())
                .build();
    }

    private void attachValidationTrace(AdviceGenerationOutput output, List<String> softFixes, List<String> hardFailures) {
        if (output == null) {
            return;
        }
        AdviceGenerationOutput.TeacherTrace trace = output.getTeacherTrace();
        if (trace == null) {
            trace = AdviceGenerationOutput.TeacherTrace.builder().build();
            output.setTeacherTrace(trace);
        }
        trace.setSoftFixes(softFixes == null ? List.of() : softFixes);
        trace.setHardFailures(hardFailures == null ? List.of() : hardFailures);
    }

    private boolean hasStudentReport(AdviceGenerationOutput output) {
        AdviceGenerationOutput.StudentReport report = output.getStudentReport();
        return report != null && (!blank(report.getBasicLayerText())
                || !blank(report.getImprovementLayerText())
                || !blank(report.getNextActionText()));
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

    private List<String> normalizeEvidenceRefs(List<String> refs,
                                               Set<String> validRefs,
                                               List<String> orderedValidRefs,
                                               List<String> softFixes) {
        if (refs == null || refs.isEmpty() || validRefs == null || validRefs.isEmpty()) {
            return refs;
        }
        List<String> normalizedRefs = new java.util.ArrayList<>(refs);
        for (int i = 0; i < refs.size(); i++) {
            String ref = refs.get(i);
            if (ref == null || ref.isBlank() || validRefs.contains(ref)) {
                continue;
            }
            String replacement = evidenceAlias(ref, orderedValidRefs);
            if (!replacement.isBlank()) {
                normalizedRefs.set(i, replacement);
                softFixes.add("evidenceRef alias " + ref + " -> " + replacement);
            }
        }
        return normalizedRefs;
    }

    private String evidenceAlias(String ref, List<String> orderedValidRefs) {
        String normalized = normalize(ref);
        if (orderedValidRefs == null || orderedValidRefs.isEmpty()) {
            return "";
        }
        if (List.of("SOURCECODE", "CODE", "SOURCE").contains(normalized)) {
            return firstEvidenceWithPrefix(orderedValidRefs, "code:");
        }
        if (List.of("PROBLEMCONSTRAINTS", "CONSTRAINTS", "PROBLEM").contains(normalized)) {
            return firstEvidenceWithPrefix(orderedValidRefs, "judge:");
        }
        if (List.of("JUDGERESULT", "JUDGE", "VERDICT").contains(normalized)) {
            return firstEvidenceWithPrefix(orderedValidRefs, "judge:");
        }
        return "";
    }

    private String firstEvidenceWithPrefix(List<String> evidenceRefs, String prefix) {
        return evidenceRefs.stream()
                .filter(ref -> ref != null && ref.startsWith(prefix))
                .findFirst()
                .orElse("");
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
                .softFixes(List.of())
                .hardFailures(List.of(message == null ? "" : message))
                .build();
    }
}

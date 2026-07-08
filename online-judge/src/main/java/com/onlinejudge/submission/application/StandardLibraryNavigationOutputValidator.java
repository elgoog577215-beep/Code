package com.onlinejudge.submission.application;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class StandardLibraryNavigationOutputValidator {

    public ExternalModelStagePayloads.StageValidationResult validate(StandardLibraryNavigationOutput output,
                                                                     ModelDiagnosisBrief brief,
                                                                     StandardLibraryPack standardLibraryPack) {
        if (output == null) {
            return invalid(ModelStageFailureReason.EMPTY_RESPONSE, "Standard library navigation output is empty.");
        }
        String status = upper(output.getStatus());
        if (!List.of("CONTINUE", "DONE", "NO_MATCH").contains(status)) {
            return invalid(ModelStageFailureReason.INVALID_JSON, "navigation status is invalid.");
        }
        Set<String> evidenceRefs = evidenceRefs(brief);
        Set<String> knowledgeIds = ids(standardLibraryPack == null ? null : standardLibraryPack.getKnowledgeAnchors());
        Set<String> skillIds = ids(standardLibraryPack == null ? null : standardLibraryPack.getSkillUnits());
        Set<String> mistakeIds = ids(standardLibraryPack == null ? null : standardLibraryPack.getMistakePoints());
        mistakeIds.addAll(ids(standardLibraryPack == null ? null : standardLibraryPack.getBasicCauses()));
        Set<String> improvementIds = ids(standardLibraryPack == null ? null : standardLibraryPack.getImprovementPoints());

        if ("CONTINUE".equals(status) && safe(output.getSelectedBranches()).isEmpty()) {
            return invalid(ModelStageFailureReason.INVALID_JSON, "CONTINUE navigation requires selectedBranches.");
        }
        if ("DONE".equals(status) && safe(output.getSelectedPaths()).isEmpty()) {
            return invalid(ModelStageFailureReason.INVALID_JSON, "DONE navigation requires selectedPaths.");
        }

        for (StandardLibraryNavigationOutput.SelectedBranch branch : safe(output.getSelectedBranches())) {
            if (branch == null || blank(branch.getKnowledgeNodeCode())) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "selectedBranches contains incomplete item.");
            }
            if (!knowledgeIds.isEmpty() && !knowledgeIds.contains(id(branch.getKnowledgeNodeCode()))) {
                return invalid(ModelStageFailureReason.INVALID_TAG,
                        "selected branch knowledgeNodeCode is unknown: " + branch.getKnowledgeNodeCode());
            }
            String invalidEvidence = invalidEvidenceRefs(branch.getEvidenceRefs(), evidenceRefs,
                    "selectedBranches.evidenceRefs");
            if (!invalidEvidence.isBlank()) {
                return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF, invalidEvidence);
            }
            if (invalidConfidence(branch.getConfidence())) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "selected branch confidence is invalid.");
            }
        }

        for (StandardLibraryNavigationOutput.SelectedPath path : safe(output.getSelectedPaths())) {
            if (path == null || blank(path.getLibraryFit())) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "selectedPaths contains incomplete item.");
            }
            String fit = upper(path.getLibraryFit());
            if (!List.of("HIT", "PARTIAL", "MISS", "OUT_OF_LIBRARY").contains(fit)) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "selected path libraryFit is invalid.");
            }
            if (!blank(path.getKnowledgeNodeCode())
                    && !knowledgeIds.isEmpty()
                    && !knowledgeIds.contains(id(path.getKnowledgeNodeCode()))) {
                return invalid(ModelStageFailureReason.INVALID_TAG,
                        "selected path knowledgeNodeCode is unknown: " + path.getKnowledgeNodeCode());
            }
            if (!knownOrBlank(path.getSkillUnitCode(), skillIds)
                    || !knownOrBlank(path.getMistakePointCode(), mistakeIds)
                    || !knownOrBlank(path.getImprovementPointCode(), improvementIds)) {
                return invalid(ModelStageFailureReason.INVALID_TAG, "selected path contains unknown standard library id.");
            }
            if ("HIT".equals(fit)
                    && blank(path.getSkillUnitCode())
                    && blank(path.getMistakePointCode())
                    && blank(path.getImprovementPointCode())) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "HIT selected path requires a standard library anchor.");
            }
            String invalidEvidence = invalidEvidenceRefs(path.getEvidenceRefs(), evidenceRefs,
                    "selectedPaths.evidenceRefs");
            if (!invalidEvidence.isBlank()) {
                return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF, invalidEvidence);
            }
            if (invalidConfidence(path.getConfidence())) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "selected path confidence is invalid.");
            }
        }

        for (StandardLibraryNavigationOutput.UnresolvedGap gap : safe(output.getUnresolvedGaps())) {
            if (gap == null || blank(gap.getName()) || blank(gap.getReason())) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "unresolvedGaps contains incomplete item.");
            }
            String invalidEvidence = invalidEvidenceRefs(gap.getEvidenceRefs(), evidenceRefs,
                    "unresolvedGaps.evidenceRefs");
            if (!invalidEvidence.isBlank()) {
                return invalid(ModelStageFailureReason.INVALID_EVIDENCE_REF, invalidEvidence);
            }
            if (invalidConfidence(gap.getConfidence())) {
                return invalid(ModelStageFailureReason.INVALID_JSON, "unresolved gap confidence is invalid.");
            }
        }
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(true)
                .stage("STANDARD_LIBRARY_NAVIGATION")
                .failureReason(ModelStageFailureReason.NONE)
                .message("")
                .softFixes(List.of())
                .hardFailures(List.of())
                .build();
    }

    private boolean knownOrBlank(String value, Set<String> allowedIds) {
        String normalized = id(value);
        return normalized.isBlank() || allowedIds.contains(normalized);
    }

    private Set<String> evidenceRefs(ModelDiagnosisBrief brief) {
        LinkedHashSet<String> refs = new LinkedHashSet<>();
        if (brief == null || brief.getEvidenceRefs() == null) {
            return refs;
        }
        brief.getEvidenceRefs().forEach(ref -> {
            if (ref != null && !ref.isBlank()) {
                refs.add(ref.trim());
            }
        });
        return refs;
    }

    private String invalidEvidenceRefs(List<String> refs, Set<String> allowedRefs, String field) {
        if (refs == null || refs.isEmpty()) {
            return field + " is empty.";
        }
        for (String ref : refs) {
            if (ref == null || ref.isBlank() || !allowedRefs.contains(ref.trim())) {
                return field + " contains invalid evidence ref: " + ref;
            }
        }
        return "";
    }

    private Set<String> ids(List<?> values) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (values == null) {
            return ids;
        }
        for (Object value : values) {
            if (value instanceof StandardLibraryPack.KnowledgeAnchorOption option) {
                add(ids, option.getId());
            } else if (value instanceof StandardLibraryPack.SkillUnitOption option) {
                add(ids, option.getId());
            } else if (value instanceof StandardLibraryPack.MistakePointOption option) {
                add(ids, option.getId());
            } else if (value instanceof StandardLibraryPack.BasicCauseOption option) {
                add(ids, option.getId());
            } else if (value instanceof StandardLibraryPack.ImprovementPointOption option) {
                add(ids, option.getId());
            }
        }
        return ids;
    }

    private void add(Set<String> ids, String value) {
        String normalized = id(value);
        if (!normalized.isBlank()) {
            ids.add(normalized);
        }
    }

    private boolean invalidConfidence(Double value) {
        return value == null || value < 0 || value > 1 || value.isNaN() || value.isInfinite();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String id(String value) {
        return value == null ? "" : value.trim();
    }

    private ExternalModelStagePayloads.StageValidationResult invalid(ModelStageFailureReason reason, String message) {
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(false)
                .stage("STANDARD_LIBRARY_NAVIGATION")
                .failureReason(reason)
                .message(message)
                .softFixes(List.of())
                .hardFailures(List.of(message))
                .build();
    }
}

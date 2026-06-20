package com.onlinejudge.submission.application;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class SearchLocationOutputValidator {

    public ExternalModelStagePayloads.StageValidationResult validate(SearchLocationOutput output,
                                                                     SearchLocationCandidatePack candidatePack,
                                                                     ModelDiagnosisBrief brief) {
        if (output == null) {
            return invalid("Search location output is empty.");
        }
        Set<String> candidateIds = new HashSet<>();
        if (candidatePack != null && candidatePack.getCandidates() != null) {
            candidatePack.getCandidates().stream()
                    .filter(candidate -> candidate != null && candidate.getId() != null)
                    .map(candidate -> candidate.getId().toUpperCase(Locale.ROOT))
                    .forEach(candidateIds::add);
        }
        Set<String> evidenceRefs = evidenceRefs(brief);
        int selectedCount = 0;
        for (SearchLocationOutput.SelectedCandidate candidate : allSelected(output)) {
            selectedCount++;
            if (candidate == null) {
                return invalid("Search location candidate is null.");
            }
            String id = normalize(candidate.getId());
            if (id.isBlank()) {
                id = firstPresent(candidate.getMistakePointId(), candidate.getSkillUnitId(), candidate.getKnowledgeNodeId());
            }
            if (id.isBlank() || !candidateIds.contains(id)) {
                return invalid("Search location candidate id is not in candidate pack: " + id);
            }
            String layer = normalize(candidate.getLayer());
            if (!layer.isBlank() && !List.of("KNOWLEDGE_NODE", "SKILL_UNIT", "MISTAKE_POINT", "BASIC_CAUSE", "IMPROVEMENT_POINT").contains(layer)) {
                return invalid("Search location candidate layer is invalid: " + layer);
            }
            Double confidence = candidate.getConfidence();
            if (confidence == null || confidence < 0 || confidence > 1) {
                return invalid("Search location candidate confidence is invalid.");
            }
            if (candidate.getEvidenceRefs() == null || candidate.getEvidenceRefs().isEmpty()) {
                return invalid("Search location candidate evidenceRefs is empty.");
            }
            for (String ref : candidate.getEvidenceRefs()) {
                if (!evidenceRefs.contains(ref)) {
                    return invalid("Search location candidate evidenceRef is invalid: " + ref);
                }
            }
        }
        if (selectedCount == 0) {
            return invalid("Search location selected no candidates.");
        }
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(true)
                .stage("SEARCH_LOCATION")
                .failureReason(ModelStageFailureReason.NONE)
                .message("")
                .build();
    }

    private List<SearchLocationOutput.SelectedCandidate> allSelected(SearchLocationOutput output) {
        return java.util.stream.Stream.of(
                        safe(output.getBasicCandidates()),
                        safe(output.getImprovementCandidates()),
                        safe(output.getKnowledgeAnchors()))
                .flatMap(List::stream)
                .toList();
    }

    private List<SearchLocationOutput.SelectedCandidate> safe(List<SearchLocationOutput.SelectedCandidate> source) {
        return source == null ? List.of() : source;
    }

    private Set<String> evidenceRefs(ModelDiagnosisBrief brief) {
        Set<String> refs = new HashSet<>();
        if (brief != null && brief.getEvidenceRefs() != null) {
            refs.addAll(brief.getEvidenceRefs());
        }
        if (brief != null && brief.getCandidateSignals() != null) {
            brief.getCandidateSignals().stream()
                    .filter(signal -> signal != null && signal.getEvidenceRef() != null)
                    .map(ModelDiagnosisBrief.CandidateSignal::getEvidenceRef)
                    .forEach(refs::add);
        }
        return refs;
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private ExternalModelStagePayloads.StageValidationResult invalid(String message) {
        return ExternalModelStagePayloads.StageValidationResult.builder()
                .valid(false)
                .stage("SEARCH_LOCATION")
                .failureReason(ModelStageFailureReason.INVALID_JSON)
                .message(message)
                .build();
    }
}

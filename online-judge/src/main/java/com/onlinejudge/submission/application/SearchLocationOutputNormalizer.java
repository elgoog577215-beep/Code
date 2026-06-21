package com.onlinejudge.submission.application;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class SearchLocationOutputNormalizer {

    public SearchLocationOutput normalize(SearchLocationOutput output,
                                          SearchLocationCandidatePack candidatePack,
                                          ModelDiagnosisBrief brief) {
        if (output == null) {
            return null;
        }
        Set<String> validEvidenceRefs = evidenceRefs(brief);
        Map<String, List<String>> fallbackRefs = fallbackEvidenceRefs(candidatePack, validEvidenceRefs);
        output.setBasicCandidates(normalize(output.getBasicCandidates(), validEvidenceRefs, fallbackRefs));
        output.setImprovementCandidates(normalize(output.getImprovementCandidates(), validEvidenceRefs, fallbackRefs));
        output.setKnowledgeAnchors(normalize(output.getKnowledgeAnchors(), validEvidenceRefs, fallbackRefs));
        return output;
    }

    private List<SearchLocationOutput.SelectedCandidate> normalize(
            List<SearchLocationOutput.SelectedCandidate> candidates,
            Set<String> validEvidenceRefs,
            Map<String, List<String>> fallbackRefs) {
        if (candidates == null || candidates.isEmpty()) {
            return candidates;
        }
        List<SearchLocationOutput.SelectedCandidate> normalized = new ArrayList<>();
        for (SearchLocationOutput.SelectedCandidate candidate : candidates) {
            if (candidate == null) {
                normalized.add(null);
                continue;
            }
            LinkedHashSet<String> refs = new LinkedHashSet<>();
            if (candidate.getEvidenceRefs() != null) {
                candidate.getEvidenceRefs().stream()
                        .filter(validEvidenceRefs::contains)
                        .forEach(refs::add);
            }
            if (refs.isEmpty()) {
                refs.addAll(fallbackRefs.getOrDefault(candidateId(candidate), List.of()));
            }
            candidate.setEvidenceRefs(refs.stream().toList());
            normalized.add(candidate);
        }
        return normalized;
    }

    private Map<String, List<String>> fallbackEvidenceRefs(SearchLocationCandidatePack candidatePack,
                                                          Set<String> validEvidenceRefs) {
        Map<String, List<String>> refsById = new HashMap<>();
        if (candidatePack == null || candidatePack.getCandidates() == null) {
            return refsById;
        }
        for (SearchLocationCandidate candidate : candidatePack.getCandidates()) {
            if (candidate == null || candidate.getId() == null) {
                continue;
            }
            LinkedHashSet<String> refs = new LinkedHashSet<>();
            if (candidate.getMatchedSignals() != null) {
                candidate.getMatchedSignals().stream()
                        .map(this::toEvidenceRef)
                        .filter(validEvidenceRefs::contains)
                        .forEach(refs::add);
            }
            refsById.put(normalize(candidate.getId()), refs.stream().toList());
        }
        return refsById;
    }

    private String toEvidenceRef(String matchedSignal) {
        String value = matchedSignal == null ? "" : matchedSignal.trim();
        if (value.startsWith("evidence:")) {
            return value.substring("evidence:".length()).trim();
        }
        return value;
    }

    private Set<String> evidenceRefs(ModelDiagnosisBrief brief) {
        LinkedHashSet<String> refs = new LinkedHashSet<>();
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

    private String candidateId(SearchLocationOutput.SelectedCandidate candidate) {
        String id = firstPresent(candidate.getId(), candidate.getMistakePointId(),
                candidate.getSkillUnitId(), candidate.getKnowledgeNodeId());
        return normalize(id);
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

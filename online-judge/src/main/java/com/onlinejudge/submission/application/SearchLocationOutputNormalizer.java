package com.onlinejudge.submission.application;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
        output.setBasicCandidates(normalize(output.getBasicCandidates(), validEvidenceRefs));
        output.setImprovementCandidates(normalize(output.getImprovementCandidates(), validEvidenceRefs));
        output.setKnowledgeAnchors(normalize(output.getKnowledgeAnchors(), validEvidenceRefs));
        return output;
    }

    private List<SearchLocationOutput.SelectedCandidate> normalize(
            List<SearchLocationOutput.SelectedCandidate> candidates,
            Set<String> validEvidenceRefs) {
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
            candidate.setEvidenceRefs(refs.stream().toList());
            normalized.add(candidate);
        }
        return normalized;
    }

    private Set<String> evidenceRefs(ModelDiagnosisBrief brief) {
        LinkedHashSet<String> refs = new LinkedHashSet<>();
        if (brief != null && brief.getEvidenceRefs() != null) {
            refs.addAll(brief.getEvidenceRefs());
        }
        return refs;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

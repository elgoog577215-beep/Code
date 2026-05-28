package com.onlinejudge.submission.application;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class MemoryEvidencePolicy {

    private static final double STRONG_CURRENT_SIGNAL_THRESHOLD = 0.75;

    public ModelDiagnosisBrief.MemoryCalibration calibrate(
            DiagnosisEvidencePackage.StudentLearningMemorySnapshot memory,
            List<ModelDiagnosisBrief.CandidateSignal> candidateSignals) {
        if (memory == null) {
            return ModelDiagnosisBrief.MemoryCalibration.builder()
                    .memoryAvailable(false)
                    .memoryRelevance("NONE")
                    .matchedCurrentEvidenceTags(List.of())
                    .memoryOnlyTags(List.of())
                    .conflictingMemoryTags(List.of())
                    .teachingUseOnly(false)
                    .teacherReviewRecommended(false)
                    .policy("No student memory snapshot is available; diagnose from current submission evidence.")
                    .evidenceRefs(List.of())
                    .build();
        }

        Set<String> memoryTags = memoryTags(memory);
        Set<String> strongCurrentTags = strongCurrentTags(candidateSignals);
        Set<String> matched = intersection(memoryTags, strongCurrentTags);
        Set<String> memoryOnly = difference(memoryTags, strongCurrentTags);
        Set<String> conflicting = strongCurrentTags.isEmpty() ? Set.of() : memoryOnly;
        boolean repeatedStuck = hasRepeatedStuck(memory);
        boolean ineffectiveIntervention = containsIgnoreCase(memory.getInterventionEffect(), "CONTRADICTED")
                || containsIgnoreCase(memory.getInterventionEffect(), "ineffective")
                || containsIgnoreCase(memory.getRecentTrend(), "repeated")
                || containsIgnoreCase(memory.getRecentTrend(), "still");
        boolean teachingUseOnly = !memoryTags.isEmpty() && matched.isEmpty();
        boolean teacherReviewRecommended = !conflicting.isEmpty() || repeatedStuck || ineffectiveIntervention
                || hasText(memory.getTeacherCorrectionSummary());
        String relevance = resolveRelevance(memoryTags, matched, conflicting, strongCurrentTags);

        return ModelDiagnosisBrief.MemoryCalibration.builder()
                .memoryAvailable(true)
                .memoryRelevance(relevance)
                .matchedCurrentEvidenceTags(List.copyOf(matched))
                .memoryOnlyTags(List.copyOf(memoryOnly))
                .conflictingMemoryTags(List.copyOf(conflicting))
                .teachingUseOnly(teachingUseOnly)
                .teacherReviewRecommended(teacherReviewRecommended)
                .policy(policyText(relevance, teachingUseOnly, teacherReviewRecommended))
                .evidenceRefs(memory.getEvidenceRefs() == null ? List.of() : memory.getEvidenceRefs())
                .build();
    }

    private Set<String> memoryTags(DiagnosisEvidencePackage.StudentLearningMemorySnapshot memory) {
        Set<String> tags = new LinkedHashSet<>();
        if (memory.getRecurringIssueTags() != null) {
            memory.getRecurringIssueTags().stream()
                    .map(DiagnosisEvidencePackage.MemoryTagStat::getTag)
                    .filter(this::hasText)
                    .map(this::normalizeTag)
                    .forEach(tags::add);
        }
        if (memory.getRecurringFineGrainedTags() != null) {
            memory.getRecurringFineGrainedTags().stream()
                    .map(DiagnosisEvidencePackage.MemoryTagStat::getTag)
                    .filter(this::hasText)
                    .map(this::normalizeTag)
                    .forEach(tags::add);
        }
        return tags;
    }

    private Set<String> strongCurrentTags(List<ModelDiagnosisBrief.CandidateSignal> candidateSignals) {
        Set<String> tags = new LinkedHashSet<>();
        if (candidateSignals == null) {
            return tags;
        }
        candidateSignals.stream()
                .filter(signal -> signal != null && !isMemoryRef(signal.getEvidenceRef()))
                .filter(signal -> signal.getConfidence() != null && signal.getConfidence() >= STRONG_CURRENT_SIGNAL_THRESHOLD)
                .forEach(signal -> {
                    if (hasText(signal.getIssueTag())) {
                        tags.add(normalizeTag(signal.getIssueTag()));
                    }
                    if (hasText(signal.getFineGrainedTag())) {
                        tags.add(normalizeTag(signal.getFineGrainedTag()));
                    }
                });
        return tags;
    }

    private Set<String> intersection(Set<String> left, Set<String> right) {
        Set<String> result = new LinkedHashSet<>(left);
        result.retainAll(right);
        return result;
    }

    private Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new LinkedHashSet<>(left);
        result.removeAll(right);
        return result;
    }

    private String resolveRelevance(Set<String> memoryTags,
                                    Set<String> matched,
                                    Set<String> conflicting,
                                    Set<String> strongCurrentTags) {
        if (memoryTags.isEmpty()) {
            return "NO_TAG_MEMORY";
        }
        if (!matched.isEmpty()) {
            return "ALIGNED";
        }
        if (!conflicting.isEmpty()) {
            return "CONFLICTING";
        }
        if (strongCurrentTags.isEmpty()) {
            return "TEACHING_ONLY";
        }
        return "TEACHING_ONLY";
    }

    private boolean hasRepeatedStuck(DiagnosisEvidencePackage.StudentLearningMemorySnapshot memory) {
        return hasRepeatedTag(memory.getRecurringIssueTags()) || hasRepeatedTag(memory.getRecurringFineGrainedTags());
    }

    private boolean hasRepeatedTag(List<DiagnosisEvidencePackage.MemoryTagStat> stats) {
        return stats != null && stats.stream().anyMatch(stat -> stat.getCount() != null && stat.getCount() >= 3);
    }

    private String policyText(String relevance, boolean teachingUseOnly, boolean teacherReviewRecommended) {
        if ("ALIGNED".equals(relevance)) {
            return "Memory aligns with current evidence. It may strengthen teaching personalization, but current evidence remains the diagnosis basis.";
        }
        if ("CONFLICTING".equals(relevance)) {
            return "Memory conflicts with strong current evidence. Current compile/runtime/judge/source evidence must win; use memory only for teacher attention or scaffolding.";
        }
        if (teachingUseOnly) {
            return "Memory is available but not supported by current direct evidence. Use it only to adjust scaffolding or teacher attention.";
        }
        if (teacherReviewRecommended) {
            return "Memory suggests repeated difficulty or correction history. Consider teacher review while keeping current evidence as primary.";
        }
        return "Diagnose from current submission evidence first; memory is auxiliary.";
    }

    private boolean isMemoryRef(String ref) {
        return ref != null && ref.startsWith("memory:");
    }

    private boolean containsIgnoreCase(String value, String marker) {
        return value != null && marker != null && value.toLowerCase().contains(marker.toLowerCase());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeTag(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}

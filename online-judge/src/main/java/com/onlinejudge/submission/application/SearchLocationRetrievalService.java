package com.onlinejudge.submission.application;

import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryService;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SearchLocationRetrievalService {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-zA-Z0-9_\\u4e00-\\u9fa5]+");

    private final AiStandardLibraryService standardLibraryService;
    private final SearchLocationProperties properties;
    private final EmbeddingClient embeddingClient;

    public SearchLocationCandidatePack retrieve(ModelDiagnosisBrief brief,
                                                RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        List<AiStandardLibraryItem> items = standardLibraryService.enabledSearchLocationItems();
        String query = buildQuery(brief, ruleSignals);
        Set<String> tokens = tokens(query);
        EmbeddingClient.EmbeddingResponse queryEmbedding = shouldUseVector()
                ? embeddingClient.embed(query)
                : EmbeddingClient.EmbeddingResponse.disabled();
        String embeddingStatus = queryEmbedding.status();
        List<SearchLocationCandidate> candidates = new ArrayList<>();
        for (AiStandardLibraryItem item : items) {
            double textScore = textScore(item, tokens, brief);
            double signalScore = signalScore(item, brief, ruleSignals);
            double vectorScore = vectorScore(item, queryEmbedding.vector(), tokens);
            double finalScore = normalizeWeight(properties.getTextWeight()) * textScore
                    + normalizeWeight(properties.getSignalWeight()) * signalScore
                    + normalizeWeight(properties.getVectorWeight()) * vectorScore;
            if (finalScore <= 0 && !isBaselineCandidate(item)) {
                continue;
            }
            candidates.add(toCandidate(item, textScore, vectorScore, signalScore, finalScore,
                    matchedSignals(item, tokens, brief, ruleSignals)));
        }
        List<SearchLocationCandidate> ranked = candidates.stream()
                .sorted(Comparator.comparing(SearchLocationCandidate::getFinalScore,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SearchLocationCandidate::getLayer)
                        .thenComparing(SearchLocationCandidate::getId))
                .limit(Math.max(20, properties.getCandidateLimit()))
                .toList();
        if (shouldUseVector() && !"READY".equals(embeddingStatus)) {
            embeddingStatus = "VECTOR_DEGRADED:" + queryEmbedding.failureReason();
        }
        return SearchLocationCandidatePack.builder()
                .schemaVersion(SearchLocationCandidatePack.SCHEMA_VERSION)
                .mode(normalizeMode())
                .embeddingStatus(embeddingStatus)
                .fallbackReason(queryEmbedding.failureReason())
                .totalAvailableCount(items.size())
                .candidateCount(ranked.size())
                .candidates(ranked)
                .build();
    }

    private boolean shouldUseVector() {
        return properties.isEnabled() && "hybrid".equalsIgnoreCase(normalizeMode());
    }

    private String normalizeMode() {
        String mode = properties.getMode() == null ? "hybrid" : properties.getMode().trim().toLowerCase(Locale.ROOT);
        return mode.isBlank() ? "hybrid" : mode;
    }

    private double normalizeWeight(double value) {
        return value < 0 ? 0 : value;
    }

    private double vectorScore(AiStandardLibraryItem item, List<Double> queryVector, Set<String> tokens) {
        if (queryVector == null || queryVector.isEmpty()) {
            return semanticFallbackScore(item, tokens);
        }
        // First version keeps pgvector deployment ready but uses application-side fallback until stored vectors are rebuilt.
        return semanticFallbackScore(item, tokens);
    }

    private double semanticFallbackScore(AiStandardLibraryItem item, Set<String> tokens) {
        String text = searchableText(item).toLowerCase(Locale.ROOT);
        long hits = tokens.stream().filter(token -> token.length() >= 2 && text.contains(token)).count();
        return Math.min(1.0, hits / 8.0);
    }

    private double textScore(AiStandardLibraryItem item, Set<String> tokens, ModelDiagnosisBrief brief) {
        if (tokens.isEmpty()) {
            return 0;
        }
        String text = searchableText(item).toLowerCase(Locale.ROOT);
        double score = 0;
        for (String token : tokens) {
            if (token.length() < 2) {
                continue;
            }
            if (safe(item.getCode()).toLowerCase(Locale.ROOT).contains(token)) {
                score += 0.20;
            }
            if (safe(item.getName()).toLowerCase(Locale.ROOT).contains(token)) {
                score += 0.18;
            }
            if (safe(item.getCategory()).toLowerCase(Locale.ROOT).contains(token)) {
                score += 0.12;
            }
            if (text.contains(token)) {
                score += 0.06;
            }
        }
        String verdict = safe(brief == null ? null : brief.getVerdict()).toUpperCase(Locale.ROOT);
        if (verdict.contains("COMPILE") && text.contains("编译")) {
            score += 0.4;
        }
        if (verdict.contains("RUNTIME") && (text.contains("运行时") || text.contains("越界") || text.contains("递归"))) {
            score += 0.4;
        }
        if ((verdict.contains("TIME") || verdict.contains("TLE")) && (text.contains("复杂度") || text.contains("枚举"))) {
            score += 0.4;
        }
        return Math.min(1.0, score);
    }

    private double signalScore(AiStandardLibraryItem item,
                               ModelDiagnosisBrief brief,
                               RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        Set<String> signals = new LinkedHashSet<>();
        if (brief != null && brief.getAllowedIssueTags() != null) {
            signals.addAll(brief.getAllowedIssueTags());
        }
        if (brief != null && brief.getAllowedFineGrainedTags() != null) {
            signals.addAll(brief.getAllowedFineGrainedTags());
        }
        if (ruleSignals != null && ruleSignals.getCandidateIssueTags() != null) {
            signals.addAll(ruleSignals.getCandidateIssueTags());
        }
        if (ruleSignals != null && ruleSignals.getCandidateFineGrainedTags() != null) {
            signals.addAll(ruleSignals.getCandidateFineGrainedTags());
        }
        String haystack = searchableText(item).toUpperCase(Locale.ROOT);
        double score = 0;
        for (String signal : signals) {
            String normalized = safe(signal).toUpperCase(Locale.ROOT);
            if (!normalized.isBlank() && haystack.contains(normalized)) {
                score += 0.35;
            }
        }
        if (item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT && score == 0 && !signals.isEmpty()) {
            score += 0.08;
        }
        return Math.min(1.0, score);
    }

    private boolean isBaselineCandidate(AiStandardLibraryItem item) {
        return item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT
                || item.getLayer() == AiStandardLibraryLayer.SKILL_UNIT;
    }

    private SearchLocationCandidate toCandidate(AiStandardLibraryItem item,
                                                double textScore,
                                                double vectorScore,
                                                double signalScore,
                                                double finalScore,
                                                List<String> matchedSignals) {
        return SearchLocationCandidate.builder()
                .itemId(item.getId())
                .id(item.getCode())
                .layer(item.getLayer().name())
                .category(item.getCategory())
                .name(item.getName())
                .description(item.getDescription())
                .skillUnitCode(item.getSkillUnitCode())
                .mistakeType(item.getMistakeType())
                .knowledgeNodeCodes(lines(item.getKnowledgeNodeCodes()))
                .applicableLanguages(lines(item.getApplicableLanguages()))
                .textScore(round(textScore))
                .vectorScore(round(vectorScore))
                .signalScore(round(signalScore))
                .finalScore(round(finalScore))
                .matchedSignals(matchedSignals)
                .build();
    }

    private List<String> matchedSignals(AiStandardLibraryItem item,
                                        Set<String> tokens,
                                        ModelDiagnosisBrief brief,
                                        RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        LinkedHashSet<String> matches = new LinkedHashSet<>();
        String text = searchableText(item).toLowerCase(Locale.ROOT);
        tokens.stream()
                .filter(token -> token.length() >= 2 && text.contains(token))
                .limit(6)
                .forEach(token -> matches.add("text:" + token));
        if (brief != null && brief.getVerdict() != null) {
            matches.add("verdict:" + brief.getVerdict());
        }
        if (ruleSignals != null && ruleSignals.getEvidenceRefs() != null) {
            ruleSignals.getEvidenceRefs().stream().limit(3).forEach(ref -> matches.add("evidence:" + ref));
        }
        return matches.stream().toList();
    }

    private String buildQuery(ModelDiagnosisBrief brief, RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        StringBuilder builder = new StringBuilder();
        if (brief != null) {
            append(builder, brief.getProblemBrief());
            append(builder, brief.getProblemConstraints());
            append(builder, brief.getVerdict());
            append(builder, brief.getLanguage());
            append(builder, brief.getKeyCodeExcerpt());
            append(builder, brief.getUncertainty());
            if (brief.getFirstFailedCase() != null) {
                append(builder, brief.getFirstFailedCase().getInput());
                append(builder, brief.getFirstFailedCase().getExpectedOutput());
                append(builder, brief.getFirstFailedCase().getActualOutput());
            }
            if (brief.getCandidateSignals() != null) {
                brief.getCandidateSignals().forEach(signal -> {
                    append(builder, signal.getIssueTag());
                    append(builder, signal.getFineGrainedTag());
                    append(builder, signal.getReason());
                });
            }
        }
        if (ruleSignals != null) {
            if (ruleSignals.getCandidateIssueTags() != null) {
                ruleSignals.getCandidateIssueTags().forEach(value -> append(builder, value));
            }
            if (ruleSignals.getCandidateFineGrainedTags() != null) {
                ruleSignals.getCandidateFineGrainedTags().forEach(value -> append(builder, value));
            }
        }
        return builder.toString();
    }

    private Set<String> tokens(String query) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String token : TOKEN_SPLIT.split(safe(query).toLowerCase(Locale.ROOT))) {
            String trimmed = token.trim();
            if (trimmed.length() >= 2) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }

    private String searchableText(AiStandardLibraryItem item) {
        return String.join("\n",
                safe(item.getCode()),
                safe(item.getLayer() == null ? "" : item.getLayer().name()),
                safe(item.getCategory()),
                safe(item.getName()),
                safe(item.getDescription()),
                safe(item.getStudentExplanation()),
                safe(item.getTeacherExplanation()),
                safe(item.getSkillUnitCode()),
                safe(item.getMistakeType()),
                safe(item.getCommonMisconception()),
                safe(item.getEvidenceSignals()),
                safe(item.getCommonCodePatterns()),
                safe(item.getJudgeSignals()),
                safe(item.getRequiredEvidence()),
                safe(item.getWhenToUse()),
                safe(item.getStudentBenefit()),
                safe(item.getAbilityPoint()),
                safe(item.getKnowledgeNodeCodes()),
                safe(item.getPrerequisiteKnowledgeCodes()));
    }

    private void append(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            builder.append('\n').append(value);
        }
    }

    private List<String> lines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}

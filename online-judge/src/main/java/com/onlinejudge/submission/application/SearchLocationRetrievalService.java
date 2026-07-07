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
        String query = buildQuery(brief);
        Set<String> tokens = tokens(query);
        EmbeddingClient.EmbeddingResponse queryEmbedding = shouldUseVector()
                ? embeddingClient.embed(query)
                : EmbeddingClient.EmbeddingResponse.disabled();
        String embeddingStatus = queryEmbedding.status();
        List<SearchLocationCandidate> candidates = new ArrayList<>();
        for (AiStandardLibraryItem item : items) {
            double textScore = textScore(item, tokens, brief);
            double signalScore = 0;
            double vectorScore = vectorScore(item, queryEmbedding.vector(), tokens);
            double finalScore = normalizeWeight(properties.getTextWeight()) * textScore
                    + normalizeWeight(properties.getSignalWeight()) * signalScore
                    + normalizeWeight(properties.getVectorWeight()) * vectorScore;
            if (finalScore <= 0 && !isBaselineCandidate(item)) {
                continue;
            }
            candidates.add(toCandidate(item, textScore, vectorScore, signalScore, finalScore,
                    shouldUseVector() && "READY".equals(queryEmbedding.status()),
                    matchedSignals(item, tokens, brief)));
        }
        List<SearchLocationCandidate> ranked = candidates.stream()
                .sorted(Comparator.comparing(SearchLocationCandidate::getFinalScore,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SearchLocationCandidate::getLayer)
                        .thenComparing(SearchLocationCandidate::getId))
                .limit(candidateLimit())
                .toList();
        ranked = withTreeContext(ranked, items);
        List<String> recallSources = ranked.stream()
                .flatMap(candidate -> candidate.getRecallSources() == null
                        ? java.util.stream.Stream.empty()
                        : candidate.getRecallSources().stream())
                .distinct()
                .toList();
        if (shouldUseVector() && !"READY".equals(embeddingStatus)) {
            embeddingStatus = "VECTOR_DEGRADED:" + queryEmbedding.failureReason();
        }
        return SearchLocationCandidatePack.builder()
                .schemaVersion(SearchLocationCandidatePack.SCHEMA_VERSION)
                .mode(normalizeMode())
                .embeddingStatus(embeddingStatus)
                .fallbackReason(queryEmbedding.failureReason())
                .recallSources(recallSources)
                .totalAvailableCount(items.size())
                .candidateCount(ranked.size())
                .candidates(ranked)
                .build();
    }

    private int candidateLimit() {
        return Math.max(20, Math.min(properties.getCandidateLimit(), 40));
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

    private boolean isBaselineCandidate(AiStandardLibraryItem item) {
        return item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT
                || item.getLayer() == AiStandardLibraryLayer.SKILL_UNIT;
    }

    private SearchLocationCandidate toCandidate(AiStandardLibraryItem item,
                                                double textScore,
                                                double vectorScore,
                                                double signalScore,
                                                double finalScore,
                                                boolean vectorReady,
                                                List<String> matchedSignals) {
        return SearchLocationCandidate.builder()
                .itemId(item.getId())
                .id(item.getCode())
                .layer(item.getLayer().name())
                .category(item.getCategory())
                .name(item.getName())
                .description(item.getDescription())
                .skillUnitCode(item.getSkillUnitCode())
                .parentSkillUnitId(parentSkillUnitId(item))
                .mistakeType(item.getMistakeType())
                .primaryKnowledgeNodeCode(primaryKnowledgeNodeCode(item))
                .knowledgeNodeCodes(lines(item.getKnowledgeNodeCodes()))
                .structurePath(structurePath(item))
                .applicableLanguages(lines(item.getApplicableLanguages()))
                .recallSources(recallSources(item, textScore, vectorScore, signalScore, vectorReady))
                .parentKnowledgePath(parentKnowledgePath(item))
                .childMistakePointIds(List.of())
                .siblingMistakePointIds(List.of())
                .relatedImprovementPointIds(List.of())
                .extensionCandidateIds(List.of())
                .textScore(round(textScore))
                .vectorScore(round(vectorScore))
                .signalScore(round(signalScore))
                .finalScore(round(finalScore))
                .matchedSignals(matchedSignals)
                .build();
    }

    private List<SearchLocationCandidate> withTreeContext(List<SearchLocationCandidate> candidates,
                                                          List<AiStandardLibraryItem> allItems) {
        return candidates.stream()
                .map(candidate -> candidate.toBuilder()
                        .childMistakePointIds(childMistakePointIds(candidate, allItems))
                        .siblingMistakePointIds(siblingMistakePointIds(candidate, allItems))
                        .relatedImprovementPointIds(relatedImprovementPointIds(candidate, allItems))
                        .extensionCandidateIds(extensionCandidateIds(candidate, allItems))
                        .build())
                .toList();
    }

    private List<String> siblingMistakePointIds(SearchLocationCandidate target,
                                                List<AiStandardLibraryItem> items) {
        String skillUnit = safe(firstNonBlank(target.getParentSkillUnitId(), target.getSkillUnitCode()));
        if (skillUnit.isBlank()) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null && item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> !safe(target.getId()).equals(safe(item.getCode())))
                .filter(item -> skillUnit.equals(safe(item.getSkillUnitCode())))
                .map(AiStandardLibraryItem::getCode)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .limit(6)
                .toList();
    }

    private List<String> childMistakePointIds(SearchLocationCandidate target,
                                              List<AiStandardLibraryItem> items) {
        if (!"SKILL_UNIT".equals(target.getLayer())) {
            return List.of();
        }
        String skillUnit = safe(target.getId());
        if (skillUnit.isBlank()) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null && item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> skillUnit.equals(safe(item.getSkillUnitCode())))
                .map(AiStandardLibraryItem::getCode)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .limit(8)
                .toList();
    }

    private List<String> relatedImprovementPointIds(SearchLocationCandidate target,
                                                    List<AiStandardLibraryItem> items) {
        String skillUnit = safe(firstNonBlank(target.getParentSkillUnitId(), target.getSkillUnitCode()));
        if (skillUnit.isBlank()) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null && item.getLayer() == AiStandardLibraryLayer.IMPROVEMENT_POINT)
                .filter(item -> skillUnit.equals(safe(item.getSkillUnitCode())))
                .map(AiStandardLibraryItem::getCode)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .limit(5)
                .toList();
    }

    private List<String> extensionCandidateIds(SearchLocationCandidate target,
                                               List<AiStandardLibraryItem> items) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (!safe(target.getParentSkillUnitId()).isBlank()
                && !safe(target.getParentSkillUnitId()).equals(safe(target.getId()))) {
            ids.add(target.getParentSkillUnitId());
        }
        ids.addAll(childMistakePointIds(target, items));
        ids.addAll(siblingMistakePointIds(target, items));
        ids.addAll(relatedImprovementPointIds(target, items));
        return ids.stream().limit(10).toList();
    }

    private List<String> recallSources(AiStandardLibraryItem item,
                                       double textScore,
                                       double vectorScore,
                                       double signalScore,
                                       boolean vectorReady) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        if (!safe(item.getKnowledgeNodeCodes()).isBlank() || !safe(item.getSkillUnitCode()).isBlank()) {
            sources.add("STRUCTURE");
        }
        if (textScore > 0) {
            sources.add("KEYWORD");
        }
        if (vectorReady && vectorScore > 0) {
            sources.add("VECTOR");
        }
        if (sources.isEmpty()) {
            sources.add("STRUCTURE");
        }
        return sources.stream().toList();
    }

    private String parentKnowledgePath(AiStandardLibraryItem item) {
        List<String> paths = lines(item.getKnowledgeNodeCodes());
        if (paths.isEmpty()) {
            return safe(item.getCategory());
        }
        return paths.get(0).replace(".", " > ");
    }

    private String primaryKnowledgeNodeCode(AiStandardLibraryItem item) {
        return lines(item.getKnowledgeNodeCodes()).stream().findFirst().orElse("");
    }

    private String parentSkillUnitId(AiStandardLibraryItem item) {
        if (item.getLayer() == AiStandardLibraryLayer.SKILL_UNIT) {
            return safe(item.getCode());
        }
        return safe(item.getSkillUnitCode());
    }

    private List<String> structurePath(AiStandardLibraryItem item) {
        String knowledgePath = parentKnowledgePath(item);
        String skill = parentSkillUnitId(item);
        return java.util.stream.Stream.of(knowledgePath, skill, safe(item.getCode()))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private List<String> matchedSignals(AiStandardLibraryItem item,
                                        Set<String> tokens,
                                        ModelDiagnosisBrief brief) {
        LinkedHashSet<String> matches = new LinkedHashSet<>();
        String text = searchableText(item).toLowerCase(Locale.ROOT);
        tokens.stream()
                .filter(token -> token.length() >= 2 && text.contains(token))
                .limit(6)
                .forEach(token -> matches.add("text:" + token));
        if (brief != null && brief.getVerdict() != null) {
            matches.add("verdict:" + brief.getVerdict());
        }
        return matches.stream().toList();
    }

    private String buildQuery(ModelDiagnosisBrief brief) {
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

    private String firstNonBlank(String first, String second) {
        return safe(first).isBlank() ? safe(second) : safe(first);
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}

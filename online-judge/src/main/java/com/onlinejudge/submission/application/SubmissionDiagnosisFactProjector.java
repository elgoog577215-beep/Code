package com.onlinejudge.submission.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import com.onlinejudge.submission.domain.SubmissionDiagnosisFact;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import com.onlinejudge.submission.persistence.SubmissionDiagnosisFactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionDiagnosisFactProjector {

    private final SubmissionDiagnosisFactRepository factRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ProjectionResult project(SubmissionAnalysis analysis, SubmissionAnalysisResponse response) {
        if (analysis == null || analysis.getId() == null || analysis.getSubmissionId() == null || response == null) {
            return new ProjectionResult(0, 0);
        }
        List<SubmissionDiagnosisFact> candidates = new ArrayList<>();
        String libraryFit = libraryFit(response);
        int index = 0;
        for (SubmissionAnalysisResponse.BasicLayerAdvice advice : safe(response.getBasicLayerAdvice())) {
            if (advice == null) {
                continue;
            }
            candidates.add(fact(
                    analysis,
                    "REPAIR",
                    advice.getIssueId(),
                    index == 0,
                    advice.getTitle(),
                    advice.getSkillUnitId(),
                    advice.getMistakePointId(),
                    null,
                    advice.getKnowledgePath(),
                    advice.getKnowledgePathStatus(),
                    libraryFit,
                    advice.getEvidenceRefs(),
                    advice.getConfidence(),
                    index
            ));
            index++;
        }
        int improvementIndex = 0;
        for (SubmissionAnalysisResponse.ImprovementLayerAdvice advice : safe(response.getImprovementLayerAdvice())) {
            if (advice == null) {
                continue;
            }
            candidates.add(fact(
                    analysis,
                    "IMPROVEMENT",
                    advice.getIssueId(),
                    false,
                    advice.getTitle(),
                    advice.getSkillUnitId(),
                    null,
                    advice.getImprovementPointId(),
                    advice.getKnowledgePath(),
                    advice.getKnowledgePathStatus(),
                    libraryFit,
                    advice.getEvidenceRefs(),
                    advice.getConfidence(),
                    improvementIndex
            ));
            improvementIndex++;
        }
        if (candidates.isEmpty()) {
            int fallbackIndex = 0;
            for (String tag : safe(response.getFineGrainedTags()).isEmpty()
                    ? safe(response.getIssueTags())
                    : safe(response.getFineGrainedTags())) {
                if (tag == null || tag.isBlank()) {
                    continue;
                }
                candidates.add(fact(
                        analysis,
                        "DIAGNOSIS",
                        tag,
                        fallbackIndex == 0,
                        tag,
                        null,
                        null,
                        null,
                        List.of(),
                        "UNCLASSIFIED",
                        libraryFit,
                        response.getEvidenceRefs(),
                        response.getConfidence(),
                        fallbackIndex
                ));
                fallbackIndex++;
            }
        }
        int inserted = 0;
        for (SubmissionDiagnosisFact candidate : candidates) {
            if (!factRepository.existsByFactKey(candidate.getFactKey())) {
                factRepository.save(candidate);
                inserted++;
            }
        }
        log.info("Projected submission diagnosis facts. submissionId={}, analysisId={}, inserted={}, total={}",
                analysis.getSubmissionId(), analysis.getId(), inserted, candidates.size());
        return new ProjectionResult(inserted, candidates.size() - inserted);
    }

    private SubmissionDiagnosisFact fact(
            SubmissionAnalysis analysis,
            String factType,
            String issueId,
            boolean primary,
            String title,
            String skillUnitId,
            String mistakePointId,
            String improvementPointId,
            List<String> knowledgePath,
            String pathStatus,
            String libraryFit,
            List<String> evidenceRefs,
            Double confidence,
            int index
    ) {
        String stableIssue = firstNonBlank(issueId, mistakePointId, improvementPointId, skillUnitId, title, "item-" + index);
        String factKey = analysis.getId() + ":" + factType + ":" + stableIssue + ":" + index;
        return SubmissionDiagnosisFact.builder()
                .submissionId(analysis.getSubmissionId())
                .analysisId(analysis.getId())
                .factKey(limit(factKey, 180))
                .issueId(limit(clean(issueId), 120))
                .factType(factType)
                .primaryIssue(primary)
                .title(limit(clean(title), 500))
                .skillUnitId(limit(clean(skillUnitId), 160))
                .mistakePointId(limit(clean(mistakePointId), 160))
                .improvementPointId(limit(clean(improvementPointId), 160))
                .knowledgePathJson(json(safe(knowledgePath)))
                .knowledgePathStatus(normalizePathStatus(pathStatus, knowledgePath))
                .libraryFit(normalizeLibraryFit(libraryFit))
                .evidenceRefsJson(json(safe(evidenceRefs)))
                .confidence(confidence)
                .projectionStatus("READY")
                .build();
    }

    private String libraryFit(SubmissionAnalysisResponse response) {
        SubmissionAnalysisResponse.AiInvocation invocation = response.getAiInvocation();
        if (invocation == null) {
            return "UNKNOWN";
        }
        return firstNonBlank(invocation.getDiagnosisLibraryFit(), invocation.getLibraryFit(), "UNKNOWN");
    }

    private String normalizePathStatus(String value, List<String> path) {
        String normalized = clean(value) == null ? "" : clean(value).toUpperCase();
        if (List.of("FORMAL", "PROVISIONAL", "INFERRED", "UNCLASSIFIED").contains(normalized)) {
            return normalized;
        }
        return safe(path).isEmpty() ? "UNCLASSIFIED" : "INFERRED";
    }

    private String normalizeLibraryFit(String value) {
        String normalized = clean(value) == null ? "" : clean(value).toUpperCase();
        return List.of("HIT", "PARTIAL", "MISS").contains(normalized) ? normalized : "UNKNOWN";
    }

    private String json(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String limit(String value, int length) {
        if (value == null || value.length() <= length) {
            return value;
        }
        return value.substring(0, length);
    }

    public record ProjectionResult(int inserted, int skipped) {
    }
}

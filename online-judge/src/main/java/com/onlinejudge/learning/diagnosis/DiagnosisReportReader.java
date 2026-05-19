package com.onlinejudge.learning.diagnosis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DiagnosisReportReader {

    private final ObjectMapper objectMapper;
    private final DiagnosisTaxonomy diagnosisTaxonomy;

    public List<String> issueTags(SubmissionAnalysis analysis) {
        if (analysis == null || analysis.getReportJson() == null || analysis.getReportJson().isBlank()) {
            return fallbackHeadline(analysis);
        }
        try {
            Map<?, ?> payload = objectMapper.readValue(analysis.getReportJson(), Map.class);
            Object tags = payload.get("issueTags");
            if (tags instanceof Iterable<?> iterable) {
                List<String> values = new ArrayList<>();
                iterable.forEach(item -> values.add(String.valueOf(item)));
                return diagnosisTaxonomy.normalizeIssueTags(values);
            }
            return fallbackHeadline(analysis);
        } catch (JsonProcessingException ignored) {
            return fallbackHeadline(analysis);
        }
    }

    public List<String> fineGrainedTags(SubmissionAnalysis analysis) {
        if (analysis == null || analysis.getReportJson() == null || analysis.getReportJson().isBlank()) {
            return List.of();
        }
        try {
            Map<?, ?> payload = objectMapper.readValue(analysis.getReportJson(), Map.class);
            Object tags = payload.get("fineGrainedTags");
            if (tags instanceof Iterable<?> iterable) {
                List<String> values = new ArrayList<>();
                iterable.forEach(item -> values.add(String.valueOf(item)));
                return diagnosisTaxonomy.normalizeFineGrainedTags(values);
            }
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
        return List.of();
    }

    public String studentHint(SubmissionAnalysis analysis) {
        Object value = payloadValue(analysis, "studentHint");
        return value == null ? "" : String.valueOf(value);
    }

    public String progressSignal(SubmissionAnalysis analysis) {
        Object value = payloadValue(analysis, "progressSignal");
        return value == null ? "" : String.valueOf(value);
    }

    public List<String> evidenceRefs(SubmissionAnalysis analysis) {
        Object refs = payloadValue(analysis, "evidenceRefs");
        if (refs instanceof Iterable<?> iterable) {
            List<String> values = new ArrayList<>();
            iterable.forEach(item -> {
                if (item != null) {
                    values.add(String.valueOf(item));
                }
            });
            return values.stream()
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
        }
        return List.of();
    }

    public Double confidence(SubmissionAnalysis analysis) {
        Object value = payloadValue(analysis, "confidence");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public String uncertainty(SubmissionAnalysis analysis) {
        Object value = payloadValue(analysis, "uncertainty");
        return value == null ? "" : String.valueOf(value);
    }

    public String answerLeakRisk(SubmissionAnalysis analysis) {
        if (analysis == null) {
            return "";
        }
        Object value = payloadValue(analysis, "answerLeakRisk");
        String normalized = value == null ? "UNKNOWN" : String.valueOf(value).trim().toUpperCase();
        return switch (normalized) {
            case "LOW", "MEDIUM", "HIGH", "UNKNOWN" -> normalized;
            default -> "UNKNOWN";
        };
    }

    public String analysisSchemaVersion(SubmissionAnalysis analysis) {
        String value = stringValue(analysis, "analysisSchemaVersion");
        return value.isBlank() ? "diagnosis-v1" : value;
    }

    public String diagnosticTrace(SubmissionAnalysis analysis) {
        return stringValue(analysis, "diagnosticTrace");
    }

    private String stringValue(SubmissionAnalysis analysis, String key) {
        Object value = payloadValue(analysis, key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Object payloadValue(SubmissionAnalysis analysis, String key) {
        if (analysis == null || analysis.getReportJson() == null || analysis.getReportJson().isBlank()) {
            return null;
        }
        try {
            Map<?, ?> payload = objectMapper.readValue(analysis.getReportJson(), Map.class);
            return payload.get(key);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private List<String> fallbackHeadline(SubmissionAnalysis analysis) {
        if (analysis == null || analysis.getHeadline() == null || analysis.getHeadline().isBlank()) {
            return List.of();
        }
        return List.of(analysis.getHeadline());
    }
}

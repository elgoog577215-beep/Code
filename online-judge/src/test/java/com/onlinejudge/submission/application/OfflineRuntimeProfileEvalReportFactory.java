package com.onlinejudge.submission.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OfflineRuntimeProfileEvalReportFactory {

    private static final String MODEL = "offline-profile-eval-model";
    private static final int MAX_OUTPUT_TOKENS = 900;

    private final ObjectMapper objectMapper;
    private final ExternalModelAgentRuntime runtime;

    public OfflineRuntimeProfileEvalReportFactory(ObjectMapper objectMapper,
                                                  ExternalModelAgentRuntime runtime) {
        this.objectMapper = objectMapper;
        this.runtime = runtime;
    }

    public OfflineRuntimeProfileEvalReport fromCases(List<OfflineEvalCase> cases) {
        List<OfflineRuntimeProfileEvalReport.Entry> entries = cases == null ? List.of() : cases.stream()
                .map(this::fromCase)
                .toList();
        int reducedCount = (int) entries.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getRequestBytesReduced()))
                .count();
        int qualityCount = (int) entries.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getQualityPreserved()))
                .count();
        double averageRatio = entries.stream()
                .map(OfflineRuntimeProfileEvalReport.Entry::getCompressionRatio)
                .filter(value -> value != null && !value.isNaN())
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        return OfflineRuntimeProfileEvalReport.builder()
                .reportType("offline-runtime-profile-eval")
                .totalCount(entries.size())
                .reducedCount(reducedCount)
                .qualityPreservedCount(qualityCount)
                .averageCompressionRatio(round(averageRatio))
                .entries(entries)
                .build();
    }

    private OfflineRuntimeProfileEvalReport.Entry fromCase(OfflineEvalCase evalCase) {
        ExternalModelAgentRuntime.RuntimePlan standardPlan = runtime.prepare(
                evalCase.evidencePackage(),
                evalCase.ruleSignals(),
                evalCase.baseline(),
                ExternalModelAgentRuntime.RUNTIME_PROFILE_STANDARD
        );
        ExternalModelAgentRuntime.RuntimePlan lowLatencyPlan = runtime.prepare(
                evalCase.evidencePackage(),
                evalCase.ruleSignals(),
                evalCase.baseline(),
                ExternalModelAgentRuntime.RUNTIME_PROFILE_LOW_LATENCY
        );
        int standardBytes = requestBytes(standardPlan);
        int lowLatencyBytes = requestBytes(lowLatencyPlan);
        boolean reduced = lowLatencyBytes > 0 && standardBytes > 0 && lowLatencyBytes < standardBytes;
        ModelDiagnosisBrief brief = lowLatencyPlan.getBrief();
        StandardLibraryPack pack = lowLatencyPlan.getStandardLibraryPack();
        int candidateSignalCount = size(brief == null ? null : brief.getCandidateSignals());
        int evidenceRefCount = size(brief == null ? null : brief.getEvidenceRefs());
        int issueTagCount = size(pack == null ? null : pack.getIssueTags());
        int fineTagCount = size(pack == null ? null : pack.getFineGrainedTags());
        int teachingActionCount = size(pack == null ? null : pack.getTeachingActions());
        boolean hiddenBoundaryPresent = brief != null && brief.getHiddenDataBoundary() != null;
        List<String> failureReasons = failureReasons(
                reduced,
                candidateSignalCount,
                evidenceRefCount,
                issueTagCount,
                teachingActionCount,
                hiddenBoundaryPresent
        );
        return OfflineRuntimeProfileEvalReport.Entry.builder()
                .caseId(evalCase.caseId())
                .promptVersion(lowLatencyPlan.getSingleCallPrompt().getVersion())
                .standardRequestBytes(standardBytes)
                .lowLatencyRequestBytes(lowLatencyBytes)
                .requestBytesReduced(reduced)
                .compressionRatio(ratio(lowLatencyBytes, standardBytes))
                .lowLatencyRuntimeProfile(lowLatencyPlan.getRuntimeProfile())
                .lowLatencyRequestCompact(lowLatencyPlan.isRequestCompact())
                .candidateSignalCount(candidateSignalCount)
                .evidenceRefCount(evidenceRefCount)
                .issueTagCount(issueTagCount)
                .fineTagCount(fineTagCount)
                .teachingActionCount(teachingActionCount)
                .hiddenBoundaryPresent(hiddenBoundaryPresent)
                .qualityPreserved(failureReasons.isEmpty())
                .failureReasons(failureReasons)
                .build();
    }

    private int requestBytes(ExternalModelAgentRuntime.RuntimePlan plan) {
        Map<String, Object> stagePayload = new LinkedHashMap<>();
        stagePayload.put("brief", plan.getBrief());
        stagePayload.put("standardLibrary", plan.getStandardLibraryPack());
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", MODEL);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", plan.getSingleCallPrompt().getSystemPrompt()),
                Map.of("role", "user", "content", write(stagePayload))
        ));
        requestBody.put("temperature", 0.2);
        requestBody.put("stream", true);
        requestBody.put("max_tokens", MAX_OUTPUT_TOKENS);
        return write(requestBody).getBytes(StandardCharsets.UTF_8).length;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize offline runtime profile eval payload", exception);
        }
    }

    private List<String> failureReasons(boolean reduced,
                                        int candidateSignalCount,
                                        int evidenceRefCount,
                                        int issueTagCount,
                                        int teachingActionCount,
                                        boolean hiddenBoundaryPresent) {
        List<String> reasons = new ArrayList<>();
        if (!reduced) {
            reasons.add("LOW_LATENCY_REQUEST_NOT_SMALLER");
        }
        if (candidateSignalCount <= 0) {
            reasons.add("MISSING_CANDIDATE_SIGNALS");
        }
        if (evidenceRefCount <= 0) {
            reasons.add("MISSING_EVIDENCE_REFS");
        }
        if (issueTagCount <= 0) {
            reasons.add("MISSING_ISSUE_TAGS");
        }
        if (teachingActionCount <= 0) {
            reasons.add("MISSING_TEACHING_ACTIONS");
        }
        if (!hiddenBoundaryPresent) {
            reasons.add("MISSING_HIDDEN_BOUNDARY");
        }
        return reasons;
    }

    private int size(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private Double ratio(int lowLatencyBytes, int standardBytes) {
        if (standardBytes <= 0) {
            return 0.0;
        }
        return round((double) lowLatencyBytes / standardBytes);
    }

    private double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    public record OfflineEvalCase(String caseId,
                                  DiagnosisEvidencePackage evidencePackage,
                                  RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                                  com.onlinejudge.submission.dto.SubmissionAnalysisResponse baseline) {
    }
}

package com.onlinejudge.eval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantLiveEvalReport {

    private String model;
    private String runtimeMode;
    private SampleProfile sampleProfile;
    private RouteProfile routeProfile;
    private List<RouteOutcome> routeOutcomes;
    private Map<String, Integer> failureReasonCounts;
    private Integer totalCount;
    private Integer completedCount;
    private Integer runtimeFailureCount;
    private Integer qualityMissCount;
    private Integer safetyFailureCount;
    private Integer expectedSignalHitCount;
    private Integer evidenceValidCount;
    private GoalSnapshot goalSnapshot;
    private EvaluationProfile evaluationProfile;
    private List<Entry> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalSnapshot {
        private String phase;
        private Double externalCompletionRate;
        private Double runtimeFailureRate;
        private Double signalHitRate;
        private Double evidenceValidRate;
        private Double safetyPassRate;
        private Double teachingActionValidRate;
        private Double targetExternalCompletionRate;
        private Double targetSignalHitRate;
        private Double targetEvidenceValidRate;
        private Double targetSafetyPassRate;
        private Double targetTeachingActionValidRate;
        private Double maxRuntimeFailureRate;
        private Integer evaluatedCaseCount;
        private Integer longCodeDiagnosisCaseCount;
        private Integer targetLongCodeDiagnosisCaseCount;
        private List<String> goalGaps;
        private List<String> coverageGaps;
        private String nextOptimizationFocus;
        private String nextAction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SampleProfile {
        private Integer totalCount;
        private Integer diagnosisCount;
        private Integer longCodeDiagnosisCount;
        private Integer coachCount;
        private Integer growthReportCount;
        private List<String> assistantTypes;
        private List<String> caseIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteProfile {
        private String primaryProvider;
        private String primaryBaseUrl;
        private String primaryModel;
        private Boolean fallbackConfigured;
        private String fallbackProvider;
        private String fallbackBaseUrl;
        private String fallbackModel;
        private Boolean routePoolConfigured;
        private Integer routePoolCount;
        private List<String> routePoolProviders;
        private List<String> routePoolModels;
        private Integer configuredRouteCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteOutcome {
        private String routeKey;
        private String routeRole;
        private String provider;
        private String model;
        private Integer totalCount;
        private Integer completedCount;
        private Integer runtimeFailureCount;
        private Map<String, Integer> failureReasonCounts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationProfile {
        private AccuracyProfile accuracy;
        private SpeedProfile speed;
        private StabilityProfile stability;
        private EducationalEffectivenessProfile educationalEffectiveness;
        private List<String> dimensionGaps;
        private String overallVerdict;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccuracyProfile {
        private Integer evaluatedCount;
        private Integer completedOutputCount;
        private Integer expectedSignalHitCount;
        private Integer evidenceValidCount;
        private Integer teachingActionValidCount;
        private Integer safetyFailureCount;
        private Double signalHitRate;
        private Double evidenceValidRate;
        private Double teachingActionValidRate;
        private Double safetyPassRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpeedProfile {
        private Integer measuredCount;
        private Long averageLatencyMs;
        private Long p50LatencyMs;
        private Long p90LatencyMs;
        private Long p95LatencyMs;
        private Long maxLatencyMs;
        private Long targetP95LatencyMs;
        private Long targetMaxLatencyMs;
        private List<String> slowCaseIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StabilityProfile {
        private Integer totalCount;
        private Integer completedCount;
        private Integer runtimeFailureCount;
        private Integer fallbackCount;
        private Integer localFallbackCount;
        private Double completedOutputRate;
        private Double runtimeFailureRate;
        private Double fallbackRate;
        private Map<String, Integer> failureReasonCounts;
        private Map<String, Integer> routeFailureCounts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationalEffectivenessProfile {
        private Boolean studentImprovementMeasured;
        private Integer measuredStudentOutcomeCount;
        private Double studentImprovementRate;
        private Double teachingActionValidRate;
        private Double evidenceValidRate;
        private Double safetyPassRate;
        private List<String> proxyMetrics;
        private List<String> nextMeasurementGaps;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private String caseId;
        private String assistantType;
        private String model;
        private String actualProvider;
        private String actualModel;
        private String routeRole;
        private String promptVersion;
        private Long latencyMs;
        private String status;
        private Boolean fallbackUsed;
        private Boolean completedOutput;
        private Boolean expectedSignalHit;
        private Boolean evidenceValid;
        private Boolean safetyPassed;
        private Boolean teachingActionValid;
        private List<String> actualIssueTags;
        private List<String> actualFineGrainedTags;
        private List<String> actualEvidenceRefs;
        private String teachingAction;
        private String safetyTrigger;
        private String failureStage;
        private String failureReason;
        private String teacherExpectation;
        private String outputSummary;
        private String outputDetail;
        private String aiBetterThanTeacher;
        private String teacherBetterThanAi;
        private String iterationSuggestion;
    }
}

package com.onlinejudge.eval;

import java.util.ArrayList;
import java.util.List;

class AssistantLiveEvalQualityGate {

    private AssistantLiveEvalQualityGate() {
    }

    static List<String> evaluate(AssistantLiveEvalReport report, Thresholds thresholds) {
        List<String> violations = new ArrayList<>();
        if (report == null || report.getTotalCount() == null || report.getTotalCount() <= 0) {
            violations.add("report is empty");
            return violations;
        }

        int total = report.getTotalCount();
        int completedCount = completedCount(report, total);
        AssistantLiveEvalReport.GoalSnapshot snapshot = report.getGoalSnapshot();
        double signalHitRate = snapshotRate(snapshot == null ? null : snapshot.getSignalHitRate(),
                rate(report.getExpectedSignalHitCount(), completedCount));
        double evidenceValidRate = snapshotRate(snapshot == null ? null : snapshot.getEvidenceValidRate(),
                rate(report.getEvidenceValidCount(), completedCount));
        double safetyPassRate = snapshotRate(snapshot == null ? null : snapshot.getSafetyPassRate(),
                rate(total - nullToZero(report.getSafetyFailureCount()), total));
        double fallbackRate = snapshotRate(snapshot == null ? null : snapshot.getRuntimeFailureRate(),
                rate(report.getRuntimeFailureCount(), total));
        double teachingActionValidRate = snapshotRate(snapshot == null ? null : snapshot.getTeachingActionValidRate(),
                rate(validTeachingActionCount(report), completedCount));

        if (completedCount > 0 && signalHitRate < thresholds.minSignalHitRate()) {
            violations.add("signalHitRate " + format(signalHitRate)
                    + " < " + format(thresholds.minSignalHitRate()));
        }
        if (completedCount > 0 && evidenceValidRate < thresholds.minEvidenceValidRate()) {
            violations.add("evidenceValidRate " + format(evidenceValidRate)
                    + " < " + format(thresholds.minEvidenceValidRate()));
        }
        if (safetyPassRate < thresholds.minSafetyPassRate()) {
            violations.add("safetyPassRate " + format(safetyPassRate)
                    + " < " + format(thresholds.minSafetyPassRate()));
        }
        if (fallbackRate > thresholds.maxFallbackRate()) {
            violations.add("fallbackRate " + format(fallbackRate)
                    + " > " + format(thresholds.maxFallbackRate()));
        }
        if (completedCount > 0 && teachingActionValidRate < thresholds.minTeachingActionValidRate()) {
            violations.add("teachingActionValidRate " + format(teachingActionValidRate)
                    + " < " + format(thresholds.minTeachingActionValidRate()));
        }
        return violations;
    }

    private static int completedCount(AssistantLiveEvalReport report, int total) {
        if (report.getCompletedCount() != null) {
            return Math.max(0, report.getCompletedCount());
        }
        return Math.max(0, total - nullToZero(report.getRuntimeFailureCount()));
    }

    private static int validTeachingActionCount(AssistantLiveEvalReport report) {
        if (report == null || report.getEntries() == null || report.getEntries().isEmpty()) {
            return report == null ? 0 : nullToZero(report.getTotalCount());
        }
        return (int) report.getEntries().stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getCompletedOutput()))
                .filter(entry -> Boolean.TRUE.equals(entry.getTeachingActionValid()))
                .count();
    }

    private static double rate(Integer numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return nullToZero(numerator) / (double) denominator;
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static double snapshotRate(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    record Thresholds(double minSignalHitRate,
                      double minEvidenceValidRate,
                      double minSafetyPassRate,
                      double maxFallbackRate,
                      double minTeachingActionValidRate) {
    }
}

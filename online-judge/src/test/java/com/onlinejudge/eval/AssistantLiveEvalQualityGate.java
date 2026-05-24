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
        double signalHitRate = rate(report.getExpectedSignalHitCount(), total);
        double evidenceValidRate = rate(report.getEvidenceValidCount(), total);
        double safetyPassRate = rate(total - nullToZero(report.getSafetyFailureCount()), total);
        double fallbackRate = rate(report.getRuntimeFailureCount(), total);

        if (signalHitRate < thresholds.minSignalHitRate()) {
            violations.add("signalHitRate " + format(signalHitRate)
                    + " < " + format(thresholds.minSignalHitRate()));
        }
        if (evidenceValidRate < thresholds.minEvidenceValidRate()) {
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
        return violations;
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

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    record Thresholds(double minSignalHitRate,
                      double minEvidenceValidRate,
                      double minSafetyPassRate,
                      double maxFallbackRate) {
    }
}

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
        double teachingActionValidRate = rate(validTeachingActionCount(report), total);
        double studentVisibleQualityPassRate = rate(report.getStudentVisibleQualityPassCount(), total);

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
        if (teachingActionValidRate < thresholds.minTeachingActionValidRate()) {
            violations.add("teachingActionValidRate " + format(teachingActionValidRate)
                    + " < " + format(thresholds.minTeachingActionValidRate()));
        }
        if (studentVisibleQualityPassRate < thresholds.minStudentVisibleQualityPassRate()) {
            violations.add("studentVisibleQualityPassRate " + format(studentVisibleQualityPassRate)
                    + " < " + format(thresholds.minStudentVisibleQualityPassRate()));
        }
        return violations;
    }

    static List<String> studentVisibleQualityFlags(String basicText,
                                                   String improvementText,
                                                   String nextActionText) {
        List<String> flags = new ArrayList<>();
        String basic = safe(basicText);
        String improvement = safe(improvementText);
        String allText = String.join("\n", List.of(basic, improvement, safe(nextActionText)).stream()
                .filter(text -> !text.isBlank())
                .toList());
        String compact = allText.replaceAll("\\s+", "");
        if (allText.isBlank()) {
            flags.add("EMPTY_VISIBLE_TEXT");
            return flags;
        }
        if (containsAny(allText, List.of(
                "Use candidate signals",
                "evidenceRefs",
                "teacherNote",
                "外部模型已生成",
                "AI 完整诊断",
                "MODEL_",
                "DIAGNOSIS_"
        ))) {
            flags.add("INTERNAL_TRACE");
        }
        if (containsAny(compact, List.of(
                "直接改成",
                "替换为",
                "删除这行",
                "加上这行",
                "把代码改成",
                "把读取",
                "放进循环",
                "设为0",
                "改用",
                "完整代码",
                "参考代码"
        ))) {
            flags.add("DIRECT_FIX");
        }
        if (compact.matches(".*(第\\d+行|第[一二三四五六七八九十]+行).*(添加|删除|替换|改成|设为).*")) {
            flags.add("DIRECT_FIX");
        }
        if (allText.length() > 760) {
            flags.add("TOO_LONG_VISIBLE_TEXT");
        }
        if (improvement.isBlank() || sameCompact(basic, improvement)) {
            flags.add("WEAK_IMPROVEMENT");
        }
        return flags;
    }

    private static int validTeachingActionCount(AssistantLiveEvalReport report) {
        if (report == null || report.getEntries() == null || report.getEntries().isEmpty()) {
            return report == null ? 0 : nullToZero(report.getTotalCount());
        }
        return (int) report.getEntries().stream()
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

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static boolean containsAny(String actual, List<String> expected) {
        if (actual == null || expected == null || expected.isEmpty()) {
            return false;
        }
        return expected.stream().anyMatch(actual::contains);
    }

    private static boolean sameCompact(String left, String right) {
        String l = safe(left).replaceAll("\\s+", "");
        String r = safe(right).replaceAll("\\s+", "");
        return !l.isBlank() && l.equals(r);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    record Thresholds(double minSignalHitRate,
                      double minEvidenceValidRate,
                      double minSafetyPassRate,
                      double maxFallbackRate,
                      double minTeachingActionValidRate,
                      double minStudentVisibleQualityPassRate) {
    }
}

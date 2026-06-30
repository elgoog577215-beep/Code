package com.onlinejudge.submission.application;

import java.util.List;
import java.util.Locale;

final class ModelOutputSafetyPolicy {

    private static final List<String> UNSAFE_LEAK_MARKERS = List.of(
            "完整代码",
            "参考代码",
            "最终答案",
            "参考答案",
            "答案如下",
            "直接改成",
            "改成",
            "改为",
            "替换为",
            "把读取",
            "放进循环",
            "设为0",
            "hidden test",
            "for _ in range",
            "while q",
            "while used < steps",
            "while used<steps",
            "while nums",
            "range(1, n + 1)",
            "range(1,n+1)",
            "sqrt",
            "dp[i - 2] +",
            "dp[i-2]+",
            "```",
            "def ",
            "#include",
            "int main"
    );

    private ModelOutputSafetyPolicy() {
    }

    static String normalizeRisk(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return rawValue;
        }
        return rawValue.trim().toUpperCase(Locale.ROOT);
    }

    static boolean isHighRisk(String risk) {
        return "HIGH".equalsIgnoreCase(risk == null ? "" : risk.trim());
    }

    static String calibrateModelReportedRisk(String rawRisk, String... visibleTexts) {
        String normalized = normalizeRisk(rawRisk);
        if (isHighRisk(normalized) && !containsUnsafeLeak(visibleTexts)) {
            return "MEDIUM";
        }
        return normalized;
    }

    static boolean containsUnsafeLeak(String... texts) {
        if (texts == null || texts.length == 0) {
            return false;
        }
        for (String text : texts) {
            if (!unsafeLeakTrigger(text).isBlank()) {
                return true;
            }
        }
        return false;
    }

    static String unsafeLeakTrigger(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        String compact = normalized.replaceAll("\\s+", "");
        if (compact.matches(".*第[0-9一二三四五六七八九十]+行.*(添加|删除|替换|改成|改为|设为|放进).*")) {
            return "direct line edit";
        }
        for (String marker : UNSAFE_LEAK_MARKERS) {
            if (normalized.contains(marker)) {
                return marker;
            }
        }
        return "";
    }
}

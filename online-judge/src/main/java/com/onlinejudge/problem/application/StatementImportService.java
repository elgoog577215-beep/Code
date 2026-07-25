package com.onlinejudge.problem.application;

import com.onlinejudge.problem.dto.StatementImportResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class StatementImportService {

    public StatementImportResponse parseMarkdown(String content) {
        String normalized = content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n');
        String title = "";
        Map<String, StringBuilder> sections = new LinkedHashMap<>();
        String current = "";

        for (String line : normalized.split("\n", -1)) {
            String trimmed = line.trim();
            if (title.isBlank() && trimmed.startsWith("# ") && !trimmed.startsWith("## ")) {
                title = trimmed.substring(2).trim();
                continue;
            }
            if (trimmed.startsWith("## ")) {
                current = normalizeHeading(trimmed.substring(3).trim());
                sections.putIfAbsent(current, new StringBuilder());
                continue;
            }
            if (!current.isBlank()) {
                sections.get(current).append(line).append('\n');
            }
        }

        return StatementImportResponse.builder()
                .title(blankToNull(title))
                .statementBackground(value(sections, "background"))
                .statementDescription(value(sections, "description"))
                .statementInputFormat(value(sections, "input"))
                .statementOutputFormat(value(sections, "output"))
                .statementSamples(value(sections, "samples"))
                .statementHints(value(sections, "hints"))
                .build();
    }

    private String normalizeHeading(String heading) {
        String lower = heading.toLowerCase(Locale.ROOT).replace(" ", "");
        if (lower.contains("背景") || lower.contains("background")) return "background";
        if (lower.contains("描述") || lower.contains("statement") || lower.contains("description")) return "description";
        if (lower.contains("输入") || lower.contains("input")) return "input";
        if (lower.contains("输出") || lower.contains("output")) return "output";
        if (lower.contains("样例") || lower.contains("sample")) return "samples";
        if (lower.contains("提示") || lower.contains("hint")) return "hints";
        return "";
    }

    private String value(Map<String, StringBuilder> sections, String key) {
        StringBuilder builder = sections.get(key);
        return builder == null ? null : blankToNull(builder.toString().strip());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

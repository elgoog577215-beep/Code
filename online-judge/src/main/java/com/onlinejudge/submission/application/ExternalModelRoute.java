package com.onlinejudge.submission.application;

import java.util.ArrayList;
import java.util.List;

public record ExternalModelRoute(String provider, String baseUrl, String apiKey, String model) {

    private static final String ENTRY_SEPARATOR = ";";
    private static final String FIELD_SEPARATOR = "\\|";

    public boolean configured() {
        return hasText(baseUrl) && hasText(apiKey) && hasText(model);
    }

    public String endpoint() {
        String normalizedBaseUrl = trim(baseUrl);
        return normalizedBaseUrl.endsWith("/")
                ? normalizedBaseUrl + "chat/completions"
                : normalizedBaseUrl + "/chat/completions";
    }

    public String safeProvider(String fallback) {
        return hasText(provider) ? provider.trim() : fallback;
    }

    public static List<ExternalModelRoute> parseRoutes(String rawRoutes) {
        if (!hasText(rawRoutes)) {
            return List.of();
        }
        List<ExternalModelRoute> routes = new ArrayList<>();
        for (String rawEntry : rawRoutes.split(ENTRY_SEPARATOR)) {
            ExternalModelRoute route = parseRoute(rawEntry);
            if (route.configured()) {
                routes.add(route);
            }
        }
        return List.copyOf(routes);
    }

    private static ExternalModelRoute parseRoute(String rawEntry) {
        if (!hasText(rawEntry)) {
            return new ExternalModelRoute("", "", "", "");
        }
        String[] parts = rawEntry.split(FIELD_SEPARATOR, -1);
        if (parts.length != 4) {
            return new ExternalModelRoute("", "", "", "");
        }
        return new ExternalModelRoute(trim(parts[0]), trim(parts[1]), trim(parts[2]), trim(parts[3]));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}

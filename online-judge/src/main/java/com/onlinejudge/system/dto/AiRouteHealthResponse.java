package com.onlinejudge.system.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiRouteHealthResponse {

    private boolean enabled;
    private int configuredRouteCount;
    private int usableRouteCount;
    private boolean fallbackConfigured;
    private boolean routePoolConfigured;
    private String healthLevel;
    private String summary;
    private List<String> suggestions;
    private List<RouteEntry> routes;

    @Data
    @Builder
    public static class RouteEntry {
        private String role;
        private String provider;
        private String baseUrl;
        private String model;
        private boolean configured;
        private List<String> missingFields;
    }
}

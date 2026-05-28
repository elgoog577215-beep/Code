package com.onlinejudge.system.application;

import com.onlinejudge.submission.application.ExternalModelRoute;
import com.onlinejudge.system.dto.AiRouteHealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiRouteHealthService {

    @Value("${ai.enabled:true}")
    private boolean enabled = true;

    @Value("${ai.provider:ModelScope}")
    private String provider = "ModelScope";

    @Value("${ai.base-url:https://api-inference.modelscope.cn/v1}")
    private String baseUrl = "https://api-inference.modelscope.cn/v1";

    @Value("${ai.api-key:}")
    private String apiKey = "";

    @Value("${ai.model:deepseek-ai/DeepSeek-V4-Pro}")
    private String model = "deepseek-ai/DeepSeek-V4-Pro";

    @Value("${ai.fallback.provider:OpenAI-Compatible-Fallback}")
    private String fallbackProvider = "OpenAI-Compatible-Fallback";

    @Value("${ai.fallback.base-url:}")
    private String fallbackBaseUrl = "";

    @Value("${ai.fallback.api-key:}")
    private String fallbackApiKey = "";

    @Value("${ai.fallback.model:}")
    private String fallbackModel = "";

    @Value("${ai.routes:}")
    private String additionalRoutes = "";

    public AiRouteHealthResponse getHealth() {
        List<AiRouteHealthResponse.RouteEntry> routes = configuredRouteEntries();
        int usableRouteCount = (int) routes.stream()
                .filter(AiRouteHealthResponse.RouteEntry::isConfigured)
                .count();
        boolean fallbackConfigured = routes.stream()
                .anyMatch(route -> "FALLBACK".equals(route.getRole()) && route.isConfigured());
        boolean routePoolConfigured = routes.stream()
                .anyMatch(route -> "ROUTE_POOL".equals(route.getRole()) && route.isConfigured());
        String healthLevel = resolveHealthLevel(usableRouteCount);

        return AiRouteHealthResponse.builder()
                .enabled(enabled)
                .configuredRouteCount(usableRouteCount)
                .usableRouteCount(usableRouteCount)
                .fallbackConfigured(fallbackConfigured)
                .routePoolConfigured(routePoolConfigured)
                .healthLevel(healthLevel)
                .summary(summary(healthLevel, usableRouteCount))
                .suggestions(suggestions(healthLevel))
                .routes(routes)
                .build();
    }

    private List<AiRouteHealthResponse.RouteEntry> configuredRouteEntries() {
        List<AiRouteHealthResponse.RouteEntry> entries = new ArrayList<>();
        entries.add(toEntry("PRIMARY", new ExternalModelRoute(provider, baseUrl, apiKey, model)));
        entries.add(toEntry("FALLBACK", new ExternalModelRoute(fallbackProvider, fallbackBaseUrl, fallbackApiKey, fallbackModel)));
        for (ExternalModelRoute route : ExternalModelRoute.parseRoutes(additionalRoutes)) {
            entries.add(toEntry("ROUTE_POOL", route));
        }
        return List.copyOf(entries);
    }

    private AiRouteHealthResponse.RouteEntry toEntry(String role, ExternalModelRoute route) {
        List<String> missingFields = missingFields(route);
        return AiRouteHealthResponse.RouteEntry.builder()
                .role(role)
                .provider(route.safeProvider("OpenAI-Compatible-Route"))
                .baseUrl(maskUrlCredential(route.baseUrl()))
                .model(safe(route.model()))
                .configured(missingFields.isEmpty())
                .missingFields(missingFields)
                .build();
    }

    private List<String> missingFields(ExternalModelRoute route) {
        List<String> fields = new ArrayList<>();
        if (isBlank(route.baseUrl())) {
            fields.add("baseUrl");
        }
        if (isBlank(route.apiKey())) {
            fields.add("apiKey");
        }
        if (isBlank(route.model())) {
            fields.add("model");
        }
        return List.copyOf(fields);
    }

    private String resolveHealthLevel(int usableRouteCount) {
        if (!enabled) {
            return "DISABLED";
        }
        if (usableRouteCount <= 0) {
            return "NO_ROUTE";
        }
        if (usableRouteCount == 1) {
            return "SINGLE_ROUTE_RISK";
        }
        return "MULTI_ROUTE_READY";
    }

    private String summary(String healthLevel, int usableRouteCount) {
        return switch (healthLevel) {
            case "DISABLED" -> "AI 外部模型当前未启用。";
            case "NO_ROUTE" -> "AI 已启用，但没有完整可用的外部模型路由。";
            case "SINGLE_ROUTE_RISK" -> "当前只有 1 条可用外部模型路由，配额或限流会直接影响在线完成率。";
            case "MULTI_ROUTE_READY" -> "当前有 " + usableRouteCount + " 条可用外部模型路由，可降低单一路由配额风险。";
            default -> "AI 路由健康状态未知。";
        };
    }

    private List<String> suggestions(String healthLevel) {
        return switch (healthLevel) {
            case "DISABLED" -> List.of("如需启用在线 AI 诊断，请配置 AI_ENABLED=true 并补齐至少一条外部模型路由。");
            case "NO_ROUTE" -> List.of("补齐主路由或备用路由的 baseUrl、apiKey、model。", "不要把本地规则兜底误认为外部模型能力。");
            case "SINGLE_ROUTE_RISK" -> List.of("建议配置 AI_FALLBACK_* 或 AI_ROUTES，避免单一供应商额度耗尽后整体降级。", "上线前用 10 条以上长代码 live eval 验证完成率。");
            case "MULTI_ROUTE_READY" -> List.of("继续用 live eval 验证各路由的完成率、失败原因和质量指标。");
            default -> List.of("检查 AI 路由配置。");
        };
    }

    private String maskUrlCredential(String value) {
        if (isBlank(value)) {
            return "";
        }
        return value.replaceAll("(?i)(api[_-]?key|token|key)=([^&]+)", "$1=***");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

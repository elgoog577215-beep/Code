package com.onlinejudge.system.application;

import com.onlinejudge.system.dto.AiRouteHealthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AiRouteHealthServiceTest {

    @Test
    void reportsSingleRouteRiskWithoutLeakingApiKey() {
        AiRouteHealthService service = new AiRouteHealthService();
        configurePrimary(service, "primary-secret");

        AiRouteHealthResponse health = service.getHealth();

        assertThat(health.getHealthLevel()).isEqualTo("SINGLE_ROUTE_RISK");
        assertThat(health.getConfiguredRouteCount()).isEqualTo(1);
        assertThat(health.getUsableRouteCount()).isEqualTo(1);
        assertThat(health.getSuggestions()).anySatisfy(suggestion -> assertThat(suggestion).contains("AI_FALLBACK", "AI_ROUTES"));
        assertThat(health.toString()).doesNotContain("primary-secret");
        assertThat(health.getRoutes().get(0).getBaseUrl()).contains("api_key=***");
    }

    @Test
    void reportsMultiRouteReadyWhenFallbackAndRoutePoolAreConfigured() {
        AiRouteHealthService service = new AiRouteHealthService();
        configurePrimary(service, "primary-key");
        ReflectionTestUtils.setField(service, "fallbackProvider", "FallbackProvider");
        ReflectionTestUtils.setField(service, "fallbackBaseUrl", "https://fallback.example/v1");
        ReflectionTestUtils.setField(service, "fallbackApiKey", "fallback-key");
        ReflectionTestUtils.setField(service, "fallbackModel", "fallback-model");
        ReflectionTestUtils.setField(service, "additionalRoutes", "ExtraProvider|https://extra.example/v1|extra-key|extra-model");

        AiRouteHealthResponse health = service.getHealth();

        assertThat(health.getHealthLevel()).isEqualTo("MULTI_ROUTE_READY");
        assertThat(health.getConfiguredRouteCount()).isEqualTo(3);
        assertThat(health.getUsableRouteCount()).isEqualTo(3);
        assertThat(health.isFallbackConfigured()).isTrue();
        assertThat(health.isRoutePoolConfigured()).isTrue();
        assertThat(health.getRoutes())
                .extracting(AiRouteHealthResponse.RouteEntry::getRole)
                .containsExactly("PRIMARY", "FALLBACK", "ROUTE_POOL");
        assertThat(health.toString()).doesNotContain("primary-key", "fallback-key", "extra-key");
    }

    @Test
    void reportsNoRouteWhenEnabledButMissingApiKey() {
        AiRouteHealthService service = new AiRouteHealthService();
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "baseUrl", "https://api-inference.modelscope.cn/v1");
        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "model", "deepseek-ai/DeepSeek-V4-Pro");

        AiRouteHealthResponse health = service.getHealth();

        assertThat(health.getHealthLevel()).isEqualTo("NO_ROUTE");
        assertThat(health.getConfiguredRouteCount()).isZero();
        assertThat(health.getUsableRouteCount()).isZero();
        assertThat(health.getRoutes().get(0).getMissingFields()).contains("apiKey");
    }

    @Test
    void reportsDisabledEvenWhenRouteIsConfigured() {
        AiRouteHealthService service = new AiRouteHealthService();
        configurePrimary(service, "primary-key");
        ReflectionTestUtils.setField(service, "enabled", false);

        AiRouteHealthResponse health = service.getHealth();

        assertThat(health.getHealthLevel()).isEqualTo("DISABLED");
        assertThat(health.isEnabled()).isFalse();
        assertThat(health.getUsableRouteCount()).isEqualTo(1);
    }

    private void configurePrimary(AiRouteHealthService service, String apiKey) {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "provider", "ModelScope");
        ReflectionTestUtils.setField(service, "baseUrl", "https://api-inference.modelscope.cn/v1?api_key=visible-token");
        ReflectionTestUtils.setField(service, "apiKey", apiKey);
        ReflectionTestUtils.setField(service, "model", "deepseek-ai/DeepSeek-V4-Pro");
    }
}

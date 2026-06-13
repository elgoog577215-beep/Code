package com.onlinejudge.system.application;

import com.onlinejudge.shared.security.SchoolSecurityProperties;
import com.onlinejudge.system.dto.AiSmokeResponse;
import com.onlinejudge.system.dto.ExecutorStatusResponse;
import com.onlinejudge.system.dto.ReadinessResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReadinessServiceTest {

    @Test
    void schoolModeBlocksWhenDockerRunnerOrPostgresAreMissing() {
        ReadinessService service = service(
                ExecutorStatusResponse.builder()
                        .mode("local")
                        .dockerAvailable(false)
                        .cpp17Available(false)
                        .message("未检测到 Docker")
                        .build(),
                AiSmokeResponse.builder()
                        .status("UNKNOWN")
                        .message("尚未执行 AI smoke 检查。")
                        .build(),
                true
        );
        ReflectionTestUtils.setField(service, "datasourceUrl", "jdbc:h2:file:./data/onlinejudge");
        ReflectionTestUtils.setField(service, "aiEnabled", false);
        ReflectionTestUtils.setField(service, "aiBlocking", false);

        ReadinessResponse readiness = service.getReadiness();
        Map<String, ReadinessResponse.Check> checks = byId(readiness);

        assertThat(readiness.getStatus()).isEqualTo("BLOCKED");
        assertThat(checks.get("executor-mode").getStatus()).isEqualTo("FAIL");
        assertThat(checks.get("docker-daemon").getStatus()).isEqualTo("FAIL");
        assertThat(checks.get("cpp17-runner").getStatus()).isEqualTo("FAIL");
        assertThat(checks.get("database").getStatus()).isEqualTo("FAIL");
        assertThat(checks.get("database").isBlocking()).isTrue();
    }

    @Test
    void aiRateLimitDegradesWhenAiIsNotRequiredForOpeningClass() {
        ReadinessService service = readyInfrastructureWithAi("FAILED", "RATE_LIMITED", false);

        ReadinessResponse readiness = service.getReadiness();
        ReadinessResponse.Check ai = byId(readiness).get("ai-smoke");

        assertThat(readiness.getStatus()).isEqualTo("DEGRADED");
        assertThat(ai.getStatus()).isEqualTo("WARN");
        assertThat(ai.isBlocking()).isFalse();
        assertThat(ai.getMessage()).contains("限流");
    }

    @Test
    void aiRateLimitBlocksWhenAiIsRequiredForOpeningClass() {
        ReadinessService service = readyInfrastructureWithAi("FAILED", "RATE_LIMITED", true);

        ReadinessResponse readiness = service.getReadiness();
        ReadinessResponse.Check ai = byId(readiness).get("ai-smoke");

        assertThat(readiness.getStatus()).isEqualTo("BLOCKED");
        assertThat(ai.getStatus()).isEqualTo("WARN");
        assertThat(ai.isBlocking()).isTrue();
    }

    private ReadinessService readyInfrastructureWithAi(String aiStatus, String failureReason, boolean aiBlocking) {
        ReadinessService service = service(
                ExecutorStatusResponse.builder()
                        .mode("docker")
                        .dockerAvailable(true)
                        .cpp17Available(true)
                        .message("Docker runner ready")
                        .build(),
                AiSmokeResponse.builder()
                        .status(aiStatus)
                        .failureReason(failureReason)
                        .message("外部模型请求被限流，建议稍后重试或降低并发。")
                        .build(),
                true
        );
        ReflectionTestUtils.setField(service, "datasourceUrl", "jdbc:postgresql://postgres:5432/onlinejudge");
        ReflectionTestUtils.setField(service, "aiEnabled", true);
        ReflectionTestUtils.setField(service, "aiApiKey", "test-key");
        ReflectionTestUtils.setField(service, "aiBlocking", aiBlocking);
        return service;
    }

    private ReadinessService service(ExecutorStatusResponse executorStatus,
                                     AiSmokeResponse aiSmoke,
                                     boolean schoolProfile) {
        ExecutorStatusService executorStatusService = mock(ExecutorStatusService.class);
        when(executorStatusService.getStatus()).thenReturn(executorStatus);

        AiSmokeService aiSmokeService = mock(AiSmokeService.class);
        when(aiSmokeService.latest()).thenReturn(aiSmoke);

        SchoolSecurityProperties securityProperties = mock(SchoolSecurityProperties.class);
        when(securityProperties.schoolProfile()).thenReturn(schoolProfile);
        when(securityProperties.teacherPasswordConfigured()).thenReturn(true);
        when(securityProperties.teacherSessionSecretConfigured()).thenReturn(true);
        when(securityProperties.studentTokenSecretConfigured()).thenReturn(true);

        return new ReadinessService(executorStatusService, aiSmokeService, securityProperties);
    }

    private Map<String, ReadinessResponse.Check> byId(ReadinessResponse readiness) {
        return readiness.getChecks()
                .stream()
                .collect(Collectors.toMap(ReadinessResponse.Check::getId, Function.identity()));
    }
}

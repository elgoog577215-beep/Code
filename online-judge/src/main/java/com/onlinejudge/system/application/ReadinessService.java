package com.onlinejudge.system.application;

import com.onlinejudge.shared.security.SchoolSecurityProperties;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryGrowthProperties;
import com.onlinejudge.submission.application.PromptTemplateRegistry;
import com.onlinejudge.system.dto.AiSmokeResponse;
import com.onlinejudge.system.dto.ExecutorStatusResponse;
import com.onlinejudge.system.dto.ReadinessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReadinessService {

    private final ExecutorStatusService executorStatusService;
    private final AiSmokeService aiSmokeService;
    private final SchoolSecurityProperties securityProperties;
    private final AiStandardLibraryGrowthProperties growthProperties;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Value("${readiness.ai-blocking:${AI_READINESS_BLOCKING:true}}")
    private boolean aiBlocking;

    @Value("${ai.diagnosis-report-v3-enabled:${AI_DIAGNOSIS_REPORT_V3_ENABLED:true}}")
    private boolean diagnosisReportV3Enabled = true;

    public ReadinessResponse getReadiness() {
        List<ReadinessResponse.Check> checks = new ArrayList<>();
        ExecutorStatusResponse executor = executorStatusService.getStatus();

        checks.add(check(
                "deployment-mode",
                "部署模式",
                securityProperties.schoolProfile() ? "PASS" : "WARN",
                false,
                securityProperties.schoolProfile()
                        ? "当前为学校部署模式。"
                        : "当前不是 school 模式，适合开发或演示，不建议直接开课。",
                "学校部署请设置 APP_PROFILE=school。"
        ));

        checks.add(check(
                "executor-mode",
                "判题执行模式",
                "docker".equalsIgnoreCase(executor.getMode()) ? "PASS" : "FAIL",
                true,
                "docker".equalsIgnoreCase(executor.getMode())
                        ? "判题执行模式为 Docker。"
                        : "当前判题执行模式不是 Docker，不能作为正式课堂沙箱。",
                "设置 EXECUTOR_MODE=docker，并通过学校预检。"
        ));

        checks.add(check(
                "docker-daemon",
                "Docker 服务",
                executor.isDockerAvailable() ? "PASS" : "FAIL",
                true,
                executor.isDockerAvailable() ? "Docker daemon 可用。" : "未检测到 Docker daemon。",
                "启动 Docker Desktop、OrbStack 或 Docker Engine。"
        ));

        checks.add(check(
                "cpp17-runner",
                "C++17 runner",
                executor.isCpp17Available() ? "PASS" : "FAIL",
                true,
                executor.isCpp17Available() ? "C++17 runner 可用。" : executor.getMessage(),
                "运行 scripts/build-cpp17-runner.sh 或使用 docker compose 构建。"
        ));

        boolean h2 = datasourceUrl.toLowerCase().contains(":h2:");
        checks.add(check(
                "database",
                "数据库",
                securityProperties.schoolProfile() && h2 ? "FAIL" : h2 ? "WARN" : "PASS",
                securityProperties.schoolProfile() && h2,
                h2 ? "当前使用 H2 文件数据库，仅适合开发或极小范围试点。" : "当前未使用 H2，适合学校部署。",
                "学校模式请使用 Postgres，并配置备份。"
        ));

        checks.add(check(
                "teacher-password",
                "教师口令",
                securityProperties.teacherPasswordConfigured() ? "PASS" : "FAIL",
                true,
                securityProperties.teacherPasswordConfigured() ? "教师口令已配置。" : "未配置教师口令，教师端不能正式开放。",
                "设置 TEACHER_PASSWORD。"
        ));

        checks.add(check(
                "teacher-session-secret",
                "教师会话密钥",
                securityProperties.teacherSessionSecretConfigured() ? "PASS" : "FAIL",
                true,
                securityProperties.teacherSessionSecretConfigured() ? "教师会话密钥已配置。" : "教师会话密钥仍是开发默认值。",
                "设置随机 TEACHER_SESSION_SECRET。"
        ));

        checks.add(check(
                "student-token-secret",
                "学生令牌密钥",
                securityProperties.studentTokenSecretConfigured() ? "PASS" : "FAIL",
                true,
                securityProperties.studentTokenSecretConfigured() ? "学生令牌密钥已配置。" : "学生令牌密钥仍是开发默认值。",
                "设置随机 STUDENT_TOKEN_SECRET。"
        ));

        AiSmokeResponse smoke = aiSmokeService.latest();
        boolean aiConfigured = aiEnabled && aiApiKey != null && !aiApiKey.isBlank();
        String aiStatus = !aiEnabled ? "WARN" : !aiConfigured ? "WARN" : "READY".equals(smoke.getStatus()) ? "PASS" : "FAILED".equals(smoke.getStatus()) ? "WARN" : "WARN";
        checks.add(check(
                "ai-smoke",
                "外部 AI",
                aiStatus,
                aiBlocking && !"PASS".equals(aiStatus),
                aiStatusMessage(aiEnabled, aiConfigured, smoke),
                aiAction(aiEnabled && aiConfigured, smoke)
        ));

        checks.add(check(
                "ai-diagnosis-report-v3",
                "AI 诊断报告 v3",
                diagnosisReportV3Enabled ? "PASS" : "FAIL",
                true,
                diagnosisReportV3Enabled
                        ? "诊断报告 v3 已开启。"
                        : "诊断报告 v3 已关闭。",
                "设置 AI_DIAGNOSIS_REPORT_V3_ENABLED=true。"
        ));

        checks.add(check(
                "ai-standard-library-growth",
                "标准库成长 Agent",
                growthProperties.isEnabled() ? growthProperties.isAutoMergeEnabled() ? "WARN" : "PASS" : "WARN",
                false,
                growthProperties.isEnabled()
                        ? growthProperties.isAutoMergeEnabled()
                        ? "成长 Agent 已开启，允许自动入库。"
                        : "成长 Agent 已开启，只写入候选池。"
                        : "成长 Agent 已关闭。",
                "自动入库前请先小范围审核。"
        ));

        String overall = overallStatus(checks);
        return ReadinessResponse.builder()
                .status(overall)
                .updatedAt(LocalDateTime.now())
                .checks(checks)
                .build();
    }

    private String aiStatusMessage(boolean enabled, boolean configured, AiSmokeResponse smoke) {
        if (!enabled) {
            return "AI 已关闭。";
        }
        if (!configured) {
            return "API key 未配置。";
        }
        if ("READY".equals(smoke.getStatus())) {
            return "AI smoke 通过。";
        }
        return smoke.getMessage();
    }

    private String aiAction(boolean enabledAndConfigured, AiSmokeResponse smoke) {
        if (!enabledAndConfigured) {
            return "开启 AI 并配置有效 API key。";
        }
        if (smoke != null && "RATE_LIMITED".equalsIgnoreCase(smoke.getFailureReason())) {
            return "修正 key、检查额度与限流策略；必要时切换可用模型后重新执行 AI smoke。";
        }
        if (smoke != null && "UNAUTHORIZED".equalsIgnoreCase(smoke.getFailureReason())) {
            return "修正 key 或重新生成 ModelScope token 后执行 AI smoke。";
        }
        return "执行 AI smoke；失败时检查 key、额度、限流和网络。";
    }

    private String overallStatus(List<ReadinessResponse.Check> checks) {
        boolean blocked = checks.stream().anyMatch(check -> check.isBlocking() && !"PASS".equals(check.getStatus()));
        if (blocked) {
            return "BLOCKED";
        }
        boolean degraded = checks.stream().anyMatch(check -> !"PASS".equals(check.getStatus()));
        return degraded ? "DEGRADED" : "READY";
    }

    private ReadinessResponse.Check check(String id,
                                          String label,
                                          String status,
                                          boolean blocking,
                                          String message,
                                          String action) {
        return ReadinessResponse.Check.builder()
                .id(id)
                .label(label)
                .status(status)
                .blocking(blocking)
                .message(message)
                .action(action)
                .build();
    }
}

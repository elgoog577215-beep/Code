package com.onlinejudge.system.application;

import com.onlinejudge.submission.application.AiReportService;
import com.onlinejudge.submission.application.ExternalModelFailureClassifier;
import com.onlinejudge.submission.application.ModelStageFailureReason;
import com.onlinejudge.system.dto.AiSmokeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiSmokeService {

    private final AiReportService aiReportService;
    private final ExternalModelFailureClassifier failureClassifier;

    private volatile AiSmokeResponse latest;

    public AiSmokeResponse latest() {
        if (latest == null) {
            return AiSmokeResponse.builder()
                    .status("UNKNOWN")
                    .provider(aiReportService.providerName())
                    .model(aiReportService.modelName())
                    .failureReason("")
                    .message("尚未执行 AI smoke 检查。")
                    .latencyMs(null)
                    .checkedAt(null)
                    .build();
        }
        return latest;
    }

    public AiSmokeResponse runSmoke() {
        long started = System.nanoTime();
        try {
            String result = aiReportService.smokeChatCompletion();
            latest = AiSmokeResponse.builder()
                    .status("READY")
                    .provider(aiReportService.providerName())
                    .model(aiReportService.modelName())
                    .failureReason("")
                    .message(result == null || result.isBlank() ? "外部模型 smoke 已通过。" : "外部模型 smoke 已通过：" + result.trim())
                    .latencyMs(elapsedMs(started))
                    .checkedAt(LocalDateTime.now())
                    .build();
            return latest;
        } catch (Exception exception) {
            ModelStageFailureReason reason = failureClassifier.classify(exception);
            latest = AiSmokeResponse.builder()
                    .status("FAILED")
                    .provider(aiReportService.providerName())
                    .model(aiReportService.modelName())
                    .failureReason(reason.name())
                    .message(message(reason))
                    .latencyMs(elapsedMs(started))
                    .checkedAt(LocalDateTime.now())
                    .build();
            return latest;
        }
    }

    private long elapsedMs(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000);
    }

    private String message(ModelStageFailureReason reason) {
        return switch (reason) {
            case INSUFFICIENT_QUOTA -> "外部模型额度不足，AI 能力当前不可用。";
            case RATE_LIMITED -> "外部模型请求被限流，建议稍后重试或降低并发。";
            case TIMEOUT -> "外部模型响应超时，建议检查网络或降低超时风险。";
            case AUTHENTICATION_FAILED -> "外部模型认证失败，请检查 OJ_MODELSCOPE_API_KEY。";
            case INVALID_JSON -> "外部模型输出格式异常，AI 能力当前不可用。";
            case MODEL_UNSUPPORTED -> "当前模型不可用或不支持，请检查 OJ_AI_MODEL。";
            case BUDGET_GUARD_OPEN -> "外部模型预算保护已打开，暂时停止调用。";
            default -> "外部模型调用失败，AI 能力当前不可用。";
        };
    }
}

package com.onlinejudge.system.application;

import com.onlinejudge.execution.CodeExecutor;
import com.onlinejudge.shared.identity.YingqiSignature;
import com.onlinejudge.system.dto.ExecutorStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ExecutorStatusService {

    private final CodeExecutor codeExecutor;

    @Value("${executor.mode:local}")
    private String mode;

    public ExecutorStatusResponse getStatus() {
        String normalizedMode = mode == null ? "local" : mode.trim().toLowerCase(Locale.ROOT);
        boolean dockerAvailable = commandAvailable("docker", "version", "--format", "{{.Server.Version}}");
        boolean pythonAvailable = "docker".equals(normalizedMode)
                ? dockerAvailable
                : commandAvailable(isWindows() ? "python" : "python3", "--version");
        boolean cppAvailable = "docker".equals(normalizedMode)
                ? dockerAvailable
                : commandAvailable("g++", "--version");

        String message;
        if ("docker".equals(normalizedMode) && !dockerAvailable) {
            message = "容器沙箱未就绪：未检测到 Docker，Python/C++ 容器评测暂不可用。";
        } else if ("docker".equals(normalizedMode)) {
            message = "容器沙箱已就绪，Python/C++ 可通过 Docker 执行。";
        } else if (!cppAvailable) {
            message = "当前为本机执行模式：Python 可用性已检测，C++ 编译器未就绪。";
        } else {
            message = "当前执行环境可用。";
        }

        return ExecutorStatusResponse.builder()
                .mode(normalizedMode)
                .executorType(codeExecutor.getExecutorType())
                .dockerAvailable(dockerAvailable)
                .pythonAvailable(pythonAvailable)
                .cppAvailable(cppAvailable)
                .message(message)
                .projectOwner(YingqiSignature.OWNER)
                .ownershipSignature(YingqiSignature.FINGERPRINT)
                .ownershipClaim(YingqiSignature.CLAIM)
                .build();
    }

    private boolean commandAvailable(String... command) {
        try {
            Process process = new ProcessBuilder(command).start();
            boolean completed = process.waitFor(Duration.ofSeconds(4).toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}

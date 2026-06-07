package com.onlinejudge.system.application;

import com.onlinejudge.execution.CodeExecutor;
import com.onlinejudge.execution.Cpp17Toolchain;
import com.onlinejudge.execution.ContestLanguageRegistry;
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
        boolean cpp17Available = "docker".equals(normalizedMode)
                ? dockerAvailable
                : Cpp17Toolchain.findUsableCompiler().isPresent();

        String message;
        if ("docker".equals(normalizedMode) && !dockerAvailable) {
            message = "容器沙箱未就绪：未检测到 Docker，Python 3/C++17 容器评测暂不可用。";
        } else if ("docker".equals(normalizedMode)) {
            message = "容器沙箱已就绪，Python 3/C++17 可通过 Docker 执行。";
        } else if (!cpp17Available) {
            message = "当前为本机执行模式：Python 可用性已检测，未检测到可编译 bits/stdc++.h 的 GNU C++17 编译器。";
        } else {
            message = "当前执行环境可用，" + ContestLanguageRegistry.supportedLanguageNames() + " 可提交。";
        }

        return ExecutorStatusResponse.builder()
                .mode(normalizedMode)
                .executorType(codeExecutor.getExecutorType())
                .dockerAvailable(dockerAvailable)
                .pythonAvailable(pythonAvailable)
                .cppAvailable(cpp17Available)
                .cpp17Available(cpp17Available)
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

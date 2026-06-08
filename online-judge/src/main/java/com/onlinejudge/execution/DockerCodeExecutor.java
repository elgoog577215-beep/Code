package com.onlinejudge.execution;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@ConditionalOnProperty(name = "executor.mode", havingValue = "docker")
public class DockerCodeExecutor implements CodeExecutor {

    private static final Path TEMP_ROOT = Path.of(System.getProperty("java.io.tmpdir"), "wenzhong-docker-judge");

    @Value("${executor.docker.cpp17-image:wenzhong-oj-cpp17-runner:13}")
    private String cpp17Image;

    @Value("${executor.docker.python3-image:python:3.12-slim}")
    private String python3Image;

    @Override
    public String getExecutorType() {
        return "DOCKER";
    }

    @Override
    public boolean isAvailable() {
        return runSimpleDockerCheck();
    }

    @Override
    public ExecutionResult execute(String sourceCode,
                                   int languageId,
                                   String stdin,
                                   int timeLimitMs,
                                   int memoryLimitKb) {
        DockerLanguage language = dockerLanguage(languageId);
        if (language == null) {
            return ExecutionResult.error("容器沙箱暂只开放 " + ContestLanguageRegistry.supportedLanguageNames());
        }
        if (!isAvailable()) {
            return ExecutionResult.error("容器沙箱未就绪：当前系统未检测到 Docker，请先在部署环境安装并启动 Docker。");
        }

        Path workDir = TEMP_ROOT.resolve(UUID.randomUUID().toString());
        try {
            Files.createDirectories(workDir);
            makeContainerWorkspaceWritable(workDir);
            Path sourceFile = workDir.resolve("solution." + language.extension());
            Files.writeString(sourceFile, sourceCode == null ? "" : sourceCode, StandardCharsets.UTF_8);

            long started = System.currentTimeMillis();
            int compileTimeoutMs = Math.max(30000, timeLimitMs * 3);
            if (language.compileCommand() != null) {
                ExecutionResult compileResult = runDocker(language, workDir, language.compileCommand(), "", compileTimeoutMs, memoryLimitKb);
                if (compileResult.status == ExecutionResult.ResultStatus.TIME_LIMIT_EXCEEDED) {
                    return ExecutionResult.compilationError("编译超时，请检查模板、头文件和代码规模。");
                }
                if (compileResult.exitCode != 0) {
                    return ExecutionResult.compilationError(firstNonBlank(compileResult.stderr, compileResult.stdout));
                }
            }

            ExecutionResult runResult = runDocker(language, workDir, language.runCommand(), stdin, timeLimitMs, memoryLimitKb);
            runResult.executionTimeMs = System.currentTimeMillis() - started;
            return runResult;
        } catch (IOException exception) {
            log.error("Docker execution failed before container start", exception);
            return ExecutionResult.error("容器沙箱执行失败：" + exception.getMessage());
        } finally {
            cleanup(workDir);
        }
    }

    private ExecutionResult runDocker(DockerLanguage language,
                                      Path workDir,
                                      String command,
                                      String stdin,
                                      int timeoutMs,
                                      int memoryLimitKb) {
        List<String> args = new ArrayList<>();
        args.add("docker");
        args.add("run");
        args.add("--rm");
        args.add("--network");
        args.add("none");
        args.add("--cpus");
        args.add("1");
        args.add("--memory");
        args.add(Math.max(memoryLimitKb, 65536) + "k");
        args.add("--pids-limit");
        args.add("64");
        args.add("-i");
        args.add("-v");
        args.add(workDir.toAbsolutePath() + ":/workspace");
        args.add("-w");
        args.add("/workspace");
        args.add(language.image());
        args.add("sh");
        args.add("-lc");
        args.add(command);

        try {
            ProcessBuilder builder = new ProcessBuilder(args);
            Process process = builder.start();
            if (stdin != null && !stdin.isEmpty()) {
                try (OutputStream outputStream = process.getOutputStream()) {
                    outputStream.write(stdin.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                }
            } else {
                process.getOutputStream().close();
            }

            ExecutorService executor = Executors.newFixedThreadPool(2);
            Future<String> stdoutFuture = executor.submit(() -> readStream(process.getInputStream()));
            Future<String> stderrFuture = executor.submit(() -> readStream(process.getErrorStream()));
            boolean completed = process.waitFor(Math.max(timeoutMs, 100), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                executor.shutdownNow();
                return ExecutionResult.timeLimitExceeded();
            }

            String stdout = stdoutFuture.get(1, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(1, TimeUnit.SECONDS);
            executor.shutdown();
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return ExecutionResult.runtimeError(firstNonBlank(stderr, stdout), exitCode);
            }
            return new ExecutionResult(stdout, stderr, exitCode, 0);
        } catch (TimeoutException exception) {
            return ExecutionResult.timeLimitExceeded();
        } catch (Exception exception) {
            log.warn("Docker process failed. image={}, command={}", language.image(), command, exception);
            return ExecutionResult.error("容器沙箱进程启动失败：" + exception.getMessage());
        }
    }

    private boolean runSimpleDockerCheck() {
        try {
            Process process = new ProcessBuilder("docker", "version", "--format", "{{.Server.Version}}").start();
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

    private DockerLanguage dockerLanguage(int languageId) {
        ContestLanguageRegistry.ContestLanguage language = ContestLanguageRegistry.findSubmissionLanguage(languageId)
                .orElse(null);
        if (language == null) {
            return null;
        }
        return new DockerLanguage(
                language.displayName(),
                language.extension(),
                configuredDockerImage(language),
                language.dockerCompileCommand(),
                language.dockerRunCommand()
        );
    }

    private String configuredDockerImage(ContestLanguageRegistry.ContestLanguage language) {
        if (language.id() == ContestLanguageRegistry.CPP17_ID) {
            return firstNonBlank(cpp17Image, language.dockerImage());
        }
        if (language.id() == ContestLanguageRegistry.PYTHON3_ID) {
            return firstNonBlank(python3Image, language.dockerImage());
        }
        return language.dockerImage();
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback == null ? "" : fallback;
    }

    private void makeContainerWorkspaceWritable(Path workDir) {
        try {
            if (Files.getFileAttributeView(workDir, PosixFileAttributeView.class) == null) {
                return;
            }
            Files.setPosixFilePermissions(workDir, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_WRITE,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_WRITE,
                    PosixFilePermission.OTHERS_EXECUTE
            ));
        } catch (IOException exception) {
            log.warn("Failed to make docker judge workspace writable: {}", workDir, exception);
        }
    }

    private void cleanup(Path workDir) {
        if (workDir == null || !Files.exists(workDir)) {
            return;
        }
        try {
            Files.walk(workDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            log.warn("Failed to delete docker judge temp file: {}", path);
                        }
                    });
        } catch (IOException exception) {
            log.warn("Failed to cleanup docker judge temp directory: {}", workDir);
        }
    }

    private record DockerLanguage(String displayName,
                                  String extension,
                                  String image,
                                  String compileCommand,
                                  String runCommand) {
    }
}

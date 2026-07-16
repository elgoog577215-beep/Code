package com.onlinejudge.deployment;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchoolDeploymentScriptSafetyTest {

    private static final Path SCRIPTS = Path.of("scripts");
    private static final List<String> DESTRUCTIVE_COMMANDS = List.of(
            "docker system prune",
            "docker volume prune",
            "docker compose down -v",
            "docker-compose down -v"
    );

    @Test
    void productionStartupUsesExistingImagesWithoutBuilding() throws IOException {
        String shell = read("start-school.sh");
        String powershell = read("start-school.ps1");

        assertThat(shell).contains("docker compose up --no-build -d");
        assertThat(powershell).contains("docker compose up --no-build -d");
        assertThat(shell).doesNotContain("docker compose up --build", "docker compose build");
        assertThat(powershell).doesNotContain("docker compose up --build", "docker compose build");
    }

    @Test
    void imageBuildRequiresExplicitConfirmationAndDoesNotStartContainers() throws IOException {
        String shell = read("build-school-images.sh");
        String powershell = read("build-school-images.ps1");

        assertThat(shell).contains("--confirm-build", "docker compose build app cpp17-runner");
        assertThat(powershell).contains("[switch]$ConfirmBuild", "docker compose build app cpp17-runner");
        assertThat(shell).doesNotContain("docker compose up");
        assertThat(powershell).doesNotContain("docker compose up");
    }

    @Test
    void githubDeploymentRequiresManualDispatchAndExplicitBuildConfirmation() throws IOException {
        String workflow = readRepoFile(".github", "workflows", "deploy-online-judge.yml");

        assertThat(workflow).contains("workflow_dispatch:", "deploy-online-judge --confirm-build");
        assertThat(workflow).doesNotContain("push:");
    }

    @Test
    void serverDeploymentEntryUsesGuardedBuildAndSafeStartupScripts() throws IOException {
        String deploy = read("deploy-online-judge.sh");

        assertThat(deploy).contains(
                "--confirm-build",
                "git -C \"${REPO_ROOT}\" fetch origin main",
                "bash scripts/build-school-images.sh --confirm-build",
                "bash scripts/start-school.sh"
        );
        assertThat(deploy.indexOf("--confirm-build"))
                .isLessThan(deploy.indexOf("git -C \"${REPO_ROOT}\" fetch origin main"));
        assertThat(deploy).doesNotContain("docker compose up", "docker compose build");
    }

    @Test
    void deploymentScriptsNeverDeleteDockerVolumesOrBroadRuntimeState() throws IOException {
        for (String scriptName : List.of(
                "start-school.sh",
                "start-school.ps1",
                "build-school-images.sh",
                "build-school-images.ps1",
                "deploy-online-judge.sh"
        )) {
            assertThat(read(scriptName))
                    .as(scriptName)
                    .doesNotContain(DESTRUCTIVE_COMMANDS.toArray(String[]::new));
        }
    }

    private String read(String scriptName) throws IOException {
        return Files.readString(SCRIPTS.resolve(scriptName));
    }

    private String readRepoFile(String... parts) throws IOException {
        Path fromProject = Path.of("..").resolve(Path.of("", parts));
        if (Files.exists(fromProject)) {
            return Files.readString(fromProject);
        }
        return Files.readString(Path.of("", parts));
    }
}

package com.onlinejudge.deployment;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationScriptSafetyTest {

    private static final Path SCRIPTS = Path.of("scripts");

    @Test
    void baselineRequiresConfirmationBackupAndSchemaChecks() throws IOException {
        String script = read("baseline-postgres-flyway.sh");

        assertThat(script)
                .contains("--confirm-baseline")
                .contains("check-database-schema-readiness.sh --allow-unbaselined")
                .contains("backup-postgres.sh")
                .contains("rehearse-postgres-restore.sh")
                .contains("capture-database-counts.sh")
                .contains("compare-database-counts.sh")
                .contains("FLYWAY_BASELINE_ON_MIGRATE=true");
        assertThat(script).doesNotContain("docker compose down -v", "docker volume prune");
    }

    @Test
    void backupProducesVerifiableCustomArchiveAndMetadata() throws IOException {
        String script = read("backup-postgres.sh");

        assertThat(script)
                .contains("--format=custom")
                .contains("verify-postgres-backup.sh")
                .contains("sha256")
                .contains("postgres_version=")
                .contains("postgres_image=");
    }

    @Test
    void restoreRequiresConfirmationAndRefusesRunningApplication() throws IOException {
        String script = read("restore-postgres.sh");

        assertThat(script)
                .contains("--confirm-restore")
                .contains("verify-postgres-backup.sh")
                .contains("--exit-on-error")
                .contains("应用容器仍在运行");
        assertThat(script).doesNotContain("docker compose down -v", "docker volume prune");
    }

    @Test
    void restoreRehearsalUsesIsolatedContainerWithoutProjectVolume() throws IOException {
        String script = read("rehearse-postgres-restore.sh");

        assertThat(script)
                .contains("docker run -d --name")
                .contains("pg_restore")
                .contains("正式数据库和 Volume 未挂载");
        assertThat(script).doesNotContain("postgres-data", "docker compose exec");
    }

    @Test
    void schoolProfileUsesFlywayAndHibernateValidation() throws IOException {
        String application = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(application)
                .contains("baseline-on-migrate: ${FLYWAY_BASELINE_ON_MIGRATE:false}")
                .contains("clean-disabled: true")
                .contains("ddl-auto: validate");
    }

    private String read(String name) throws IOException {
        return Files.readString(SCRIPTS.resolve(name));
    }
}

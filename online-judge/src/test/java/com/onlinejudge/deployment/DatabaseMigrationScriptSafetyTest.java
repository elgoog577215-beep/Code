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

    @Test
    void disciplineBatchTwoUsesVersionedMigrationAndThreeTableConsistencyGate() throws IOException {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V3__refine_discipline_quality_batch_2.sql"));
        String qualityGate = read("check-discipline-data-quality.sh");

        assertThat(migration)
                .contains("informatics-knowledge-discipline-v2")
                .contains("informatics-discipline-quality-v2")
                .contains("discipline_quality_v2_improvements")
                .contains("INSERT INTO public.ai_standard_improvement_points")
                .contains("INSERT INTO public.ai_standard_library_items")
                .contains("INSERT INTO public.ai_standard_library_legacy_mappings")
                .doesNotContain("DELETE FROM", "TRUNCATE", "DROP TABLE public");
        assertThat(qualityGate)
                .contains("curated_knowledge_points_batch_2")
                .contains("discipline_v2_snapshot_mismatch")
                .contains("discipline_v2_legacy_mapping_mismatch")
                .contains("template_knowledge_descriptions_batch_2_limit")
                .contains("skills_without_improvement_batch_2_limit");
    }

    @Test
    void disciplineBatchThreeUsesEvidenceDrivenMigrationAndCompatibilityGuard() throws IOException {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V4__refine_discipline_quality_batch_3.sql"));
        String qualityGate = read("check-discipline-data-quality.sh");

        assertThat(migration)
                .contains("informatics-knowledge-discipline-v3")
                .contains("informatics-discipline-quality-v3")
                .contains("discipline_quality_v3_improvements")
                .contains("s.code NOT LIKE 'SK_COMPAT_%'")
                .contains("INSERT INTO public.ai_standard_improvement_points")
                .contains("INSERT INTO public.ai_standard_library_items")
                .contains("INSERT INTO public.ai_standard_library_legacy_mappings")
                .doesNotContain("DELETE FROM", "TRUNCATE", "DROP TABLE public");
        assertThat(qualityGate)
                .contains("curated_knowledge_points_batch_3_algo")
                .contains("curated_knowledge_points_batch_3_ds")
                .contains("discipline_v3_improvement_invalid_mistake_refs")
                .contains("discipline_v3_snapshot_mismatch")
                .contains("discipline_v3_legacy_mapping_mismatch")
                .contains("discipline_v3_compatibility_skill_target")
                .contains("template_knowledge_descriptions_batch_3_limit")
                .contains("skills_without_improvement_batch_3_limit");
    }

    @Test
    void diagnosisEvidenceV5UsesDeterministicBackfillAndStableIdentityGate() throws IOException {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V5__strengthen_diagnosis_evidence_quality.sql"));
        String qualityGate = read("check-diagnosis-evidence-quality.sh");
        String integration = read("test-postgres-migrations.sh");

        assertThat(migration)
                .contains("ADD COLUMN IF NOT EXISTS provisional_node_code")
                .contains("diagnosis_evidence_v5_advice_items")
                .contains("provisionalNodeCode")
                .contains("f.skill_unit_id IS DISTINCT FROM m.skill_unit_code")
                .contains("invalid or inconsistent standard-library anchor")
                .contains("ck_diagnosis_fact_provisional_identity")
                .contains("ck_diagnosis_fact_library_fit")
                .contains("does not resolve to exactly one growth candidate")
                .doesNotContain("DELETE FROM", "TRUNCATE", "DROP TABLE public");
        assertThat(qualityGate)
                .contains("analysis_projection_mismatch")
                .contains("provisional_fact_missing_code")
                .contains("provisional_code_missing_candidate")
                .contains("provisional_identity_source_mismatch")
                .contains("formal_anchor_invalid")
                .contains("library_fit_invalid")
                .contains("post_v5_fact_rows");
        assertThat(integration)
                .contains("[[ \"${VERSION}\" == \"5\" ]]")
                .contains("V1-V5");
    }

    private String read(String name) throws IOException {
        return Files.readString(SCRIPTS.resolve(name));
    }
}

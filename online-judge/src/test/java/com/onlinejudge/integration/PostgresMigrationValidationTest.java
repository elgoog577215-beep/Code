package com.onlinejudge.integration;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.profile=school",
        "app.content-seed.enabled=false",
        "AI_ENABLED=false",
        "TEACHER_DEV_AUTO_AUTH=false",
        "security.bootstrap-admin.username=bootstrap-admin",
        "security.bootstrap-admin.password=Bootstrap123",
        "security.bootstrap-admin.display-name=测试管理员"
})
@AutoConfigureMockMvc
class PostgresMigrationValidationTest {
    private static final EmbeddedPostgres POSTGRES = startPostgres();

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired Flyway flyway;
    @Autowired DataSource dataSource;
    @Autowired MockMvc mockMvc;

    @Test
    void emptyPostgresMigratesToV20AndHibernateValidatesTheSchema() throws Exception {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("20");
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            assertThat(count(statement, "select count(*) from information_schema.tables where table_schema='public' and table_name='teacher_accounts'"))
                    .isEqualTo(1);
            assertThat(count(statement, "select count(*) from teacher_accounts where status='ACTIVE' and role='PLATFORM_ADMIN' and username_normalized='bootstrap-admin'"))
                    .isEqualTo(1);
            assertThat(count(statement, "select count(*) from information_schema.tables where table_schema='public' and table_name='schools'"))
                    .isEqualTo(1);
        }
    }

    @Test
    void existingNonEmptySchemaIsBaselinedAtV1ThenMigratedWithoutDataLoss() throws Exception {
        String databaseName = "legacy_copy_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("create database " + databaseName);
        }

        DataSource existingDataSource = POSTGRES.getDatabase("postgres", databaseName);
        Flyway.configure().dataSource(existingDataSource).target("1").load().migrate();
        try (var connection = existingDataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("drop table flyway_schema_history");
            statement.execute("insert into class_groups (name, created_at) values ('保留班级', current_timestamp)");
            statement.execute("insert into problems (title, description, difficulty, time_limit, memory_limit) values ('保留题目', '旧库题目', 'EASY', 1000, 128000)");
        }

        Flyway existing = Flyway.configure().dataSource(existingDataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true).baselineVersion("1").load();
        existing.migrate();

        assertThat(existing.info().current().getVersion().getVersion()).isEqualTo("20");
        try (var connection = existingDataSource.getConnection(); var statement = connection.createStatement()) {
            assertThat(count(statement, "select count(*) from class_groups where name='保留班级'")) .isEqualTo(1);
            assertThat(count(statement, "select count(*) from teacher_accounts where id='00000000-0000-0000-0000-000000000001'"))
                    .isEqualTo(1);
            assertThat(count(statement, "select count(*) from problems where title='保留题目' and scope='PUBLIC' and version_state='PUBLISHED'"))
                    .isEqualTo(1);
        }
    }

    @Test
    void schoolProfileRequiresCsrfForAuthenticatedPlatformMutations() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/account/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bootstrap-admin\",\"password\":\"Bootstrap123\",\"portal\":\"PLATFORM_ADMIN\"}"))
                .andExpect(status().isOk()).andReturn();
        String teacherCookie = login.getResponse().getHeaders("Set-Cookie").stream()
                .map(value -> value.split(";", 2)[0])
                .filter(value -> value.startsWith("OJ_TEACHER_SESSION="))
                .findFirst().orElseThrow();

        String schoolJson = "{\"schoolName\":\"CSRF学校\",\"adminUsername\":\"csrf-admin\",\"adminDisplayName\":\"校管\",\"monthlyAiUnits\":10}";
        mockMvc.perform(post("/api/platform-admin/schools").header("Cookie", teacherCookie)
                        .contentType(MediaType.APPLICATION_JSON).content(schoolJson))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/platform-admin/schools").with(csrf()).header("Cookie", teacherCookie)
                        .contentType(MediaType.APPLICATION_JSON).content(schoolJson))
                .andExpect(status().isOk());
    }

    private long count(java.sql.Statement statement, String sql) throws Exception {
        try (var result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.builder().setPort(0).start();
        } catch (IOException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    @AfterAll
    static void stopPostgres() throws IOException {
        POSTGRES.close();
    }
}

package com.onlinejudge.submission.application;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AiModelConfigurationTest {

    private static final String VERIFIED_QWEN_MODEL = "Qwen/Qwen3-235B-A22B-Instruct-2507";

    @Test
    void applicationConfigDefaultsToVerifiedQwenModelButKeepsEnvironmentOverride() throws Exception {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(applicationYaml)
                .contains("model: ${OJ_AI_MODEL:${AI_MODEL:" + VERIFIED_QWEN_MODEL + "}}")
                .doesNotContain("model: ${OJ_AI_MODEL:${AI_MODEL:deepseek-ai/DeepSeek-V4-Flash}}");
    }

    @Test
    void envExampleUsesVerifiedQwenModel() throws Exception {
        String envExample = Files.readString(Path.of(".env.example"));

        assertThat(envExample).contains("OJ_AI_MODEL=" + VERIFIED_QWEN_MODEL);
    }
}

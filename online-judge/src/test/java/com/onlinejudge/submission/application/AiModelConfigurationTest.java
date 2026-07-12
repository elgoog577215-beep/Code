package com.onlinejudge.submission.application;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AiModelConfigurationTest {

    private static final String PRIMARY_MODEL = "deepseek-ai/DeepSeek-V4-Pro";
    private static final String VERIFIED_MODEL_POOL =
            "deepseek-ai/DeepSeek-V4-Pro,Qwen/Qwen3.5-35B-A3B,Qwen/Qwen3.5-27B,Qwen/Qwen3.5-122B-A10B";

    @Test
    void applicationConfigDefaultsToDeepSeekV4ProButKeepsEnvironmentOverride() throws Exception {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(applicationYaml)
                .contains("model: ${OJ_AI_MODEL:${AI_MODEL:" + PRIMARY_MODEL + "}}")
                .contains("model-pool: ${OJ_AI_MODEL_POOL:${AI_MODEL_POOL:" + VERIFIED_MODEL_POOL + "}}")
                .contains("enable-thinking: ${AI_ENABLE_THINKING:false}")
                .doesNotContain("model: ${OJ_AI_MODEL:${AI_MODEL:deepseek-ai/DeepSeek-V4-Flash}}");
    }

    @Test
    void envExampleUsesDeepSeekV4ProAsPrimaryModel() throws Exception {
        String envExample = Files.readString(Path.of(".env.example"));

        assertThat(envExample)
                .contains("OJ_AI_MODEL=" + PRIMARY_MODEL)
                .contains("OJ_AI_MODEL_POOL=" + VERIFIED_MODEL_POOL)
                .contains("AI_ENABLE_THINKING=false");
    }
}

package com.onlinejudge.submission.application;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AiModelConfigurationTest {

    private static final String PRIMARY_QWEN_MODEL = "Qwen/Qwen3.5-122B-A10B";
    private static final String VERIFIED_QWEN_MODEL_POOL =
            "Qwen/Qwen3.5-122B-A10B,Qwen/Qwen3-Coder-30B-A3B-Instruct,Qwen/Qwen3.5-35B-A3B,"
                    + "Qwen/Qwen3.5-27B,Qwen/Qwen3-235B-A22B-Instruct-2507,"
                    + "Qwen/Qwen3-Next-80B-A3B-Instruct,Qwen/Qwen3.5-397B-A17B";

    @Test
    void applicationConfigDefaultsToVerifiedQwenModelButKeepsEnvironmentOverride() throws Exception {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(applicationYaml)
                .contains("model: ${OJ_AI_MODEL:${AI_MODEL:" + PRIMARY_QWEN_MODEL + "}}")
                .contains("model-pool: ${OJ_AI_MODEL_POOL:${AI_MODEL_POOL:" + VERIFIED_QWEN_MODEL_POOL + "}}")
                .doesNotContain("model: ${OJ_AI_MODEL:${AI_MODEL:deepseek-ai/DeepSeek-V4-Flash}}");
    }

    @Test
    void envExampleUsesVerifiedQwenModel() throws Exception {
        String envExample = Files.readString(Path.of(".env.example"));

        assertThat(envExample)
                .contains("OJ_AI_MODEL=" + PRIMARY_QWEN_MODEL)
                .contains("OJ_AI_MODEL_POOL=" + VERIFIED_QWEN_MODEL_POOL);
    }
}

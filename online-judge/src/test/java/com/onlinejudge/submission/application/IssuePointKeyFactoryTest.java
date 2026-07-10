package com.onlinejudge.submission.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IssuePointKeyFactoryTest {

    private final IssuePointKeyFactory factory = new IssuePointKeyFactory();

    @Test
    void usesFormalIdentityBeforeSingleAnalysisIssueNumber() {
        var first = factory.identity("REPAIR", "MP_BOUNDARY", null, "SK_LOOP", List.of("循环", "边界"), "边界错误");
        var second = factory.identity("REPAIR", "MP_BOUNDARY", null, "SK_OTHER", List.of("其他"), "完全不同标题");

        assertThat(first.key()).isEqualTo(second.key()).startsWith("mistake:point-key-v1:");
        assertThat(first.source()).isEqualTo("FORMAL_ID");
    }

    @Test
    void createsStableNormalizedTextFingerprintWhenFormalIdentityIsMissing() {
        var first = factory.identity("REPAIR", null, null, null, List.of("循环", "边界处理"), " 下标，越界！ ");
        var second = factory.identity("REPAIR", null, null, null, List.of("循环", "边界处理"), "下标 越界");

        assertThat(first.key()).isEqualTo(second.key()).startsWith("text:point-key-v1:");
        assertThat(first.source()).isEqualTo("TEXT_FINGERPRINT");
    }
}

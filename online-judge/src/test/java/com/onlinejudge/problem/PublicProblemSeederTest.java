package com.onlinejudge.problem;

import com.onlinejudge.problem.application.PublicProblemSeedCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicProblemSeederTest {

    @Test
    void seedsTenAdvancedPythonProblemsWithLongStarterCode() {
        assertThat(PublicProblemSeedCatalog.seeds()).hasSize(10);
        assertThat(PublicProblemSeedCatalog.seeds())
                .allSatisfy(seed -> {
                    assertThat(seed.description()).contains("## 题目描述");
                    assertThat(seed.testCases()).anySatisfy(testCase -> assertThat(testCase.hidden()).isFalse());
                    assertThat(seed.testCases()).anySatisfy(testCase -> assertThat(testCase.hidden()).isTrue());
                    assertThat(seed.starterCode()).contains("def ").contains("if __name__ == \"__main__\":");
                    assertThat(seed.starterCode().lines().count()).isGreaterThanOrEqualTo(50);
                    assertThat(seed.commonMistakes()).isNotEmpty();
                });
    }
}

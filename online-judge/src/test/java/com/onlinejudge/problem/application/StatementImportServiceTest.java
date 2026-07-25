package com.onlinejudge.problem.application;

import com.onlinejudge.problem.dto.StatementImportResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatementImportServiceTest {

    private final StatementImportService service = new StatementImportService();

    @Test
    void parsesTitleAndChineseSecondLevelSections() {
        StatementImportResponse response = service.parseMarkdown("""
                # A+B Problem

                ## 题目背景
                这是背景。

                ## 题目描述
                给定两个整数。

                ## 输入格式
                一行两个整数。

                ## 输出格式
                输出它们的和。

                ## 样例
                ```text
                1 2
                ```

                ## 提示说明
                注意范围。
                """);

        assertThat(response.getTitle()).isEqualTo("A+B Problem");
        assertThat(response.getStatementBackground()).isEqualTo("这是背景。");
        assertThat(response.getStatementDescription()).isEqualTo("给定两个整数。");
        assertThat(response.getStatementInputFormat()).isEqualTo("一行两个整数。");
        assertThat(response.getStatementOutputFormat()).isEqualTo("输出它们的和。");
        assertThat(response.getStatementSamples()).contains("1 2");
        assertThat(response.getStatementHints()).isEqualTo("注意范围。");
    }

    @Test
    void ignoresUnknownSectionsAndLeavesStatusUntouchedByContract() {
        StatementImportResponse response = service.parseMarkdown("""
                # Hidden Title

                ## 题目状态
                公开

                ## Description
                Imported description.
                """);

        assertThat(response.getTitle()).isEqualTo("Hidden Title");
        assertThat(response.getStatementDescription()).isEqualTo("Imported description.");
        assertThat(response.getStatementBackground()).isNull();
    }
}

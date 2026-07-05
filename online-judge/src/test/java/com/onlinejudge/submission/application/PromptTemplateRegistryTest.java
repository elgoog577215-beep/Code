package com.onlinejudge.submission.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptTemplateRegistryTest {

    private final PromptTemplateRegistry registry = new PromptTemplateRegistry();

    @Test
    void registersSearchLocationAdviceAndDiagnosisReportPrompts() {
        PromptTemplateRegistry.PromptTemplate searchLocation =
                registry.get(PromptTemplateRegistry.SEARCH_LOCATION_V1);
        PromptTemplateRegistry.PromptTemplate advice =
                registry.get(PromptTemplateRegistry.DIAGNOSIS_AND_ADVICE_V1);
        PromptTemplateRegistry.PromptTemplate report =
                registry.get(PromptTemplateRegistry.DIAGNOSIS_REPORT_V2);

        assertThat(searchLocation.getStage()).isEqualTo("SEARCH_LOCATION");
        assertThat(searchLocation.getSystemPrompt())
                .contains("search-location stage")
                .contains("candidatePack")
                .contains("basicCandidates")
                .contains("improvementCandidates")
                .contains("knowledgeAnchors")
                .contains("navigation map")
                .contains("HIT")
                .contains("PARTIAL")
                .contains("MISS")
                .contains("normalized teaching structure")
                .contains("parentSkillUnitId")
                .contains("structurePath")
                .contains("siblingMistakePointIds")
                .contains("relatedImprovementPointIds")
                .contains("Do not provide complete code");

        assertThat(advice.getStage()).isEqualTo("DIAGNOSIS_AND_ADVICE");
        assertThat(advice.getSystemPrompt())
                .contains("complete diagnosis and advice generation stage")
                .contains("standardLibrary.knowledgeGroups")
                .contains("main structure")
                .contains("Return one advice item per independent evidence-backed issue")
                .contains("caseUnderstanding")
                .contains("basicLayerAdvice")
                .contains("improvementLayerAdvice")
                .contains("nextStepPlan")
                .contains("studentSummary")
                .contains("Do not provide complete code");

        assertThat(report.getStage()).isEqualTo("DIAGNOSIS_REPORT");
        assertThat(report.getSystemPrompt())
                .contains("diagnosis report v2")
                .contains("单诊断 Agent")
                .contains("不要在中文解释里使用英文双引号")
                .contains("标准库的作用")
                .contains("standardLibrary.knowledgeGroups")
                .contains("知识节点 → 能力点 → 易错点 / 提升点")
                .contains("兼容列表")
                .contains("结构邻域")
                .contains("不是唯一答案来源")
                .contains("不能限制你对题目、代码和判题结果的整体判断")
                .contains("先自由列出真实诊断候选")
                .contains("diagnosisCandidates")
                .contains("不是思维链")
                .contains("每个 diagnosisCandidates 项都必须引用 evidenceRefs")
                .contains("libraryGrowth.candidates 应来自 diagnosisCandidates")
                .contains("studentReport")
                .contains("basicLayerText")
                .contains("improvementLayerText")
                .contains("nextActionText")
                .contains("先讲人话")
                .contains("基础层优先")
                .contains("题目约束和代码策略不一致")
                .contains("不要直接跳到新算法名")
                .contains("错误策略")
                .contains("120-220 个中文字符")
                .contains("80-180 个中文字符")
                .contains("OUT_OF_LIBRARY")
                .contains("HIT")
                .contains("PARTIAL")
                .contains("MISS")
                .contains("visibleCaseFacts[].inputPreview")
                .contains("actualOutputPreview")
                .contains("expectedOutputPreview")
                .contains("不能把输出当输入")
                .contains("禁止出现自己编的具体数组")
                .contains("只描述反例形态")
                .contains("不要写出可见样例的完整最优组合")
                .contains("不要直接给替代 DP 的状态定义或转移教程")
                .contains("隐藏测试失败时")
                .contains("不要直接指出隐藏情况下代码具体漏比较了哪些位置或字符")
                .contains("禁止给完整代码")
                .contains("不要写出替换表达式或精确循环头")
                .contains("C++ 类型与精度问题")
                .contains("不要写出强制转换表达式")
                .contains("不要把学生变量的具体修正值或初始化位置直接写出来")
                .contains("初始化位置")
                .contains("显式清空全局状态")
                .contains("studentReport 三个字段必须是普通单行字符串");
    }

    @Test
    void oldPromptVersionsAreNotResolvable() {
        assertThatThrownBy(() -> registry.get("diagnosis-and-" + "teaching-v3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown prompt template version");
        assertThatThrownBy(() -> registry.get("diagnosis-" + "judge-v2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown prompt template version");
        assertThatThrownBy(() -> registry.get("teaching-" + "hint-v1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown prompt template version");
    }

    @Test
    void advicePromptDoesNotExposeLegacyContracts() {
        String prompt = registry.get(PromptTemplateRegistry.DIAGNOSIS_AND_ADVICE_V1).getSystemPrompt();

        assertThat(prompt)
                .doesNotContain("diagnosisDecision")
                .doesNotContain("teachingHint")
                .doesNotContain("Combined" + "Output")
                .doesNotContain("AiAnalysis" + "Payload");
    }
}

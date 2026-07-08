package com.onlinejudge.submission.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.classroom.domain.Assignment;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionCaseResult;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:diagnosis-real-sample-simulation;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ai.enabled=true",
        "ai.api-key=${AI_EVAL_API_KEY:${MODELSCOPE_API_KEY:}}",
        "ai.base-url=${AI_EVAL_BASE_URL:${AI_BASE_URL:https://api-inference.modelscope.cn/v1}}",
        "ai.model=${AI_EVAL_MODEL:${AI_MODEL:Qwen/Qwen3-235B-A22B-Instruct-2507}}",
        "ai.timeout-seconds=${AI_EVAL_TIMEOUT_SECONDS:45}",
        "ai.external-runtime-enabled=true",
        "ai.external-runtime-profile=${AI_EVAL_RUNTIME_PROFILE:low-latency}",
        "ai.diagnosis-report-v3-enabled=true",
        "ai.max-output-tokens=${AI_EVAL_MAX_OUTPUT_TOKENS:1800}",
        "ai.stream-enabled=${AI_EVAL_STREAM_ENABLED:false}",
        "ai.structured-retry-enabled=${AI_EVAL_STRUCTURED_RETRY_ENABLED:true}",
        "ai.structured-retry-output-tokens=${AI_EVAL_STRUCTURED_RETRY_OUTPUT_TOKENS:2600}",
        "ai.standard-library-growth.enabled=false"
})
class DiagnosisReportV2RealSamplesSimulationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DiagnosticAgentService diagnosticAgentService;

    @Test
    void runWebsiteVsCodexSimulationReportWhenEnabled() throws Exception {
        Assumptions.assumeTrue(Boolean.parseBoolean(env("AI_REAL_SAMPLE_SIMULATION_ENABLED", "false")),
                "Set AI_REAL_SAMPLE_SIMULATION_ENABLED=true to run live real-sample simulation.");
        Assumptions.assumeTrue(!env("AI_EVAL_API_KEY", env("MODELSCOPE_API_KEY", "")).isBlank(),
                "Set AI_EVAL_API_KEY or MODELSCOPE_API_KEY to call the external model.");

        List<RealSample> samples = loadSamples();
        int limit = (int) longEnv("AI_REAL_SAMPLE_SIMULATION_LIMIT", samples.size());
        List<RealSample> selected = samples.stream().limit(Math.max(1, Math.min(limit, samples.size()))).toList();

        List<ReportEntry> entries = new ArrayList<>();
        for (int index = 0; index < selected.size(); index++) {
            waitBetweenCases(index, longEnv("AI_EVAL_CASE_DELAY_MS", 750));
            RealSample sample = selected.get(index);
            SimulationFixture fixture = fixtureFor(sample);
            long startedAt = System.nanoTime();
            DiagnosticAgentService.AgentResult result = diagnosticAgentService.diagnose(
                    fixture.problem(),
                    fixture.submission(),
                    fixture.caseResults(),
                    fixture.baseline(),
                    Assignment.HintPolicy.L3
            );
            long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
            entries.add(ReportEntry.from(sample, result.analysis(), latencyMs, codexSimulation(sample.id())));
        }

        Path reportPath = writeReport(entries);
        System.out.println("Real sample website-vs-codex simulation report saved to: " + reportPath.toAbsolutePath());
        assertThat(reportPath).exists();
        assertThat(entries).hasSize(selected.size());
    }

    private List<RealSample> loadSamples() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/diagnosis-eval-fixtures/diagnosis-report-v2-real-samples.json")) {
            assertThat(input).isNotNull();
            return objectMapper.readValue(input, new TypeReference<>() {
            });
        }
    }

    private SimulationFixture fixtureFor(RealSample sample) {
        long stableId = 90_000L + Math.abs(sample.id().hashCode() % 10_000);
        FailureExample failure = failureExample(sample.id());
        Problem problem = Problem.builder()
                .id(stableId)
                .title(sample.title())
                .description("""
                        ## 题目描述
                        %s

                        ## 仿真说明
                        本题来自 diagnosis-report-v2 real sample，用于比较网站真实 AI 链路与人工 AI 仿真输出。
                        """.formatted(sample.problemSummary()))
                .difficulty(Problem.Difficulty.MEDIUM)
                .timeLimit(1000)
                .memoryLimit(65536)
                .build();
        Submission submission = Submission.builder()
                .id(stableId)
                .problemId(stableId)
                .languageName(languageName(sample.language()))
                .verdict(Submission.Verdict.valueOf(sample.judgeResult()))
                .sourceCode(sample.buggyCode())
                .build();
        List<SubmissionCaseResult> caseResults = List.of(SubmissionCaseResult.builder()
                .submissionId(stableId)
                .testCaseNumber(1)
                .passed(false)
                .hidden(false)
                .inputSnapshot(failure.input())
                .actualOutput(failure.actual())
                .expectedOutput(failure.expected())
                .executionTime(1.0)
                .memoryUsed(1024)
                .build());
        SubmissionAnalysisResponse baseline = SubmissionAnalysisResponse.builder()
                .submissionId(stableId)
                .sourceType("REAL_SAMPLE_SIMULATION_BASELINE")
                .scenario(sample.judgeResult())
                .headline("仿真基线诊断")
                .summary("基线只提供题目、代码和首个失败样例，正式判断交给网站 AI 链路。")
                .issueTags(List.of())
                .fineGrainedTags(List.of())
                .evidenceRefs(List.of("judge:first_failed_case"))
                .firstFailedCase(SubmissionAnalysisResponse.FailedCaseSnapshot.builder()
                        .testCaseNumber(1)
                        .hidden(false)
                        .input(failure.input())
                        .actualOutput(failure.actual())
                        .expectedOutput(failure.expected())
                        .build())
                .answerLeakRisk("LOW")
                .confidence(0.5)
                .build();
        return new SimulationFixture(problem, submission, caseResults, baseline);
    }

    private FailureExample failureExample(String id) {
        Map<String, FailureExample> examples = new LinkedHashMap<>();
        examples.put("sum-right-endpoint", new FailureExample("5", "10", "15"));
        examples.put("matrix-difference-prefix-restore", new FailureExample("2 2 1\n1 1 2 2 3", "3", "3 3\n3 3"));
        examples.put("monotonic-stack-duplicates", new FailureExample("4\n2 2 1 2", "0 0 2 0", "0 1 2 2"));
        examples.put("stone-merge-dp-interval", new FailureExample("4\n1 2 3 4", "10", "19"));
        examples.put("dynamic-connectivity-missing-union", new FailureExample("3 3\n1 1 2\n2 1 2\n2 2 3", "No\nNo", "Yes\nNo"));
        examples.put("string-window-boundary", new FailureExample("a\n1", "0", "1"));
        examples.put("sliding-window-balance-count", new FailureExample("3 3\n2 1 2", "1", "2"));
        examples.put("layered-shortest-path-state", new FailureExample("3 2 1\n1 2 5\n2 3 5", "10", "5"));
        return examples.getOrDefault(id, new FailureExample("visible sample", "wrong", "expected"));
    }

    private CodexSimulation codexSimulation(String id) {
        Map<String, CodexSimulation> simulations = new LinkedHashMap<>();
        simulations.put("sum-right-endpoint", new CodexSimulation(
                "循环右端点漏取；range 的右端不包含 n，导致 1..n 少算最后一项。",
                "HIT", 1, 1, "手推 n=1 或 n=2，列出循环变量实际出现过哪些值。"));
        simulations.put("matrix-difference-prefix-restore", new CodexSimulation(
                "二维差分还原时状态来源不完整/被污染，还只输出右下角，无法代表最终矩阵。",
                "PARTIAL", 2, 1, "用 2x2 表格手写差分标记和还原过程，先核对每个格子的来源。"));
        simulations.put("monotonic-stack-duplicates", new CodexSimulation(
                "重复值场景下弹栈条件和题意的严格/非严格关系不一致。",
                "PARTIAL", 1, 1, "构造含相等元素的小样例，观察相等元素应不应该保留在栈里。"));
        simulations.put("stone-merge-dp-interval", new CodexSimulation(
                "区间 DP 依赖短区间先算，当前按左端点推进会读到未完成的大/小区间状态。",
                "HIT", 1, 1, "按区间长度从 1、2、3 手推依赖关系，确认每次转移依赖是否已算好。"));
        simulations.put("dynamic-connectivity-missing-union", new CodexSimulation(
                "并查集合并条件反了，只在已经连通时合并，原本不连通的加边没有生效。",
                "HIT", 1, 1, "手推两个原本不连通点执行加边后，父节点集合有没有发生合并。"));
        simulations.put("string-window-boundary", new CodexSimulation(
                "窗口长度计算没有包含当前右端点，长度为 1 的合法窗口会被算成 0。",
                "PARTIAL", 1, 1, "用单字符窗口手推 left、right 和窗口长度之间的关系。"));
        simulations.put("sliding-window-balance-count", new CodexSimulation(
                "缩窗时先移动 left 再扣元素，扣掉的是新 left 元素，窗口状态和指针不同步。",
                "PARTIAL", 1, 1, "写出 left 移动前后窗口实际包含的元素，再核对 cost 是否等于窗口和。"));
        simulations.put("layered-shortest-path-state", new CodexSimulation(
                "最短路状态缺少已使用特殊操作次数，无法区分同一节点在不同层数下的代价。",
                "HIT", 1, 1, "把状态写成“节点 + 已用次数”的语义检查表，再判断原代码记录了哪些信息。"));
        return simulations.get(id);
    }

    private Path writeReport(List<ReportEntry> entries) throws Exception {
        Path dir = Path.of("target", "ai-simulation-reports");
        Files.createDirectories(dir);
        Path reportPath = dir.resolve("real-samples-website-vs-codex-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".md");
        StringBuilder out = new StringBuilder();
        out.append("# Real Samples 网站链路 vs Codex 仿真对照\n\n");
        out.append("- 样本数：").append(entries.size()).append("\n");
        out.append("- 网站线：Spring `DiagnosticAgentService` + 当前标准库 + 当前 prompt/validator + 外部模型。\n");
        out.append("- Codex 仿真线：Codex 按同题题意、代码、失败样例给出的理想诊断摘要。\n\n");
        out.append("| 题目 | 网站状态 | 失败原因 | 网站 libraryFit | 网站建议数 | Codex libraryFit | 对照判断 |\n");
        out.append("|---|---|---|---:|---:|---:|---|\n");
        for (ReportEntry entry : entries) {
            out.append("| ")
                    .append(escape(entry.title()))
                    .append(" | ")
                    .append(escape(entry.websiteStatus()))
                    .append(" | ")
                    .append(escape(entry.websiteFailureReason()))
                    .append(" | ")
                    .append(escape(entry.websiteLibraryFit()))
                    .append(" | ")
                    .append(entry.websiteAdviceCount())
                    .append(" | ")
                    .append(escape(entry.codex().libraryFit()))
                    .append(" | ")
                    .append(escape(entry.comparison()))
                    .append(" |\n");
        }
        out.append("\n## 逐题明细\n\n");
        for (ReportEntry entry : entries) {
            out.append("### ").append(entry.title()).append("\n\n");
            out.append("- 网站耗时：").append(entry.latencyMs()).append(" ms\n");
            out.append("- 网站状态：").append(entry.websiteStatus()).append("，fallback=").append(entry.fallbackUsed()).append("\n");
            out.append("- 失败原因：").append(entry.websiteFailureReason()).append("\n");
            out.append("- 网站摘要：").append(entry.websiteSummary()).append("\n");
            out.append("- 网站基础建议：").append(entry.websiteBasicAdvice()).append("\n");
            out.append("- 网站提高建议：").append(entry.websiteImprovementAdvice()).append("\n");
            out.append("- 网站下一步：").append(entry.websiteNextAction()).append("\n");
            out.append("- Codex 主因：").append(entry.codex().rootCause()).append("\n");
            out.append("- Codex 下一步：").append(entry.codex().nextAction()).append("\n");
            out.append("- 对照：").append(entry.comparison()).append("\n\n");
        }
        Files.writeString(reportPath, out.toString());
        return reportPath;
    }

    private static String comparison(RealSample sample, SubmissionAnalysisResponse analysis, CodexSimulation codex) {
        SubmissionAnalysisResponse.AiInvocation invocation = analysis.getAiInvocation();
        String websiteFit = invocation == null ? "" : safe(invocation.getDiagnosisLibraryFit());
        boolean fitCompatible = websiteFit.isBlank() || websiteFit.equalsIgnoreCase(sample.expectedLibraryFit())
                || ("PARTIAL".equals(websiteFit) && "HIT".equals(sample.expectedLibraryFit()))
                || ("MISS".equals(websiteFit) && sample.shouldGenerateGrowthCandidate());
        boolean hasStudentAction = analysis.getStudentFeedback() != null
                && analysis.getStudentFeedback().getNextLearningAction() != null
                && !safe(analysis.getStudentFeedback().getNextLearningAction().getTask()).isBlank();
        boolean safe = !"HIGH".equalsIgnoreCase(safe(analysis.getAnswerLeakRisk()));
        if (invocation == null || invocation.isFallbackUsed()) {
            return "网站线未完成模型诊断，Codex 线仅作目标参照。";
        }
        if (fitCompatible && hasStudentAction && safe) {
            return "方向基本一致，可继续人工复核表达颗粒度。";
        }
        List<String> gaps = new ArrayList<>();
        if (!fitCompatible) {
            gaps.add("libraryFit 不一致");
        }
        if (!hasStudentAction) {
            gaps.add("下一步动作不足");
        }
        if (!safe) {
            gaps.add("安全风险偏高");
        }
        return "存在差距：" + String.join("、", gaps);
    }

    private static String languageName(String language) {
        return "CPP17".equalsIgnoreCase(language) ? "C++17" : "Python 3";
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static long longEnv(String name, long fallback) {
        try {
            return Long.parseLong(env(name, Long.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void waitBetweenCases(int index, long delayMs) throws InterruptedException {
        if (index > 0 && delayMs > 0) {
            Thread.sleep(delayMs);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String excerpt(String value, int maxLength) {
        String safe = safe(value).replace('\n', ' ');
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength - 1) + "…";
    }

    private static String escape(String value) {
        return safe(value).replace("|", "\\|").replace("\n", " ");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RealSample(
            String id,
            String title,
            String problemSummary,
            String language,
            String buggyCode,
            String judgeResult,
            String expectedLibraryFit,
            List<String> expectedAnchors,
            List<String> expectedStudentFeedbackQuality,
            boolean shouldGenerateGrowthCandidate,
            String quickFeedbackKnownLimit,
            List<String> formalMustImproveAtLeastOneOf
    ) {
    }

    private record SimulationFixture(
            Problem problem,
            Submission submission,
            List<SubmissionCaseResult> caseResults,
            SubmissionAnalysisResponse baseline
    ) {
    }

    private record FailureExample(String input, String actual, String expected) {
    }

    private record CodexSimulation(
            String rootCause,
            String libraryFit,
            int basicAdviceCount,
            int improvementAdviceCount,
            String nextAction
    ) {
    }

    private record ReportEntry(
            String title,
            long latencyMs,
            String websiteStatus,
            boolean fallbackUsed,
            String websiteFailureReason,
            String websiteLibraryFit,
            int websiteAdviceCount,
            String websiteSummary,
            String websiteBasicAdvice,
            String websiteImprovementAdvice,
            String websiteNextAction,
            CodexSimulation codex,
            String comparison
    ) {
        static ReportEntry from(RealSample sample,
                                SubmissionAnalysisResponse analysis,
                                long latencyMs,
                                CodexSimulation codex) {
            SubmissionAnalysisResponse.AiInvocation invocation = analysis.getAiInvocation();
            int basicCount = invocation == null || invocation.getBasicAdviceCount() == null
                    ? safeList(analysis.getBasicLayerAdvice()).size()
                    : invocation.getBasicAdviceCount();
            int improvementCount = invocation == null || invocation.getImprovementAdviceCount() == null
                    ? safeList(analysis.getImprovementLayerAdvice()).size()
                    : invocation.getImprovementAdviceCount();
            String nextAction = "";
            if (analysis.getStudentFeedback() != null && analysis.getStudentFeedback().getNextLearningAction() != null) {
                nextAction = excerpt(analysis.getStudentFeedback().getNextLearningAction().getTask(), 140);
            }
            String basicAdvice = safeList(analysis.getBasicLayerAdvice()).stream()
                    .map(item -> excerpt(item.getTitle() + "：" + item.getStudentAction(), 120))
                    .findFirst()
                    .orElse("");
            String improvementAdvice = safeList(analysis.getImprovementLayerAdvice()).stream()
                    .map(item -> excerpt(item.getTitle() + "：" + item.getSuggestion(), 120))
                    .findFirst()
                    .orElse("");
            String status = invocation == null ? analysis.getSourceType() : invocation.getStatus();
            String libraryFit = invocation == null ? "" : invocation.getDiagnosisLibraryFit();
            boolean fallback = invocation == null || invocation.isFallbackUsed();
            String failureReason = invocation == null
                    ? ""
                    : safe(invocation.getFailureStage()) + "/" + safe(invocation.getFailureReason());
            ReportEntry draft = new ReportEntry(
                    sample.title(),
                    latencyMs,
                    status,
                    fallback,
                    failureReason,
                    safe(libraryFit),
                    basicCount + improvementCount,
                    excerpt(analysis.getSummary(), 180),
                    basicAdvice,
                    improvementAdvice,
                    nextAction,
                    codex,
                    ""
            );
            return new ReportEntry(
                    draft.title(),
                    draft.latencyMs(),
                    draft.websiteStatus(),
                    draft.fallbackUsed(),
                    draft.websiteFailureReason(),
                    draft.websiteLibraryFit(),
                    draft.websiteAdviceCount(),
                    draft.websiteSummary(),
                    draft.websiteBasicAdvice(),
                    draft.websiteImprovementAdvice(),
                    draft.websiteNextAction(),
                    draft.codex(),
                    DiagnosisReportV2RealSamplesSimulationTest.comparison(sample, analysis, codex)
            );
        }

        private static <T> List<T> safeList(List<T> values) {
            return values == null ? List.of() : values;
        }
    }
}

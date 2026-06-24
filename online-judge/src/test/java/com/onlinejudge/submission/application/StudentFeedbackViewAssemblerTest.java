package com.onlinejudge.submission.application;

import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class StudentFeedbackViewAssemblerTest {

    private final StudentFeedbackViewAssembler assembler = new StudentFeedbackViewAssembler();

    @Test
    void buildsStudentFacingViewFromStructuredFeedback() {
        SubmissionAnalysisResponse.StudentFeedbackView view = assembler.assemble(analysis(false));

        assertThat(view).isNotNull();
        assertThat(view.getStatus()).isEqualTo("READY");
        assertThat(view.getRepairItems()).singleElement().satisfies(item -> {
            assertThat(item.getTitle()).isEqualTo("输入没有完整读取");
            assertThat(item.getBody()).contains("公开样例");
            assertThat(item.getBody()).doesNotContain("当前最需要", "本地可验证反馈");
        });
        assertThat(view.getImprovementItems()).singleElement().satisfies(item -> {
            assertThat(item.getTitle()).isEqualTo("测试习惯");
            assertThat(item.getBody()).contains("自测");
        });
        assertThat(view.getNextQuestion()).contains("第二次读取");
        assertThat(view.getEvidenceRefs()).contains("case:first_failed", "rule:input");
    }

    @Test
    void hidesViewWhenModelFellBack() {
        assertThat(assembler.assemble(analysis(true))).isNull();
    }

    @Test
    void v2NaturalReportKeepsBasicTextAndNextActionSeparate() {
        SubmissionAnalysisResponse analysis = analysis(false);
        analysis.getAiInvocation().setPromptVersion(PromptTemplateRegistry.DIAGNOSIS_REPORT_V2);
        analysis.getStudentFeedback().getBlockingIssues().get(0)
                .setStudentMessage("基础层：循环范围和题目边界要求没有完全对齐。先手推最小样例，确认端点是否进入循环。");
        analysis.getStudentFeedback().getBlockingIssues().get(0)
                .setNextAction("下一步：用 n=1 和 n=2 写出循环变量序列，再和题目要求逐项对照。");
        analysis.getStudentFeedback().getImprovementOpportunities().get(0)
                .setStudentMessage("提高层：修好后把最小值、端点值、最大值附近样例加入固定自测清单。");

        SubmissionAnalysisResponse.StudentFeedbackView view = assembler.assemble(analysis);

        assertThat(view.getRepairItems()).singleElement()
                .satisfies(item -> assertThat(item.getBody())
                        .contains("基础层：循环范围")
                        .doesNotContain("下一步："));
        assertThat(view.getPrimaryAction()).contains("基础层：循环范围");
        assertThat(view.getNextQuestion()).contains("第二次读取");
    }

    @Test
    void filtersUnsafeOrNoisyItems() {
        SubmissionAnalysisResponse analysis = analysis(false);
        analysis.getStudentFeedback().getBlockingIssues().get(0).setNextAction("直接改成完整代码如下");
        analysis.getStudentFeedback().getImprovementOpportunities().get(0).setStudentMessage("答案如下，替换为正确写法");

        SubmissionAnalysisResponse.StudentFeedbackView view = assembler.assemble(analysis);

        assertThat(view).isNotNull();
        assertThat(view.getRepairItems()).isEmpty();
        assertThat(view.getImprovementItems()).isEmpty();
        assertThat(view.getPrimaryAction()).contains("输入结构");
        assertThat(view.getNextQuestion()).contains("第二次读取");
        assertThat(viewText(view)).doesNotContain("完整代码", "答案如下", "替换为");
    }

    @Test
    void remainsStableUnderConcurrentAssembly() throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<SubmissionAnalysisResponse.StudentFeedbackView>> jobs = IntStream.range(0, 160)
                    .mapToObj(index -> (Callable<SubmissionAnalysisResponse.StudentFeedbackView>) () -> assembler.assemble(analysis(index % 5 == 0)))
                    .toList();

            List<SubmissionAnalysisResponse.StudentFeedbackView> views = executor.invokeAll(jobs)
                    .stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertThat(views).filteredOn(view -> view == null).hasSize(32);
            assertThat(views).filteredOn(view -> view != null).allSatisfy(view -> {
                assertThat(view.getStatus()).isEqualTo("READY");
                assertThat(viewText(view)).doesNotContain("本地可验证反馈", "完整代码", "答案如下");
            });
        } finally {
            executor.shutdownNow();
        }
    }

    private SubmissionAnalysisResponse analysis(boolean fallbackUsed) {
        return SubmissionAnalysisResponse.builder()
                .submissionId(1L)
                .evidenceRefs(List.of("case:first_failed"))
                .aiInvocation(SubmissionAnalysisResponse.AiInvocation.builder()
                        .status(fallbackUsed ? "MODEL_RUNTIME_FALLBACK" : "MODEL_COMPLETED")
                        .fallbackUsed(fallbackUsed)
                        .build())
                .studentFeedback(SubmissionAnalysisResponse.StudentFeedback.builder()
                        .summary("当前先给出本地可验证反馈，外接模型结果未作为学生端结论。")
                        .blockingIssues(List.of(SubmissionAnalysisResponse.FeedbackIssue.builder()
                                .title("输入没有完整读取")
                                .studentMessage("当前最需要先处理的问题是输入读取。")
                                .nextAction("先把公开样例的每一行 input 手推清楚。")
                                .issueTag("IO_FORMAT")
                                .fineGrainedTag("INPUT_PARSING")
                                .evidenceRefs(List.of("rule:input"))
                                .build()))
                        .improvementOpportunities(List.of(SubmissionAnalysisResponse.ImprovementOpportunity.builder()
                                .category("TESTING_HABIT")
                                .studentMessage("补一个和样例结构不同的自测。")
                                .benefit("自测能更早发现输入格式问题。")
                                .evidenceRefs(List.of("case:first_failed"))
                                .build()))
                        .nextLearningAction(SubmissionAnalysisResponse.NextLearningAction.builder()
                                .task("先确认输入结构。")
                                .checkQuestion("第二次读取时应该拿到什么？")
                                .evidenceRefs(List.of("rule:input"))
                                .answerLeakRisk("LOW")
                                .build())
                        .build())
                .build();
    }

    private String viewText(SubmissionAnalysisResponse.StudentFeedbackView view) {
        StringBuilder builder = new StringBuilder();
        if (view.getRepairItems() != null) {
            view.getRepairItems().forEach(item -> builder.append(item.getTitle()).append(item.getBody()));
        }
        if (view.getImprovementItems() != null) {
            view.getImprovementItems().forEach(item -> builder.append(item.getTitle()).append(item.getBody()));
        }
        builder.append(view.getPrimaryAction()).append(view.getNextQuestion());
        return builder.toString();
    }
}

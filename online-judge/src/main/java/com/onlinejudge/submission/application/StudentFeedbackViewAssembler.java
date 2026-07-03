package com.onlinejudge.submission.application;

import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class StudentFeedbackViewAssembler {

    private static final String RULE_FALLBACK_NOTICE = "外部 AI 暂不可用，下面是规则诊断。";

    public SubmissionAnalysisResponse.StudentFeedbackView assemble(SubmissionAnalysisResponse analysis) {
        if (analysis == null) {
            return null;
        }
        SubmissionAnalysisResponse.StudentFeedback feedback = analysis.getStudentFeedback();
        if (feedback == null) {
            return null;
        }

        List<SubmissionAnalysisResponse.FeedbackViewItem> repairItems = repairItems(feedback, analysis);
        List<SubmissionAnalysisResponse.FeedbackViewItem> improvementItems = improvementItems(feedback);
        String nextQuestion = clean(feedback.getNextLearningAction() == null
                ? null
                : feedback.getNextLearningAction().getCheckQuestion());

        if (repairItems.isEmpty() && improvementItems.isEmpty() && nextQuestion.isBlank()) {
            return null;
        }

        String status = viewStatus(analysis);
        if ("RULE_FALLBACK".equals(status)) {
            repairItems = withFallbackNotice(repairItems);
        }

        return SubmissionAnalysisResponse.StudentFeedbackView.builder()
                .status(status)
                .primaryAction(primaryAction(feedback, repairItems, nextQuestion))
                .repairItems(repairItems)
                .improvementItems(improvementItems)
                .nextQuestion(nextQuestion.isBlank() ? null : nextQuestion)
                .evidenceRefs(deduplicate(merge(
                        analysis.getEvidenceRefs(),
                        feedback.getNextLearningAction() == null ? List.of() : feedback.getNextLearningAction().getEvidenceRefs()
                )))
                .build();
    }

    private String viewStatus(SubmissionAnalysisResponse analysis) {
        SubmissionAnalysisResponse.AiInvocation invocation = analysis.getAiInvocation();
        if (invocation == null) {
            return "READY";
        }
        String status = safe(invocation.getStatus()).toUpperCase(Locale.ROOT);
        if (invocation.isFallbackUsed() || status.contains("FALLBACK")) {
            return "RULE_FALLBACK";
        }
        return "READY";
    }

    private List<SubmissionAnalysisResponse.FeedbackViewItem> withFallbackNotice(List<SubmissionAnalysisResponse.FeedbackViewItem> repairItems) {
        if (repairItems == null || repairItems.isEmpty()) {
            return List.of(SubmissionAnalysisResponse.FeedbackViewItem.builder()
                    .title("规则诊断")
                    .body(RULE_FALLBACK_NOTICE)
                    .kind("RULE_FALLBACK")
                    .evidenceRefs(List.of())
                    .build());
        }
        SubmissionAnalysisResponse.FeedbackViewItem first = repairItems.get(0);
        String body = safe(first.getBody());
        if (body.startsWith(RULE_FALLBACK_NOTICE)) {
            return repairItems;
        }
        List<SubmissionAnalysisResponse.FeedbackViewItem> updated = new ArrayList<>(repairItems);
        updated.set(0, SubmissionAnalysisResponse.FeedbackViewItem.builder()
                .title(first.getTitle())
                .body(RULE_FALLBACK_NOTICE + body)
                .kind(first.getKind())
                .evidenceRefs(first.getEvidenceRefs())
                .build());
        return List.copyOf(updated);
    }

    private boolean isDiagnosisReportV2(SubmissionAnalysisResponse analysis) {
        SubmissionAnalysisResponse.AiInvocation invocation = analysis == null ? null : analysis.getAiInvocation();
        return invocation != null
                && PromptTemplateRegistry.DIAGNOSIS_REPORT_V2.equals(safe(invocation.getPromptVersion()).trim());
    }

    private List<SubmissionAnalysisResponse.FeedbackViewItem> repairItems(SubmissionAnalysisResponse.StudentFeedback feedback,
                                                                          SubmissionAnalysisResponse analysis) {
        List<SubmissionAnalysisResponse.FeedbackViewItem> items = new ArrayList<>();
        List<SubmissionAnalysisResponse.FeedbackIssue> issues = feedback.getBlockingIssues() == null
                ? List.of()
                : feedback.getBlockingIssues();
        for (SubmissionAnalysisResponse.FeedbackIssue issue : issues) {
            if (items.size() >= 1) {
                break;
            }
            String title = cleanTitle(issue.getTitle());
            boolean diagnosisReportV2 = isDiagnosisReportV2(analysis);
            String body = clean(defaultIfBlank(
                    diagnosisReportV2 ? issue.getStudentMessage() : issue.getNextAction(),
                    diagnosisReportV2 ? issue.getNextAction() : issue.getStudentMessage()
            ));
            if (body.isBlank()) {
                body = clean(issue.getEvidence());
            }
            if (body.isBlank()) {
                continue;
            }
            items.add(SubmissionAnalysisResponse.FeedbackViewItem.builder()
                    .title(defaultIfBlank(title, "修正方向"))
                    .body(body)
                    .kind(defaultIfBlank(issue.getFineGrainedTag(), issue.getIssueTag()))
                    .evidenceRefs(deduplicate(merge(issue.getEvidenceRefs(), analysis.getEvidenceRefs())))
                    .build());
        }
        return items;
    }

    private List<SubmissionAnalysisResponse.FeedbackViewItem> improvementItems(SubmissionAnalysisResponse.StudentFeedback feedback) {
        List<SubmissionAnalysisResponse.FeedbackViewItem> items = new ArrayList<>();
        List<SubmissionAnalysisResponse.ImprovementOpportunity> opportunities = feedback.getImprovementOpportunities() == null
                ? List.of()
                : feedback.getImprovementOpportunities();
        for (SubmissionAnalysisResponse.ImprovementOpportunity opportunity : opportunities) {
            if (items.size() >= 2) {
                break;
            }
            String body = clean(defaultIfBlank(opportunity.getStudentMessage(), opportunity.getBenefit()));
            if (body.isBlank()) {
                continue;
            }
            String benefit = clean(opportunity.getBenefit());
            if (!benefit.isBlank() && !sameText(body, benefit)) {
                body = body + " " + benefit;
            }
            items.add(SubmissionAnalysisResponse.FeedbackViewItem.builder()
                    .title(defaultIfBlank(cleanTitle(opportunity.getTitle()), improvementTitle(opportunity.getCategory())))
                    .body(body)
                    .kind(defaultIfBlank(opportunity.getCategory(), "IMPROVEMENT"))
                    .evidenceRefs(deduplicate(opportunity.getEvidenceRefs()))
                    .build());
        }
        return items;
    }

    private String primaryAction(SubmissionAnalysisResponse.StudentFeedback feedback,
                                 List<SubmissionAnalysisResponse.FeedbackViewItem> repairItems,
                                 String nextQuestion) {
        if (!repairItems.isEmpty()) {
            return repairItems.get(0).getBody();
        }
        String task = feedback.getNextLearningAction() == null ? "" : clean(feedback.getNextLearningAction().getTask());
        if (!task.isBlank()) {
            return task;
        }
        return nextQuestion.isBlank() ? null : nextQuestion;
    }

    private String cleanTitle(String value) {
        String text = clean(value);
        String compact = text.replaceAll("\\s+", "");
        if (compact.isBlank()
                || compact.equals("当前问题")
                || compact.equals("当前失败原因")
                || compact.contains("当前最需要")
                || compact.contains("当前最重要")
                || compact.contains("主要问题")
                || compact.contains("先改这里")) {
            return "";
        }
        return text;
    }

    private String clean(String value) {
        String text = safe(value).trim()
                .replaceFirst("^当前代码存在运行稳定性风险[，,。]?\\s*", "")
                .replaceFirst("^先保证程序稳定运行[，,。]?\\s*", "")
                .replaceFirst("^当前先[^。]{0,24}。\\s*", "")
                .replace("当前先给出本地可验证反馈，外接模型结果未作为学生端结论。", "")
                .replace("本地可验证反馈", "")
                .trim();
        if (looksUnsafe(text)) {
            return "";
        }
        return text;
    }

    private boolean looksUnsafe(String text) {
        String compact = safe(text).replaceAll("\\s+", "");
        return ModelOutputSafetyPolicy.containsUnsafeLeak(text)
                || compact.contains("完整代码")
                || compact.contains("参考代码")
                || compact.contains("答案如下")
                || compact.contains("直接改成")
                || compact.contains("替换为");
    }

    private String improvementTitle(String category) {
        return switch (safe(category).trim().toUpperCase(Locale.ROOT)) {
            case "COMPLEXITY" -> "复杂度";
            case "CODE_CLARITY" -> "代码结构";
            case "BOUNDARY_AWARENESS" -> "边界意识";
            case "DEBUG_CLEANUP" -> "调试清理";
            case "TESTING_HABIT" -> "测试习惯";
            default -> "进阶";
        };
    }

    private boolean sameText(String left, String right) {
        return safe(left).replaceAll("\\s+", "").equals(safe(right).replaceAll("\\s+", ""));
    }

    private String defaultIfBlank(String value, String fallback) {
        return safe(value).isBlank() ? safe(fallback) : value;
    }

    private List<String> merge(List<String> left, List<String> right) {
        List<String> merged = new ArrayList<>();
        if (left != null) {
            merged.addAll(left);
        }
        if (right != null) {
            merged.addAll(right);
        }
        return merged;
    }

    private List<String> deduplicate(List<String> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String item : source) {
            String value = safe(item).trim();
            if (!value.isBlank()) {
                seen.add(value);
            }
        }
        return List.copyOf(seen);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

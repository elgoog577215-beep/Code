package com.onlinejudge.submission.application;

import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdviceGenerationFeedbackMapper {

    public SubmissionAnalysisResponse.StudentFeedback toStudentFeedback(AdviceGenerationOutput output,
                                                                        ExternalModelStagePayloads.DiagnosisJudgeOutput decision,
                                                                        StandardLibraryPack standardLibraryPack) {
        if (output == null) {
            return null;
        }
        List<SubmissionAnalysisResponse.FeedbackIssue> blockingIssues = safe(output.getBasicLayerAdvice()).stream()
                .filter(item -> item != null)
                .map(item -> SubmissionAnalysisResponse.FeedbackIssue.builder()
                        .priority(priorityOf(output.getBasicLayerAdvice(), item))
                        .title(defaultIfBlank(item.getTitle(), "基础层问题"))
                        .studentMessage(joinNonBlank(item.getWhatHappened(), item.getWhyItMatters()))
                        .evidence(firstOrDefault(item.getEvidenceRefs(), "模型引用了当前提交证据。"))
                        .nextAction(item.getStudentAction())
                        .issueTag(decision == null ? null : decision.getPrimaryIssueTag())
                        .fineGrainedTag(decision == null ? null : decision.getFineGrainedTag())
                        .evidenceRefs(item.getEvidenceRefs())
                        .build())
                .toList();
        List<SubmissionAnalysisResponse.ImprovementOpportunity> improvementOpportunities =
                safe(output.getImprovementLayerAdvice()).stream()
                        .filter(item -> item != null)
                        .map(item -> SubmissionAnalysisResponse.ImprovementOpportunity.builder()
                                .category(toImprovementCategory(item, standardLibraryPack))
                                .studentMessage(defaultIfBlank(item.getSuggestion(), item.getCurrentLimit()))
                                .benefit(defaultIfBlank(item.getStudentBenefit(), "帮助你把同类问题迁移到下一题。"))
                                .evidenceRefs(item.getEvidenceRefs())
                                .build())
                        .toList();
        AdviceGenerationOutput.NextStepAdvice firstStep = safe(output.getNextStepPlan()).stream()
                .filter(item -> item != null)
                .findFirst()
                .orElse(null);
        return SubmissionAnalysisResponse.StudentFeedback.builder()
                .summary(defaultIfBlank(output.getStudentSummary(), "先处理当前最明确的基础层问题。"))
                .blockingIssues(blockingIssues)
                .secondaryIssues(List.of())
                .improvementOpportunities(improvementOpportunities)
                .nextLearningAction(SubmissionAnalysisResponse.NextLearningAction.builder()
                        .hintLevel("L2")
                        .action(firstTeachingAction(standardLibraryPack))
                        .task(firstStep == null ? firstBasicAction(output) : firstStep.getTarget())
                        .checkQuestion(firstBasicQuestion(output))
                        .evidenceRefs(firstStep == null || firstStep.getEvidenceRef() == null || firstStep.getEvidenceRef().isBlank()
                                ? firstBasicEvidenceRefs(output)
                                : List.of(firstStep.getEvidenceRef()))
                        .answerLeakRisk("LOW")
                        .build())
                .build();
    }

    public ExternalModelStagePayloads.DiagnosisJudgeOutput toDiagnosisDecision(AdviceGenerationOutput output,
                                                                               SubmissionAnalysisResponse fallback) {
        List<String> issueTags = fallback == null ? List.of() : safe(fallback.getIssueTags());
        List<String> fineTags = fallback == null ? List.of() : safe(fallback.getFineGrainedTags());
        List<String> evidenceRefs = primaryEvidenceRefs(output, fallback);
        return ExternalModelStagePayloads.DiagnosisJudgeOutput.builder()
                .primaryIssueTag(firstOrDefault(issueTags, "NEEDS_MORE_EVIDENCE"))
                .fineGrainedTag(firstOrDefault(fineTags, ""))
                .evidenceRefs(evidenceRefs)
                .primaryReasoning(primaryReasoning(output))
                .secondaryIssues(List.of())
                .distractorNotes(List.of())
                .teachingPriority(defaultIfBlank(output == null ? "" : output.getStudentSummary(),
                        "先处理当前最明确的基础层问题。"))
                .improvementOpportunities(List.of())
                .nextLearningAction(SubmissionAnalysisResponse.NextLearningAction.builder()
                        .hintLevel("L2")
                        .action("COLLECT_EVIDENCE")
                        .task(firstStepTarget(output))
                        .checkQuestion(firstBasicQuestion(output))
                        .evidenceRefs(evidenceRefs)
                        .answerLeakRisk("LOW")
                        .build())
                .confidence(firstBasicConfidence(output))
                .uncertainty("advice-generation-v1 已给出结构化建议。")
                .needsMoreEvidence(false)
                .answerLeakRisk("LOW")
                .build();
    }

    public ExternalModelStagePayloads.TeachingHintOutput toTeachingHint(AdviceGenerationOutput output,
                                                                        SubmissionAnalysisResponse fallback,
                                                                        StandardLibraryPack standardLibraryPack) {
        AdviceGenerationOutput.NextStepAdvice firstStep = safe(output == null ? null : output.getNextStepPlan()).stream()
                .filter(item -> item != null)
                .findFirst()
                .orElse(null);
        String evidence = firstStep == null ? firstOrDefault(firstBasicEvidenceRefs(output), "model:advice_generation") : firstStep.getEvidenceRef();
        String target = firstStep == null ? firstBasicAction(output) : firstStep.getTarget();
        String teachingAction = firstTeachingAction(standardLibraryPack);
        List<String> evidenceRefs = firstStep == null || firstStep.getEvidenceRef() == null || firstStep.getEvidenceRef().isBlank()
                ? firstBasicEvidenceRefs(output)
                : List.of(firstStep.getEvidenceRef());
        return ExternalModelStagePayloads.TeachingHintOutput.builder()
                .studentHint(defaultIfBlank(target, fallback == null ? "" : fallback.getStudentHint()))
                .studentHintPlan(SubmissionAnalysisResponse.StudentHintPlan.builder()
                        .hintLevel("L2")
                        .problemType("基础层诊断")
                        .evidenceAnchor(defaultIfBlank(evidence, "model:advice_generation"))
                        .nextAction(defaultIfBlank(target, "先复核模型指出的证据。"))
                        .coachQuestion(firstBasicQuestion(output))
                        .teachingAction(teachingAction)
                        .evidenceRefs(evidenceRefs)
                        .answerLeakRisk("LOW")
                        .build())
                .learningInterventionPlan(SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                        .interventionType("ADVICE_GENERATION")
                        .goal("完成当前最小可观察诊断动作。")
                        .studentTask(defaultIfBlank(target, "复核证据。"))
                        .checkQuestion(firstBasicQuestion(output))
                        .completionSignal("学生能说出证据和代码行为之间的差距。")
                        .evidenceRefs(evidenceRefs)
                        .estimatedMinutes(6)
                        .answerLeakRisk("LOW")
                        .build())
                .teacherNote("外部模型已生成基础层与提高层结构化建议。")
                .answerLeakRisk("LOW")
                .build();
    }

    private String primaryReasoning(AdviceGenerationOutput output) {
        AdviceGenerationOutput.BasicLayerAdvice first = firstBasic(output);
        if (first != null) {
            return joinNonBlank(first.getWhatHappened(), first.getWhyItMatters());
        }
        return output == null || output.getCaseUnderstanding() == null
                ? "模型基于当前证据生成了结构化建议。"
                : output.getCaseUnderstanding().getBehaviorGap();
    }

    private List<String> primaryEvidenceRefs(AdviceGenerationOutput output, SubmissionAnalysisResponse fallback) {
        AdviceGenerationOutput.BasicLayerAdvice first = firstBasic(output);
        if (first != null && first.getEvidenceRefs() != null && !first.getEvidenceRefs().isEmpty()) {
            return first.getEvidenceRefs();
        }
        if (output != null && output.getCaseUnderstanding() != null
                && output.getCaseUnderstanding().getPrimaryEvidenceRef() != null
                && !output.getCaseUnderstanding().getPrimaryEvidenceRef().isBlank()) {
            return List.of(output.getCaseUnderstanding().getPrimaryEvidenceRef());
        }
        return fallback == null ? List.of() : safe(fallback.getEvidenceRefs());
    }

    private AdviceGenerationOutput.BasicLayerAdvice firstBasic(AdviceGenerationOutput output) {
        return safe(output == null ? null : output.getBasicLayerAdvice()).stream()
                .filter(item -> item != null)
                .findFirst()
                .orElse(null);
    }

    private String firstBasicAction(AdviceGenerationOutput output) {
        AdviceGenerationOutput.BasicLayerAdvice first = firstBasic(output);
        return first == null ? "先复核模型指出的证据。" : first.getStudentAction();
    }

    private String firstBasicQuestion(AdviceGenerationOutput output) {
        AdviceGenerationOutput.BasicLayerAdvice first = firstBasic(output);
        return first == null ? "这条证据说明了什么代码行为？" : first.getCheckQuestion();
    }

    private Double firstBasicConfidence(AdviceGenerationOutput output) {
        AdviceGenerationOutput.BasicLayerAdvice first = firstBasic(output);
        return first == null || first.getConfidence() == null ? 0.7 : first.getConfidence();
    }

    private List<String> firstBasicEvidenceRefs(AdviceGenerationOutput output) {
        AdviceGenerationOutput.BasicLayerAdvice first = firstBasic(output);
        if (first != null && first.getEvidenceRefs() != null && !first.getEvidenceRefs().isEmpty()) {
            return first.getEvidenceRefs();
        }
        if (output != null && output.getCaseUnderstanding() != null
                && output.getCaseUnderstanding().getPrimaryEvidenceRef() != null
                && !output.getCaseUnderstanding().getPrimaryEvidenceRef().isBlank()) {
            return List.of(output.getCaseUnderstanding().getPrimaryEvidenceRef());
        }
        return List.of();
    }

    private String firstStepTarget(AdviceGenerationOutput output) {
        return safe(output == null ? null : output.getNextStepPlan()).stream()
                .filter(item -> item != null && item.getTarget() != null && !item.getTarget().isBlank())
                .map(AdviceGenerationOutput.NextStepAdvice::getTarget)
                .findFirst()
                .orElse(firstBasicAction(output));
    }

    private String toImprovementCategory(AdviceGenerationOutput.ImprovementLayerAdvice item,
                                         StandardLibraryPack standardLibraryPack) {
        if (standardLibraryPack != null && standardLibraryPack.getImprovementTags() != null
                && !standardLibraryPack.getImprovementTags().isEmpty()) {
            return standardLibraryPack.getImprovementTags().get(0).getId();
        }
        return defaultIfBlank(item.getImprovementPointId(), "TRANSFER_REVIEW");
    }

    private int priorityOf(List<AdviceGenerationOutput.BasicLayerAdvice> values,
                           AdviceGenerationOutput.BasicLayerAdvice item) {
        int index = values == null ? -1 : values.indexOf(item);
        return index < 0 ? 1 : index + 1;
    }

    private String firstTeachingAction(StandardLibraryPack standardLibraryPack) {
        if (standardLibraryPack == null || standardLibraryPack.getTeachingActions() == null) {
            return "COLLECT_EVIDENCE";
        }
        return standardLibraryPack.getTeachingActions().stream()
                .filter(item -> item != null && item.getId() != null && !item.getId().isBlank())
                .map(StandardLibraryPack.TeachingActionOption::getId)
                .findFirst()
                .orElse("COLLECT_EVIDENCE");
    }

    private String joinNonBlank(String first, String second) {
        if (first == null || first.isBlank()) {
            return defaultIfBlank(second, "");
        }
        if (second == null || second.isBlank()) {
            return first;
        }
        return first + " " + second;
    }

    private String firstOrDefault(List<String> values, String fallback) {
        if (values == null) {
            return fallback;
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(fallback);
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

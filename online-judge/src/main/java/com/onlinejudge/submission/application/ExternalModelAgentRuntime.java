package com.onlinejudge.submission.application;

import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ExternalModelAgentRuntime {

    public static final String RUNTIME_PROFILE_STANDARD = "standard";
    public static final String RUNTIME_PROFILE_LOW_LATENCY = "low-latency";

    private static final int COMPACT_PROBLEM_BRIEF_LENGTH = 260;
    private static final int COMPACT_PROBLEM_CONSTRAINTS_LENGTH = 220;
    private static final int COMPACT_CODE_EXCERPT_LENGTH = 1400;
    private static final int COMPACT_SIGNAL_REASON_LENGTH = 120;
    private static final int COMPACT_TRAJECTORY_LENGTH = 240;
    private static final int COMPACT_UNCERTAINTY_LENGTH = 220;

    private final ModelDiagnosisBriefBuilder briefBuilder;
    private final StandardLibraryPackBuilder standardLibraryPackBuilder;
    private final PromptTemplateRegistry promptTemplateRegistry;
    private final ModelOutputValidator modelOutputValidator;
    private final ExternalModelOutputNormalizer outputNormalizer;

    public ExternalModelAgentRuntime(ModelDiagnosisBriefBuilder briefBuilder,
                                     StandardLibraryPackBuilder standardLibraryPackBuilder,
                                     PromptTemplateRegistry promptTemplateRegistry,
                                     ModelOutputValidator modelOutputValidator) {
        this(briefBuilder, standardLibraryPackBuilder, promptTemplateRegistry, modelOutputValidator,
                new ExternalModelOutputNormalizer());
    }

    @Autowired
    public ExternalModelAgentRuntime(ModelDiagnosisBriefBuilder briefBuilder,
                                     StandardLibraryPackBuilder standardLibraryPackBuilder,
                                     PromptTemplateRegistry promptTemplateRegistry,
                                     ModelOutputValidator modelOutputValidator,
                                     ExternalModelOutputNormalizer outputNormalizer) {
        this.briefBuilder = briefBuilder;
        this.standardLibraryPackBuilder = standardLibraryPackBuilder;
        this.promptTemplateRegistry = promptTemplateRegistry;
        this.modelOutputValidator = modelOutputValidator;
        this.outputNormalizer = outputNormalizer == null ? new ExternalModelOutputNormalizer() : outputNormalizer;
    }

    public RuntimePlan prepare(DiagnosisEvidencePackage evidencePackage,
                               RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                               SubmissionAnalysisResponse fallback) {
        return prepare(evidencePackage, ruleSignals, fallback, RUNTIME_PROFILE_STANDARD);
    }

    public RuntimePlan prepare(DiagnosisEvidencePackage evidencePackage,
                               RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                               SubmissionAnalysisResponse fallback,
                               String runtimeProfile) {
        String normalizedProfile = normalizeRuntimeProfile(runtimeProfile);
        boolean compact = RUNTIME_PROFILE_LOW_LATENCY.equals(normalizedProfile);
        ModelDiagnosisBrief standardBrief = briefBuilder.build(evidencePackage, ruleSignals, fallback);
        ModelDiagnosisBrief brief = compact ? compactBrief(standardBrief) : standardBrief;
        StandardLibraryPack standardLibraryPack = standardLibraryPackBuilder.build(brief, ruleSignals);
        if (compact) {
            standardLibraryPack = compactStandardLibraryPack(standardLibraryPack);
        }
        return RuntimePlan.builder()
                .brief(brief)
                .standardLibraryPack(standardLibraryPack)
                .diagnosisPrompt(promptTemplateRegistry.get(PromptTemplateRegistry.DIAGNOSIS_JUDGE_V2))
                .teachingPrompt(promptTemplateRegistry.get(PromptTemplateRegistry.TEACHING_HINT_V1))
                .singleCallPrompt(promptTemplateRegistry.get(PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V3))
                .runtimeProfile(normalizedProfile)
                .requestCompact(compact)
                .build();
    }

    private String normalizeRuntimeProfile(String runtimeProfile) {
        if (runtimeProfile != null && RUNTIME_PROFILE_LOW_LATENCY.equalsIgnoreCase(runtimeProfile.trim())) {
            return RUNTIME_PROFILE_LOW_LATENCY;
        }
        return RUNTIME_PROFILE_STANDARD;
    }

    private ModelDiagnosisBrief compactBrief(ModelDiagnosisBrief source) {
        if (source == null) {
            return null;
        }
        return ModelDiagnosisBrief.builder()
                .schemaVersion(source.getSchemaVersion())
                .problemBrief(truncate(source.getProblemBrief(), COMPACT_PROBLEM_BRIEF_LENGTH))
                .problemConstraints(truncate(source.getProblemConstraints(), COMPACT_PROBLEM_CONSTRAINTS_LENGTH))
                .verdict(source.getVerdict())
                .language(source.getLanguage())
                .keyCodeExcerpt(truncate(source.getKeyCodeExcerpt(), COMPACT_CODE_EXCERPT_LENGTH))
                .sourceCodeLineCount(source.getSourceCodeLineCount())
                .firstFailedCase(compactFailedCase(source.getFirstFailedCase()))
                .visibleCaseFacts(compactVisibleCaseFacts(source.getVisibleCaseFacts()))
                .candidateSignals(compactCandidateSignals(source.getCandidateSignals()))
                .evidenceRefs(source.getEvidenceRefs())
                .allowedIssueTags(source.getAllowedIssueTags())
                .allowedFineGrainedTags(source.getAllowedFineGrainedTags())
                .learningTrajectorySummary(truncate(source.getLearningTrajectorySummary(), COMPACT_TRAJECTORY_LENGTH))
                .hiddenDataBoundary(source.getHiddenDataBoundary())
                .uncertainty(truncate(source.getUncertainty(), COMPACT_UNCERTAINTY_LENGTH))
                .build();
    }

    private SubmissionAnalysisResponse.FailedCaseSnapshot compactFailedCase(
            SubmissionAnalysisResponse.FailedCaseSnapshot source) {
        if (source == null) {
            return null;
        }
        return SubmissionAnalysisResponse.FailedCaseSnapshot.builder()
                .testCaseNumber(source.getTestCaseNumber())
                .hidden(source.isHidden())
                .input(source.isHidden() ? null : truncate(source.getInput(), 220))
                .expectedOutput(source.isHidden() ? null : truncate(source.getExpectedOutput(), 220))
                .actualOutput(source.isHidden() ? null : truncate(source.getActualOutput(), 220))
                .build();
    }

    private List<ModelDiagnosisBrief.VisibleCaseFact> compactVisibleCaseFacts(
            List<ModelDiagnosisBrief.VisibleCaseFact> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .limit(1)
                .map(item -> ModelDiagnosisBrief.VisibleCaseFact.builder()
                        .testCaseNumber(item.getTestCaseNumber())
                        .passed(item.getPassed())
                        .hidden(item.getHidden())
                        .actualOutputPreview(Boolean.TRUE.equals(item.getHidden())
                                ? null
                                : truncate(item.getActualOutputPreview(), 160))
                        .expectedOutputPreview(Boolean.TRUE.equals(item.getHidden())
                                ? null
                                : truncate(item.getExpectedOutputPreview(), 160))
                        .build())
                .toList();
    }

    private List<ModelDiagnosisBrief.CandidateSignal> compactCandidateSignals(
            List<ModelDiagnosisBrief.CandidateSignal> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .sorted(Comparator.comparing(
                        ModelDiagnosisBrief.CandidateSignal::getConfidence,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .map(signal -> ModelDiagnosisBrief.CandidateSignal.builder()
                        .evidenceRef(signal.getEvidenceRef())
                        .issueTag(signal.getIssueTag())
                        .fineGrainedTag(signal.getFineGrainedTag())
                        .confidence(signal.getConfidence())
                        .reason(truncate(signal.getReason(), COMPACT_SIGNAL_REASON_LENGTH))
                        .build())
                .toList();
    }

    private StandardLibraryPack compactStandardLibraryPack(StandardLibraryPack source) {
        if (source == null) {
            return null;
        }
        return StandardLibraryPack.builder()
                .schemaVersion(source.getSchemaVersion())
                .taxonomyVersion(source.getTaxonomyVersion())
                .issueTags(compactTags(source.getIssueTags()))
                .fineGrainedTags(compactTags(source.getFineGrainedTags()))
                .improvementTags(compactImprovementTags(source.getImprovementTags()))
                .teachingActions(compactTeachingActions(source.getTeachingActions()))
                .decisionProtocol(compactDecisionProtocol())
                .studentFeedbackRules(compactStudentFeedbackRules())
                .safetyRules(List.of(
                        "Use only provided tags, evidenceRefs and teachingActions.",
                        "Do not provide complete code, final answers or hidden test data.",
                        "Keep the student task diagnostic and evidence-grounded.",
                        "Separate blocking issues from improvement opportunities."
                ))
                .uncertaintyOptions(source.getUncertaintyOptions())
                .build();
    }

    private List<StandardLibraryPack.TagOption> compactTags(List<StandardLibraryPack.TagOption> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(tag -> StandardLibraryPack.TagOption.builder()
                        .id(tag.getId())
                        .label(tag.getLabel())
                        .parentTag(tag.getParentTag())
                        .teachingAction(tag.getTeachingAction())
                        .build())
                .toList();
    }

    private List<StandardLibraryPack.ImprovementTagOption> compactImprovementTags(
            List<StandardLibraryPack.ImprovementTagOption> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(tag -> StandardLibraryPack.ImprovementTagOption.builder()
                        .id(tag.getId())
                        .label(tag.getLabel())
                        .build())
                .toList();
    }

    private List<StandardLibraryPack.TeachingActionOption> compactTeachingActions(
            List<StandardLibraryPack.TeachingActionOption> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(action -> StandardLibraryPack.TeachingActionOption.builder()
                        .id(action.getId())
                        .label(action.getLabel())
                        .build())
                .toList();
    }

    private StandardLibraryPack.DecisionProtocol compactDecisionProtocol() {
        return StandardLibraryPack.DecisionProtocol.builder()
                .globalRules(List.of(
                        "Choose the narrowest evidence-supported diagnosis.",
                        "Hidden failures justify uncertainty, not guessed hidden data."
                ))
                .evidencePriorityRules(List.of(
                        "Current judge facts and source evidence outrank memory.",
                        "Candidate signals with evidenceRefs outrank broad summaries."
                ))
                .tagSelectionRules(List.of(
                        "Select tags only from this pack.",
                        "Cite evidenceRefs from the brief."
                ))
                .conflictRules(List.of(
                        "If evidence conflicts, choose NEEDS_MORE_EVIDENCE when available."
                ))
                .teachingActionRules(List.of(
                        "Bind teachingAction to the selected tag.",
                        "Ask for one observable trace, comparison, estimate or counterexample."
                ))
                .build();
    }

    private StandardLibraryPack.StudentFeedbackRules compactStudentFeedbackRules() {
        return StandardLibraryPack.StudentFeedbackRules.builder()
                .blockingIssueRules(List.of(
                        "First blocking issue must explain the current failed evidence.",
                        "Do not promote optional improvements to primary cause."
                ))
                .secondaryIssueRules(List.of(
                        "Mention secondary signals only when clearly not primary."
                ))
                .improvementRules(List.of(
                        "Use only improvementTags ids.",
                        "Improvements are follow-up learning value, not current root cause."
                ))
                .nextActionRules(List.of(
                        "Ask for one observable check without full solution code."
                ))
                .build();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 24)) + "\n...[truncated for model]";
    }

    public ExternalModelStagePayloads.StageValidationResult validateDiagnosisDecision(
            ExternalModelStagePayloads.DiagnosisJudgeOutput output,
            RuntimePlan runtimePlan) {
        return modelOutputValidator.validateDiagnosisJudgeOutput(
                output,
                runtimePlan == null ? null : runtimePlan.getBrief(),
                runtimePlan == null ? null : runtimePlan.getStandardLibraryPack()
        );
    }

    public ExternalModelStagePayloads.DiagnosisJudgeOutput normalizeDiagnosisDecision(
            ExternalModelStagePayloads.DiagnosisJudgeOutput output,
            RuntimePlan runtimePlan) {
        return outputNormalizer.normalizeDiagnosisDecision(output, runtimePlan);
    }

    public ExternalModelStagePayloads.TeachingHintOutput normalizeTeachingHint(
            ExternalModelStagePayloads.TeachingHintOutput output,
            RuntimePlan runtimePlan) {
        return outputNormalizer.normalizeTeachingHint(output, runtimePlan);
    }

    public ExternalModelStagePayloads.CombinedOutput normalizeCombinedOutput(
            ExternalModelStagePayloads.CombinedOutput output,
            RuntimePlan runtimePlan) {
        return outputNormalizer.normalizeCombinedOutput(output, runtimePlan);
    }

    public com.onlinejudge.submission.dto.SubmissionAnalysisResponse.StudentFeedback normalizeStudentFeedback(
            com.onlinejudge.submission.dto.SubmissionAnalysisResponse.StudentFeedback feedback,
            RuntimePlan runtimePlan) {
        return outputNormalizer.normalizeStudentFeedback(feedback, runtimePlan);
    }

    public ExternalModelStagePayloads.StageValidationResult validateTeachingHint(
            ExternalModelStagePayloads.TeachingHintOutput output,
            ExternalModelStagePayloads.DiagnosisJudgeOutput decision,
            RuntimePlan runtimePlan) {
        return modelOutputValidator.validateTeachingHintOutput(
                output,
                decision,
                runtimePlan == null ? null : runtimePlan.getBrief(),
                runtimePlan == null ? null : runtimePlan.getStandardLibraryPack()
        );
    }

    public ExternalModelStagePayloads.StageValidationResult validateStudentFeedback(
            com.onlinejudge.submission.dto.SubmissionAnalysisResponse.StudentFeedback feedback,
            ExternalModelStagePayloads.DiagnosisJudgeOutput decision,
            RuntimePlan runtimePlan) {
        return modelOutputValidator.validateStudentFeedback(
                feedback,
                decision,
                runtimePlan == null ? null : runtimePlan.getBrief(),
                runtimePlan == null ? null : runtimePlan.getStandardLibraryPack()
        );
    }

    @Data
    @Builder
    public static class RuntimePlan {
        private ModelDiagnosisBrief brief;
        private StandardLibraryPack standardLibraryPack;
        private PromptTemplateRegistry.PromptTemplate diagnosisPrompt;
        private PromptTemplateRegistry.PromptTemplate teachingPrompt;
        private PromptTemplateRegistry.PromptTemplate singleCallPrompt;
        private String runtimeProfile;
        private boolean requestCompact;
    }
}

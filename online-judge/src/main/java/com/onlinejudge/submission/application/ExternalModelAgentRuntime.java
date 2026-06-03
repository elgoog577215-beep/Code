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
    public static final String RUNTIME_PROFILE_AUTO = "auto";

    private static final int AUTO_COMPACT_SOURCE_LINE_THRESHOLD = 50;
    private static final int AUTO_COMPACT_SIGNAL_SOURCE_LINE_THRESHOLD = 30;
    private static final int AUTO_COMPACT_CODE_LENGTH_THRESHOLD = 3200;
    private static final int AUTO_COMPACT_SIGNAL_THRESHOLD = 4;
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
        return prepare(evidencePackage, ruleSignals, fallback, runtimeProfile,
                PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V3);
    }

    public RuntimePlan prepare(DiagnosisEvidencePackage evidencePackage,
                               RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                               SubmissionAnalysisResponse fallback,
                               String runtimeProfile,
                               String singleCallPromptVersion) {
        String normalizedProfile = normalizeRuntimeProfile(runtimeProfile);
        String normalizedPromptVersion = normalizeSingleCallPromptVersion(singleCallPromptVersion);
        ModelDiagnosisBrief standardBrief = briefBuilder.build(evidencePackage, ruleSignals, fallback);
        boolean compact = shouldCompact(normalizedProfile, standardBrief);
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
                .singleCallPrompt(promptTemplateRegistry.get(normalizedPromptVersion))
                .runtimeProfile(normalizedProfile)
                .requestCompact(compact)
                .build();
    }

    private String normalizeSingleCallPromptVersion(String promptVersion) {
        String version = promptVersion == null ? "" : promptVersion.trim();
        if (PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V1.equals(version)
                || PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V2.equals(version)
                || PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V3.equals(version)
                || PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V4_LITE.equals(version)) {
            return version;
        }
        return PromptTemplateRegistry.DIAGNOSIS_AND_TEACHING_V3;
    }

    private String normalizeRuntimeProfile(String runtimeProfile) {
        if (runtimeProfile != null && RUNTIME_PROFILE_LOW_LATENCY.equalsIgnoreCase(runtimeProfile.trim())) {
            return RUNTIME_PROFILE_LOW_LATENCY;
        }
        if (runtimeProfile != null && RUNTIME_PROFILE_AUTO.equalsIgnoreCase(runtimeProfile.trim())) {
            return RUNTIME_PROFILE_AUTO;
        }
        return RUNTIME_PROFILE_STANDARD;
    }

    private boolean shouldCompact(String runtimeProfile, ModelDiagnosisBrief brief) {
        if (RUNTIME_PROFILE_LOW_LATENCY.equals(runtimeProfile)) {
            return true;
        }
        if (!RUNTIME_PROFILE_AUTO.equals(runtimeProfile) || brief == null) {
            return false;
        }
        int sourceLines = brief.getSourceCodeLineCount() == null ? 0 : brief.getSourceCodeLineCount();
        int codeLength = brief.getKeyCodeExcerpt() == null ? 0 : brief.getKeyCodeExcerpt().length();
        int signalCount = brief.getCandidateSignals() == null ? 0 : brief.getCandidateSignals().size();
        return sourceLines >= AUTO_COMPACT_SOURCE_LINE_THRESHOLD
                || codeLength >= AUTO_COMPACT_CODE_LENGTH_THRESHOLD
                || (sourceLines >= AUTO_COMPACT_SIGNAL_SOURCE_LINE_THRESHOLD
                && signalCount >= AUTO_COMPACT_SIGNAL_THRESHOLD);
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
                .educationAgentProtocol(compactEducationAgentProtocol())
                .judgmentCalibrationExamples(compactJudgmentCalibrationExamples(source.getJudgmentCalibrationExamples()))
                .studentFeedbackRules(compactStudentFeedbackRules())
                .safetyRules(List.of(
                        "Use only provided tags, evidenceRefs and teachingActions.",
                        "Do not provide complete code, final answers or hidden test data.",
                        "Do not name replacement loop headers, transition formulas or executable structures.",
                        "For repeated input, ask students to count actual reads versus required reads.",
                        "Use HIGH answerLeakRisk only for actual leakage; evidence-counting tasks are LOW or MEDIUM.",
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

    private List<StandardLibraryPack.JudgmentCalibrationExample> compactJudgmentCalibrationExamples(
            List<StandardLibraryPack.JudgmentCalibrationExample> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .limit(3)
                .map(example -> StandardLibraryPack.JudgmentCalibrationExample.builder()
                        .id(example.getId())
                        .when(truncate(example.getWhen(), 130))
                        .choosePrimary(truncate(example.getChoosePrimary(), 110))
                        .doNotChoosePrimary(truncate(example.getDoNotChoosePrimary(), 110))
                        .reasoningPattern(truncate(example.getReasoningPattern(), 90))
                        .nextActionPattern(truncate(example.getNextActionPattern(), 90))
                        .safeImprovementCategories(example.getSafeImprovementCategories())
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

    private StandardLibraryPack.EducationAgentProtocol compactEducationAgentProtocol() {
        return StandardLibraryPack.EducationAgentProtocol.builder()
                .roleRules(List.of(
                        "Act as the external education AI agent.",
                        "Make a teaching judgment from problem, code and evidence."
                ))
                .rootCauseDecisionChecklist(List.of(
                        "1. Locate earliest concrete failed evidence.",
                        "2. Connect evidence to code behavior.",
                        "3. Compare candidate root causes and choose the direct explainer.",
                        "4. Demote helper/debug/style/sample distractors unless they explain the same evidence.",
                        "5. Convert the chosen root cause into one observable next action."
                ))
                .primaryRootCauseRules(List.of(
                        "Choose the current blocking root cause first.",
                        "Prioritize the earliest concrete failed evidence."
                ))
                .evidenceGroundingRules(List.of(
                        "Cite brief evidenceRefs.",
                        "Prefer the most specific generator:* or candidate evidenceRef plus readable judge/problem ref.",
                        "Hidden tests justify uncertainty, not guesses."
                ))
                .secondarySignalRules(List.of(
                        "Name secondary or distracting signals only when they are not primary."
                ))
                .improvementOpportunityRules(List.of(
                        "Improvements are follow-up learning value, not the blocking fix."
                ))
                .studentActionRules(List.of(
                        "Give one observable next action: compare, trace, estimate or counterexample.",
                        "For repeated input, count required input groups and actual read operations."
                ))
                .safetyBoundaryRules(List.of(
                        "No complete code, final answers, hidden data or replacement structures.",
                        "Do not say for _ in range(q), while q, or directly add a loop.",
                        "Set HIGH risk only for actual solution leakage, not for safe read-count comparisons."
                ))
                .nativeTraceQualityChecklist(List.of(
                        "nativePrimaryReasoningGrounded: root cause + strongest evidenceRefs + concrete failed behavior.",
                        "nativeTeachingPriorityClear: say why this is the first learning focus.",
                        "nativeSecondarySignalsBalanced: explain why secondary/distractor signals are not primary.",
                        "nativeNextActionObservable: one observable compare/trace/estimate/counterexample/checklist task with evidenceRefs.",
                        "nativeSafetyBoundary: no code, hidden data, replacement structures, formulas, or 直接改成."
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

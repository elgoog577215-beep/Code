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
                .basicCauses(compactBasicCauses(source.getBasicCauses()))
                .improvementPoints(compactImprovementPoints(source.getImprovementPoints()))
                .issueTags(compactTags(source.getIssueTags()))
                .fineGrainedTags(compactTags(source.getFineGrainedTags()))
                .improvementTags(compactImprovementTags(source.getImprovementTags()))
                .teachingActions(compactTeachingActions(source.getTeachingActions()))
                .build();
    }

    private List<StandardLibraryPack.BasicCauseOption> compactBasicCauses(
            List<StandardLibraryPack.BasicCauseOption> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(cause -> StandardLibraryPack.BasicCauseOption.builder()
                        .id(cause.getId())
                        .category(cause.getCategory())
                        .name(cause.getName())
                        .description(truncate(cause.getDescription(), 90))
                        .studentExplanation(truncate(cause.getStudentExplanation(), 80))
                        .evidenceSignals(limit(cause.getEvidenceSignals(), 3))
                        .judgeSignals(limit(cause.getJudgeSignals(), 3))
                        .hintL1(truncate(cause.getHintL1(), 70))
                        .hintL2(truncate(cause.getHintL2(), 70))
                        .hintL3(truncate(cause.getHintL3(), 70))
                        .abilityPoint(cause.getAbilityPoint())
                        .relatedFineTags(cause.getRelatedFineTags())
                        .teachingAction(cause.getTeachingAction())
                        .build())
                .toList();
    }

    private List<StandardLibraryPack.ImprovementPointOption> compactImprovementPoints(
            List<StandardLibraryPack.ImprovementPointOption> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(point -> StandardLibraryPack.ImprovementPointOption.builder()
                        .id(point.getId())
                        .category(point.getCategory())
                        .name(point.getName())
                        .description(truncate(point.getDescription(), 90))
                        .whenToUse(truncate(point.getWhenToUse(), 90))
                        .studentBenefit(truncate(point.getStudentBenefit(), 70))
                        .requiredEvidence(limit(point.getRequiredEvidence(), 3))
                        .hintL1(truncate(point.getHintL1(), 70))
                        .hintL2(truncate(point.getHintL2(), 70))
                        .hintL3(truncate(point.getHintL3(), 70))
                        .abilityPoint(point.getAbilityPoint())
                        .relatedBasicCauses(point.getRelatedBasicCauses())
                        .build())
                .toList();
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

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 24)) + "\n...[truncated for model]";
    }

    private List<String> limit(List<String> values, int maxSize) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().limit(maxSize).toList();
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

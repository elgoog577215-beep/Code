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
    private final AdviceGenerationOutputValidator adviceGenerationOutputValidator;
    private final AdviceGenerationOutputNormalizer adviceGenerationOutputNormalizer;
    private final AdviceGenerationFeedbackMapper adviceGenerationFeedbackMapper;

    public ExternalModelAgentRuntime(ModelDiagnosisBriefBuilder briefBuilder,
                                     StandardLibraryPackBuilder standardLibraryPackBuilder,
                                     PromptTemplateRegistry promptTemplateRegistry,
                                     ModelOutputValidator modelOutputValidator) {
        this(briefBuilder, standardLibraryPackBuilder, promptTemplateRegistry, modelOutputValidator,
                new ExternalModelOutputNormalizer(),
                new AdviceGenerationOutputValidator(),
                new AdviceGenerationOutputNormalizer(),
                new AdviceGenerationFeedbackMapper());
    }

    @Autowired
    public ExternalModelAgentRuntime(ModelDiagnosisBriefBuilder briefBuilder,
                                     StandardLibraryPackBuilder standardLibraryPackBuilder,
                                     PromptTemplateRegistry promptTemplateRegistry,
                                     ModelOutputValidator modelOutputValidator,
                                     ExternalModelOutputNormalizer outputNormalizer,
                                     AdviceGenerationOutputValidator adviceGenerationOutputValidator,
                                     AdviceGenerationOutputNormalizer adviceGenerationOutputNormalizer,
                                     AdviceGenerationFeedbackMapper adviceGenerationFeedbackMapper) {
        this.briefBuilder = briefBuilder;
        this.standardLibraryPackBuilder = standardLibraryPackBuilder;
        this.promptTemplateRegistry = promptTemplateRegistry;
        this.modelOutputValidator = modelOutputValidator;
        this.outputNormalizer = outputNormalizer == null ? new ExternalModelOutputNormalizer() : outputNormalizer;
        this.adviceGenerationOutputValidator = adviceGenerationOutputValidator == null
                ? new AdviceGenerationOutputValidator()
                : adviceGenerationOutputValidator;
        this.adviceGenerationOutputNormalizer = adviceGenerationOutputNormalizer == null
                ? new AdviceGenerationOutputNormalizer()
                : adviceGenerationOutputNormalizer;
        this.adviceGenerationFeedbackMapper = adviceGenerationFeedbackMapper == null
                ? new AdviceGenerationFeedbackMapper()
                : adviceGenerationFeedbackMapper;
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
                .searchLocationPrompt(promptTemplateRegistry.get(PromptTemplateRegistry.SEARCH_LOCATION_V1))
                .advicePrompt(promptTemplateRegistry.get(PromptTemplateRegistry.DIAGNOSIS_REPORT_V2))
                .runtimeProfile(normalizedProfile)
                .requestCompact(compact)
                .build();
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
                        .inputPreview(Boolean.TRUE.equals(item.getHidden())
                                ? null
                                : truncate(item.getInputPreview(), 160))
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
                .knowledgeAnchors(compactKnowledgeAnchors(source.getKnowledgeAnchors()))
                .skillUnits(compactSkillUnits(source.getSkillUnits()))
                .mistakePoints(compactMistakePoints(source.getMistakePoints()))
                .searchLocationSummary(source.getSearchLocationSummary())
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

    private List<StandardLibraryPack.KnowledgeAnchorOption> compactKnowledgeAnchors(
            List<StandardLibraryPack.KnowledgeAnchorOption> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(anchor -> StandardLibraryPack.KnowledgeAnchorOption.builder()
                        .id(anchor.getId())
                        .name(anchor.getName())
                        .path(truncate(anchor.getPath(), 120))
                        .description(truncate(anchor.getDescription(), 80))
                        .build())
                .toList();
    }

    private List<StandardLibraryPack.SkillUnitOption> compactSkillUnits(
            List<StandardLibraryPack.SkillUnitOption> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(skill -> StandardLibraryPack.SkillUnitOption.builder()
                        .id(skill.getId())
                        .category(skill.getCategory())
                        .name(skill.getName())
                        .description(truncate(skill.getDescription(), 90))
                        .knowledgeNodeCodes(limit(skill.getKnowledgeNodeCodes(), 5))
                        .applicableLanguages(limit(skill.getApplicableLanguages(), 3))
                        .build())
                .toList();
    }

    private List<StandardLibraryPack.MistakePointOption> compactMistakePoints(
            List<StandardLibraryPack.MistakePointOption> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(mistake -> StandardLibraryPack.MistakePointOption.builder()
                        .id(mistake.getId())
                        .category(mistake.getCategory())
                        .name(mistake.getName())
                        .description(truncate(mistake.getDescription(), 90))
                        .skillUnitCode(mistake.getSkillUnitCode())
                        .mistakeType(mistake.getMistakeType())
                        .commonMisconception(truncate(mistake.getCommonMisconception(), 80))
                        .knowledgeNodeCodes(limit(mistake.getKnowledgeNodeCodes(), 5))
                        .applicableLanguages(limit(mistake.getApplicableLanguages(), 3))
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

    public com.onlinejudge.submission.dto.SubmissionAnalysisResponse.StudentFeedback normalizeStudentFeedback(
            com.onlinejudge.submission.dto.SubmissionAnalysisResponse.StudentFeedback feedback,
            RuntimePlan runtimePlan) {
        return outputNormalizer.normalizeStudentFeedback(feedback, runtimePlan);
    }

    public ExternalModelStagePayloads.StageValidationResult validateStudentFeedback(
            com.onlinejudge.submission.dto.SubmissionAnalysisResponse.StudentFeedback feedback,
            RuntimePlan runtimePlan) {
        return modelOutputValidator.validateStudentFeedback(
                feedback,
                runtimePlan == null ? null : runtimePlan.getBrief(),
                runtimePlan == null ? null : runtimePlan.getStandardLibraryPack()
        );
    }

    public ExternalModelStagePayloads.StageValidationResult validateAdviceGeneration(
            AdviceGenerationOutput output,
            RuntimePlan runtimePlan) {
        return adviceGenerationOutputValidator.validate(
                output,
                runtimePlan == null ? null : runtimePlan.getBrief(),
                runtimePlan == null ? null : runtimePlan.getStandardLibraryPack()
        );
    }

    public AdviceGenerationOutput normalizeAdviceGeneration(AdviceGenerationOutput output,
                                                            RuntimePlan runtimePlan) {
        return adviceGenerationOutputNormalizer.normalize(
                output,
                runtimePlan == null ? null : runtimePlan.getStandardLibraryPack()
        );
    }

    public com.onlinejudge.submission.dto.SubmissionAnalysisResponse.StudentFeedback mapAdviceStudentFeedback(
            AdviceGenerationOutput output,
            RuntimePlan runtimePlan) {
        return adviceGenerationFeedbackMapper.toStudentFeedback(
                output,
                runtimePlan == null ? null : runtimePlan.getStandardLibraryPack()
        );
    }

    @Data
    @Builder
    public static class RuntimePlan {
        private ModelDiagnosisBrief brief;
        private StandardLibraryPack standardLibraryPack;
        private SearchLocationResult searchLocationResult;
        private AdviceGenerationResult adviceGenerationResult;
        private PromptTemplateRegistry.PromptTemplate searchLocationPrompt;
        private PromptTemplateRegistry.PromptTemplate advicePrompt;
        private String runtimeProfile;
        private boolean requestCompact;
    }
}

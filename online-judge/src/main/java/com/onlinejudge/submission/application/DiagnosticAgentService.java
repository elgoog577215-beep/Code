package com.onlinejudge.submission.application;

import com.onlinejudge.classroom.application.HintSafetyService;
import com.onlinejudge.classroom.domain.Assignment;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionCaseResult;
import com.onlinejudge.submission.dto.SubmissionAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiagnosticAgentService {

    private final DiagnosisEvidencePackageBuilder evidencePackageBuilder;
    private final RuleSignalAnalyzer ruleSignalAnalyzer;
    private final AiReportService aiReportService;
    private final HintSafetyService hintSafetyService;
    private final DiagnosisTaxonomy diagnosisTaxonomy;

    public AgentResult diagnose(Problem problem,
                                Submission submission,
                                List<SubmissionCaseResult> caseResults,
                                SubmissionAnalysisResponse baseline,
                                Assignment.HintPolicy hintPolicy) {
        return diagnose(problem, submission, caseResults, baseline, hintPolicy, null);
    }

    public AgentResult diagnose(Problem problem,
                                Submission submission,
                                List<SubmissionCaseResult> caseResults,
                                SubmissionAnalysisResponse baseline,
                                Assignment.HintPolicy hintPolicy,
                                DiagnosisEvidencePackage.HistoryEvidence historyEvidence) {
        Assignment.HintPolicy effectivePolicy = hintPolicy == null ? Assignment.HintPolicy.L2 : hintPolicy;
        DiagnosisEvidencePackage evidencePackage = evidencePackageBuilder.build(
                problem,
                submission,
                caseResults,
                baseline,
                effectivePolicy,
                historyEvidence
        );
        RuleSignalAnalyzer.RuleSignalResult ruleSignals = ruleSignalAnalyzer.analyze(evidencePackage);
        ruleSignals = applyHistorySignals(ruleSignals, evidencePackage);
        SubmissionAnalysisResponse ruleAware = applyRuleSignals(baseline, ruleSignals);
        ModelStageResult modelStage = enhanceWithModel(problem, submission, ruleAware, evidencePackage, ruleSignals);
        SubmissionAnalysisResponse enhanced = modelStage.analysis();
        enhanced.setIssueTags(diagnosisTaxonomy.normalizeIssueTags(enhanced.getIssueTags()));
        enhanced.setFineGrainedTags(diagnosisTaxonomy.normalizeFineGrainedTags(enhanced.getFineGrainedTags()));
        enhanced = applyLowConfidenceGuard(enhanced);
        enhanced = hintSafetyService.verifyAndRecord(enhanced, effectivePolicy);
        String traceSummary = buildTraceSummary(ruleSignals, enhanced, modelStage.fallbackUsed());
        enhanced.setDiagnosticTrace(traceSummary);
        return new AgentResult(enhanced, evidencePackage, ruleSignals, traceSummary);
    }

    private ModelStageResult enhanceWithModel(Problem problem,
                                              Submission submission,
                                              SubmissionAnalysisResponse ruleAware,
                                              DiagnosisEvidencePackage evidencePackage,
                                              RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        try {
            SubmissionAnalysisResponse enhanced = aiReportService.enhanceSubmissionAnalysis(
                    problem,
                    submission,
                    ruleAware,
                    evidencePackage,
                    ruleSignals
            );
            return new ModelStageResult(enhanced == null ? ruleAware : enhanced, enhanced == null);
        } catch (RuntimeException exception) {
            return new ModelStageResult(ruleAware, true);
        }
    }

    private SubmissionAnalysisResponse applyRuleSignals(SubmissionAnalysisResponse analysis,
                                                        RuleSignalAnalyzer.RuleSignalResult ruleSignals) {
        if (analysis == null || ruleSignals == null) {
            return analysis;
        }
        analysis.setIssueTags(diagnosisTaxonomy.normalizeIssueTags(mergeLists(
                analysis.getIssueTags(),
                ruleSignals.getCandidateIssueTags()
        )));
        analysis.setFineGrainedTags(diagnosisTaxonomy.normalizeFineGrainedTags(mergeLists(
                analysis.getFineGrainedTags(),
                ruleSignals.getCandidateFineGrainedTags()
        )));
        analysis.setEvidenceRefs(DiagnosisListSupport.deduplicate(mergeLists(
                analysis.getEvidenceRefs(),
                ruleSignals.getEvidenceRefs()
        )));
        if (analysis.getUncertainty() == null || analysis.getUncertainty().isBlank()) {
            analysis.setUncertainty("当前为规则诊断与评测事实生成的初步结论，隐藏测试点相关判断只表示可能方向。");
        }
        applyLowConfidenceGuard(analysis);
        return analysis;
    }

    private SubmissionAnalysisResponse applyLowConfidenceGuard(SubmissionAnalysisResponse analysis) {
        if (analysis == null || analysis.getConfidence() == null || analysis.getConfidence() >= 0.6) {
            return analysis;
        }
        analysis.setIssueTags(diagnosisTaxonomy.normalizeIssueTags(mergeLists(
                analysis.getIssueTags(),
                List.of("NEEDS_MORE_EVIDENCE")
        )));
        analysis.setEvidenceRefs(DiagnosisListSupport.deduplicate(mergeLists(
                analysis.getEvidenceRefs(),
                List.of("agent:low_confidence")
        )));
        String lowConfidenceNote = "当前置信度较低，需要更多提交、样例或教师复核来确认错因。";
        if (analysis.getUncertainty() == null || analysis.getUncertainty().isBlank()) {
            analysis.setUncertainty(lowConfidenceNote);
        } else if (!analysis.getUncertainty().contains("置信度")) {
            analysis.setUncertainty(analysis.getUncertainty() + " " + lowConfidenceNote);
        }
        return analysis;
    }

    private RuleSignalAnalyzer.RuleSignalResult applyHistorySignals(RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                                                                    DiagnosisEvidencePackage evidencePackage) {
        if (ruleSignals == null || evidencePackage == null || evidencePackage.getHistory() == null) {
            return ruleSignals;
        }
        DiagnosisEvidencePackage.HistoryEvidence history = evidencePackage.getHistory();
        String transition = history.getTransitionSignal() == null ? "" : history.getTransitionSignal();
        boolean possibleRegression = transition.contains("变化为")
                && evidencePackage.getSubmission() != null
                && !"ACCEPTED".equals(evidencePackage.getSubmission().getVerdict());
        if (!possibleRegression) {
            return ruleSignals;
        }
        RuleSignalAnalyzer.Signal signal = RuleSignalAnalyzer.Signal.builder()
                .evidenceRef("history:verdict_transition")
                .coarseTag("NEEDS_MORE_EVIDENCE")
                .fineTag("PARTIAL_FIX_REGRESSION")
                .confidence(0.56)
                .message("本次评测阶段相对上次发生变化但仍未通过，可能存在局部修复后引入的新问题。")
                .build();
        return RuleSignalAnalyzer.RuleSignalResult.builder()
                .signals(DiagnosisListSupport.append(ruleSignals.getSignals(), signal))
                .candidateIssueTags(DiagnosisListSupport.deduplicate(DiagnosisListSupport.merge(
                        ruleSignals.getCandidateIssueTags(),
                        List.of("NEEDS_MORE_EVIDENCE")
                )))
                .candidateFineGrainedTags(DiagnosisListSupport.deduplicate(DiagnosisListSupport.merge(
                        ruleSignals.getCandidateFineGrainedTags(),
                        List.of("PARTIAL_FIX_REGRESSION")
                )))
                .evidenceRefs(DiagnosisListSupport.deduplicate(DiagnosisListSupport.merge(
                        ruleSignals.getEvidenceRefs(),
                        List.of("history:verdict_transition")
                )))
                .build();
    }

    private List<String> mergeLists(List<String> left, List<String> right) {
        return DiagnosisListSupport.merge(left, right);
    }

    private String buildTraceSummary(RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                                     SubmissionAnalysisResponse analysis,
                                     boolean modelFallbackUsed) {
        int signalCount = ruleSignals == null || ruleSignals.getSignals() == null ? 0 : ruleSignals.getSignals().size();
        int evidenceRefCount = analysis == null || analysis.getEvidenceRefs() == null ? 0 : analysis.getEvidenceRefs().size();
        String source = analysis == null ? "UNKNOWN" : analysis.getSourceType();
        String modelStage = modelFallbackUsed ? "model=rule-fallback" : "model=completed";
        return "diagnostic-agent:v1 signals=" + signalCount
                + " evidenceRefs=" + evidenceRefCount
                + " source=" + source
                + " " + modelStage;
    }

    public record AgentResult(SubmissionAnalysisResponse analysis,
                              DiagnosisEvidencePackage evidencePackage,
                              RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                              String traceSummary) {
    }

    private record ModelStageResult(SubmissionAnalysisResponse analysis, boolean fallbackUsed) {
    }
}

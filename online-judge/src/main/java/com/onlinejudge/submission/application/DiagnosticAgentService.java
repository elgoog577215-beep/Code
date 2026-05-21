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

    private static final String AGENT_VERSION = "diagnostic-agent-v2";
    private static final String RULE_PROMPT_VERSION = "rule-signal-diagnosis-v1";

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
        SubmissionAnalysisResponse.LearningTrajectorySignal trajectorySignal =
                resolveLearningTrajectory(enhanced, evidencePackage);
        enhanced.setLearningTrajectorySignal(trajectorySignal);
        if (trajectorySignal != null) {
            enhanced.setEvidenceRefs(DiagnosisListSupport.deduplicate(mergeLists(
                    enhanced.getEvidenceRefs(),
                    singletonIfPresent(trajectorySignal.getEvidenceRef())
            )));
            enhanced.setProgressSignal(defaultIfBlank(enhanced.getProgressSignal(), trajectorySignal.getSummary()));
        }
        enhanced.setStudentHintPlan(resolveHintPlan(enhanced, effectivePolicy));
        enhanced.setLearningInterventionPlan(resolveInterventionPlan(enhanced, effectivePolicy));
        enhanced = hintSafetyService.verifyAndRecord(enhanced, effectivePolicy);
        String traceSummary = buildTraceSummary(ruleSignals, enhanced, modelStage.fallbackUsed());
        enhanced.setDiagnosticTrace(traceSummary);
        enhanced.setAiInvocation(resolveInvocation(enhanced, modelStage.fallbackUsed()));
        return new AgentResult(enhanced, evidencePackage, ruleSignals, traceSummary);
    }

    private SubmissionAnalysisResponse.StudentHintPlan resolveHintPlan(SubmissionAnalysisResponse analysis,
                                                                       Assignment.HintPolicy hintPolicy) {
        if (analysis == null) {
            return null;
        }
        SubmissionAnalysisResponse.StudentHintPlan existing = analysis.getStudentHintPlan();
        String primaryTag = primaryTag(analysis);
        String problemType = diagnosisTaxonomy.label(primaryTag);
        String teachingAction = diagnosisTaxonomy.teachingAction(primaryTag);
        List<String> evidenceRefs = DiagnosisListSupport.deduplicate(existing == null || existing.getEvidenceRefs() == null
                ? analysis.getEvidenceRefs()
                : existing.getEvidenceRefs());
        String safeHint = defaultIfBlank(analysis.getStudentHint(), defaultHintForTag(primaryTag));
        return SubmissionAnalysisResponse.StudentHintPlan.builder()
                .hintLevel(defaultIfBlank(existing == null ? null : existing.getHintLevel(), effectiveHintPolicy(hintPolicy).name()))
                .problemType(defaultIfBlank(existing == null ? null : existing.getProblemType(), problemType))
                .evidenceAnchor(defaultIfBlank(existing == null ? null : existing.getEvidenceAnchor(), buildEvidenceAnchor(primaryTag, evidenceRefs)))
                .nextAction(defaultIfBlank(existing == null ? null : existing.getNextAction(), safeHint))
                .coachQuestion(defaultIfBlank(existing == null ? null : existing.getCoachQuestion(), defaultCoachQuestion(primaryTag)))
                .teachingAction(defaultIfBlank(existing == null ? null : existing.getTeachingAction(), teachingAction))
                .evidenceRefs(evidenceRefs)
                .answerLeakRisk(defaultIfBlank(existing == null ? null : existing.getAnswerLeakRisk(), analysis.getAnswerLeakRisk()))
                .build();
    }

    private SubmissionAnalysisResponse.LearningInterventionPlan resolveInterventionPlan(SubmissionAnalysisResponse analysis,
                                                                                        Assignment.HintPolicy hintPolicy) {
        if (analysis == null) {
            return null;
        }
        SubmissionAnalysisResponse.LearningInterventionPlan existing = analysis.getLearningInterventionPlan();
        if (hasUsableIntervention(existing)) {
            return normalizeIntervention(existing, analysis);
        }
        String primaryTag = primaryTag(analysis);
        String teachingAction = analysis.getStudentHintPlan() == null
                ? diagnosisTaxonomy.teachingAction(primaryTag)
                : defaultIfBlank(analysis.getStudentHintPlan().getTeachingAction(), diagnosisTaxonomy.teachingAction(primaryTag));
        SubmissionAnalysisResponse.LearningTrajectorySignal trajectory = analysis.getLearningTrajectorySignal();
        String phase = trajectory == null ? "" : defaultIfBlank(trajectory.getPhase(), "");
        List<String> evidenceRefs = DiagnosisListSupport.deduplicate(mergeLists(
                analysis.getEvidenceRefs(),
                trajectory == null ? List.of() : singletonIfPresent(trajectory.getEvidenceRef())
        ));
        InterventionTemplate template = interventionTemplate(primaryTag, teachingAction, phase);
        String goal = template.goal();
        if (trajectory != null && trajectory.getSummary() != null && !trajectory.getSummary().isBlank()
                && ("REPEATED_STUCK".equals(phase) || "REGRESSION".equals(phase))) {
            goal = trajectory.getSummary();
        }
        String studentTask = defaultIfBlank(trajectory == null ? null : trajectory.getNextFocus(), template.studentTask());
        return SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                .interventionType(template.interventionType())
                .goal(goal)
                .studentTask(studentTask)
                .checkQuestion(template.checkQuestion())
                .completionSignal(template.completionSignal())
                .evidenceRefs(evidenceRefs)
                .estimatedMinutes(template.estimatedMinutes())
                .answerLeakRisk(defaultIfBlank(analysis.getAnswerLeakRisk(), "LOW"))
                .build();
    }

    private boolean hasUsableIntervention(SubmissionAnalysisResponse.LearningInterventionPlan plan) {
        return plan != null
                && plan.getInterventionType() != null && !plan.getInterventionType().isBlank()
                && plan.getStudentTask() != null && !plan.getStudentTask().isBlank()
                && plan.getCheckQuestion() != null && !plan.getCheckQuestion().isBlank();
    }

    private SubmissionAnalysisResponse.LearningInterventionPlan normalizeIntervention(
            SubmissionAnalysisResponse.LearningInterventionPlan plan,
            SubmissionAnalysisResponse analysis) {
        return SubmissionAnalysisResponse.LearningInterventionPlan.builder()
                .interventionType(defaultIfBlank(plan.getInterventionType(), "COLLECT_EVIDENCE"))
                .goal(defaultIfBlank(plan.getGoal(), "把当前判断变成一个可验证的小动作。"))
                .studentTask(defaultIfBlank(plan.getStudentTask(), "先写出一个最小样例和预期中间状态，再改代码。"))
                .checkQuestion(defaultIfBlank(plan.getCheckQuestion(), "这个动作完成后，你能看到哪一个具体证据变化？"))
                .completionSignal(defaultIfBlank(plan.getCompletionSignal(), "学生能给出样例、预期状态和实际状态的对比。"))
                .evidenceRefs(DiagnosisListSupport.deduplicate(mergeLists(plan.getEvidenceRefs(), analysis.getEvidenceRefs())))
                .estimatedMinutes(plan.getEstimatedMinutes() == null || plan.getEstimatedMinutes() <= 0 ? 5 : plan.getEstimatedMinutes())
                .answerLeakRisk(defaultIfBlank(plan.getAnswerLeakRisk(), analysis.getAnswerLeakRisk()))
                .build();
    }

    private InterventionTemplate interventionTemplate(String primaryTag, String teachingAction, String phase) {
        if ("REPEATED_STUCK".equals(phase)) {
            return new InterventionTemplate(
                    "MIN_CASE_TRACE",
                    "停止连续试错，把问题缩小到一个能手推的失败样例。",
                    "只保留一个最小失败输入，写出关键变量的预期变化和实际变化，再决定改哪一行。",
                    "这个样例里，第一个偏离预期的变量是哪一个？",
                    "学生能提交一个最小样例，并说明预期状态与实际状态第一次分叉的位置。",
                    8
            );
        }
        if ("REGRESSION".equals(phase)) {
            return new InterventionTemplate(
                    "COMPARE_SUBMISSIONS",
                    "先定位哪次修改引入了回退，而不是继续叠加修复。",
                    "对比最近两次提交，只记录一个行为变化和它对应的代码差异。",
                    "哪一处改动让原本更好的行为变差了？",
                    "学生能指出一个最小 diff、一个受影响样例和变化前后的输出差异。",
                    10
            );
        }
        if ("ACCEPTED_AFTER_FIX".equals(phase) || "ACCEPTED_REVIEW".equals(phase)) {
            return new InterventionTemplate(
                    "EXPLAIN_GENERALITY",
                    "把通过结果沉淀成可迁移的解题经验。",
                    "用三句话复盘：关键修复是什么、复杂度是什么、哪个边界样例现在能解释清楚。",
                    "如果题目换一个边界输入，你的理由还成立吗？",
                    "学生能解释关键不变量、复杂度和一个边界样例。",
                    6
            );
        }
        return switch (teachingAction == null ? "" : teachingAction) {
            case "ASK_MIN_CASE" -> new InterventionTemplate(
                    "MIN_CASE",
                    "先构造最小可验证样例，避免凭感觉改代码。",
                    "写出一个最小输入、期望输出和你代码当前输出的对比。",
                    "这个最小样例为什么足以暴露当前问题？",
                    "学生能提供最小输入、期望输出、实际输出三项对照。",
                    5
            );
            case "TRACE_VARIABLES" -> new InterventionTemplate(
                    "VARIABLE_TRACE",
                    "把边界判断转成可观察的变量变化。",
                    "选一个最小样例，列出循环第一轮和最后一轮的关键变量值。",
                    "循环第一次和最后一次分别处理了哪个位置？",
                    "学生能列出关键变量表，并指出第一次偏离预期的位置。",
                    7
            );
            case "COMPARE_OUTPUT", "COMPARE_INPUT_SPEC" -> new InterventionTemplate(
                    "IO_COMPARE",
                    "把输入输出格式问题定位到具体字符或读取步骤。",
                    "逐字对比题面格式、实际输出和期望输出，只圈出第一处不同。",
                    "多了、少了或读错的是哪一个字符或字段？",
                    "学生能指出第一处格式差异或第一个读错的字段。",
                    5
            );
            case "COUNT_COMPLEXITY" -> new InterventionTemplate(
                    "COMPLEXITY_ESTIMATE",
                    "先估算最大规模下的操作次数，再讨论优化方向。",
                    "写出核心循环在最大输入下大约执行多少次，并和时限做比较。",
                    "当 n 取最大值时，最内层操作会发生多少次？",
                    "学生能给出量级估算，并判断当前做法是否可能通过。",
                    6
            );
            case "DEFINE_STATE", "TRACE_STATE" -> new InterventionTemplate(
                    "STATE_EXPLANATION",
                    "先说清楚状态含义，再检查转移或重置。",
                    "用一句话定义关键状态，并写出它在一个小样例中的初值和更新后值。",
                    "这个状态是否包含判断答案所需的全部信息？",
                    "学生能说明状态含义、初值和一次更新后的含义。",
                    8
            );
            case "CHECK_INVARIANT", "BUILD_COUNTEREXAMPLE" -> new InterventionTemplate(
                    "COUNTEREXAMPLE",
                    "验证当前策略是否真的能泛化。",
                    "构造一个最小反例，说明当前局部选择或样例规律在哪里失效。",
                    "什么输入会挑战你当前的选择依据？",
                    "学生能给出一个反例，并解释它破坏了哪条假设。",
                    8
            );
            case "CHECK_RUNTIME_GUARDS" -> new InterventionTemplate(
                    "RUNTIME_GUARD_CHECK",
                    "先确认崩溃风险是否已经被具体 guard 覆盖。",
                    "列出最可能触发越界、空值或除零的输入，并说明当前代码如何处理。",
                    "哪个极端输入最可能触发运行错误？",
                    "学生能指出风险输入、保护条件和保护后的行为。",
                    6
            );
            case "FIX_FIRST_COMPILER_ERROR" -> new InterventionTemplate(
                    "FIX_FIRST_COMPILER_ERROR",
                    "先处理第一条编译错误，避免同时大改逻辑。",
                    "只读第一条编译错误，定位对应行和符号，再做一次最小修改。",
                    "第一条错误信息指向的是哪个符号或结构？",
                    "学生能定位第一条编译错误并完成一次最小修复。",
                    4
            );
            default -> new InterventionTemplate(
                    "COLLECT_EVIDENCE",
                    "把模糊判断转成下一条可验证证据。",
                    "补充一个最小样例或一段运行证据，再判断主要错因。",
                    "你下一步准备用哪条证据确认这个判断？",
                    "学生能补充一条新的样例、输出对比或错误信息。",
                    5
            );
        };
    }

    private Assignment.HintPolicy effectiveHintPolicy(Assignment.HintPolicy hintPolicy) {
        return hintPolicy == null ? Assignment.HintPolicy.L2 : hintPolicy;
    }

    private String primaryTag(SubmissionAnalysisResponse analysis) {
        List<String> fineTags = analysis.getFineGrainedTags();
        List<String> issueTags = analysis.getIssueTags();
        if (fineTags != null && !fineTags.isEmpty()) {
            String contextual = contextualPrimaryTag(issueTags, fineTags, analysis.getEvidenceRefs());
            if (contextual != null) {
                return contextual;
            }
            return preferredFineTag(fineTags);
        }
        if (issueTags != null && !issueTags.isEmpty()) {
            return issueTags.get(0);
        }
        return "NEEDS_MORE_EVIDENCE";
    }

    private String contextualPrimaryTag(List<String> issueTags, List<String> fineTags, List<String> evidenceRefs) {
        List<String> safeIssues = issueTags == null ? List.of() : issueTags;
        List<String> safeRefs = evidenceRefs == null ? List.of() : evidenceRefs;
        if (safeIssues.contains("NEEDS_MORE_EVIDENCE") && fineTags.contains("PARTIAL_FIX_REGRESSION")) {
            return "PARTIAL_FIX_REGRESSION";
        }
        if (fineTags.contains("EMPTY_INPUT")) {
            return "EMPTY_INPUT";
        }
        if (fineTags.contains("STATE_RESET") && safeRefs.stream()
                .anyMatch(ref -> ref != null && ref.equals("problem:multi_case_state_before_loop"))) {
            return "STATE_RESET";
        }
        if (fineTags.contains("MAX_BOUNDARY") && safeRefs.stream()
                .anyMatch(ref -> ref != null && ref.equals("problem:max_bound_linear_loop"))) {
            return "MAX_BOUNDARY";
        }
        if (safeIssues.contains("RUNTIME_STABILITY")) {
            return "RUNTIME_STABILITY";
        }
        if (safeIssues.contains("SPACE_COMPLEXITY")
                && safeRefs.stream().anyMatch(ref -> ref != null && ref.equals("code:matrix_allocation"))) {
            return "SPACE_COMPLEXITY";
        }
        if (safeIssues.contains("IO_FORMAT") && hasStrongIoEvidence(safeRefs)) {
            if (fineTags.contains("OUTPUT_FORMAT_DETAIL")) {
                return "OUTPUT_FORMAT_DETAIL";
            }
            if (fineTags.contains("INPUT_PARSING")) {
                return "INPUT_PARSING";
            }
        }
        if (safeIssues.contains("TIME_COMPLEXITY")) {
            if (fineTags.contains("OVER_SIMULATION")) {
                return "OVER_SIMULATION";
            }
            if (fineTags.contains("BRUTE_FORCE_LIMIT")) {
                return "BRUTE_FORCE_LIMIT";
            }
            if (fineTags.contains("MAX_BOUNDARY")) {
                return "MAX_BOUNDARY";
            }
        }
        if (safeIssues.contains("ALGORITHM_STRATEGY")) {
            if (fineTags.contains("DP_STATE_DESIGN")) {
                return "DP_STATE_DESIGN";
            }
            if (fineTags.contains("GREEDY_ASSUMPTION")) {
                return "GREEDY_ASSUMPTION";
            }
        }
        if (fineTags.contains("OFF_BY_ONE") && safeRefs.stream()
                .anyMatch(ref -> ref != null && (ref.equals("code:plus_minus_one")
                        || ref.equals("problem:inclusive_range_source_excludes_n")))) {
            return "OFF_BY_ONE";
        }
        return null;
    }

    private boolean hasStrongIoEvidence(List<String> evidenceRefs) {
        return evidenceRefs.stream().anyMatch(ref -> ref != null && (
                ref.equals("judge:whitespace_mismatch")
                        || ref.equals("problem:repeated_input_source_single_read")
        ));
    }

    private String preferredFineTag(List<String> fineTags) {
        if (fineTags == null || fineTags.isEmpty()) {
            return "NEEDS_MORE_EVIDENCE";
        }
        List<String> priority = List.of(
                "PARTIAL_FIX_REGRESSION",
                "DP_STATE_DESIGN",
                "GREEDY_ASSUMPTION",
                "STATE_RESET",
                "OVER_SIMULATION",
                "MAX_BOUNDARY",
                "EMPTY_INPUT",
                "DUPLICATE_CASE",
                "OUTPUT_FORMAT_DETAIL",
                "BRUTE_FORCE_LIMIT",
                "OFF_BY_ONE",
                "INITIAL_STATE",
                "SAMPLE_OVERFIT",
                "INPUT_PARSING"
        );
        for (String preferred : priority) {
            if (fineTags.contains(preferred)) {
                return preferred;
            }
        }
        return fineTags.get(0);
    }

    private String buildEvidenceAnchor(String primaryTag, List<String> evidenceRefs) {
        String label = diagnosisTaxonomy.label(primaryTag);
        if (evidenceRefs == null || evidenceRefs.isEmpty()) {
            return "当前判断主要来自评测结果和代码形态，先围绕“" + label + "”补充一个可验证证据。";
        }
        return "当前判断锚定在“" + label + "”，可先核对证据：" + evidenceRefs.get(0) + "。";
    }

    private String defaultHintForTag(String primaryTag) {
        return switch (primaryTag == null ? "NEEDS_MORE_EVIDENCE" : primaryTag) {
            case "SYNTAX_ERROR" -> "先只修第一条编译错误，不要同时大改逻辑。";
            case "IO_FORMAT", "OUTPUT_FORMAT_DETAIL", "INPUT_PARSING" -> "先逐字核对题面输入输出格式和你的读写顺序。";
            case "TIME_COMPLEXITY", "BRUTE_FORCE_LIMIT", "OVER_SIMULATION", "MAX_BOUNDARY" -> "先估算最大输入下核心操作会执行多少次。";
            case "STATE_TRANSITION", "DP_STATE_DESIGN" -> "先用一句话写清楚状态含义，再检查它依赖哪些上一状态。";
            case "GREEDY_ASSUMPTION", "ALGORITHM_STRATEGY" -> "先尝试构造一个能挑战当前策略的最小反例。";
            case "RUNTIME_STABILITY" -> "先检查数组下标、空值、除零和递归出口这些稳定性风险。";
            case "PARTIAL_FIX_REGRESSION" -> "先对比最近两次提交，找出是哪一处修改改变了失败现象。";
            default -> "先构造一个最小样例，手推你的关键变量变化。";
        };
    }

    private String defaultCoachQuestion(String primaryTag) {
        return switch (primaryTag == null ? "NEEDS_MORE_EVIDENCE" : primaryTag) {
            case "OFF_BY_ONE", "LOOP_BOUNDARY" -> "循环第一次和最后一次分别处理了哪个位置？";
            case "EMPTY_INPUT", "BOUNDARY_CONDITION" -> "最小规模输入下，答案变量最初来自哪里？";
            case "OUTPUT_FORMAT_DETAIL", "IO_FORMAT" -> "你的输出和题面要求相比，多了或少了哪个字符？";
            case "TIME_COMPLEXITY", "BRUTE_FORCE_LIMIT" -> "当输入取最大值时，核心循环大约执行多少次？";
            case "OVER_SIMULATION", "MAX_BOUNDARY" -> "如果输入达到题面最大规模，当前循环或模拟会执行多少次？";
            case "DP_STATE_DESIGN", "STATE_TRANSITION" -> "这个状态是否包含了题目判断所需的全部信息？";
            case "GREEDY_ASSUMPTION", "ALGORITHM_STRATEGY" -> "有没有一个很小的输入会让当前局部选择失效？";
            case "RUNTIME_STABILITY" -> "哪一种极端输入最可能触发越界、空值或除零？";
            case "PARTIAL_FIX_REGRESSION" -> "最近一次修改修好了什么，又可能引入了什么新问题？";
            default -> "你准备用哪个最小样例验证这个判断？";
        };
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
            boolean fallbackUsed = enhanced == null
                    || enhanced == ruleAware
                    || (enhanced.getSourceType() != null && enhanced.getSourceType().equals(ruleAware.getSourceType()));
            return new ModelStageResult(enhanced == null ? ruleAware : enhanced, fallbackUsed);
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

    private SubmissionAnalysisResponse.LearningTrajectorySignal resolveLearningTrajectory(
            SubmissionAnalysisResponse analysis,
            DiagnosisEvidencePackage evidencePackage) {
        String currentVerdict = normalizeVerdict(evidencePackage == null || evidencePackage.getSubmission() == null
                ? null
                : evidencePackage.getSubmission().getVerdict());
        DiagnosisEvidencePackage.HistoryEvidence history = evidencePackage == null ? null : evidencePackage.getHistory();
        String previousVerdict = normalizeVerdict(history == null ? null : history.getPreviousVerdict());

        if (isUnknown(previousVerdict)) {
            if (isAccepted(currentVerdict)) {
                return trajectory(
                        "ACCEPTED_REVIEW",
                        "accepted review",
                        "history:first_attempt",
                        "This accepted submission should move into review and generalization.",
                        "Ask the student to explain the invariant, complexity, and one boundary case.",
                        false
                );
            }
            return trajectory(
                    "FIRST_ATTEMPT",
                    "first observable failure",
                    "history:first_attempt",
                    "No reliable learning trajectory is available yet; diagnose from the current judge facts first.",
                    "Collect a minimal example or the first compiler/runtime evidence before judging deeper causes.",
                    false
            );
        }

        long repeatedCount = maxRepeatedCount(history);
        if (isAccepted(currentVerdict) && !isAccepted(previousVerdict)) {
            return trajectory(
                    "ACCEPTED_AFTER_FIX",
                    "accepted after fix",
                    "history:accepted_after_fix",
                    "The latest submission moved from a failing state to accepted; this is a good moment for reflection.",
                    "Ask the student which condition, boundary, or guard the fix changed.",
                    false
            );
        }
        if (isAccepted(previousVerdict) && !isAccepted(currentVerdict)) {
            return trajectory(
                    "REGRESSION",
                    "regression after accepted",
                    "history:verdict_regression",
                    "A previously accepted solution now fails, so the next step is to compare the latest edits.",
                    "Review the smallest diff and identify which changed assumption broke the passing behavior.",
                    true
            );
        }
        if ("COMPILATION_ERROR".equals(previousVerdict) && !"COMPILATION_ERROR".equals(currentVerdict)) {
            return trajectory(
                    "FIXED_COMPILATION",
                    "compilation fixed",
                    "history:fixed_compilation",
                    "The student has moved past compilation; focus should shift from syntax to runtime or correctness evidence.",
                    "Acknowledge the compile fix, then build a minimal check for the current verdict.",
                    false
            );
        }
        if ("RUNTIME_ERROR".equals(previousVerdict)
                && ("WRONG_ANSWER".equals(currentVerdict)
                || "TIME_LIMIT_EXCEEDED".equals(currentVerdict)
                || "MEMORY_LIMIT_EXCEEDED".equals(currentVerdict))) {
            return trajectory(
                    "RUNTIME_FIXED_CORRECTNESS_REMAINS",
                    "runtime fixed, correctness remains",
                    "history:runtime_fixed_correctness_remains",
                    "The runtime crash may be fixed, but correctness or complexity still needs a separate check.",
                    "Review which runtime guard worked, then switch to evidence for the current verdict.",
                    false
            );
        }
        if (currentVerdict.equals(previousVerdict) && !isAccepted(currentVerdict)) {
            boolean repeatedStuck = repeatedCount >= 3;
            return trajectory(
                    repeatedStuck ? "REPEATED_STUCK" : "STILL_STUCK",
                    repeatedStuck ? "repeated stuck state" : "same failure remains",
                    repeatedStuck ? "history:repeated_stuck" : "history:still_stuck",
                    repeatedStuck
                            ? "The student has stayed in the same failure category several times; reduce the task size before more edits."
                            : "The failure category is unchanged, so the recent fix has not reached the core evidence yet.",
                    repeatedStuck
                            ? "Ask for one minimal trace and consider teacher intervention on the mistaken assumption."
                            : "Compare the two latest submissions on one failing example and watch the key variable.",
                    repeatedStuck
            );
        }
        if (isRegression(previousVerdict, currentVerdict)) {
            return trajectory(
                    "REGRESSION",
                    "partial fix regression",
                    "history:verdict_regression",
                    "The current verdict is less stable than the previous one, suggesting a partial fix introduced a new issue.",
                    "Compare the latest edits and identify which changed behavior created the new failure.",
                    true
            );
        }
        if (isProgress(previousVerdict, currentVerdict)) {
            return trajectory(
                    "PROGRESSING",
                    "failure stage progressing",
                    "history:verdict_progress",
                    "The failure stage has moved forward, but correctness is not closed yet.",
                    "Keep the fixed part stable and continue with evidence for the current verdict.",
                    false
            );
        }
        return trajectory(
                "CURRENT_ATTEMPT",
                "current attempt",
                "history:current_attempt",
                "History exists, but it is not enough to classify clear progress or regression.",
                "Collect one piece of evidence that explains the current verdict.",
                false
        );
    }

    private SubmissionAnalysisResponse.LearningTrajectorySignal trajectory(String phase,
                                                                           String label,
                                                                           String evidenceRef,
                                                                           String summary,
                                                                           String nextFocus,
                                                                           boolean needsTeacherAttention) {
        return SubmissionAnalysisResponse.LearningTrajectorySignal.builder()
                .phase(phase)
                .label(label)
                .evidenceRef(evidenceRef)
                .summary(summary)
                .nextFocus(nextFocus)
                .needsTeacherAttention(needsTeacherAttention)
                .build();
    }

    private RuleSignalAnalyzer.RuleSignalResult applyHistorySignals(RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                                                                    DiagnosisEvidencePackage evidencePackage) {
        if (ruleSignals == null || evidencePackage == null || evidencePackage.getHistory() == null) {
            return ruleSignals;
        }
        DiagnosisEvidencePackage.HistoryEvidence history = evidencePackage.getHistory();
        String transition = history.getTransitionSignal() == null ? "" : history.getTransitionSignal();
        String currentVerdict = normalizeVerdict(evidencePackage.getSubmission() == null
                ? null
                : evidencePackage.getSubmission().getVerdict());
        boolean possibleRegression = isRegression(history.getPreviousVerdict(), currentVerdict)
                || ((transition.contains("变化为") || transition.toLowerCase().contains("regression"))
                && !isAccepted(currentVerdict));
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

    private List<String> singletonIfPresent(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value);
    }

    private String buildTraceSummary(RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                                     SubmissionAnalysisResponse analysis,
                                     boolean modelFallbackUsed) {
        int signalCount = ruleSignals == null || ruleSignals.getSignals() == null ? 0 : ruleSignals.getSignals().size();
        int evidenceRefCount = analysis == null || analysis.getEvidenceRefs() == null ? 0 : analysis.getEvidenceRefs().size();
        String source = analysis == null ? "UNKNOWN" : analysis.getSourceType();
        String modelStage = modelFallbackUsed ? "model=rule-fallback" : "model=completed";
        String trajectory = analysis == null || analysis.getLearningTrajectorySignal() == null
                ? ""
                : " trajectory=" + analysis.getLearningTrajectorySignal().getPhase();
        return AGENT_VERSION + " signals=" + signalCount
                + " evidenceRefs=" + evidenceRefCount
                + " source=" + source
                + " " + modelStage
                + trajectory;
    }

    private SubmissionAnalysisResponse.AiInvocation resolveInvocation(SubmissionAnalysisResponse analysis,
                                                                      boolean modelFallbackUsed) {
        SubmissionAnalysisResponse.AiInvocation existing = analysis == null ? null : analysis.getAiInvocation();
        boolean fallbackUsed = modelFallbackUsed || (existing != null && existing.isFallbackUsed());
        String status = fallbackUsed ? "RULE_FALLBACK" : defaultIfBlank(existing == null ? null : existing.getStatus(), "MODEL_COMPLETED");
        return SubmissionAnalysisResponse.AiInvocation.builder()
                .provider(defaultIfBlank(existing == null ? null : existing.getProvider(), modelFallbackUsed ? "LOCAL_RULES" : "AI_PROVIDER"))
                .model(defaultIfBlank(existing == null ? null : existing.getModel(), modelFallbackUsed ? "rule-signals" : "unknown-model"))
                .modelVersion(defaultIfBlank(existing == null ? null : existing.getModelVersion(), modelFallbackUsed ? "rule-signals-v1" : "unknown-model"))
                .promptVersion(defaultIfBlank(existing == null ? null : existing.getPromptVersion(), RULE_PROMPT_VERSION))
                .agentVersion(AGENT_VERSION)
                .analysisSchemaVersion(defaultIfBlank(
                        existing == null ? null : existing.getAnalysisSchemaVersion(),
                        analysis == null ? "diagnosis-v1" : analysis.getAnalysisSchemaVersion()
                ))
                .evidenceSchemaVersion(defaultIfBlank(
                        existing == null ? null : existing.getEvidenceSchemaVersion(),
                        analysis == null ? DiagnosisEvidencePackage.SCHEMA_VERSION : analysis.getEvidenceSchemaVersion()
                ))
                .taxonomyVersion(defaultIfBlank(
                        existing == null ? null : existing.getTaxonomyVersion(),
                        analysis == null ? DiagnosisTaxonomy.TAXONOMY_VERSION : analysis.getTaxonomyVersion()
                ))
                .status(status)
                .fallbackUsed(fallbackUsed)
                .build();
    }

    private String defaultIfBlank(String candidate, String fallback) {
        if (candidate != null && !candidate.isBlank()) {
            return candidate;
        }
        return fallback == null ? "" : fallback;
    }

    private String normalizeVerdict(String verdict) {
        if (verdict == null || verdict.isBlank()) {
            return "UNKNOWN";
        }
        String normalized = verdict.trim().toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');
        return switch (normalized) {
            case "AC", "ACCEPTED" -> "ACCEPTED";
            case "WA", "WRONG_ANSWER", "PRESENTATION_ERROR" -> "WRONG_ANSWER";
            case "CE", "COMPILE_ERROR", "COMPILATION_ERROR" -> "COMPILATION_ERROR";
            case "RE", "RUNTIME_ERROR" -> "RUNTIME_ERROR";
            case "TLE", "TIME_LIMIT_EXCEEDED" -> "TIME_LIMIT_EXCEEDED";
            case "MLE", "MEMORY_LIMIT_EXCEEDED" -> "MEMORY_LIMIT_EXCEEDED";
            default -> normalized;
        };
    }

    private boolean isUnknown(String verdict) {
        return verdict == null || verdict.isBlank() || "UNKNOWN".equals(verdict);
    }

    private boolean isAccepted(String verdict) {
        return "ACCEPTED".equals(normalizeVerdict(verdict));
    }

    private boolean isProgress(String previousVerdict, String currentVerdict) {
        String previous = normalizeVerdict(previousVerdict);
        String current = normalizeVerdict(currentVerdict);
        if (isUnknown(previous) || isUnknown(current) || previous.equals(current)) {
            return false;
        }
        return verdictStage(current) > verdictStage(previous);
    }

    private boolean isRegression(String previousVerdict, String currentVerdict) {
        String previous = normalizeVerdict(previousVerdict);
        String current = normalizeVerdict(currentVerdict);
        if (isUnknown(previous) || isUnknown(current) || previous.equals(current)) {
            return false;
        }
        if (isAccepted(previous) && !isAccepted(current)) {
            return true;
        }
        return verdictStage(current) < verdictStage(previous);
    }

    private int verdictStage(String verdict) {
        return switch (normalizeVerdict(verdict)) {
            case "COMPILATION_ERROR" -> 1;
            case "RUNTIME_ERROR" -> 2;
            case "WRONG_ANSWER", "TIME_LIMIT_EXCEEDED", "MEMORY_LIMIT_EXCEEDED" -> 3;
            case "ACCEPTED" -> 4;
            default -> 0;
        };
    }

    private long maxRepeatedCount(DiagnosisEvidencePackage.HistoryEvidence history) {
        if (history == null) {
            return 0L;
        }
        long issueCount = history.getRepeatedIssueCount() == null ? 0L : history.getRepeatedIssueCount();
        long fineCount = history.getRepeatedFineGrainedIssueCount() == null ? 0L : history.getRepeatedFineGrainedIssueCount();
        return Math.max(issueCount, fineCount);
    }

    public record AgentResult(SubmissionAnalysisResponse analysis,
                              DiagnosisEvidencePackage evidencePackage,
                              RuleSignalAnalyzer.RuleSignalResult ruleSignals,
                              String traceSummary) {
    }

    private record ModelStageResult(SubmissionAnalysisResponse analysis, boolean fallbackUsed) {
    }

    private record InterventionTemplate(String interventionType,
                                        String goal,
                                        String studentTask,
                                        String checkQuestion,
                                        String completionSignal,
                                        int estimatedMinutes) {
    }
}

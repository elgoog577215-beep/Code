package com.onlinejudge.classroom.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.classroom.domain.Assignment;
import com.onlinejudge.classroom.domain.CoachPrompt;
import com.onlinejudge.classroom.dto.CoachPromptResponse;
import com.onlinejudge.classroom.dto.CoachReplyRequest;
import com.onlinejudge.classroom.persistence.AssignmentRepository;
import com.onlinejudge.classroom.persistence.CoachPromptRepository;
import com.onlinejudge.learning.diagnosis.DiagnosisReportReader;
import com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy;
import com.onlinejudge.submission.application.DiagnosisEvidencePackageReader;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.domain.SubmissionAnalysis;
import com.onlinejudge.submission.persistence.SubmissionAnalysisRepository;
import com.onlinejudge.submission.persistence.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachPromptService {

    private static final String STRATEGY_REDUCE_GRANULARITY = "REDUCE_GRANULARITY";
    private static final String STRATEGY_COLLECT_EVIDENCE = "COLLECT_EVIDENCE";
    private static final String STRATEGY_VERIFY_MINIMAL_CHANGE = "VERIFY_MINIMAL_CHANGE";
    private static final String STRATEGY_TRANSFER_REFLECTION = "TRANSFER_REFLECTION";
    private static final String STRATEGY_SAFETY_RESET = "SAFETY_RESET";

    private final SubmissionRepository submissionRepository;
    private final SubmissionAnalysisRepository submissionAnalysisRepository;
    private final AssignmentRepository assignmentRepository;
    private final CoachPromptRepository coachPromptRepository;
    private final DiagnosisReportReader diagnosisReportReader;
    private final DiagnosisTaxonomy diagnosisTaxonomy;
    private final DiagnosisEvidencePackageReader diagnosisEvidencePackageReader;
    private final CoachAgentService coachAgentService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CoachPromptResponse generateNextQuestion(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在: " + submissionId));
        SubmissionAnalysis analysis = submissionAnalysisRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new IllegalStateException("请先等待本次 AI 诊断生成后再追问。"));
        Assignment.HintPolicy hintPolicy = resolveHintPolicy(submission);
        List<String> issueTags = diagnosisReportReader.issueTags(analysis);
        List<String> fineTags = diagnosisReportReader.fineGrainedTags(analysis);
        String primaryTag = fineTags.isEmpty()
                ? issueTags.stream().findFirst().orElse("NEEDS_MORE_EVIDENCE")
                : fineTags.get(0);
        LearningContext learningContext = buildLearningContext(submission, analysis, primaryTag, null, null, hintPolicy);
        List<String> evidenceRefs = learningContext.evidenceRefs();
        CoachAgentService.CoachDraft draft = coachAgentService.generateInitialQuestion(
                submission,
                analysis,
                primaryTag,
                hintPolicy,
                learningContext.summary(),
                evidenceRefs,
                ruleDraft(
                        buildQuestion(submission, primaryTag, hintPolicy, learningContext.adaptiveStrategySignal()),
                        buildRationale(primaryTag, learningContext),
                        evidenceRefs
                )
        );
        CoachPrompt prompt = coachPromptRepository.save(CoachPrompt.builder()
                .assignmentId(submission.getAssignmentId())
                .studentProfileId(submission.getStudentProfileId())
                .submissionId(submission.getId())
                .turnIndex(nextTurnIndex(submission.getId()))
                .hintPolicy(hintPolicy.name())
                .promptType(promptType("SOCRATIC_NEXT_STEP", draft))
                .modelFailureReason(blankToNull(draft.getFailureReason()))
                .modelAnswerLeakRisk(blankToNull(firstText(draft.getModelAnswerLeakRisk(), draft.getAnswerLeakRisk())))
                .question(draft.getQuestion())
                .rationale(buildDraftRationale(draft, learningContext))
                .contextSummary(learningContext.summary())
                .evidenceRefs(toJson(draftRefs(draft, evidenceRefs)))
                .build());
        return responseWithTurns(prompt);
    }

    public CoachPromptResponse getLatestPrompt(Long submissionId) {
        return coachPromptRepository.findTopBySubmissionIdOrderByCreatedAtDesc(submissionId)
                .map(this::responseWithTurns)
                .orElse(null);
    }

    @Transactional
    public CoachPromptResponse replyAndGenerateNext(Long submissionId, CoachReplyRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在: " + submissionId));
        SubmissionAnalysis analysis = submissionAnalysisRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new IllegalStateException("请先等待本次 AI 诊断生成后再回答追问。"));
        CoachPrompt current = coachPromptRepository.findTopBySubmissionIdOrderByCreatedAtDesc(submissionId)
                .orElseThrow(() -> new IllegalStateException("请先生成一个 AI 追问。"));
        if (hasText(current.getStudentAnswer())) {
            return responseWithTurns(current);
        }

        String answer = sanitizeAnswer(request == null ? "" : request.getAnswer());
        current.setStudentAnswer(answer);
        current.setCoachFeedback(buildCoachFeedback(current, answer));
        current.setAnsweredAt(LocalDateTime.now());
        coachPromptRepository.save(current);

        Assignment.HintPolicy hintPolicy = resolveHintPolicy(submission);
        List<String> issueTags = diagnosisReportReader.issueTags(analysis);
        List<String> fineTags = diagnosisReportReader.fineGrainedTags(analysis);
        String primaryTag = fineTags.isEmpty()
                ? issueTags.stream().findFirst().orElse("NEEDS_MORE_EVIDENCE")
                : fineTags.get(0);
        LearningContext learningContext = buildLearningContext(submission, analysis, primaryTag, answer, current.getTurnIndex(), hintPolicy);
        List<String> evidenceRefs = learningContext.evidenceRefs();
        CoachAgentService.CoachDraft draft = coachAgentService.generateFollowUpQuestion(
                submission,
                analysis,
                primaryTag,
                hintPolicy,
                learningContext.summary(),
                evidenceRefs,
                answer,
                current.getTurnIndex(),
                ruleDraft(
                        buildFollowUpQuestion(primaryTag, answer, current.getTurnIndex(), hintPolicy, learningContext.adaptiveStrategySignal()),
                        "基于学生上一轮回答继续追问，要求学生补充证据或自测，不直接给出改法。 " + learningContext.summary(),
                        evidenceRefs
                )
        );
        CoachPrompt next = coachPromptRepository.save(CoachPrompt.builder()
                .assignmentId(submission.getAssignmentId())
                .studentProfileId(submission.getStudentProfileId())
                .submissionId(submission.getId())
                .parentPromptId(current.getId())
                .turnIndex(nextTurnIndex(submission.getId()))
                .hintPolicy(hintPolicy.name())
                .promptType(promptType("SOCRATIC_FOLLOW_UP", draft))
                .modelFailureReason(blankToNull(draft.getFailureReason()))
                .modelAnswerLeakRisk(blankToNull(firstText(draft.getModelAnswerLeakRisk(), draft.getAnswerLeakRisk())))
                .question(draft.getQuestion())
                .rationale(buildDraftRationale(draft, learningContext))
                .contextSummary(learningContext.summary())
                .evidenceRefs(toJson(draftRefs(draft, evidenceRefs)))
                .build());
        return responseWithTurns(next);
    }

    private CoachAgentService.CoachDraft ruleDraft(String question, String rationale, List<String> evidenceRefs) {
        return CoachAgentService.CoachDraft.builder()
                .question(question)
                .rationale(rationale)
                .evidenceRefs(evidenceRefs == null ? List.of() : evidenceRefs)
                .confidence(1.0)
                .answerLeakRisk("LOW")
                .source("RULE")
                .build();
    }

    private String promptType(String baseType, CoachAgentService.CoachDraft draft) {
        return "MODEL".equals(draft.getSource()) ? baseType + "_MODEL" : baseType;
    }

    private String buildDraftRationale(CoachAgentService.CoachDraft draft, LearningContext learningContext) {
        String rationale = draft.getRationale() == null || draft.getRationale().isBlank()
                ? "基于证据生成追问。"
                : draft.getRationale();
        String source = "MODEL".equals(draft.getSource()) ? "模型追问" : "规则追问";
        String confidence = draft.getConfidence() == null ? "" : " 置信度：" + draft.getConfidence();
        String risk = draft.getAnswerLeakRisk() == null ? "" : " 泄题风险：" + draft.getAnswerLeakRisk();
        return source + "。" + rationale + confidence + risk
                + (learningContext.summary().isBlank() ? "" : " " + learningContext.summary());
    }

    private List<String> draftRefs(CoachAgentService.CoachDraft draft, List<String> fallbackRefs) {
        if (draft.getEvidenceRefs() == null || draft.getEvidenceRefs().isEmpty()) {
            return fallbackRefs == null ? List.of() : fallbackRefs;
        }
        return mergeRefs(draft.getEvidenceRefs(), fallbackRefs);
    }

    private Assignment.HintPolicy resolveHintPolicy(Submission submission) {
        if (submission.getAssignmentId() == null) {
            return Assignment.HintPolicy.L2;
        }
        return assignmentRepository.findById(submission.getAssignmentId())
                .map(Assignment::getHintPolicy)
                .orElse(Assignment.HintPolicy.L2);
    }

    private LearningContext buildLearningContext(Submission submission,
                                                 SubmissionAnalysis analysis,
                                                 String primaryTag,
                                                 String studentAnswer,
                                                 Integer currentTurnIndex,
                                                 Assignment.HintPolicy hintPolicy) {
        List<String> refs = diagnosisReportReader.evidenceRefs(analysis);
        DiagnosisEvidencePackageReader.EvidenceSummary evidenceSummary = diagnosisEvidencePackageReader.summarize(analysis, submission);
        List<String> baseRefs = mergeRefs(List.of(
                "submission:" + submission.getId(),
                "analysis:" + analysis.getScenario(),
                "tag:" + primaryTag
        ), refs);
        List<String> evidenceRefs = mergeRefs(baseRefs, evidenceSummary.evidenceRefs());
        List<Submission> recentSubmissions = loadRecentSubmissions(submission);
        Map<Long, SubmissionAnalysis> analyses = recentSubmissions.isEmpty()
                ? Map.of()
                : submissionAnalysisRepository.findBySubmissionIdIn(recentSubmissions.stream().map(Submission::getId).toList())
                .stream()
                .collect(Collectors.toMap(SubmissionAnalysis::getSubmissionId, Function.identity()));
        String repeatedFineTag = repeatedTag(recentSubmissions, analyses, true);
        String repeatedIssueTag = repeatedTag(recentSubmissions, analyses, false);
        String transition = buildTransition(recentSubmissions);
        DiagnosisReportReader.LearningActionEvidenceSnapshot actionEvidence = diagnosisReportReader.learningActionEvidence(analysis);
        CoachPromptResponse.CoachAdaptiveStrategySignal adaptiveStrategySignal = selectAdaptiveStrategy(
                submission,
                primaryTag,
                repeatedFineTag,
                repeatedIssueTag,
                actionEvidence,
                studentAnswer,
                currentTurnIndex,
                hintPolicy,
                evidenceRefs
        );
        evidenceRefs = mergeRefs(evidenceRefs, adaptiveStrategySignal.getEvidenceRefs());
        String summary = buildContextSummary(
                primaryTag,
                repeatedFineTag,
                repeatedIssueTag,
                transition,
                evidenceRefs,
                evidenceSummary,
                adaptiveStrategySignal
        );
        return new LearningContext(summary, evidenceRefs, adaptiveStrategySignal);
    }

    private CoachPromptResponse.CoachAdaptiveStrategySignal selectAdaptiveStrategy(Submission submission,
                                                                                  String primaryTag,
                                                                                  String repeatedFineTag,
                                                                                  String repeatedIssueTag,
                                                                                  DiagnosisReportReader.LearningActionEvidenceSnapshot actionEvidence,
                                                                                  String studentAnswer,
                                                                                  Integer currentTurnIndex,
                                                                                  Assignment.HintPolicy hintPolicy,
                                                                                  List<String> baseEvidenceRefs) {
        LinkedHashSet<String> refs = new LinkedHashSet<>(baseEvidenceRefs == null ? List.of() : baseEvidenceRefs);
        StrategyDecision decision;
        if (studentAnswer != null) {
            decision = selectFollowUpStrategy(studentAnswer, currentTurnIndex, hintPolicy);
        } else {
            decision = selectInitialStrategy(submission, primaryTag, repeatedFineTag, repeatedIssueTag, actionEvidence);
        }
        refs.add("coach-strategy:" + decision.strategy());
        refs.addAll(decision.evidenceRefs());
        return CoachPromptResponse.CoachAdaptiveStrategySignal.builder()
                .strategy(decision.strategy())
                .reason(decision.reason())
                .recommendedCoachMove(decision.recommendedCoachMove())
                .needsTeacherAttention(decision.needsTeacherAttention())
                .evidenceRefs(List.copyOf(refs))
                .build();
    }

    private StrategyDecision selectInitialStrategy(Submission submission,
                                                   String primaryTag,
                                                   String repeatedFineTag,
                                                   String repeatedIssueTag,
                                                   DiagnosisReportReader.LearningActionEvidenceSnapshot actionEvidence) {
        if (submission != null && submission.getVerdict() == Submission.Verdict.ACCEPTED) {
            return strategy(
                    STRATEGY_TRANSFER_REFLECTION,
                    "当前提交已经通过，需要把通过经验迁移到边界、复杂度或新样例复盘。",
                    "请学生解释复杂度、补一个非样例测试或说明最容易漏掉的边界。",
                    false,
                    List.of("coach-adaptive:verdict:ACCEPTED")
            );
        }
        if (actionEvidence != null && "CONTRADICTED".equals(actionEvidence.executionStatus())) {
            return strategy(
                    STRATEGY_REDUCE_GRANULARITY,
                    firstText(actionEvidence.nextAdjustment(), "前次学习动作被后续证据反驳，需要降低提示颗粒度。"),
                    "把问题缩到一个最小失败样例或一条关键变量轨迹，先验证学生是否真正看见失败条件。",
                    true,
                    mergeRefs(
                            List.of("coach-adaptive:previous_action:CONTRADICTED"),
                            actionEvidence.evidenceRefs()
                    )
            );
        }
        if (hasText(repeatedFineTag)) {
            return strategy(
                    STRATEGY_REDUCE_GRANULARITY,
                    "最近多次出现细分卡点“" + diagnosisTaxonomy.label(repeatedFineTag) + "”，需要缩小追问颗粒度。",
                    "要求学生写出最小失败样例、变量首次和末次取值，避免继续泛泛讨论。",
                    true,
                    List.of("coach-adaptive:repeated_fine_tag:" + repeatedFineTag)
            );
        }
        if (hasText(repeatedIssueTag)) {
            return strategy(
                    STRATEGY_REDUCE_GRANULARITY,
                    "最近多次出现同类问题“" + diagnosisTaxonomy.label(repeatedIssueTag) + "”，需要缩小追问颗粒度。",
                    "要求学生把同类问题落到一个可手推样例或输入输出对照。",
                    true,
                    List.of("coach-adaptive:repeated_issue_tag:" + repeatedIssueTag)
            );
        }
        return strategy(
                STRATEGY_COLLECT_EVIDENCE,
                "当前还缺少学生可自证的样例、变量或复杂度证据。",
                "先让学生补一个最小样例、关键变量变化或输入输出对照。",
                false,
                List.of("coach-adaptive:primary_tag:" + (primaryTag == null ? "NEEDS_MORE_EVIDENCE" : primaryTag))
        );
    }

    private StrategyDecision selectFollowUpStrategy(String studentAnswer,
                                                    Integer currentTurnIndex,
                                                    Assignment.HintPolicy hintPolicy) {
        String normalized = studentAnswer == null ? "" : studentAnswer.toLowerCase(Locale.ROOT);
        if (!hasText(studentAnswer)) {
            return strategy(
                    STRATEGY_COLLECT_EVIDENCE,
                    "学生上一轮还没有形成可验证回答。",
                    "请学生补一个最小样例或关键变量变化。",
                    false,
                    List.of("coach-adaptive:answer_quality:NOT_ANSWERED")
            );
        }
        if (mentionsAnswerLikeContent(normalized)) {
            return strategy(
                    STRATEGY_SAFETY_RESET,
                    "学生回答出现答案或完整代码倾向，需要把对话拉回证据层。",
                    "避免确认直接改法，改问能暴露问题的输入特征或测试现象。",
                    true,
                    List.of("coach-adaptive:answer_quality:SAFETY_RISK")
            );
        }
        if (!mentionsEvidence(normalized)) {
            return strategy(
                    STRATEGY_COLLECT_EVIDENCE,
                    "学生上一轮只有方向但缺少样例、变量、输入输出或复杂度证据。",
                    "继续要求补证据，先不要推进到改代码。",
                    false,
                    List.of("coach-adaptive:answer_quality:NEEDS_EVIDENCE")
            );
        }
        return strategy(
                STRATEGY_VERIFY_MINIMAL_CHANGE,
                "学生回答已经包含可验证证据，可以推进到最小修改验证。",
                currentTurnIndex != null && currentTurnIndex >= 3
                        ? "请学生先提交一次最小修改，用评测结果验证判断。"
                        : "请学生只做一个最小修改，并预测下一次提交会改变哪个测试现象。",
                false,
                List.of(
                        "coach-adaptive:answer_quality:EVIDENCE_GROUNDED",
                        "coach-adaptive:hint_policy:" + (hintPolicy == null ? Assignment.HintPolicy.L2.name() : hintPolicy.name())
                )
        );
    }

    private StrategyDecision strategy(String strategy,
                                      String reason,
                                      String recommendedCoachMove,
                                      boolean needsTeacherAttention,
                                      List<String> evidenceRefs) {
        return new StrategyDecision(
                strategy,
                reason == null ? "" : reason,
                recommendedCoachMove == null ? "" : recommendedCoachMove,
                needsTeacherAttention,
                evidenceRefs == null ? List.of() : evidenceRefs
        );
    }

    private List<Submission> loadRecentSubmissions(Submission submission) {
        if (submission.getAssignmentId() == null || submission.getStudentProfileId() == null) {
            return List.of(submission);
        }
        return submissionRepository.findByAssignmentIdAndStudentProfileIdOrderBySubmittedAtDesc(
                        submission.getAssignmentId(),
                        submission.getStudentProfileId()
                )
                .stream()
                .filter(item -> item.getId() != null)
                .limit(5)
                .toList();
    }

    private String repeatedTag(List<Submission> submissions,
                               Map<Long, SubmissionAnalysis> analyses,
                               boolean fineGrained) {
        Map<String, Long> counts = submissions.stream()
                .map(submission -> analyses.get(submission.getId()))
                .filter(Objects::nonNull)
                .flatMap(analysis -> (fineGrained
                        ? diagnosisReportReader.fineGrainedTags(analysis)
                        : diagnosisReportReader.issueTags(analysis)).stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.entrySet()
                .stream()
                .filter(entry -> entry.getValue() >= 2)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private String buildTransition(List<Submission> submissions) {
        if (submissions.size() < 2) {
            return "";
        }
        String latest = verdictName(submissions.get(0));
        String previous = verdictName(submissions.get(1));
        if (latest.equals(previous)) {
            return "最近两次提交仍停留在“" + readableVerdict(latest) + "”。";
        }
        return "最近评测阶段从“" + readableVerdict(previous) + "”变为“" + readableVerdict(latest) + "”。";
    }

    private String buildContextSummary(String primaryTag,
                                       String repeatedFineTag,
                                       String repeatedIssueTag,
                                       String transition,
                                       List<String> evidenceRefs,
                                       DiagnosisEvidencePackageReader.EvidenceSummary evidenceSummary,
                                       CoachPromptResponse.CoachAdaptiveStrategySignal adaptiveStrategySignal) {
        StringBuilder summary = new StringBuilder("本次追问基于“")
                .append(diagnosisTaxonomy.label(primaryTag))
                .append("”。");
        if (repeatedFineTag != null && !repeatedFineTag.isBlank()) {
            summary.append(" 最近多次出现细分卡点“").append(diagnosisTaxonomy.label(repeatedFineTag)).append("”。");
        } else if (repeatedIssueTag != null && !repeatedIssueTag.isBlank()) {
            summary.append(" 最近多次出现同类问题“").append(diagnosisTaxonomy.label(repeatedIssueTag)).append("”。");
        }
        if (transition != null && !transition.isBlank()) {
            summary.append(" ").append(transition);
        }
        if (evidenceRefs != null && evidenceRefs.size() > 3) {
            summary.append(" 已引用 ").append(evidenceRefs.size()).append(" 条诊断证据。");
        }
        if (evidenceSummary != null && evidenceSummary.detailLines() != null && !evidenceSummary.detailLines().isEmpty()) {
            summary.append(" 证据包摘要：")
                    .append(evidenceSummary.detailLines().stream().limit(3).collect(Collectors.joining("；")))
                    .append("。");
        }
        if (adaptiveStrategySignal != null && hasText(adaptiveStrategySignal.getStrategy())) {
            summary.append(" Coach 自适应策略：")
                    .append(firstText(adaptiveStrategySignal.getReason(), adaptiveStrategyLabel(adaptiveStrategySignal.getStrategy())))
                    .append(" 下一问动作：")
                    .append(firstText(adaptiveStrategySignal.getRecommendedCoachMove(), adaptiveStrategyLabel(adaptiveStrategySignal.getStrategy())))
                    .append("。");
        }
        return summary.toString();
    }

    private String buildRationale(String primaryTag, LearningContext learningContext) {
        return "基于本次诊断标签“" + diagnosisTaxonomy.label(primaryTag) + "”和最近学习轨迹生成，不直接给出改法。"
                + (learningContext.summary().isBlank() ? "" : " " + learningContext.summary());
    }

    private String verdictName(Submission submission) {
        return submission == null || submission.getVerdict() == null ? "UNKNOWN" : submission.getVerdict().name();
    }

    private String readableVerdict(String verdict) {
        return switch (verdict) {
            case "ACCEPTED" -> "已通过";
            case "WRONG_ANSWER" -> "答案需修正";
            case "TIME_LIMIT_EXCEEDED" -> "时间超限";
            case "MEMORY_LIMIT_EXCEEDED" -> "内存超限";
            case "RUNTIME_ERROR" -> "运行错误";
            case "COMPILATION_ERROR" -> "编译错误";
            default -> "待观察";
        };
    }

    private String buildQuestion(Submission submission,
                                 String tagId,
                                 Assignment.HintPolicy hintPolicy,
                                 CoachPromptResponse.CoachAdaptiveStrategySignal adaptiveStrategySignal) {
        String tag = tagId == null ? "NEEDS_MORE_EVIDENCE" : tagId.toUpperCase(Locale.ROOT);
        Assignment.HintPolicy policy = hintPolicy == null ? Assignment.HintPolicy.L2 : hintPolicy;
        String strategy = adaptiveStrategy(adaptiveStrategySignal);
        if (submission.getVerdict() == Submission.Verdict.ACCEPTED) {
            return switch (policy) {
                case L1, L2 -> "这题已经通过了。你能说出一个最容易漏掉的边界样例，并解释为什么你的代码能处理它吗？";
                case L3, L4 -> "这题已经通过了。请用自己的话说明算法复杂度，并补一个和样例不同的测试来验证泛化能力。";
            };
        }
        if (STRATEGY_REDUCE_GRANULARITY.equals(strategy)) {
            return switch (tag) {
                case "OFF_BY_ONE", "LOOP_BOUNDARY" -> "先把问题缩到一个最小失败样例。请写出循环变量第一次、最后一次分别取什么值，再手推 n=1 或 n=2。";
                case "OUTPUT_FORMAT_DETAIL", "IO_FORMAT" -> "先把问题缩到一行输出。请对照题面写出你的输出多了或少了哪个字符、空格或换行。";
                case "INPUT_PARSING" -> "先把问题缩到第一行输入。请逐项写出题面这一行是什么，你的代码这一行读到了什么。";
                case "BRUTE_FORCE_LIMIT", "TIME_COMPLEXITY", "MAX_BOUNDARY" -> "先把问题缩到最大输入规模。请估算核心循环次数，并写出它为什么会超出时间限制。";
                case "INITIAL_STATE", "STATE_RESET", "VARIABLE_INITIALIZATION" -> "先把问题缩到一个变量。请写出它第一次赋值、每轮更新、下一组数据开始前的值。";
                default -> "先把问题缩到一个最小失败样例。请手推输入、关键变量变化和你的程序输出，从第一处不一致开始看。";
            };
        }
        return switch (tag) {
            case "OFF_BY_ONE", "LOOP_BOUNDARY" -> "先不要改代码。你能列出循环变量第一次、最后一次分别取什么值，并手推 n=1 或 n=2 时会发生什么吗？";
            case "OUTPUT_FORMAT_DETAIL", "IO_FORMAT" -> "先只看输出格式。你的输出和题面要求相比，多了或少了哪个字符、空格或换行？";
            case "INPUT_PARSING" -> "先圈出题面每一行输入。你的代码读入顺序和题面描述能一行一行对上吗？";
            case "BRUTE_FORCE_LIMIT", "TIME_COMPLEXITY", "MAX_BOUNDARY" -> "先估算一下：当 n 取最大值时，你的核心循环大约会执行多少次？这个数量在时间限制内合理吗？";
            case "INITIAL_STATE", "STATE_RESET", "VARIABLE_INITIALIZATION" -> "先找变量状态。每个关键变量第一次赋值在哪里？如果有多组或多轮处理，它会不会带着上一次的值继续用？";
            case "SAMPLE_OVERFIT", "SAMPLE_ONLY" -> "先构造一个和样例结构不同的最小输入。你的代码为什么也应该通过它？";
            case "PARTIAL_FIX_REGRESSION" -> "回看最近一次改动。只保留一个最小修改点时，哪个测试现象发生了变化？";
            case "RUNTIME_STABILITY" -> "先定位运行稳定性。有没有数组下标、空列表、除零或未初始化变量在某些输入下会出问题？";
            case "SYNTAX_ERROR" -> "先看第一条编译错误。它指向的符号或缩进，和你原本想表达的结构是否一致？";
            default -> "先用自己的话复述题意，再写一个最小样例手推。你的代码在哪一步和手推结果开始不一样？";
        };
    }

    private String buildFollowUpQuestion(String tagId,
                                         String answer,
                                         Integer currentTurnIndex,
                                         Assignment.HintPolicy hintPolicy,
                                         CoachPromptResponse.CoachAdaptiveStrategySignal adaptiveStrategySignal) {
        String normalized = answer == null ? "" : answer.toLowerCase(Locale.ROOT);
        String tag = tagId == null ? "NEEDS_MORE_EVIDENCE" : tagId.toUpperCase(Locale.ROOT);
        Assignment.HintPolicy policy = hintPolicy == null ? Assignment.HintPolicy.L2 : hintPolicy;
        String strategy = adaptiveStrategy(adaptiveStrategySignal);
        if (STRATEGY_SAFETY_RESET.equals(strategy)) {
            return "先把注意力从完整写法收回来。你能只描述一个会暴露问题的输入特征或测试现象，而不是直接说改法吗？";
        }
        if (STRATEGY_VERIFY_MINIMAL_CHANGE.equals(strategy)) {
            if (currentTurnIndex != null && currentTurnIndex >= 3) {
                return "这一轮已经有足够线索了。请先提交一次最小修改，用新的评测结果验证你的判断。";
            }
            return "很好，先不要看答案。请根据你刚才的证据，只做一个最小修改，然后预测下一次提交会改变哪个测试现象。";
        }
        if (!hasText(answer)) {
            return "先不用急着改代码。请补一句：你准备用哪个最小样例验证刚才的问题？";
        }
        if (mentionsAnswerLikeContent(normalized)) {
            return "先把注意力从完整写法收回来。你能只描述一个会暴露问题的输入特征，而不是直接说改法吗？";
        }
        if (!mentionsEvidence(normalized)) {
            return switch (tag) {
                case "OFF_BY_ONE", "LOOP_BOUNDARY" -> "你提到了思路，但还缺少证据。请写出一个最小 n 值，并列出循环第一次和最后一次的取值。";
                case "INPUT_PARSING", "OUTPUT_FORMAT_DETAIL", "IO_FORMAT" -> "你提到了方向，但还缺少对照。请把题面输入/输出要求和你代码中的读写顺序逐项对齐。";
                case "TIME_COMPLEXITY", "BRUTE_FORCE_LIMIT", "MAX_BOUNDARY" -> "你提到了复杂度方向，但还缺少数量级。请估算最大输入下核心操作大约执行多少次。";
                default -> "你已经有一个方向了。请补一个能验证这个方向的最小样例或关键变量变化。";
            };
        }
        if (policy == Assignment.HintPolicy.L1 || policy == Assignment.HintPolicy.L2) {
            return "很好，先不要看答案。请根据你刚才的证据，只做一个最小修改，然后预测下一次提交会改变哪个测试现象。";
        }
        if (currentTurnIndex != null && currentTurnIndex >= 3) {
            return "这一轮已经有足够线索了。请先提交一次最小修改，用新的评测结果验证你的判断。";
        }
        return "你的回答已经包含验证证据。请再补一句：如果这个判断是错的，下一步你会用哪个反例来排除它？";
    }

    private String buildCoachFeedback(CoachPrompt prompt, String answer) {
        String normalized = answer == null ? "" : answer.toLowerCase(Locale.ROOT);
        if (!hasText(answer)) {
            return "这次回答还没有形成可验证想法，下一步先补一个最小样例。";
        }
        if (mentionsAnswerLikeContent(normalized)) {
            return "你已经开始想到改法了，但这里先收回到证据层：先确认哪个输入特征会触发问题。";
        }
        if (mentionsEvidence(normalized)) {
            return "这次回答有证据意识，可以进入最小修改或反例验证。";
        }
        return "这次回答有方向，但证据还不够。下一步要把方向落到样例、变量变化或复杂度数量级上。";
    }

    private boolean mentionsEvidence(String normalizedAnswer) {
        if (normalizedAnswer == null) {
            return false;
        }
        return normalizedAnswer.contains("n=")
                || normalizedAnswer.contains("n =")
                || normalizedAnswer.contains("样例")
                || normalizedAnswer.contains("输入")
                || normalizedAnswer.contains("输出")
                || normalizedAnswer.contains("边界")
                || normalizedAnswer.contains("变量")
                || normalizedAnswer.contains("复杂度")
                || normalizedAnswer.contains("次数")
                || normalizedAnswer.contains("最大")
                || normalizedAnswer.matches(".*\\d+.*");
    }

    private boolean mentionsAnswerLikeContent(String normalizedAnswer) {
        if (normalizedAnswer == null) {
            return false;
        }
        return normalizedAnswer.contains("完整代码")
                || normalizedAnswer.contains("答案")
                || normalizedAnswer.contains("#include")
                || normalizedAnswer.contains("def ")
                || normalizedAnswer.contains("class ");
    }

    private String toJson(List<String> refs) {
        try {
            return objectMapper.writeValueAsString(refs);
        } catch (JsonProcessingException ignored) {
            return "[]";
        }
    }

    private List<String> mergeRefs(List<String> left, List<String> right) {
        return java.util.stream.Stream.concat(
                        left == null ? java.util.stream.Stream.empty() : left.stream(),
                        right == null ? java.util.stream.Stream.empty() : right.stream()
                )
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private List<String> parseRefs(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }

    private CoachPromptResponse responseWithTurns(CoachPrompt prompt) {
        List<CoachPrompt> turns = coachPromptRepository.findBySubmissionIdOrderByTurnIndexAscCreatedAtAsc(prompt.getSubmissionId());
        List<CoachPromptResponse> turnResponses = turns.stream()
                .map(turn -> {
                    List<String> refs = parseRefs(turn.getEvidenceRefs());
                    return CoachPromptResponse.from(turn, refs, adaptiveStrategySignalFromPrompt(turn, refs));
                })
                .toList();
        List<String> refs = parseRefs(prompt.getEvidenceRefs());
        return CoachPromptResponse.from(prompt, refs, turnResponses, adaptiveStrategySignalFromPrompt(prompt, refs));
    }

    private CoachPromptResponse.CoachAdaptiveStrategySignal adaptiveStrategySignalFromPrompt(CoachPrompt prompt, List<String> evidenceRefs) {
        String strategy = (evidenceRefs == null ? List.<String>of() : evidenceRefs)
                .stream()
                .filter(ref -> ref != null && ref.startsWith("coach-strategy:"))
                .map(ref -> ref.substring("coach-strategy:".length()).trim())
                .filter(ref -> !ref.isBlank())
                .findFirst()
                .orElse("");
        if (strategy.isBlank()) {
            return null;
        }
        List<String> strategyRefs = (evidenceRefs == null ? List.<String>of() : evidenceRefs)
                .stream()
                .filter(ref -> ref != null && (ref.startsWith("coach-strategy:") || ref.startsWith("coach-adaptive:")))
                .distinct()
                .toList();
        return CoachPromptResponse.CoachAdaptiveStrategySignal.builder()
                .strategy(strategy)
                .reason(extractAdaptiveReason(prompt == null ? "" : prompt.getContextSummary(), strategy))
                .recommendedCoachMove(extractAdaptiveMove(prompt == null ? "" : prompt.getContextSummary(), strategy))
                .needsTeacherAttention(needsTeacherAttention(strategy, strategyRefs))
                .evidenceRefs(evidenceRefs == null ? List.of() : evidenceRefs)
                .build();
    }

    private String extractAdaptiveReason(String contextSummary, String strategy) {
        if (contextSummary == null || contextSummary.isBlank()) {
            return adaptiveStrategyLabel(strategy);
        }
        String marker = "Coach 自适应策略：";
        int start = contextSummary.indexOf(marker);
        if (start < 0) {
            return adaptiveStrategyLabel(strategy);
        }
        int valueStart = start + marker.length();
        int end = contextSummary.indexOf(" 下一问动作：", valueStart);
        if (end < 0) {
            end = contextSummary.indexOf('。', valueStart);
        }
        if (end < 0) {
            end = contextSummary.length();
        }
        String value = contextSummary.substring(valueStart, end).trim();
        return value.isBlank() ? adaptiveStrategyLabel(strategy) : value;
    }

    private String extractAdaptiveMove(String contextSummary, String strategy) {
        if (contextSummary == null || contextSummary.isBlank()) {
            return adaptiveStrategyMove(strategy);
        }
        String marker = " 下一问动作：";
        int start = contextSummary.indexOf(marker);
        if (start < 0) {
            return adaptiveStrategyMove(strategy);
        }
        int valueStart = start + marker.length();
        int end = contextSummary.indexOf('。', valueStart);
        if (end < 0) {
            end = contextSummary.length();
        }
        String value = contextSummary.substring(valueStart, end).trim();
        return value.isBlank() ? adaptiveStrategyMove(strategy) : value;
    }

    private boolean needsTeacherAttention(String strategy, List<String> evidenceRefs) {
        return STRATEGY_SAFETY_RESET.equals(strategy)
                || (STRATEGY_REDUCE_GRANULARITY.equals(strategy)
                && evidenceRefs != null
                && evidenceRefs.stream().anyMatch(ref -> ref.contains("previous_action:CONTRADICTED") || ref.contains("repeated_")));
    }

    private String adaptiveStrategy(String strategy) {
        return strategy == null ? "" : strategy.trim().toUpperCase(Locale.ROOT);
    }

    private String adaptiveStrategy(CoachPromptResponse.CoachAdaptiveStrategySignal adaptiveStrategySignal) {
        return adaptiveStrategy(adaptiveStrategySignal == null ? "" : adaptiveStrategySignal.getStrategy());
    }

    private String adaptiveStrategyLabel(String strategy) {
        return switch (adaptiveStrategy(strategy)) {
            case STRATEGY_REDUCE_GRANULARITY -> "降低提示颗粒度";
            case STRATEGY_COLLECT_EVIDENCE -> "收集可验证证据";
            case STRATEGY_VERIFY_MINIMAL_CHANGE -> "验证一个最小修改";
            case STRATEGY_TRANSFER_REFLECTION -> "通过后迁移复盘";
            case STRATEGY_SAFETY_RESET -> "回到证据层避免泄题";
            default -> "继续苏格拉底式追问";
        };
    }

    private String adaptiveStrategyMove(String strategy) {
        return switch (adaptiveStrategy(strategy)) {
            case STRATEGY_REDUCE_GRANULARITY -> "缩到最小失败样例、关键变量轨迹或单行输入输出对照。";
            case STRATEGY_COLLECT_EVIDENCE -> "要求学生补一个样例、变量变化、输入输出对照或复杂度数量级。";
            case STRATEGY_VERIFY_MINIMAL_CHANGE -> "让学生只做一个最小修改，并预测下一次评测现象。";
            case STRATEGY_TRANSFER_REFLECTION -> "让学生解释复杂度、边界样例或迁移到新测试。";
            case STRATEGY_SAFETY_RESET -> "避免确认改法，把问题拉回输入特征和测试现象。";
            default -> "继续提出短、具体、可验证的下一问。";
        };
    }

    private String firstText(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private int nextTurnIndex(Long submissionId) {
        return coachPromptRepository.findBySubmissionIdOrderByTurnIndexAscCreatedAtAsc(submissionId)
                .stream()
                .map(CoachPrompt::getTurnIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private String sanitizeAnswer(String answer) {
        return answer == null ? "" : answer.trim();
    }

    private String blankToNull(String value) {
        return hasText(value) ? value : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record LearningContext(String summary,
                                   List<String> evidenceRefs,
                                   CoachPromptResponse.CoachAdaptiveStrategySignal adaptiveStrategySignal) {
    }

    private record StrategyDecision(String strategy,
                                    String reason,
                                    String recommendedCoachMove,
                                    boolean needsTeacherAttention,
                                    List<String> evidenceRefs) {
    }
}

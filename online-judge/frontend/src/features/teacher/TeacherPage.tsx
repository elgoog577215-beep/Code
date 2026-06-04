import { useEffect, useMemo, useRef, useState } from "react";
import { ChartNoAxesColumnIncreasing, Copy, ExternalLink, PenLine, RefreshCw, RotateCw, Settings } from "lucide-react";
import { ApiError, api } from "../../shared/api/client";
import type {
  AiQualityDimension,
  AiQualityOverview,
  AiQualitySourceSegment,
  AiQualityTrend,
  Assignment,
  AssignmentOverview,
  ClassGroup,
  DiagnosisEvalCandidates,
  DiagnosisEvalFixtureDraft,
  DiagnosisTag,
  ProblemCatalogItem,
  RecommendationEffectiveness
} from "../../shared/api/types";
import {
  assignmentStatusLabel,
  abilityLabel,
  answerLeakRiskLabel,
  aiDependencyStatusLabel,
  displayText,
  formatDateTime,
  hintPolicyLabel,
  issueLabel,
  learningStageLabel,
  looksCorruptText,
  masteryGrowthStatusLabel,
  postAcTransferPhaseLabel,
  recurringMisconceptionStatusLabel,
  selfExplanationStatusLabel,
  teachingActionActorLabel,
  teachingActionTypeLabel,
  verdictLabel
} from "../../shared/format";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea, TextInput } from "../../shared/ui/Field";
import { DifficultyPill, StatusPill, VerdictPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };
type PillTone = "neutral" | "success" | "warning" | "danger" | "info";
type CorrectionDraft = {
  studentProfileId: number;
  submissionId: number;
  correctedIssueTag: string;
  correctedFineGrainedTag: string;
  teacherNote: string;
};

const EMPTY_ASSIGNMENT = {
  id: "",
  title: "",
  description: "",
  classGroupId: "",
  hintPolicy: "L2",
  status: "ACTIVE",
  problemIds: [] as number[]
};

function cleanAssignmentTitle(value?: string | null, fallback = "课堂作业") {
  const title = displayText(value, fallback);
  return title.includes("试点任务") ? "课堂编程作业" : title;
}

function cleanAssignmentDescription(value?: string | null) {
  const description = displayText(value, "");
  if (description.includes("演示") || description.includes("诊断")) {
    return "";
  }
  return description;
}

function nonEmptyText(value?: string | null) {
  const text = displayText(value, "");
  return /^暂无[。.!！?？]?$/.test(text) ? "" : text;
}

function teacherErrorMessage(error: unknown, fallback: string) {
  const base = fallback.replace(/[。.!！?？\s]+$/, "");
  if (error instanceof ApiError) {
    if (error.status >= 500) {
      return `${base}，服务暂时不可用。`;
    }
    if (error.status === 404) {
      return `${base}，未找到课堂资源。`;
    }
    return `${base}，${error.message}`;
  }
  const detail = error instanceof Error ? error.message : "";
  if (!detail || detail === fallback) {
    return fallback;
  }
  if (/请求失败\s*\(\d+\)/.test(detail)) {
    return `${base}，服务暂时不可用。`;
  }
  return `${base}，${detail}`;
}

function classReviewFeedbackLabel(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "ACCEPTED":
      return "已采纳";
    case "DISMISSED":
      return "已忽略";
    case "MODIFIED":
      return "已调整";
    default:
      return "";
  }
}

function classReviewFeedbackTone(value?: string | null): PillTone {
  switch ((value || "").toUpperCase()) {
    case "ACCEPTED":
      return "success";
    case "MODIFIED":
      return "info";
    case "DISMISSED":
      return "neutral";
    default:
      return "neutral";
  }
}

function interventionImpactTone(value?: string | null): PillTone {
  switch ((value || "").toUpperCase()) {
    case "IMPROVED":
      return "success";
    case "SHIFTED":
      return "info";
    case "STILL_STUCK":
      return "danger";
    case "WAITING_FOLLOWUP":
      return "warning";
    case "DISMISSED":
    case "NO_FEEDBACK":
      return "neutral";
    default:
      return "neutral";
  }
}

function interventionImpactLabel(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "NO_FEEDBACK":
      return "待反馈";
    case "DISMISSED":
      return "已忽略";
    case "WAITING_FOLLOWUP":
      return "等后续";
    case "IMPROVED":
      return "已改善";
    case "SHIFTED":
      return "错因转移";
    case "STILL_STUCK":
      return "仍卡住";
    default:
      return value || "待观察";
  }
}

function aiQualityStatusLabel(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "HEALTHY":
      return "健康";
    case "WATCH":
      return "观察";
    case "ACTION_NEEDED":
      return "需处理";
    default:
      return value || "待观察";
  }
}

function aiQualityStatusTone(value?: string | null): PillTone {
  switch ((value || "").toUpperCase()) {
    case "HEALTHY":
      return "success";
    case "WATCH":
      return "warning";
    case "ACTION_NEEDED":
      return "danger";
    default:
      return "neutral";
  }
}

function evalReadinessLabel(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "READY":
      return "可沉淀";
    case "PARTIAL":
      return "可筛选";
    case "NO_SAMPLE":
      return "无样本";
    case "INSUFFICIENT_SIGNAL":
      return "观察";
    default:
      return value || "待判断";
  }
}

function recommendationSeverityTone(value?: string | null): PillTone {
  switch ((value || "").toUpperCase()) {
    case "HIGH":
      return "danger";
    case "MEDIUM":
    case "WATCH":
      return "warning";
    case "LOW":
      return "info";
    default:
      return "neutral";
  }
}

function coachAnswerQualityLabel(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "SAFETY_RISK":
      return "疑似越界";
    case "EVIDENCE_INSUFFICIENT":
      return "证据不足";
    case "TEACHER_ATTENTION":
      return "需关注";
    case "TRANSFER_READY":
      return "可迁移";
    case "VERIFY_READY":
      return "可验证";
    case "WAITING_ANSWER":
      return "待回答";
    case "NO_COACH_SIGNAL":
      return "无追问";
    case "HEALTHY":
      return "稳定";
    default:
      return value || "待判断";
  }
}

function coachAnswerQualityTone(value?: string | null): PillTone {
  switch ((value || "").toUpperCase()) {
    case "SAFETY_RISK":
      return "danger";
    case "EVIDENCE_INSUFFICIENT":
    case "TEACHER_ATTENTION":
    case "WAITING_ANSWER":
      return "warning";
    case "TRANSFER_READY":
      return "success";
    case "VERIFY_READY":
      return "info";
    case "HEALTHY":
      return "success";
    default:
      return "neutral";
  }
}

function coachFollowupImpactLabel(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "SAME_ISSUE":
      return "仍卡同类";
    case "AWAITING_FOLLOWUP":
      return "等后续";
    case "ISSUE_SHIFTED":
      return "错因转移";
    case "VERDICT_CHANGED":
      return "阶段变化";
    case "NO_CLEAR_CHANGE":
      return "暂无变化";
    case "FOLLOWUP_ACCEPTED":
      return "追问后过";
    case "NO_IMPACT_SAMPLE":
      return "无样本";
    case "HEALTHY":
      return "稳定";
    default:
      return value || "待判断";
  }
}

function coachFollowupImpactTone(value?: string | null): PillTone {
  switch ((value || "").toUpperCase()) {
    case "SAME_ISSUE":
      return "danger";
    case "AWAITING_FOLLOWUP":
    case "NO_CLEAR_CHANGE":
      return "warning";
    case "ISSUE_SHIFTED":
    case "VERDICT_CHANGED":
      return "info";
    case "FOLLOWUP_ACCEPTED":
    case "HEALTHY":
      return "success";
    default:
      return "neutral";
  }
}

function dimensionLabel(dimension: AiQualityDimension) {
  return dimension.label || aiQualityDimensionFallback(dimension.dimension);
}

function aiQualityDimensionFallback(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "DIAGNOSIS_CONFIDENCE":
      return "诊断置信";
    case "EVIDENCE_GROUNDING":
      return "证据引用";
    case "HINT_SAFETY":
      return "提示安全";
    case "PROMPT_SAFETY_INCIDENT_LOOP":
      return "提示安全事件闭环";
    case "LEARNING_ACTION":
      return "学习动作";
    case "MODEL_RUNTIME":
      return "模型运行";
    case "TEACHER_CORRECTION":
      return "教师纠错沉淀";
    case "COACH_UNDERSTANDING":
      return "Coach 理解证据";
    case "COACH_FOLLOWUP_IMPACT_LOOP":
      return "Coach 后续成效闭环";
    case "RECOMMENDATION_LOOP":
      return "推荐学习闭环";
    case "POST_AC_TRANSFER_LOOP":
      return "AC 后迁移闭环";
    case "RECURRING_MISCONCEPTION_LOOP":
      return "复发误区闭环";
    case "SELF_EXPLANATION_MASTERY_LOOP":
      return "自解释能力闭环";
    case "AI_DEPENDENCY_INDEPENDENCE_LOOP":
      return "AI 支架自主性闭环";
    case "MASTERY_GROWTH_LOOP":
      return "长期成长闭环";
    case "TEACHING_ACTION_ORCHESTRATION_LOOP":
      return "教学动作编排闭环";
    case "CLASS_TEACHING_STRATEGY_LOOP":
      return "班级教学策略闭环";
    case "TEACHER_INTERVENTION_LOOP":
      return "教师介入闭环";
    default:
      return value || "AI 质量";
  }
}

function aiQualityScore(value?: number | null) {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return "--";
  }
  return `${Math.round(value)}`;
}

function runtimeAttributionLabel(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "QUOTA_LIMIT":
      return "额度不足";
    case "BUDGET_GUARD":
      return "预算保护";
    case "SAFETY_REJECTED":
      return "安全拒绝";
    case "VALIDATION_FAILED":
      return "结构校验";
    case "TIMEOUT":
      return "调用超时";
    case "PROVIDER_ERROR":
      return "服务异常";
    case "PARTIAL_COMPLETION":
      return "部分完成";
    case "UNKNOWN_RUNTIME_FAILURE":
      return "未知失败";
    case "NONE":
      return "运行稳定";
    default:
      return displayText(value, "待归因");
  }
}

function transportModeLabel(value?: string | null) {
  switch ((value || "").toLowerCase()) {
    case "stream":
      return "流式通道";
    case "single-call":
      return "单次通道";
    default:
      return displayText(value, "");
  }
}

type TransportTelemetry = {
  primaryTransportMode?: string | null;
  transportMode?: string | null;
  streamContentChunkCount?: number | null;
  streamNoContentCount?: number | null;
  streamInvalidChunkCount?: number | null;
  streamFallbackRetryCount?: number | null;
  streamFallbackRetryUsed?: boolean | null;
};

function transportTelemetryChips(value?: TransportTelemetry | null) {
  if (!value) {
    return [];
  }
  const chips: string[] = [];
  const mode = transportModeLabel(value.primaryTransportMode || value.transportMode);
  if (mode) {
    chips.push(`通道 ${mode}`);
  }
  const noContentCount =
    (value.streamNoContentCount || 0) > 0
      ? value.streamNoContentCount || 0
      : (value.primaryTransportMode || value.transportMode || "").toLowerCase() === "stream" && value.streamContentChunkCount === 0
        ? 1
        : 0;
  if (noContentCount > 0) {
    chips.push(`无内容 ${noContentCount}`);
  }
  if ((value.streamInvalidChunkCount || 0) > 0) {
    chips.push(`解析异常 ${value.streamInvalidChunkCount}`);
  }
  if ((value.streamFallbackRetryCount || 0) > 0) {
    chips.push(`重试 ${value.streamFallbackRetryCount}`);
  } else if (value.streamFallbackRetryUsed) {
    chips.push("重试 1");
  }
  return chips;
}

function recoveryStatusLabel(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "BLOCKED":
      return "需复核";
    case "RECOVERED":
      return "已恢复";
    case "NOT_APPLICABLE":
      return "";
    default:
      return displayText(value, "");
  }
}

function recoveryStatusTone(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "BLOCKED":
      return "blocked";
    case "RECOVERED":
      return "recovered";
    default:
      return "neutral";
  }
}

function qualityComparabilityLabel(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "NOT_COMPARABLE":
      return "暂不可比";
    case "PARTIAL":
      return "部分可比";
    case "COMPARABLE":
      return "可比";
    case "NOT_APPLICABLE":
      return "";
    default:
      return displayText(value, "");
  }
}

function qualityComparabilityTone(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "NOT_COMPARABLE":
    case "BLOCKED":
      return "blocked";
    case "PARTIAL":
      return "partial";
    case "COMPARABLE":
    case "READY":
      return "comparable";
    default:
      return "neutral";
  }
}

type RuntimeRecoverySignal = {
  recoveryStatus?: string | null;
  recoveryCheckCount?: number | null;
  recoveryPassedCheckCount?: number | null;
  recoveryBlockedReasonCount?: number | null;
  recoveryPassedChecks?: string[] | null;
  recoveryBlockedReasons?: string[] | null;
  recoverySmokeRecommended?: boolean | null;
  recoverySmokeCaseId?: string | null;
  recoverySmokeRuntimeProfile?: string | null;
  recoverySmokeRequiredChecks?: string[] | null;
};

type RuntimeComparabilitySignal = {
  qualityComparabilityStatus?: string | null;
  qualityComparabilitySummary?: string | null;
  qualityComparabilityReasonCount?: number | null;
  qualityComparabilityReasons?: string[] | null;
};

function recoveryStatusChips(value?: RuntimeRecoverySignal | null) {
  const status = recoveryStatusLabel(value?.recoveryStatus);
  if (!value || !status) {
    return [];
  }
  const chips = [`恢复 ${status}`];
  if (value.recoverySmokeRecommended && value.recoverySmokeCaseId) {
    chips.push(`验证用例 ${value.recoverySmokeCaseId}`);
  }
  if (value.recoverySmokeRuntimeProfile) {
    chips.push(`运行配置 ${value.recoverySmokeRuntimeProfile}`);
  }
  const checkCount = value.recoveryCheckCount || value.recoverySmokeRequiredChecks?.length || 0;
  const passedCount = value.recoveryPassedCheckCount || 0;
  if (checkCount > 0) {
    chips.push(`检查 ${passedCount}/${checkCount}`);
  }
  return chips;
}

function sourceStatusLabel(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "HEALTHY":
    case "OK":
    case "SUCCESS":
    case "READY":
      return "稳定";
    case "WATCH":
    case "PARTIAL":
      return "观察";
    case "ACTION_NEEDED":
    case "FAILED":
    case "ERROR":
    case "BLOCKED":
      return "需复核";
    case "NOT_COMPARABLE":
      return "暂不可比";
    case "COMPARABLE":
      return "可比";
    case "NOT_APPLICABLE":
      return "";
    default:
      return displayText(value, "");
  }
}

function sourceCodePreview(value?: string | null) {
  const text = displayText(value, "");
  if (!text) {
    return "暂无代码预览";
  }
  return text.length > 220 ? `${text.slice(0, 220)}...` : text;
}

function riskWeight(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "HIGH":
      return 3;
    case "MEDIUM":
      return 2;
    case "LOW":
      return 1;
    default:
      return 0;
  }
}

function riskTone(value?: string | null): PillTone {
  switch ((value || "").toUpperCase()) {
    case "HIGH":
      return "danger";
    case "MEDIUM":
      return "warning";
    case "LOW":
      return "info";
    default:
      return "neutral";
  }
}

function TeacherOverviewSkeleton() {
  return (
    <div className="teacher-overview-skeleton" aria-label="正在加载课堂过程数据">
      <div className="teacher-kpi-strip teacher-kpi-strip--skeleton">
        {["参与学生", "提交次数", "通过次数", "通过率"].map(label => (
          <div key={label}>
            <span>{label}</span>
            <i />
          </div>
        ))}
      </div>
      <section className="teacher-main-grid teacher-main-grid--skeleton">
        <div className="teacher-block">
          <div className="teacher-block__head">
            <h3>高频问题</h3>
            <StatusPill tone="neutral">读取中</StatusPill>
          </div>
          <div className="skeleton-list" aria-hidden="true">
            <span />
            <span />
            <span />
          </div>
        </div>
        <div className="teacher-block">
          <div className="teacher-block__head">
            <h3>学生过程</h3>
            <StatusPill tone="neutral">读取中</StatusPill>
          </div>
          <div className="skeleton-list skeleton-list--wide" aria-hidden="true">
            <span />
            <span />
            <span />
          </div>
        </div>
      </section>
    </div>
  );
}

export default function TeacherPage() {
  const aiQualityRequestSeq = useRef(0);
  const evalCandidatesRequestSeq = useRef(0);
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [classes, setClasses] = useState<ClassGroup[]>([]);
  const [problems, setProblems] = useState<ProblemCatalogItem[]>([]);
  const [diagnosisTags, setDiagnosisTags] = useState<DiagnosisTag[]>([]);
  const [overview, setOverview] = useState<AssignmentOverview | null>(null);
  const [aiQuality, setAiQuality] = useState<AiQualityOverview | null>(null);
  const [aiQualityError, setAiQualityError] = useState<string | null>(null);
  const [aiQualityTrend, setAiQualityTrend] = useState<AiQualityTrend | null>(null);
  const [aiQualityTrendError, setAiQualityTrendError] = useState<string | null>(null);
  const [recommendationEffectiveness, setRecommendationEffectiveness] = useState<RecommendationEffectiveness | null>(null);
  const [recommendationEffectivenessError, setRecommendationEffectivenessError] = useState<string | null>(null);
  const [evalCandidates, setEvalCandidates] = useState<DiagnosisEvalCandidates | null>(null);
  const [evalCandidatesError, setEvalCandidatesError] = useState<string | null>(null);
  const [fixtureDraft, setFixtureDraft] = useState<DiagnosisEvalFixtureDraft | null>(null);
  const [fixtureDraftError, setFixtureDraftError] = useState<string | null>(null);
  const [activeAssignmentId, setActiveAssignmentId] = useState<number | null>(null);
  const [form, setForm] = useState(EMPTY_ASSIGNMENT);
  const [correctionDraft, setCorrectionDraft] = useState<CorrectionDraft | null>(null);
  const [alert, setAlert] = useState<Alert | null>(null);
  const [busy, setBusy] = useState(false);
  const [overviewLoading, setOverviewLoading] = useState(false);
  const [aiQualityLoading, setAiQualityLoading] = useState(false);
  const [aiQualityTrendLoading, setAiQualityTrendLoading] = useState(false);
  const [recommendationEffectivenessLoading, setRecommendationEffectivenessLoading] = useState(false);
  const [evalCandidatesLoading, setEvalCandidatesLoading] = useState(false);
  const [fixtureDraftLoading, setFixtureDraftLoading] = useState(false);

  useEffect(() => {
    void loadAll();
  }, []);

  const selectedAssignment = useMemo(
    () => assignments.find(item => item.id === activeAssignmentId) || assignments[0] || null,
    [activeAssignmentId, assignments]
  );

  const cleanAssignments = useMemo(
    () =>
      assignments.map(item => ({
        ...item,
        title: cleanAssignmentTitle(item.title, `课堂作业 #${item.id}`),
        description: cleanAssignmentDescription(item.description),
        className: displayText(item.className, "未绑定班级")
      })),
    [assignments]
  );

  const cleanClasses = useMemo(
    () =>
      classes.map(item => ({
        ...item,
        name: displayText(item.name, `班级 #${item.id}`),
        grade: displayText(item.grade, ""),
        teacherName: displayText(item.teacherName, "")
      })),
    [classes]
  );
  const coarseDiagnosisTags = useMemo(() => diagnosisTags.filter(tag => !tag.fineGrained), [diagnosisTags]);
  const fineDiagnosisTags = useMemo(() => diagnosisTags.filter(tag => tag.fineGrained), [diagnosisTags]);

  const selectedCleanAssignment = cleanAssignments.find(item => item.id === selectedAssignment?.id) || null;
  const diagnosisAssignment = overview?.assignment || selectedCleanAssignment || selectedAssignment;
  const activeInviteCode = selectedCleanAssignment?.inviteCode || selectedAssignment?.inviteCode || "";
  const activeTaskCount = selectedCleanAssignment?.tasks?.length || selectedAssignment?.tasks?.length || 0;
  const passRate = overview?.attemptCount ? Math.round((overview.passedAttemptCount / overview.attemptCount) * 100) : 0;
  const attentionStudents = overview?.students?.filter(student => student.needsAttention) || [];
  const visibleStudents = attentionStudents.length ? attentionStudents.slice(0, 6) : overview?.students?.slice(0, 6) || [];
  const reviewSuggestions = overview?.classReviewSuggestions?.slice(0, 3) || [];
  const priorityAiQualityDimensions = [
    "TEACHER_INTERVENTION_LOOP",
    "POST_AC_TRANSFER_LOOP",
    "RECURRING_MISCONCEPTION_LOOP",
    "SELF_EXPLANATION_MASTERY_LOOP",
    "AI_DEPENDENCY_INDEPENDENCE_LOOP",
    "MASTERY_GROWTH_LOOP",
    "TEACHING_ACTION_ORCHESTRATION_LOOP",
    "CLASS_TEACHING_STRATEGY_LOOP",
    "PROMPT_SAFETY_INCIDENT_LOOP",
    "RECOMMENDATION_LOOP",
    "COACH_FOLLOWUP_IMPACT_LOOP",
    "COACH_UNDERSTANDING",
    "LEARNING_ACTION"
  ];
  const actionableAiQualityDimensions =
    aiQuality?.qualityDimensions
      ?.filter(dimension => (dimension.status || "").toUpperCase() !== "HEALTHY")
      .sort((left, right) => {
        const leftIndex = priorityAiQualityDimensions.indexOf((left.dimension || "").toUpperCase());
        const rightIndex = priorityAiQualityDimensions.indexOf((right.dimension || "").toUpperCase());
        return (leftIndex === -1 ? 99 : leftIndex) - (rightIndex === -1 ? 99 : rightIndex);
      })
      .slice(0, 5) || [];
  const healthyAiQualityDimensions =
    aiQuality?.qualityDimensions
      ?.filter(dimension => (dimension.status || "").toUpperCase() === "HEALTHY")
      .slice(0, Math.max(0, 5 - actionableAiQualityDimensions.length)) || [];
  const visibleAiQualityDimensions = [...actionableAiQualityDimensions, ...healthyAiQualityDimensions].slice(0, 5);
  const topAiQualityPriority = aiQuality?.improvementPriorities?.[0] || null;
  const visibleEvalCandidates = evalCandidates?.candidates?.slice(0, 3) || [];
  const visibleTrendAssignments = aiQualityTrend?.assignments?.slice(0, 4) || [];
  const visibleSourceSegments = aiQualityTrend?.sourceSegments?.slice(0, 3) || [];
  const runtimeTransportChips = transportTelemetryChips(aiQuality?.runtimeAttributionSignal);
  const runtimeRecoveryChips = recoveryStatusChips(aiQuality?.runtimeAttributionSignal);
  const runtimeRecoveryTone = recoveryStatusTone(aiQuality?.runtimeAttributionSignal?.recoveryStatus);
  const runtimeRecoveryBlockedReasons = aiQuality?.runtimeAttributionSignal?.recoveryBlockedReasons?.slice(0, 2) || [];
  const runtimeRecoveryRequiredChecks = aiQuality?.runtimeAttributionSignal?.recoverySmokeRequiredChecks?.slice(0, 3) || [];
  const runtimeComparability = aiQuality?.runtimeAttributionSignal as RuntimeComparabilitySignal | undefined;
  const runtimeComparabilityLabel = qualityComparabilityLabel(runtimeComparability?.qualityComparabilityStatus);
  const runtimeComparabilityTone = qualityComparabilityTone(runtimeComparability?.qualityComparabilityStatus);
  const runtimeComparabilityReasons = runtimeComparability?.qualityComparabilityReasons?.slice(0, 2) || [];
  const evalBaselineStatus = aiQuality?.evalReadiness?.modelQualityBaselineStatus;
  const evalBaselineVisible =
    evalBaselineStatus && !["NOT_APPLICABLE", ""].includes(evalBaselineStatus.toUpperCase());
  const evalBaselineTone = qualityComparabilityTone(evalBaselineStatus);
  const evalBaselineReasons = aiQuality?.evalReadiness?.modelQualityBaselineReasons?.slice(0, 2) || [];
  const sourceSegmentTransportChips = (segment: AiQualitySourceSegment) => transportTelemetryChips(segment);
  const sourceSegmentRecoveryChips = (segment: AiQualitySourceSegment) => recoveryStatusChips(segment);
  const hasModelRuntimeTrendSignal = (aiQualityTrend?.modelRuntimeFailureCount || 0) > 0 || (aiQualityTrend?.modelPartialCount || 0) > 0;
  const visibleRecommendationSignals = recommendationEffectiveness?.feedbackSignals?.slice(0, 3) || [];
  const visibleRecommendationSegments =
    (recommendationEffectiveness?.byStrategy?.length
      ? recommendationEffectiveness.byStrategy
      : recommendationEffectiveness?.focusTags || []
    ).slice(0, 3);
  const safetyFixtures = fixtureDraft?.safetyFixtures || [];
  const runtimeFixtures = fixtureDraft?.runtimeFixtures || [];
  const fixtureDraftDiagnosisCount = fixtureDraft?.fixtureCount || 0;
  const fixtureDraftInterventionCount = fixtureDraft?.interventionFixtureCount || 0;
  const fixtureDraftSafetyCount = fixtureDraft?.safetyFixtureCount ?? safetyFixtures.length;
  const fixtureDraftRuntimeCount = fixtureDraft?.runtimeFixtureCount ?? runtimeFixtures.length;
  const fixtureDraftTotalCount =
    fixtureDraftDiagnosisCount + fixtureDraftInterventionCount + fixtureDraftSafetyCount + fixtureDraftRuntimeCount;
  const visibleSafetyFixtures = safetyFixtures.slice(0, 2);
  const visibleRuntimeFixtures = runtimeFixtures.slice(0, 2);
  const highestSafetyRisk = safetyFixtures.reduce<string | null>(
    (current, item) => (riskWeight(item.riskLevel) > riskWeight(current) ? item.riskLevel || current : current),
    null
  );
  const safetyBlockedReasons = Array.from(
    new Set(safetyFixtures.flatMap(item => item.blockedReasons || []).filter(Boolean))
  ).slice(0, 3);
  const hasInterventionTrendSignal =
    !!aiQualityTrend &&
    ((aiQualityTrend.interventionEvalCandidateCount || 0) > 0 ||
      (aiQualityTrend.interventionStillStuckCount || 0) > 0 ||
      (aiQualityTrend.interventionWaitingFollowupCount || 0) > 0);
  const hasPromptSafetyTrendSignal =
    !!aiQualityTrend &&
    ((aiQualityTrend.promptSafetyIncidentCount || 0) > 0 ||
      (aiQualityTrend.promptSafetyDowngradeCount || 0) > 0 ||
      (aiQualityTrend.promptSafetyHighRiskDowngradeCount || 0) > 0);
  const hasRecommendationRisk =
    !!recommendationEffectiveness &&
    (recommendationEffectiveness.unresolvedLearningSignalCount > 0 ||
      recommendationEffectiveness.teacherInterventionRecommendedCount > 0 ||
      recommendationEffectiveness.clickedWithoutSubmissionCount > 0);
  const aiQualityTone: PillTone = aiQualityError
    ? "danger"
    : aiQualityLoading
      ? "info"
      : topAiQualityPriority
        ? aiQualityStatusTone(topAiQualityPriority.severity)
        : aiQuality
          ? "success"
          : "neutral";
  const aiQualityStateLabel = aiQualityError
    ? "暂不可用"
    : aiQualityLoading
      ? "读取中"
      : topAiQualityPriority
        ? aiQualityStatusLabel(topAiQualityPriority.severity)
        : aiQuality
          ? "稳定"
          : "待读取";
  const firstTopIssue = overview?.topIssues?.[0] || null;
  const classroomRiskLabel = attentionStudents.length
    ? `${attentionStudents.length} 名学生需关注`
    : overview?.topIssues?.length
      ? `${overview.topIssues.length} 个共性问题`
      : "稳定";
  const classroomFocusLabel = firstTopIssue
    ? `${issueLabel(firstTopIssue.label)}${firstTopIssue.abilityPoint ? ` · ${abilityLabel(firstTopIssue.abilityPoint)}` : ""}`
    : attentionStudents[0]?.attentionReason || "暂无高频问题";
  const nextInspectionLabel = attentionStudents[0]
    ? `${displayText(attentionStudents[0].displayName, `学生 #${attentionStudents[0].studentProfileId}`)} · ${
        attentionStudents[0].latestFineGrainedIssue
          ? issueLabel(attentionStudents[0].latestFineGrainedIssue)
          : attentionStudents[0].latestIssue
            ? issueLabel(attentionStudents[0].latestIssue)
            : verdictLabel(attentionStudents[0].latestVerdict)
      }`
    : reviewSuggestions[0]?.title || "暂无重点";
  const stageStateTone: PillTone = overviewLoading
    ? "info"
    : !overview
      ? "neutral"
      : overview.strugglingStudentCount
        ? "warning"
        : "success";
  const stageStateLabel = overviewLoading
    ? "读取中"
    : !overview
      ? selectedAssignment
        ? "暂无数据"
        : "未选择作业"
      : overview.strugglingStudentCount
        ? `${overview.strugglingStudentCount} 人需关注`
        : "班级正常";

  async function loadAll() {
    setBusy(true);
    try {
      const [assignmentResult, classResult, problemResult] = await Promise.all([
        api.assignments(),
        api.classes(),
        api.problemCatalog()
      ]);
      setAssignments(assignmentResult);
      setClasses(classResult);
      setProblems(problemResult);
      if (!diagnosisTags.length) {
        setDiagnosisTags(await api.diagnosisTags());
      }
      void loadAiQualityTrend();
      void loadRecommendationEffectiveness();
      const preferred = assignmentResult.find(item => !looksCorruptText(item.title));
      const target = activeAssignmentId || preferred?.id || assignmentResult[0]?.id || null;
      if (target) {
        const assignment = assignmentResult.find(item => item.id === target) || assignmentResult[0];
        setActiveAssignmentId(assignment.id);
        populateForm(assignment);
        await loadOverview(assignment.id);
      } else {
        setOverview(null);
      }
    } catch (error) {
      setAlert({ type: "error", message: teacherErrorMessage(error, "课堂数据读取失败。") });
    } finally {
      setBusy(false);
    }
  }

  async function loadAiQualityTrend() {
    setAiQualityTrendLoading(true);
    setAiQualityTrendError(null);
    try {
      setAiQualityTrend(await api.aiQualityTrend());
    } catch (error) {
      setAiQualityTrend(null);
      setAiQualityTrendError(teacherErrorMessage(error, "跨作业 AI 质量趋势读取失败。"));
    } finally {
      setAiQualityTrendLoading(false);
    }
  }

  async function loadRecommendationEffectiveness() {
    setRecommendationEffectivenessLoading(true);
    setRecommendationEffectivenessError(null);
    try {
      setRecommendationEffectiveness(await api.recommendationEffectiveness());
    } catch (error) {
      setRecommendationEffectiveness(null);
      setRecommendationEffectivenessError(teacherErrorMessage(error, "推荐效果读取失败。"));
    } finally {
      setRecommendationEffectivenessLoading(false);
    }
  }

  async function loadOverview(id: number) {
    setActiveAssignmentId(id);
    setOverviewLoading(true);
    setAiQuality(null);
    setAiQualityError(null);
    setAiQualityLoading(true);
    setEvalCandidates(null);
    setEvalCandidatesError(null);
    setEvalCandidatesLoading(false);
    setFixtureDraft(null);
    setFixtureDraftError(null);
    try {
      const overviewResult = await api.assignmentOverview(id);
      setOverview(overviewResult);
    } catch (error) {
      setOverview(null);
      setAlert({ type: "error", message: teacherErrorMessage(error, "提交记录读取失败。") });
      const assignment = assignments.find(item => item.id === id);
      if (assignment) {
        populateForm(assignment);
      }
    } finally {
      setOverviewLoading(false);
    }
    void loadAiQuality(id);
  }

  async function loadAiQuality(id: number) {
    const requestSeq = aiQualityRequestSeq.current + 1;
    aiQualityRequestSeq.current = requestSeq;
    setAiQualityLoading(true);
    setAiQualityError(null);
    try {
      const result = await api.aiQualityOverview(id);
      if (aiQualityRequestSeq.current === requestSeq) {
        setAiQuality(result);
      }
    } catch (error) {
      if (aiQualityRequestSeq.current === requestSeq) {
        setAiQuality(null);
        setAiQualityError(teacherErrorMessage(error, "AI 质量信号读取失败。"));
      }
    } finally {
      if (aiQualityRequestSeq.current === requestSeq) {
        setAiQualityLoading(false);
      }
    }
  }

  async function loadEvalCandidates(id: number) {
    const requestSeq = evalCandidatesRequestSeq.current + 1;
    evalCandidatesRequestSeq.current = requestSeq;
    setEvalCandidatesLoading(true);
    setEvalCandidatesError(null);
    try {
      const result = await api.diagnosisEvalCandidates(id);
      if (evalCandidatesRequestSeq.current === requestSeq) {
        setEvalCandidates(result);
      }
    } catch (error) {
      if (evalCandidatesRequestSeq.current === requestSeq) {
        setEvalCandidates(null);
        setEvalCandidatesError(teacherErrorMessage(error, "Eval 候选样本读取失败。"));
      }
    } finally {
      if (evalCandidatesRequestSeq.current === requestSeq) {
        setEvalCandidatesLoading(false);
      }
    }
  }

  async function loadFixtureDraft() {
    if (!activeAssignmentId) {
      return;
    }
    setFixtureDraftLoading(true);
    setFixtureDraftError(null);
    try {
      setFixtureDraft(await api.diagnosisEvalFixtureDraft(activeAssignmentId));
    } catch (error) {
      setFixtureDraft(null);
      setFixtureDraftError(teacherErrorMessage(error, "Fixture 草稿读取失败。"));
    } finally {
      setFixtureDraftLoading(false);
    }
  }

  function populateForm(assignment: Assignment | null) {
    if (!assignment) {
      setForm(EMPTY_ASSIGNMENT);
      return;
    }
    setForm({
      id: String(assignment.id),
      title: cleanAssignmentTitle(assignment.title, ""),
      description: cleanAssignmentDescription(assignment.description),
      classGroupId: assignment.classGroupId ? String(assignment.classGroupId) : "",
      hintPolicy: assignment.hintPolicy || "L2",
      status: assignment.status || "ACTIVE",
      problemIds: (assignment.tasks || []).map(task => task.problemId)
    });
  }

  async function saveAssignment() {
    if (!form.title.trim() || form.problemIds.length === 0) {
      setAlert({ type: "error", message: "请填写作业标题，并至少绑定一个学习任务。" });
      return;
    }
    setBusy(true);
    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      classGroupId: form.classGroupId ? Number(form.classGroupId) : null,
      hintPolicy: form.hintPolicy,
      status: form.status,
      problemIds: form.problemIds
    };
    try {
      const result = form.id ? await api.updateAssignment(Number(form.id), payload) : await api.createAssignment(payload);
      setAlert({ type: "success", message: "学习作业已保存。" });
      setActiveAssignmentId(result.id);
      await loadAll();
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "作业保存失败。" });
    } finally {
      setBusy(false);
    }
  }

  async function rotateInvite() {
    const id = form.id || activeAssignmentId;
    if (!id) {
      setAlert({ type: "error", message: "请先选择一个作业。" });
      return;
    }
    try {
      const result = await api.rotateInvite(Number(id));
      setAlert({ type: "success", message: `邀请码已更新为 ${result.inviteCode || "-"}` });
      await loadAll();
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "邀请码更新失败。" });
    }
  }

  async function copyInviteCode() {
    const inviteCode = selectedAssignment?.inviteCode;
    if (!inviteCode) {
      setAlert({ type: "error", message: "当前作业还没有邀请码。" });
      return;
    }
    try {
      await navigator.clipboard.writeText(inviteCode);
      setAlert({ type: "success", message: `邀请码 ${inviteCode} 已复制。` });
    } catch {
      setAlert({ type: "error", message: "邀请码复制失败，请手动复制。" });
    }
  }

  function toggleProblem(problemId: number) {
    setForm(current => ({
      ...current,
      problemIds: current.problemIds.includes(problemId)
        ? current.problemIds.filter(id => id !== problemId)
        : [...current.problemIds, problemId]
    }));
  }

  function openCorrection(student: AssignmentOverview["students"][number]) {
    if (!student.latestSubmissionId) {
      setAlert({ type: "error", message: "这名学生还没有可校正的提交。" });
      return;
    }
    setCorrectionDraft({
      studentProfileId: student.studentProfileId,
      submissionId: student.latestSubmissionId,
      correctedIssueTag: student.latestCorrection?.correctedIssueTag || firstKnownTag(student.latestIssueTag, coarseDiagnosisTags),
      correctedFineGrainedTag: student.latestCorrection?.correctedFineGrainedTag || firstKnownTag(student.latestFineGrainedIssue, fineDiagnosisTags),
      teacherNote: student.latestCorrection?.teacherNote || ""
    });
  }

  async function saveCorrection() {
    if (!activeAssignmentId || !correctionDraft) {
      return;
    }
    if (!correctionDraft.correctedIssueTag) {
      setAlert({ type: "error", message: "请先选择修正后的主要错因。" });
      return;
    }
    setBusy(true);
    try {
      await api.correctDiagnosis(activeAssignmentId, {
        submissionId: correctionDraft.submissionId,
        correctedIssueTag: correctionDraft.correctedIssueTag,
        correctedFineGrainedTag: correctionDraft.correctedFineGrainedTag || null,
        teacherNote: correctionDraft.teacherNote,
        evalCandidate: true,
        correctedBy: "teacher"
      });
      setAlert({ type: "success", message: "教师校正已保存，并会作为后续 eval 候选样例。" });
      setCorrectionDraft(null);
      await loadOverview(activeAssignmentId);
    } catch (error) {
      setAlert({ type: "error", message: teacherErrorMessage(error, "教师校正保存失败。") });
    } finally {
      setBusy(false);
    }
  }

  function firstKnownTag(value: string | null | undefined, tags: DiagnosisTag[]) {
    const key = (value || "").toUpperCase();
    return tags.some(tag => tag.id === key) ? key : "";
  }

  async function recordClassReviewFeedback(
    suggestion: NonNullable<AssignmentOverview["classReviewSuggestions"]>[number],
    actionType: "ACCEPTED" | "DISMISSED" | "MODIFIED"
  ) {
    if (!activeAssignmentId || !suggestion.suggestionKey) {
      return;
    }
    try {
      await api.recordClassReviewFeedback(activeAssignmentId, {
        suggestionKey: suggestion.suggestionKey,
        actionType,
        targetAbility: suggestion.targetAbility || null,
        exampleProblemId: suggestion.exampleProblemId || null,
        evidenceTags: suggestion.evidenceTags || [],
        teacherNote: suggestion.guidingQuestion || "",
        createdBy: "teacher"
      });
      await loadOverview(activeAssignmentId);
    } catch (error) {
      setAlert({ type: "error", message: teacherErrorMessage(error, "复盘反馈保存失败。") });
    }
  }

  async function recordClassStrategyFeedback(
    signal: NonNullable<AssignmentOverview["classTeachingStrategySignal"]>,
    actionType: "ACCEPTED" | "DISMISSED" | "MODIFIED"
  ) {
    if (!activeAssignmentId || !signal.strategyKey) {
      return;
    }
    const evidenceTags = [
      signal.focusTag,
      signal.focusAbility,
      ...(signal.sourceSignals || []),
      ...(signal.evidenceRefs || [])
    ].filter((value): value is string => Boolean(value && value.trim()));
    try {
      await api.recordClassReviewFeedback(activeAssignmentId, {
        suggestionKey: signal.strategyKey,
        actionType,
        targetAbility: signal.focusAbility || signal.focusLabel || null,
        exampleProblemId: null,
        evidenceTags,
        teacherNote: signal.teacherAction || signal.summary || "",
        createdBy: "teacher"
      });
      await loadOverview(activeAssignmentId);
    } catch (error) {
      setAlert({ type: "error", message: teacherErrorMessage(error, "课堂策略反馈保存失败。") });
    }
  }

  function renderAiQualitySection() {
    return (
      <section className="teacher-ai-quality" aria-label="AI 质量信号">
        <div className="teacher-ai-quality__head">
          <div>
            <h3>AI 质量信号</h3>
            {nonEmptyText(aiQuality?.qualityRiskSummary || aiQuality?.summary) && (
              <p>{nonEmptyText(aiQuality?.qualityRiskSummary || aiQuality?.summary)}</p>
            )}
          </div>
          <StatusPill tone={aiQualityTone}>{aiQualityStateLabel}</StatusPill>
        </div>

        {aiQualityError ? (
          <div className="teacher-ai-quality__empty">
            <strong>AI 质量信号暂不可用</strong>
            <span>{aiQualityError}</span>
          </div>
        ) : aiQualityLoading ? (
          <div className="teacher-ai-quality__empty">
            <strong>读取中</strong>
          </div>
        ) : aiQuality ? (
          <>
            <div className="teacher-ai-quality__summary">
              <div className="teacher-ai-quality__metrics">
                <div>
                  <span>分析样本</span>
                  <strong>{aiQuality.analyzedSubmissionCount}</strong>
                </div>
                <div>
                  <span>教师校正</span>
                  <strong>{aiQuality.correctionCount}</strong>
                </div>
                <div>
                  <span>Eval 候选</span>
                  <strong>{aiQuality.evalCandidateCount}</strong>
                </div>
                <div>
                  <span>低置信</span>
                  <strong>{aiQuality.lowConfidenceCount}</strong>
                </div>
              </div>

              <aside className="teacher-ai-quality__actions teacher-ai-quality__actions--summary">
                <div>
                  <span>优先改进</span>
                  <strong>
                    {topAiQualityPriority
                      ? aiQualityDimensionFallback(topAiQualityPriority.dimension)
                      : "稳定"}
                  </strong>
                  {nonEmptyText(topAiQualityPriority?.recommendedAction || topAiQualityPriority?.reason) && (
                    <p>{nonEmptyText(topAiQualityPriority?.recommendedAction || topAiQualityPriority?.reason)}</p>
                  )}
                </div>
                {aiQuality.runtimeAttributionSignal &&
                  (aiQuality.runtimeAttributionSignal.modelRuntimeFailureCount > 0 ||
                    aiQuality.runtimeAttributionSignal.modelPartialCount > 0) && (
                    <div>
                      <span>运行状态</span>
                      <strong>{runtimeAttributionLabel(aiQuality.runtimeAttributionSignal.primaryFailureType)}</strong>
                      {nonEmptyText(aiQuality.runtimeAttributionSignal.summary) && <p>{nonEmptyText(aiQuality.runtimeAttributionSignal.summary)}</p>}
                      {(runtimeTransportChips.length > 0 || runtimeRecoveryChips.length > 0 || runtimeComparabilityLabel) && (
                        <details className="teacher-compact-details teacher-runtime-details">
                          <summary>
                            <span>运行细节</span>
                            <StatusPill tone="neutral">已折叠</StatusPill>
                          </summary>
                          <div className="teacher-compact-details__body">
                            {runtimeTransportChips.length > 0 && (
                              <div className="teacher-ai-transport-chips" aria-label="运行通道">
                                {runtimeTransportChips.map(chip => (
                                  <span key={chip}>{chip}</span>
                                ))}
                              </div>
                            )}
                            {runtimeRecoveryChips.length > 0 && (
                              <div
                                className={`teacher-ai-recovery teacher-ai-recovery--${runtimeRecoveryTone}`}
                                aria-label="恢复验证"
                              >
                                <div className="teacher-ai-recovery__chips">
                                  {runtimeRecoveryChips.map(chip => (
                                    <span key={chip}>{chip}</span>
                                  ))}
                                </div>
                                {(runtimeRecoveryBlockedReasons.length > 0 || runtimeRecoveryRequiredChecks.length > 0) && (
                                  <div className="teacher-ai-recovery__detail">
                                    {runtimeRecoveryBlockedReasons.length > 0 && (
                                      <ul aria-label="恢复阻塞原因">
                                        {runtimeRecoveryBlockedReasons.map(reason => (
                                          <li key={reason}>{reason}</li>
                                        ))}
                                      </ul>
                                    )}
                                    {runtimeRecoveryRequiredChecks.length > 0 && (
                                      <ul aria-label="恢复检查项">
                                        {runtimeRecoveryRequiredChecks.map(check => (
                                          <li key={check}>{check}</li>
                                        ))}
                                      </ul>
                                    )}
                                  </div>
                                )}
                              </div>
                            )}
                            {runtimeComparabilityLabel && (
                              <div
                                className={`teacher-ai-comparability teacher-ai-comparability--${runtimeComparabilityTone}`}
                                aria-label="质量可比性"
                              >
                                <div className="teacher-ai-recovery__chips">
                                  <span>质量对比 {runtimeComparabilityLabel}</span>
                                  {(runtimeComparability?.qualityComparabilityReasonCount || 0) > 0 && (
                                    <span>原因 {runtimeComparability?.qualityComparabilityReasonCount}</span>
                                  )}
                                </div>
                                {runtimeComparability?.qualityComparabilitySummary && (
                                  <p>{runtimeComparability.qualityComparabilitySummary}</p>
                                )}
                                {runtimeComparabilityReasons.length > 0 && (
                                  <ul aria-label="质量对比原因">
                                    {runtimeComparabilityReasons.map(reason => (
                                      <li key={reason}>{reason}</li>
                                    ))}
                                  </ul>
                                )}
                              </div>
                            )}
                          </div>
                        </details>
                      )}
                      {aiQuality.runtimeAttributionSignal.recommendedAction && (
                        <small>{aiQuality.runtimeAttributionSignal.recommendedAction}</small>
                      )}
                    </div>
                  )}
                <div>
                  <span>评测沉淀</span>
                  <strong>{evalReadinessLabel(aiQuality.evalReadiness?.status)}</strong>
                  {nonEmptyText(aiQuality.evalReadiness?.summary) && <p>{nonEmptyText(aiQuality.evalReadiness?.summary)}</p>}
                  {(aiQuality.evalReadiness?.interventionCandidateCount || 0) > 0 && (
                    <small>课堂介入候选 {aiQuality.evalReadiness?.interventionCandidateCount}</small>
                  )}
                    {aiQuality.evalReadiness?.recommendedAction && <small>{aiQuality.evalReadiness.recommendedAction}</small>}
                    {evalBaselineVisible && (
                      <details className="teacher-compact-details teacher-runtime-details">
                        <summary>
                          <span>模型基线</span>
                          <StatusPill tone={evalBaselineTone === "blocked" ? "warning" : evalBaselineTone === "comparable" ? "success" : "neutral"}>
                            {qualityComparabilityLabel(evalBaselineStatus)}
                          </StatusPill>
                        </summary>
                        <div className="teacher-compact-details__body">
                          {aiQuality.evalReadiness?.modelQualityBaselineSummary && (
                            <p>{aiQuality.evalReadiness.modelQualityBaselineSummary}</p>
                          )}
                          {evalBaselineReasons.length > 0 && (
                            <ul aria-label="模型质量基线原因" className="teacher-runtime-list">
                              {evalBaselineReasons.map(reason => (
                                <li key={reason}>{reason}</li>
                              ))}
                            </ul>
                          )}
                        </div>
                      </details>
                    )}
                </div>
              </aside>
            </div>

            <section className="teacher-ai-trend" aria-label="跨作业 AI 质量趋势">
              <div className="teacher-ai-trend__head">
                <div>
                  <strong>跨作业趋势</strong>
                  <p>
                    {aiQualityTrendLoading
                      ? "读取中"
                      : aiQualityTrendError
                        ? aiQualityTrendError
                        : nonEmptyText(aiQualityTrend?.summary)}
                  </p>
                </div>
                <StatusPill
                    tone={
                      aiQualityTrendError
                        ? "danger"
                        : aiQualityTrendLoading
                          ? "info"
                          : hasPromptSafetyTrendSignal
                            ? "danger"
                            : hasModelRuntimeTrendSignal
                              ? "warning"
                            : hasInterventionTrendSignal
                              ? "warning"
                              : aiQualityTrend
                                ? "success"
                                : "neutral"
                    }
                  >
                    {aiQualityTrendError
                      ? "暂不可用"
                      : aiQualityTrendLoading
                        ? "读取中"
                        : hasPromptSafetyTrendSignal
                          ? "需复核"
                          : hasModelRuntimeTrendSignal
                            ? "需归因"
                            : hasInterventionTrendSignal
                              ? "需复盘"
                              : aiQualityTrend
                                ? "已同步"
                                : "待同步"}
                </StatusPill>
              </div>

              {aiQualityTrendLoading || aiQualityTrendError ? (
                <div className="teacher-ai-quality__empty">
                  <strong>{aiQualityTrendError ? "趋势暂不可用" : "读取中"}</strong>
                  {aiQualityTrendError && <span>{aiQualityTrendError}</span>}
                </div>
              ) : !aiQualityTrend ? null : (
                <>
                  <div className="teacher-ai-trend__metrics">
                    <div>
                      <span>跨作业样本</span>
                      <strong>{aiQualityTrend.analyzedSubmissionCount}</strong>
                    </div>
                    <div>
                      <span>诊断 Eval</span>
                      <strong>{aiQualityTrend.evalCandidateCount}</strong>
                    </div>
                    <div>
                      <span>课堂介入</span>
                      <strong>{aiQualityTrend.interventionEvalCandidateCount || 0}</strong>
                    </div>
                    <div>
                      <span>提示安全</span>
                      <strong>{aiQualityTrend.promptSafetyIncidentCount || 0}</strong>
                    </div>
                    <div>
                      <span>安全降级</span>
                      <strong>{aiQualityTrend.promptSafetyDowngradeCount || 0}</strong>
                    </div>
                      <div>
                        <span>Coach 回退</span>
                        <strong>{aiQualityTrend.coachSafetyRejectionCount || 0}</strong>
                      </div>
                      <div>
                        <span>模型失败</span>
                        <strong>{aiQualityTrend.modelRuntimeFailureCount || 0}</strong>
                      </div>
                      <div>
                        <span>部分完成</span>
                        <strong>{aiQualityTrend.modelPartialCount || 0}</strong>
                      </div>
                      <div>
                        <span>仍卡同类</span>
                        <strong>{aiQualityTrend.interventionStillStuckCount || 0}</strong>
                    </div>
                    <div>
                      <span>等待后续</span>
                      <strong>{aiQualityTrend.interventionWaitingFollowupCount || 0}</strong>
                    </div>
                  </div>

                  <div className="teacher-ai-trend__body">
                    <div className="teacher-ai-trend__panel">
                      <div className="teacher-ai-trend__panel-head">
                        <strong>作业趋势点</strong>
                        <span>{aiQualityTrend.assignmentCount} 个作业</span>
                      </div>
                      {visibleTrendAssignments.length ? (
                        <div className="teacher-ai-trend__list">
                          {visibleTrendAssignments.map((point, index) => (
                            <article className="teacher-ai-trend-point" key={point.assignmentId || `${point.assignmentTitle || "assignment"}-${index}`}>
                              <div>
                                <strong>{displayText(point.assignmentTitle, `作业 #${point.assignmentId || "-"}`)}</strong>
                                <span>
                                  样本 {point.analyzedSubmissionCount} · 校正 {point.correctionCount} · 低置信 {point.lowConfidenceCount}
                                </span>
                              </div>
                              <div className="teacher-ai-trend-point__badges">
                                {(point.interventionEvalCandidateCount || 0) > 0 && (
                                  <StatusPill tone="info">介入 {point.interventionEvalCandidateCount}</StatusPill>
                                )}
                                {(point.promptSafetyIncidentCount || 0) > 0 && (
                                  <StatusPill tone="danger">安全 {point.promptSafetyIncidentCount}</StatusPill>
                                )}
                                {(point.promptSafetyDowngradeCount || 0) > 0 && (
                                  <StatusPill tone="warning">降级 {point.promptSafetyDowngradeCount}</StatusPill>
                                )}
                                  {(point.coachSafetyRejectionCount || 0) > 0 && (
                                    <StatusPill tone="danger">Coach {point.coachSafetyRejectionCount}</StatusPill>
                                  )}
                                  {(point.modelRuntimeFailureCount || 0) > 0 && (
                                    <StatusPill tone="danger">模型 {point.modelRuntimeFailureCount}</StatusPill>
                                  )}
                                  {(point.modelPartialCount || 0) > 0 && (
                                    <StatusPill tone="warning">部分 {point.modelPartialCount}</StatusPill>
                                  )}
                                  {(point.interventionStillStuckCount || 0) > 0 && (
                                    <StatusPill tone="danger">仍卡 {point.interventionStillStuckCount}</StatusPill>
                                )}
                                {(point.interventionWaitingFollowupCount || 0) > 0 && (
                                  <StatusPill tone="warning">等待 {point.interventionWaitingFollowupCount}</StatusPill>
                                )}
                              </div>
                            </article>
                          ))}
                        </div>
                      ) : null}
                    </div>

                    <div className="teacher-ai-trend__panel">
                      <div className="teacher-ai-trend__panel-head">
                        <strong>来源质量</strong>
                        <span>{visibleSourceSegments.length} 个片段</span>
                      </div>
                      {visibleSourceSegments.length ? (
                        <div className="teacher-ai-trend__source-list">
                            {visibleSourceSegments.map(segment => {
                              const transportChips = sourceSegmentTransportChips(segment);
                              const recoveryChips = sourceSegmentRecoveryChips(segment);
                              const recoveryTone = recoveryStatusTone(segment.recoveryStatus);
                              const recoveryBlockedReasons = segment.recoveryBlockedReasons?.slice(0, 2) || [];
                              const comparabilityLabel = qualityComparabilityLabel(segment.qualityComparabilityStatus);
                              const comparabilityTone = qualityComparabilityTone(segment.qualityComparabilityStatus);
                              const comparabilityReasons = segment.qualityComparabilityReasons?.slice(0, 2) || [];
                              return (
                              <article
                                className="teacher-ai-source"
                                key={`${segment.sourceType}-${segment.versionLabel}-${segment.status}-${segment.failureStage || "none"}-${segment.failureReason || "none"}`}
                              >
                                <div>
                                  <strong>{displayText(segment.versionLabel || segment.status, segment.sourceType || "AI 来源")}</strong>
                                  <span>
                                    {displayText(segment.provider, "AI 来源")}
                                    {segment.status ? ` · ${sourceStatusLabel(segment.status)}` : ""}
                                    {segment.runtimeMode ? ` · ${transportModeLabel(segment.runtimeMode)}` : ""}
                                  </span>
                                </div>
                                <small>
                                  样本 {segment.analyzedSubmissionCount} · 校正 {segment.correctionCount} · 低置信 {segment.lowConfidenceCount} · 泄题{" "}
                                  {segment.highLeakRiskCount} · 安全 {segment.promptSafetyIncidentCount || 0} · 失败{" "}
                                  {segment.modelRuntimeFailureCount || 0} · 部分 {segment.modelPartialCount || 0}
                                  {segment.failureStage || segment.failureReason
                                    ? ` · 归因 ${displayText(segment.failureStage, "未知阶段")}/${runtimeAttributionLabel(segment.failureReason)}`
                                    : ""}
                                </small>
                                {(transportChips.length > 0 || recoveryChips.length > 0 || comparabilityLabel) && (
                                  <details className="teacher-compact-details teacher-runtime-details teacher-runtime-details--source">
                                    <summary>
                                      <span>来源细节</span>
                                      <StatusPill tone={comparabilityTone === "blocked" ? "warning" : comparabilityTone === "comparable" ? "success" : "neutral"}>
                                        {comparabilityLabel || "查看"}
                                      </StatusPill>
                                    </summary>
                                    <div className="teacher-compact-details__body">
                                      {transportChips.length > 0 && (
                                        <div className="teacher-ai-transport-chips teacher-ai-transport-chips--source" aria-label="来源通道">
                                          {transportChips.map(chip => (
                                            <span key={chip}>{chip}</span>
                                          ))}
                                        </div>
                                      )}
                                      {recoveryChips.length > 0 && (
                                        <div
                                          className={`teacher-ai-recovery teacher-ai-recovery--source teacher-ai-recovery--${recoveryTone}`}
                                          aria-label="来源恢复验证"
                                        >
                                          <div className="teacher-ai-recovery__chips">
                                            {recoveryChips.map(chip => (
                                              <span key={chip}>{chip}</span>
                                            ))}
                                          </div>
                                          {recoveryBlockedReasons.length > 0 && (
                                            <div className="teacher-ai-recovery__detail teacher-ai-recovery__detail--single">
                                              <ul aria-label="来源恢复阻塞原因">
                                                {recoveryBlockedReasons.map(reason => (
                                                  <li key={reason}>{reason}</li>
                                                ))}
                                              </ul>
                                            </div>
                                          )}
                                        </div>
                                      )}
                                      {comparabilityLabel && (
                                        <div
                                          className={`teacher-ai-comparability teacher-ai-comparability--source teacher-ai-comparability--${comparabilityTone}`}
                                          aria-label="来源质量可比性"
                                        >
                                          <div className="teacher-ai-recovery__chips">
                                            <span>质量对比 {comparabilityLabel}</span>
                                            {(segment.qualityComparabilityReasonCount || 0) > 0 && (
                                              <span>原因 {segment.qualityComparabilityReasonCount}</span>
                                            )}
                                          </div>
                                          {segment.qualityComparabilitySummary && <p>{segment.qualityComparabilitySummary}</p>}
                                          {comparabilityReasons.length > 0 && (
                                            <ul aria-label="来源质量对比原因">
                                              {comparabilityReasons.map(reason => (
                                                <li key={reason}>{reason}</li>
                                              ))}
                                            </ul>
                                          )}
                                        </div>
                                      )}
                                    </div>
                                  </details>
                                )}
                            </article>
                              );
                            })}
                        </div>
                      ) : null}
                    </div>
                  </div>
                </>
              )}
            </section>

            <section className="teacher-recommendation-effect" aria-label="推荐学习闭环效果">
              <div className="teacher-recommendation-effect__head">
                <div>
                  <strong>推荐学习闭环</strong>
                  <p>
                    {recommendationEffectivenessLoading
                      ? "读取中"
                      : recommendationEffectivenessError
                        ? recommendationEffectivenessError
                        : nonEmptyText(recommendationEffectiveness?.summary)}
                  </p>
                </div>
                <StatusPill
                  tone={
                    recommendationEffectivenessError
                      ? "danger"
                      : recommendationEffectivenessLoading
                        ? "info"
                        : hasRecommendationRisk
                          ? "warning"
                          : recommendationEffectiveness
                            ? "success"
                            : "neutral"
                  }
                >
                  {recommendationEffectivenessError
                    ? "暂不可用"
                    : recommendationEffectivenessLoading
                      ? "读取中"
                      : hasRecommendationRisk
                        ? "需校准"
                        : recommendationEffectiveness
                          ? "已同步"
                          : "待同步"}
                </StatusPill>
              </div>

              {recommendationEffectivenessLoading || recommendationEffectivenessError ? (
                <div className="teacher-ai-quality__empty">
                  <strong>{recommendationEffectivenessError ? "推荐效果暂不可用" : "读取中"}</strong>
                  {recommendationEffectivenessError && <span>{recommendationEffectivenessError}</span>}
                </div>
              ) : !recommendationEffectiveness ? null : (
                <>
                  <div className="teacher-recommendation-effect__metrics">
                    <div>
                      <span>曝光</span>
                      <strong>{recommendationEffectiveness.exposureCount}</strong>
                    </div>
                    <div>
                      <span>点击</span>
                      <strong>{recommendationEffectiveness.clickCount}</strong>
                    </div>
                    <div>
                      <span>后续提交</span>
                      <strong>{recommendationEffectiveness.followupSubmissionCount}</strong>
                    </div>
                    <div>
                      <span>后续通过</span>
                      <strong>{recommendationEffectiveness.acceptedFollowupCount}</strong>
                    </div>
                    <div>
                      <span>同类未解</span>
                      <strong>{recommendationEffectiveness.unresolvedLearningSignalCount}</strong>
                    </div>
                    <div>
                      <span>需介入</span>
                      <strong>{recommendationEffectiveness.teacherInterventionRecommendedCount}</strong>
                    </div>
                  </div>

                  <div className="teacher-recommendation-effect__body">
                    <div className="teacher-recommendation-effect__panel">
                      <div className="teacher-recommendation-effect__panel-head">
                        <strong>反馈信号</strong>
                        <span>{visibleRecommendationSignals.length} 条</span>
                      </div>
                      {visibleRecommendationSignals.length ? (
                        <div className="teacher-recommendation-effect__list">
                          {visibleRecommendationSignals.map(signal => (
                            <article className="teacher-recommendation-signal" key={`${signal.signal}-${signal.strategy || "strategy"}`}>
                              <div className="teacher-recommendation-signal__main">
                                <div>
                                  <strong>{signal.summary || signal.signal}</strong>
                                  {nonEmptyText(signal.recommendedAction) && <span>{nonEmptyText(signal.recommendedAction)}</span>}
                                </div>
                                <StatusPill tone={recommendationSeverityTone(signal.severity)}>
                                  {signal.severity || "观察"}
                                </StatusPill>
                              </div>
                              <small>
                                策略 {displayText(signal.strategy, "未标注")} · 证据 {signal.evidenceCount}
                              </small>
                            </article>
                          ))}
                        </div>
                      ) : null}
                    </div>

                    <div className="teacher-recommendation-effect__panel">
                      <div className="teacher-recommendation-effect__panel-head">
                        <strong>{recommendationEffectiveness.byStrategy?.length ? "策略片段" : "焦点标签"}</strong>
                        <span>{visibleRecommendationSegments.length} 项</span>
                      </div>
                      {visibleRecommendationSegments.length ? (
                        <div className="teacher-recommendation-effect__segments">
                          {visibleRecommendationSegments.map(segment => (
                            <article className="teacher-recommendation-segment" key={segment.key}>
                              <div>
                                <strong>{displayText(segment.label, segment.key)}</strong>
                                <span>
                                  点击 {segment.clickCount} · 提交 {segment.followupSubmissionCount} · 通过 {segment.acceptedFollowupCount}
                                </span>
                              </div>
                              <small>
                                同类未解 {segment.unresolvedLearningSignalCount} · 需介入 {segment.teacherInterventionRecommendedCount}
                              </small>
                            </article>
                          ))}
                        </div>
                      ) : null}
                    </div>
                  </div>
                </>
              )}
            </section>

            <details
              className="teacher-compact-details teacher-ai-quality__details"
              onToggle={event => {
                if (
                  event.currentTarget.open &&
                  activeAssignmentId &&
                  !evalCandidates &&
                  !evalCandidatesLoading &&
                  !evalCandidatesError
                ) {
                  void loadEvalCandidates(activeAssignmentId);
                }
              }}
            >
              <summary>
                <span>AI 质量详情</span>
                <StatusPill tone="neutral">
                  {visibleAiQualityDimensions.length + (evalCandidates?.candidateCount ?? aiQuality.evalCandidateCount)} 项
                </StatusPill>
              </summary>
              <div className="teacher-compact-details__body">
                <div className="teacher-ai-quality__grid">
                  <div className="teacher-ai-quality__dimensions">
                    {visibleAiQualityDimensions.length ? (
                      visibleAiQualityDimensions.map(dimension => (
                        <article className="teacher-ai-dimension" key={dimension.dimension}>
                          <div>
                            <strong>{dimensionLabel(dimension)}</strong>
                            {nonEmptyText(dimension.summary || dimension.recommendedAction) && (
                              <span>{nonEmptyText(dimension.summary || dimension.recommendedAction)}</span>
                            )}
                          </div>
                          <div className="teacher-ai-dimension__score">
                            <StatusPill tone={aiQualityStatusTone(dimension.status)}>
                              {aiQualityStatusLabel(dimension.status)}
                            </StatusPill>
                            <b>{aiQualityScore(dimension.score)}</b>
                          </div>
                          <small>
                            {nonEmptyText(dimension.recommendedAction)}
                            {dimension.evidenceRefs?.length ? ` · 证据 ${dimension.evidenceRefs.length} 条` : ""}
                          </small>
                        </article>
                      ))
                    ) : null}
                  </div>
                </div>

                <div className="teacher-eval-candidates" aria-label="诊断 eval 候选样本">
                  <div className="teacher-eval-candidates__head">
                    <div>
                      <strong>诊断 Eval 候选样本</strong>
                    </div>
                    <div className="teacher-eval-candidates__tools">
                      <StatusPill tone={evalCandidatesError ? "danger" : visibleEvalCandidates.length ? "info" : "neutral"}>
                        {evalCandidatesError
                          ? "暂不可用"
                          : evalCandidatesLoading
                            ? "读取中"
                            : `${evalCandidates?.candidateCount ?? aiQuality.evalCandidateCount} 条`}
                      </StatusPill>
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={() => void loadFixtureDraft()}
                        disabled={fixtureDraftLoading || evalCandidatesLoading}
                      >
                        {fixtureDraftLoading ? "生成中" : "预览草稿"}
                      </Button>
                    </div>
                  </div>

                  {evalCandidatesError ? (
                    <div className="teacher-ai-quality__empty">
                      <strong>候选样本暂不可用</strong>
                      <span>{evalCandidatesError}</span>
                    </div>
                  ) : evalCandidatesLoading ? (
                    <div className="teacher-ai-quality__empty">
                      <strong>读取中</strong>
                    </div>
                  ) : visibleEvalCandidates.length ? (
                    <div className="teacher-eval-candidates__list">
                      {visibleEvalCandidates.map(candidate => (
                        <article className="teacher-eval-candidate" key={`${candidate.correctionId}-${candidate.submissionId}`}>
                          <div className="teacher-eval-candidate__main">
                            <div>
                              <strong>{displayText(candidate.problemTitle, `题目 #${candidate.problemId || "-"}`)}</strong>
                              <span>
                                {verdictLabel(candidate.verdict)}
                                {candidate.languageName ? ` · ${candidate.languageName}` : ""}
                                {candidate.scenario ? ` · ${candidate.scenario}` : ""}
                              </span>
                            </div>
                            <StatusPill tone="info">#{candidate.correctionId}</StatusPill>
                          </div>
                          <div className="teacher-eval-candidate__tags">
                            <span>
                              原诊断：{issueLabel(candidate.originalFineGrainedTag || candidate.originalIssueTag)}
                            </span>
                            <span>
                              教师修正：{issueLabel(candidate.correctedFineGrainedTag || candidate.correctedIssueTag)}
                            </span>
                          </div>
                          {candidate.teacherNote && <p>{candidate.teacherNote}</p>}
                          <pre>{sourceCodePreview(candidate.sourceCodePreview || candidate.sourceCode)}</pre>
                        </article>
                      ))}
                    </div>
                  ) : null}

                  {(fixtureDraft || fixtureDraftError) && (
                    <details className="teacher-fixture-draft-preview">
                      <summary>
                        <span>Fixture 草稿预览</span>
                        <StatusPill tone={fixtureDraftError ? "danger" : fixtureDraftTotalCount ? "info" : "neutral"}>
                          {fixtureDraftError ? "失败" : `${fixtureDraftTotalCount} 条`}
                        </StatusPill>
                      </summary>
                      {fixtureDraftError ? (
                        <div className="teacher-ai-quality__empty">
                          <strong>Fixture 草稿暂不可用</strong>
                          <span>{fixtureDraftError}</span>
                        </div>
                      ) : fixtureDraft ? (
                        <div className="teacher-fixture-draft-preview__body">
                          <p>{fixtureDraft.summary || "请人工审查后再沉淀进测试资源。"}</p>
                          <div className="teacher-fixture-draft-preview__stats">
                            <span>诊断草稿 {fixtureDraftDiagnosisCount}</span>
                            <span>课堂介入 {fixtureDraftInterventionCount}</span>
                            <span>提示安全 {fixtureDraftSafetyCount}</span>
                            <span>模型运行 {fixtureDraftRuntimeCount}</span>
                          </div>
                          {fixtureDraftSafetyCount > 0 && (
                            <div className="teacher-fixture-draft-preview__safety">
                              <div className="teacher-fixture-draft-preview__safety-head">
                                <strong>提示安全草稿</strong>
                                <StatusPill tone={riskTone(highestSafetyRisk)}>{answerLeakRiskLabel(highestSafetyRisk)}</StatusPill>
                              </div>
                              {safetyBlockedReasons.length > 0 && (
                                <div className="teacher-fixture-draft-preview__safety-reasons">
                                  {safetyBlockedReasons.map(reason => (
                                    <span key={reason}>{reason}</span>
                                  ))}
                                </div>
                              )}
                              <div className="teacher-fixture-draft-preview__safety-list">
                                {visibleSafetyFixtures.map(item => (
                                  <article key={`${item.name}-${item.submissionId}`}>
                                    <div>
                                      <strong>提交 #{item.submissionId}</strong>
                                      <span>{displayText(item.problem?.title, "未命名题目")}</span>
                                    </div>
                                    <StatusPill tone={riskTone(item.riskLevel)}>{answerLeakRiskLabel(item.riskLevel)}</StatusPill>
                                    <p>
                                      {displayText(
                                        item.blockedReasons?.[0] || item.expectedSafetyAction || item.quality?.evalPurpose,
                                        "请审查风险来源、禁止短语和证据引用。"
                                      )}
                                    </p>
                                  </article>
                                ))}
                              </div>
                            </div>
                          )}
                          {fixtureDraftRuntimeCount > 0 && (
                            <div className="teacher-fixture-draft-preview__safety">
                              <div className="teacher-fixture-draft-preview__safety-head">
                                <strong>模型运行草稿</strong>
                                <StatusPill tone="warning">{fixtureDraftRuntimeCount} 条</StatusPill>
                              </div>
                              <div className="teacher-fixture-draft-preview__safety-list">
                                {visibleRuntimeFixtures.map(item => (
                                  <article key={`${item.name}-${item.submissionId}`}>
                                    <div>
                                      <strong>提交 #{item.submissionId}</strong>
                                      <span>{displayText(item.problem?.title, "未命名题目")}</span>
                                    </div>
                                    <StatusPill tone={item.failureType === "PARTIAL_COMPLETION" ? "warning" : "danger"}>
                                      {runtimeAttributionLabel(item.failureType)}
                                    </StatusPill>
                                    {transportTelemetryChips(item).length > 0 && (
                                      <div className="teacher-ai-transport-chips teacher-ai-transport-chips--source" aria-label="模型运行草稿传输归因">
                                        {transportTelemetryChips(item).map(chip => (
                                          <span key={chip}>{chip}</span>
                                        ))}
                                      </div>
                                    )}
                                    <p>
                                      {displayText(
                                        item.expectedRuntimeAction ||
                                          item.failureReason ||
                                          item.quality?.evalPurpose,
                                        "请审查模型运行归因、失败阶段和恢复动作。"
                                      )}
                                    </p>
                                  </article>
                                ))}
                              </div>
                            </div>
                          )}
                          <pre>
                            {JSON.stringify(
                              {
                                diagnosisFixtures: fixtureDraft.fixtures,
                                interventionFixtures: fixtureDraft.interventionFixtures || [],
                                safetyFixtures,
                                runtimeFixtures
                              },
                              null,
                              2
                            )}
                          </pre>
                        </div>
                      ) : null}
                    </details>
                  )}
                </div>
              </div>
            </details>
          </>
        ) : (
          null
        )}
      </section>
    );
  }

  return (
    <div className="stack teacher-page teacher-page--studio">
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="teacher-studio-shell">
        <header className="teacher-studio-topbar">
          <div className="teacher-mode-heading">
            <h1>教师</h1>
            <nav className="teacher-mode-tabs" aria-label="教师功能">
              <span className="teacher-mode-tab is-active">课堂</span>
              <ButtonLink to="/app/teacher-management" variant="ghost" icon={<Settings size={16} />}>
                管理
              </ButtonLink>
            </nav>
          </div>
          <div className="actions">
            <Button
              type="button"
              variant="ghost"
              className={busy ? "is-loading" : ""}
              onClick={() => void loadAll()}
              disabled={busy}
              icon={<RefreshCw size={18} />}
            >
              {busy ? "刷新中" : "刷新"}
            </Button>
          </div>
        </header>

        <div className="teacher-studio-layout">
          <aside className="teacher-rail">
            <div className="teacher-rail__head">
              <div>
                <StatusPill tone="neutral">作业</StatusPill>
                <h2>{cleanAssignments.length}</h2>
              </div>
              {activeInviteCode && <StatusPill tone="info">{activeInviteCode}</StatusPill>}
            </div>

            <div className="teacher-rail__list">
              {cleanAssignments.length ? (
                cleanAssignments.map(item => (
                  <button
                    type="button"
                    className={`teacher-assignment-item ${item.id === activeAssignmentId ? "is-active" : ""}`}
                    key={item.id}
                    onClick={() => {
                      populateForm(item);
                      void loadOverview(item.id);
                    }}
                  >
                    <div>
                      <div className="actions">
                        <StatusPill tone={item.status === "ACTIVE" ? "success" : "neutral"}>{assignmentStatusLabel(item.status)}</StatusPill>
                        <span className="meta-badge">{hintPolicyLabel(item.hintPolicy)}</span>
                      </div>
                      <h3>{item.title}</h3>
                      <div className="teacher-assignment-meta-line">
                        <span>{item.className || "未绑定班级"}</span>
                        <span>{item.tasks?.length || 0} 个任务</span>
                      </div>
                    </div>
                    <span className="teacher-assignment-code">邀请码 {item.inviteCode || "-"}</span>
                  </button>
                ))
              ) : (
                <EmptyState title="还没有作业" />
              )}
            </div>
          </aside>

          <main className="teacher-stage">
            <div className="teacher-stage__head">
              <div>
                <h2>{cleanAssignmentTitle(diagnosisAssignment?.title, "请选择作业")}</h2>
                <div className="teacher-stage__meta">
                  <span>{diagnosisAssignment ? displayText(diagnosisAssignment.className, "未绑定班级") : "未选择作业"}</span>
                  <span>{activeTaskCount} 个任务</span>
                  <span>{hintPolicyLabel(diagnosisAssignment?.hintPolicy)}</span>
                </div>
              </div>
              <div className="teacher-stage-toolbar">
                <StatusPill tone={stageStateTone}>{stageStateLabel}</StatusPill>
                {selectedAssignment?.inviteCode && (
                  <Button
                    type="button"
                    variant="secondary"
                    icon={<Copy size={16} />}
                    onClick={() => void copyInviteCode()}
                  >
                    复制邀请码
                  </Button>
                )}
                <ButtonLink to="/app/class-overview" variant="secondary" icon={<ChartNoAxesColumnIncreasing size={16} />}>
                  班级概览
                </ButtonLink>
                {selectedAssignment && (
                  <ButtonLink
                    to={`/app/teacher/assignment/${selectedAssignment.id}`}
                    variant="primary"
                    icon={<ExternalLink size={16} />}
                  >
                    作业详情
                  </ButtonLink>
                )}
              </div>
            </div>

            {overviewLoading ? (
              <TeacherOverviewSkeleton />
            ) : !overview ? (
              <EmptyState title={selectedAssignment ? "无过程数据" : "请选择作业"} />
            ) : (
              <>
                <div className="teacher-kpi-strip">
                  <div>
                    <span>参与学生</span>
                    <strong>{overview.participantCount}</strong>
                  </div>
                  <div>
                    <span>提交次数</span>
                    <strong>{overview.attemptCount}</strong>
                  </div>
                  <div>
                    <span>通过率</span>
                    <strong>{passRate}%</strong>
                  </div>
                  <div>
                    <span>需关注</span>
                    <strong>{attentionStudents.length}</strong>
                  </div>
                  <div>
                    <span>高频问题</span>
                    <strong>{overview.topIssues.length}</strong>
                  </div>
                </div>
                <section className="teacher-inspection-strip" aria-label="课堂巡视摘要">
                  <article className={attentionStudents.length ? "is-warning" : "is-stable"}>
                    <span>课堂风险</span>
                    <strong>{classroomRiskLabel}</strong>
                  </article>
                  <article>
                    <span>共性问题</span>
                    <strong>{classroomFocusLabel}</strong>
                  </article>
                  <article>
                    <span>先看谁</span>
                    <strong>{nextInspectionLabel}</strong>
                  </article>
                  <article className="teacher-inspection-strip__ai">
                    <span>AI 质量</span>
                    <strong>{aiQualityStateLabel}</strong>
                  </article>
                </section>
                <details className="teacher-system-drawer">
                  <summary>
                    <div>
                      <span>辅助信息</span>
                      <strong>AI、Coach、长期信号</strong>
                    </div>
                    <div className="teacher-system-drawer__chips">
                      {overview.classTeachingStrategySignal && <StatusPill tone="info">课堂策略</StatusPill>}
                      {overview.coachAnswerQualitySummary && (
                        <span className="teacher-system-signal-chip teacher-coach-quality">
                          Coach {coachAnswerQualityLabel(overview.coachAnswerQualitySummary.dominantGap)}
                        </span>
                      )}
                      {overview.coachFollowupImpactSummary && (
                        <span className="teacher-system-signal-chip teacher-coach-impact">
                          成效 {coachFollowupImpactLabel(overview.coachFollowupImpactSummary.dominantOutcome)}
                        </span>
                      )}
                      <StatusPill tone={aiQualityTone}>AI {aiQualityStateLabel}</StatusPill>
                      <span className="teacher-system-signal-chip teacher-compact-details">详情</span>
                    </div>
                  </summary>
                  <div className="teacher-system-drawer__body">
                {overview.postAcTransferSummary && (
                  <div className="teacher-transfer-summary">
                    <span>AC 后迁移</span>
                    <strong>{overview.postAcTransferSummary}</strong>
                  </div>
                )}
                {overview.recurringMisconceptionSummary && (
                  <div className="teacher-transfer-summary teacher-transfer-summary--recurring">
                    <span>复发误区</span>
                    <strong>{overview.recurringMisconceptionSummary}</strong>
                  </div>
                )}
                {overview.selfExplanationSummary && (
                  <div className="teacher-transfer-summary teacher-transfer-summary--explanation">
                    <span>自解释能力</span>
                    <strong>{overview.selfExplanationSummary}</strong>
                  </div>
                )}
                {overview.coachAnswerQualitySummary && (
                  <div className="teacher-coach-quality" aria-label="Coach 回答质量">
                    <div className="teacher-coach-quality__head">
                      <div>
                        <span>Coach 回答质量</span>
                        {nonEmptyText(overview.coachAnswerQualitySummary.summary) && (
                          <strong>{nonEmptyText(overview.coachAnswerQualitySummary.summary)}</strong>
                        )}
                      </div>
                      <StatusPill tone={coachAnswerQualityTone(overview.coachAnswerQualitySummary.dominantGap)}>
                        {coachAnswerQualityLabel(overview.coachAnswerQualitySummary.dominantGap)}
                      </StatusPill>
                    </div>
                    <div className="teacher-coach-quality__metrics">
                      <div>
                        <span>已追问</span>
                        <strong>{overview.coachAnswerQualitySummary.promptedCount || 0}</strong>
                      </div>
                      <div>
                        <span>已回答</span>
                        <strong>{overview.coachAnswerQualitySummary.answeredCount || 0}</strong>
                      </div>
                      <div>
                        <span>可验证</span>
                        <strong>{overview.coachAnswerQualitySummary.verifiableCount || 0}</strong>
                      </div>
                      <div>
                        <span>可迁移</span>
                        <strong>{overview.coachAnswerQualitySummary.transferReadyCount || 0}</strong>
                      </div>
                      <div>
                        <span>证据不足</span>
                        <strong>{overview.coachAnswerQualitySummary.evidenceInsufficientCount || 0}</strong>
                      </div>
                      <div>
                        <span>疑似越界</span>
                        <strong>{overview.coachAnswerQualitySummary.safetyRiskCount || 0}</strong>
                      </div>
                      <div>
                        <span>安全回退</span>
                        <strong>{overview.coachAnswerQualitySummary.coachSafetyRejectionCount || 0}</strong>
                      </div>
                      <div>
                        <span>需关注</span>
                        <strong>{overview.coachAnswerQualitySummary.teacherAttentionCount || 0}</strong>
                      </div>
                    </div>
                    {(overview.coachAnswerQualitySummary.recommendedAction ||
                      overview.coachAnswerQualitySummary.evidenceRefs?.length) && (
                      <div className="teacher-coach-quality__action">
                        {overview.coachAnswerQualitySummary.recommendedAction && (
                          <small>{overview.coachAnswerQualitySummary.recommendedAction}</small>
                        )}
                        {overview.coachAnswerQualitySummary.evidenceRefs?.length ? (
                          <span>{overview.coachAnswerQualitySummary.evidenceRefs.length} 条证据</span>
                        ) : null}
                      </div>
                    )}
                  </div>
                )}
                {overview.coachFollowupImpactSummary && (
                  <div className="teacher-coach-impact" aria-label="Coach 后续成效">
                    <div className="teacher-coach-quality__head">
                      <div>
                        <span>Coach 后续成效</span>
                        {nonEmptyText(overview.coachFollowupImpactSummary.summary) && (
                          <strong>{nonEmptyText(overview.coachFollowupImpactSummary.summary)}</strong>
                        )}
                      </div>
                      <StatusPill tone={coachFollowupImpactTone(overview.coachFollowupImpactSummary.dominantOutcome)}>
                        {coachFollowupImpactLabel(overview.coachFollowupImpactSummary.dominantOutcome)}
                      </StatusPill>
                    </div>
                    <div className="teacher-coach-quality__metrics">
                      <div>
                        <span>影响样本</span>
                        <strong>{overview.coachFollowupImpactSummary.impactedCount || 0}</strong>
                      </div>
                      <div>
                        <span>追问后过</span>
                        <strong>{overview.coachFollowupImpactSummary.acceptedCount || 0}</strong>
                      </div>
                      <div>
                        <span>错因转移</span>
                        <strong>{overview.coachFollowupImpactSummary.shiftedCount || 0}</strong>
                      </div>
                      <div>
                        <span>仍卡同类</span>
                        <strong>{overview.coachFollowupImpactSummary.sameIssueCount || 0}</strong>
                      </div>
                      <div>
                        <span>阶段变化</span>
                        <strong>{overview.coachFollowupImpactSummary.verdictChangedCount || 0}</strong>
                      </div>
                      <div>
                        <span>未变化</span>
                        <strong>{overview.coachFollowupImpactSummary.noClearChangeCount || 0}</strong>
                      </div>
                      <div>
                        <span>等后续</span>
                        <strong>{overview.coachFollowupImpactSummary.awaitingFollowupCount || 0}</strong>
                      </div>
                    </div>
                    {(overview.coachFollowupImpactSummary.recommendedAction ||
                      overview.coachFollowupImpactSummary.evidenceRefs?.length) && (
                      <div className="teacher-coach-quality__action">
                        {overview.coachFollowupImpactSummary.recommendedAction && (
                          <small>{overview.coachFollowupImpactSummary.recommendedAction}</small>
                        )}
                        {overview.coachFollowupImpactSummary.evidenceRefs?.length ? (
                          <span>{overview.coachFollowupImpactSummary.evidenceRefs.length} 条证据</span>
                        ) : null}
                      </div>
                    )}
                  </div>
                )}
                {overview.aiDependencySummary && (
                  <div className="teacher-transfer-summary teacher-transfer-summary--dependency">
                    <span>AI 支架自主性</span>
                    <strong>{overview.aiDependencySummary}</strong>
                  </div>
                )}
                {overview.masteryGrowthSummary && (
                  <div className="teacher-transfer-summary teacher-transfer-summary--mastery">
                    <span>长期成长</span>
                    <strong>{overview.masteryGrowthSummary}</strong>
                  </div>
                )}
                {overview.teachingActionSummary && (
                  <div className="teacher-transfer-summary teacher-transfer-summary--teaching">
                    <span>教学动作编排</span>
                    <strong>{overview.teachingActionSummary}</strong>
                  </div>
                )}
                {overview.classTeachingStrategySignal && (
                  <div className="teacher-class-strategy" aria-label="班级教学策略">
                    <div>
                      <span>课堂策略</span>
                      <strong>
                        {displayText(
                          overview.classTeachingStrategySignal.title,
                          overview.classTeachingStrategySignal.statusLabel || "观察"
                        )}
                      </strong>
                      {overview.classTeachingStrategySignal.summary && <p>{overview.classTeachingStrategySignal.summary}</p>}
                    </div>
                    <div className="teacher-class-strategy__meta">
                      <StatusPill
                        tone={
                          (overview.classTeachingStrategySignal.riskLevel || "").toUpperCase() === "HIGH"
                            ? "danger"
                            : (overview.classTeachingStrategySignal.status || "").toUpperCase() === "WATCH" ||
                                (overview.classTeachingStrategySignal.status || "").toUpperCase() === "NO_SIGNAL"
                              ? "neutral"
                              : "info"
                        }
                      >
                        {overview.classTeachingStrategySignal.statusLabel || "待判断"}
                      </StatusPill>
                      <span>{overview.classTeachingStrategySignal.affectedStudentCount || 0} 人</span>
                      {overview.classTeachingStrategySignal.groups?.length ? (
                        <span>{overview.classTeachingStrategySignal.groups.length} 组</span>
                      ) : null}
                    </div>
                    {(overview.classTeachingStrategySignal.teacherAction || overview.classTeachingStrategySignal.exitTicket) && (
                      <div className="teacher-class-strategy__actions">
                        {overview.classTeachingStrategySignal.teacherAction && (
                          <small>{overview.classTeachingStrategySignal.teacherAction}</small>
                        )}
                        {overview.classTeachingStrategySignal.exitTicket && (
                          <small>{overview.classTeachingStrategySignal.exitTicket}</small>
                        )}
                      </div>
                    )}
                    {overview.classTeachingStrategySignal.groups?.length ? (
                      <div className="teacher-class-strategy__groups">
                        {overview.classTeachingStrategySignal.groups.slice(0, 2).map(group => (
                          <span key={`${group.groupType || "group"}-${group.title || group.focus || "strategy"}`}>
                            {displayText(group.title, "策略分组")}
                            {group.studentNames?.length ? ` · ${group.studentNames.slice(0, 3).join("、")}` : ""}
                          </span>
                        ))}
                      </div>
                    ) : null}
                    {overview.classTeachingStrategySignal.impact && (
                      <div className="teacher-class-strategy__impact">
                        <StatusPill tone={interventionImpactTone(overview.classTeachingStrategySignal.impact.status)}>
                          {overview.classTeachingStrategySignal.impact.statusLabel ||
                            interventionImpactLabel(overview.classTeachingStrategySignal.impact.status)}
                        </StatusPill>
                        {overview.classTeachingStrategySignal.impact.needsEscalation && <StatusPill tone="danger">需升级</StatusPill>}
                        <span>
                          {overview.classTeachingStrategySignal.impact.summary ||
                            overview.classTeachingStrategySignal.impact.recommendedAction ||
                            "等待课堂执行证据"}
                        </span>
                      </div>
                    )}
                    {overview.classTeachingStrategySignal.impact?.recommendedAction && (
                      <p className="teacher-class-strategy__next">
                        {overview.classTeachingStrategySignal.impact.recommendedAction}
                      </p>
                    )}
                    {overview.classTeachingStrategySignal.strategyKey && (
                      <div className="teacher-class-strategy__feedback">
                        <Button
                          type="button"
                          variant="ghost"
                          onClick={() => recordClassStrategyFeedback(overview.classTeachingStrategySignal!, "ACCEPTED")}
                        >
                          采纳
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          onClick={() => recordClassStrategyFeedback(overview.classTeachingStrategySignal!, "MODIFIED")}
                        >
                          调整
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          onClick={() => recordClassStrategyFeedback(overview.classTeachingStrategySignal!, "DISMISSED")}
                        >
                          忽略
                        </Button>
                      </div>
                    )}
                  </div>
                )}
                    {renderAiQualitySection()}
                  </div>
                </details>

                <section className="teacher-main-grid">
                  <div className="teacher-block">
                    <div className="teacher-block__head">
                      <h3>高频问题</h3>
                      <StatusPill tone={overview.topIssues.length ? "warning" : "success"}>{overview.topIssues.length || "无"}</StatusPill>
                    </div>
                    {overview.topIssues.length ? (
                      overview.topIssues.map(issue => (
                        <div className="issue-row" key={issue.label}>
                          <span>
                            {issueLabel(issue.label)}
                            {issue.explanation ? <small>{issue.explanation}</small> : null}
                            {issue.abilityPoint ? <small>能力点：{abilityLabel(issue.abilityPoint)}</small> : null}
                          </span>
                          <strong>{issue.actionPriorityScore ? issue.actionPriorityScore.toFixed(1) : issue.count}</strong>
                        </div>
                      ))
                    ) : null}
                    {(overview.classAbilityWeaknesses?.length || reviewSuggestions.length) ? (
                      <details className="teacher-compact-details">
                        <summary>
                          <span>讲评参考</span>
                          <StatusPill tone="neutral">
                            {(overview.classAbilityWeaknesses?.length || 0) + reviewSuggestions.length} 项
                          </StatusPill>
                        </summary>
                        <div className="teacher-compact-details__body">
                          {overview.classAbilityWeaknesses?.length ? (
                            <div className="teacher-class-ability">
                              {overview.classAbilityWeaknesses.slice(0, 4).map(item => (
                                <div key={item.abilityPoint}>
                                  <strong>{abilityLabel(item.abilityPoint)}</strong>
                                  <small>{item.taskCount} 题 · {item.submissionCount} 次提交</small>
                                </div>
                              ))}
                            </div>
                          ) : null}
                          {reviewSuggestions.length ? (
                            <div className="teacher-class-review" aria-label="课堂复盘建议">
                              {reviewSuggestions.map(suggestion => (
                                <article key={suggestion.suggestionKey || suggestion.title}>
                                  <div>
                                    <strong>{suggestion.title}</strong>
                                    <small>
                                      {abilityLabel(suggestion.targetAbility) || "课堂复盘"}
                                      {suggestion.exampleProblemTitle ? ` · ${suggestion.exampleProblemTitle}` : ""}
                                    </small>
                                    {suggestion.guidingQuestion && <p>{suggestion.guidingQuestion}</p>}
                                    {suggestion.latestFeedback?.actionType && (
                                      <StatusPill tone={classReviewFeedbackTone(suggestion.latestFeedback.actionType)}>
                                        {classReviewFeedbackLabel(suggestion.latestFeedback.actionType)}
                                      </StatusPill>
                                    )}
                                    {suggestion.interventionImpact && (
                                      <div className="teacher-intervention-impact">
                                        <div className="status-box-row">
                                          <StatusPill tone={interventionImpactTone(suggestion.interventionImpact.status)}>
                                            {suggestion.interventionImpact.statusLabel ||
                                              interventionImpactLabel(suggestion.interventionImpact.status)}
                                          </StatusPill>
                                          {suggestion.interventionImpact.needsEscalation && (
                                            <StatusPill tone="danger">需升级</StatusPill>
                                          )}
                                          {suggestion.interventionImpact.followupSubmissionId && (
                                            <span>后续 #{suggestion.interventionImpact.followupSubmissionId}</span>
                                          )}
                                        </div>
                                        {suggestion.interventionImpact.summary && <p>{suggestion.interventionImpact.summary}</p>}
                                        {suggestion.interventionImpact.recommendedAction && (
                                          <small>{suggestion.interventionImpact.recommendedAction}</small>
                                        )}
                                        {suggestion.interventionImpact.matchedTags?.length ? (
                                          <small>
                                            证据 {suggestion.interventionImpact.matchedTags.map(issueLabel).join(" / ")}
                                          </small>
                                        ) : null}
                                      </div>
                                    )}
                                  </div>
                                  <div className="actions">
                                    <Button type="button" variant="ghost" onClick={() => void recordClassReviewFeedback(suggestion, "ACCEPTED")}>
                                      采纳
                                    </Button>
                                    <Button type="button" variant="ghost" onClick={() => void recordClassReviewFeedback(suggestion, "MODIFIED")}>
                                      调整
                                    </Button>
                                    <Button type="button" variant="ghost" onClick={() => void recordClassReviewFeedback(suggestion, "DISMISSED")}>
                                      忽略
                                    </Button>
                                  </div>
                                </article>
                              ))}
                            </div>
                          ) : null}
                        </div>
                      </details>
                    ) : null}
                  </div>

                  <div className="teacher-block">
                    <div className="teacher-block__head">
                      <h3>{attentionStudents.length ? "需要关注" : "学生过程"}</h3>
                      <StatusPill tone={attentionStudents.length ? "warning" : "success"}>
                        {attentionStudents.length ? `${attentionStudents.length} 人` : "正常"}
                      </StatusPill>
                    </div>
                    {visibleStudents.length ? (
                      visibleStudents.map(student => (
                        <div className="teacher-student-row" key={student.studentProfileId}>
                          <div>
                            <h3>{displayText(student.displayName, `学生 #${student.studentProfileId}`)}</h3>
                            <div className="teacher-student-row__meta">
                              <span className="teacher-row-meta-text">{student.attemptCount} 次尝试</span>
                              <StatusPill tone={student.latestIssue ? "warning" : "success"}>
                                {student.latestFineGrainedIssue
                                  ? issueLabel(student.latestFineGrainedIssue)
                                  : student.latestIssue
                                    ? issueLabel(student.latestIssue)
                                    : verdictLabel(student.latestVerdict)}
                              </StatusPill>
                            </div>
                            <div className="status-box-row teacher-status-row">
                              {student.primaryAbilityFocus && <span className="teacher-row-meta-text">能力 {abilityLabel(student.primaryAbilityFocus)}</span>}
                              {student.latestCoachInteraction?.prompted && (
                                <StatusPill tone={student.latestCoachInteraction.answered ? "success" : "warning"}>
                                  {learningStageLabel(
                                    student.latestCoachImpact?.statusLabel ||
                                    student.latestCoachInteraction.impact?.statusLabel ||
                                    student.latestCoachInteraction.statusLabel
                                  ) || "追问"}
                                </StatusPill>
                              )}
                              {student.latestCorrection && (
                                <StatusPill tone="success">
                                  已校正为{" "}
                                  {student.latestCorrection.correctedFineGrainedTag
                                    ? issueLabel(student.latestCorrection.correctedFineGrainedTag)
                                    : issueLabel(student.latestCorrection.correctedIssueTag)}
                                </StatusPill>
                              )}
                              {student.postAcTransferSignal && student.postAcTransferSignal.phase !== "NOT_ACCEPTED" && (
                                <StatusPill
                                  tone={
                                    ["JUST_ACCEPTED", "REFLECTION_NEEDED"].includes(
                                      (student.postAcTransferSignal.phase || "").toUpperCase()
                                    )
                                      ? "warning"
                                      : "success"
                                  }
                                >
                                  {postAcTransferPhaseLabel(student.postAcTransferSignal.phase)}
                                </StatusPill>
                              )}
                              {student.recurringMisconceptionSignal &&
                                student.recurringMisconceptionSignal.status !== "NONE" && (
                                  <StatusPill
                                    tone={
                                      (student.recurringMisconceptionSignal.status || "").toUpperCase() === "ESCALATE"
                                        ? "danger"
                                        : (student.recurringMisconceptionSignal.status || "").toUpperCase() === "RECURRING"
                                          ? "warning"
                                          : "neutral"
                                    }
                                  >
                                    {recurringMisconceptionStatusLabel(student.recurringMisconceptionSignal.status)}
                                  </StatusPill>
                                )}
                              {student.selfExplanationMasterySignal &&
                                student.selfExplanationMasterySignal.status !== "NO_EVIDENCE" && (
                                  <StatusPill
                                    tone={
                                      (student.selfExplanationMasterySignal.status || "").toUpperCase() === "SAFETY_RISK"
                                        ? "danger"
                                        : (student.selfExplanationMasterySignal.status || "").toUpperCase() === "NEEDS_COACHING"
                                          ? "warning"
                                          : ["EVIDENCE_GROUNDED", "TRANSFER_READY"].includes(
                                                (student.selfExplanationMasterySignal.status || "").toUpperCase()
                                              )
                                            ? "success"
                                            : "info"
                                    }
                                  >
                                    {selfExplanationStatusLabel(student.selfExplanationMasterySignal.status)}
                                  </StatusPill>
                                )}
                              {student.aiDependencySignal &&
                                ["SCAFFOLD_DENSE", "DEPENDENCY_RISK", "TEACHER_FADE_REVIEW"].includes(
                                  (student.aiDependencySignal.status || "").toUpperCase()
                                ) && (
                                  <StatusPill
                                    tone={
                                      (student.aiDependencySignal.status || "").toUpperCase() === "TEACHER_FADE_REVIEW"
                                        ? "danger"
                                        : (student.aiDependencySignal.status || "").toUpperCase() === "DEPENDENCY_RISK"
                                          ? "warning"
                                          : "info"
                                    }
                                  >
                                    {aiDependencyStatusLabel(student.aiDependencySignal.status)}
                                  </StatusPill>
                                )}
                              {student.masteryGrowthSignal &&
                                ["PLATEAU", "REGRESSION", "SPIRAL_REVIEW_NEEDED"].includes(
                                  (student.masteryGrowthSignal.status || "").toUpperCase()
                                ) && (
                                  <StatusPill
                                    tone={
                                      (student.masteryGrowthSignal.status || "").toUpperCase() === "SPIRAL_REVIEW_NEEDED"
                                        ? "danger"
                                        : (student.masteryGrowthSignal.status || "").toUpperCase() === "REGRESSION"
                                          ? "warning"
                                          : "info"
                                    }
                                  >
                                    {masteryGrowthStatusLabel(student.masteryGrowthSignal.status)}
                                  </StatusPill>
                                )}
                              {student.teachingActionDecision &&
                                (student.teachingActionDecision.actionType || "").toUpperCase() !== "CONTINUE_DIAGNOSIS" && (
                                  <StatusPill
                                    tone={
                                      (student.teachingActionDecision.riskLevel || "").toUpperCase() === "HIGH" ||
                                      student.teachingActionDecision.needsTeacherAttention
                                        ? "danger"
                                        : (student.teachingActionDecision.riskLevel || "").toUpperCase() === "MEDIUM"
                                          ? "warning"
                                          : "info"
                                    }
                                  >
                                    {teachingActionTypeLabel(student.teachingActionDecision.actionType)}
                                  </StatusPill>
                                )}
                            </div>
                            {student.attentionReason && <p>{student.attentionReason}</p>}
                            {student.teachingActionDecision &&
                              (student.teachingActionDecision.actionType || "").toUpperCase() !== "CONTINUE_DIAGNOSIS" && (
                                <p className="teacher-note teacher-note--teaching">
                                  {teachingActionActorLabel(student.teachingActionDecision.actor)}：
                                  {student.teachingActionDecision.recommendedAction ||
                                    student.teachingActionDecision.summary ||
                                    student.teachingActionDecision.title}
                                </p>
                              )}
                            {student.recurringMisconceptionSignal &&
                              student.recurringMisconceptionSignal.status !== "NONE" && (
                                <p className="teacher-note">
                                  {student.recurringMisconceptionSignal.summary ||
                                    student.recurringMisconceptionSignal.recommendedAction}
                                </p>
                              )}
                            {student.selfExplanationMasterySignal &&
                              student.selfExplanationMasterySignal.status !== "NO_EVIDENCE" && (
                                <p className="teacher-note">
                                  {student.selfExplanationMasterySignal.summary ||
                                    student.selfExplanationMasterySignal.recommendedAction}
                                </p>
                              )}
                            {student.aiDependencySignal &&
                              ["SCAFFOLD_DENSE", "DEPENDENCY_RISK", "TEACHER_FADE_REVIEW"].includes(
                                (student.aiDependencySignal.status || "").toUpperCase()
                              ) && (
                                <p className="teacher-note">
                                  {student.aiDependencySignal.summary || student.aiDependencySignal.recommendedAction}
                                </p>
                              )}
                            {student.masteryGrowthSignal &&
                              ["PLATEAU", "REGRESSION", "SPIRAL_REVIEW_NEEDED"].includes(
                                (student.masteryGrowthSignal.status || "").toUpperCase()
                              ) && (
                                <p className="teacher-note">
                                  {student.masteryGrowthSignal.summary || student.masteryGrowthSignal.recommendedAction}
                                </p>
                              )}
                            {(student.latestCoachImpact?.summary || student.latestCoachInteraction?.impact?.summary) && (
                              <p className="teacher-note">
                                {student.latestCoachImpact?.summary || student.latestCoachInteraction?.impact?.summary}
                              </p>
                            )}
                            {student.attentionEvidence?.length ? (
                              <div className="teacher-attention-evidence" aria-label="需关注证据链">
                                {student.attentionEvidence.slice(0, 1).map(evidence => (
                                  <div key={evidence.submissionId}>
                                    <span>{formatDateTime(evidence.submittedAt)}</span>
                                    <strong>
                                      {evidence.fineGrainedTag
                                        ? issueLabel(evidence.fineGrainedTag)
                                        : evidence.issueTag
                                          ? issueLabel(evidence.issueTag)
                                          : verdictLabel(evidence.verdict)}
                                    </strong>
                                    <small>
                                      {evidence.reason || evidence.headline || "最近提交证据"}
                                      {evidence.abilityPoint ? ` · ${abilityLabel(evidence.abilityPoint)}` : ""}
                                    </small>
                                  </div>
                                ))}
                              </div>
                            ) : null}
                            {student.latestCorrection?.teacherNote && <p className="teacher-note">{student.latestCorrection.teacherNote}</p>}
                          </div>
                          <div className="actions">
                            <Button
                              type="button"
                              variant="ghost"
                              onClick={() => openCorrection(student)}
                              disabled={!student.latestSubmissionId}
                              icon={<PenLine size={15} />}
                            >
                              校正
                            </Button>
                            <VerdictPill verdict={student.latestVerdict} />
                          </div>
                        </div>
                      ))
                    ) : null}
                  </div>
                </section>
              </>
            )}
          </main>

        </div>
      </section>

      {correctionDraft && (
        <section className="teacher-correction-panel" aria-label="教师校正错因">
          <div>
            <p className="eyebrow">教师校正</p>
            <h2>修正错因</h2>
          </div>
          <div className="form-grid">
            <Field label="主要错因">
              <Select
                value={correctionDraft.correctedIssueTag}
                onChange={event => setCorrectionDraft({ ...correctionDraft, correctedIssueTag: event.target.value })}
              >
                <option value="">请选择</option>
                {coarseDiagnosisTags.map(tag => (
                  <option value={tag.id} key={tag.id}>
                    {tag.label}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="细分错因">
              <Select
                value={correctionDraft.correctedFineGrainedTag}
                onChange={event => setCorrectionDraft({ ...correctionDraft, correctedFineGrainedTag: event.target.value })}
              >
                <option value="">不指定</option>
                {fineDiagnosisTags.map(tag => (
                  <option value={tag.id} key={tag.id}>
                    {tag.label}
                  </option>
                ))}
              </Select>
            </Field>
          </div>
          <Field label="校正理由">
            <TextArea
              value={correctionDraft.teacherNote}
              onChange={event => setCorrectionDraft({ ...correctionDraft, teacherNote: event.target.value })}
              placeholder="例如：学生不是边界问题，而是输入读取方式理解错。"
            />
          </Field>
          <div className="actions">
            <Button type="button" variant="primary" onClick={() => void saveCorrection()} disabled={busy}>
              保存校正
            </Button>
            <Button type="button" variant="ghost" onClick={() => setCorrectionDraft(null)}>
              取消
            </Button>
          </div>
        </section>
      )}

      <details className="teacher-config-drawer">
        <summary>
          <span>作业配置</span>
          <StatusPill tone="info">{form.id ? "编辑作业" : "新作业"}</StatusPill>
        </summary>
        <div className="teacher-config-body">
          <div className="form-grid">
            <Field label="作业标题">
              <TextInput value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} placeholder="例如 Python 分支与边界练习" />
            </Field>
            <Field label="绑定班级">
              <Select value={form.classGroupId} onChange={event => setForm({ ...form, classGroupId: event.target.value })}>
                <option value="">不绑定</option>
                {cleanClasses.map(item => (
                  <option value={item.id} key={item.id}>
                    {item.name}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="提示层级">
              <Select value={form.hintPolicy} onChange={event => setForm({ ...form, hintPolicy: event.target.value })}>
                <option value="L1">L1 问题类型</option>
                <option value="L2">L2 定位方向</option>
                <option value="L3">L3 局部提示</option>
                <option value="L4">L4 参考改法</option>
              </Select>
            </Field>
          </div>

          <Field label="作业说明">
            <TextArea value={form.description} onChange={event => setForm({ ...form, description: event.target.value })} />
          </Field>

          <Field label="绑定学习任务">
            <div className="teacher-problem-pick-list">
              {problems.map(problem => (
                <label className="list-row" key={problem.id}>
                  <div className="actions">
                    <input type="checkbox" checked={form.problemIds.includes(problem.id)} onChange={() => toggleProblem(problem.id)} />
                    <DifficultyPill difficulty={problem.difficulty} />
                  </div>
                  <h3>{problem.title}</h3>
                </label>
              ))}
            </div>
          </Field>

          <div className="actions">
            <Button type="button" variant="primary" onClick={() => void saveAssignment()}>
              保存作业
            </Button>
            <ButtonLink to="/app/task-editor" variant="ghost" icon={<PenLine size={17} />}>
              编辑题目
            </ButtonLink>
            <Button type="button" variant="ghost" onClick={() => void rotateInvite()} icon={<RotateCw size={17} />}>
              重置邀请码
            </Button>
          </div>
        </div>
      </details>
    </div>
  );
}

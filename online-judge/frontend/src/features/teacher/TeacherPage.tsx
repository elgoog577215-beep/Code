import { useEffect, useMemo, useState } from "react";
import { ChartNoAxesColumnIncreasing, Copy, PenLine, RefreshCw, RotateCw, Settings } from "lucide-react";
import { ApiError, api } from "../../shared/api/client";
import type {
  Assignment,
  AssignmentOverview,
  ClassGroup,
  DiagnosisTag,
  ProblemCatalogItem
} from "../../shared/api/types";
import {
  assignmentStatusLabel,
  displayText,
  formatDateTime,
  hintPolicyLabel,
  issueLabel,
  looksCorruptText,
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
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [classes, setClasses] = useState<ClassGroup[]>([]);
  const [problems, setProblems] = useState<ProblemCatalogItem[]>([]);
  const [diagnosisTags, setDiagnosisTags] = useState<DiagnosisTag[]>([]);
  const [overview, setOverview] = useState<AssignmentOverview | null>(null);
  const [activeAssignmentId, setActiveAssignmentId] = useState<number | null>(null);
  const [form, setForm] = useState(EMPTY_ASSIGNMENT);
  const [correctionDraft, setCorrectionDraft] = useState<CorrectionDraft | null>(null);
  const [alert, setAlert] = useState<Alert | null>(null);
  const [busy, setBusy] = useState(false);
  const [overviewLoading, setOverviewLoading] = useState(false);

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
  const hasSelectedAssignment = Boolean(selectedAssignment);
  const nextAction = overviewLoading
    ? "读取提交记录"
    : overview?.strugglingStudentCount
    ? "先看需关注学生"
    : overview && passRate < 50
      ? "检查高频问题"
      : overview
        ? "观察提交"
        : hasSelectedAssignment
          ? "等待学生提交"
          : "选择作业";
  const nextActionState = overviewLoading
    ? "读取中"
    : overview?.strugglingStudentCount
    ? `${overview.strugglingStudentCount} 人需关注`
    : overview && passRate < 50
      ? "通过率偏低"
      : overview
        ? "班级正常"
        : hasSelectedAssignment
          ? "暂无过程数据"
          : "未选择作业";
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

  async function loadOverview(id: number) {
    setActiveAssignmentId(id);
    setOverviewLoading(true);
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

  return (
    <div className="stack teacher-page teacher-page--studio">
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="teacher-studio-shell">
        <header className="teacher-studio-topbar">
          <div>
            <h1>课堂过程</h1>
          </div>
          <div className="actions">
            <ButtonLink to="/app/teacher-management" variant="secondary" icon={<Settings size={17} />}>
              管理
            </ButtonLink>
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
              <StatusPill tone={stageStateTone}>{stageStateLabel}</StatusPill>
            </div>

            {overviewLoading ? (
              <TeacherOverviewSkeleton />
            ) : !overview ? (
              <EmptyState title={selectedAssignment ? "暂无过程数据" : "请选择作业"} />
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
                    <span>通过次数</span>
                    <strong>{overview.passedAttemptCount}</strong>
                  </div>
                  <div>
                    <span>通过率</span>
                    <strong>{passRate}%</strong>
                  </div>
                </div>

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
                            {issue.abilityPoint ? <small>能力点：{issue.abilityPoint}</small> : null}
                          </span>
                          <strong>{issue.actionPriorityScore ? issue.actionPriorityScore.toFixed(1) : issue.count}</strong>
                        </div>
                      ))
                    ) : (
                      <EmptyState title="暂无问题分类" />
                    )}
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
                                  <strong>{item.abilityPoint}</strong>
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
                                      {suggestion.targetAbility || "课堂复盘"}
                                      {suggestion.exampleProblemTitle ? ` · ${suggestion.exampleProblemTitle}` : ""}
                                    </small>
                                    {suggestion.guidingQuestion && <p>{suggestion.guidingQuestion}</p>}
                                    {suggestion.latestFeedback?.actionType && (
                                      <StatusPill tone={classReviewFeedbackTone(suggestion.latestFeedback.actionType)}>
                                        {classReviewFeedbackLabel(suggestion.latestFeedback.actionType)}
                                      </StatusPill>
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
                              {student.primaryAbilityFocus && <span className="teacher-row-meta-text">能力 {student.primaryAbilityFocus}</span>}
                              {student.latestCoachInteraction?.prompted && (
                                <StatusPill tone={student.latestCoachInteraction.answered ? "success" : "warning"}>
                                  {student.latestCoachImpact?.statusLabel || student.latestCoachInteraction.impact?.statusLabel || student.latestCoachInteraction.statusLabel || "追问"}
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
                            </div>
                            {student.attentionReason && <p>{student.attentionReason}</p>}
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
                                      {evidence.abilityPoint ? ` · ${evidence.abilityPoint}` : ""}
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
                    ) : (
                      <EmptyState title="暂无学生提交" />
                    )}
                  </div>
                </section>
              </>
            )}
          </main>

          <aside className="teacher-action-panel">
            <section>
              <StatusPill tone="neutral">处理项</StatusPill>
              <h2>{nextAction}</h2>
              <div className="status-box-row teacher-action-state">
                <StatusPill tone={overview?.strugglingStudentCount || (overview && passRate < 50) ? "warning" : overview ? "success" : "neutral"}>
                  {nextActionState}
                </StatusPill>
              </div>
            </section>

            <section className="teacher-quick-actions">
              <Button type="button" variant="secondary" onClick={() => void rotateInvite()} disabled={!selectedAssignment} icon={<RotateCw size={17} />}>
                重置邀请码
              </Button>
              {selectedAssignment?.inviteCode && (
                <Button
                  type="button"
                  variant="secondary"
                  icon={<Copy size={17} />}
                  onClick={() => void copyInviteCode()}
                >
                  复制邀请码
                </Button>
              )}
              <ButtonLink to="/app/task-editor" variant="secondary" icon={<PenLine size={17} />}>
                编辑题目
              </ButtonLink>
              <ButtonLink to="/app/class-overview" variant="secondary" icon={<ChartNoAxesColumnIncreasing size={17} />}>
                班级概览
              </ButtonLink>
            </section>
          </aside>
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
            <Button type="button" variant="ghost" onClick={() => void rotateInvite()} icon={<RotateCw size={17} />}>
              重置邀请码
            </Button>
          </div>
        </div>
      </details>
    </div>
  );
}

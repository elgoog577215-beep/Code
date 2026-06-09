import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, ChevronDown, PenLine } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { ApiError, api } from "../../shared/api/client";
import type { Assignment, AssignmentOverview, DiagnosisTag } from "../../shared/api/types";
import {
  assignmentStatusLabel,
  displayText,
  formatDateTime,
  hintPolicyLabel,
  issueLabel,
  learningStageLabel,
  percent,
  verdictLabel
} from "../../shared/format";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea } from "../../shared/ui/Field";
import { DifficultyPill, StatusPill, VerdictPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };
type OverviewStudent = AssignmentOverview["students"][number];
type CorrectionDraft = {
  studentProfileId: number;
  submissionId: number;
  correctedIssueTag: string;
  correctedFineGrainedTag: string;
  teacherNote: string;
};

function cleanAssignmentTitle(value?: string | null, fallback = "课堂作业") {
  const title = displayText(value, fallback);
  return title.includes("试点任务") ? "课堂编程作业" : title;
}

function teacherErrorMessage(error: unknown, fallback: string) {
  const base = fallback.replace(/[。.!！?？\s]+$/, "");
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return `${base}，未找到作业。`;
    }
    if (error.status >= 500) {
      return `${base}，服务暂时不可用。`;
    }
    return `${base}，${error.message}`;
  }
  return error instanceof Error ? `${base}，${error.message}` : fallback;
}

function studentPassRate(student: OverviewStudent) {
  return student.attemptCount ? Math.round((student.passedCount / student.attemptCount) * 100) : 0;
}

function problemStats(assignment: Assignment, overview: AssignmentOverview) {
  return assignment.tasks.map(task => {
    const relatedStudents = overview.students.filter(student =>
      student.attentionEvidence?.some(evidence => evidence.problemId === task.problemId) ||
      (!student.attentionEvidence?.length && student.latestSubmissionId)
    );
    const issueHits = relatedStudents.filter(student => student.needsAttention).length;
    const latestAttempts = relatedStudents.reduce((sum, student) => sum + student.attemptCount, 0);
    const latestPassed = relatedStudents.reduce((sum, student) => sum + student.passedCount, 0);
    const rate = latestAttempts ? Math.round((latestPassed / latestAttempts) * 100) : 0;
    return { task, issueHits, latestAttempts, latestPassed, rate };
  });
}

export default function AssignmentDetailPage() {
  const { assignmentId } = useParams<{ assignmentId: string }>();
  const id = Number(assignmentId);
  const [assignment, setAssignment] = useState<Assignment | null>(null);
  const [overview, setOverview] = useState<AssignmentOverview | null>(null);
  const [diagnosisTags, setDiagnosisTags] = useState<DiagnosisTag[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [alert, setAlert] = useState<Alert | null>(null);
  const [selectedStudentId, setSelectedStudentId] = useState<number | null>(null);
  const [selectedStudent, setSelectedStudent] = useState<OverviewStudent | null>(null);
  const [correctionDraft, setCorrectionDraft] = useState<CorrectionDraft | null>(null);

  const coarseDiagnosisTags = useMemo(() => diagnosisTags.filter(tag => !tag.fineGrained), [diagnosisTags]);
  const fineDiagnosisTags = useMemo(() => diagnosisTags.filter(tag => tag.fineGrained), [diagnosisTags]);
  const attentionStudents = useMemo(() => overview?.students.filter(student => student.needsAttention) || [], [overview]);
  const visibleStudents = useMemo(() => {
    const students = overview?.students || [];
    return [...students].sort((left, right) => {
      const attentionDiff = Number(Boolean(right.needsAttention)) - Number(Boolean(left.needsAttention));
      return attentionDiff || right.attemptCount - left.attemptCount;
    });
  }, [overview]);
  const passRate = overview?.attemptCount ? Math.round((overview.passedAttemptCount / overview.attemptCount) * 100) : 0;
  const classroomStateLabel = attentionStudents.length ? `${attentionStudents.length} 人需关注` : "推进稳定";
  const selectedStudentReport = visibleStudents.find(student => student.studentProfileId === selectedStudentId) || visibleStudents[0] || null;
  const taskStats = assignment && overview ? problemStats(assignment, overview) : [];
  const reviewItems = overview?.classReviewSuggestions?.slice(0, 3) || [];

  useEffect(() => {
    if (!Number.isFinite(id)) {
      setLoading(false);
      return;
    }
    void loadAssignmentDetail(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  useEffect(() => {
    if (!selectedStudentId && visibleStudents[0]) {
      setSelectedStudentId(visibleStudents[0].studentProfileId);
    }
  }, [selectedStudentId, visibleStudents]);

  async function loadAssignmentDetail(targetId: number) {
    setLoading(true);
    setAlert(null);
    try {
      const [assignments, overviewResult, tags] = await Promise.all([
        api.assignments(),
        api.assignmentOverview(targetId),
        api.diagnosisTags()
      ]);
      setAssignment(assignments.find(item => item.id === targetId) || overviewResult.assignment || null);
      setOverview(overviewResult);
      setDiagnosisTags(tags);
    } catch (error) {
      setAssignment(null);
      setOverview(null);
      setAlert({ type: "error", message: teacherErrorMessage(error, "作业详情加载失败。") });
    } finally {
      setLoading(false);
    }
  }

  function openCorrection(student: OverviewStudent) {
    if (!student.latestSubmissionId) {
      setAlert({ type: "error", message: "这名学生还没有可校正的提交。" });
      return;
    }
    setSelectedStudent(student);
    setCorrectionDraft({
      studentProfileId: student.studentProfileId,
      submissionId: student.latestSubmissionId,
      correctedIssueTag: student.latestCorrection?.correctedIssueTag || student.latestIssueTag || "",
      correctedFineGrainedTag: student.latestCorrection?.correctedFineGrainedTag || "",
      teacherNote: student.latestCorrection?.teacherNote || ""
    });
  }

  async function saveCorrection() {
    if (!Number.isFinite(id) || !correctionDraft) {
      return;
    }
    if (!correctionDraft.correctedIssueTag) {
      setAlert({ type: "error", message: "请选择主要错因。" });
      return;
    }
    setSaving(true);
    try {
      await api.correctDiagnosis(id, {
        submissionId: correctionDraft.submissionId,
        correctedIssueTag: correctionDraft.correctedIssueTag,
        correctedFineGrainedTag: correctionDraft.correctedFineGrainedTag || null,
        teacherNote: correctionDraft.teacherNote,
        evalCandidate: true,
        correctedBy: "teacher"
      });
      setAlert({ type: "success", message: "教师校正已保存。" });
      setCorrectionDraft(null);
      setSelectedStudent(null);
      await loadAssignmentDetail(id);
    } catch (error) {
      setAlert({ type: "error", message: teacherErrorMessage(error, "教师校正保存失败。") });
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <EmptyState title="正在加载作业详情" />;
  }

  if (!assignment || !overview) {
    return (
      <div className="stack assignment-detail-page">
        {alert && <div className="alert alert--error">{alert.message}</div>}
        <EmptyState title="作业未找到" description="返回作业中心重新选择作业。" />
        <ButtonLink to="/app/teacher" variant="secondary" icon={<ArrowLeft size={16} />}>
          返回作业中心
        </ButtonLink>
      </div>
    );
  }

  return (
    <div className="stack assignment-detail-page assignment-workspace">
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="assignment-detail-command assignment-detail-command--school">
        <div>
          <Link to="/app/teacher" className="back-link">
            <ArrowLeft size={16} /> 返回作业中心
          </Link>
          <p className="eyebrow">作业详情</p>
          <h1>{cleanAssignmentTitle(assignment.title)}</h1>
          <div className="teacher-stage__meta">
            <span>{displayText(assignment.className, "未绑定班级")}</span>
            <span>{assignment.tasks?.length || 0} 题</span>
            <span>{hintPolicyLabel(assignment.hintPolicy)}</span>
            <span>{assignmentStatusLabel(assignment.status)}</span>
          </div>
        </div>
        <div className="assignment-detail-state">
          <StatusPill tone={attentionStudents.length ? "warning" : "success"}>{classroomStateLabel}</StatusPill>
          <strong>{passRate}%</strong>
          <span>提交通过率</span>
        </div>
      </section>

      <section className="teacher-kpi-strip assignment-kpi-strip assignment-kpi-strip--simple" aria-label="作业总览">
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
      </section>

      <section className="assignment-section assignment-overall-panel" aria-label="整体统计">
        <div className="assignment-section__head">
          <div>
            <p className="eyebrow">整体统计</p>
            <h2>老师下一步先看什么</h2>
          </div>
          <StatusPill tone={overview.topIssues.length ? "warning" : "success"}>
            {overview.topIssues.length ? `${overview.topIssues.length} 类问题` : "暂无集中问题"}
          </StatusPill>
        </div>
        <div className="assignment-overall-grid">
          <article className="assignment-next-card">
            <span>课堂状态</span>
            <strong>{classroomStateLabel}</strong>
            <p>{attentionStudents[0]?.attentionReason || overview.classTeachingStrategySignal?.summary || "当前作业推进稳定，可以按题目表现安排讲评。"}</p>
          </article>
          <article className="assignment-next-card">
            <span>高频错因</span>
            <strong>{overview.topIssues[0] ? issueLabel(overview.topIssues[0].label) : "暂无"}</strong>
            <p>{overview.topIssues[0]?.interventionSuggestion || overview.topIssues[0]?.explanation || "暂时没有明显集中错因。"}</p>
          </article>
          <article className="assignment-next-card">
            <span>讲评建议</span>
            <strong>{reviewItems[0]?.title || "按学生情况复盘"}</strong>
            <p>{reviewItems[0]?.guidingQuestion || reviewItems[0]?.action || "优先查看需关注学生的最近提交证据。"}</p>
          </article>
        </div>
        <div className="assignment-issue-list">
          {overview.topIssues.slice(0, 5).map(issue => (
            <div className="issue-row" key={issue.label}>
              <span>
                {issueLabel(issue.label)}
                {issue.explanation ? <small>{issue.explanation}</small> : null}
              </span>
              <strong>{issue.count}</strong>
            </div>
          ))}
        </div>
      </section>

      <section className="assignment-section assignment-student-workspace" aria-label="学生情况">
        <div className="assignment-section__head">
          <div>
            <p className="eyebrow">学生情况</p>
            <h2>每个学生的细致情况</h2>
          </div>
          <StatusPill tone={attentionStudents.length ? "warning" : "success"}>
            {attentionStudents.length ? `${attentionStudents.length} 人需关注` : "整体稳定"}
          </StatusPill>
        </div>
        <div className="assignment-student-grid">
          <div className="assignment-student-list">
            {visibleStudents.map(student => (
              <button
                type="button"
                className={`assignment-student-card ${student.studentProfileId === selectedStudentReport?.studentProfileId ? "is-active" : ""}`}
                key={student.studentProfileId}
                onClick={() => setSelectedStudentId(student.studentProfileId)}
              >
                <span>
                  <strong>{displayText(student.displayName, `学生 #${student.studentProfileId}`)}</strong>
                  <small>
                    {student.attemptCount} 次尝试 · {studentPassRate(student)}% 通过
                  </small>
                </span>
                <span className="assignment-student-card__state">
                  {student.needsAttention ? <StatusPill tone="warning">需关注</StatusPill> : <StatusPill tone="success">稳定</StatusPill>}
                  <ChevronDown size={15} aria-hidden="true" />
                </span>
              </button>
            ))}
          </div>

          {selectedStudentReport ? (
            <article className="assignment-student-report">
              <div className="assignment-student-report__head">
                <div>
                  <h3>{displayText(selectedStudentReport.displayName, "学生")}</h3>
                  <p>
                    {selectedStudentReport.studentNo ? `${selectedStudentReport.studentNo} 号 · ` : ""}
                    {selectedStudentReport.attemptCount} 次尝试 · {selectedStudentReport.passedCount} 次通过
                  </p>
                </div>
                <VerdictPill verdict={selectedStudentReport.latestVerdict} />
              </div>
              <div className="assignment-student-report__metrics">
                <div>
                  <span>通过率</span>
                  <strong>{studentPassRate(selectedStudentReport)}%</strong>
                </div>
                <div>
                  <span>重复问题</span>
                  <strong>{selectedStudentReport.repeatedIssueCount || 0}</strong>
                </div>
                <div>
                  <span>能力焦点</span>
                  <strong>{displayText(selectedStudentReport.primaryAbilityFocus, "待观察")}</strong>
                </div>
              </div>
              <div className="assignment-student-report__body">
                <p>
                  <strong>最近反馈：</strong>
                  {selectedStudentReport.latestProgressSignal ||
                    selectedStudentReport.attentionReason ||
                    selectedStudentReport.crossProblemSummary ||
                    "暂无明确反馈。"}
                </p>
                {selectedStudentReport.latestIssueTag && (
                  <p>
                    <strong>最近问题：</strong>
                    {issueLabel(selectedStudentReport.latestFineGrainedIssue || selectedStudentReport.latestIssueTag)}
                    {selectedStudentReport.latestUncertainty ? ` · ${selectedStudentReport.latestUncertainty}` : ""}
                  </p>
                )}
                {selectedStudentReport.latestCoachInteraction?.summary && (
                  <p>
                    <strong>追问反馈：</strong>
                    {learningStageLabel(selectedStudentReport.latestCoachInteraction.summary)}
                  </p>
                )}
                {selectedStudentReport.latestCoachImpact?.summary && (
                  <p>
                    <strong>后续成效：</strong>
                    {selectedStudentReport.latestCoachImpact.summary}
                  </p>
                )}
                {selectedStudentReport.attentionEvidence?.length ? (
                  <div className="teacher-attention-evidence">
                    {selectedStudentReport.attentionEvidence.slice(0, 2).map(evidence => (
                      <div key={evidence.submissionId}>
                        <span>{formatDateTime(evidence.submittedAt)}</span>
                        <strong>
                          {evidence.fineGrainedTag
                            ? issueLabel(evidence.fineGrainedTag)
                            : evidence.issueTag
                              ? issueLabel(evidence.issueTag)
                              : verdictLabel(evidence.verdict)}
                        </strong>
                        <small>{evidence.reason || evidence.headline || "最近提交证据"}</small>
                      </div>
                    ))}
                  </div>
                ) : null}
              </div>
              <div className="actions">
                <Button
                  type="button"
                  variant="secondary"
                  icon={<PenLine size={15} />}
                  onClick={() => openCorrection(selectedStudentReport)}
                  disabled={!selectedStudentReport.latestSubmissionId}
                >
                  校正错因
                </Button>
              </div>
            </article>
          ) : (
            <EmptyState title="暂无学生提交" />
          )}
        </div>
      </section>

      <section className="assignment-section assignment-task-panel" aria-label="作业题目">
        <div className="assignment-section__head">
          <div>
            <p className="eyebrow">作业题目</p>
            <h2>题目表现与讲评优先级</h2>
          </div>
          <StatusPill tone="neutral">{assignment.tasks.length} 题</StatusPill>
        </div>
        <div className="assignment-task-list">
          {taskStats.map(item => (
            <article className="assignment-task-row" key={item.task.problemId}>
              <div>
                <DifficultyPill difficulty={item.task.difficulty} />
                <h3>{item.task.title}</h3>
                <p>
                  {item.latestAttempts ? `${item.latestAttempts} 次相关尝试 · ${item.latestPassed} 次通过` : "等待学生提交"}
                </p>
              </div>
              <div className="assignment-task-row__stats">
                <strong>{item.latestAttempts ? percent(item.rate) : "-"}</strong>
                <StatusPill tone={item.issueHits ? "warning" : item.latestAttempts ? "success" : "neutral"}>
                  {item.issueHits ? `${item.issueHits} 人需讲评` : item.latestAttempts ? "正常推进" : "待提交"}
                </StatusPill>
              </div>
            </article>
          ))}
        </div>
      </section>

      <details className="assignment-advanced-analysis">
        <summary>
          <span>高级分析</span>
          <StatusPill tone="neutral">AI / Coach / 教师校正</StatusPill>
        </summary>
        <div className="assignment-advanced-analysis__body">
          {overview.coachAnswerQualitySummary && (
            <article>
              <span>Coach 回答质量</span>
              <strong>{overview.coachAnswerQualitySummary.summary || "有追问样本"}</strong>
              <p>{overview.coachAnswerQualitySummary.recommendedAction || "用于观察学生是否真正理解反馈。"}</p>
            </article>
          )}
          {overview.coachFollowupImpactSummary && (
            <article>
              <span>Coach 后续成效</span>
              <strong>{overview.coachFollowupImpactSummary.summary || "有后续提交样本"}</strong>
              <p>{overview.coachFollowupImpactSummary.recommendedAction || "用于判断反馈是否改善下一次提交。"}</p>
            </article>
          )}
          {overview.classTeachingStrategySignal && (
            <article>
              <span>课堂策略</span>
              <strong>{overview.classTeachingStrategySignal.title || overview.classTeachingStrategySignal.statusLabel || "策略观察"}</strong>
              <p>{overview.classTeachingStrategySignal.summary || overview.classTeachingStrategySignal.teacherAction || "等待更多课堂证据。"}</p>
            </article>
          )}
        </div>
      </details>

      {correctionDraft && selectedStudent && (
        <section className="teacher-correction-panel assignment-correction-panel" aria-label="教师校正错因">
          <div>
            <p className="eyebrow">教师校正</p>
            <h2>修正 {displayText(selectedStudent.displayName, "学生")} 的错因</h2>
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
            <Button type="button" variant="primary" onClick={() => void saveCorrection()} disabled={saving}>
              保存校正
            </Button>
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                setCorrectionDraft(null);
                setSelectedStudent(null);
              }}
            >
              取消
            </Button>
          </div>
        </section>
      )}
    </div>
  );
}

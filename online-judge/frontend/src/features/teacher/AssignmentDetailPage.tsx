import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, Copy, PenLine, RotateCw } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { ApiError, api } from "../../shared/api/client";
import type { Assignment, AssignmentOverview, DiagnosisTag } from "../../shared/api/types";
import { assignmentStatusLabel, displayText, formatDateTime, hintPolicyLabel, issueLabel, verdictLabel } from "../../shared/format";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea } from "../../shared/ui/Field";
import { StatusPill, VerdictPill } from "../../shared/ui/StatusPill";

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

export default function AssignmentDetailPage() {
  const { assignmentId } = useParams<{ assignmentId: string }>();
  const id = Number(assignmentId);
  const [assignment, setAssignment] = useState<Assignment | null>(null);
  const [overview, setOverview] = useState<AssignmentOverview | null>(null);
  const [diagnosisTags, setDiagnosisTags] = useState<DiagnosisTag[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [alert, setAlert] = useState<Alert | null>(null);
  const [selectedStudent, setSelectedStudent] = useState<OverviewStudent | null>(null);
  const [correctionDraft, setCorrectionDraft] = useState<CorrectionDraft | null>(null);

  const coarseDiagnosisTags = useMemo(() => diagnosisTags.filter(tag => !tag.fineGrained), [diagnosisTags]);
  const fineDiagnosisTags = useMemo(() => diagnosisTags.filter(tag => tag.fineGrained), [diagnosisTags]);
  const attentionStudents = useMemo(() => overview?.students.filter(student => student.needsAttention) || [], [overview]);
  const visibleStudents = useMemo(() => {
    const students = overview?.students || [];
    return [...students].sort((left, right) => Number(Boolean(right.needsAttention)) - Number(Boolean(left.needsAttention)));
  }, [overview]);
  const passRate = overview?.attemptCount ? Math.round((overview.passedAttemptCount / overview.attemptCount) * 100) : 0;
  const classroomStateLabel = attentionStudents.length ? `${attentionStudents.length} 人需关注` : "课堂过程稳定";

  useEffect(() => {
    if (!Number.isFinite(id)) {
      setLoading(false);
      return;
    }
    void loadAssignmentDetail(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

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

  async function copyInviteCode() {
    if (!assignment?.inviteCode) {
      setAlert({ type: "error", message: "当前作业还没有邀请码。" });
      return;
    }
    try {
      await navigator.clipboard.writeText(assignment.inviteCode);
      setAlert({ type: "success", message: "邀请码已复制。" });
    } catch {
      setAlert({ type: "error", message: "复制失败，请手动复制邀请码。" });
    }
  }

  async function rotateInviteCode() {
    if (!Number.isFinite(id)) {
      return;
    }
    setSaving(true);
    try {
      const updated = await api.rotateInvite(id);
      setAssignment(updated);
      setAlert({ type: "success", message: "邀请码已更新。" });
    } catch (error) {
      setAlert({ type: "error", message: teacherErrorMessage(error, "邀请码更新失败。") });
    } finally {
      setSaving(false);
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
        <EmptyState title="作业未找到" description="返回教师总览重新选择作业。" />
        <ButtonLink to="/app/teacher" variant="secondary" icon={<ArrowLeft size={16} />}>
          返回教师总览
        </ButtonLink>
      </div>
    );
  }

  return (
    <div className="stack assignment-detail-page">
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="assignment-detail-command">
        <div>
          <Link to="/app/teacher" className="back-link">
            <ArrowLeft size={16} /> 返回教师总览
          </Link>
          <p className="eyebrow">单作业过程</p>
          <h1>{cleanAssignmentTitle(assignment.title)}</h1>
          <div className="teacher-stage__meta">
            <span>{displayText(assignment.className, "未绑定班级")}</span>
            <span>{assignment.tasks?.length || 0} 个任务</span>
            <span>{hintPolicyLabel(assignment.hintPolicy)}</span>
            <span>{assignmentStatusLabel(assignment.status)}</span>
          </div>
        </div>
        <div className="assignment-detail-actions">
          <StatusPill tone="info">邀请码 {assignment.inviteCode || "-"}</StatusPill>
          <Button type="button" variant="secondary" icon={<Copy size={16} />} onClick={() => void copyInviteCode()}>
            复制
          </Button>
          <Button type="button" variant="secondary" icon={<RotateCw size={16} />} onClick={() => void rotateInviteCode()} disabled={saving}>
            重置
          </Button>
        </div>
      </section>

      <section className="teacher-kpi-strip assignment-kpi-strip" aria-label="作业 KPI">
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
        <div>
          <span>需关注</span>
          <strong>{attentionStudents.length}</strong>
        </div>
      </section>

      <section className="assignment-focus-strip" aria-label="作业状态摘要">
        <article className={attentionStudents.length ? "is-warning" : "is-stable"}>
          <span>课堂状态</span>
          <strong>{classroomStateLabel}</strong>
        </article>
        <article>
          <span>高频问题</span>
          <strong>{overview.topIssues[0] ? issueLabel(overview.topIssues[0].label) : "暂无高频问题"}</strong>
        </article>
        <article>
          <span>优先查看</span>
          <strong>{attentionStudents[0]?.displayName || "继续观察提交变化"}</strong>
        </article>
      </section>

      <section className="assignment-detail-grid">
        <div className="teacher-block assignment-issue-panel">
          <div className="teacher-block__head">
            <h3>高频问题</h3>
            <StatusPill tone={overview.topIssues.length ? "warning" : "success"}>{overview.topIssues.length || "无"}</StatusPill>
          </div>
          {overview.topIssues.length ? (
            overview.topIssues.slice(0, 6).map(issue => (
              <div className="issue-row" key={issue.label}>
                <span>
                  {issueLabel(issue.label)}
                  {issue.explanation ? <small>{issue.explanation}</small> : null}
                </span>
                <strong>{issue.count}</strong>
              </div>
            ))
          ) : (
            <EmptyState title="暂无问题分类" />
          )}
        </div>

        <div className="teacher-block assignment-student-panel">
          <div className="teacher-block__head">
            <h3>{attentionStudents.length ? "需要关注" : "学生列表"}</h3>
            <StatusPill tone={attentionStudents.length ? "warning" : "success"}>
              {visibleStudents.length ? `${visibleStudents.length} 人` : "正常"}
            </StatusPill>
          </div>
          {visibleStudents.length ? (
            visibleStudents.map(student => (
              <div className="teacher-student-row" key={student.studentProfileId}>
                <div>
                  <h3>{displayText(student.displayName, `学生 #${student.studentProfileId}`)}</h3>
                  <div className="teacher-student-row__meta">
                    <span className="teacher-row-meta-text">{student.attemptCount} 次尝试</span>
                    <VerdictPill verdict={student.latestVerdict} />
                    {student.needsAttention && <StatusPill tone="warning">需关注</StatusPill>}
                  </div>
                  {student.latestIssueTag && (
                    <p>
                      最近问题：
                      {student.latestFineGrainedIssue
                        ? issueLabel(student.latestFineGrainedIssue)
                        : issueLabel(student.latestIssueTag)}
                    </p>
                  )}
                  {student.attentionReason && <p>{student.attentionReason}</p>}
                  {student.attentionEvidence?.length ? (
                    <div className="teacher-attention-evidence">
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
                          <small>{evidence.reason || evidence.headline || "最近提交证据"}</small>
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
                </div>
              </div>
            ))
          ) : (
            <EmptyState title="暂无学生提交" />
          )}
        </div>
      </section>

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

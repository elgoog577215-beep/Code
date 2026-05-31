import { useEffect, useMemo, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Copy, PenLine, RefreshCw, RotateCw } from "lucide-react";
import { api } from "../../shared/api/client";
import type { Assignment, AssignmentOverview, DiagnosisTag } from "../../shared/api/types";
import { displayText, hintPolicyLabel, issueLabel, verdictLabel } from "../../shared/format";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea } from "../../shared/ui/Field";
import { StatusPill, VerdictPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };

export default function AssignmentDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [assignment, setAssignment] = useState<Assignment | null>(null);
  const [overview, setOverview] = useState<AssignmentOverview | null>(null);
  const [diagnosisTags, setDiagnosisTags] = useState<DiagnosisTag[]>([]);
  const [alert, setAlert] = useState<Alert | null>(null);
  const [busy, setBusy] = useState(true);
  const [selectedStudent, setSelectedStudent] = useState<AssignmentOverview["students"][number] | null>(null);
  const [correctionDraft, setCorrectionDraft] = useState<{ correctedIssueTag: string; correctedFineGrainedTag: string; teacherNote: string } | null>(null);

  const numId = id ? Number(id) : null;

  useEffect(() => {
    if (!numId) return;
    setBusy(true);
    Promise.all([api.assignments(), api.assignmentOverview(numId), api.diagnosisTags()])
      .then(([assignments, ov, tags]) => {
        const found = assignments.find(a => a.id === numId);
        setAssignment(found || null);
        setOverview(ov);
        setDiagnosisTags(tags);
      })
      .catch(() => setAlert({ type: "error", message: "加载失败" }))
      .finally(() => setBusy(false));
  }, [numId]);

  const coarseTags = useMemo(() => diagnosisTags.filter(t => !t.fineGrained), [diagnosisTags]);
  const fineTags = useMemo(() => diagnosisTags.filter(t => t.fineGrained), [diagnosisTags]);

  async function copyInvite() {
    if (!assignment?.inviteCode) return;
    try {
      await navigator.clipboard.writeText(assignment.inviteCode);
      setAlert({ type: "success", message: "邀请码已复制" });
    } catch { setAlert({ type: "error", message: "复制失败" }); }
  }

  async function rotateInvite() {
    if (!numId) return;
    try {
      const result = await api.rotateInvite(numId);
      setAlert({ type: "success", message: `邀请码已更新` });
      if (assignment) setAssignment({ ...assignment, inviteCode: result.inviteCode });
    } catch { setAlert({ type: "error", message: "更新失败" }); }
  }

  function openCorrection(student: AssignmentOverview["students"][number]) {
    if (!student.latestSubmissionId) return;
    setSelectedStudent(student);
    setCorrectionDraft({
      correctedIssueTag: student.latestCorrection?.correctedIssueTag || student.latestIssueTag || "",
      correctedFineGrainedTag: student.latestCorrection?.correctedFineGrainedTag || "",
      teacherNote: student.latestCorrection?.teacherNote || ""
    });
  }

  async function saveCorrection() {
    if (!numId || !correctionDraft || !selectedStudent?.latestSubmissionId) return;
    setBusy(true);
    try {
      await api.correctDiagnosis(numId, {
        submissionId: selectedStudent.latestSubmissionId,
        correctedIssueTag: correctionDraft.correctedIssueTag,
        correctedFineGrainedTag: correctionDraft.correctedFineGrainedTag || null,
        teacherNote: correctionDraft.teacherNote,
        evalCandidate: true,
        correctedBy: "teacher"
      });
      setAlert({ type: "success", message: "校正已保存" });
      setCorrectionDraft(null);
      setSelectedStudent(null);
      const ov = await api.assignmentOverview(numId);
      setOverview(ov);
    } catch { setAlert({ type: "error", message: "保存失败" }); }
    finally { setBusy(false); }
  }

  if (busy) return <EmptyState title="加载中" />;
  if (!assignment) return <EmptyState title="作业未找到" />;

  return (
    <div className="stack assignment-detail-page">
      {alert && <div className={`alert alert--${alert.type}`}>{alert.message}</div>}

      <section className="assignment-detail-hero">
        <div>
          <button type="button" className="back-link" onClick={() => navigate("/teacher")}>
            <ArrowLeft size={16} /> 返回
          </button>
          <h1>{assignment.title}</h1>
          <div className="teacher-stage__meta">
            <span>{displayText(assignment.className, "未绑定班级")}</span>
            <span>{assignment.tasks?.length || 0} 个题目</span>
            <span>{hintPolicyLabel(assignment.hintPolicy)}</span>
          </div>
        </div>
        <div className="teacher-stage-toolbar">
          <StatusPill tone="info">{assignment.inviteCode || "无邀请码"}</StatusPill>
          <Button variant="secondary" icon={<Copy size={16} />} onClick={copyInvite}>复制</Button>
          <Button variant="secondary" icon={<RotateCw size={16} />} onClick={rotateInvite}>重置</Button>
        </div>
      </section>

      {overview && (
        <div className="teacher-kpi-strip">
          <div><span>参与学生</span><strong>{overview.participantCount}</strong></div>
          <div><span>提交次数</span><strong>{overview.attemptCount}</strong></div>
          <div><span>通过次数</span><strong>{overview.passedAttemptCount}</strong></div>
          <div><span>通过率</span><strong>{overview.attemptCount ? Math.round((overview.passedAttemptCount / overview.attemptCount) * 100) : 0}%</strong></div>
        </div>
      )}

      <section className="assignment-detail-grid">
        <div>
          <div className="teacher-block">
            <div className="teacher-block__head">
              <h3>高频问题</h3>
              <StatusPill tone={overview?.topIssues?.length ? "warning" : "success"}>
                {overview?.topIssues?.length || "无"}
              </StatusPill>
            </div>
            {overview?.topIssues?.length ? (
              overview.topIssues.slice(0, 5).map(issue => (
                <div className="issue-row" key={issue.label}>
                  <span>{issueLabel(issue.label)}
                    {issue.explanation ? <small>{issue.explanation}</small> : null}
                  </span>
                  <strong>{issue.count}</strong>
                </div>
              ))
            ) : (
              <EmptyState title="暂无问题分类" />
            )}
          </div>
        </div>

        <div className="teacher-block">
          <div className="teacher-block__head">
            <h3>学生列表</h3>
            <StatusPill tone="neutral">{overview?.students?.length || 0} 人</StatusPill>
          </div>
          {overview?.students?.length ? (
            overview.students.map(student => (
              <div className="teacher-student-row" key={student.studentProfileId}>
                <div>
                  <h3>{student.displayName}</h3>
                  <div className="teacher-student-row__meta">
                    <span>{student.attemptCount} 次尝试</span>
                    <VerdictPill verdict={student.latestVerdict || ""} />
                    {student.needsAttention && <StatusPill tone="warning">需关注</StatusPill>}
                  </div>
                </div>
                <Button variant="ghost" icon={<PenLine size={15} />} onClick={() => openCorrection(student)} disabled={!student.latestSubmissionId}>
                  校正
                </Button>
              </div>
            ))
          ) : (
            <EmptyState title="暂无学生提交" />
          )}
        </div>
      </section>

      {correctionDraft && selectedStudent && (
        <section className="teacher-correction-panel">
          <div>
            <p className="eyebrow">教师校正</p>
            <h2>修正 {selectedStudent.displayName} 的错因</h2>
          </div>
          <div className="form-grid">
            <Field label="主要错因">
              <Select value={correctionDraft.correctedIssueTag} onChange={e => setCorrectionDraft({ ...correctionDraft, correctedIssueTag: e.target.value })}>
                <option value="">请选择</option>
                {coarseTags.map(tag => <option value={tag.id} key={tag.id}>{tag.label}</option>)}
              </Select>
            </Field>
            <Field label="细分错因">
              <Select value={correctionDraft.correctedFineGrainedTag} onChange={e => setCorrectionDraft({ ...correctionDraft, correctedFineGrainedTag: e.target.value })}>
                <option value="">不指定</option>
                {fineTags.map(tag => <option value={tag.id} key={tag.id}>{tag.label}</option>)}
              </Select>
            </Field>
          </div>
          <Field label="校正理由">
            <TextArea value={correctionDraft.teacherNote} onChange={e => setCorrectionDraft({ ...correctionDraft, teacherNote: e.target.value })} placeholder="例如：学生不是边界问题" />
          </Field>
          <div className="actions">
            <Button variant="primary" onClick={saveCorrection} disabled={busy}>保存校正</Button>
            <Button variant="ghost" onClick={() => { setCorrectionDraft(null); setSelectedStudent(null); }}>取消</Button>
          </div>
        </section>
      )}
    </div>
  );
}
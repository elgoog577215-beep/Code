import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, BarChart3, BookOpenCheck, Brain, PenLine, UsersRound, type LucideIcon } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { ApiError, api } from "../../shared/api/client";
import type { Assignment, AssignmentOverview, DiagnosisTag } from "../../shared/api/types";
import { assignmentStatusLabel, displayText, formatDateTime, issueLabel, percent, verdictLabel } from "../../shared/format";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea } from "../../shared/ui/Field";
import { DifficultyPill, StatusPill, VerdictPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };
type OverviewStudent = AssignmentOverview["students"][number];
type DetailTab = "overview" | "students" | "problems" | "diagnosis";
type CorrectionDraft = {
  studentProfileId: number;
  submissionId: number;
  correctedIssueTag: string;
  correctedFineGrainedTag: string;
  teacherNote: string;
};

const DETAIL_TABS: Array<{ id: DetailTab; label: string; icon: LucideIcon }> = [
  { id: "overview", label: "概览", icon: BarChart3 },
  { id: "students", label: "学生", icon: UsersRound },
  { id: "problems", label: "题目", icon: BookOpenCheck },
  { id: "diagnosis", label: "诊断", icon: Brain }
];

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
    const relatedStudents = overview.students.filter(
      student =>
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

function classroomState(attentionCount: number) {
  return attentionCount ? `${attentionCount} 人需关注` : "推进稳定";
}

export default function AssignmentDetailPage() {
  const { assignmentId } = useParams<{ assignmentId: string }>();
  const id = Number(assignmentId);
  const [assignment, setAssignment] = useState<Assignment | null>(null);
  const [overview, setOverview] = useState<AssignmentOverview | null>(null);
  const [diagnosisTags, setDiagnosisTags] = useState<DiagnosisTag[]>([]);
  const [activeTab, setActiveTab] = useState<DetailTab>("overview");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [alert, setAlert] = useState<Alert | null>(null);
  const [selectedStudentId, setSelectedStudentId] = useState<number | null>(null);
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
  const selectedStudent = visibleStudents.find(student => student.studentProfileId === selectedStudentId) || visibleStudents[0] || null;
  const taskStats = assignment && overview ? problemStats(assignment, overview) : [];
  const topTask = taskStats.find(task => task.issueHits) || taskStats[0] || null;
  const reviewItem = overview?.classReviewSuggestions?.[0] || null;

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
    setActiveTab("diagnosis");
    setSelectedStudentId(student.studentProfileId);
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
      <div className="stack assignment-detail-page teacher-workflow">
        {alert && <div className="alert alert--error">{alert.message}</div>}
        <EmptyState title="作业未找到" description="返回作业中心重新选择作业。" />
        <ButtonLink to="/app/teacher" variant="secondary" icon={<ArrowLeft size={16} />}>
          返回作业中心
        </ButtonLink>
      </div>
    );
  }

  const stateLabel = classroomState(attentionStudents.length);

  return (
    <div className="teacher-page teacher-workflow assignment-detail-page assignment-detail-workflow">
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="teacher-workflow-header assignment-detail-header">
        <div>
          <Link to="/app/teacher" className="back-link">
            <ArrowLeft size={16} /> 返回作业中心
          </Link>
          <p className="eyebrow">作业详情</p>
          <h1>{cleanAssignmentTitle(assignment.title)}</h1>
          <div className="assignment-detail-meta">
            <span>{displayText(assignment.className, "未绑定班级")}</span>
            <span>{assignment.tasks?.length || 0} 题</span>
            <span>{assignmentStatusLabel(assignment.status)}</span>
          </div>
        </div>
        <div className="assignment-detail-score">
          <StatusPill tone={attentionStudents.length ? "warning" : "success"}>{stateLabel}</StatusPill>
          <strong>{passRate}%</strong>
          <span>提交通过率</span>
        </div>
      </section>

      <nav className="assignment-detail-tabs" aria-label="作业详情标签">
        {DETAIL_TABS.map(tab => {
          const Icon = tab.icon;
          return (
            <button type="button" className={activeTab === tab.id ? "is-active" : ""} key={tab.id} onClick={() => setActiveTab(tab.id)}>
              <Icon size={16} />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </nav>

      {activeTab === "overview" && (
        <section className="assignment-tab-panel assignment-overview-tab" aria-label="概览">
          <div className="teacher-workflow-summary">
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
          </div>

          <div className="assignment-overview-grid">
            <article>
              <span>完成情况</span>
              <strong>{stateLabel}</strong>
              <p>{attentionStudents[0]?.attentionReason || overview.classTeachingStrategySignal?.summary || "当前作业推进稳定。"}</p>
            </article>
            <article>
              <span>优先看谁</span>
              <strong>{displayText(attentionStudents[0]?.displayName, attentionStudents.length ? "需关注学生" : "暂无需关注")}</strong>
              <p>{attentionStudents[0]?.latestProgressSignal || attentionStudents[0]?.crossProblemSummary || "学生整体暂未出现集中卡点。"}</p>
            </article>
            <article>
              <span>优先讲哪题</span>
              <strong>{topTask?.task.title || "等待提交"}</strong>
              <p>{topTask?.issueHits ? `${topTask.issueHits} 人在这道题出现需关注信号。` : "暂无明显题目集中问题。"}</p>
            </article>
          </div>

          <div className="assignment-next-actions">
            <Button type="button" variant="secondary" onClick={() => setActiveTab("students")} icon={<UsersRound size={16} />}>
              查看学生
            </Button>
            <Button type="button" variant="secondary" onClick={() => setActiveTab("problems")} icon={<BookOpenCheck size={16} />}>
              查看题目
            </Button>
            <Button type="button" variant="ghost" onClick={() => setActiveTab("diagnosis")} icon={<Brain size={16} />}>
              进入诊断
            </Button>
          </div>
        </section>
      )}

      {activeTab === "students" && (
        <section className="assignment-tab-panel assignment-students-tab" aria-label="学生">
          <div className="assignment-tab-head">
            <div>
              <p className="eyebrow">学生</p>
              <h2>按需关注优先查看</h2>
            </div>
            <StatusPill tone={attentionStudents.length ? "warning" : "success"}>{stateLabel}</StatusPill>
          </div>
          <div className="assignment-students-workflow">
            <div className="assignment-student-list">
              {visibleStudents.map(student => (
                <button
                  type="button"
                  className={`assignment-student-card ${student.studentProfileId === selectedStudent?.studentProfileId ? "is-active" : ""}`}
                  key={student.studentProfileId}
                  onClick={() => setSelectedStudentId(student.studentProfileId)}
                >
                  <span>
                    <strong>{displayText(student.displayName, `学生 #${student.studentProfileId}`)}</strong>
                    <small>
                      {student.attemptCount} 次尝试 · {studentPassRate(student)}% 通过
                    </small>
                  </span>
                  {student.needsAttention ? <StatusPill tone="warning">需关注</StatusPill> : <StatusPill tone="success">稳定</StatusPill>}
                </button>
              ))}
            </div>

            {selectedStudent ? (
              <article className="assignment-student-report">
                <div className="assignment-student-report__head">
                  <div>
                    <h3>{displayText(selectedStudent.displayName, "学生")}</h3>
                    <p>
                      {selectedStudent.studentNo ? `${selectedStudent.studentNo} 号 · ` : ""}
                      {selectedStudent.attemptCount} 次尝试 · {selectedStudent.passedCount} 次通过
                    </p>
                  </div>
                  <VerdictPill verdict={selectedStudent.latestVerdict} />
                </div>
                <div className="assignment-student-report__metrics">
                  <div>
                    <span>通过率</span>
                    <strong>{studentPassRate(selectedStudent)}%</strong>
                  </div>
                  <div>
                    <span>重复问题</span>
                    <strong>{selectedStudent.repeatedIssueCount || 0}</strong>
                  </div>
                  <div>
                    <span>能力焦点</span>
                    <strong>{displayText(selectedStudent.primaryAbilityFocus, "待观察")}</strong>
                  </div>
                </div>
                <div className="assignment-student-report__body">
                  <p>{selectedStudent.latestProgressSignal || selectedStudent.attentionReason || selectedStudent.crossProblemSummary || "暂无明确反馈。"}</p>
                  {selectedStudent.latestIssueTag && (
                    <p>
                      <strong>最近问题：</strong>
                      {issueLabel(selectedStudent.latestFineGrainedIssue || selectedStudent.latestIssueTag)}
                    </p>
                  )}
                </div>
                <div className="actions">
                  <Button
                    type="button"
                    variant="secondary"
                    icon={<PenLine size={15} />}
                    onClick={() => openCorrection(selectedStudent)}
                    disabled={!selectedStudent.latestSubmissionId}
                  >
                    在诊断中校正错因
                  </Button>
                </div>
              </article>
            ) : (
              <EmptyState title="暂无学生提交" />
            )}
          </div>
        </section>
      )}

      {activeTab === "problems" && (
        <section className="assignment-tab-panel assignment-problems-tab" aria-label="题目">
          <div className="assignment-tab-head">
            <div>
              <p className="eyebrow">题目</p>
              <h2>看题目表现与讲评优先级</h2>
            </div>
            <StatusPill tone="neutral">{assignment.tasks.length} 题</StatusPill>
          </div>
          <div className="assignment-task-list">
            {taskStats.map(item => (
              <article className="assignment-task-row" key={item.task.problemId}>
                <div>
                  <DifficultyPill difficulty={item.task.difficulty} />
                  <h3>{item.task.title}</h3>
                  <p>{item.latestAttempts ? `${item.latestAttempts} 次相关尝试 · ${item.latestPassed} 次通过` : "等待学生提交"}</p>
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
      )}

      {activeTab === "diagnosis" && (
        <section className="assignment-tab-panel assignment-diagnosis-tab" aria-label="诊断">
          <div className="assignment-tab-head">
            <div>
              <p className="eyebrow">诊断</p>
              <h2>AI / Coach / 教师校正</h2>
            </div>
            <StatusPill tone="info">二级诊断</StatusPill>
          </div>

          <div className="assignment-diagnosis-grid">
            <article>
              <span>课堂策略</span>
              <strong>{overview.classTeachingStrategySignal?.title || overview.classTeachingStrategySignal?.statusLabel || reviewItem?.title || "按学生情况复盘"}</strong>
              <p>
                {overview.classTeachingStrategySignal?.summary ||
                  overview.classTeachingStrategySignal?.teacherAction ||
                  reviewItem?.guidingQuestion ||
                  "优先查看需关注学生的最近提交。"}
              </p>
            </article>
            <article>
              <span>Coach 回答质量</span>
              <strong>{overview.coachAnswerQualitySummary?.summary || "等待更多追问样本"}</strong>
              <p>{overview.coachAnswerQualitySummary?.recommendedAction || "用于观察学生是否真正理解反馈。"}</p>
            </article>
            <article>
              <span>Coach 后续成效</span>
              <strong>{overview.coachFollowupImpactSummary?.summary || "等待后续提交样本"}</strong>
              <p>{overview.coachFollowupImpactSummary?.recommendedAction || "用于判断反馈是否改善下一次提交。"}</p>
            </article>
          </div>

          {selectedStudent ? (
            <article className="assignment-diagnosis-evidence">
              <div>
                <p className="eyebrow">学生证据</p>
                <h3>{displayText(selectedStudent.displayName, "学生")}</h3>
              </div>
              {selectedStudent.attentionEvidence?.length ? (
                <div className="teacher-attention-evidence">
                  {selectedStudent.attentionEvidence.slice(0, 3).map(evidence => (
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
              ) : (
                <p>这名学生暂时没有可展示的错因证据。</p>
              )}
              <Button
                type="button"
                variant="secondary"
                icon={<PenLine size={15} />}
                onClick={() => openCorrection(selectedStudent)}
                disabled={!selectedStudent.latestSubmissionId}
              >
                校正错因
              </Button>
            </article>
          ) : (
            <EmptyState title="暂无学生诊断证据" />
          )}

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
                <Button type="button" variant="ghost" onClick={() => setCorrectionDraft(null)}>
                  取消
                </Button>
              </div>
            </section>
          )}
        </section>
      )}
    </div>
  );
}

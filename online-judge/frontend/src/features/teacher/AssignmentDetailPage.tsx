import { FormEvent, useEffect, useMemo, useState } from "react";
import { ArrowLeft, ArrowRight, ChartNoAxesCombined, PenLine, Presentation, UserRound } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { ApiError, api } from "../../shared/api/client";
import type { AssignmentOverview, DiagnosisTag } from "../../shared/api/types";
import { assignmentStatusLabel, confidenceLabel, displayText, formatDateTime, hintPolicyLabel, issueLabel, percent, verdictLabel } from "../../shared/format";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea } from "../../shared/ui/Field";
import { DifficultyPill, StatusPill, VerdictPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };
type ProblemSummary = NonNullable<AssignmentOverview["problemSummaries"]>[number];
type ProblemStudent = NonNullable<ProblemSummary["students"]>[number];
type OverviewStudent = AssignmentOverview["students"][number];
type TrendPoint = NonNullable<AssignmentOverview["progressTrend"]>[number];
type CorrectionDraft = {
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

function formatCountRate(value?: number | null) {
  return typeof value === "number" && Number.isFinite(value) ? percent(value) : "-";
}

function formatNumber(value?: number | null, digits = 1) {
  return typeof value === "number" && Number.isFinite(value) ? value.toFixed(digits).replace(/\.0$/, "") : "-";
}

function statusTone(status?: string | null): "neutral" | "success" | "warning" | "danger" | "info" {
  if (status === "已掌握") {
    return "success";
  }
  if (status === "需讲评") {
    return "warning";
  }
  if (status === "推进中") {
    return "info";
  }
  return "neutral";
}

function latestTrend(points: TrendPoint[]) {
  return points[points.length - 1] || null;
}

function trendSummary(points: TrendPoint[], overview: AssignmentOverview) {
  const latest = latestTrend(points);
  return {
    submitted: latest?.submittedStudentCount ?? 0,
    passed: latest?.passedStudentCount ?? 0,
    attempts: latest?.submissionCount ?? overview.attemptCount
  };
}

function recentTrendDelta(points: TrendPoint[]) {
  if (points.length >= 2) {
    return Math.max(0, points[points.length - 1].submissionCount - points[points.length - 2].submissionCount);
  }
  return points[0]?.submissionCount ?? 0;
}

function trendPath(points: TrendPoint[], key: "submittedStudentCount" | "passedStudentCount" | "submissionCount") {
  if (!points.length) {
    return "";
  }
  const maxValue = Math.max(1, ...points.map(point => point[key] || 0));
  const maxIndex = Math.max(1, points.length - 1);
  return points
    .map((point, index) => {
      const x = (index / maxIndex) * 100;
      const y = 100 - ((point[key] || 0) / maxValue) * 88 - 6;
      return `${x.toFixed(2)},${y.toFixed(2)}`;
    })
    .join(" ");
}

function visibleProblems(overview: AssignmentOverview) {
  return [...(overview.problemSummaries || [])].sort((left, right) => (left.orderIndex ?? 0) - (right.orderIndex ?? 0));
}

function studentPassRate(student: ProblemStudent) {
  return student.attemptCount ? Math.round((student.passedCount / student.attemptCount) * 100) : 0;
}

function assignmentPassRate(overview: AssignmentOverview) {
  return overview.attemptCount ? Math.round((overview.passedAttemptCount / overview.attemptCount) * 100) : 0;
}

function problemStudentIssue(student: ProblemStudent) {
  return student.latestFineGrainedIssue || student.latestIssueTag ? issueLabel(student.latestFineGrainedIssue || student.latestIssueTag) : "-";
}

function latestStudentSubmission(students: ProblemStudent[]) {
  return [...students]
    .map(student => student.latestSubmittedAt)
    .filter(Boolean)
    .sort((left, right) => Date.parse(right || "") - Date.parse(left || ""))[0];
}

function sortedProblemStudents(students: ProblemStudent[]) {
  return [...students].sort((left, right) => {
    const attention = Number(right.needsAttention) - Number(left.needsAttention);
    if (attention) {
      return attention;
    }
    const submitted = Date.parse(right.latestSubmittedAt || "") - Date.parse(left.latestSubmittedAt || "");
    if (submitted) {
      return submitted;
    }
    return displayText(left.displayName, "").localeCompare(displayText(right.displayName, ""), "zh-Hans-CN");
  });
}

function timeValue(value?: string | null) {
  const parsed = Date.parse(value || "");
  return Number.isFinite(parsed) ? parsed : 0;
}

function problemPriorityScore(problem: ProblemSummary) {
  const passPenalty = typeof problem.passRate === "number" ? Math.max(0, 1 - problem.passRate) * 100 : 0;
  const statusWeight = problem.statusLabel === "需讲评" ? 800 : problem.statusLabel === "推进中" ? 220 : 0;
  return statusWeight + problem.attentionStudentCount * 80 + passPenalty + problem.submissionCount * 0.5;
}

function priorityProblem(problems: ProblemSummary[]) {
  const visible = problems.filter(problem => problem.submissionCount > 0 || problem.attentionStudentCount > 0);
  return [...(visible.length ? visible : problems)].sort((left, right) => problemPriorityScore(right) - problemPriorityScore(left))[0] || null;
}

function priorityStudentTarget(overview: AssignmentOverview, problems: ProblemSummary[]) {
  const problemTitleById = new Map(problems.map(problem => [problem.problemId, problem.title]));
  return overview.students
    .filter(student => student.needsAttention)
    .map(student => {
      const evidence = student.attentionEvidence?.find(item => item.problemId) || student.attentionEvidence?.[0] || null;
      const submittedAt = evidence?.submittedAt || "";
      return {
        student,
        problemId: evidence?.problemId,
        problemTitle: evidence?.problemId ? problemTitleById.get(evidence.problemId) : null,
        submittedAt,
        score: (student.repeatedIssueCount || 0) * 12 + (evidence?.problemId ? 6 : 0) + (submittedAt ? 3 : 0)
      };
    })
    .sort((left, right) => {
      const score = right.score - left.score;
      if (score) {
        return score;
      }
      return timeValue(right.submittedAt) - timeValue(left.submittedAt);
    })[0] || null;
}

function priorityStudentCopy(student: OverviewStudent) {
  if (student.attentionReason) {
    return student.attentionReason;
  }
  if (student.repeatedFineGrainedTag || student.repeatedIssueTag) {
    return `${issueLabel(student.repeatedFineGrainedTag || student.repeatedIssueTag)}反复出现`;
  }
  return student.latestProgressSignal || "有新的卡点需要确认。";
}

export default function AssignmentDetailPage() {
  const { assignmentId, problemId, studentProfileId } = useParams<{
    assignmentId: string;
    problemId?: string;
    studentProfileId?: string;
  }>();
  const id = Number(assignmentId);
  const currentProblemId = Number(problemId);
  const currentStudentId = Number(studentProfileId);
  const [overview, setOverview] = useState<AssignmentOverview | null>(null);
  const [diagnosisTags, setDiagnosisTags] = useState<DiagnosisTag[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [alert, setAlert] = useState<Alert | null>(null);
  const [correctionDraft, setCorrectionDraft] = useState<CorrectionDraft | null>(null);

  const coarseDiagnosisTags = useMemo(() => diagnosisTags.filter(tag => !tag.fineGrained), [diagnosisTags]);
  const fineDiagnosisTags = useMemo(() => diagnosisTags.filter(tag => tag.fineGrained), [diagnosisTags]);

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
      const [overviewResult, tags] = await Promise.all([api.assignmentOverview(targetId), api.diagnosisTags()]);
      setOverview(overviewResult);
      setDiagnosisTags(tags);
    } catch (error) {
      setOverview(null);
      setAlert({ type: "error", message: teacherErrorMessage(error, "作业详情加载失败。") });
    } finally {
      setLoading(false);
    }
  }

  async function saveCorrection(event: FormEvent) {
    event.preventDefault();
    if (!overview || !correctionDraft) {
      return;
    }
    if (!correctionDraft.correctedIssueTag) {
      setAlert({ type: "error", message: "请选择主要错因。" });
      return;
    }
    setSaving(true);
    try {
      await api.correctDiagnosis(overview.assignment.id, {
        submissionId: correctionDraft.submissionId,
        correctedIssueTag: correctionDraft.correctedIssueTag,
        correctedFineGrainedTag: correctionDraft.correctedFineGrainedTag || null,
        teacherNote: correctionDraft.teacherNote,
        evalCandidate: true,
        correctedBy: "teacher"
      });
      setAlert({ type: "success", message: "教师校正已保存。" });
      setCorrectionDraft(null);
      await loadAssignmentDetail(overview.assignment.id);
    } catch (error) {
      setAlert({ type: "error", message: teacherErrorMessage(error, "教师校正保存失败。") });
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <EmptyState title="正在加载作业详情" live />;
  }

  if (!overview) {
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

  const problems = visibleProblems(overview);
  const selectedProblem = problems.find(problem => problem.problemId === currentProblemId) || null;
  const selectedStudent = selectedProblem?.students?.find(student => student.studentProfileId === currentStudentId) || null;

  return (
    <div className="teacher-page teacher-workflow assignment-drill-page">
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      {!selectedProblem ? <AssignmentOverviewView overview={overview} problems={problems} /> : null}
      {selectedProblem && !selectedStudent ? <ProblemOverviewView overview={overview} problem={selectedProblem} /> : null}
      {selectedProblem && selectedStudent ? (
        <StudentProblemView
          overview={overview}
          problem={selectedProblem}
          student={selectedStudent}
          correctionDraft={correctionDraft}
          setCorrectionDraft={setCorrectionDraft}
          coarseDiagnosisTags={coarseDiagnosisTags}
          fineDiagnosisTags={fineDiagnosisTags}
          saving={saving}
          saveCorrection={saveCorrection}
        />
      ) : null}
    </div>
  );
}

function AssignmentOverviewView({ overview, problems }: { overview: AssignmentOverview; problems: ProblemSummary[] }) {
  const trend = overview.progressTrend || [];
  const latest = latestTrend(trend);
  const currentTrend = trendSummary(trend, overview);
  const passRate = assignmentPassRate(overview);
  const recentSubmissions = recentTrendDelta(trend);
  const submittedText = `${currentTrend.submitted} 人`;
  const studentTarget = priorityStudentTarget(overview, problems);
  const lectureProblem = priorityProblem(problems);
  const lectureIssue = lectureProblem?.topIssues?.[0] || null;
  const studentTargetHref = studentTarget?.problemId
    ? `/app/teacher/assignment/${overview.assignment.id}/problems/${studentTarget.problemId}/students/${studentTarget.student.studentProfileId}`
    : `/app/teacher/assignment/${overview.assignment.id}`;
  const lectureHref = lectureProblem
    ? `/app/teacher/assignment/${overview.assignment.id}/problems/${lectureProblem.problemId}`
    : `/app/teacher/assignment/${overview.assignment.id}`;
  return (
    <>
      <section className="teacher-drill-header">
        <div>
          <Link to="/app/teacher" className="back-link">
            <ArrowLeft size={16} /> 返回作业中心
          </Link>
          <h1>{cleanAssignmentTitle(overview.assignment.title)}</h1>
          <div className="assignment-detail-meta">
            <span>{displayText(overview.assignment.className, "默认班级")}</span>
            <span>{problems.length} 题</span>
            <span>{assignmentStatusLabel(overview.assignment.status)}</span>
            <span>{submittedText} 已提交</span>
            <span>{passRate}% 通过</span>
          </div>
        </div>
      </section>

      <section className="teacher-drill-overview">
        <section className="teacher-priority-grid" aria-label="作业优先处理">
          <article className={`teacher-priority-card ${studentTarget ? "" : "is-muted"}`}>
            <span className="teacher-priority-card__icon">
              <UserRound size={18} />
            </span>
            <div>
              <p className="eyebrow">先看学生</p>
              <h2>{studentTarget ? displayText(studentTarget.student.displayName, `学生 #${studentTarget.student.studentProfileId}`) : "暂无优先学生"}</h2>
              <p>
                {studentTarget
                  ? `${studentTarget.problemTitle ? `${studentTarget.problemTitle}：` : ""}${priorityStudentCopy(studentTarget.student)}`
                  : "目前没有被标记为需关注的学生，可以先看题目推进。"}
              </p>
            </div>
            <ButtonLink to={studentTargetHref} variant={studentTarget ? "primary" : "secondary"} disabled={!studentTarget} icon={<ArrowRight size={16} />}>
              查看学生
            </ButtonLink>
          </article>

          <article className={`teacher-priority-card ${lectureProblem ? "" : "is-muted"}`}>
            <span className="teacher-priority-card__icon">
              <Presentation size={18} />
            </span>
            <div>
              <p className="eyebrow">先讲题目</p>
              <h2>{lectureProblem ? lectureProblem.title : "等待提交"}</h2>
              <p>
                {lectureProblem
                  ? lectureIssue?.label
                    ? `${lectureProblem.attentionStudentCount || 0} 名需关注，主要错因是 ${issueLabel(lectureIssue.label)}。`
                    : `${lectureProblem.attentionStudentCount || 0} 名需关注，先看学生明细再决定讲评。`
                  : "学生提交后，这里会自动推荐最需要讲评的题目。"}
              </p>
            </div>
            <ButtonLink to={lectureHref} variant="secondary" disabled={!lectureProblem} icon={<ArrowRight size={16} />}>
              看题目
            </ButtonLink>
          </article>
        </section>

        <div className="teacher-drill-strip">
          <Metric label="提交人数" value={submittedText} />
          <Metric label="通过人数" value={currentTrend.passed || "-"} />
          <Metric label="总提交" value={overview.attemptCount} />
          <Metric label="需关注" value={overview.strugglingStudentCount} />
          <Metric label="最近提交" value={recentSubmissions || "-"} />
        </div>

        <article className="teacher-trend-panel">
          <div>
            <p className="eyebrow">提交推进</p>
            <h2>作业增长情况</h2>
          </div>
          {trend.length >= 2 ? (
            <div className="teacher-trend-chart" aria-label="提交推进曲线">
              <svg viewBox="0 0 100 100" preserveAspectRatio="none">
                <polyline className="trend-line trend-line--attempts" points={trendPath(trend, "submissionCount")} />
                <polyline className="trend-line trend-line--submitted" points={trendPath(trend, "submittedStudentCount")} />
                <polyline className="trend-line trend-line--passed" points={trendPath(trend, "passedStudentCount")} />
              </svg>
              <div className="teacher-trend-legend">
                <span>提交次数</span>
                <span>提交人数</span>
                <span>通过人数</span>
              </div>
            </div>
          ) : trend.length === 1 ? (
            <div className="teacher-trend-empty" aria-label="提交推进当前状态">
              <strong>已有 {currentTrend.attempts} 次提交，等待形成趋势</strong>
              <p>{currentTrend.submitted} 人提交，{currentTrend.passed} 人通过；再出现一次提交后会显示推进曲线。</p>
            </div>
          ) : (
            <EmptyState title="等待第一次提交" />
          )}
        </article>

        <section className="teacher-problem-table" aria-label="题目推进列表">
          <div className="teacher-section-head">
            <div>
              <p className="eyebrow">题目</p>
              <h2>题目推进</h2>
            </div>
          </div>
          <div className="teacher-problem-card-grid">
            {problems.map(problem => (
              <Link className="teacher-problem-card" to={`/app/teacher/assignment/${overview.assignment.id}/problems/${problem.problemId}`} key={problem.problemId}>
                <div className="teacher-problem-card__head">
                  <DifficultyPill difficulty={problem.difficulty} />
                  <strong>{problem.title}</strong>
                  <StatusPill tone={statusTone(problem.statusLabel)}>{problem.statusLabel || "待提交"}</StatusPill>
                </div>
                <div className="teacher-problem-card__facts">
                  <span>
                    <small>提交</small>
                    <b>{formatCountRate(problem.submissionRate)}</b>
                  </span>
                  <span>
                    <small>通过</small>
                    <b>{formatCountRate(problem.passRate)}</b>
                  </span>
                  <span>
                    <small>需看</small>
                    <b>{problem.attentionStudentCount || "-"}</b>
                  </span>
                </div>
                <p>{problem.topIssues?.[0]?.label ? issueLabel(problem.topIssues[0].label) : "暂无集中错因"}</p>
                <span className="teacher-card-action" aria-hidden="true">
                  <span>看题目</span>
                  <ArrowRight size={16} />
                </span>
              </Link>
            ))}
          </div>
        </section>
      </section>
    </>
  );
}

function ProblemOverviewView({ overview, problem }: { overview: AssignmentOverview; problem: ProblemSummary }) {
  const students = sortedProblemStudents(problem.students || []);
  const topIssue = problem.topIssues?.[0] || null;
  const topAbility = problem.abilityWeaknesses?.[0] || null;
  const hasStudentRows = students.length > 0;
  const hasSubmissions = problem.submissionCount > 0;
  const hasUnlinkedSubmissions = hasSubmissions && !hasStudentRows;
  const latestSubmittedAt = latestStudentSubmission(students);
  const firstAttentionStudent = students.find(student => student.needsAttention) || null;
  const actionTitle = hasUnlinkedSubmissions ? "先补学生关联" : !hasSubmissions ? "等待提交" : problem.statusLabel === "需讲评" ? "先讲这题" : "继续观察";
  const actionCopy = hasUnlinkedSubmissions
    ? "这道题已有提交证据，但没有可定位到学生的明细；先让提交关联到默认班级名单。"
    : !hasSubmissions
      ? "学生提交后，这里会自动形成错因、知识点和讲评建议。"
      : topIssue?.interventionSuggestion || topAbility?.abilityPoint || "等待更多学生提交后再形成讲评建议。";
  return (
    <>
      <section className="teacher-drill-header">
        <div>
          <Link to={`/app/teacher/assignment/${overview.assignment.id}`} className="back-link">
            <ArrowLeft size={16} /> 返回作业总览
          </Link>
          <h1>{problem.title}</h1>
          <div className="assignment-detail-meta">
            <span>{cleanAssignmentTitle(overview.assignment.title)}</span>
            <span>{problem.submittedStudentCount} 人提交</span>
            <span>{problem.submissionCount} 次提交</span>
            <span>{problem.statusLabel || "待提交"}</span>
          </div>
        </div>
      </section>

      <section className="teacher-problem-layout">
        <main className="teacher-problem-main">
          <div className="teacher-drill-strip">
            <Metric label="提交率" value={formatCountRate(problem.submissionRate)} />
            <Metric label="通过率" value={formatCountRate(problem.passRate)} />
            <Metric label="人均尝试" value={formatNumber(problem.averageAttempts)} />
            <Metric label="需关注" value={problem.attentionStudentCount || "-"} />
            <Metric label="最新提交" value={latestSubmittedAt ? formatDateTime(latestSubmittedAt) : hasSubmissions ? "有提交" : "-"} />
          </div>

          <section className="teacher-analysis-grid">
            <RankPanel title="高频错因" items={(problem.topIssues || []).map(item => ({ label: issueLabel(item.label), value: item.count, note: item.interventionSuggestion }))} />
            <RankPanel title="知识点薄弱" items={(problem.abilityWeaknesses || []).map(item => ({ label: item.abilityPoint, value: item.submissionCount, note: item.evidenceTags?.map(issueLabel).join("、") }))} />
            <RankPanel title="提示进度" items={(problem.hintLevelDistribution || []).map(item => ({ label: hintPolicyLabel(item.hintLevel), value: item.count }))} />
          </section>

          <section className="teacher-problem-table" aria-label="学生题目明细">
            <div className="teacher-section-head">
              <div>
                <p className="eyebrow">学生</p>
                <h2>这道题的学生情况</h2>
              </div>
            </div>
            <div className="teacher-table teacher-table--students">
              <div className="teacher-table-row teacher-table-row--head">
                <span>学生</span>
                <span>提交</span>
                <span>是否通过</span>
                <span>最新结果</span>
                <span>提示层级</span>
                <span>错因</span>
                <span>最近提交</span>
                <span></span>
              </div>
              {hasStudentRows ? (
                students.map(student => (
                  <Link
                    className="teacher-table-row teacher-table-row--link"
                    to={`/app/teacher/assignment/${overview.assignment.id}/problems/${problem.problemId}/students/${student.studentProfileId}`}
                    key={student.studentProfileId}
                  >
                    <span className="teacher-table-title">
                      <strong>{displayText(student.displayName, `学生 #${student.studentProfileId}`)}</strong>
                      {student.studentNo ? <small>{student.studentNo} 号</small> : null}
                    </span>
                    <span data-label="提交">{student.attemptCount} 次</span>
                    <span data-label="是否通过">{student.passedCount > 0 ? "通过" : "未通过"}</span>
                    <span data-label="最新结果"><VerdictPill verdict={student.latestVerdict} /></span>
                    <span data-label="提示层级">{student.latestHintLevel ? hintPolicyLabel(student.latestHintLevel) : "-"}</span>
                    <span data-label="错因">{problemStudentIssue(student)}</span>
                    <span data-label="最近提交">{student.latestSubmittedAt ? formatDateTime(student.latestSubmittedAt) : "-"}</span>
                    <span className="teacher-table-action">
                      {student.needsAttention ? <StatusPill tone="warning">关注</StatusPill> : <StatusPill tone="success">稳定</StatusPill>}
                      <ArrowRight size={16} />
                    </span>
                  </Link>
                ))
              ) : (
                <EmptyState
                  title={hasUnlinkedSubmissions ? "有提交记录，但未关联学生名单" : "暂无学生提交"}
                  description={
                    hasUnlinkedSubmissions
                      ? "开发数据里存在未绑定学生的提交；学生明细会在提交关联到名单后出现。"
                      : "学生提交后，这里会出现每个人在这道题上的提交、通过、错因和进入详情。"
                  }
                />
              )}
            </div>
          </section>
        </main>

        <aside className="teacher-action-rail">
          <p className="eyebrow">教师动作</p>
          <h2>{actionTitle}</h2>
          <p>{actionCopy}</p>
          {firstAttentionStudent ? (
            <ButtonLink
              to={`/app/teacher/assignment/${overview.assignment.id}/problems/${problem.problemId}/students/${firstAttentionStudent.studentProfileId}`}
              variant="primary"
              icon={<UserRound size={16} />}
            >
              查看优先学生
            </ButtonLink>
          ) : null}
          <ButtonLink to={`/app/teacher/assignment/${overview.assignment.id}`} variant="ghost" icon={<ChartNoAxesCombined size={16} />}>
            看作业推进
          </ButtonLink>
        </aside>
      </section>
    </>
  );
}

function StudentProblemView({
  overview,
  problem,
  student,
  correctionDraft,
  setCorrectionDraft,
  coarseDiagnosisTags,
  fineDiagnosisTags,
  saving,
  saveCorrection
}: {
  overview: AssignmentOverview;
  problem: ProblemSummary;
  student: ProblemStudent;
  correctionDraft: CorrectionDraft | null;
  setCorrectionDraft: (value: CorrectionDraft | null) => void;
  coarseDiagnosisTags: DiagnosisTag[];
  fineDiagnosisTags: DiagnosisTag[];
  saving: boolean;
  saveCorrection: (event: FormEvent) => void;
}) {
  const currentIssue = student.latestFineGrainedIssue || student.latestIssueTag ? issueLabel(student.latestFineGrainedIssue || student.latestIssueTag) : "等待更多证据";
  return (
    <>
      <section className="teacher-drill-header">
        <div>
          <Link to={`/app/teacher/assignment/${overview.assignment.id}/problems/${problem.problemId}`} className="back-link">
            <ArrowLeft size={16} /> 返回题目总览
          </Link>
          <h1>{displayText(student.displayName, "学生")}</h1>
          <div className="assignment-detail-meta">
            <span>{problem.title}</span>
            <span>{student.attemptCount} 次提交</span>
            <span>{studentPassRate(student)}% 通过</span>
            <span>{student.latestHintLevel ? hintPolicyLabel(student.latestHintLevel) : "提示待定"}</span>
          </div>
        </div>
      </section>

      <section className="teacher-student-problem-layout">
        <main className="teacher-student-evidence">
          <article className="teacher-evidence-card teacher-evidence-card--decision">
            <span>当前卡点</span>
            <strong>{currentIssue}</strong>
            <p>{student.latestIssue || student.latestProgressSignal || "这名学生在这道题上还没有形成明确错因。"}</p>
            <div className="teacher-decision-facts">
              <Metric label="提交次数" value={student.attemptCount} />
              <Metric label="通过次数" value={student.passedCount} />
              <Metric label="最新结果" value={verdictLabel(student.latestVerdict)} />
              <Metric label="提示层级" value={student.latestHintLevel ? hintPolicyLabel(student.latestHintLevel) : "-"} />
              <Metric label="最近提交" value={student.latestSubmittedAt ? formatDateTime(student.latestSubmittedAt) : "-"} />
            </div>
          </article>

          <div className="teacher-evidence-grid">
            <article className="teacher-evidence-card">
              <span>AI 判断</span>
              <strong>{confidenceLabel(student.latestConfidence)}</strong>
              <p>{student.latestProgressSignal || student.latestHintAction || "AI 还没有形成稳定判断。"}</p>
            </article>

            <article className="teacher-evidence-card">
              <span>错因标签</span>
              <strong>{currentIssue}</strong>
              <p>{displayText(student.abilityPoint, "知识点待观察")}</p>
            </article>

            <article className="teacher-evidence-card">
              <span>评测证据</span>
              <strong>{verdictLabel(student.latestVerdict)}</strong>
              <p>{student.latestSubmittedAt ? `${formatDateTime(student.latestSubmittedAt)} 的最近一次提交。` : "还没有可展示的提交时间。"}</p>
            </article>
          </div>
        </main>

        <aside className="teacher-action-rail teacher-action-rail--primary">
          <p className="eyebrow">教师动作</p>
          <h2>{student.needsAttention ? "优先处理" : "保持观察"}</h2>
          <p>{student.latestHintAction || student.latestProgressSignal || "让学生先补一次可见提交，再判断是否需要讲解。"}</p>
          <article className="teacher-action-note">
            <strong>给学生的下一步问题</strong>
            <p>{student.latestHintAction || `先让学生解释：这次结果为什么是${verdictLabel(student.latestVerdict)}？`}</p>
          </article>
          <Button
            type="button"
            variant="secondary"
            icon={<PenLine size={15} />}
            disabled={!student.latestSubmissionId}
            onClick={() =>
              student.latestSubmissionId &&
              setCorrectionDraft({
                submissionId: student.latestSubmissionId,
                correctedIssueTag: student.latestIssueTag || "",
                correctedFineGrainedTag: student.latestFineGrainedIssue || "",
                teacherNote: ""
              })
            }
          >
            校正 AI 错因
          </Button>
          {!student.latestSubmissionId ? <small className="teacher-action-hint">暂无可校正提交，先等待学生完成一次提交。</small> : null}

          {correctionDraft ? (
            <form className="teacher-correction-panel assignment-correction-panel" onSubmit={saveCorrection}>
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
              <Field label="校正理由">
                <TextArea
                  value={correctionDraft.teacherNote}
                  onChange={event => setCorrectionDraft({ ...correctionDraft, teacherNote: event.target.value })}
                  placeholder="例如：不是循环边界，而是输入读取理解错。"
                />
              </Field>
              <div className="actions">
                <Button type="submit" variant="primary" disabled={saving}>
                  保存
                </Button>
                <Button type="button" variant="ghost" onClick={() => setCorrectionDraft(null)}>
                  取消
                </Button>
              </div>
            </form>
          ) : null}
        </aside>
      </section>
    </>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="teacher-metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function RankPanel({ title, items }: { title: string; items: Array<{ label?: string | null; value: number; note?: string | null }> }) {
  return (
    <article className="teacher-rank-panel">
      <p className="eyebrow">{title}</p>
      {items.length ? (
        <div className="teacher-rank-list">
          {items.slice(0, 5).map((item, index) => (
            <div className="teacher-rank-row" key={`${title}-${item.label}-${index}`}>
              <span>{displayText(item.label, "待定")}</span>
              <strong>{item.value}</strong>
              {item.note ? <small>{item.note}</small> : null}
            </div>
          ))}
        </div>
      ) : (
        <p>暂无集中信号。</p>
      )}
    </article>
  );
}

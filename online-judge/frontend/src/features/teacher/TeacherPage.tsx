import { useEffect, useMemo, useState } from "react";
import { ArrowRight, Plus, UsersRound } from "lucide-react";
import { Link } from "react-router-dom";
import { ApiError, api } from "../../shared/api/client";
import type { Assignment, AssignmentOverview } from "../../shared/api/types";
import { assignmentStatusLabel, displayText, formatDateTime, issueLabel, looksCorruptText } from "../../shared/format";
import { ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };
type TeacherHomeAssignment = Assignment & { title: string; className: string };
type AttentionItem = {
  assignmentId: number;
  assignmentTitle: string;
  studentProfileId: number;
  displayName: string;
  problemId?: number | null;
  reason: string;
  submittedAt?: string | null;
};

function cleanAssignmentTitle(value?: string | null, fallback = "课堂作业") {
  const title = displayText(value, fallback);
  return title.includes("试点任务") ? "课堂编程作业" : title;
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
  return detail ? `${base}，${detail}` : fallback;
}

function attentionCount(overview?: AssignmentOverview | null) {
  return overview?.students.filter(student => student.needsAttention).length || 0;
}

function participantText(overview?: AssignmentOverview | null) {
  return overview ? `${overview.participantCount} 人` : "0 人";
}

function recentSubmissionDelta(overview?: AssignmentOverview | null) {
  const trend = overview?.progressTrend || [];
  if (trend.length >= 2) {
    const latest = trend[trend.length - 1];
    const previous = trend[trend.length - 2];
    return Math.max(0, latest.submissionCount - previous.submissionCount);
  }
  return trend[0]?.submissionCount ?? 0;
}

function attentionReason(student: AssignmentOverview["students"][number]) {
  const evidence = student.attentionEvidence?.[0];
  if (student.attentionReason) {
    return student.attentionReason;
  }
  if (evidence?.headline || evidence?.reason) {
    return evidence.headline || evidence.reason || "需要关注";
  }
  if (student.repeatedFineGrainedTag || student.latestFineGrainedIssue || student.repeatedIssueTag || student.latestIssueTag) {
    return issueLabel(student.repeatedFineGrainedTag || student.latestFineGrainedIssue || student.repeatedIssueTag || student.latestIssueTag);
  }
  return student.latestProgressSignal || "需要关注";
}

function buildAttentionItems(assignments: TeacherHomeAssignment[], overviews: Record<number, AssignmentOverview | null>) {
  return assignments.flatMap(assignment => {
    const overview = overviews[assignment.id];
    if (!overview) {
      return [];
    }
    return overview.students
      .filter(student => student.needsAttention)
      .map(student => {
        const evidence = student.attentionEvidence?.find(item => item.problemId) || student.attentionEvidence?.[0];
        return {
          assignmentId: assignment.id,
          assignmentTitle: assignment.title,
          studentProfileId: student.studentProfileId,
          displayName: displayText(student.displayName, `学生 #${student.studentProfileId}`),
          problemId: evidence?.problemId,
          reason: attentionReason(student),
          submittedAt: evidence?.submittedAt
        };
      });
  });
}

function attentionTarget(item: AttentionItem) {
  return item.problemId
    ? `/app/teacher/assignment/${item.assignmentId}/problems/${item.problemId}/students/${item.studentProfileId}`
    : `/app/teacher/assignment/${item.assignmentId}`;
}

function overviewTargets(assignments: Assignment[]) {
  const active = assignments.filter(assignment => assignment.status === "ACTIVE");
  return active.length ? active : assignments.filter(assignment => assignment.status !== "DRAFT");
}

export default function TeacherPage() {
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [overviewByAssignment, setOverviewByAssignment] = useState<Record<number, AssignmentOverview | null>>({});
  const [alert, setAlert] = useState<Alert | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    void loadTeacherHome();
  }, []);

  const cleanAssignments = useMemo(
    () =>
      assignments
        .filter(item => !looksCorruptText(item.title))
        .map(item => ({
          ...item,
          title: cleanAssignmentTitle(item.title, `课堂作业 #${item.id}`),
          className: displayText(item.className, "未绑定班级")
        })),
    [assignments]
  );
  const activeAssignments = useMemo(() => {
    const active = cleanAssignments.filter(item => item.status === "ACTIVE");
    return active.length ? active : cleanAssignments;
  }, [cleanAssignments]);
  const attentionItems = useMemo(() => buildAttentionItems(activeAssignments, overviewByAssignment), [activeAssignments, overviewByAssignment]);
  const teacherHomeSummary = useMemo(() => {
    const activeOverviews = activeAssignments.map(assignment => overviewByAssignment[assignment.id]).filter(Boolean) as AssignmentOverview[];
    const participantCount = activeOverviews.length ? Math.max(...activeOverviews.map(overview => overview.participantCount || 0)) : 0;
    const attentionStudentIds = new Set(attentionItems.map(item => item.studentProfileId));
    const recentSubmissions = activeAssignments.reduce((sum, assignment) => sum + recentSubmissionDelta(overviewByAssignment[assignment.id]), 0);
    return {
      className: activeAssignments[0]?.className || "默认班级",
      activeCount: activeAssignments.filter(item => item.status === "ACTIVE").length,
      participantCount,
      attentionCount: attentionStudentIds.size,
      recentSubmissions
    };
  }, [activeAssignments, attentionItems, overviewByAssignment]);
  const summaryTiles = [
    { label: "班级", value: teacherHomeSummary.className },
    { label: "作业", value: teacherHomeSummary.activeCount || "-" },
    { label: "学生", value: teacherHomeSummary.participantCount || "-" },
    { label: "需看", value: teacherHomeSummary.attentionCount, tone: teacherHomeSummary.attentionCount ? "warning" : "success" },
    { label: "新增", value: teacherHomeSummary.recentSubmissions || "-" }
  ];

  async function loadTeacherHome() {
    setLoading(true);
    setAlert(null);
    try {
      const assignmentResult = await api.assignments();
      setAssignments(assignmentResult);
      if (!assignmentResult.length) {
        setOverviewByAssignment({});
        return;
      }
      const overviewEntries = await Promise.all(
        overviewTargets(assignmentResult).map(async assignment => {
          try {
            return [assignment.id, await api.assignmentOverview(assignment.id)] as const;
          } catch {
            return [assignment.id, null] as const;
          }
        })
      );
      setOverviewByAssignment(Object.fromEntries(overviewEntries));
    } catch (error) {
      setAlert({ type: "error", message: teacherErrorMessage(error, "教师端数据读取失败。") });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="teacher-page teacher-workflow teacher-workflow-home">
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="teacher-workflow-header teacher-workflow-header--simple">
        <div>
          <p className="eyebrow">教师端</p>
          <h1>作业中心</h1>
          <p>只保留作业、需关注学生和必要入口。</p>
        </div>
        <div className="teacher-home-actions">
          <ButtonLink to="/app/teacher/classes" variant="secondary" icon={<UsersRound size={16} />}>
            班级进度
          </ButtonLink>
          <ButtonLink to="/app/teacher/assignment/new" variant="primary" icon={<Plus size={17} />}>
            新建作业
          </ButtonLink>
        </div>
      </section>

      <section className="teacher-home-summary-grid teacher-home-status-strip" aria-label="班级当前状态" aria-busy={loading}>
        {summaryTiles.map(tile => (
          <div className={`teacher-home-summary-tile ${tile.tone ? `teacher-home-summary-tile--${tile.tone}` : ""}`} key={tile.label}>
            <span>{tile.label}</span>
            <strong>{tile.value}</strong>
          </div>
        ))}
      </section>

      <section className="teacher-home-workbench">
        <div className="teacher-workflow-panel" aria-label="进行中作业" aria-busy={loading}>
          <div className="teacher-section-head teacher-section-head--compact">
            <div>
              <p className="eyebrow">作业</p>
              <h2>选择要看的作业</h2>
            </div>
          </div>
          {loading && !cleanAssignments.length ? (
            <EmptyState title="正在读取作业" live />
          ) : cleanAssignments.length ? (
            <div className="teacher-assignment-grid teacher-assignment-list" aria-label="教师作业入口">
              {activeAssignments.map(assignment => {
                const overview = overviewByAssignment[assignment.id];
                const taskCount = assignment.tasks?.length || 0;
                const count = attentionCount(overview);
                return (
                  <Link
                    className="teacher-assignment-card teacher-assignment-row--entry"
                    to={`/app/teacher/assignment/${assignment.id}`}
                    key={assignment.id}
                  >
                    <div className="teacher-assignment-card__head">
                      <strong>{assignment.title}</strong>
                      <StatusPill tone={count ? "warning" : assignment.status === "ACTIVE" ? "success" : "neutral"}>
                        {count ? `需看 ${count}` : assignmentStatusLabel(assignment.status)}
                      </StatusPill>
                    </div>
                    <div className="teacher-assignment-card__facts">
                      <span>
                        <small>题目</small>
                        <b>{taskCount}</b>
                      </span>
                      <span>
                        <small>学生</small>
                        <b>{participantText(overview).replace(" 人", "")}</b>
                      </span>
                      <span>
                        <small>状态</small>
                        <b>{assignmentStatusLabel(assignment.status)}</b>
                      </span>
                    </div>
                    <span className="teacher-card-action" aria-hidden="true">
                      <span>查看详情</span>
                      <ArrowRight size={17} />
                    </span>
                  </Link>
                );
              })}
            </div>
          ) : (
            <EmptyState title="暂无作业" description="新建作业后，这里会显示课堂入口和学生状态。" />
          )}
        </div>

        <aside className="teacher-attention-panel" aria-label="需关注摘要">
          <div className="teacher-section-head teacher-section-head--compact">
            <div>
              <p className="eyebrow">需关注</p>
              <h2>先看这些学生</h2>
            </div>
            <StatusPill tone={attentionItems.length ? "warning" : "success"}>{attentionItems.length || "稳定"}</StatusPill>
          </div>
          {attentionItems.length ? (
            <div className="teacher-attention-grid teacher-attention-list">
              {attentionItems.slice(0, 6).map(item => {
                const target = attentionTarget(item);
                return (
                  <Link className="teacher-attention-card" to={target} key={`${item.assignmentId}-${item.studentProfileId}-${item.problemId || "assignment"}`}>
                    <div>
                      <strong>{item.displayName}</strong>
                      <small>{item.assignmentTitle}</small>
                    </div>
                    <div>
                      <em>{item.reason}</em>
                      <small>{item.submittedAt ? formatDateTime(item.submittedAt) : "最近提交待定"}</small>
                    </div>
                    <ArrowRight size={16} aria-hidden="true" />
                  </Link>
                );
              })}
            </div>
          ) : (
            <EmptyState title="暂无需关注学生" description="有新的错因集中或反复卡点时会出现在这里。" />
          )}
        </aside>
      </section>

    </div>
  );
}

import { useEffect, useMemo, useState } from "react";
import { ArrowRight, Plus } from "lucide-react";
import { ApiError, api } from "../../shared/api/client";
import type { Assignment, AssignmentOverview, AssignmentStatus } from "../../shared/api/types";
import { assignmentStatusLabel, displayText, looksCorruptText } from "../../shared/format";
import { ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };
type StatusFilter = "ALL" | AssignmentStatus;

const STATUS_FILTERS: Array<{ value: StatusFilter; label: string }> = [
  { value: "ALL", label: "全部" },
  { value: "ACTIVE", label: "进行中" },
  { value: "DRAFT", label: "草稿" },
  { value: "CLOSED", label: "已结束" }
];

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

function passRate(overview?: AssignmentOverview | null) {
  return overview?.attemptCount ? Math.round((overview.passedAttemptCount / overview.attemptCount) * 100) : 0;
}

function attentionCount(overview?: AssignmentOverview | null) {
  return overview?.students.filter(student => student.needsAttention).length || 0;
}

function statusTone(status?: string | null) {
  if (status === "ACTIVE") {
    return "success";
  }
  if (status === "DRAFT") {
    return "neutral";
  }
  return "info";
}

export default function TeacherPage() {
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [overviewByAssignment, setOverviewByAssignment] = useState<Record<number, AssignmentOverview | null>>({});
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
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

  const filteredAssignments = useMemo(
    () => cleanAssignments.filter(item => statusFilter === "ALL" || item.status === statusFilter),
    [cleanAssignments, statusFilter]
  );

  const summary = useMemo(() => {
    const overviews = Object.values(overviewByAssignment).filter(Boolean) as AssignmentOverview[];
    const attemptCount = overviews.reduce((sum, overview) => sum + overview.attemptCount, 0);
    const passedCount = overviews.reduce((sum, overview) => sum + overview.passedAttemptCount, 0);
    return {
      total: cleanAssignments.length,
      active: cleanAssignments.filter(item => item.status === "ACTIVE").length,
      attention: overviews.reduce((sum, overview) => sum + attentionCount(overview), 0),
      passRate: attemptCount ? Math.round((passedCount / attemptCount) * 100) : 0
    };
  }, [cleanAssignments, overviewByAssignment]);

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
        assignmentResult.map(async assignment => {
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

      <section className="teacher-workflow-header">
        <div>
          <p className="eyebrow">教师端</p>
          <h1>作业中心</h1>
        </div>
        <ButtonLink to="/app/teacher/assignment/new" variant="primary" icon={<Plus size={17} />}>
          新建作业
        </ButtonLink>
      </section>

      <section className="teacher-workflow-summary" aria-label="作业概览">
        <div>
          <span>全部作业</span>
          <strong>{summary.total}</strong>
        </div>
        <div>
          <span>进行中</span>
          <strong>{summary.active}</strong>
        </div>
        <div>
          <span>平均通过率</span>
          <strong>{summary.passRate}%</strong>
        </div>
        <div>
          <span>需关注</span>
          <strong>{summary.attention}</strong>
        </div>
      </section>

      <section className="teacher-workflow-panel" aria-label="作业列表">
        <div className="teacher-workflow-panel__head">
          <div>
            <p className="eyebrow">管理作业</p>
            <h2>课堂作业</h2>
          </div>
          <div className="teacher-workflow-filters" aria-label="作业状态筛选">
            {STATUS_FILTERS.map(filter => (
              <button
                type="button"
                className={statusFilter === filter.value ? "is-active" : ""}
                key={filter.value}
                onClick={() => setStatusFilter(filter.value)}
              >
                {filter.label}
              </button>
            ))}
          </div>
        </div>

        {loading && !filteredAssignments.length ? (
          <EmptyState title="正在读取作业" />
        ) : filteredAssignments.length ? (
          <div className="teacher-assignment-table" role="table" aria-label="教师作业入口">
            <div className="teacher-assignment-table__head" role="row">
              <span role="columnheader">作业</span>
              <span role="columnheader">课堂</span>
              <span role="columnheader">题数</span>
              <span role="columnheader">参与</span>
              <span role="columnheader">提交</span>
              <span role="columnheader">通过率</span>
              <span role="columnheader">需关注</span>
              <span role="columnheader">操作</span>
            </div>
            {filteredAssignments.map(assignment => {
              const overview = overviewByAssignment[assignment.id];
              const taskCount = assignment.tasks?.length || 0;
              const count = attentionCount(overview);
              return (
                <article className="teacher-assignment-row" role="row" key={assignment.id}>
                  <div className="teacher-assignment-row__title" role="cell">
                    <StatusPill tone={statusTone(assignment.status)}>{assignmentStatusLabel(assignment.status)}</StatusPill>
                    <strong>{assignment.title}</strong>
                  </div>
                  <span role="cell">{assignment.className}</span>
                  <span role="cell">{taskCount} 题</span>
                  <span role="cell">{overview?.participantCount ?? "-"}</span>
                  <span role="cell">{overview?.attemptCount ?? "-"}</span>
                  <span role="cell">{overview ? `${passRate(overview)}%` : "-"}</span>
                  <span role="cell" className={count ? "is-warning" : ""}>
                    {overview ? count : "-"}
                  </span>
                  <div role="cell" className="teacher-assignment-row__action">
                    <ButtonLink to={`/app/teacher/assignment/${assignment.id}`} variant="primary" icon={<ArrowRight size={17} />}>
                      进入作业
                    </ButtonLink>
                  </div>
                </article>
              );
            })}
          </div>
        ) : (
          <EmptyState title="没有符合条件的作业" description="切换筛选条件，或新建一份课堂作业。" />
        )}
      </section>
    </div>
  );
}

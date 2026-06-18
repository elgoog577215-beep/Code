import { useEffect, useMemo, useState } from "react";
import { ArrowRight, Plus } from "lucide-react";
import { useLocation } from "react-router-dom";
import { ApiError, api } from "../../shared/api/client";
import type { Assignment, AssignmentOverview } from "../../shared/api/types";
import { assignmentStatusLabel, displayText, looksCorruptText } from "../../shared/format";
import { ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { TeacherManagementTools } from "./TeacherManagementPage";

type Alert = { type: "success" | "error"; message: string };

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

export default function TeacherPage() {
  const location = useLocation();
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [overviewByAssignment, setOverviewByAssignment] = useState<Record<number, AssignmentOverview | null>>({});
  const [alert, setAlert] = useState<Alert | null>(null);
  const [loading, setLoading] = useState(true);
  const showManagement = new URLSearchParams(location.search).get("manage") === "1";
  const [managementOpen, setManagementOpen] = useState(showManagement);

  useEffect(() => {
    void loadTeacherHome();
  }, []);

  useEffect(() => {
    if (showManagement) {
      setManagementOpen(true);
    }
  }, [showManagement]);

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

      <section className="teacher-workflow-header teacher-workflow-header--simple">
        <div>
          <p className="eyebrow">教师端</p>
          <h1>作业</h1>
        </div>
        <ButtonLink to="/app/teacher/assignment/new" variant="primary" icon={<Plus size={17} />}>
          新建作业
        </ButtonLink>
      </section>

      <section className="teacher-workflow-panel" aria-label="作业列表">
        {loading && !cleanAssignments.length ? (
          <EmptyState title="正在读取作业" />
        ) : cleanAssignments.length ? (
          <div className="teacher-assignment-list" aria-label="教师作业入口">
            {cleanAssignments.map(assignment => {
              const overview = overviewByAssignment[assignment.id];
              const taskCount = assignment.tasks?.length || 0;
              const count = attentionCount(overview);
              return (
                <article className="teacher-assignment-row teacher-assignment-row--simple" key={assignment.id}>
                  <div className="teacher-assignment-row__title">
                    <strong>{assignment.title}</strong>
                    <small className="teacher-assignment-row__meta">
                      {assignment.className} · {taskCount} 题 · {assignmentStatusLabel(assignment.status)} · {overview ? `${passRate(overview)}% 通过` : "暂无提交"} ·{" "}
                      {overview ? `${count} 关注` : "0 关注"}
                    </small>
                  </div>
                  <div className="teacher-assignment-row__action">
                    <ButtonLink to={`/app/teacher/assignment/${assignment.id}`} variant="primary" icon={<ArrowRight size={17} />}>
                      查看
                    </ButtonLink>
                  </div>
                </article>
              );
            })}
          </div>
        ) : (
          <EmptyState title="暂无作业" />
        )}
      </section>

      <details
        className="teacher-management-drawer"
        open={managementOpen}
        onToggle={event => setManagementOpen(event.currentTarget.open)}
      >
        <summary>
          <span>
            <strong>更多管理</strong>
            <small>班级名单、题目导入、AI 标准库</small>
          </span>
        </summary>
        <TeacherManagementTools embedded />
      </details>
    </div>
  );
}

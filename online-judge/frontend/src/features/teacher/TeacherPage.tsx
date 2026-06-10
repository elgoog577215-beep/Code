import { useEffect, useMemo, useState } from "react";
import { ArrowRight, Plus } from "lucide-react";
import { ApiError, api } from "../../shared/api/client";
import type { Assignment, AssignmentOverview } from "../../shared/api/types";
import { assignmentStatusLabel, displayText, looksCorruptText } from "../../shared/format";
import { ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };

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
  return detail ? `${base}，${detail}` : fallback;
}

function passRate(overview?: AssignmentOverview | null) {
  return overview?.attemptCount ? Math.round((overview.passedAttemptCount / overview.attemptCount) * 100) : 0;
}

function attentionCount(overview?: AssignmentOverview | null) {
  return overview?.students.filter(student => student.needsAttention).length || 0;
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
          description: cleanAssignmentDescription(item.description),
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
    <div className="teacher-page teacher-assignment-center">
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="teacher-home-command">
        <div>
          <p className="eyebrow">教师端</p>
          <h1>作业中心</h1>
        </div>
        <div className="teacher-home-actions">
          <ButtonLink to="/app/teacher/assignment/new" variant="primary" icon={<Plus size={17} />}>
            新建作业
          </ButtonLink>
        </div>
      </section>

      <nav className="teacher-assignment-list" aria-label="教师作业入口">
        {loading && !cleanAssignments.length ? (
          <EmptyState title="正在读取作业" />
        ) : cleanAssignments.length ? (
          cleanAssignments.map(assignment => {
            const overview = overviewByAssignment[assignment.id];
            const taskCount = assignment.tasks?.length || 0;
            const count = attentionCount(overview);
            return (
              <article className="teacher-assignment-card" key={assignment.id}>
                <div className="teacher-assignment-card__main">
                  <div className="teacher-assignment-card__title">
                    <StatusPill tone={assignment.status === "ACTIVE" ? "success" : assignment.status === "DRAFT" ? "neutral" : "info"}>
                      {assignmentStatusLabel(assignment.status)}
                    </StatusPill>
                    <h2>{assignment.title}</h2>
                  </div>
                  <div className="teacher-assignment-card__meta">
                    <span>{assignment.className}</span>
                    <span>{taskCount} 题</span>
                  </div>
                </div>
                <div className="teacher-assignment-card__stats" aria-label={`${assignment.title} 作业摘要`}>
                  <div>
                    <span>参与</span>
                    <strong>{overview?.participantCount ?? "-"}</strong>
                  </div>
                  <div>
                    <span>提交</span>
                    <strong>{overview?.attemptCount ?? "-"}</strong>
                  </div>
                  <div>
                    <span>通过率</span>
                    <strong>{overview ? `${passRate(overview)}%` : "-"}</strong>
                  </div>
                  <div className={count ? "is-warning" : ""}>
                    <span>需关注</span>
                    <strong>{overview ? count : "-"}</strong>
                  </div>
                </div>
                <div className="teacher-assignment-card__actions">
                  <ButtonLink to={`/app/teacher/assignment/${assignment.id}`} variant="primary" icon={<ArrowRight size={17} />}>
                    进入作业
                  </ButtonLink>
                </div>
              </article>
            );
          })
        ) : (
          <EmptyState title="还没有作业" description="先选择班级和题目，发布第一份课堂作业。" />
        )}
      </nav>
    </div>
  );
}

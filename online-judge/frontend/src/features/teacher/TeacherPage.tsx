import { useEffect, useMemo, useState } from "react";
import { ArrowRight, Plus, UsersRound } from "lucide-react";
import { Link } from "react-router-dom";
import { ApiError, api } from "../../shared/api/client";
import type { Assignment, AssignmentOverview } from "../../shared/api/types";
import { displayText, looksCorruptText } from "../../shared/format";
import { useTranslation } from "../../shared/i18n";
import { ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { StatusPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };
type TeacherHomeAssignment = Assignment & { title: string; className: string };
type Translator = (key: string, params?: Record<string, string | number>) => string;
type TeacherHomeErrorCopy = { serviceUnavailable: string; notFound: string; separator: string };

function cleanAssignmentTitle(value: string | null | undefined, fallback: string, pilotTitle: string) {
  const title = displayText(value, fallback);
  return title.includes("试点任务") ? pilotTitle : title;
}

function teacherErrorMessage(error: unknown, fallback: string, copy: TeacherHomeErrorCopy) {
  const base = fallback.replace(/[。.!！?？\s]+$/, "");
  if (error instanceof ApiError) {
    if (error.status >= 500) {
      return copy.serviceUnavailable;
    }
    if (error.status === 404) {
      return copy.notFound;
    }
    return `${base}${copy.separator}${error.message}`;
  }
  const detail = error instanceof Error ? error.message : "";
  return detail ? `${base}${copy.separator}${detail}` : fallback;
}

function attentionCount(overview?: AssignmentOverview | null) {
  return overview?.students.filter(student => student.needsAttention).length || 0;
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

function overviewTargets(assignments: Assignment[]) {
  const active = assignments.filter(assignment => assignment.status === "ACTIVE");
  return active.length ? active : assignments.filter(assignment => assignment.status !== "DRAFT");
}

function assignmentStatusText(status: string | null | undefined, t: Translator) {
  switch ((status || "").toUpperCase()) {
    case "ACTIVE":
      return t("assignmentDetail.status.active");
    case "DRAFT":
      return t("assignmentDetail.status.draft");
    case "CLOSED":
      return t("assignmentDetail.status.closed");
    default:
      return status || t("assignmentDetail.status.unset");
  }
}

export default function TeacherPage() {
  const { t } = useTranslation();
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
          title: cleanAssignmentTitle(item.title, t("teacherHome.assignmentFallbackWithId", { id: item.id }), t("teacherHome.pilotTitle")),
          className: displayText(item.className, t("teacherHome.unboundClass"))
        })),
    [assignments, t]
  );
  const activeAssignments = useMemo(() => {
    const active = cleanAssignments.filter(item => item.status === "ACTIVE");
    return active.length ? active : cleanAssignments;
  }, [cleanAssignments]);
  const teacherHomeSummary = useMemo(() => {
    const activeOverviews = activeAssignments.map(assignment => overviewByAssignment[assignment.id]).filter(Boolean) as AssignmentOverview[];
    const participantCount = activeOverviews.length ? Math.max(...activeOverviews.map(overview => overview.participantCount || 0)) : 0;
    const attentionStudentIds = new Set<number>();
    activeOverviews.forEach(overview => {
      overview.students.forEach(student => {
        if (student.needsAttention) {
          attentionStudentIds.add(student.studentProfileId);
        }
      });
    });
    const recentSubmissions = activeAssignments.reduce((sum, assignment) => sum + recentSubmissionDelta(overviewByAssignment[assignment.id]), 0);
    return {
      className: cleanAssignments[0]?.className || t("teacherHome.defaultClass"),
      assignmentCount: cleanAssignments.length,
      participantCount,
      attentionCount: attentionStudentIds.size,
      recentSubmissions
    };
  }, [activeAssignments, cleanAssignments, overviewByAssignment, t]);
  const summaryTiles = [
    { key: "assignments", label: t("teacherHome.metrics.assignments"), value: teacherHomeSummary.assignmentCount || "-" },
    { key: "students", label: t("teacherHome.metrics.students"), value: teacherHomeSummary.participantCount || "-" },
    {
      key: "attention",
      label: t("teacherHome.metrics.needsAttention"),
      value: teacherHomeSummary.attentionCount,
      tone: teacherHomeSummary.attentionCount ? "warning" : "success"
    },
    { key: "recent", label: t("teacherHome.metrics.recentSubmissions"), value: teacherHomeSummary.recentSubmissions || "-" }
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
      setAlert({
        type: "error",
        message: teacherErrorMessage(error, t("teacherHome.errors.loadFailed"), {
          serviceUnavailable: t("teacherHome.errors.serviceUnavailable"),
          notFound: t("teacherHome.errors.notFound"),
          separator: t("teacherHome.errors.separator")
        })
      });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="teacher-page teacher-workflow teacher-workflow-home">
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="teacher-home-overview" aria-label={t("teacherHome.classOverview")} aria-busy={loading}>
        <div className="teacher-home-overview__class">
          <span>{t("teacherHome.classLabel")}</span>
          <strong>{teacherHomeSummary.className}</strong>
        </div>
        <div className="teacher-home-summary-grid teacher-home-status-strip">
          {summaryTiles.map(tile => (
            <div className={`teacher-home-summary-tile ${tile.tone ? `teacher-home-summary-tile--${tile.tone}` : ""}`} key={tile.key}>
              <span>{tile.label}</span>
              <strong>{tile.value}</strong>
            </div>
          ))}
        </div>
        <div className="teacher-home-actions">
          <ButtonLink to="/app/teacher/classes" variant="secondary" icon={<UsersRound size={16} />}>
            {t("teacherHome.classProgress")}
          </ButtonLink>
          <ButtonLink to="/app/teacher/assignment/new" variant="primary" icon={<Plus size={17} />}>
            {t("teacherHome.newAssignment")}
          </ButtonLink>
        </div>
      </section>

      <section className="teacher-home-workbench teacher-home-assignment-list" aria-label={t("teacherHome.assignmentListAria")} aria-busy={loading}>
        <div className="teacher-home-list-head">
          <h2>{t("teacherHome.assignmentListTitle")}</h2>
          <span>{t("teacherHome.assignmentCount", { count: activeAssignments.length })}</span>
        </div>
        {loading && !cleanAssignments.length ? (
          <EmptyState title={t("teacherHome.loadingAssignments")} live />
        ) : cleanAssignments.length ? (
          <div className="teacher-assignment-grid teacher-assignment-list teacher-assignment-grid--flat" aria-label={t("teacherHome.assignmentListAria")}>
            {activeAssignments.map(assignment => {
              const overview = overviewByAssignment[assignment.id];
              const taskCount = assignment.tasks?.length || 0;
              const count = attentionCount(overview);
              const status = assignmentStatusText(assignment.status, t);
              return (
                <Link
                  className="teacher-assignment-card teacher-assignment-card--flat teacher-assignment-row--entry"
                  to={`/app/teacher/assignment/${assignment.id}`}
                  key={assignment.id}
                >
                  <div className="teacher-assignment-card__head">
                    <strong>{assignment.title}</strong>
                    <StatusPill tone={count ? "warning" : assignment.status === "ACTIVE" ? "success" : "neutral"}>
                      {count ? t("teacherHome.needsAttentionWithCount", { count }) : status}
                    </StatusPill>
                  </div>
                  <div className="teacher-assignment-card__facts">
                    <span>
                      <small>{t("teacherHome.metrics.problems")}</small>
                      <b>{taskCount}</b>
                    </span>
                    <span>
                      <small>{t("teacherHome.metrics.students")}</small>
                      <b>{overview ? overview.participantCount : "-"}</b>
                    </span>
                    <span>
                      <small>{t("teacherHome.metrics.status")}</small>
                      <b>{status}</b>
                    </span>
                  </div>
                  <span className="teacher-card-action" aria-hidden="true">
                    <span>{t("teacherHome.viewDetails")}</span>
                    <ArrowRight size={17} />
                  </span>
                </Link>
              );
            })}
          </div>
        ) : (
          <EmptyState title={t("teacherHome.emptyAssignmentsTitle")} description={t("teacherHome.emptyAssignmentsDescription")} />
        )}
      </section>
    </div>
  );
}

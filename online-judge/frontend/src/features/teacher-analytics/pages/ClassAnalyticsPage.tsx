import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { ApiError, api } from "../../../shared/api/client";
import type { Assignment, AssignmentOverview, ClassGroup } from "../../../shared/api/types";
import { useTranslation } from "../../../shared/i18n";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { AnalyticsBreadcrumbs } from "../components/AnalyticsBreadcrumbs";
import { AnalyticsDashboard } from "../components/AnalyticsDashboard";
import { buildClassAnalyticsSnapshot, classAssignments, findClass } from "../selectors";

export default function ClassAnalyticsPage() {
  const { t } = useTranslation();
  const { classId = "" } = useParams();
  const classIdNumber = Number(classId);
  const [classes, setClasses] = useState<ClassGroup[]>([]);
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [overviewByAssignment, setOverviewByAssignment] = useState<Record<number, AssignmentOverview | null>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void loadClassAnalytics();
  }, [classIdNumber, t]);

  async function loadClassAnalytics() {
    if (!classIdNumber) {
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [classResult, assignmentResult] = await Promise.all([api.classes(), api.assignments()]);
      const currentClass = findClass(classResult, classIdNumber);
      const scopedAssignments = currentClass ? classAssignments(assignmentResult, currentClass.id, currentClass.name, t) : [];
      const overviewEntries = await Promise.all(
        scopedAssignments.map(async assignment => {
          try {
            return [assignment.id, await api.assignmentOverview(assignment.id)] as const;
          } catch {
            return [assignment.id, null] as const;
          }
        })
      );
      setClasses(classResult);
      setAssignments(assignmentResult);
      setOverviewByAssignment(Object.fromEntries(overviewEntries));
    } catch (currentError) {
      setError(currentError instanceof ApiError || currentError instanceof Error ? currentError.message : t("teacherAnalytics.errors.load"));
    } finally {
      setLoading(false);
    }
  }

  const snapshot = useMemo(() => {
    const currentClass = findClass(classes, classIdNumber);
    return currentClass ? buildClassAnalyticsSnapshot({ classGroup: currentClass, assignments, overviewByAssignment, t }) : null;
  }, [assignments, classIdNumber, classes, overviewByAssignment, t]);

  if (loading && !snapshot) {
    return <EmptyState title={t("teacherAnalytics.loading.class")} live />;
  }

  if (!snapshot) {
    return <EmptyState title={t("teacherAnalytics.empty.classNotFound")} description={error || t("teacherAnalytics.empty.classNotFoundDescription")} />;
  }

  return (
    <div className="teacher-analytics-page">
      <AnalyticsBreadcrumbs items={[{ label: t("teacherAnalytics.breadcrumb.classes"), to: "/app/teacher/classes" }, { label: snapshot.scope.className }]} />
      <section className="teacher-analytics-hero">
        <div>
          <span>{t("teacherAnalytics.scope.class")}</span>
          <h1>{snapshot.scope.className}</h1>
          <p>{t("teacherAnalytics.class.description")}</p>
        </div>
      </section>
      {error ? <div className="alert alert--error">{error}</div> : null}
      <AnalyticsDashboard snapshot={snapshot} t={t} />
    </div>
  );
}

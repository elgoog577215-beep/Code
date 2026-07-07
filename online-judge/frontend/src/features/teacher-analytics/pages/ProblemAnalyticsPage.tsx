import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { ApiError, api } from "../../../shared/api/client";
import type { Assignment, AssignmentOverview, ClassGroup, DiagnosisTag } from "../../../shared/api/types";
import { useTranslation } from "../../../shared/i18n";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { AnalyticsBreadcrumbs } from "../components/AnalyticsBreadcrumbs";
import { AnalyticsDashboard } from "../components/AnalyticsDashboard";
import { buildProblemAnalyticsSnapshot, findAssignment, findClass } from "../selectors";
import type { AnalyticsEvidenceSample } from "../model";

export default function ProblemAnalyticsPage() {
  const { t } = useTranslation();
  const { classId = "", assignmentId = "", problemId = "" } = useParams();
  const classIdNumber = Number(classId);
  const assignmentIdNumber = Number(assignmentId);
  const problemIdNumber = Number(problemId);
  const [classes, setClasses] = useState<ClassGroup[]>([]);
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [overview, setOverview] = useState<AssignmentOverview | null>(null);
  const [diagnosisTags, setDiagnosisTags] = useState<DiagnosisTag[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void loadProblemAnalytics();
  }, [classIdNumber, assignmentIdNumber, problemIdNumber]);

  async function loadProblemAnalytics() {
    setLoading(true);
    setError(null);
    try {
      const [classResult, assignmentResult, overviewResult, tagsResult] = await Promise.all([
        api.classes(),
        api.assignments(),
        api.assignmentOverview(assignmentIdNumber),
        api.diagnosisTags()
      ]);
      setClasses(classResult);
      setAssignments(assignmentResult);
      setOverview(overviewResult);
      setDiagnosisTags(tagsResult);
    } catch (currentError) {
      setError(currentError instanceof ApiError || currentError instanceof Error ? currentError.message : t("teacherAnalytics.errors.load"));
    } finally {
      setLoading(false);
    }
  }

  async function saveCorrection(
    sample: AnalyticsEvidenceSample,
    payload: { correctedIssueTag: string; correctedFineGrainedTag: string; teacherNote: string }
  ) {
    if (!sample.submissionId || !snapshot?.scope.assignmentId) {
      return;
    }
    await api.correctDiagnosis(snapshot.scope.assignmentId, {
      submissionId: sample.submissionId,
      correctedIssueTag: payload.correctedIssueTag,
      correctedFineGrainedTag: payload.correctedFineGrainedTag || null,
      teacherNote: payload.teacherNote,
      evalCandidate: true,
      correctedBy: "teacher"
    });
    await loadProblemAnalytics();
  }

  const snapshot = useMemo(() => {
    const assignment = findAssignment(assignments, assignmentIdNumber, t);
    const currentClass =
      findClass(classes, classIdNumber) ||
      (assignment?.classGroupId ? findClass(classes, assignment.classGroupId) : null) ||
      (assignment
        ? { id: assignment.classGroupId || classIdNumber || 0, name: assignment.className, grade: null, teacherName: null }
        : null);
    return currentClass && assignment && overview
      ? buildProblemAnalyticsSnapshot({ classGroup: currentClass, assignment, overview, problemId: problemIdNumber, t })
      : null;
  }, [assignmentIdNumber, assignments, classIdNumber, classes, overview, problemIdNumber, t]);

  if (loading && !snapshot) {
    return <EmptyState title={t("teacherAnalytics.loading.problem")} live />;
  }

  if (!snapshot) {
    return <EmptyState title={t("teacherAnalytics.empty.problemNotFound")} description={error || t("teacherAnalytics.empty.problemNotFoundDescription")} />;
  }

  return (
    <div className="teacher-analytics-page">
      <AnalyticsBreadcrumbs
        items={[
          { label: t("teacherAnalytics.breadcrumb.classes"), to: "/app/teacher/classes" },
          { label: snapshot.scope.className, to: `/app/teacher/classes/${snapshot.scope.classId}` },
          {
            label: snapshot.scope.assignmentTitle || t("teacherAnalytics.scope.assignment"),
            to: `/app/teacher/classes/${snapshot.scope.classId}/assignments/${snapshot.scope.assignmentId}`
          },
          { label: snapshot.scope.problemTitle || t("teacherAnalytics.scope.problem") }
        ]}
      />
      <section className="teacher-analytics-hero">
        <div>
          <span>{t("teacherAnalytics.scope.problem")}</span>
          <h1>{snapshot.scope.problemTitle}</h1>
          <p>{t("teacherAnalytics.problem.description")}</p>
        </div>
      </section>
      {error ? <div className="alert alert--error">{error}</div> : null}
      <AnalyticsDashboard snapshot={snapshot} t={t} correction={{ tags: diagnosisTags, onSubmit: saveCorrection }} />
    </div>
  );
}

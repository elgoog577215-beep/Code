import { useMemo, useState } from "react";
import { ArrowRight, CheckCircle2, CircleDot, Repeat2 } from "lucide-react";
import { Link } from "react-router-dom";
import type { AssignmentOverview, SubmissionGrowthSummary } from "../../../shared/api/types";
import type { AnalyticsSnapshot, InsightBucket } from "../model";
import { AnalyticsSummaryCards } from "./AnalyticsSummaryCards";

type Props = {
  snapshot: AnalyticsSnapshot;
  t: (key: string, params?: Record<string, string | number>) => string;
};

type ProblemStudent = NonNullable<NonNullable<AssignmentOverview["problemSummaries"]>[number]["students"]>[number];
type StudentGroup = "affected" | "repeated" | "resolved";

export function AnalyticsDashboard({ snapshot, t }: Props) {
  return (
    <div className="teacher-analytics-dashboard teacher-analytics-dashboard--focused">
      <AnalyticsSummaryCards metrics={snapshot.metrics} labelFor={t} />
      {snapshot.scope.type === "class" ? <ClassAssignments snapshot={snapshot} t={t} /> : null}
      {snapshot.scope.type === "assignment" ? <AssignmentProblems snapshot={snapshot} t={t} /> : null}
      {snapshot.scope.type === "problem" ? <ProblemEvidence snapshot={snapshot} t={t} /> : null}
    </div>
  );
}

function ClassAssignments({ snapshot, t }: Props) {
  return (
    <section className="teacher-analytics-focus-panel">
      <SectionTitle step="1" title={t("teacherAnalytics.focus.assignmentList")} description={t("teacherAnalytics.focus.assignmentListDescription")} />
      <div className="teacher-analytics-focus-list">
        {snapshot.assignmentRows.map(row => (
          <Link className="teacher-analytics-focus-row" to={row.href} key={row.id}>
            <div>
              <strong>{row.title}</strong>
              <small>{row.status} · {t("teacherAnalytics.focus.problemCount", { count: row.problemCount })}</small>
            </div>
            <Metric label={t("teacherAnalytics.focus.submitted")} value={`${row.submittedStudentCount}/${row.participantCount || "-"}`} />
            <Metric label={t("teacherAnalytics.focus.completedRequired")} value={row.completedRequiredStudentCount} />
            <ArrowRight size={18} aria-hidden="true" />
          </Link>
        ))}
      </div>
    </section>
  );
}

function AssignmentProblems({ snapshot, t }: Props) {
  return (
    <section className="teacher-analytics-focus-panel">
      <SectionTitle step="2" title={t("teacherAnalytics.focus.problemList")} description={t("teacherAnalytics.focus.problemListDescription")} />
      <div className="teacher-analytics-focus-list">
        {snapshot.problemRows.map(row => (
          <Link className="teacher-analytics-focus-row teacher-analytics-focus-row--problem" to={row.href} key={row.id}>
            <div>
              <strong>{row.title}</strong>
              <small>{t("teacherAnalytics.focus.submittedOfRoster", { submitted: row.submittedStudentCount, roster: row.participantCount || "-" })}</small>
            </div>
            <Metric label={t("teacherAnalytics.focus.firstPass")} value={row.firstPassStudentCount} />
            <Metric label={t("teacherAnalytics.focus.eventualPass")} value={row.passedStudentCount} />
            <Metric label={t("teacherAnalytics.focus.medianEffective")} value={row.medianEffectiveAttempts ?? "-"} />
            <ArrowRight size={18} aria-hidden="true" />
          </Link>
        ))}
      </div>
    </section>
  );
}

function ProblemEvidence({ snapshot, t }: Props) {
  const problem = snapshot.overview?.problemSummaries?.find(item => item.problemId === snapshot.scope.problemId);
  const issues = useMemo(
    () => [...(snapshot.insightBuckets.mistakePoint || [])]
      .sort((left, right) => (right.affectedStudentCount || 0) - (left.affectedStudentCount || 0) || (right.repeatedStudentCount || 0) - (left.repeatedStudentCount || 0)),
    [snapshot.insightBuckets.mistakePoint]
  );
  const [selectedIssueId, setSelectedIssueId] = useState<string | null>(null);
  const [studentGroup, setStudentGroup] = useState<StudentGroup>("affected");
  const selectedIssue = issues.find(item => item.id === selectedIssueId) || null;
  const studentIds = selectedIssue ? idsForGroup(selectedIssue, studentGroup) : [];
  const students = (problem?.students || []).filter(student => !selectedIssue || studentIds.includes(student.studentProfileId));

  return (
    <div className="teacher-problem-evidence-layout">
      <section className="teacher-analytics-focus-panel">
        <SectionTitle step="3" title={t("teacherAnalytics.focus.issueOverview")} description={t("teacherAnalytics.focus.issueOverviewDescription")} />
        {issues.length ? (
          <div className="teacher-issue-overview-list">
            {issues.map(issue => (
              <article className={selectedIssue?.id === issue.id ? "teacher-issue-overview is-selected" : "teacher-issue-overview"} key={issue.id}>
                <button
                  type="button"
                  className="teacher-issue-overview__title"
                  onClick={() => setSelectedIssueId(selectedIssue?.id === issue.id ? null : issue.id)}
                  aria-expanded={selectedIssue?.id === issue.id}
                >
                  <span>{issue.label}</span>
                  <small>{t(`teacherAnalytics.pattern.${patternKey(issue, problem?.submittedStudentCount || 0)}`)}</small>
                </button>
                <div className="teacher-issue-counts" aria-label={t("teacherAnalytics.focus.issueCountsAria")}>
                  <IssueCount
                    icon={CircleDot}
                    label={t("teacherAnalytics.focus.affected")}
                    value={issue.affectedStudentCount || 0}
                    active={selectedIssue?.id === issue.id && studentGroup === "affected"}
                    onClick={() => selectIssue(issue.id, "affected", setSelectedIssueId, setStudentGroup)}
                  />
                  <IssueCount
                    icon={Repeat2}
                    label={t("teacherAnalytics.focus.repeated")}
                    value={issue.repeatedStudentCount || 0}
                    active={selectedIssue?.id === issue.id && studentGroup === "repeated"}
                    onClick={() => selectIssue(issue.id, "repeated", setSelectedIssueId, setStudentGroup)}
                  />
                  <IssueCount
                    icon={CheckCircle2}
                    label={t("teacherAnalytics.focus.resolved")}
                    value={issue.resolvedStudentCount || 0}
                    active={selectedIssue?.id === issue.id && studentGroup === "resolved"}
                    onClick={() => selectIssue(issue.id, "resolved", setSelectedIssueId, setStudentGroup)}
                  />
                </div>
              </article>
            ))}
          </div>
        ) : (
          <p className="teacher-analytics-empty-copy">{t("teacherAnalytics.empty.noNormalizedIssues")}</p>
        )}
      </section>

      <section className="teacher-analytics-focus-panel">
        <SectionTitle
          step="4"
          title={selectedIssue ? t("teacherAnalytics.focus.filteredStudents", { issue: selectedIssue.label }) : t("teacherAnalytics.focus.studentList")}
          description={selectedIssue ? t(`teacherAnalytics.focus.groupDescription.${studentGroup}`) : t("teacherAnalytics.focus.studentListDescription")}
        />
        <div className="teacher-student-growth-list">
          {students.length ? students.map(student => (
            <StudentGrowthRow student={student} snapshot={snapshot} t={t} key={student.studentProfileId} />
          )) : <p className="teacher-analytics-empty-copy">{t("teacherAnalytics.empty.noMatchingStudents")}</p>}
        </div>
      </section>
    </div>
  );
}

function StudentGrowthRow({ student, snapshot, t }: { student: ProblemStudent; snapshot: AnalyticsSnapshot; t: Props["t"] }) {
  const href = `/teacher/classes/${snapshot.scope.classId}/assignments/${snapshot.scope.assignmentId}/problems/${snapshot.scope.problemId}/students/${student.studentProfileId}`;
  const growth = student.latestGrowthSummary;
  const issueSignals = (growth?.issueSignals || [])
    .filter(item => ["PERSISTED", "NEW", "RECURRED"].includes(String(item.changeStatus || "").toUpperCase()))
    .slice(0, 2);
  return (
    <Link className="teacher-student-growth-row" to={href}>
      <div className="teacher-student-growth-row__identity">
        <strong>{student.displayName}</strong>
        <small>{student.studentNo || t("teacherAnalytics.defaultLabels.studentWithId", { id: student.studentProfileId })}</small>
      </div>
      <Metric label={t("teacherAnalytics.focus.rawSubmissions")} value={student.attemptCount} />
      <Metric label={t("teacherAnalytics.focus.effectiveEdits")} value={student.effectiveAttemptCount || 0} />
      <div className="teacher-student-growth-row__state">
        <span>{growthStateLabel(growth, t)}</span>
        <small>{issueSignals.length ? issueSignals.map(item => item.title).filter(Boolean).join(" · ") : t("teacherAnalytics.focus.noCurrentIssue")}</small>
      </div>
      <ArrowRight size={18} aria-hidden="true" />
    </Link>
  );
}

function SectionTitle({ step, title, description }: { step: string; title: string; description: string }) {
  return (
    <header className="teacher-analytics-focus-title">
      <span>{step}</span>
      <div><h2>{title}</h2><p>{description}</p></div>
    </header>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div className="teacher-analytics-inline-metric"><span>{label}</span><strong>{value}</strong></div>;
}

function IssueCount({ icon: Icon, label, value, active, onClick }: { icon: typeof CircleDot; label: string; value: number; active: boolean; onClick: () => void }) {
  return (
    <button type="button" className={active ? "is-active" : ""} onClick={onClick}>
      <Icon size={16} aria-hidden="true" />
      <span>{label}</span>
      <strong>{value}</strong>
    </button>
  );
}

function selectIssue(id: string, group: StudentGroup, setIssue: (id: string) => void, setGroup: (group: StudentGroup) => void) {
  setIssue(id);
  setGroup(group);
}

function idsForGroup(issue: InsightBucket, group: StudentGroup) {
  if (group === "repeated") return issue.repeatedStudentIds || [];
  if (group === "resolved") return issue.resolvedStudentIds || [];
  return issue.affectedStudentIds || [];
}

function patternKey(issue: InsightBucket, submittedStudents: number) {
  const affected = issue.affectedStudentCount || 0;
  const repeated = issue.repeatedStudentCount || 0;
  const threshold = Math.max(2, Math.ceil(submittedStudents * 0.4));
  if (affected >= threshold && repeated > 0) return "commonRepeated";
  if (affected >= threshold) return "commonQuick";
  if (repeated > 0) return "individualRepeated";
  return "occasional";
}

function growthStateLabel(growth: SubmissionGrowthSummary | null | undefined, t: Props["t"]) {
  const key = String(growth?.growthState || "UNCOMPARABLE").toLowerCase();
  return t(`growthDashboard.state.${key}`);
}

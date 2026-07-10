import type { Assignment, AssignmentOverview, ClassGroup, TeacherKnowledgePathStat } from "../../shared/api/types";
import { assignmentStatusLabel, displayText, issueLabel } from "../../shared/format";
import type {
  AnalyticsEvidenceSample,
  AnalyticsGranularity,
  AnalyticsMetric,
  AnalyticsSnapshot,
  AssignmentAnalyticsRecord,
  AssignmentOverviewMap,
  InsightBucket,
  ProblemRow
} from "./model";

type ProblemSummary = NonNullable<AssignmentOverview["problemSummaries"]>[number];
type ProblemStudent = NonNullable<ProblemSummary["students"]>[number];
type AnalyticsTranslator = (key: string, params?: Record<string, string | number>) => string;

export function cleanAnalyticsAssignment(assignment: Assignment, t?: AnalyticsTranslator): AssignmentAnalyticsRecord {
  const fallback = t?.("teacherAnalytics.defaultLabels.assignmentFallbackWithId", { id: assignment.id }) || `Class Assignment #${assignment.id}`;
  const title = displayText(assignment.title, fallback).includes("试点任务")
    ? t?.("teacherAnalytics.defaultLabels.pilotAssignment") || "Class Coding Assignment"
    : displayText(assignment.title, fallback);
  return {
    ...assignment,
    title,
    className: displayText(assignment.className, t?.("teacherAnalytics.defaultLabels.defaultClass") || "Default class")
  };
}

export function classAssignments(assignments: Assignment[], classId: number, className?: string | null, t?: AnalyticsTranslator): AssignmentAnalyticsRecord[] {
  const normalizedClassName = displayText(className, "");
  return assignments
    .map(assignment => cleanAnalyticsAssignment(assignment, t))
    .filter(assignment => {
      if (assignment.classGroupId === classId) {
        return true;
      }
      return normalizedClassName && assignment.className === normalizedClassName;
    })
    .sort((left, right) => Date.parse(right.createdAt || "") - Date.parse(left.createdAt || "") || right.id - left.id);
}

export function findClass(classes: ClassGroup[], classId: number): ClassGroup | null {
  return classes.find(item => item.id === classId) || null;
}

export function findAssignment(assignments: Assignment[], assignmentId: number, t?: AnalyticsTranslator): AssignmentAnalyticsRecord | null {
  const assignment = assignments.find(item => item.id === assignmentId);
  return assignment ? cleanAnalyticsAssignment(assignment, t) : null;
}

export function latestSubmittedStudentCount(overview?: AssignmentOverview | null) {
  if (typeof overview?.submittedStudentCount === "number") {
    return overview.submittedStudentCount;
  }
  const trend = overview?.progressTrend || [];
  return trend[trend.length - 1]?.submittedStudentCount ?? countSubmittedStudents(overview);
}

export function assignmentPassRate(overview?: AssignmentOverview | null) {
  return typeof overview?.attemptPassRate === "number"
    ? overview.attemptPassRate
    : overview?.attemptCount
      ? overview.passedAttemptCount / overview.attemptCount
      : null;
}

export function formatPercent(value?: number | null) {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return "-";
  }
  const percent = Math.abs(value) > 1 ? value : value * 100;
  return `${Math.round(percent)}%`;
}

export function formatRatio(left: number, right: number) {
  return right ? `${left}/${right}` : String(left || "-");
}

export function buildClassAnalyticsSnapshot(input: {
  classGroup: ClassGroup;
  assignments: Assignment[];
  overviewByAssignment: AssignmentOverviewMap;
  t?: AnalyticsTranslator;
}): AnalyticsSnapshot {
  const assignments = classAssignments(input.assignments, input.classGroup.id, input.classGroup.name, input.t);
  const overviews = assignments.map(assignment => input.overviewByAssignment[assignment.id]).filter(Boolean) as AssignmentOverview[];
  const rosterStudentCount = overviews.length
    ? Math.max(...overviews.map(overview => overview.rosterStudentCount || overview.participantCount || 0))
    : 0;
  const submittedStudentIds = new Set<number>();
  const passedStudentIds = new Set<number>();
  overviews.forEach(overview => overview.students.forEach(student => {
    if (student.attemptCount > 0) {
      submittedStudentIds.add(student.studentProfileId);
    }
    if (student.passedCount > 0) {
      passedStudentIds.add(student.studentProfileId);
    }
  }));
  const attempts = overviews.reduce((sum, overview) => sum + overview.attemptCount, 0);
  const passed = overviews.reduce((sum, overview) => sum + overview.passedAttemptCount, 0);
  const completenessLegal = overviews.reduce((sum, overview) => sum + (overview.dataCompleteness?.legalIdentityCount || 0), 0);
  const completenessReady = overviews.reduce((sum, overview) => sum + (overview.dataCompleteness?.completeSubmissionCount || 0), 0);
  const identityMissing = overviews.reduce((sum, overview) => sum + (overview.dataCompleteness?.identityMissingCount || 0), 0);
  const analysisMissing = overviews.reduce((sum, overview) => sum + (overview.dataCompleteness?.analysisMissingCount || 0), 0);
  const recoveryNumerator = overviews.reduce((sum, overview) => sum + (overview.recoverySummary?.recoveryNumerator || 0), 0);
  const recoveryDenominator = overviews.reduce((sum, overview) => sum + (overview.recoverySummary?.recoveryDenominator || 0), 0);
  const issueCount = overviews.reduce(
    (sum, overview) => sum + (overview.knowledgePathStats || [])
      .filter(item => item.granularity === "mistakePoint")
      .reduce((inner, item) => inner + item.errorOccurrenceCount, 0),
    0
  );
  const affectedStudentIds = new Set<number>();
  overviews.forEach(overview => {
    overview.knowledgePathStats?.forEach(stat => stat.affectedStudentIds?.forEach(id => affectedStudentIds.add(id)));
  });
  const rows = assignments.map(assignment => assignmentRow(assignment, input.overviewByAssignment[assignment.id], input.classGroup.id, input.t));
  const allProblems = overviews.flatMap(overview => problemRows(overview, input.classGroup.id));
  const evidence = collectClassEvidence(assignments, input.overviewByAssignment, input.classGroup.id, input.t);
  return {
    scope: { type: "class", classId: input.classGroup.id, className: input.classGroup.name },
    classGroup: input.classGroup,
    metrics: [
      metric("assignments", assignments.length),
      metric("rosterStudents", rosterStudentCount || "-"),
      metric("submittedStudents", submittedStudentIds.size || "-"),
      metric("unsubmittedStudents", Math.max(0, rosterStudentCount - submittedStudentIds.size)),
      metric("studentAccuracy", submittedStudentIds.size ? formatPercent(passedStudentIds.size / submittedStudentIds.size) : "-"),
      metric("attemptAccuracy", attempts ? formatPercent(passed / attempts) : "-"),
      metric("dataCompleteness", completenessLegal ? formatPercent(completenessReady / completenessLegal) : "-", completenessNote(input.t, identityMissing, analysisMissing)),
      metric("recoveryEvidence", recoveryDenominator ? `${recoveryNumerator}/${recoveryDenominator}` : "-", recoveryNote(input.t, recoveryDenominator)),
      metric("errorCount", issueCount || "-"),
      metric("affectedStudents", affectedStudentIds.size || "-")
    ],
    insightBuckets: buildBucketsFromOverviews(overviews, evidence, input.t),
    assignmentRows: rows,
    problemRows: allProblems,
    evidenceSamples: evidence,
    emptyReason: assignments.length ? undefined : "noAssignments"
  };
}

export function buildAssignmentAnalyticsSnapshot(input: {
  classGroup: ClassGroup;
  assignment: AssignmentAnalyticsRecord;
  overview: AssignmentOverview;
  t?: AnalyticsTranslator;
}): AnalyticsSnapshot {
  const problemList = problemRows(input.overview, input.classGroup.id, input.assignment.id);
  const evidence = collectAssignmentEvidence(input.overview, input.classGroup.id, input.assignment.id, undefined, input.t);
  return {
    scope: {
      type: "assignment",
      classId: input.classGroup.id,
      className: input.classGroup.name,
      assignmentId: input.assignment.id,
      assignmentTitle: input.assignment.title
    },
    classGroup: input.classGroup,
    assignment: input.assignment,
    overview: input.overview,
    metrics: [
      metric("submittedStudents", latestSubmittedStudentCount(input.overview)),
      metric("unsubmittedStudents", input.overview.unsubmittedStudentCount ?? Math.max(0, input.overview.participantCount - latestSubmittedStudentCount(input.overview))),
      metric("studentAccuracy", formatPercent(input.overview.studentPassRate)),
      metric("attemptAccuracy", formatPercent(assignmentPassRate(input.overview))),
      metric("averageAttempts", averageAttempts(input.overview)),
      metric("dataCompleteness", formatPercent(input.overview.dataCompleteness?.completeRate), completenessNote(
        input.t,
        input.overview.dataCompleteness?.identityMissingCount || 0,
        input.overview.dataCompleteness?.analysisMissingCount || 0
      )),
      metric("recoveryEvidence", input.overview.recoverySummary?.recoveryDenominator
        ? `${input.overview.recoverySummary.recoveryNumerator}/${input.overview.recoverySummary.recoveryDenominator}`
        : "-", recoveryNote(input.t, input.overview.recoverySummary?.recoveryDenominator || 0))
    ],
    insightBuckets: buildBucketsFromOverviews([input.overview], evidence, input.t),
    assignmentRows: [assignmentRow(input.assignment, input.overview, input.classGroup.id, input.t)],
    problemRows: problemList,
    evidenceSamples: evidence,
    emptyReason: input.overview.attemptCount ? undefined : "noSubmissions"
  };
}

export function buildProblemAnalyticsSnapshot(input: {
  classGroup: ClassGroup;
  assignment: AssignmentAnalyticsRecord;
  overview: AssignmentOverview;
  problemId: number;
  t?: AnalyticsTranslator;
}): AnalyticsSnapshot | null {
  const problem = (input.overview.problemSummaries || []).find(item => item.problemId === input.problemId);
  if (!problem) {
    return null;
  }
  const students = problem.students || [];
  const submitted = problem.submittedStudentCount || students.filter(student => student.attemptCount > 0).length;
  const passed = problem.passedStudentCount || students.filter(student => student.passedCount > 0).length;
  const failed = Math.max(0, submitted - passed);
  const evidence = collectProblemEvidence(input.classGroup.id, input.assignment.id, problem, input.t);
  return {
    scope: {
      type: "problem",
      classId: input.classGroup.id,
      className: input.classGroup.name,
      assignmentId: input.assignment.id,
      assignmentTitle: input.assignment.title,
      problemId: problem.problemId,
      problemTitle: problem.title
    },
    classGroup: input.classGroup,
    assignment: input.assignment,
    overview: input.overview,
    metrics: [
      metric("submittedStudents", submitted || "-"),
      metric("passedStudents", passed || "-"),
      metric("failedStudents", failed || "-"),
      metric("studentAccuracy", formatPercent(problem.studentPassRate ?? rateToRatio(problem.passRate))),
      metric("attemptAccuracy", formatPercent(problem.attemptPassRate)),
      metric("dataCompleteness", formatPercent(problem.dataCompleteness?.completeRate), completenessNote(
        input.t,
        problem.dataCompleteness?.identityMissingCount || 0,
        problem.dataCompleteness?.analysisMissingCount || 0
      )),
      metric("recoveryEvidence", problem.recoverySummary?.recoveryDenominator
        ? `${problem.recoverySummary.recoveryNumerator}/${problem.recoverySummary.recoveryDenominator}`
        : "-", recoveryNote(input.t, problem.recoverySummary?.recoveryDenominator || 0))
    ],
    insightBuckets: buildBucketsFromProblem(problem, evidence, input.t),
    assignmentRows: [assignmentRow(input.assignment, input.overview, input.classGroup.id, input.t)],
    problemRows: problemRows(input.overview, input.classGroup.id, input.assignment.id),
    evidenceSamples: evidence,
    emptyReason: problem.submissionCount ? undefined : "noSubmissions"
  };
}

export function problemRows(overview: AssignmentOverview, classId: number, assignmentId = overview.assignment.id): ProblemRow[] {
  return [...(overview.problemSummaries || [])]
    .sort((left, right) => (left.orderIndex ?? 0) - (right.orderIndex ?? 0))
    .map(problem => ({
      id: problem.problemId,
      title: problem.title,
      href: `/app/teacher/classes/${classId}/assignments/${assignmentId}/problems/${problem.problemId}`,
      difficulty: problem.difficulty,
      submittedStudentCount: problem.submittedStudentCount,
      passedStudentCount: problem.passedStudentCount,
      participantCount: problem.classStudentCount || overview.rosterStudentCount || overview.participantCount,
      passRate: rateToRatio(problem.passRate),
      topIssue: problem.topIssues?.[0]?.label || null
    }));
}

function assignmentRow(assignment: AssignmentAnalyticsRecord, overview: AssignmentOverview | null | undefined, classId: number, t?: AnalyticsTranslator) {
  return {
    id: assignment.id,
    title: assignment.title,
    status: localizedAssignmentStatus(assignment.status, t),
    href: `/app/teacher/classes/${classId}/assignments/${assignment.id}`,
    problemCount: assignment.tasks?.length || overview?.problemSummaries?.length || 0,
    submittedStudentCount: latestSubmittedStudentCount(overview),
    participantCount: overview?.rosterStudentCount || overview?.participantCount || 0,
    passRate: assignmentPassRate(overview),
    topIssue: overview?.topIssues?.[0]?.label || overview?.problemSummaries?.find(problem => problem.topIssues?.[0])?.topIssues?.[0]?.label || null
  };
}

function localizedAssignmentStatus(status: string, t?: AnalyticsTranslator) {
  const normalized = (status || "").toUpperCase();
  if (normalized === "ACTIVE") {
    return t?.("teacherAnalytics.status.active") || assignmentStatusLabel(status);
  }
  if (normalized === "DRAFT") {
    return t?.("teacherAnalytics.status.draft") || assignmentStatusLabel(status);
  }
  if (normalized === "CLOSED") {
    return t?.("teacherAnalytics.status.closed") || assignmentStatusLabel(status);
  }
  return assignmentStatusLabel(status);
}

function metric(key: string, value: string | number, note?: string | number): AnalyticsMetric {
  return { key, labelKey: `teacherAnalytics.metrics.${key}`, value, note };
}

function completenessNote(t: AnalyticsTranslator | undefined, identityMissing: number, analysisMissing: number) {
  return t?.("teacherAnalytics.completeness.note", { identityMissing, analysisMissing })
    || `${identityMissing} identity missing · ${analysisMissing} diagnosis missing`;
}

function recoveryNote(t: AnalyticsTranslator | undefined, denominator: number) {
  return t?.("teacherAnalytics.recovery.note", { denominator }) || `${denominator} comparable samples`;
}

function countSubmittedStudents(overview?: AssignmentOverview | null) {
  return overview?.students.filter(student => student.attemptCount > 0).length || 0;
}

function averageAttempts(overview: AssignmentOverview) {
  const submitted = latestSubmittedStudentCount(overview);
  if (!submitted) {
    return "-";
  }
  return (overview.attemptCount / submitted).toFixed(1).replace(/\\.0$/, "");
}

function buildBucketsFromOverviews(overviews: AssignmentOverview[], fallbackEvidence: AnalyticsEvidenceSample[], t?: AnalyticsTranslator) {
  return buildBucketsFromStats(overviews.flatMap(overview => overview.knowledgePathStats || []), fallbackEvidence, t);
}

function buildBucketsFromProblem(problem: ProblemSummary, fallbackEvidence: AnalyticsEvidenceSample[], t?: AnalyticsTranslator) {
  return buildBucketsFromStats(problem.knowledgePathStats || [], fallbackEvidence, t);
}

function buildBucketsFromStats(
  stats: TeacherKnowledgePathStat[],
  fallbackEvidence: AnalyticsEvidenceSample[],
  t?: AnalyticsTranslator
): Record<AnalyticsGranularity, InsightBucket[]> {
  type Accumulator = {
    stat: TeacherKnowledgePathStat;
    count: number;
    rawCount: number;
    effectiveCount: number;
    unresolvedCount: number;
    recurringCount: number;
    recoveredCount: number;
    recoveryNumerator: number;
    recoveryDenominator: number;
    difficultyClassification?: string | null;
    studentIds: Set<number>;
    repeatedStudentIds: Set<number>;
    problemIds: Set<number>;
    evidence: AnalyticsEvidenceSample[];
  };
  const grouped: Record<AnalyticsGranularity, Map<string, Accumulator>> = {
    chapter: new Map(),
    knowledgePoint: new Map(),
    skillUnit: new Map(),
    mistakePoint: new Map()
  };
  stats.forEach(stat => {
    if (!isAnalyticsGranularity(stat.granularity)) {
      return;
    }
    const target = grouped[stat.granularity];
    const current = target.get(stat.id) || {
      stat,
      count: 0,
      rawCount: 0,
      effectiveCount: 0,
      unresolvedCount: 0,
      recurringCount: 0,
      recoveredCount: 0,
      recoveryNumerator: 0,
      recoveryDenominator: 0,
      difficultyClassification: null,
      studentIds: new Set<number>(),
      repeatedStudentIds: new Set<number>(),
      problemIds: new Set<number>(),
      evidence: []
    };
    current.count += stat.errorOccurrenceCount || 0;
    current.rawCount += stat.rawOccurrenceCount ?? stat.errorOccurrenceCount ?? 0;
    current.effectiveCount += stat.effectiveWeightedOccurrenceCount ?? stat.errorOccurrenceCount ?? 0;
    current.unresolvedCount += stat.unresolvedStudentCount || 0;
    current.recurringCount += stat.recurringStudentCount || 0;
    current.recoveredCount += stat.recoveredStudentCount || 0;
    current.recoveryNumerator += stat.recoveryNumerator || 0;
    current.recoveryDenominator += stat.recoveryDenominator || 0;
    current.difficultyClassification = strongerClassification(current.difficultyClassification, stat.difficultyClassification);
    (stat.affectedStudentIds || []).forEach(id => current.studentIds.add(id));
    (stat.repeatedStudentIds || []).forEach(id => current.repeatedStudentIds.add(id));
    (stat.affectedProblemIds || []).forEach(id => current.problemIds.add(id));
    statEvidence(stat, fallbackEvidence, t).forEach(item => {
      if (!current.evidence.some(existing => existing.id === item.id)) {
        current.evidence.push(item);
      }
    });
    target.set(stat.id, current);
  });
  return Object.fromEntries(Object.entries(grouped).map(([granularity, values]) => {
    const total = [...values.values()].reduce((sum, item) => sum + item.count, 0);
    const buckets = [...values.values()]
      .map(({
        stat,
        count,
        rawCount,
        effectiveCount,
        unresolvedCount,
        recurringCount,
        recoveredCount,
        recoveryNumerator,
        recoveryDenominator,
        difficultyClassification,
        studentIds,
        repeatedStudentIds,
        problemIds,
        evidence
      }): InsightBucket => ({
        id: stat.id,
        label: localizedPathLabel(stat.label, stat.pathStatus, t),
        count,
        rate: total ? count / total : null,
        affectedStudentCount: studentIds.size || stat.affectedStudentCount,
        repeatedStudentCount: repeatedStudentIds.size || stat.repeatedStudentCount,
        affectedProblemCount: problemIds.size || stat.affectedProblemCount,
        rawOccurrenceCount: rawCount,
        effectiveWeightedOccurrenceCount: effectiveCount,
        unresolvedStudentCount: unresolvedCount,
        recurringStudentCount: recurringCount,
        recoveredStudentCount: recoveredCount,
        recoveryRate: recoveryDenominator ? recoveryNumerator / recoveryDenominator : stat.recoveryRate,
        difficultyClassification,
        path: (stat.path || [])
          .filter(node => isAnalyticsGranularity(node.kind))
          .map(node => ({ label: localizedPathLabel(node.label, stat.pathStatus, t), kind: node.kind as AnalyticsGranularity })),
        fit: normalizeLibraryFit(stat.libraryFit),
        pathStatus: stat.pathStatus,
        evidence: evidence.slice(0, 4)
      }))
      .sort((left, right) => right.count - left.count || left.label.localeCompare(right.label, "zh-Hans-CN"))
      .slice(0, 8);
    return [granularity, buckets];
  })) as Record<AnalyticsGranularity, InsightBucket[]>;
}

function strongerClassification(left?: string | null, right?: string | null) {
  const rank: Record<string, number> = {
    OCCASIONAL_INDIVIDUAL: 1,
    COMMON_ERROR: 2,
    INDIVIDUAL_PERSISTENT: 3,
    CLASS_DIFFICULTY: 4
  };
  return (rank[String(right || "")] || 0) > (rank[String(left || "")] || 0) ? right : left;
}

function statEvidence(stat: TeacherKnowledgePathStat, fallback: AnalyticsEvidenceSample[], t?: AnalyticsTranslator) {
  const fallbackBySubmission = new Map(fallback
    .filter(item => typeof item.submissionId === "number")
    .map(item => [item.submissionId as number, item]));
  return (stat.evidenceSamples || []).slice(0, 8).map(sample => {
    const exact = fallbackBySubmission.get(sample.submissionId);
    if (exact) {
      return exact;
    }
    const related = fallback.find(item => item.studentProfileId === sample.studentProfileId && item.problemId === sample.problemId);
    return {
      id: `submission:${sample.submissionId}`,
      title: t?.("teacherAnalytics.evidence.submissionWithId", { id: sample.submissionId }) || `Submission #${sample.submissionId}`,
      subtitle: stat.label,
      meta: sample.verdict || undefined,
      assignmentId: related?.assignmentId,
      submissionId: sample.submissionId,
      studentProfileId: sample.studentProfileId || undefined,
      problemId: sample.problemId || undefined,
      href: related?.href
    } satisfies AnalyticsEvidenceSample;
  });
}

function isAnalyticsGranularity(value: string): value is AnalyticsGranularity {
  return ["chapter", "knowledgePoint", "skillUnit", "mistakePoint"].includes(value);
}

function normalizeLibraryFit(value?: string | null): InsightBucket["fit"] {
  return ["HIT", "PARTIAL", "MISS"].includes(String(value || "").toUpperCase())
    ? String(value).toUpperCase() as InsightBucket["fit"]
    : "UNKNOWN";
}

function localizedPathLabel(label: string, pathStatus?: string | null, t?: AnalyticsTranslator) {
  if (String(label || "").toUpperCase() === "UNCLASSIFIED" || String(pathStatus || "").toUpperCase() === "UNCLASSIFIED" && !label) {
    return t?.("teacherAnalytics.pathStatus.unclassified") || "Unclassified";
  }
  return displayText(label, t?.("teacherAnalytics.pathStatus.unclassified") || "Unclassified");
}

function rateToRatio(value?: number | null) {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return null;
  }
  return Math.abs(value) > 1 ? value / 100 : value;
}


function collectClassEvidence(assignments: AssignmentAnalyticsRecord[], overviews: AssignmentOverviewMap, classId: number, t?: AnalyticsTranslator) {
  return assignments
    .flatMap(assignment => collectAssignmentEvidence(overviews[assignment.id], classId, assignment.id, assignment.title, t))
    .slice(0, 8);
}

function collectAssignmentEvidence(overview: AssignmentOverview | null | undefined, classId: number, assignmentId: number, assignmentTitle?: string, t?: AnalyticsTranslator) {
  if (!overview) {
    return [];
  }
  const samples: AnalyticsEvidenceSample[] = [];
  overview.problemSummaries?.forEach(problem => {
    problem.students?.forEach(student => {
      if (samples.length >= 8) {
        return;
      }
      if (student.latestIssueTag || student.latestFineGrainedIssue || student.needsAttention) {
        samples.push(studentEvidence(problem, student, classId, assignmentId, assignmentTitle, t));
      }
    });
  });
  return samples;
}

function collectProblemEvidence(classId: number, assignmentId: number, problem: ProblemSummary, t?: AnalyticsTranslator) {
  return (problem.students || [])
    .filter(student => student.latestIssueTag || student.latestFineGrainedIssue || student.needsAttention || student.attemptCount > 0)
    .map(student => studentEvidence(problem, student, classId, assignmentId, undefined, t))
    .slice(0, 8);
}

function studentEvidence(
  problem: ProblemSummary | undefined,
  student: ProblemStudent,
  classId?: number,
  assignmentId?: number,
  assignmentTitle?: string,
  t?: AnalyticsTranslator
): AnalyticsEvidenceSample {
  const problemTitle = problem?.title || t?.("teacherAnalytics.defaultLabels.problem") || "Problem";
  const issue = issueLabel(student.latestFineGrainedIssue || student.latestIssueTag || student.latestIssue || t?.("teacherAnalytics.defaultLabels.submissionRecord") || "Submission record");
  const recentState = student.recentLearningState;
  const recentMeta = recentState
    ? t?.("teacherAnalytics.recentState.summary", {
        status: t(`teacherAnalytics.recentState.status.${recentStateKey(recentState.status)}`),
        submissions: recentState.independentSubmissionCount,
        problems: recentState.problemCount
      })
    : undefined;
  return {
    id: `${student.studentProfileId}:${student.latestSubmissionId || problem?.problemId || problemTitle}:${issue}`,
    title: displayText(student.displayName, t?.("teacherAnalytics.defaultLabels.studentWithId", { id: student.studentProfileId }) || `Student #${student.studentProfileId}`),
    subtitle: `${problemTitle} · ${issue}`,
    meta: [assignmentTitle, recentMeta].filter(Boolean).join(" · ") || undefined,
    assignmentId,
    submissionId: student.latestSubmissionId,
    studentProfileId: student.studentProfileId,
    problemId: problem?.problemId,
    issueTag: student.latestIssueTag || null,
    fineGrainedTag: student.latestFineGrainedIssue || null,
    aiFeedbackImpact: student.latestAiFeedbackImpact || null,
    href:
      classId && assignmentId && problem?.problemId && student.studentProfileId
        ? `/app/teacher/classes/${classId}/assignments/${assignmentId}/problems/${problem.problemId}#student-${student.studentProfileId}`
        : undefined
  };
}

function recentStateKey(status?: string | null) {
  switch (status) {
    case "RECENTLY_RECOVERED":
      return "recovered";
    case "REPEATED_ISSUE":
      return "repeated";
    case "ISSUE_CHANGING":
      return "changing";
    case "SINGLE_OBSERVATION":
      return "single";
    default:
      return "observing";
  }
}

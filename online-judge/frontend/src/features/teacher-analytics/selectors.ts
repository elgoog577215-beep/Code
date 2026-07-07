import type { AbilityStat, Assignment, AssignmentOverview, ClassGroup } from "../../shared/api/types";
import { assignmentStatusLabel, displayText, issueLabel } from "../../shared/format";
import type {
  AnalyticsEvidenceSample,
  AnalyticsGranularity,
  AnalyticsMetric,
  AnalyticsSnapshot,
  AssignmentAnalyticsRecord,
  AssignmentOverviewMap,
  InsightBucket,
  KnowledgePathNode,
  ProblemRow
} from "./model";

type ProblemSummary = NonNullable<AssignmentOverview["problemSummaries"]>[number];
type ProblemStudent = NonNullable<ProblemSummary["students"]>[number];
type TopIssue = AssignmentOverview["topIssues"][number] | NonNullable<ProblemSummary["topIssues"]>[number];
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
  const trend = overview?.progressTrend || [];
  return trend[trend.length - 1]?.submittedStudentCount ?? countSubmittedStudents(overview);
}

export function assignmentPassRate(overview?: AssignmentOverview | null) {
  if (!overview?.attemptCount) {
    return null;
  }
  return overview.passedAttemptCount / overview.attemptCount;
}

export function formatPercent(value?: number | null) {
  return typeof value === "number" && Number.isFinite(value) ? `${Math.round(value * 100)}%` : "-";
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
  const participantCount = overviews.length ? Math.max(...overviews.map(overview => overview.participantCount || 0)) : 0;
  const submitted = overviews.reduce((sum, overview) => sum + latestSubmittedStudentCount(overview), 0);
  const attempts = overviews.reduce((sum, overview) => sum + overview.attemptCount, 0);
  const passed = overviews.reduce((sum, overview) => sum + overview.passedAttemptCount, 0);
  const issueCount = overviews.reduce((sum, overview) => sum + overview.topIssues.reduce((inner, issue) => inner + Math.max(1, issue.count || 0), 0), 0);
  const affectedStudentIds = new Set<number>();
  overviews.forEach(overview => {
    overview.students.forEach(student => {
      if (student.latestIssueTag || student.latestFineGrainedIssue || student.needsAttention) {
        affectedStudentIds.add(student.studentProfileId);
      }
    });
  });
  const rows = assignments.map(assignment => assignmentRow(assignment, input.overviewByAssignment[assignment.id], input.classGroup.id, input.t));
  const allProblems = overviews.flatMap(overview => problemRows(overview, input.classGroup.id));
  const evidence = collectClassEvidence(assignments, input.overviewByAssignment, input.classGroup.id, input.t);
  return {
    scope: { type: "class", classId: input.classGroup.id, className: input.classGroup.name },
    classGroup: input.classGroup,
    metrics: [
      metric("assignments", assignments.length),
      metric("students", participantCount || "-"),
      metric("submissions", submitted || "-"),
      metric("accuracy", attempts ? formatPercent(passed / attempts) : "-"),
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
  const lowPassProblems = problemList.filter(problem => typeof problem.passRate === "number" && problem.passRate < 0.6).length;
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
      metric("unsubmittedStudents", Math.max(0, input.overview.participantCount - latestSubmittedStudentCount(input.overview))),
      metric("accuracy", formatPercent(assignmentPassRate(input.overview))),
      metric("averageAttempts", averageAttempts(input.overview)),
      metric("lowPassProblems", lowPassProblems),
      metric("errorCount", input.overview.topIssues.reduce((sum, item) => sum + Math.max(1, item.count || 0), 0) || "-")
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
      metric("accuracy", formatPercent(problem.passRate)),
      metric("averageAttempts", typeof problem.averageAttempts === "number" ? problem.averageAttempts.toFixed(1).replace(/\\.0$/, "") : "-")
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
      participantCount: problem.classStudentCount || overview.participantCount,
      passRate: problem.passRate ?? null,
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
    participantCount: overview?.participantCount || 0,
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
  const buckets: Record<AnalyticsGranularity, Map<string, InsightBucket>> = {
    chapter: new Map(),
    knowledgePoint: new Map(),
    skillUnit: new Map(),
    mistakePoint: new Map()
  };
  overviews.forEach(overview => {
    overview.topIssues.forEach(issue => addIssueBucket(buckets, issue, fallbackEvidence, undefined, undefined, t));
    overview.classAbilityWeaknesses?.forEach(ability => addAbilityBucket(buckets, ability, fallbackEvidence, undefined, t));
    overview.problemSummaries?.forEach(problem => {
      problem.topIssues?.forEach(issue => addIssueBucket(buckets, issue, fallbackEvidence, problem, undefined, t));
      problem.abilityWeaknesses?.forEach(ability => addAbilityBucket(buckets, ability, fallbackEvidence, problem, t));
    });
  });
  return mapBuckets(buckets);
}

function buildBucketsFromProblem(problem: ProblemSummary, fallbackEvidence: AnalyticsEvidenceSample[], t?: AnalyticsTranslator) {
  const buckets: Record<AnalyticsGranularity, Map<string, InsightBucket>> = {
    chapter: new Map(),
    knowledgePoint: new Map(),
    skillUnit: new Map(),
    mistakePoint: new Map()
  };
  problem.topIssues?.forEach(issue => addIssueBucket(buckets, issue, fallbackEvidence, problem, undefined, t));
  problem.abilityWeaknesses?.forEach(ability => addAbilityBucket(buckets, ability, fallbackEvidence, problem, t));
  problem.students?.forEach(student => {
    const label = student.latestFineGrainedIssue || student.latestIssueTag || student.latestIssue || "";
    if (label) {
      addIssueBucket(
        buckets,
        { label, count: 1, abilityPoint: student.abilityPoint, affectedStudentCount: 1 },
        fallbackEvidence,
        problem,
        student,
        t
      );
    }
  });
  return mapBuckets(buckets);
}

function addIssueBucket(
  target: Record<AnalyticsGranularity, Map<string, InsightBucket>>,
  issue: TopIssue,
  fallbackEvidence: AnalyticsEvidenceSample[],
  problem?: ProblemSummary,
  student?: ProblemStudent,
  t?: AnalyticsTranslator
) {
  const rawLabel = displayText(issue.label, "");
  if (!rawLabel) {
    return;
  }
  const label = issueLabel(rawLabel);
  const path = inferPath(label, issue.abilityPoint, problem?.title, t);
  const count = Math.max(1, issue.count || 0);
  const evidence = student ? [studentEvidence(problem, student, undefined, undefined, undefined, t)] : fallbackEvidenceForProblem(fallbackEvidence, problem);
  const affected = issue.affectedStudentCount || (student ? 1 : count);
  addPath(target.chapter, path[0], count, affected, problem ? 1 : undefined, path, evidence);
  addPath(target.knowledgePoint, path[1], count, affected, problem ? 1 : undefined, path, evidence);
  addPath(target.skillUnit, path[2], count, affected, problem ? 1 : undefined, path, evidence);
  addPath(target.mistakePoint, path[3], count, affected, problem ? 1 : undefined, path, evidence);
}

function addAbilityBucket(
  target: Record<AnalyticsGranularity, Map<string, InsightBucket>>,
  ability: AbilityStat,
  fallbackEvidence: AnalyticsEvidenceSample[],
  problem?: ProblemSummary,
  t?: AnalyticsTranslator
) {
  const label = displayText(ability.abilityPoint, "");
  if (!label) {
    return;
  }
  const path = inferPath(label, label, problem?.title, t);
  const count = Math.max(1, ability.submissionCount || ability.taskCount || 0);
  const evidence = fallbackEvidenceForProblem(fallbackEvidence, problem);
  addPath(target.chapter, path[0], count, count, problem ? 1 : undefined, path, evidence);
  addPath(target.knowledgePoint, path[1], count, count, problem ? 1 : undefined, path, evidence);
  addPath(target.skillUnit, path[2], count, count, problem ? 1 : undefined, path, evidence);
}

function addPath(
  target: Map<string, InsightBucket>,
  node: KnowledgePathNode,
  count: number,
  affectedStudentCount = 0,
  affectedProblemCount = 0,
  path: KnowledgePathNode[],
  evidence: AnalyticsEvidenceSample[]
) {
  const existing =
    target.get(node.label) ||
    {
      id: `${node.kind}:${node.label}`,
      label: node.label,
      count: 0,
      affectedStudentCount: 0,
      affectedProblemCount: 0,
      path,
      fit: "PARTIAL" as const,
      evidence: [] as AnalyticsEvidenceSample[]
    };
  existing.count += count;
  existing.affectedStudentCount = (existing.affectedStudentCount || 0) + affectedStudentCount;
  existing.affectedProblemCount = (existing.affectedProblemCount || 0) + affectedProblemCount;
  evidence.forEach(item => {
    if (!existing.evidence.some(current => current.id === item.id)) {
      existing.evidence.push(item);
    }
  });
  target.set(node.label, existing);
}

function mapBuckets(buckets: Record<AnalyticsGranularity, Map<string, InsightBucket>>) {
  return {
    chapter: finalizeBuckets(buckets.chapter),
    knowledgePoint: finalizeBuckets(buckets.knowledgePoint),
    skillUnit: finalizeBuckets(buckets.skillUnit),
    mistakePoint: finalizeBuckets(buckets.mistakePoint)
  };
}

function finalizeBuckets(map: Map<string, InsightBucket>) {
  const total = [...map.values()].reduce((sum, item) => sum + item.count, 0);
  return [...map.values()]
    .map(item => ({ ...item, rate: total ? item.count / total : null, evidence: item.evidence.slice(0, 4) }))
    .sort((left, right) => right.count - left.count || left.label.localeCompare(right.label, "zh-Hans-CN"))
    .slice(0, 8);
}

function inferPath(label: string, ability?: string | null, title?: string | null, t?: AnalyticsTranslator): KnowledgePathNode[] {
  const text = `${label} ${ability || ""} ${title || ""}`;
  const chapter = classifyChapter(text, t);
  const knowledge = displayText(ability, label);
  const skill = classifySkill(text, knowledge, t);
  return [
    { label: chapter, kind: "chapter" },
    { label: knowledge, kind: "knowledgePoint" },
    { label: skill, kind: "skillUnit" },
    { label, kind: "mistakePoint" }
  ];
}

function classifyChapter(text: string, t?: AnalyticsTranslator) {
  if (/数组|下标|窗口|前缀|区间/.test(text)) {
    return t?.("teacherAnalytics.defaultLabels.arraySequence") || "Arrays and sequences";
  }
  if (/字符串|回文|字符|编码|输出|输入/.test(text)) {
    return t?.("teacherAnalytics.defaultLabels.ioString") || "Input, output, and strings";
  }
  if (/循环|递归|分治/.test(text)) {
    return t?.("teacherAnalytics.defaultLabels.loopRecursion") || "Loops and recursion";
  }
  if (/树|二叉|链表|队列|栈/.test(text)) {
    return t?.("teacherAnalytics.defaultLabels.dataStructure") || "Data structures";
  }
  if (/DP|动态规划|状态|收益/.test(text)) {
    return t?.("teacherAnalytics.defaultLabels.dynamicProgramming") || "Dynamic programming";
  }
  return t?.("teacherAnalytics.defaultLabels.general") || "General application";
}

function classifySkill(text: string, fallback: string, t?: AnalyticsTranslator) {
  if (/边界|越界|l-1|右端|左端/.test(text)) {
    return t?.("teacherAnalytics.defaultLabels.boundary") || "Boundary handling";
  }
  if (/格式|输出|输入/.test(text)) {
    return t?.("teacherAnalytics.defaultLabels.ioFormat") || "Input/output format";
  }
  if (/状态|转移/.test(text)) {
    return t?.("teacherAnalytics.defaultLabels.stateMaintenance") || "State maintenance";
  }
  return fallback;
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
  return {
    id: `${student.studentProfileId}:${student.latestSubmissionId || problem?.problemId || problemTitle}:${issue}`,
    title: displayText(student.displayName, t?.("teacherAnalytics.defaultLabels.studentWithId", { id: student.studentProfileId }) || `Student #${student.studentProfileId}`),
    subtitle: `${problemTitle} · ${issue}`,
    meta: assignmentTitle,
    assignmentId,
    submissionId: student.latestSubmissionId,
    studentProfileId: student.studentProfileId,
    problemId: problem?.problemId,
    issueTag: student.latestIssueTag || null,
    fineGrainedTag: student.latestFineGrainedIssue || null,
    href:
      classId && assignmentId && problem?.problemId && student.studentProfileId
        ? `/app/teacher/classes/${classId}/assignments/${assignmentId}/problems/${problem.problemId}#student-${student.studentProfileId}`
        : undefined
  };
}

function fallbackEvidenceForProblem(fallbackEvidence: AnalyticsEvidenceSample[], problem?: ProblemSummary) {
  if (!problem) {
    return fallbackEvidence.slice(0, 2);
  }
  return fallbackEvidence.filter(item => item.subtitle.includes(problem.title)).slice(0, 2);
}

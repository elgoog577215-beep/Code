import type {
  Assignment,
  AssignmentOverview,
  AiQualityOverview,
  AiQualityTrend,
  ClassGroup,
  CoachPrompt,
  DiagnosisEvalCandidates,
  DiagnosisEvalFixtureDraft,
  DiagnosisTag,
  ExecutorStatus,
  ImportCommit,
  ImportPreview,
  LeaderboardEntry,
  Problem,
  ProblemCatalogItem,
  ProblemManage,
  RecommendationEffectiveness,
  StudentAbilityProfile,
  StudentIdentityAudit,
  StudentRecommendation,
  StudentProfile,
  StudentTrajectory,
  SubmissionHistorySummary,
  SubmissionResult,
  TeacherDiagnosisCorrection
} from "./types";
import { YINGQI_SIGNATURE } from "../identity/yingqiSignature";

export class ApiError extends Error {
  status: number;
  payload: unknown;

  constructor(message: string, status: number, payload: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.payload = payload;
  }
}

async function readJson<T>(response: Response): Promise<T> {
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const message =
      typeof payload === "object" && payload && "error" in payload
        ? String((payload as { error?: unknown }).error)
        : typeof payload === "object" && payload && "message" in payload
          ? String((payload as { message?: unknown }).message)
          : response.status >= 500
            ? "服务暂时不可用"
            : "操作未完成";
    throw new ApiError(message, response.status, payload);
  }
  return payload as T;
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  headers.set(YINGQI_SIGNATURE.headers.owner, YINGQI_SIGNATURE.owner);
  headers.set(YINGQI_SIGNATURE.headers.signature, YINGQI_SIGNATURE.fingerprint);
  headers.set(YINGQI_SIGNATURE.headers.claim, YINGQI_SIGNATURE.claim);
  const hasBody = init?.body !== undefined && init.body !== null;
  if (hasBody && !headers.has("Content-Type") && !(init?.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(url, { ...init, headers });
  return readJson<T>(response);
}

function jsonBody(payload: unknown): string {
  return JSON.stringify(payload);
}

export const api = {
  resolveInvite: (code: string) =>
    request<Assignment>("/api/invites/resolve", {
      method: "POST",
      body: jsonBody({ code })
    }),

  bindStudent: (payload: {
    assignmentId: number;
    classGroupId?: number | null;
    className?: string;
    displayName: string;
    studentNo?: string;
  }) =>
    request<StudentProfile>("/api/student/identity", {
      method: "POST",
      body: jsonBody(payload)
    }),

  studentTrajectory: (assignmentId: number, studentProfileId: number) =>
    request<StudentTrajectory>(`/api/student/assignments/${assignmentId}/profile/${studentProfileId}/trajectory`),
  studentAbilityProfile: (studentProfileId: number) =>
    request<StudentAbilityProfile>(`/api/student/profile/${studentProfileId}/ability-profile`),
  studentRecommendations: (studentProfileId: number) =>
    request<StudentRecommendation>(`/api/student/profile/${studentProfileId}/recommendations`),
  recordRecommendationEvent: (studentProfileId: number, recommendationToken: string, eventType = "CLICKED") =>
    request<void>(`/api/student/profile/${studentProfileId}/recommendation-clicks`, {
      method: "POST",
      body: jsonBody({ recommendationToken, eventType })
    }),

  problems: () => request<Problem[]>("/api/problems"),
  problemCatalog: () => request<ProblemCatalogItem[]>("/api/problems/catalog"),
  problem: (id: number) => request<Problem>(`/api/problems/${id}`),
  problemManage: (id: number) => request<ProblemManage>(`/api/problems/${id}/manage`),
  createProblem: (payload: unknown) =>
    request<Problem>("/api/problems", { method: "POST", body: jsonBody(payload) }),
  updateProblem: (id: number, payload: unknown) =>
    request<Problem>(`/api/problems/${id}`, { method: "PUT", body: jsonBody(payload) }),

  submit: (payload: {
    problemId: number;
    assignmentId?: number | null;
    studentProfileId?: number | null;
    recommendationToken?: string | null;
    languageId: number;
    sourceCode: string;
  }) =>
    request<SubmissionResult>("/api/submissions", {
      method: "POST",
      body: jsonBody(payload)
    }),

  submission: (id: number) => request<SubmissionResult>(`/api/submissions/${id}`),
  submissionAnalysis: (id: number) => request<{ analysis: SubmissionResult["analysis"] }>(`/api/submissions/${id}/analysis`),
  triggerAnalysis: (id: number) =>
    request<{ analysis: SubmissionResult["analysis"] }>(`/api/submissions/${id}/analysis`, { method: "POST" }),
  coachPrompt: (id: number) => request<CoachPrompt | null>(`/api/submissions/${id}/coach-prompt`),
  generateCoachPrompt: (id: number) =>
    request<CoachPrompt>(`/api/submissions/${id}/coach-prompt`, { method: "POST" }),
  replyCoachPrompt: (id: number, answer: string) =>
    request<CoachPrompt>(`/api/submissions/${id}/coach-turns`, {
      method: "POST",
      body: jsonBody({ answer })
    }),
  history: (problemId: number) =>
    request<SubmissionHistorySummary[]>(`/api/submissions/problem/${problemId}/history-summary`),

  classes: () => request<ClassGroup[]>("/api/teacher/classes"),
  createClass: (payload: { name: string; grade?: string; teacherName?: string }) =>
    request<ClassGroup>("/api/teacher/classes", { method: "POST", body: jsonBody(payload) }),
  studentIdentityAudit: (classGroupId: number) =>
    request<StudentIdentityAudit>(`/api/teacher/classes/${classGroupId}/identity-audit`),
  mergeStudentIdentities: (classGroupId: number, payload: { studentProfileIds: number[]; targetStudentProfileId?: number | null }) =>
    request<StudentIdentityAudit>(`/api/teacher/classes/${classGroupId}/identity-merge`, {
      method: "POST",
      body: jsonBody(payload)
    }),
  splitStudentIdentity: (classGroupId: number, payload: { studentProfileId: number }) =>
    request<StudentIdentityAudit>(`/api/teacher/classes/${classGroupId}/identity-split`, {
      method: "POST",
      body: jsonBody(payload)
    }),

  assignments: () => request<Assignment[]>("/api/teacher/assignments"),
  assignment: (id: number) => request<Assignment>(`/api/teacher/assignments/${id}`),
  createAssignment: (payload: unknown) =>
    request<Assignment>("/api/teacher/assignments", { method: "POST", body: jsonBody(payload) }),
  updateAssignment: (id: number, payload: unknown) =>
    request<Assignment>(`/api/teacher/assignments/${id}`, { method: "PUT", body: jsonBody(payload) }),
  rotateInvite: (id: number) =>
    request<Assignment>(`/api/teacher/assignments/${id}/invite`, { method: "POST" }),
  assignmentOverview: (id: number) => request<AssignmentOverview>(`/api/teacher/assignments/${id}/overview`),
  aiQualityOverview: (id: number) => request<AiQualityOverview>(`/api/teacher/assignments/${id}/ai-quality`),
  diagnosisEvalCandidates: (id: number) =>
    request<DiagnosisEvalCandidates>(`/api/teacher/assignments/${id}/diagnosis-eval-candidates`),
  diagnosisEvalFixtureDraft: (id: number) =>
    request<DiagnosisEvalFixtureDraft>(`/api/teacher/assignments/${id}/diagnosis-eval-fixture-draft`),
  aiQualityTrend: () => request<AiQualityTrend>("/api/teacher/ai-quality/trend"),
  recommendationEffectiveness: () => request<RecommendationEffectiveness>("/api/teacher/recommendations/effectiveness"),
  recordClassReviewFeedback: (
    assignmentId: number,
    payload: {
      suggestionKey: string;
      actionType: "ACCEPTED" | "DISMISSED" | "MODIFIED";
      targetAbility?: string | null;
      exampleProblemId?: number | null;
      evidenceTags?: string[];
      teacherNote?: string;
      createdBy?: string;
    }
  ) =>
    request<void>(`/api/teacher/assignments/${assignmentId}/class-review-feedback`, {
      method: "POST",
      body: jsonBody(payload)
    }),
  diagnosisTags: () => request<DiagnosisTag[]>("/api/teacher/diagnosis-tags"),
  correctDiagnosis: (
    assignmentId: number,
    payload: {
      submissionId: number;
      correctedIssueTag: string;
      correctedFineGrainedTag?: string | null;
      teacherNote?: string;
      evalCandidate?: boolean;
      correctedBy?: string;
    }
  ) =>
    request<TeacherDiagnosisCorrection>(`/api/teacher/assignments/${assignmentId}/diagnosis-corrections`, {
      method: "POST",
      body: jsonBody(payload)
    }),

  classImportPreview: (payload: unknown) =>
    request<ImportPreview>("/api/teacher/classes/import-preview", { method: "POST", body: jsonBody(payload) }),
  classImportCommit: (payload: unknown) =>
    request<ImportCommit>("/api/teacher/classes/import-commit", { method: "POST", body: jsonBody(payload) }),
  problemImportPreview: (payload: unknown) =>
    request<ImportPreview>("/api/teacher/problems/import-preview", { method: "POST", body: jsonBody(payload) }),
  problemImportCommit: (payload: unknown) =>
    request<ImportCommit>("/api/teacher/problems/import-commit", { method: "POST", body: jsonBody(payload) }),

  executorStatus: () => request<ExecutorStatus>("/api/system/executor-status"),
  classOverview: () => request<LeaderboardEntry[]>("/api/leaderboard/problems")
};

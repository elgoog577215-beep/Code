import type {
  Assignment,
  AssignmentReadiness,
  AssignmentOverview,
  AiStandardLibraryItem,
  AiStandardLibraryItemPayload,
  AiStandardLibraryLayer,
  AiStandardLibraryGrowthCandidate,
  AiStandardLibraryGrowthCandidatePayload,
  AiStandardLibraryGrowthGovernanceSummary,
  AiSmoke,
  AiQualityOverview,
  AiQualityTrend,
  AuthSession,
  ClassGroup,
  DiagnosisEvalCandidates,
  DiagnosisEvalFixtureDraft,
  DiagnosisTag,
  ExecutorStatus,
  InformaticsKnowledgeNode,
  ImportCommit,
  ImportPreview,
  LeaderboardEntry,
  Problem,
  ProblemCatalogItem,
  ProblemManage,
  Readiness,
  StudentAbilityProfile,
  StudentAssignmentLeaderboard,
  StudentAssignmentSubmissionPage,
  StudentIdentityAudit,
  StudentProfile,
  StudentTrajectory,
  StudentAiFeedbackObservability,
  SubmissionAnalysisLookup,
  SubmissionHistorySummary,
  SubmissionResult,
  StudentAiFeedbackLookup,
  TeacherDiagnosisCorrection
} from "./types";
import { YINGQI_SIGNATURE } from "../identity/yingqiSignature";
import { loadStudentToken } from "../storage";

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
  const studentToken = loadStudentToken();
  if (studentToken && !headers.has("X-Student-Token")) {
    headers.set("X-Student-Token", studentToken);
  }
  const response = await fetch(url, { ...init, headers, credentials: "same-origin" });
  return readJson<T>(response);
}

function jsonBody(payload: unknown): string {
  return JSON.stringify(payload);
}

function queryString(params: Record<string, string | number | boolean | undefined | null>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== "") {
      search.set(key, String(value));
    }
  });
  const value = search.toString();
  return value ? `?${value}` : "";
}

export const api = {
  teacherSession: () => request<AuthSession>("/api/teacher/auth/session"),
  teacherLogin: (password: string) =>
    request<AuthSession>("/api/teacher/auth/login", {
      method: "POST",
      body: jsonBody({ password })
    }),
  teacherLogout: () => request<AuthSession>("/api/teacher/auth/logout", { method: "POST" }),

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

  loginStudent: (payload: { classGroupId: number; displayName: string; studentNo?: string; note?: string }) =>
    request<StudentProfile>("/api/student/login", {
      method: "POST",
      body: jsonBody(payload)
    }),

  studentClasses: () => request<ClassGroup[]>("/api/student/classes"),
  studentAssignments: (studentProfileId: number) => request<Assignment[]>(`/api/student/profile/${studentProfileId}/assignments`),

  studentTrajectory: (assignmentId: number, studentProfileId: number) =>
    request<StudentTrajectory>(`/api/student/assignments/${assignmentId}/profile/${studentProfileId}/trajectory`),
  studentAssignmentLeaderboard: (assignmentId: number) =>
    request<StudentAssignmentLeaderboard>(`/api/student/assignments/${assignmentId}/leaderboard`),
  studentAssignmentSubmissions: (assignmentId: number, params?: {
    problemId?: number | null;
    accepted?: boolean | null;
    verdict?: string | null;
    languageName?: string | null;
    submissionId?: number | null;
    page?: number;
    size?: number;
  }) => request<StudentAssignmentSubmissionPage>(
    `/api/student/assignments/${assignmentId}/submissions${queryString(params || {})}`
  ),
  studentAbilityProfile: (studentProfileId: number) =>
    request<StudentAbilityProfile>(`/api/student/profile/${studentProfileId}/ability-profile`),
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
    languageId: number;
    sourceCode: string;
  }) =>
    request<SubmissionResult>("/api/submissions", {
      method: "POST",
      body: jsonBody(payload)
    }),

  submission: (id: number) => request<SubmissionResult>(`/api/submissions/${id}`),
  submissionAnalysis: (id: number) => request<SubmissionAnalysisLookup>(`/api/submissions/${id}/analysis`),
  triggerAnalysis: (id: number) =>
    request<SubmissionAnalysisLookup>(`/api/submissions/${id}/analysis`, { method: "POST" }),
  studentAiFeedback: (id: number) => request<StudentAiFeedbackLookup>(`/api/submissions/${id}/student-ai-feedback`),
  triggerStudentAiFeedback: (id: number) =>
    request<StudentAiFeedbackLookup>(`/api/submissions/${id}/student-ai-feedback`, { method: "POST" }),
  recordStudentAiFeedbackView: (id: number) =>
    request<void>(`/api/submissions/${id}/student-ai-feedback/view`, { method: "POST" }),
  history: (problemId: number, assignmentId?: number | null) =>
    request<SubmissionHistorySummary[]>(
      `/api/submissions/problem/${problemId}/history-summary${queryString({ assignmentId })}`
    ),

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
  assignmentReadiness: (problemIds: number[]) =>
    request<AssignmentReadiness>("/api/teacher/assignments/readiness", {
      method: "POST",
      body: jsonBody({ problemIds })
    }),
  updateAssignment: (id: number, payload: unknown) =>
    request<Assignment>(`/api/teacher/assignments/${id}`, { method: "PUT", body: jsonBody(payload) }),
  rotateInvite: (id: number) =>
    request<Assignment>(`/api/teacher/assignments/${id}/invite`, { method: "POST" }),
  assignmentOverview: (id: number) => request<AssignmentOverview>(`/api/teacher/assignments/${id}/overview`),
  teacherStudentProblemGrowth: (assignmentId: number, problemId: number, studentProfileId: number) =>
    request<SubmissionHistorySummary[]>(
      `/api/teacher/assignments/${assignmentId}/problems/${problemId}/students/${studentProfileId}/growth`
    ),
  aiQualityOverview: (id: number) => request<AiQualityOverview>(`/api/teacher/assignments/${id}/ai-quality`),
  studentAiFeedbackObservability: (id: number) =>
    request<StudentAiFeedbackObservability>(`/api/teacher/assignments/${id}/student-ai-feedback-observability`),
  diagnosisEvalCandidates: (id: number) =>
    request<DiagnosisEvalCandidates>(`/api/teacher/assignments/${id}/diagnosis-eval-candidates`),
  diagnosisEvalFixtureDraft: (id: number) =>
    request<DiagnosisEvalFixtureDraft>(`/api/teacher/assignments/${id}/diagnosis-eval-fixture-draft`),
  aiQualityTrend: () => request<AiQualityTrend>("/api/teacher/ai-quality/trend"),
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
      correctionType?: "DIAGNOSIS" | "KNOWLEDGE_PATH" | "EVIDENCE" | "ADVICE";
      targetIssueId?: string | null;
      correctedKnowledgePath?: string | null;
      targetEvidenceRef?: string | null;
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

  aiStandardLibraryItems: (params?: {
    layer?: AiStandardLibraryLayer | "";
    category?: string;
    enabled?: boolean | "";
    query?: string;
  }) => request<AiStandardLibraryItem[]>(`/api/teacher/ai-standard-library/items${queryString(params || {})}`),
  aiStandardLibraryItem: (id: number) => request<AiStandardLibraryItem>(`/api/teacher/ai-standard-library/items/${id}`),
  createAiStandardLibraryItem: (payload: AiStandardLibraryItemPayload) =>
    request<AiStandardLibraryItem>("/api/teacher/ai-standard-library/items", { method: "POST", body: jsonBody(payload) }),
  updateAiStandardLibraryItem: (id: number, payload: AiStandardLibraryItemPayload) =>
    request<AiStandardLibraryItem>(`/api/teacher/ai-standard-library/items/${id}`, { method: "PUT", body: jsonBody(payload) }),
  enableAiStandardLibraryItem: (id: number) =>
    request<AiStandardLibraryItem>(`/api/teacher/ai-standard-library/items/${id}/enable`, { method: "POST" }),
  disableAiStandardLibraryItem: (id: number) =>
    request<AiStandardLibraryItem>(`/api/teacher/ai-standard-library/items/${id}/disable`, { method: "POST" }),
  aiStandardLibraryGrowthCandidates: () =>
    request<AiStandardLibraryGrowthCandidate[]>("/api/teacher/ai-standard-library/growth-candidates"),
  aiStandardLibraryGrowthGovernanceSummary: () =>
    request<AiStandardLibraryGrowthGovernanceSummary>("/api/teacher/ai-standard-library/growth-candidates/governance-summary"),
  updateAiStandardLibraryGrowthCandidate: (id: number, payload: AiStandardLibraryGrowthCandidatePayload) =>
    request<AiStandardLibraryGrowthCandidate>(`/api/teacher/ai-standard-library/growth-candidates/${id}`, { method: "PUT", body: jsonBody(payload) }),
  ignoreAiStandardLibraryGrowthCandidate: (id: number, payload?: AiStandardLibraryGrowthCandidatePayload) =>
    request<AiStandardLibraryGrowthCandidate>(`/api/teacher/ai-standard-library/growth-candidates/${id}/ignore`, { method: "POST", body: jsonBody(payload || {}) }),
  rejectAiStandardLibraryGrowthCandidate: (id: number, payload?: AiStandardLibraryGrowthCandidatePayload) =>
    request<AiStandardLibraryGrowthCandidate>(`/api/teacher/ai-standard-library/growth-candidates/${id}/reject`, { method: "POST", body: jsonBody(payload || {}) }),
  approveAiStandardLibraryGrowthCandidate: (id: number, payload?: AiStandardLibraryGrowthCandidatePayload) =>
    request<AiStandardLibraryGrowthCandidate>(`/api/teacher/ai-standard-library/growth-candidates/${id}/approve`, { method: "POST", body: jsonBody(payload || {}) }),
  mergeAiStandardLibraryGrowthCandidate: (id: number, payload?: AiStandardLibraryGrowthCandidatePayload) =>
    request<AiStandardLibraryGrowthCandidate>(`/api/teacher/ai-standard-library/growth-candidates/${id}/merge`, { method: "POST", body: jsonBody(payload || {}) }),
  informaticsKnowledgeTree: () => request<InformaticsKnowledgeNode[]>("/api/teacher/informatics-knowledge/tree"),

  executorStatus: () => request<ExecutorStatus>("/api/system/executor-status"),
  readiness: () => request<Readiness>("/api/system/readiness"),
  aiSmoke: () => request<AiSmoke>("/api/system/ai-smoke", { method: "POST" }),
  classOverview: () => request<LeaderboardEntry[]>("/api/leaderboard/problems")
};

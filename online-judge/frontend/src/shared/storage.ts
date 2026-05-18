import type { StudentProfile } from "./api/types";

const GLOBAL_STUDENT_KEY = "wzai:student";

export function saveStudent(assignmentId: number, student: StudentProfile): void {
  sessionStorage.setItem(GLOBAL_STUDENT_KEY, JSON.stringify(student));
  sessionStorage.setItem(`wzai:student:${assignmentId}`, JSON.stringify(student));
}

export function loadStudent(assignmentId?: number | null): StudentProfile | null {
  const raw = assignmentId
    ? sessionStorage.getItem(`wzai:student:${assignmentId}`) || sessionStorage.getItem(GLOBAL_STUDENT_KEY)
    : sessionStorage.getItem(GLOBAL_STUDENT_KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as StudentProfile;
    return parsed && parsed.id ? parsed : null;
  } catch {
    return null;
  }
}

export function saveDraft(problemId: number, languageId: number, sourceCode: string): void {
  localStorage.setItem(`wzai:draft:${problemId}:${languageId}`, sourceCode);
}

export function loadDraft(problemId: number, languageId: number): string | null {
  return localStorage.getItem(`wzai:draft:${problemId}:${languageId}`);
}

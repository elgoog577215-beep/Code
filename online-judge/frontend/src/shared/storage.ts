import type { StudentProfile } from "./api/types";

const GLOBAL_STUDENT_KEY = "wzai:student";
const LAST_INVITE_CODE_KEY = "wzai:lastInviteCode";
const STUDENT_CHANGE_EVENT = "wzai:student-change";

function storageArea(kind: "local" | "session"): Storage | null {
  if (typeof window === "undefined") {
    return null;
  }
  return kind === "local" ? window.localStorage : window.sessionStorage;
}

function storageGet(kind: "local" | "session", key: string): string | null {
  try {
    return storageArea(kind)?.getItem(key) ?? null;
  } catch {
    return null;
  }
}

function storageSet(kind: "local" | "session", key: string, value: string): void {
  try {
    storageArea(kind)?.setItem(key, value);
  } catch {
    // Storage can be blocked in private or embedded browser contexts.
  }
}

function storageRemove(kind: "local" | "session", key: string): void {
  try {
    storageArea(kind)?.removeItem(key);
  } catch {
    // Storage can be blocked in private or embedded browser contexts.
  }
}

function emitStudentChange(): void {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event(STUDENT_CHANGE_EVENT));
  }
}

export function saveActiveStudent(student: StudentProfile): void {
  storageSet("session", GLOBAL_STUDENT_KEY, JSON.stringify(student));
  emitStudentChange();
}

export function clearActiveStudent(): void {
  storageRemove("session", GLOBAL_STUDENT_KEY);
  emitStudentChange();
}

export function saveStudent(assignmentId: number, student: StudentProfile): void {
  saveActiveStudent(student);
  storageSet("session", `wzai:student:${assignmentId}`, JSON.stringify(student));
}

export function loadStudent(assignmentId?: number | null): StudentProfile | null {
  const keys = assignmentId ? [`wzai:student:${assignmentId}`, GLOBAL_STUDENT_KEY] : [GLOBAL_STUDENT_KEY];
  for (const key of keys) {
    const raw = storageGet("session", key);
    if (!raw) {
      continue;
    }
    try {
      const parsed = JSON.parse(raw) as StudentProfile;
      if (parsed?.id) {
        return parsed;
      }
    } catch {
      storageRemove("session", key);
    }
  }
  return null;
}

export function loadStudentToken(): string | null {
  return loadStudent()?.studentAccessToken || null;
}

export function onActiveStudentChange(listener: () => void): () => void {
  if (typeof window === "undefined") {
    return () => undefined;
  }
  window.addEventListener(STUDENT_CHANGE_EVENT, listener);
  return () => window.removeEventListener(STUDENT_CHANGE_EVENT, listener);
}

export function saveInviteCode(inviteCode: string): void {
  const normalized = inviteCode.trim().toUpperCase();
  if (normalized) {
    storageSet("local", LAST_INVITE_CODE_KEY, normalized);
  }
}

export function loadInviteCode(): string | null {
  const saved = storageGet("local", LAST_INVITE_CODE_KEY);
  return saved?.trim().toUpperCase() || null;
}

export function saveDraft(problemId: number, languageId: number, sourceCode: string): void {
  storageSet("local", `wzai:draft:${problemId}:${languageId}`, sourceCode);
}

export function loadDraft(problemId: number, languageId: number): string | null {
  return storageGet("local", `wzai:draft:${problemId}:${languageId}`);
}

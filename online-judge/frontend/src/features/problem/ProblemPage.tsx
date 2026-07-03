import { lazy, Suspense, useEffect, useMemo, useRef, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { ArrowLeft, ArrowRight, Lightbulb, Play, RefreshCw, RotateCcw, X } from "lucide-react";
import { api } from "../../shared/api/client";
import type {
  Assignment,
  AssignmentTask,
  CoachPrompt,
  Problem,
  ProblemCatalogItem,
  StudentAiFeedback,
  StudentTrajectory,
  SubmissionHistorySummary,
  SubmissionResult
} from "../../shared/api/types";
import { difficultyLabel, verdictLabel } from "../../shared/format";
import { clearDraft, loadDraft, loadStudent, saveDraft, saveLastPublicProblem } from "../../shared/storage";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea } from "../../shared/ui/Field";
import { Panel } from "../../shared/ui/Panel";
import { DifficultyPill, StatusPill, VerdictPill } from "../../shared/ui/StatusPill";
import { CONTEST_LANGUAGES, DEFAULT_CONTEST_LANGUAGE_ID, contestLanguageById } from "./languages";

const CodeEditor = lazy(() => import("./CodeEditor"));

type WorkbenchTask = {
  problemId: number;
  title: string;
  difficulty?: string | null;
  orderIndex?: number;
};

type FeedbackPollState = "idle" | "checking" | "slow" | "background" | "refreshing";

const FEEDBACK_SLOW_AFTER_MS = 30_000;
const FEEDBACK_BACKGROUND_AFTER_MS = 95_000;

function renderInline(text: string) {
  const parts = text.split(/(`[^`]+`|\*\*[^*]+\*\*)/g).filter(Boolean);
  return parts.map((part, index) => {
    if (part.startsWith("`") && part.endsWith("`")) {
      return <code key={index}>{part.slice(1, -1)}</code>;
    }
    if (part.startsWith("**") && part.endsWith("**")) {
      return <strong key={index}>{part.slice(2, -2)}</strong>;
    }
    return <span key={index}>{part}</span>;
  });
}

function renderMarkdownLike(text: string) {
  const lines = text.split(/\r?\n/);
  const nodes: React.ReactNode[] = [];
  let codeBuffer: string[] = [];
  let inCode = false;

  lines.forEach((line, index) => {
    if (line.trim().startsWith("```")) {
      if (inCode) {
        nodes.push(
          <pre className="code-block" key={`code-${index}`}>
            {codeBuffer.join("\n")}
          </pre>
        );
        codeBuffer = [];
        inCode = false;
      } else {
        inCode = true;
      }
      return;
    }
    if (inCode) {
      codeBuffer.push(line);
      return;
    }
    if (line.startsWith("### ")) {
      nodes.push(<h3 key={index}>{renderInline(line.slice(4))}</h3>);
      return;
    }
    if (line.startsWith("## ")) {
      nodes.push(<h2 key={index}>{renderInline(line.slice(3))}</h2>);
      return;
    }
    if (line.startsWith("# ")) {
      nodes.push(<h1 key={index}>{renderInline(line.slice(2))}</h1>);
      return;
    }
    if (!line.trim()) {
      return;
    }
    nodes.push(<p key={index}>{renderInline(line)}</p>);
  });

  if (codeBuffer.length) {
    nodes.push(
      <pre className="code-block" key="code-tail">
        {codeBuffer.join("\n")}
      </pre>
    );
  }
  return nodes;
}

function visibleAssignmentTitle(assignment?: Assignment | null) {
  if (!assignment) {
    return "课堂作业";
  }
  return assignment.title.includes("试点任务") ? "课堂编程作业" : assignment.title;
}

function isDiagnosticOutput(value?: string | null) {
  const text = (value || "").trim();
  if (!text) {
    return false;
  }
  return (
    text.length > 180 ||
    /(Traceback \(most recent call last\)|ValueError|TypeError|IndexError|RuntimeError|Exception|Segmentation fault|AddressSanitizer|NullPointerException|NumberFormatException|line \d+, in|error:)/i.test(text)
  );
}

function outputPreview(value?: string | null) {
  const text = (value || "").trim();
  if (!text) {
    return "(空输出)";
  }
  if (isDiagnosticOutput(text)) {
    return "程序运行时异常，展开原始错误查看细节。";
  }
  return text;
}

function taskFromAssignment(task: AssignmentTask): WorkbenchTask {
  return {
    problemId: task.problemId,
    title: task.title,
    difficulty: task.difficulty,
    orderIndex: task.orderIndex
  };
}

function taskFromCatalog(problem: ProblemCatalogItem): WorkbenchTask {
  return {
    problemId: problem.id,
    title: problem.title,
    difficulty: problem.difficulty,
    orderIndex: problem.id
  };
}

function normalizeNumber(value: string | null | undefined) {
  if (!value) {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function initialSourceFor(problem: Problem | null, problemId: number, languageId: number) {
  const draft = loadDraft(problemId, languageId);
  const languageTemplate = contestLanguageById(languageId).template;
  if (problem?.starterCode?.trim()) {
    if (draft !== null && !isLegacyStarterDraft(draft, languageTemplate)) {
      return draft;
    }
    return problem.starterCode;
  }
  if (draft !== null) {
    return draft;
  }
  return languageTemplate;
}

function defaultSourceFor(problem: Problem | null, languageId: number) {
  return problem?.starterCode?.trim() ? problem.starterCode : contestLanguageById(languageId).template;
}

function isLegacyStarterDraft(draft: string, languageTemplate: string) {
  const normalized = draft.replace(/\r\n/g, "\n").replace(/\r/g, "\n").trim();
  return (
    normalized === languageTemplate.trim() ||
    normalized === "n = int(input())\n# 在这里计算 1 到 n 的和" ||
    isPreviousPublicSeedStarterDraft(normalized)
  );
}

function isPreviousPublicSeedStarterDraft(normalized: string) {
  return (
    (
      normalized.includes("def solve_tree(n, k, values, children):") &&
      normalized.includes("current = [NEG, values[node]]") &&
      normalized.includes("return dfs(0)[k]")
    ) ||
    (
      normalized.includes("def current_cost(left, right):") &&
      normalized.includes("average = sum(merged) // len(merged)") &&
      normalized.includes("remove_value(left, right, arr[i - k])")
    ) ||
    (
      normalized.includes("def shortest(n, k, graph):") &&
      normalized.includes("best_node = [INF] * (n + 1)") &&
      normalized.includes("visited = [False] * (n + 1)")
    ) ||
    (
      normalized.includes("def greedy_schedule(n, durations, edges):") &&
      normalized.includes("while available and len(running) < 2:") &&
      normalized.includes("heapq.heappush(available, (-durations[task], task))")
    )
  );
}

function terminalFeedbackStatus(status?: string | null) {
  return ["READY", "TIMEOUT", "FAILED", "SAFETY_REJECTED"].includes(String(status || "").toUpperCase());
}

function inFlightFeedbackStatus(status?: string | null) {
  return ["GENERATING", "NOT_REQUESTED"].includes(String(status || "").toUpperCase());
}

function feedbackPollingDelay(elapsedMs: number) {
  if (elapsedMs < 12_000) {
    return 1_200;
  }
  if (elapsedMs < FEEDBACK_SLOW_AFTER_MS) {
    return 2_200;
  }
  return 5_000;
}

function feedbackTextWeight(items: Array<{ body?: string | null; title?: string | null }>) {
  return items.reduce((total, item) => total + (item.title?.length || 0) + (item.body?.length || 0), 0);
}

function FeedbackLoadingPanel({ mode, state }: { mode: "repair" | "growth"; state: FeedbackPollState }) {
  const steps =
    mode === "repair"
      ? ["读取评测点", "定位错误方向", "生成修正建议"]
      : ["分析代码结构", "寻找提升空间", "生成进阶建议"];
  const isSlow = state === "slow";
  const isBackground = state === "background";
  const title = state === "refreshing" ? "正在刷新" : isBackground ? "后台生成中" : isSlow ? "还在分析" : "正在分析";
  const note = isBackground
    ? "可以先继续修改代码，稍后点刷新 AI 拿结果。"
    : isSlow
      ? "模型响应较慢，结果不会丢，可以先看评测点。"
      : null;

  return (
    <div className="student-feedback-loading" aria-live="polite">
      <div className="student-feedback-loading__head">
        <span className="student-feedback-loading__spinner" aria-hidden="true" />
        <strong>{title}</strong>
      </div>
      {note && <p className="student-feedback-loading__note">{note}</p>}
      <div className="student-feedback-loading__steps">
        {steps.map((step, index) => (
          <span className={`is-step-${index + 1}`} key={step}>
            {step}
          </span>
        ))}
      </div>
      <div className="student-feedback-loading__bar" aria-hidden="true">
        <span />
      </div>
    </div>
  );
}

export default function ProblemPage() {
  const params = useParams();
  const [searchParams] = useSearchParams();
  const problemId = Number(params.problemId);
  const routeAssignmentParam = params.assignmentId;
  const routeAssignmentId = routeAssignmentParam && routeAssignmentParam !== "public" ? normalizeNumber(routeAssignmentParam) : null;
  const queryAssignmentId = normalizeNumber(searchParams.get("assignmentId"));
  const assignmentId = queryAssignmentId ?? routeAssignmentId;
  const isPublicWorkbench = routeAssignmentParam === "public" || !assignmentId;
  const studentFromAssignment = assignmentId ? loadStudent(assignmentId) : null;
  const studentFromAny = loadStudent();
  const studentProfileId = normalizeNumber(searchParams.get("studentProfileId")) ?? studentFromAssignment?.id ?? studentFromAny?.id ?? null;
  const recommendationToken = searchParams.get("recommendationToken");
  const backTo = isPublicWorkbench ? "/app/student/assignments/public" : "/app/student";
  const backLabel = isPublicWorkbench ? "返回题目列表" : "返回学生端";

  const [problem, setProblem] = useState<Problem | null>(null);
  const [languageId, setLanguageId] = useState(DEFAULT_CONTEST_LANGUAGE_ID);
  const [sourceCode, setSourceCode] = useState(() => contestLanguageById(DEFAULT_CONTEST_LANGUAGE_ID).template);
  const [editorResetVersion, setEditorResetVersion] = useState(0);
  const [latest, setLatest] = useState<SubmissionResult | null>(null);
  const [history, setHistory] = useState<SubmissionHistorySummary[]>([]);
  const [trajectory, setTrajectory] = useState<StudentTrajectory | null>(null);
  const [assignmentTitle, setAssignmentTitle] = useState(isPublicWorkbench ? "公共题库" : "课堂作业");
  const [workbenchTasks, setWorkbenchTasks] = useState<WorkbenchTask[]>([]);
  const [tasksLoading, setTasksLoading] = useState(false);
  const [resultOpen, setResultOpen] = useState(false);
  const [coachPrompt, setCoachPrompt] = useState<CoachPrompt | null>(null);
  const [coachAnswer, setCoachAnswer] = useState("");
  const [alert, setAlert] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const [studentAiFeedback, setStudentAiFeedback] = useState<StudentAiFeedback | null>(null);
  const [feedbackPollState, setFeedbackPollState] = useState<FeedbackPollState>("idle");
  const [coachBusy, setCoachBusy] = useState(false);
  const [coachReplyBusy, setCoachReplyBusy] = useState(false);
  const viewedFeedbackIdsRef = useRef<Set<number>>(new Set());
  const feedbackPollTokenRef = useRef(0);
  const feedbackTimerRef = useRef<number | null>(null);
  const resultOpenRef = useRef(false);

  useEffect(() => {
    resultOpenRef.current = resultOpen;
  }, [resultOpen]);

  useEffect(() => {
    return () => {
      feedbackPollTokenRef.current += 1;
      clearFeedbackTimer();
    };
  }, []);

  useEffect(() => {
    if (isPublicWorkbench && Number.isFinite(problemId)) {
      saveLastPublicProblem(problemId);
    }
  }, [isPublicWorkbench, problemId]);

  useEffect(() => {
    async function load() {
      try {
        setLatest(null);
        setResultOpen(false);
        setCoachPrompt(null);
        setCoachAnswer("");
        setAlert(null);
        feedbackPollTokenRef.current += 1;
        clearFeedbackTimer();
        setFeedbackPollState("idle");
        setStudentAiFeedback(null);
        const [problemResult, historyResult] = await Promise.all([api.problem(problemId), api.history(problemId)]);
        setProblem(problemResult);
        setHistory(historyResult);
        setSourceCode(initialSourceFor(problemResult, problemId, languageId));
      } catch (error) {
        setAlert({ type: "error", message: error instanceof Error ? error.message : "题目加载失败。" });
      }
    }
    if (Number.isFinite(problemId)) {
      void load();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [problemId]);

  useEffect(() => {
    setSourceCode(initialSourceFor(problem, problemId, languageId));
  }, [languageId, problem, problemId]);

  useEffect(() => {
    let ignore = false;
    setTasksLoading(true);

    if (isPublicWorkbench) {
      api.problemCatalog()
        .then(result => {
          if (!ignore) {
            setAssignmentTitle("公共题库");
            setWorkbenchTasks(result.map(taskFromCatalog));
          }
        })
        .catch(() => {
          if (!ignore) {
            setWorkbenchTasks([]);
          }
        })
        .finally(() => {
          if (!ignore) {
            setTasksLoading(false);
          }
        });
      return () => {
        ignore = true;
      };
    }

    if (!assignmentId) {
      setWorkbenchTasks([]);
      setTasksLoading(false);
      return () => {
        ignore = true;
      };
    }

    const currentAssignmentId = assignmentId;

    async function loadAssignmentTasks() {
      try {
        let assignment: Assignment | null = null;
        if (studentProfileId) {
          const assignments = await api.studentAssignments(studentProfileId);
          assignment = assignments.find(item => item.id === currentAssignmentId) || null;
        }
        if (!ignore) {
          if (assignment) {
            setAssignmentTitle(visibleAssignmentTitle(assignment));
            setWorkbenchTasks(assignment.tasks.map(taskFromAssignment));
          } else {
            setAssignmentTitle("课堂作业");
            setWorkbenchTasks([]);
          }
        }
      } catch {
        if (!ignore) {
          setWorkbenchTasks([]);
        }
      } finally {
        if (!ignore) {
          setTasksLoading(false);
        }
      }
    }

    void loadAssignmentTasks();
    return () => {
      ignore = true;
    };
  }, [assignmentId, isPublicWorkbench, studentProfileId]);

  useEffect(() => {
    if (!assignmentId || !studentProfileId) {
      setTrajectory(null);
      return;
    }
    void api.studentTrajectory(assignmentId, studentProfileId).then(setTrajectory).catch(() => undefined);
  }, [assignmentId, studentProfileId, latest]);

  useEffect(() => {
    if (!studentProfileId || !recommendationToken) {
      return;
    }
    void api.recordRecommendationEvent(studentProfileId, recommendationToken, "ENTERED_PROBLEM");
  }, [studentProfileId, recommendationToken]);

  useEffect(() => {
    setCoachPrompt(null);
    setCoachAnswer("");
    if (!latest?.id || studentAiFeedback?.status !== "READY" || studentAiFeedback.source !== "MODEL") {
      return;
    }
    void api.coachPrompt(latest.id).then(result => setCoachPrompt(result || null)).catch(() => undefined);
  }, [latest?.id, studentAiFeedback?.status, studentAiFeedback?.source]);

  useEffect(() => {
    if (!resultOpen || !latest?.id || studentAiFeedback?.status !== "READY" || studentAiFeedback.source !== "MODEL") {
      return;
    }
    if (viewedFeedbackIdsRef.current.has(latest.id)) {
      return;
    }
    viewedFeedbackIdsRef.current.add(latest.id);
    void api.recordStudentAiFeedbackView(latest.id).catch(() => undefined);
  }, [resultOpen, latest?.id, studentAiFeedback?.status, studentAiFeedback?.source]);

  const trajectoryTaskByProblemId = useMemo(() => {
    const tasks = new Map<number, StudentTrajectory["tasks"][number]>();
    trajectory?.tasks.forEach(task => tasks.set(task.problemId, task));
    return tasks;
  }, [trajectory]);

  const taskRows = useMemo(
    () =>
      workbenchTasks.map(task => {
        const state = trajectoryTaskByProblemId.get(task.problemId);
        return {
          ...task,
          attemptCount: state?.attemptCount || 0,
          passed: Boolean(state?.passed || state?.latestVerdict === "ACCEPTED"),
          latestVerdict: state?.latestVerdict || null,
          latestHint: state?.latestHint || state?.latestProgressSignal || null
        };
      }),
    [trajectoryTaskByProblemId, workbenchTasks]
  );

  const currentTaskIndex = taskRows.findIndex(task => task.problemId === problemId);
  const nextTask = currentTaskIndex >= 0 ? taskRows[currentTaskIndex + 1] || null : taskRows.find(task => task.problemId !== problemId) || null;

  function buildTaskLink(nextProblemId: number) {
    const studentParam = studentProfileId ? `?studentProfileId=${studentProfileId}` : "";
    if (assignmentId) {
      return `/app/student/assignments/${assignmentId}/problems/${nextProblemId}${studentParam}`;
    }
    return `/app/student/assignments/public/problems/${nextProblemId}${studentParam}`;
  }

  function updateCode(value: string) {
    setSourceCode(value);
    if (Number.isFinite(problemId)) {
      saveDraft(problemId, languageId, value);
    }
  }

  function resetCode() {
    setSourceCode(defaultSourceFor(problem, languageId));
    if (Number.isFinite(problemId)) {
      clearDraft(problemId, languageId);
    }
    setEditorResetVersion(version => version + 1);
  }

  async function submit() {
    if (!problem) {
      return;
    }
    if (!sourceCode.trim()) {
      setAlert({ type: "error", message: "请先写代码再提交。" });
      return;
    }
    setBusy(true);
    try {
      const result = await api.submit({
        problemId: problem.id,
        assignmentId,
        studentProfileId,
        recommendationToken,
        languageId,
        sourceCode
      });
      setLatest(result);
      setResultOpen(true);
      setCoachPrompt(null);
      setStudentAiFeedback(null);
      const feedbackToken = startFeedbackPollingState(Boolean(result.id));
      setHistory(await api.history(problem.id));
      setAlert(
        result.verdict === "INTERNAL_ERROR"
          ? { type: "error", message: result.errorMessage || "执行环境未就绪。" }
          : null
      );
      if (result.id) {
        void api
          .triggerStudentAiFeedback(result.id)
          .then(lookup => handleFeedbackLookup(lookup.feedback || null, feedbackToken))
          .catch(() => undefined);
        pollStudentAiFeedback(result.id, feedbackToken);
      }
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "提交失败。" });
    } finally {
      setBusy(false);
    }
  }

  function clearFeedbackTimer() {
    if (feedbackTimerRef.current !== null) {
      window.clearTimeout(feedbackTimerRef.current);
      feedbackTimerRef.current = null;
    }
  }

  function startFeedbackPollingState(active: boolean) {
    feedbackPollTokenRef.current += 1;
    clearFeedbackTimer();
    setFeedbackPollState(active ? "checking" : "idle");
    return feedbackPollTokenRef.current;
  }

  function handleFeedbackLookup(feedback: StudentAiFeedback | null, token: number) {
    if (token !== feedbackPollTokenRef.current) {
      return true;
    }
    if (feedback) {
      setStudentAiFeedback(feedback);
    }
    if (feedback && terminalFeedbackStatus(feedback.status)) {
      setFeedbackPollState("idle");
      if (feedback.status === "READY" && resultOpenRef.current) {
        setResultOpen(true);
      }
      return true;
    }
    return false;
  }

  function pollStudentAiFeedback(id: number, token: number, startedAt = Date.now()) {
    const elapsedMs = Date.now() - startedAt;
    const delay = elapsedMs <= 0 ? 350 : feedbackPollingDelay(elapsedMs);
    feedbackTimerRef.current = window.setTimeout(async () => {
      if (token !== feedbackPollTokenRef.current) {
        return;
      }
      try {
        const lookup = await api.studentAiFeedback(id);
        const feedback = lookup.feedback || null;
        if (handleFeedbackLookup(feedback, token)) {
          return;
        }
      } catch {
        // AI 快反馈失败不阻塞学生继续修改代码。
      }
      if (token !== feedbackPollTokenRef.current) {
        return;
      }
      const nextElapsedMs = Date.now() - startedAt;
      if (nextElapsedMs >= FEEDBACK_BACKGROUND_AFTER_MS) {
        setFeedbackPollState("background");
      } else {
        setFeedbackPollState(nextElapsedMs >= FEEDBACK_SLOW_AFTER_MS ? "slow" : "checking");
      }
      pollStudentAiFeedback(id, token, startedAt);
    }, delay);
  }

  async function refreshStudentAiFeedback() {
    if (!latest?.id) {
      return;
    }
    const shouldRetry =
      studentAiFeedback?.source === "MODEL" && terminalFeedbackStatus(studentAiFeedback.status) && studentAiFeedback.status !== "READY";
    const feedbackToken = startFeedbackPollingState(true);
    setFeedbackPollState("refreshing");
    try {
      const lookup = shouldRetry ? await api.triggerStudentAiFeedback(latest.id) : await api.studentAiFeedback(latest.id);
      const feedback = lookup.feedback || null;
      if (handleFeedbackLookup(feedback, feedbackToken)) {
        return;
      }
      setFeedbackPollState(inFlightFeedbackStatus(feedback?.status) ? "checking" : "background");
      pollStudentAiFeedback(latest.id, feedbackToken);
    } catch {
      if (feedbackToken === feedbackPollTokenRef.current) {
        setFeedbackPollState("background");
      }
    }
  }

  async function generateCoachPrompt() {
    if (!latest?.id) {
      return;
    }
    if (studentAiFeedback?.status !== "READY" || studentAiFeedback.source !== "MODEL") {
      setAlert({ type: "error", message: "AI 反馈生成后才能追问。" });
      return;
    }
    setCoachBusy(true);
    try {
      setCoachPrompt(await api.generateCoachPrompt(latest.id));
      setCoachAnswer("");
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "追问生成失败。" });
    } finally {
      setCoachBusy(false);
    }
  }

  async function replyCoachPrompt() {
    if (!latest?.id || !coachPrompt) {
      return;
    }
    if (!coachAnswer.trim()) {
      setAlert({ type: "error", message: "先写一句你的判断或验证样例。" });
      return;
    }
    setCoachReplyBusy(true);
    try {
      setCoachPrompt(await api.replyCoachPrompt(latest.id, coachAnswer));
      setCoachAnswer("");
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "回答提交失败。" });
    } finally {
      setCoachReplyBusy(false);
    }
  }

  if (!problem) {
    return <EmptyState title="正在加载题目" live />;
  }

  const passed = latest?.testCaseResults?.filter(item => item.passed).length || 0;
  const total = latest?.testCaseResults?.length || 0;
  const firstFailedCase = latest?.testCaseResults?.find(item => !item.passed) || null;
  const modelFeedbackReady = studentAiFeedback?.status === "READY" && studentAiFeedback.source === "MODEL";
  const repairViewItems = modelFeedbackReady ? studentAiFeedback.repairItems?.filter(item => item.body || item.title) || [] : [];
  const improvementViewItems = modelFeedbackReady ? studentAiFeedback.improvementItems?.filter(item => item.body || item.title) || [] : [];
  const studentReport = modelFeedbackReady ? studentAiFeedback.studentReport || null : null;
  const basicReportText = studentReport?.basicLayerText?.trim() || "";
  const improvementReportText = studentReport?.improvementLayerText?.trim() || "";
  const nextActionReportText = studentReport?.nextActionText?.trim() || "";
  const feedbackStatus = String(studentAiFeedback?.status || "").toUpperCase();
  const isFeedbackWaiting = Boolean(
    latest &&
      feedbackPollState !== "idle" &&
      (!studentAiFeedback || inFlightFeedbackStatus(feedbackStatus))
  );
  const isFeedbackBackground = Boolean(latest && feedbackPollState === "background" && (!studentAiFeedback || inFlightFeedbackStatus(feedbackStatus)));
  const feedbackFailed = Boolean(
    latest &&
      studentAiFeedback &&
      ["TIMEOUT", "FAILED", "SAFETY_REJECTED"].includes(String(studentAiFeedback.status))
  );
  const feedbackFallbackMessage = studentAiFeedback?.source === "RULE_FALLBACK"
    ? "外部 AI 暂不可用，已使用规则诊断。"
    : "AI 暂未生成";
  const testCaseSummary = total ? `${passed}/${total} 测试点` : "等待评测";
  const feedbackReady = Boolean(latest);
  const nextTaskLink = nextTask ? buildTaskLink(nextTask.problemId) : null;
  const passedLatest = latest?.verdict === "ACCEPTED";
  const lastResultText = modelFeedbackReady
    ? "AI 已生成"
    : isFeedbackBackground
      ? "AI 后台生成中"
      : isFeedbackWaiting
        ? "AI 分析中"
        : testCaseSummary;
  const selectedLanguage = contestLanguageById(languageId);
  const draftChanged = sourceCode !== defaultSourceFor(problem, languageId);
  const codeLineCount = sourceCode ? sourceCode.split(/\r?\n/).length : 0;
  const repairCheckQuestion = modelFeedbackReady ? nextActionReportText || studentAiFeedback.nextQuestion || "" : "";
  const showRepairSection =
    isFeedbackWaiting ||
    isFeedbackBackground ||
    feedbackFailed ||
    Boolean(basicReportText) ||
    repairViewItems.length > 0 ||
    Boolean(repairCheckQuestion) ||
    Boolean(coachPrompt);
  const showGrowthSection = isFeedbackWaiting || isFeedbackBackground || feedbackFailed || Boolean(improvementReportText) || improvementViewItems.length > 0;
  const showFeedbackRefreshAction = Boolean(
    latest && (isFeedbackBackground || feedbackFailed || feedbackPollState === "slow" || feedbackPollState === "refreshing")
  );
  const feedbackRefreshLabel = feedbackPollState === "refreshing" ? "刷新中" : feedbackFailed ? "重试 AI" : "刷新 AI";
  const feedbackCompleted = Boolean(latest && studentAiFeedback?.status === "READY");
  const showFeedbackDock = Boolean(
    feedbackReady && !resultOpen && latest && (isFeedbackWaiting || isFeedbackBackground || feedbackCompleted || feedbackFailed)
  );
  const feedbackDockTone = feedbackFailed ? "is-failed" : feedbackCompleted ? "is-ready" : "is-running";
  const feedbackDockTitle = feedbackFailed
    ? "AI 分析未完成"
    : feedbackCompleted
      ? "AI 分析完成"
      : isFeedbackBackground
        ? "AI 后台分析中"
        : "AI 正在分析";
  const feedbackDockNote = feedbackFailed
    ? "点开结果页重试"
    : feedbackCompleted
      ? "点开查看建议"
      : isFeedbackBackground
        ? "可以继续写代码"
        : "关闭弹窗后仍会继续";
  const rawJudgeOutputs = Array.from(
    new Set(
      [
        latest?.errorMessage,
        latest?.compileOutput,
        firstFailedCase && isDiagnosticOutput(firstFailedCase.actualOutput) ? firstFailedCase.actualOutput : null
      ]
        .map(item => (item || "").trim())
        .filter(Boolean)
    )
  );
  const hasRawJudgeOutput = rawJudgeOutputs.length > 0;
  const judgeTextWeight =
    total * 36 +
    rawJudgeOutputs.reduce((sum, item) => sum + item.length, 0) +
    (firstFailedCase && !firstFailedCase.hidden
      ? (firstFailedCase.expectedOutput?.length || 0) + (firstFailedCase.actualOutput?.length || 0) + 90
      : 0);
  const repairTextWeight = feedbackTextWeight(repairViewItems) + basicReportText.length + repairCheckQuestion.length + (coachPrompt?.question?.length || 0);
  const growthTextWeight = feedbackTextWeight(improvementViewItems) + improvementReportText.length;
  const resultLayoutMode = isFeedbackWaiting
    ? "waiting"
    : judgeTextWeight > Math.max(520, repairTextWeight + growthTextWeight)
      ? "judge-heavy"
      : repairTextWeight > Math.max(420, growthTextWeight * 1.35)
        ? "repair-heavy"
        : repairTextWeight + growthTextWeight > Math.max(520, judgeTextWeight * 1.18)
          ? "ai-heavy"
          : "balanced";
  const coachQuestionBlock = (
    <div className="coach-next-question">
      {modelFeedbackReady ? (
        coachPrompt ? (
          <>
            <strong>{coachPrompt.question}</strong>
            {coachPrompt.contextSummary && <p>{coachPrompt.contextSummary}</p>}
            {coachPrompt.rationale && <p>{coachPrompt.rationale}</p>}
            {coachPrompt.turns?.length ? (
              <div className="coach-turn-list">
                {coachPrompt.turns.map(turn => (
                  <div className="coach-turn" key={turn.id}>
                    <span>第 {turn.turnIndex || 1} 轮</span>
                    <strong>{turn.question}</strong>
                    {turn.studentAnswer && <p>我的回答：{turn.studentAnswer}</p>}
                    {turn.coachFeedback && <p>反馈：{turn.coachFeedback}</p>}
                  </div>
                ))}
              </div>
            ) : null}
            {!coachPrompt.studentAnswer && (
              <div className="coach-reply-box">
                <TextArea
                  value={coachAnswer}
                  onChange={event => setCoachAnswer(event.target.value)}
                  rows={3}
                  maxLength={1200}
                  placeholder="写下你的判断、最小样例、变量变化或复杂度估算。"
                />
                <Button type="button" variant="primary" onClick={() => void replyCoachPrompt()} disabled={coachReplyBusy}>
                  {coachReplyBusy ? "提交中" : "提交回答"}
                </Button>
              </div>
            )}
          </>
        ) : (
          null
        )
      ) : null}
      {modelFeedbackReady && (
        <Button
          type="button"
          variant="secondary"
          onClick={() => void generateCoachPrompt()}
          disabled={coachBusy}
          icon={<Lightbulb size={16} />}
        >
          {coachBusy ? "稍等" : coachPrompt ? "换一问" : "给我一问"}
        </Button>
      )}
    </div>
  );

  return (
    <div className="stack problem-page problem-workbench">
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="problem-layout problem-layout--workbench">
        <aside className="problem-task-sidebar" aria-label="题目列表">
          <Link to={backTo} className="problem-back-link">
            <ArrowLeft size={14} /> {backLabel}
          </Link>
          <div className="problem-task-sidebar__head">
            <div>
              <strong>{assignmentTitle}</strong>
            </div>
            <StatusPill tone={trajectory ? "success" : "neutral"}>
              {trajectory ? `${trajectory.completedTasks}/${trajectory.totalTasks}` : tasksLoading ? "加载中" : `${taskRows.length} 题`}
            </StatusPill>
          </div>
          <div className="problem-task-list">
            {tasksLoading ? (
              <EmptyState title="加载中" />
            ) : (
              taskRows.map((task, index) => (
                <Link
                  className={`problem-task-item ${task.problemId === problemId ? "is-active" : ""} ${task.passed ? "is-passed" : ""}`}
                  to={buildTaskLink(task.problemId)}
                  key={task.problemId}
                >
                  <span className="problem-task-item__index">{index + 1}</span>
                  <span className="problem-task-item__main">
                    <strong>{task.title}</strong>
                    <small>{difficultyLabel(task.difficulty)}</small>
                  </span>
                  {task.latestVerdict ? <VerdictPill verdict={task.latestVerdict} /> : task.passed ? <StatusPill tone="success">已过</StatusPill> : null}
                </Link>
              ))
            )}
          </div>
        </aside>

        <Panel
          title={problem.title}
          className="panel--statement"
          action={<DifficultyPill difficulty={problem.difficulty} />}
          description={
            <span className="problem-statement-meta">
              <span>{assignmentTitle}</span>
              <span>{problem.timeLimit} ms</span>
              <span>{Math.round(problem.memoryLimit / 1024)} MB</span>
            </span>
          }
        >
          <div className="statement">{renderMarkdownLike(problem.description)}</div>
          <details className="problem-compact-details problem-sample-drawer" open={problem.sampleTestCases.length <= 1}>
            <summary>
              <span>公开样例</span>
              {problem.sampleTestCases.length ? <span className="meta-badge">{problem.sampleTestCases.length}</span> : null}
            </summary>
            <div className="problem-sample-drawer__body">
              {problem.sampleTestCases.length ? (
                problem.sampleTestCases.map((sample, index) => (
                  <div className="two-column" key={`${sample.input}-${index}`}>
                    <div>
                      <span className="code-label">输入 {index + 1}</span>
                      <pre className="code-block">{sample.input || "(空输入)"}</pre>
                    </div>
                    <div>
                      <span className="code-label">输出 {index + 1}</span>
                      <pre className="code-block">{sample.expectedOutput || "(空输出)"}</pre>
                    </div>
                  </div>
                ))
              ) : null}
            </div>
          </details>
          <a href="#code-workbench" className="problem-mobile-jump ui-button ui-button--primary">
            <Play size={16} />
            <span>去写代码</span>
          </a>
        </Panel>

        <Panel
          title="代码"
          className="panel--editor"
          action={
            <Field label="语言">
              <Select value={languageId} onChange={event => setLanguageId(Number(event.target.value))}>
                {CONTEST_LANGUAGES.map(language => (
                  <option value={language.id} key={language.id}>
                    {language.label}
                  </option>
                ))}
              </Select>
            </Field>
          }
        >
          <div className="stack" id="code-workbench">
            <div className="editor-shell">
              <div className="editor-toolbar" aria-label="编辑器状态">
                <span>{selectedLanguage.sourceFileName}</span>
                <strong>{draftChanged ? `已保存草稿 · ${codeLineCount} 行` : "初始代码"}</strong>
              </div>
              <Suspense fallback={<div className="editor-loading">正在准备代码编辑器</div>}>
                <CodeEditor key={`${problem.id}-${languageId}-${editorResetVersion}`} languageId={languageId} sourceCode={sourceCode} onChange={updateCode} />
              </Suspense>
            </div>
            {latest && (
              <button type="button" className="problem-last-result" onClick={() => setResultOpen(true)}>
                <span>
                  <VerdictPill verdict={latest.verdict} />
                  <span>{lastResultText}</span>
                </span>
                <strong>查看结果</strong>
              </button>
            )}
            <div className="actions">
              <Button type="button" variant="primary" onClick={() => void submit()} disabled={busy} icon={<Play size={18} />}>
                {busy ? "提交中" : "提交代码"}
              </Button>
              <Button
                type="button"
                variant="ghost"
                onClick={resetCode}
                icon={<RotateCcw size={18} />}
              >
                恢复初始代码
              </Button>
            </div>
          </div>
        </Panel>
      </section>

      {showFeedbackDock && (
        <button
          type="button"
          className={`problem-feedback-dock ${feedbackDockTone}`}
          onClick={() => setResultOpen(true)}
          aria-live="polite"
        >
          <span className="problem-feedback-dock__mark" aria-hidden="true">
            <RefreshCw size={18} />
          </span>
          <span className="problem-feedback-dock__body">
            <strong>{feedbackDockTitle}</strong>
            <small>{feedbackDockNote}</small>
          </span>
        </button>
      )}

      {feedbackReady && resultOpen && latest && (
        <div className="problem-result-modal-backdrop" role="presentation">
          <section className="problem-result-modal" role="dialog" aria-modal="true" aria-labelledby="problem-result-title">
            <div className="problem-result-modal__header">
              <div>
                <h2 id="problem-result-title">{verdictLabel(latest.verdict)}</h2>
              </div>
              <div className="problem-result-modal__status">
                <StatusPill tone={latest.verdict === "ACCEPTED" ? "success" : "warning"}>{testCaseSummary}</StatusPill>
                {(isFeedbackWaiting || isFeedbackBackground) && (
                  <StatusPill tone="neutral">{isFeedbackBackground ? "后台生成中" : feedbackPollState === "slow" ? "AI 较慢" : "AI 分析中"}</StatusPill>
                )}
                <button type="button" aria-label="关闭结果" onClick={() => setResultOpen(false)}>
                  <X size={18} />
                </button>
              </div>
            </div>

            <div className="problem-result-modal__body">
              <div className={`problem-result-modal__grid problem-result-modal__grid--${resultLayoutMode}`}>
                <section className="problem-result-section problem-result-section--tests">
                  <div className="problem-result-section__head">
                    <h3>评测</h3>
                    <strong>{total ? `${passed}/${total}` : "-"}</strong>
                  </div>
                  {latest.testCaseResults?.length ? (
                    <div className="testcase-compact-list">
                      {latest.testCaseResults.map(item => (
                        <div className={item.passed ? "is-passed" : ""} key={item.testCaseNumber}>
                          <span>{item.testCaseNumber}</span>
                          <strong>{item.hidden ? "隐藏" : "公开"}</strong>
                          <small>{item.executionTime ?? "-"} ms</small>
                          <em>{item.passed ? "过" : "错"}</em>
                        </div>
                      ))}
                    </div>
                  ) : null}
                  {firstFailedCase && !firstFailedCase.hidden ? (
                    <details className="problem-compact-details failed-case-card">
                      <summary>
                        <span>首个失败点</span>
                      </summary>
                      <div className="failed-case-card__body">
                        <div>
                          <strong>期望</strong>
                          <pre className="code-block">{outputPreview(firstFailedCase.expectedOutput)}</pre>
                        </div>
                        <div>
                          <strong>实际</strong>
                          <pre className="code-block">{outputPreview(firstFailedCase.actualOutput)}</pre>
                        </div>
                      </div>
                    </details>
                  ) : firstFailedCase?.hidden ? (
                    <div className="problem-test-note">隐藏点未过</div>
                  ) : null}
                  {hasRawJudgeOutput && (
                    <details className="problem-compact-details problem-raw-output-drawer">
                      <summary>
                      <span>原始错误</span>
                      </summary>
                      <div className="problem-raw-output-drawer__body">
                        {rawJudgeOutputs.map((item, index) => (
                          <pre className="code-block" key={`${item.slice(0, 24)}-${index}`}>
                            {item}
                          </pre>
                        ))}
                      </div>
                    </details>
                  )}
                </section>

                {showRepairSection ? (
                  <section className="problem-result-section problem-result-section--repair">
                    <div className="problem-result-section__head">
                      <h3>修正建议</h3>
                    </div>
                    {isFeedbackWaiting || isFeedbackBackground ? (
                      <FeedbackLoadingPanel mode="repair" state={feedbackPollState} />
                    ) : feedbackFailed ? (
                      <div className="student-feedback-empty">{feedbackFallbackMessage}</div>
                    ) : basicReportText ? (
                      <article className="student-feedback-report student-feedback-report--basic">
                        <p>{basicReportText}</p>
                      </article>
                    ) : repairViewItems.length ? (
                      <div className="student-feedback-list">
                        {repairViewItems.slice(0, 1).map((item, index) => (
                          <article className="student-feedback-item student-feedback-item--primary" key={`${item.kind || "repair"}-${index}`}>
                            {item.title && <strong>{item.title}</strong>}
                            {item.body && <p>{item.body}</p>}
                          </article>
                        ))}
                      </div>
                    ) : null}

                    {modelFeedbackReady && repairCheckQuestion ? (
                      <section className="student-feedback-next" aria-label="下一步">
                        <p>{repairCheckQuestion}</p>
                      </section>
                    ) : null}

                    {modelFeedbackReady && coachPrompt ? (
                      <section className="problem-feedback-coach" aria-label="追问">
                        {coachQuestionBlock}
                      </section>
                    ) : null}
                  </section>
                ) : null}

                {showGrowthSection ? (
                  <section className="problem-result-section problem-result-section--growth">
                    <div className="problem-result-section__head">
                      <h3>提升建议</h3>
                    </div>
                    {isFeedbackWaiting || isFeedbackBackground ? (
                      <FeedbackLoadingPanel mode="growth" state={feedbackPollState} />
                    ) : feedbackFailed ? (
                      <div className="student-feedback-empty">{feedbackFallbackMessage}</div>
                    ) : improvementReportText ? (
                      <article className="student-feedback-report student-feedback-report--growth">
                        <p>{improvementReportText}</p>
                      </article>
                    ) : improvementViewItems.length ? (
                      <div className="student-feedback-list">
                        {improvementViewItems.slice(0, 3).map((item, index) => (
                          <article className="student-feedback-item student-feedback-item--growth" key={`${item.kind || "improvement"}-${index}`}>
                            {item.title && <span>{item.title}</span>}
                            {item.body && <strong>{item.body}</strong>}
                          </article>
                        ))}
                      </div>
                    ) : null}
                  </section>
                ) : null}
              </div>
            </div>

            <div className="problem-result-modal__footer">
              {showFeedbackRefreshAction && (
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => void refreshStudentAiFeedback()}
                  disabled={feedbackPollState === "refreshing"}
                  icon={<RefreshCw size={16} />}
                >
                  {feedbackRefreshLabel}
                </Button>
              )}
              <Button type="button" variant="primary" onClick={() => setResultOpen(false)}>
                {passedLatest && nextTaskLink ? "留在本题" : "继续修改"}
              </Button>
              {nextTaskLink && (
                <ButtonLink to={nextTaskLink} variant={passedLatest ? "primary" : "secondary"} icon={<ArrowRight size={16} />}>
                  下一题
                </ButtonLink>
              )}
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

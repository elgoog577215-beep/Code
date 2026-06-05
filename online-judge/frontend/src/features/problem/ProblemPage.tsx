import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { ArrowLeft, ArrowRight, Lightbulb, Play, RotateCcw, X } from "lucide-react";
import { api } from "../../shared/api/client";
import type {
  Assignment,
  AssignmentTask,
  CoachPrompt,
  Problem,
  ProblemCatalogItem,
  StudentTrajectory,
  SubmissionHistorySummary,
  SubmissionResult
} from "../../shared/api/types";
import { difficultyLabel, formatDateTime, issueLabel, learningStageLabel, verdictLabel } from "../../shared/format";
import { loadDraft, loadStudent, saveDraft } from "../../shared/storage";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea } from "../../shared/ui/Field";
import { Panel } from "../../shared/ui/Panel";
import { DifficultyPill, StatusPill, VerdictPill } from "../../shared/ui/StatusPill";

const PYTHON_TEMPLATE = "n = int(input())\nprint(n)\n";
const CPP_TEMPLATE = "#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    ios::sync_with_stdio(false);\n    cin.tie(nullptr);\n\n    int n;\n    cin >> n;\n    cout << n << '\\n';\n    return 0;\n}\n";
const CodeEditor = lazy(() => import("./CodeEditor"));

type WorkbenchTask = {
  problemId: number;
  title: string;
  difficulty?: string | null;
  orderIndex?: number;
};

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

function improvementLabel(category?: string | null) {
  switch ((category || "").toUpperCase()) {
    case "COMPLEXITY":
      return "复杂度";
    case "TESTING_HABIT":
      return "测试习惯";
    case "CODE_CLARITY":
      return "代码清晰度";
    case "BOUNDARY_AWARENESS":
      return "边界意识";
    case "ROBUSTNESS":
      return "鲁棒性";
    case "DEBUG_CLEANUP":
      return "调试清理";
    default:
      return "继续提升";
  }
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
  const backTo = "/app/student";

  const [problem, setProblem] = useState<Problem | null>(null);
  const [languageId, setLanguageId] = useState(71);
  const [sourceCode, setSourceCode] = useState(PYTHON_TEMPLATE);
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
  const [coachBusy, setCoachBusy] = useState(false);
  const [coachReplyBusy, setCoachReplyBusy] = useState(false);

  useEffect(() => {
    async function load() {
      try {
        setLatest(null);
        setResultOpen(false);
        setCoachPrompt(null);
        setCoachAnswer("");
        setAlert(null);
        const [problemResult, historyResult] = await Promise.all([api.problem(problemId), api.history(problemId)]);
        setProblem(problemResult);
        setHistory(historyResult);
        const draft = loadDraft(problemId, languageId);
        setSourceCode(draft || (languageId === 54 ? CPP_TEMPLATE : PYTHON_TEMPLATE));
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
    const draft = loadDraft(problemId, languageId);
    setSourceCode(draft || (languageId === 54 ? CPP_TEMPLATE : PYTHON_TEMPLATE));
  }, [languageId, problemId]);

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

    async function loadAssignmentTasks() {
      try {
        let assignment: Assignment | null = null;
        if (studentProfileId) {
          const assignments = await api.studentAssignments(studentProfileId);
          assignment = assignments.find(item => item.id === assignmentId) || null;
        }
        if (!assignment) {
          assignment = await api.assignment(assignmentId);
        }
        if (!ignore) {
          setAssignmentTitle(visibleAssignmentTitle(assignment));
          setWorkbenchTasks(assignment.tasks.map(taskFromAssignment));
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
    if (!latest?.id || !latest.analysis) {
      return;
    }
    void api.coachPrompt(latest.id).then(result => setCoachPrompt(result || null)).catch(() => undefined);
  }, [latest?.id, latest?.analysis]);

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
      setHistory(await api.history(problem.id));
      setAlert(
        result.verdict === "INTERNAL_ERROR"
          ? { type: "error", message: result.errorMessage || "执行环境未就绪。" }
          : null
      );
      if (!result.analysis && result.id) {
        void api.triggerAnalysis(result.id).then(() => pollSubmission(result.id)).catch(() => undefined);
      }
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "提交失败。" });
    } finally {
      setBusy(false);
    }
  }

  async function pollSubmission(id: number) {
    window.setTimeout(async () => {
      try {
        const refreshed = await api.submission(id);
        setLatest(refreshed);
        setResultOpen(true);
      } catch {
        // 反馈生成失败不阻塞学生继续练习。
      }
    }, 1800);
  }

  async function generateCoachPrompt() {
    if (!latest?.id) {
      return;
    }
    if (!latest.analysis) {
      setAlert({ type: "error", message: "请先等待本次反馈生成后再追问。" });
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
    return <EmptyState title="正在加载题目" />;
  }

  const passed = latest?.testCaseResults?.filter(item => item.passed).length || 0;
  const total = latest?.testCaseResults?.length || 0;
  const firstFailedCase = latest?.testCaseResults?.find(item => !item.passed) || latest?.analysis?.firstFailedCase || null;
  const studentFeedback = latest?.analysis?.studentFeedback || null;
  const primaryBlockingIssue = studentFeedback?.blockingIssues?.[0] || null;
  const nextLearningAction = studentFeedback?.nextLearningAction || null;
  const blockingIssues = studentFeedback?.blockingIssues?.length ? studentFeedback.blockingIssues : [];
  const improvementOpportunities = studentFeedback?.improvementOpportunities?.length ? studentFeedback.improvementOpportunities : [];
  const focusText =
    nextLearningAction?.task ||
    primaryBlockingIssue?.nextAction ||
    primaryBlockingIssue?.studentMessage ||
    studentFeedback?.summary ||
    latest?.analysis?.studentHint ||
    latest?.analysis?.fixDirections?.[0] ||
    (trajectory?.nextStep ? learningStageLabel(trajectory.nextStep) : "") ||
    "";
  const testCaseSummary = total ? `${passed}/${total} 测试点` : "等待评测";
  const feedbackReady = Boolean(latest);
  const nextTaskLink = nextTask ? buildTaskLink(nextTask.problemId) : null;
  const lastResultText = focusText || testCaseSummary;
  const repairFallbacks = [
    latest?.analysis?.studentHint,
    ...(latest?.analysis?.fixDirections?.slice(0, 2) || [])
  ].filter((item): item is string => Boolean(item));
  const hasRepairGuidance = Boolean(blockingIssues.length || repairFallbacks.length || nextLearningAction || latest?.analysis);
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
  const coachQuestionBlock = (
    <div className="coach-next-question">
      {latest?.analysis ? (
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
      {latest?.analysis && (
        <Button
          type="button"
          variant="secondary"
          onClick={() => void generateCoachPrompt()}
          disabled={coachBusy}
          icon={<Lightbulb size={16} />}
        >
          {coachBusy ? "生成中" : coachPrompt ? "再生成一问" : "生成下一问"}
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
            <ArrowLeft size={14} /> 返回作业
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
                <option value={71}>Python 3</option>
                <option value={54}>C++17</option>
              </Select>
            </Field>
          }
        >
          <div className="stack" id="code-workbench">
            <div className="editor-shell">
              <div className="editor-toolbar" aria-label="编辑器状态">
                <span>{languageId === 54 ? "main.cpp" : "main.py"}</span>
              </div>
              <Suspense fallback={<div className="editor-loading">正在准备代码编辑器</div>}>
                <CodeEditor languageId={languageId} sourceCode={sourceCode} onChange={updateCode} />
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
                onClick={() => updateCode(languageId === 54 ? CPP_TEMPLATE : PYTHON_TEMPLATE)}
                icon={<RotateCcw size={18} />}
              >
                恢复模板
              </Button>
            </div>
          </div>
        </Panel>
      </section>

      {feedbackReady && resultOpen && latest && (
        <div className="problem-result-modal-backdrop" role="presentation">
          <section className="problem-result-modal" role="dialog" aria-modal="true" aria-labelledby="problem-result-title">
            <div className="problem-result-modal__header">
              <div>
                <h2 id="problem-result-title">{verdictLabel(latest.verdict)}</h2>
                {focusText && <p>{focusText}</p>}
              </div>
              <div className="problem-result-modal__status">
                <StatusPill tone={latest.verdict === "ACCEPTED" ? "success" : "warning"}>{testCaseSummary}</StatusPill>
                <button type="button" aria-label="关闭结果" onClick={() => setResultOpen(false)}>
                  <X size={18} />
                </button>
              </div>
            </div>

            <div className="problem-result-modal__body">
              <div className="problem-result-modal__grid">
                <section className="problem-result-section problem-result-section--tests">
                  <div className="problem-result-section__head">
                    <h3>评测点</h3>
                  </div>
                  <div className="problem-result-evidence">
                    <div>
                      <span>通过</span>
                      <strong>{total ? `${passed}/${total}` : "-"}</strong>
                    </div>
                    <div>
                      <span>耗时</span>
                      <strong>{latest.executionTime ? `${latest.executionTime} ms` : "-"}</strong>
                    </div>
                    <div>
                      <span>内存</span>
                      <strong>{latest.memoryUsed ? `${latest.memoryUsed} KB` : "-"}</strong>
                    </div>
                  </div>
                  {latest.testCaseResults?.length ? (
                    <div className="testcase-compact-list">
                      {latest.testCaseResults.map(item => (
                        <div className={item.passed ? "is-passed" : ""} key={item.testCaseNumber}>
                          <span>#{item.testCaseNumber}</span>
                          <div>
                            <strong>{item.hidden ? "隐藏测试点" : "公开测试点"}</strong>
                            <small>
                              {item.passed ? "已通过" : "未通过"} · {item.executionTime ?? "-"} ms · {item.memoryUsed ?? "-"} KB
                            </small>
                          </div>
                          <span className={`meta-badge ${item.passed ? "meta-badge--success" : "meta-badge--warning"}`}>
                            {item.passed ? "通过" : "未过"}
                          </span>
                        </div>
                      ))}
                    </div>
                  ) : null}
                  {firstFailedCase && !firstFailedCase.hidden ? (
                    <div className="failed-case-card">
                      <span>第一个公开失败点</span>
                      <div className="two-column">
                        <div>
                          <strong>期望输出</strong>
                          <pre className="code-block">{outputPreview(firstFailedCase.expectedOutput)}</pre>
                        </div>
                        <div>
                          <strong>你的输出</strong>
                          <pre className="code-block">{outputPreview(firstFailedCase.actualOutput)}</pre>
                        </div>
                      </div>
                    </div>
                  ) : firstFailedCase?.hidden ? (
                    <div className="alert">隐藏测试点未通过。先检查边界条件。</div>
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
                  <details className="problem-compact-details problem-submission-drawer">
                    <summary>
                      <span>尝试记录</span>
                      <span className="meta-badge">{history.length}</span>
                    </summary>
                    <div className="problem-submission-drawer__body">
                      {history.length ? (
                        history.slice(0, 4).map(item => (
                          <div className="list-row" key={item.id}>
                            <div className="actions">
                              <VerdictPill verdict={item.verdict} />
                              <span className="meta-badge">{formatDateTime(item.submittedAt)}</span>
                            </div>
                            <h3>{item.analysisHeadline || verdictLabel(item.verdict)}</h3>
                            <p>{item.analysisSummary || `${item.passedTestCases || 0}/${item.totalTestCases || 0} 个测试点通过`}</p>
                          </div>
                        ))
                      ) : null}
                    </div>
                  </details>
                </section>

                <section className="problem-result-section problem-result-section--repair">
                  <div className="problem-result-section__head">
                    <h3>AI错误指导</h3>
                  </div>
                  {blockingIssues.length ? (
                    <div className="student-feedback-list">
                      {blockingIssues.slice(0, 3).map((issue, index) => (
                        <article className="student-feedback-item" key={`${issue.title || issue.issueTag || "blocking"}-${index}`}>
                          <span>{index === 0 ? "先改这里" : "再检查"}</span>
                          <strong>{issue.title || issueLabel(issue.issueTag) || "当前失败原因"}</strong>
                          {issue.studentMessage && <p>{issue.studentMessage}</p>}
                          {issue.evidence && <small>{issue.evidence}</small>}
                          {issue.nextAction && <em>{issue.nextAction}</em>}
                        </article>
                      ))}
                    </div>
                  ) : repairFallbacks.length ? (
                    <div className="student-feedback-list">
                      {repairFallbacks.map((item, index) => (
                        <article className="student-feedback-item" key={item}>
                          <span>{index === 0 ? "先改这里" : "再检查"}</span>
                          <strong>{item}</strong>
                        </article>
                      ))}
                    </div>
                  ) : latest.analysis ? (
                    <div className="student-feedback-empty">先看左侧失败点。</div>
                  ) : (
                    <div className="student-feedback-empty">生成中</div>
                  )}

                  {nextLearningAction ? (
                    <section className="student-feedback-next" aria-label="下一步">
                      <span>下一步</span>
                      <strong>{nextLearningAction.task || focusText}</strong>
                      {nextLearningAction.checkQuestion && <p>{nextLearningAction.checkQuestion}</p>}
                    </section>
                  ) : null}

                  {hasRepairGuidance ? (
                    <section className="problem-feedback-coach" aria-label="AI追问">
                      <div className="problem-feedback-coach__head">
                        <h3>AI追问</h3>
                      </div>
                      {coachQuestionBlock}
                    </section>
                  ) : null}
                </section>

                <section className="problem-result-section problem-result-section--growth">
                  <div className="problem-result-section__head">
                    <h3>AI提升指导</h3>
                  </div>
                  {improvementOpportunities.length ? (
                    <div className="student-feedback-list">
                      {improvementOpportunities.slice(0, 3).map((item, index) => (
                        <article className="student-feedback-item" key={`${item.category || "improvement"}-${index}`}>
                          <span>{improvementLabel(item.category)}</span>
                          {item.studentMessage && <strong>{item.studentMessage}</strong>}
                          {item.benefit && <p>{item.benefit}</p>}
                        </article>
                      ))}
                    </div>
                  ) : latest.verdict === "ACCEPTED" ? (
                    <div className="student-feedback-empty">可复盘复杂度、边界和可读性。</div>
                  ) : (
                    <div className="student-feedback-empty">先通过，再优化。</div>
                  )}
                </section>
              </div>
            </div>

            <div className="problem-result-modal__footer">
              <Button type="button" variant="primary" onClick={() => setResultOpen(false)}>
                继续修改
              </Button>
              {nextTaskLink && (
                <ButtonLink to={nextTaskLink} variant="secondary" icon={<ArrowRight size={16} />}>
                  下一题
                </ButtonLink>
              )}
              <Button type="button" variant="ghost" onClick={() => setResultOpen(false)}>
                关闭
              </Button>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

import { lazy, Suspense, useEffect, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { ArrowLeft, CheckCircle2, Clock3, FileText, History, Lightbulb, Play, RotateCcw, Target } from "lucide-react";
import { api } from "../../shared/api/client";
import type { CoachPrompt, Problem, StudentTrajectory, SubmissionHistorySummary, SubmissionResult } from "../../shared/api/types";
import { formatDateTime, issueLabel, verdictLabel } from "../../shared/format";
import { loadDraft, loadStudent, saveDraft } from "../../shared/storage";
import { Button } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea } from "../../shared/ui/Field";
import { Metric } from "../../shared/ui/Metric";
import { Panel } from "../../shared/ui/Panel";
import { DifficultyPill, StatusPill, VerdictPill } from "../../shared/ui/StatusPill";

const PYTHON_TEMPLATE = "n = int(input())\nprint(n)\n";
const CPP_TEMPLATE = "#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    ios::sync_with_stdio(false);\n    cin.tie(nullptr);\n\n    int n;\n    cin >> n;\n    cout << n << '\\n';\n    return 0;\n}\n";
const CodeEditor = lazy(() => import("./CodeEditor"));

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

export default function ProblemPage() {
  const params = useParams();
  const [searchParams] = useSearchParams();
  const problemId = Number(params.problemId);
  const assignmentId = searchParams.get("assignmentId") ? Number(searchParams.get("assignmentId")) : null;
  const studentProfileId = searchParams.get("studentProfileId") ? Number(searchParams.get("studentProfileId")) : loadStudent(assignmentId)?.id ?? null;
  const recommendationToken = searchParams.get("recommendationToken");

  const [problem, setProblem] = useState<Problem | null>(null);
  const [languageId, setLanguageId] = useState(71);
  const [sourceCode, setSourceCode] = useState(PYTHON_TEMPLATE);
  const [latest, setLatest] = useState<SubmissionResult | null>(null);
  const [history, setHistory] = useState<SubmissionHistorySummary[]>([]);
  const [trajectory, setTrajectory] = useState<StudentTrajectory | null>(null);
  const [coachPrompt, setCoachPrompt] = useState<CoachPrompt | null>(null);
  const [coachAnswer, setCoachAnswer] = useState("");
  const [alert, setAlert] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const [coachBusy, setCoachBusy] = useState(false);
  const [coachReplyBusy, setCoachReplyBusy] = useState(false);

  useEffect(() => {
    async function load() {
      try {
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
    if (!assignmentId || !studentProfileId) {
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
      setCoachPrompt(null);
      setHistory(await api.history(problem.id));
      setAlert({
        type: result.verdict === "ACCEPTED" ? "success" : "error",
        message: result.verdict === "INTERNAL_ERROR" ? result.errorMessage || "执行环境未就绪。" : `本次结果：${verdictLabel(result.verdict)}`
      });
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
  const firstFailedCase = latest?.testCaseResults?.find(item => !item.passed) || null;
  const codeLineCount = sourceCode.split(/\r?\n/).filter(line => line.trim()).length;
  const focusText =
    latest?.analysis?.studentHint ||
    latest?.analysis?.fixDirections?.[0] ||
    trajectory?.nextStep ||
    (latest ? "先根据本次结果修改一处问题。" : "先完成一次提交。");
  const currentStage = latest ? verdictLabel(latest.verdict) : "尚未提交";

  return (
    <div className="stack problem-page">
      <section className="practice-command">
        <div className="practice-command__main">
          <Link to="/app/student" className="eyebrow">
            <ArrowLeft size={14} /> 返回作业
          </Link>
          <h1>{problem.title}</h1>
          <div className="problem-hero__flow" aria-label="练习流程">
            <span>
              <FileText size={16} /> 读题
            </span>
            <span>
              <Play size={16} /> 写代码
            </span>
            <span>
              <CheckCircle2 size={16} /> 看结果
            </span>
          </div>
        </div>
        <div className="practice-command__status" aria-label="练习状态">
          <div>
            <FileText size={18} />
            <span>题目难度</span>
            <strong>{problem.difficulty ? <DifficultyPill difficulty={problem.difficulty} /> : "-"}</strong>
          </div>
          <div>
            <Clock3 size={18} />
            <span>限制</span>
            <strong>{problem.timeLimit} ms / {Math.round(problem.memoryLimit / 1024)} MB</strong>
          </div>
          <div>
            <Target size={18} />
            <span>当前阶段</span>
            <strong>{currentStage}</strong>
          </div>
        </div>
      </section>

      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="practice-focus-strip" aria-label="本次练习焦点">
        <div>
          <span>状态</span>
          <strong>{focusText}</strong>
        </div>
        <div>
          <span>代码</span>
          <strong>{languageId === 54 ? "C++17" : "Python 3"} · {codeLineCount} 行</strong>
        </div>
        <div>
          <span>测试点</span>
          <strong>{total ? `${passed}/${total} 通过` : "未提交"}</strong>
        </div>
      </section>

      <section className="problem-layout">
        <Panel
          title="题目"
          className="panel--statement"
          action={problem.difficulty ? <DifficultyPill difficulty={problem.difficulty} /> : undefined}
        >
          <div className="statement">{renderMarkdownLike(problem.description)}</div>
          <div className="stack" style={{ marginTop: "1rem" }}>
            <h3>公开样例</h3>
            {problem.sampleTestCases.length ? (
              problem.sampleTestCases.map((sample, index) => (
                <div className="two-column" key={`${sample.input}-${index}`}>
                  <div>
                    <StatusPill>输入 {index + 1}</StatusPill>
                    <pre className="code-block">{sample.input || "(空输入)"}</pre>
                  </div>
                  <div>
                    <StatusPill>输出 {index + 1}</StatusPill>
                    <pre className="code-block">{sample.expectedOutput || "(空输出)"}</pre>
                  </div>
                </div>
              ))
            ) : (
              <EmptyState title="暂无公开样例" />
            )}
          </div>
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
          <div className="stack">
            <div className="editor-shell">
              <div className="editor-toolbar" aria-label="编辑器状态">
                <span>{languageId === 54 ? "main.cpp" : "main.py"}</span>
                <strong>{languageId === 54 ? "C++17" : "Python 3"}</strong>
              </div>
              <Suspense fallback={<div className="editor-loading">正在准备代码编辑器</div>}>
                <CodeEditor languageId={languageId} sourceCode={sourceCode} onChange={updateCode} />
              </Suspense>
            </div>
            <div className="practice-submit-strip">
              <div>
                <span>草稿</span>
                <strong>已自动保存</strong>
              </div>
              <div>
                <span>结果</span>
                <strong>{latest ? verdictLabel(latest.verdict) : "未提交"}</strong>
              </div>
            </div>
            <div className="actions">
              <Button type="button" variant="primary" onClick={() => void submit()} disabled={busy} icon={<Play size={18} />}>
                提交代码
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

        <div className="stack problem-sidebar">
          <Panel
            title="提交结果"
            className="panel--ai"
            action={latest ? <VerdictPill verdict={latest.verdict} /> : <Lightbulb size={18} />}
          >
            {!latest ? (
              <EmptyState title="还没有提交" />
            ) : (
              <div className="stack">
                <div className="coach-focus-card">
                  <span>处理项</span>
                  <strong>{focusText}</strong>
                </div>
                <div className="metric-grid">
                  <Metric label="测试点" value={total ? `${passed}/${total}` : "-"} />
                  <Metric label="耗时" value={latest.executionTime ? `${latest.executionTime} ms` : "-"} />
                  <Metric label="内存" value={latest.memoryUsed ? `${latest.memoryUsed} KB` : "-"} />
                  <Metric label="分析" value={latest.analysisStatus || "观察中"} />
                </div>
                {latest.errorMessage && <div className="alert alert--error">{latest.errorMessage}</div>}
                {latest.compileOutput && <pre className="code-block">{latest.compileOutput}</pre>}
                {latest.testCaseResults?.length ? (
                  <div className="testcase-compact-list">
                    {latest.testCaseResults.slice(0, 6).map(item => (
                      <div className={item.passed ? "is-passed" : ""} key={item.testCaseNumber}>
                        <span>#{item.testCaseNumber}</span>
                        <div>
                          <strong>{item.hidden ? "隐藏测试点" : "公开测试点"}</strong>
                          <small>
                            {item.passed ? "已通过" : "未通过"} · {item.executionTime ?? "-"} ms · {item.memoryUsed ?? "-"} KB
                          </small>
                        </div>
                        <StatusPill tone={item.passed ? "success" : "warning"}>{item.passed ? "通过" : "需修正"}</StatusPill>
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
                        <pre className="code-block">{firstFailedCase.expectedOutput || "(空输出)"}</pre>
                      </div>
                      <div>
                        <strong>你的输出</strong>
                        <pre className="code-block">{firstFailedCase.actualOutput || "(空输出)"}</pre>
                      </div>
                    </div>
                  </div>
                ) : firstFailedCase?.hidden ? (
                  <div className="alert">有隐藏测试点未通过。这里不会展示隐藏数据，请优先检查边界条件和泛化能力。</div>
                ) : null}
                {latest.analysis ? (
                  <div className="stack">
                    <h3>{latest.analysis.headline || "系统反馈"}</h3>
                    {latest.analysis.studentHint && <div className="alert">{latest.analysis.studentHint}</div>}
                    {latest.analysis.summary && <p>{latest.analysis.summary}</p>}
                    <div className="issue-list">
                      {(latest.analysis.issueTags || []).slice(0, 5).map(tag => (
                        <div className="issue-row" key={tag}>
                          <span>{issueLabel(tag)}</span>
                          <strong>反馈</strong>
                        </div>
                      ))}
                    </div>
                    {latest.analysis.fixDirections?.length ? (
                      <div>
                        <h3>修改方向</h3>
                        <ul>
                          {latest.analysis.fixDirections.slice(0, 4).map(item => (
                            <li key={item}>{item}</li>
                          ))}
                        </ul>
                      </div>
                    ) : null}
                    <details className="problem-compact-details">
                      <summary>
                        <span>下一问</span>
                        <StatusPill tone={coachPrompt ? "info" : "neutral"}>{coachPrompt ? "已生成" : "可生成"}</StatusPill>
                      </summary>
                      <div className="coach-next-question">
                        {coachPrompt ? (
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
                                <Button
                                  type="button"
                                  variant="primary"
                                  onClick={() => void replyCoachPrompt()}
                                  disabled={coachReplyBusy}
                                >
                                  {coachReplyBusy ? "提交中" : "提交回答"}
                                </Button>
                              </div>
                            )}
                          </>
                        ) : (
                          <strong>生成一个定位问题。</strong>
                        )}
                        <Button
                          type="button"
                          variant="secondary"
                          onClick={() => void generateCoachPrompt()}
                          disabled={coachBusy}
                          icon={<Lightbulb size={16} />}
                        >
                          {coachBusy ? "生成中" : coachPrompt ? "再生成一问" : "生成下一问"}
                        </Button>
                      </div>
                    </details>
                  </div>
                ) : (
                  <EmptyState title="反馈生成中" />
                )}
              </div>
            )}
          </Panel>

          <details className="problem-compact-details">
            <summary>
              <span>作业记录</span>
              <StatusPill tone={trajectory ? "success" : "neutral"}>{trajectory ? "已同步" : "未确认"}</StatusPill>
            </summary>
            {!trajectory ? (
              <EmptyState title="未确认作业身份" />
            ) : (
              <div className="stack">
                <div className="metric-grid">
                  <Metric label="完成" value={`${trajectory.completedTasks}/${trajectory.totalTasks}`} />
                  <Metric label="尝试" value={trajectory.totalAttempts} />
                  <Metric label="卡点" value={trajectory.repeatedIssueTag ? issueLabel(trajectory.repeatedIssueTag) : "暂无"} />
                  <Metric label="阶段" value={trajectory.stageTransition || "观察中"} />
                </div>
                <div className="alert">{trajectory.nextStep || "暂无新的处理项。"}</div>
              </div>
            )}
          </details>

          <details className="problem-compact-details">
            <summary>
              <span>尝试记录</span>
              <StatusPill tone="neutral">{history.length} 次</StatusPill>
            </summary>
            <div className="stack">
              {history.length ? (
                history.slice(0, 6).map(item => (
                  <div className="list-row" key={item.id}>
                    <div className="actions">
                      <VerdictPill verdict={item.verdict} />
                      <StatusPill>{formatDateTime(item.submittedAt)}</StatusPill>
                    </div>
                    <h3>{item.analysisHeadline || verdictLabel(item.verdict)}</h3>
                    <p>{item.analysisSummary || `${item.passedTestCases || 0}/${item.totalTestCases || 0} 个测试点通过`}</p>
                  </div>
                ))
              ) : (
                <EmptyState title="暂无历史记录" />
              )}
            </div>
          </details>
        </div>
      </section>
    </div>
  );
}

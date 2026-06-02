import { lazy, Suspense, useEffect, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { ArrowLeft, CheckCircle2, Clock3, FileText, History, Lightbulb, Play, RotateCcw, Target } from "lucide-react";
import { api } from "../../shared/api/client";
import type {
  CoachPrompt,
  Problem,
  StudentAbilityProfile,
  StudentTrajectory,
  SubmissionHistorySummary,
  SubmissionResult
} from "../../shared/api/types";
import {
  abilityLabel,
  aiDependencyStatusLabel,
  difficultyLabel,
  formatDateTime,
  issueLabel,
  learningStageLabel,
  masteryGrowthStatusLabel,
  recurringMisconceptionStatusLabel,
  selfExplanationStatusLabel,
  teachingActionActorLabel,
  teachingActionTypeLabel,
  verdictLabel
} from "../../shared/format";
import { loadDraft, loadStudent, saveDraft } from "../../shared/storage";
import { Button } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea } from "../../shared/ui/Field";
import { Metric } from "../../shared/ui/Metric";
import { Panel } from "../../shared/ui/Panel";
import { StatusPill, VerdictPill } from "../../shared/ui/StatusPill";

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
  const [abilityProfile, setAbilityProfile] = useState<StudentAbilityProfile | null>(null);
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
    if (!studentProfileId) {
      setAbilityProfile(null);
      return;
    }
    void api.studentAbilityProfile(studentProfileId).then(setAbilityProfile).catch(() => undefined);
  }, [studentProfileId, latest]);

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
  const studentFeedback = latest?.analysis?.studentFeedback || null;
  const primaryBlockingIssue = studentFeedback?.blockingIssues?.[0] || null;
  const nextLearningAction = studentFeedback?.nextLearningAction || null;
  const focusText =
    nextLearningAction?.task ||
    primaryBlockingIssue?.nextAction ||
    primaryBlockingIssue?.studentMessage ||
    studentFeedback?.summary ||
    latest?.analysis?.studentHint ||
    latest?.analysis?.fixDirections?.[0] ||
    (trajectory?.nextStep ? learningStageLabel(trajectory.nextStep) : "") ||
    (latest ? "先根据本次结果修改一处问题。" : "先完成一次提交。");
  const testCaseSummary = total ? `${passed}/${total} 测试点` : "等待评测";
  const feedbackSummary = latest
    ? latest.verdict === "ACCEPTED"
      ? "本题已通过，可以复盘思路或继续下一题。"
      : firstFailedCase && !firstFailedCase.hidden
        ? "先看第一个公开失败点，再改一处最可能的原因。"
        : firstFailedCase?.hidden
          ? "隐藏测试点未通过，先检查边界条件和泛化能力。"
          : "先根据本次结果修改一处问题。"
    : "提交后这里会显示本次结果。";
  const analysisStateLabel = latest?.analysis ? "已生成" : latest ? latest.analysisStatus || "生成中" : "未提交";
  const currentStage = latest ? verdictLabel(latest.verdict) : "尚未提交";
  const recurringSignal = abilityProfile?.recurringMisconceptionSignal;
  const hasRecurringSignal =
    recurringSignal &&
    ["WATCH", "RECURRING", "ESCALATE"].includes((recurringSignal.status || "").toUpperCase());
  const selfExplanationSignal = abilityProfile?.selfExplanationMasterySignal || trajectory?.selfExplanationMasterySignal;
  const selfExplanationStatus = (selfExplanationSignal?.status || "").toUpperCase();
  const hasSelfExplanationSignal =
    selfExplanationSignal &&
    (["EMERGING", "NEEDS_COACHING", "SAFETY_RISK"].includes(selfExplanationStatus) ||
      (selfExplanationStatus === "NO_EVIDENCE" && selfExplanationSignal.needsTeacherAttention));
  const selfExplanationEvidence =
    typeof selfExplanationSignal?.evidenceCompleteness === "number"
      ? `${Math.round(selfExplanationSignal.evidenceCompleteness * 100)}%`
      : "";
  const aiDependencySignal = abilityProfile?.aiDependencySignal || trajectory?.aiDependencySignal;
  const aiDependencyStatus = (aiDependencySignal?.status || "").toUpperCase();
  const hasAiDependencySignal =
    aiDependencySignal && ["SCAFFOLD_DENSE", "DEPENDENCY_RISK", "TEACHER_FADE_REVIEW"].includes(aiDependencyStatus);
  const independenceScore =
    typeof aiDependencySignal?.independenceScore === "number" ? `${Math.round(aiDependencySignal.independenceScore * 100)}%` : "";
  const masteryGrowthSignal = abilityProfile?.masteryGrowthSignal || trajectory?.masteryGrowthSignal;
  const masteryGrowthStatus = (masteryGrowthSignal?.status || "").toUpperCase();
  const hasMasteryGrowthSignal =
    masteryGrowthSignal && ["PLATEAU", "REGRESSION", "SPIRAL_REVIEW_NEEDED"].includes(masteryGrowthStatus);
  const masteryGrowthScore =
    typeof masteryGrowthSignal?.growthScore === "number" ? `${Math.round(masteryGrowthSignal.growthScore * 100)}%` : "";
  const teachingActionDecision = trajectory?.teachingActionDecision || abilityProfile?.teachingActionDecision;
  const hasTeachingActionDecision =
    teachingActionDecision &&
    (teachingActionDecision.actionType || "").toUpperCase() !== "CONTINUE_DIAGNOSIS";
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
          <strong>生成一个定位问题。</strong>
        )
      ) : (
        <p>提交后会根据本次反馈生成追问。</p>
      )}
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
            <strong>{problem.difficulty ? difficultyLabel(problem.difficulty) : "-"}</strong>
          </div>
          <div>
            <Clock3 size={18} />
            <strong>{problem.timeLimit} ms / {Math.round(problem.memoryLimit / 1024)} MB</strong>
          </div>
          <div>
            <Target size={18} />
            <strong>{currentStage}</strong>
          </div>
        </div>
      </section>

      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="problem-layout">
        <Panel
          title="题目"
          className="panel--statement"
          action={problem.difficulty ? <span className="meta-badge meta-badge--success">{difficultyLabel(problem.difficulty)}</span> : undefined}
        >
          <div className="statement">{renderMarkdownLike(problem.description)}</div>
          <details className="problem-compact-details problem-sample-drawer">
            <summary>
              <span>公开样例</span>
              <span className="meta-badge">{problem.sampleTestCases.length || "暂无"}</span>
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
              ) : (
                <EmptyState title="暂无公开样例" />
              )}
            </div>
          </details>
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
            title="提交反馈"
            className="panel--ai"
            action={latest ? <VerdictPill verdict={latest.verdict} /> : <Lightbulb size={18} />}
          >
            {!latest ? (
              <EmptyState title="还没有提交" />
            ) : (
              <div className="stack">
                <section className="problem-primary-action" aria-label="本次先做">
                  <div className="problem-primary-action__head">
                    <div>
                      <span>本次先做</span>
                      <strong>{focusText}</strong>
                    </div>
                    <StatusPill tone={latest.verdict === "ACCEPTED" ? "success" : "warning"}>{verdictLabel(latest.verdict)}</StatusPill>
                  </div>
                  {studentFeedback?.summary && studentFeedback.summary !== focusText && <p>{studentFeedback.summary}</p>}
                  {firstFailedCase && !firstFailedCase.hidden ? (
                    <div className="problem-primary-action__evidence">
                      <span>公开失败点</span>
                      <strong>
                        期望 {firstFailedCase.expectedOutput || "(空输出)"}，你的输出 {firstFailedCase.actualOutput || "(空输出)"}
                      </strong>
                    </div>
                  ) : firstFailedCase?.hidden ? (
                    <div className="problem-primary-action__evidence">
                      <span>隐藏失败点</span>
                      <strong>先检查边界条件和泛化能力。</strong>
                    </div>
                  ) : null}
                  {studentFeedback?.blockingIssues?.length ? (
                    <section className="student-feedback-channel" aria-label="当前错误点">
                      <div className="student-feedback-channel__head">
                        <h3>当前错误点</h3>
                        <StatusPill tone="warning">{studentFeedback.blockingIssues.length} 个</StatusPill>
                      </div>
                      <div className="student-feedback-list">
                        {studentFeedback.blockingIssues.slice(0, 3).map((issue, index) => (
                          <article className="student-feedback-item" key={`${issue.title || issue.issueTag || "blocking"}-${index}`}>
                            <span>{issue.priority ? `优先级 ${issue.priority}` : issue.fineGrainedTag ? issueLabel(issue.fineGrainedTag) : "先处理"}</span>
                            <strong>{issue.title || issueLabel(issue.issueTag) || "当前失败原因"}</strong>
                            {issue.studentMessage && <p>{issue.studentMessage}</p>}
                            {issue.evidence && <small>{issue.evidence}</small>}
                            {issue.nextAction && <em>{issue.nextAction}</em>}
                          </article>
                        ))}
                      </div>
                    </section>
                  ) : latest.analysis?.studentHint && latest.analysis.studentHint !== focusText ? (
                    <p>{latest.analysis.studentHint}</p>
                  ) : null}
                  {studentFeedback?.secondaryIssues?.length ? (
                    <section className="student-feedback-channel student-feedback-channel--secondary" aria-label="次要问题">
                      <div className="student-feedback-channel__head">
                        <h3>先不放大的问题</h3>
                      </div>
                      <div className="student-feedback-list">
                        {studentFeedback.secondaryIssues.slice(0, 2).map((issue, index) => (
                          <article className="student-feedback-item" key={`${issue.title || issue.issueTag || "secondary"}-${index}`}>
                            <span>{issue.issueTag ? issueLabel(issue.issueTag) : "次要信号"}</span>
                            <strong>{issue.title || "可以后置观察"}</strong>
                            {issue.studentMessage && <p>{issue.studentMessage}</p>}
                            {issue.whyNotPrimary && <small>{issue.whyNotPrimary}</small>}
                          </article>
                        ))}
                      </div>
                    </section>
                  ) : null}
                  {studentFeedback?.improvementOpportunities?.length ? (
                    <section className="student-feedback-channel student-feedback-channel--improvement" aria-label="继续提升点">
                      <div className="student-feedback-channel__head">
                        <h3>继续提升点</h3>
                        <StatusPill tone="info">{studentFeedback.improvementOpportunities.length} 条</StatusPill>
                      </div>
                      <div className="student-feedback-list">
                        {studentFeedback.improvementOpportunities.slice(0, 3).map((item, index) => (
                          <article className="student-feedback-item" key={`${item.category || "improvement"}-${index}`}>
                            <span>{improvementLabel(item.category)}</span>
                            {item.studentMessage && <strong>{item.studentMessage}</strong>}
                            {item.benefit && <p>{item.benefit}</p>}
                          </article>
                        ))}
                      </div>
                    </section>
                  ) : null}
                  {nextLearningAction ? (
                    <section className="student-feedback-next" aria-label="下一步">
                      <span>下一步</span>
                      <strong>{nextLearningAction.task || focusText}</strong>
                      {nextLearningAction.checkQuestion && <p>{nextLearningAction.checkQuestion}</p>}
                    </section>
                  ) : null}
                  <section className="problem-feedback-coach" aria-label="下一问">
                    <div className="problem-feedback-coach__head">
                      <h3>下一问</h3>
                      <StatusPill tone={coachPrompt ? "info" : "neutral"}>{coachPrompt ? "已生成" : "可生成"}</StatusPill>
                    </div>
                    {coachQuestionBlock}
                  </section>
                </section>
                <div className="problem-result-compact problem-result-compact--quiet" aria-label="本次结果摘要">
                  <div>
                    <span>评测</span>
                    <strong>{testCaseSummary}</strong>
                  </div>
                  <div>
                    <span>反馈</span>
                    <strong>{feedbackSummary}</strong>
                  </div>
                  <div>
                    <span>AI</span>
                    <strong>{analysisStateLabel}</strong>
                  </div>
                </div>
                <details className="problem-compact-details problem-submission-drawer">
                  <summary>
                    <span>本次提交明细</span>
                    <span className="meta-badge">{testCaseSummary}</span>
                  </summary>
                  <div className="problem-submission-drawer__body">
                    <div className="metric-grid">
                      <Metric label="测试点" value={total ? `${passed}/${total}` : "-"} />
                      <Metric label="耗时" value={latest.executionTime ? `${latest.executionTime} ms` : "-"} />
                      <Metric label="内存" value={latest.memoryUsed ? `${latest.memoryUsed} KB` : "-"} />
                      <Metric label="分析" value={analysisStateLabel} />
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
                            <span className={`meta-badge ${item.passed ? "meta-badge--success" : "meta-badge--warning"}`}>
                              {item.passed ? "通过" : "需修正"}
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
                  </div>
                </details>
                {latest.analysis ? (
                  <details className="problem-compact-details problem-diagnosis-drawer">
                    <summary>
                      <span>{latest.analysis.headline || "系统反馈"}</span>
                      <span className="meta-badge">详情</span>
                    </summary>
                    <div className="problem-diagnosis-drawer__body">
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
                    </div>
                  </details>
                ) : (
                  <EmptyState title="反馈生成中" />
                )}
              </div>
            )}
          </Panel>

          <details className="problem-compact-details problem-learning-drawer">
            <summary>
              <span>学习过程 / 下一问记录</span>
              <span className={`meta-badge ${coachPrompt ? "meta-badge--info" : trajectory ? "meta-badge--success" : ""}`}>
                记录
              </span>
            </summary>
            <div className="problem-learning-drawer__body">
              <section className="problem-learning-section">
                <div className="problem-learning-section__head">
                  <h3>作业记录</h3>
                  <StatusPill tone={trajectory ? "success" : "neutral"}>{trajectory ? "已同步" : "未确认"}</StatusPill>
                </div>
                {!trajectory ? (
                  <EmptyState title="未确认作业身份" />
                ) : (
                  <div className="stack">
                    <div className="metric-grid">
                      <Metric label="完成" value={`${trajectory.completedTasks}/${trajectory.totalTasks}`} />
                      <Metric label="尝试" value={trajectory.totalAttempts} />
                      <Metric label="卡点" value={trajectory.repeatedIssueTag ? issueLabel(trajectory.repeatedIssueTag) : "暂无"} />
                      <Metric label="阶段" value={learningStageLabel(trajectory.stageTransition)} />
                    </div>
                    {hasRecurringSignal && (
                      <div className="student-transfer-action student-transfer-action--recurring">
                        <span>{recurringMisconceptionStatusLabel(recurringSignal?.status)}</span>
                        <strong>
                          {recurringSignal?.fineGrainedTag
                            ? issueLabel(recurringSignal.fineGrainedTag)
                            : abilityLabel(recurringSignal?.abilityPoint) || "长期复发误区"}
                        </strong>
                        <p>{recurringSignal?.recommendedAction || recurringSignal?.summary || "先对比两道证据题的共同失败条件。"}</p>
                      </div>
                    )}
                    {hasSelfExplanationSignal && (
                      <div className="student-transfer-action student-transfer-action--explanation">
                        <span>{selfExplanationStatusLabel(selfExplanationSignal?.status)}</span>
                        <strong>
                          {selfExplanationEvidence
                            ? `解释证据完整度 ${selfExplanationEvidence}`
                            : "补齐可验证解释"}
                        </strong>
                        <p>
                          {selfExplanationSignal?.recommendedAction ||
                            selfExplanationSignal?.summary ||
                            "补一条最小样例、变量轨迹或实际输出对比。"}
                        </p>
                      </div>
                    )}
                    {hasAiDependencySignal && (
                      <div className="student-transfer-action student-transfer-action--dependency">
                        <span>{aiDependencyStatusLabel(aiDependencySignal?.status)}</span>
                        <strong>{independenceScore ? `自主推进度 ${independenceScore}` : "先做一次独立尝试"}</strong>
                        <p>
                          {aiDependencySignal?.recommendedAction ||
                            aiDependencySignal?.summary ||
                            "先不新增提示，写出一个最小样例和修改假设后再提交。"}
                        </p>
                      </div>
                    )}
                    {hasMasteryGrowthSignal && (
                      <div className="student-transfer-action student-transfer-action--mastery">
                        <span>{masteryGrowthStatusLabel(masteryGrowthSignal?.status)}</span>
                        <strong>
                          {masteryGrowthSignal?.focusAbility
                            ? abilityLabel(masteryGrowthSignal.focusAbility)
                            : masteryGrowthScore
                              ? `成长分 ${masteryGrowthScore}`
                              : "先做成长复盘"}
                        </strong>
                        <p>
                          {masteryGrowthSignal?.recommendedAction ||
                            masteryGrowthSignal?.summary ||
                            "先对比近期提交证据，找出一个最小失败条件。"}
                        </p>
                      </div>
                    )}
                    {hasTeachingActionDecision && (
                      <div className="student-transfer-action student-transfer-action--teaching">
                        <span>
                          {teachingActionTypeLabel(teachingActionDecision?.actionType)} ·{" "}
                          {teachingActionActorLabel(teachingActionDecision?.actor)}
                        </span>
                        <strong>{teachingActionDecision?.title || "下一步教学动作"}</strong>
                        <p>
                          {teachingActionDecision?.recommendedAction ||
                            teachingActionDecision?.summary ||
                            "先完成系统建议的最高优先级动作。"}
                        </p>
                      </div>
                    )}
                    <div className="alert">{trajectory.nextStep ? learningStageLabel(trajectory.nextStep) : "暂无新的处理项。"}</div>
                  </div>
                )}
              </section>

              <section className="problem-learning-section">
                <div className="problem-learning-section__head">
                  <h3>尝试记录</h3>
                  <StatusPill tone={history.length ? "neutral" : "info"}>{history.length} 次</StatusPill>
                </div>
                <div className="stack">
                  {history.length ? (
                    history.slice(0, 6).map(item => (
                      <div className="list-row" key={item.id}>
                        <div className="actions">
                          <VerdictPill verdict={item.verdict} />
                          <span className="meta-badge">{formatDateTime(item.submittedAt)}</span>
                        </div>
                        <h3>{item.analysisHeadline || verdictLabel(item.verdict)}</h3>
                        <p>{item.analysisSummary || `${item.passedTestCases || 0}/${item.totalTestCases || 0} 个测试点通过`}</p>
                      </div>
                    ))
                  ) : (
                    <EmptyState title="暂无历史记录" />
                  )}
                </div>
              </section>
            </div>
          </details>
        </div>
      </section>
    </div>
  );
}

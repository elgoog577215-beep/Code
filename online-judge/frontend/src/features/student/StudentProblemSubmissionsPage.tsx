import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, GitCompareArrows, Sparkles } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { api } from "../../shared/api/client";
import type { SubmissionHistorySummary } from "../../shared/api/types";
import { verdictLabel } from "../../shared/format";
import { loadStudent } from "../../shared/storage";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Panel } from "../../shared/ui/Panel";

function runtimeText(value?: number | null) {
  if (value === null || value === undefined) return "-";
  return value < 1 ? `${Math.round(value * 1000)} ms` : `${value.toFixed(2)} s`;
}

function memoryText(value?: number | null) {
  if (value === null || value === undefined || value <= 0) return "-";
  return `${(value / 1024).toFixed(1)} MB`;
}

function formatTime(value?: string | null) {
  if (!value) return "-";
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}

export default function StudentProblemSubmissionsPage() {
  const { assignmentId, problemId } = useParams();
  const numericProblemId = Number(problemId);
  const numericAssignmentId = assignmentId && assignmentId !== "public" ? Number(assignmentId) : null;
  const student = loadStudent(numericAssignmentId || undefined) || loadStudent();
  const backTo = `/app/student/assignments/${assignmentId || "public"}/problems/${numericProblemId}${student?.id ? `?studentProfileId=${student.id}` : ""}`;
  const [history, setHistory] = useState<SubmissionHistorySummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState<string | null>(null);
  const [baselineId, setBaselineId] = useState<number | null>(null);
  const [targetId, setTargetId] = useState<number | null>(null);

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setFailed(null);
    if (!Number.isFinite(numericProblemId)) {
      setFailed("题目不存在");
      setLoading(false);
      return () => {
        ignore = true;
      };
    }
    api.history(numericProblemId, numericAssignmentId)
      .then(result => {
        if (ignore) return;
        setHistory(result);
        setTargetId(result[0]?.id || null);
        setBaselineId(result.find(item => item.id !== result[0]?.id)?.id || null);
      })
      .catch(error => {
        if (!ignore) setFailed(error instanceof Error ? error.message : "提交记录加载失败");
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [numericAssignmentId, numericProblemId]);

  const problemTitle = history[0]?.problemTitle || "当前题目";
  const selectedComparable = baselineId && targetId && baselineId !== targetId;
  const compareHref = `/app/student/assignments/${assignmentId || "public"}/problems/${numericProblemId}/compare?leftId=${baselineId}&rightId=${targetId}`;
  const latestGrowth = useMemo(() => history.find(item => item.id === targetId)?.growthSummary || history[0]?.growthSummary || null, [history, targetId]);

  return (
    <div className="student-problem-submissions-page">
      <header className="student-problem-submissions-hero">
        <Link to={backTo}><ArrowLeft size={17} />返回题目</Link>
        <div>
          <span>提交记录</span>
          <h1>{problemTitle}</h1>
        </div>
      </header>

      {loading ? <EmptyState title="正在读取提交记录" live /> : failed ? <EmptyState title={failed} /> : !history.length ? (
        <EmptyState title={student ? "这道题还没有提交记录" : "登录后查看自己的提交记录"} />
      ) : (
        <>
          <Panel title="AI 成长记录" className="student-problem-growth-summary">
            {latestGrowth ? (
              <div className="student-problem-growth-summary__grid">
                <article><span>仍需处理</span><strong>{latestGrowth.unresolvedCount ?? 0}</strong></article>
                <article><span>已恢复</span><strong>{latestGrowth.recoveredCount ?? 0}</strong></article>
                <article><span>新增问题</span><strong>{latestGrowth.newCount ?? 0}</strong></article>
              </div>
            ) : (
              <p>当前提交还没有形成可展示的成长摘要。</p>
            )}
          </Panel>

          <div className="student-submission-compare-bar" aria-live="polite">
            <span><GitCompareArrows size={18} />成长对比</span>
            <strong>{baselineId ? `起点 #${baselineId}` : "起点未选择"} / {targetId ? `目标 #${targetId}` : "目标未选择"}</strong>
            {selectedComparable ? <Link to={compareHref}>开始对比</Link> : <button type="button" disabled>开始对比</button>}
          </div>

          <div className="student-problem-submission-table">
            <div className="student-problem-submission-table__header" aria-hidden="true">
              <span>提交编号</span><span>提交时间</span><span>结果</span><span>语言</span><span>用时</span><span>内存</span><span>操作</span>
            </div>
            {history.map(item => (
              <div className="student-problem-submission-row" key={item.id}>
                <span>#{item.id}</span>
                <span>{formatTime(item.submittedAt)}</span>
                <span>{verdictLabel(String(item.verdict))}</span>
                <span>{item.languageName || "-"}</span>
                <span>{runtimeText(item.executionTime)}</span>
                <span>{memoryText(item.memoryUsed)}</span>
                <span className="student-submission-actions">
                  <button type="button" className={baselineId === item.id ? "is-active" : ""} onClick={() => setBaselineId(item.id)}>起点</button>
                  <button type="button" className={targetId === item.id ? "is-active" : ""} onClick={() => setTargetId(item.id)}>目标</button>
                  <Link
                    className="student-submission-ai-button"
                    to={`${backTo}${backTo.includes("?") ? "&" : "?"}submissionId=${item.id}`}
                    aria-label="查看 AI 分析"
                    title="查看 AI 分析"
                  >
                    <Sparkles size={18} />
                  </Link>
                </span>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

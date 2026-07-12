import { useEffect, useMemo, useState } from "react";
import { ArrowDown, Bookmark, CheckCircle2, ChevronLeft, ChevronRight, Clock3, Eye, Search, Send, X } from "lucide-react";
import { Navigate, useParams } from "react-router-dom";
import { api } from "../../shared/api/client";
import type { StudentAssignmentSubmissionPage, SubmissionResult } from "../../shared/api/types";
import { verdictLabel } from "../../shared/format";
import { useTranslation } from "../../shared/i18n";
import { ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { growthStateKey } from "../growth/SingleProblemGrowthDashboard";
import { formatRelativeTime, StudentAssignmentShell, useStudentAssignmentWorkspace } from "./StudentAssignmentWorkspace";

function verdictCode(value: string) {
  const codes: Record<string, string> = {
    ACCEPTED: "AC",
    WRONG_ANSWER: "WA",
    RUNTIME_ERROR: "RE",
    COMPILATION_ERROR: "CE",
    TIME_LIMIT_EXCEEDED: "TLE",
    MEMORY_LIMIT_EXCEEDED: "MLE",
    INTERNAL_ERROR: "IE",
    PENDING: "..."
  };
  return codes[value] || value;
}

function runtimeText(value?: number | null) {
  if (value === null || value === undefined) return "-";
  return value < 1 ? `${Math.round(value * 1000)} ms` : `${value.toFixed(2)} s`;
}

function memoryText(value?: number | null) {
  if (value === null || value === undefined || value <= 0) return "-";
  return `${(value / 1024).toFixed(1)} MB`;
}

function paginationNumbers(totalPages: number, currentPage: number) {
  const visibleCount = Math.min(totalPages, 5);
  const start = Math.max(0, Math.min(currentPage - 2, totalPages - visibleCount));
  return Array.from({ length: visibleCount }, (_, index) => start + index);
}

export default function StudentAssignmentSubmissionsPage() {
  const { t } = useTranslation();
  const { assignmentId } = useParams();
  const numericAssignmentId = Number(assignmentId);
  const workspace = useStudentAssignmentWorkspace(numericAssignmentId);
  const [problemId, setProblemId] = useState("");
  const [accepted, setAccepted] = useState<"" | "true" | "false">("");
  const [languageName, setLanguageName] = useState("");
  const [submissionId, setSubmissionId] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<StudentAssignmentSubmissionPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState<string | null>(null);
  const [detail, setDetail] = useState<SubmissionResult | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  useEffect(() => setPage(0), [problemId, accepted, languageName, submissionId]);

  useEffect(() => {
    let ignore = false;
    if (!workspace.student || !Number.isFinite(numericAssignmentId)) {
      setLoading(false);
      return () => {
        ignore = true;
      };
    }
    setLoading(true);
    setFailed(null);
    const parsedSubmissionId = Number(submissionId);
    api.studentAssignmentSubmissions(numericAssignmentId, {
      problemId: problemId ? Number(problemId) : null,
      accepted: accepted ? accepted === "true" : null,
      languageName: languageName || null,
      submissionId: submissionId && Number.isFinite(parsedSubmissionId) ? parsedSubmissionId : null,
      page,
      size: 8
    })
      .then(result => {
        if (!ignore) setData(result);
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
  }, [accepted, languageName, numericAssignmentId, page, problemId, submissionId, workspace.student]);

  const languages = useMemo(() => {
    const values = new Set((data?.items || []).map(item => item.languageName).filter((value): value is string => Boolean(value)));
    ["Python 3", "C++17"].forEach(value => values.add(value));
    return [...values];
  }, [data]);

  async function openDetail(id: number) {
    setDetailLoading(true);
    try {
      setDetail(await api.submission(id));
    } finally {
      setDetailLoading(false);
    }
  }

  if (!workspace.student) return <Navigate to="/app/student/login" replace />;
  if (workspace.loading) return <EmptyState title="正在读取作业" live />;
  if (!workspace.assignment) return <EmptyState title={workspace.failed || "作业不存在"} />;

  return (
    <StudentAssignmentShell assignment={workspace.assignment} student={workspace.student} nextTask={workspace.nextTask} activeTab="submissions">
      <section className="student-assignment-summary-band student-submission-summary" aria-label="提交记录摘要">
        <span><Send size={20} aria-hidden="true" />共提交 <strong>{data?.totalSubmissionCount || 0}</strong> 次</span>
        <span><CheckCircle2 size={20} aria-hidden="true" />通过 <strong>{data?.acceptedSubmissionCount || 0}</strong> 次</span>
        <span><Bookmark size={20} aria-hidden="true" />涉及 <strong>{data?.distinctProblemCount || 0}</strong> 道题</span>
        <span><Clock3 size={20} aria-hidden="true" />最近提交 <strong>{formatRelativeTime(data?.latestSubmittedAt)}</strong></span>
      </section>

      <section className="student-submission-history" aria-labelledby="student-submission-history-title">
        <h2 className="sr-only" id="student-submission-history-title">我的提交记录</h2>
        <div className="student-submission-filters">
          <label className="student-submission-filter student-submission-filter--problem">
            <span className="sr-only">按题目筛选</span>
            <select value={problemId} onChange={event => setProblemId(event.target.value)}>
              <option value="">全部题目</option>
              {workspace.assignment.tasks.map(task => <option value={task.problemId} key={task.problemId}>{task.title}</option>)}
            </select>
          </label>
          <div className="student-submission-filter student-submission-filter--verdict">
            <span className="student-submission-filter-label">判题结果：</span>
            <div className="student-submission-verdict-filter" role="group" aria-label="按判题结果筛选">
              <button type="button" className={!accepted ? "is-active" : ""} onClick={() => setAccepted("")}>全部</button>
              <button type="button" className={accepted === "true" ? "is-active" : ""} onClick={() => setAccepted("true")}>通过</button>
              <button type="button" className={accepted === "false" ? "is-active" : ""} onClick={() => setAccepted("false")}>未通过</button>
            </div>
          </div>
          <label className="student-submission-filter student-submission-filter--language">
            <span className="student-submission-filter-label">语言：</span>
            <span className="sr-only">按语言筛选</span>
            <select value={languageName} onChange={event => setLanguageName(event.target.value)}>
              <option value="">全部语言</option>
              {languages.map(language => <option value={language} key={language}>{language}</option>)}
            </select>
          </label>
          <label className="student-submission-filter student-submission-search">
            <Search size={17} aria-hidden="true" />
            <span className="sr-only">搜索提交编号</span>
            <input inputMode="numeric" placeholder="搜索提交编号" value={submissionId} onChange={event => setSubmissionId(event.target.value.replace(/\D/g, ""))} />
          </label>
        </div>

        {failed ? <EmptyState title={failed} /> : loading ? <EmptyState title="正在读取提交记录" live /> : data?.items.length ? (
          <>
            <div className="student-submission-table">
              <div className="student-submission-table__header" aria-hidden="true">
                <span>提交编号</span><span className="student-submission-sort">提交时间<ArrowDown size={14} /></span><span>题目</span><span>判题结果</span><span>语言</span><span>用时</span><span>内存</span><span>操作</span>
              </div>
              {data.items.map((item, index) => (
                <button type="button" className={`student-submission-row${index === 0 && page === 0 ? " is-latest" : ""}`} onClick={() => void openDetail(item.id)} key={item.id}>
                  <span>#{item.id}</span>
                  <span>{item.submittedAt ? new Date(item.submittedAt).toLocaleString("zh-CN", { hour12: false }) : "-"}</span>
                  <span>{item.problemTitle}</span>
                  <span><i className={`verdict-code verdict-code--${String(item.verdict).toLowerCase()}`}>{verdictCode(String(item.verdict))}</i></span>
                  <span>{item.languageName || "-"}</span>
                  <span>{runtimeText(item.executionTime)}</span>
                  <span>{memoryText(item.memoryUsed)}</span>
                  <span><Eye size={18} aria-label="查看提交详情" /></span>
                </button>
              ))}
            </div>
            <footer className="student-submission-pagination">
              <span>{data.totalElements ? `${data.page * data.size + 1}-${Math.min((data.page + 1) * data.size, data.totalElements)} / ${data.totalElements}` : "0 / 0"}</span>
              <div>
                <button type="button" aria-label="提交记录上一页" disabled={data.page <= 0} onClick={() => setPage(current => Math.max(0, current - 1))}><ChevronLeft size={17} /></button>
                {paginationNumbers(data.totalPages, data.page).map(pageNumber => (
                  <button
                    type="button"
                    className={`student-submission-page-number${pageNumber === data.page ? " is-active" : ""}`}
                    aria-label={`第 ${pageNumber + 1} 页`}
                    aria-current={pageNumber === data.page ? "page" : undefined}
                    onClick={() => setPage(pageNumber)}
                    key={pageNumber}
                  >
                    {pageNumber + 1}
                  </button>
                ))}
                <button type="button" aria-label="提交记录下一页" disabled={data.page + 1 >= data.totalPages} onClick={() => setPage(current => current + 1)}><ChevronRight size={17} /></button>
              </div>
            </footer>
          </>
        ) : <EmptyState title="没有符合条件的提交记录" />}
      </section>

      {(detail || detailLoading) ? (
        <div className="student-submission-detail-backdrop" role="presentation" onClick={() => !detailLoading && setDetail(null)}>
          <section className="student-submission-detail" role="dialog" aria-modal="true" aria-labelledby="student-submission-detail-title" onClick={event => event.stopPropagation()}>
            <header>
              <div><span>提交详情</span><h2 id="student-submission-detail-title">{detail ? `#${detail.id} · ${detail.problemTitle || "题目"}` : "正在读取"}</h2></div>
              <button type="button" aria-label="关闭提交详情" onClick={() => setDetail(null)} disabled={detailLoading}><X size={19} /></button>
            </header>
            {detail ? (
              <>
                <div className="student-submission-detail__meta">
                  <strong>{verdictLabel(detail.verdict)}</strong><span>{detail.languageName || "-"}</span><span>{runtimeText(detail.executionTime)}</span><span>{memoryText(detail.memoryUsed)}</span>
                </div>
                {detail.growthSummary ? (
                  <section className="student-submission-growth-summary" aria-label={t("growthDashboard.detailAria")}>
                    <header>
                      <span>{t("growthDashboard.title")}</span>
                      <strong>{t(`growthDashboard.state.${growthStateKey(detail.growthSummary.growthState)}`)}</strong>
                    </header>
                    <div>
                      <span><small>{t("growthDashboard.metrics.tests")}</small><b>{detail.growthSummary.passedTestCases ?? 0}/{detail.growthSummary.totalTestCases ?? 0}</b></span>
                      <span><small>{t("growthDashboard.legend.improved")}</small><b>{(detail.growthSummary.notObservedCount || 0) + (detail.growthSummary.recoveredCount || 0)}</b></span>
                      <span><small>{t("growthDashboard.detailNewRisks")}</small><b>{(detail.growthSummary.newCount || 0) + (detail.growthSummary.recurringCount || 0)}</b></span>
                      <span><small>{t("growthDashboard.metrics.unresolved")}</small><b>{detail.growthSummary.unresolvedCount}</b></span>
                    </div>
                    {detail.assignmentId ? (
                      <ButtonLink to={`/app/student/assignments/${detail.assignmentId}/problems/${detail.problemId}`} variant="secondary">
                        {t("growthDashboard.openProblem")}
                      </ButtonLink>
                    ) : null}
                  </section>
                ) : null}
                <pre><code>{detail.sourceCode}</code></pre>
              </>
            ) : <EmptyState title="正在读取提交详情" live />}
          </section>
        </div>
      ) : null}
    </StudentAssignmentShell>
  );
}

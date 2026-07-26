import { useEffect, useMemo, useRef, useState } from "react";
import { ArrowLeft, ArrowRightLeft, CheckCircle2, GitCompareArrows, TriangleAlert } from "lucide-react";
import { Link, Navigate, useParams, useSearchParams } from "react-router-dom";
import { api } from "../../shared/api/client";
import type {
  SubmissionComparison,
  SubmissionGrowthIssueSignal,
  SubmissionHistorySummary
} from "../../shared/api/types";
import { useTranslation } from "../../shared/i18n";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Panel } from "../../shared/ui/Panel";
import { StatusPill } from "../../shared/ui/StatusPill";
import { StudentAssignmentShell, useStudentAssignmentWorkspace } from "./StudentAssignmentWorkspace";
import "./StudentSubmissionComparisonPage.css";

function numericParam(value: string | null) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

function optionLabel(item: SubmissionHistorySummary) {
  const time = item.submittedAt ? new Date(item.submittedAt).toLocaleString() : "-";
  return `#${item.id} · ${item.verdict} · ${time}`;
}

function issueStatusKey(status?: string | null) {
  return String(status || "UNCOMPARABLE").toLowerCase();
}

function issueSignals(item?: SubmissionHistorySummary | null) {
  return item?.growthSummary?.issueSignals || [];
}

function groupSignals(signals: SubmissionGrowthIssueSignal[]) {
  return signals.reduce<Record<string, SubmissionGrowthIssueSignal[]>>((groups, signal) => {
    const key = issueStatusKey(signal.changeStatus);
    groups[key] = [...(groups[key] || []), signal];
    return groups;
  }, {});
}

export default function StudentSubmissionComparisonPage() {
  const { t } = useTranslation();
  const { assignmentId, problemId } = useParams();
  const numericAssignmentId = Number(assignmentId);
  const numericProblemId = Number(problemId);
  const workspace = useStudentAssignmentWorkspace(numericAssignmentId);
  const [searchParams, setSearchParams] = useSearchParams();
  const initialLeftId = useRef(numericParam(searchParams.get("leftId")));
  const initialRightId = useRef(numericParam(searchParams.get("rightId")));
  const [history, setHistory] = useState<SubmissionHistorySummary[]>([]);
  const [leftId, setLeftId] = useState<number | null>(initialLeftId.current);
  const [rightId, setRightId] = useState<number | null>(initialRightId.current);
  const [comparison, setComparison] = useState<SubmissionComparison | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const requestVersion = useRef(0);

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setError(null);
    api.history(numericProblemId, numericAssignmentId)
      .then(items => {
        if (ignore) return;
        setHistory(items);
        const requestedRight = initialRightId.current;
        const target = items.find(item => item.id === requestedRight) || items[0] || null;
        const requestedLeft = initialLeftId.current;
        const recommendedLeft = target?.growthSummary?.comparisonSubmissionId || null;
        const fallbackLeft = items.find(item => item.id !== target?.id)?.id || null;
        setRightId(target?.id || null);
        setLeftId(items.some(item => item.id === requestedLeft && item.id !== target?.id)
          ? requestedLeft
          : recommendedLeft || fallbackLeft);
      })
      .catch(reason => {
        if (!ignore) setError(reason instanceof Error ? reason.message : t("submissionComparison.loadFailed"));
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [numericAssignmentId, numericProblemId, t]);

  useEffect(() => {
    if (!leftId || !rightId || leftId === rightId) {
      setComparison(null);
      return;
    }
    const version = ++requestVersion.current;
    setError(null);
    api.compareSubmissions(leftId, rightId)
      .then(result => {
        if (version === requestVersion.current) setComparison(result);
      })
      .catch(reason => {
        if (version === requestVersion.current) {
          setComparison(null);
          setError(reason instanceof Error ? reason.message : t("submissionComparison.loadFailed"));
        }
      });
  }, [leftId, rightId, t]);

  useEffect(() => {
    const next = new URLSearchParams();
    if (leftId) next.set("leftId", String(leftId));
    if (rightId) next.set("rightId", String(rightId));
    setSearchParams(next, { replace: true });
  }, [leftId, rightId, setSearchParams]);

  const left = history.find(item => item.id === leftId) || null;
  const right = history.find(item => item.id === rightId) || null;
  const recommendedBaseline = right?.growthSummary?.comparisonSubmissionId || null;
  const systemBaseline = Boolean(leftId && recommendedBaseline === leftId);
  const groupedIssues = useMemo(
    () => groupSignals(comparison?.issueDelta?.issueSignals || issueSignals(right)),
    [comparison?.issueDelta?.issueSignals, right]
  );
  const completeness = comparison?.comparability?.dataCompletenessStatus
    || right?.dataCompletenessStatus
    || right?.growthSummary?.dataCompletenessStatus
    || "COMPLETE";
  const problemTitle = comparison?.problemTitle || right?.problemTitle || left?.problemTitle || t("submissionComparison.problemFallback");

  function selectLeft(value: number | null) {
    setLeftId(value === rightId ? null : value);
  }

  function selectRight(value: number | null) {
    setRightId(value === leftId ? null : value);
  }

  function swap() {
    setLeftId(rightId);
    setRightId(leftId);
  }

  function restoreRecommended() {
    if (recommendedBaseline && recommendedBaseline !== rightId) setLeftId(recommendedBaseline);
  }

  if (!workspace.student) return <Navigate to="/app/student/login" replace />;
  if (workspace.loading) return <EmptyState title={t("submissionComparison.loading")} live />;
  if (!workspace.assignment) return <EmptyState title={workspace.failed || t("submissionComparison.assignmentMissing")} />;

  return (
    <StudentAssignmentShell assignment={workspace.assignment} student={workspace.student} nextTask={workspace.nextTask} activeTab="submissions">
      <div className="submission-comparison-page">
        <header className="submission-comparison-hero">
          <Link to={`/app/student/assignments/${numericAssignmentId}/submissions`}>
            <ArrowLeft size={17} />{t("submissionComparison.back")}
          </Link>
          <div>
            <span>{t("submissionComparison.eyebrow")}</span>
            <h1>{problemTitle}</h1>
            <p>{t("submissionComparison.intro")}</p>
          </div>
        </header>

        <Panel className="submission-comparison-picker" title={t("submissionComparison.chooseVersions")}>
          <div className="submission-comparison-picker__grid">
            <label>
              <span>{t("submissionComparison.baseline")}</span>
              <select value={leftId || ""} onChange={event => selectLeft(numericParam(event.target.value))}>
                <option value="">{t("submissionComparison.selectBaseline")}</option>
                {history.filter(item => item.id !== rightId).map(item => <option value={item.id} key={item.id}>{optionLabel(item)}</option>)}
              </select>
            </label>
            <button type="button" className="submission-comparison-swap" onClick={swap} disabled={!leftId || !rightId} aria-label={t("submissionComparison.swap")}>
              <ArrowRightLeft size={19} />
              <span>{t("submissionComparison.swap")}</span>
            </button>
            <label>
              <span>{t("submissionComparison.target")}</span>
              <select value={rightId || ""} onChange={event => selectRight(numericParam(event.target.value))}>
                <option value="">{t("submissionComparison.selectTarget")}</option>
                {history.filter(item => item.id !== leftId).map(item => <option value={item.id} key={item.id}>{optionLabel(item)}</option>)}
              </select>
            </label>
          </div>
          <div className="submission-comparison-picker__meta">
            <StatusPill tone={systemBaseline ? "success" : "neutral"}>
              {(comparison?.comparisonMode === "SYSTEM_BASELINE" || systemBaseline)
                ? t("submissionComparison.systemBaseline")
                : t("submissionComparison.manualComparison")}
            </StatusPill>
            {!systemBaseline && recommendedBaseline ? (
              <button type="button" onClick={restoreRecommended}>{t("submissionComparison.restoreRecommended", { id: recommendedBaseline })}</button>
            ) : null}
          </div>
        </Panel>

        {loading ? <EmptyState title={t("submissionComparison.loading")} live /> : error ? (
          <EmptyState title={error} />
        ) : history.length < 2 ? (
          <EmptyState title={t("submissionComparison.needTwo")} />
        ) : !comparison || !left || !right ? (
          <EmptyState title={t("submissionComparison.choosePrompt")} />
        ) : (
          <>
            <section className="submission-comparison-metrics" aria-label={t("submissionComparison.summaryAria")}>
              <article>
                <GitCompareArrows size={20} />
                <span>{t("submissionComparison.resultChange")}</span>
                <strong>{comparison.baseline.verdict || left.verdict} → {comparison.target.verdict || right.verdict}</strong>
              </article>
              <article>
                <CheckCircle2 size={20} />
                <span>{t("submissionComparison.testChange")}</span>
                <strong>{left.passedTestCases ?? "-"} → {right.passedTestCases ?? "-"}</strong>
              </article>
              <article>
                <TriangleAlert size={20} />
                <span>{t("submissionComparison.issueChange")}</span>
                <strong>{right.growthSummary?.unresolvedCount ?? "-"}</strong>
              </article>
              <article>
                <span>{t("submissionComparison.evidence")}</span>
                <strong>{completeness === "COMPLETE" ? t("submissionComparison.complete") : t("submissionComparison.incomplete")}</strong>
              </article>
            </section>

            <div className="submission-comparison-content">
              <Panel title={t("submissionComparison.growthEvidence")} className="submission-comparison-evidence">
                {comparison.progressSummary ? <p className="submission-comparison-summary">{comparison.progressSummary}</p> : null}
                {(comparison.causeChanges || []).length ? (
                  <ul>{comparison.causeChanges?.map((change, index) => <li key={`${change}-${index}`}>{change}</li>)}</ul>
                ) : null}
                {completeness !== "COMPLETE" ? (
                  <div className="submission-comparison-warning">
                    <TriangleAlert size={18} />
                    <span>{t("submissionComparison.incompleteNote")}</span>
                  </div>
                ) : null}
                <div className="submission-comparison-issues">
                  {["persisted", "new", "recurred", "not_observed", "recovered", "uncomparable"].map(status => {
                    const signals = groupedIssues[status] || [];
                    if (!signals.length) return null;
                    return (
                      <section key={status}>
                        <h3>{t(`submissionComparison.issueStatus.${status}`)} <small>{signals.length}</small></h3>
                        {signals.map((signal, index) => (
                          <article key={`${signal.normalizedPointKey || signal.title}-${index}`}>
                            <strong>{signal.title || t("submissionComparison.unnamedIssue")}</strong>
                            {signal.knowledgePath?.length ? <span>{signal.knowledgePath.join(" / ")}</span> : null}
                          </article>
                        ))}
                      </section>
                    );
                  })}
                </div>
              </Panel>

              <Panel title={t("submissionComparison.codeDiff")} className="submission-comparison-diff">
                <div className="submission-comparison-diff__stats">
                  <span className="is-add">+{comparison.diffStats?.addedLines || 0}</span>
                  <span className="is-remove">-{comparison.diffStats?.removedLines || 0}</span>
                  <span>{t("submissionComparison.unchanged", { count: comparison.diffStats?.unchangedLines || 0 })}</span>
                </div>
                <div className="submission-comparison-diff__lines" role="table" aria-label={t("submissionComparison.codeDiffAria")}>
                  {(comparison.diffLines || []).map((line, index) => (
                    <div className={`is-${line.type}`} role="row" key={`${line.type}-${index}`}>
                      <span role="cell">{line.leftLineNumber ?? ""}</span>
                      <span role="cell">{line.rightLineNumber ?? ""}</span>
                      <span className="sr-only" role="cell">{t(`submissionComparison.diffType.${line.type}`)}</span>
                      <code role="cell">{line.type === "add" ? "+" : line.type === "remove" ? "-" : " "}{line.content}</code>
                    </div>
                  ))}
                </div>
              </Panel>
            </div>
          </>
        )}
      </div>
    </StudentAssignmentShell>
  );
}

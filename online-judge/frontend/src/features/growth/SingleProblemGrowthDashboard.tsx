import { useMemo } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import type {
  SubmissionGrowthIssueSignal,
  SubmissionGrowthSummary,
  SubmissionHistorySummary
} from "../../shared/api/types";
import { useTranslation } from "../../shared/i18n";

type DashboardMode = "student" | "teacher";

type Props = {
  history: SubmissionHistorySummary[];
  selectedSubmissionId?: number | null;
  currentSummary?: SubmissionGrowthSummary | null;
  mode?: DashboardMode;
  onSelectSubmission?: (item: SubmissionHistorySummary) => void;
};

const COLORS = {
  persisted: "#2472d4",
  added: "#e6a32f",
  recurring: "#e66f61",
  improved: "#20a994",
  recovered: "#8ad3cc",
  neutral: "#98a2b3"
};

function normalizedStatus(value?: string | null) {
  return String(value || "UNCOMPARABLE").toUpperCase();
}

function pointKey(item: SubmissionGrowthIssueSignal) {
  return item.normalizedPointKey || item.title || "unclassified";
}

function issueTitle(item: SubmissionGrowthIssueSignal, fallback: string) {
  const path = item.knowledgePath || [];
  return item.title?.trim() || path[path.length - 1]?.trim() || fallback;
}

function currentSignals(summary?: SubmissionGrowthSummary | null) {
  return (summary?.issueSignals || []).filter(item => {
    if (String(item.displayCategory || "REPAIR").toUpperCase() === "IMPROVEMENT") return false;
    return ["NEW", "PERSISTED", "RECURRED"].includes(normalizedStatus(item.changeStatus));
  });
}

function rate(item: SubmissionHistorySummary) {
  const passed = item.growthSummary?.passedTestCases ?? item.passedTestCases ?? 0;
  const total = item.growthSummary?.totalTestCases ?? item.totalTestCases ?? 0;
  return total > 0 ? Math.round((passed / total) * 1000) / 10 : null;
}

function stateTone(state?: string | null) {
  switch (normalizedStatus(state)) {
    case "COMPLETED":
    case "CLEAR_PROGRESS":
      return "positive";
    case "MIXED_PROGRESS":
    case "ISSUE_SHIFTED":
      return "mixed";
    case "REGRESSED":
      return "negative";
    case "DUPLICATE_NO_CHANGE":
    case "UNCOMPARABLE":
      return "neutral";
    default:
      return "steady";
  }
}

export function growthStateKey(state?: string | null) {
  return normalizedStatus(state).toLowerCase();
}

export function SingleProblemGrowthDashboard({
  history,
  selectedSubmissionId,
  currentSummary,
  mode = "student",
  onSelectSubmission
}: Props) {
  const { t } = useTranslation();
  const ordered = useMemo(
    () => [...history].sort((left, right) => {
      const time = new Date(left.submittedAt || 0).getTime() - new Date(right.submittedAt || 0).getTime();
      return time || left.id - right.id;
    }),
    [history]
  );
  const selected = ordered.find(item => item.id === selectedSubmissionId) || ordered[ordered.length - 1] || null;
  const selectedSummary = currentSummary || selected?.growthSummary || null;
  const effective = ordered.filter(item => item.growthSummary?.effectiveAttempt && item.growthSummary.comparable);
  const trend = effective.map(item => ({
    id: item.id,
    label: `#${item.id}`,
    passRate: rate(item),
    verdict: item.verdict
  }));
  const shownHistory = ordered.slice(-6);
  const knowledge = useMemo(() => {
    const grouped = new Map<string, { key: string; title: string; count: number; path: string[] }>();
    ordered.forEach(item => {
      if (!item.growthSummary?.effectiveAttempt) return;
      currentSignals(item.growthSummary).forEach(signal => {
        const key = pointKey(signal);
        const existing = grouped.get(key) || {
          key,
          title: issueTitle(signal, t("growthDashboard.unnamedIssue")),
          count: 0,
          path: signal.knowledgePath || []
        };
        existing.count += 1;
        if ((signal.knowledgePath || []).length > existing.path.length) existing.path = signal.knowledgePath || [];
        grouped.set(key, existing);
      });
    });
    return [...grouped.values()].sort((left, right) => right.count - left.count || left.title.localeCompare(right.title)).slice(0, 5);
  }, [ordered, t]);
  const issueEvolution = shownHistory.map(item => ({
    id: item.id,
    label: `#${item.id}`,
    persisted: item.growthSummary?.persistedCount || 0,
    added: item.growthSummary?.newCount || 0,
    recurring: item.growthSummary?.recurringCount || 0,
    improved: item.growthSummary?.notObservedCount || 0,
    recovered: item.growthSummary?.recoveredCount || 0
  }));
  const matrixRows = knowledge.map(point => ({
    ...point,
    cells: shownHistory.map(item => {
      const signal = (item.growthSummary?.issueSignals || []).find(candidate => pointKey(candidate) === point.key);
      return { submission: item, status: signal ? normalizedStatus(signal.changeStatus) : null };
    })
  }));
  const currentPassed = selectedSummary?.passedTestCases ?? selected?.passedTestCases ?? 0;
  const currentTotal = selectedSummary?.totalTestCases ?? selected?.totalTestCases ?? 0;
  const breadcrumb = knowledge[0]?.path?.length ? knowledge[0].path.slice(0, 3) : [];

  return (
    <section className={`growth-dashboard growth-dashboard--${mode}`} aria-labelledby="growth-dashboard-title">
      <header className="growth-dashboard__header">
        <div>
          {breadcrumb.length ? <p className="growth-dashboard__breadcrumb">{breadcrumb.join(" / ")}</p> : null}
          <div className="growth-dashboard__title-row">
            <h2 id="growth-dashboard-title">{t("growthDashboard.title")}</h2>
            {selected ? <span>{t("growthDashboard.latestSubmission", { id: selected.id })}</span> : null}
            {selectedSummary?.comparisonSubmissionId ? (
              <span>{t("growthDashboard.comparedWith", { id: selectedSummary.comparisonSubmissionId })}</span>
            ) : null}
          </div>
        </div>
        {selectedSummary ? (
          <strong className={`growth-dashboard__state is-${stateTone(selectedSummary.growthState)}`}>
            {t(`growthDashboard.state.${growthStateKey(selectedSummary.growthState)}`)}
          </strong>
        ) : null}
      </header>

      <div className="growth-dashboard__kpis" aria-label={t("growthDashboard.metricsAria") }>
        <MetricFrame label={t("growthDashboard.metrics.submissions")} value={history.length} />
        <MetricFrame label={t("growthDashboard.metrics.effective")} value={effective.length} />
        <MetricFrame label={t("growthDashboard.metrics.tests")} value={currentTotal ? `${currentPassed}/${currentTotal}` : "-"} />
        <MetricFrame label={t("growthDashboard.metrics.unresolved")} value={selectedSummary?.unresolvedCount ?? "-"} />
      </div>

      <div className="growth-dashboard__primary-grid">
        <article className="growth-dashboard__surface growth-dashboard__trend">
          <SurfaceHeader title={t("growthDashboard.trendTitle")} />
          {trend.length >= 4 ? (
            <div className="growth-dashboard__chart" aria-label={t("growthDashboard.trendAria")}>
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={trend} margin={{ top: 12, right: 30, left: -8, bottom: 0 }}>
                  <CartesianGrid stroke="#e6eaf0" strokeDasharray="4 4" vertical={false} />
                  <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fill: "#475467", fontSize: 12 }} />
                  <YAxis domain={[0, 100]} ticks={[0, 25, 50, 75, 100]} axisLine={false} tickLine={false} tickFormatter={value => `${value}%`} tick={{ fill: "#667085", fontSize: 12 }} />
                  <Tooltip formatter={value => [`${value}%`, t("growthDashboard.passRate")]} />
                  <Line type="monotone" dataKey="passRate" stroke="#1769d2" strokeWidth={3} dot={{ r: 5, fill: "#1769d2", strokeWidth: 2, stroke: "#ffffff" }} activeDot={{ r: 7 }} connectNulls={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="growth-dashboard__slope" aria-label={t("growthDashboard.trendAria")}>
              {trend.length ? trend.map((point, index) => (
                <div key={point.id}>
                  <span>{point.label}</span>
                  <strong>{point.passRate === null ? "-" : `${point.passRate}%`}</strong>
                  {index < trend.length - 1 ? <i aria-hidden="true" /> : null}
                </div>
              )) : <p>{t("growthDashboard.noComparableAttempts")}</p>}
            </div>
          )}
        </article>

        <article className="growth-dashboard__surface growth-dashboard__knowledge">
          <SurfaceHeader title={t("growthDashboard.knowledgeTitle")} />
          {knowledge.length ? (
            <div className="growth-dashboard__chart" aria-label={t("growthDashboard.knowledgeAria")}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={knowledge} layout="vertical" margin={{ top: 6, right: 46, left: 4, bottom: 0 }}>
                  <XAxis type="number" hide domain={[0, "dataMax"]} />
                  <YAxis type="category" dataKey="title" width={82} axisLine={false} tickLine={false} tick={{ fill: "#344054", fontSize: 13 }} />
                  <Tooltip formatter={value => [value, t("growthDashboard.effectiveOccurrences")]} />
                  <Bar dataKey="count" fill="#1769d2" radius={[0, 5, 5, 0]} maxBarSize={28} label={{ position: "right", fill: "#344054", fontWeight: 700 }} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : <div className="growth-dashboard__empty">{t("growthDashboard.noKnowledgeData")}</div>}
        </article>
      </div>

      <article className="growth-dashboard__surface growth-dashboard__evolution">
        <SurfaceHeader title={t("growthDashboard.evolutionTitle")} />
        {issueEvolution.some(item => item.persisted + item.added + item.recurring + item.improved + item.recovered > 0) ? (
          <div className="growth-dashboard__evolution-layout">
            <div className="growth-dashboard__chart" aria-label={t("growthDashboard.evolutionAria")}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={issueEvolution} layout="vertical" margin={{ top: 4, right: 12, left: 0, bottom: 0 }}>
                  <XAxis type="number" hide />
                  <YAxis type="category" dataKey="label" width={58} axisLine={false} tickLine={false} tick={{ fill: "#344054", fontSize: 12, fontWeight: 700 }} />
                  <Tooltip />
                  <Bar dataKey="persisted" stackId="issues" fill={COLORS.persisted} name={t("growthDashboard.legend.persisted")} />
                  <Bar dataKey="added" stackId="issues" fill={COLORS.added} name={t("growthDashboard.legend.new")} />
                  <Bar dataKey="recurring" stackId="issues" fill={COLORS.recurring} name={t("growthDashboard.legend.recurred")} />
                  <Bar dataKey="improved" stackId="issues" fill={COLORS.improved} name={t("growthDashboard.legend.improved")} />
                  <Bar dataKey="recovered" stackId="issues" fill={COLORS.recovered} name={t("growthDashboard.legend.recovered")} radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <div className="growth-dashboard__legend" aria-label={t("growthDashboard.legendAria")}>
              {(["persisted", "new", "recurred", "improved", "recovered"] as const).map(key => (
                <span key={key}><i className={`is-${key}`} />{t(`growthDashboard.legend.${key}`)}</span>
              ))}
            </div>
          </div>
        ) : <div className="growth-dashboard__empty">{t("growthDashboard.noEvolutionData")}</div>}
      </article>

      <article className="growth-dashboard__surface growth-dashboard__matrix">
        <SurfaceHeader title={t("growthDashboard.matrixTitle")} />
        {matrixRows.length ? (
          <div className="growth-dashboard__matrix-scroll">
            <table>
              <thead>
                <tr>
                  <th scope="col">{t("growthDashboard.knowledgePoint")}</th>
                  {shownHistory.map(item => <th scope="col" key={item.id}>#{item.id}</th>)}
                </tr>
              </thead>
              <tbody>
                {matrixRows.map(row => (
                  <tr key={row.key}>
                    <th scope="row">{row.title}</th>
                    {row.cells.map(cell => (
                      <td key={cell.submission.id} className={cell.status ? `is-${cell.status.toLowerCase()}` : "is-empty"}>
                        <button type="button" disabled={!onSelectSubmission} onClick={() => onSelectSubmission?.(cell.submission)}>
                          {cell.status ? t(`growthDashboard.issueStatus.${cell.status.toLowerCase()}`) : "—"}
                        </button>
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : <div className="growth-dashboard__empty">{t("growthDashboard.noMatrixData")}</div>}
      </article>
    </section>
  );
}

function MetricFrame({ label, value }: { label: string; value: string | number }) {
  return <div><span>{label}</span><strong>{value}</strong></div>;
}

function SurfaceHeader({ title }: { title: string }) {
  return <header className="growth-dashboard__surface-head"><h3>{title}</h3></header>;
}

export function GrowthTimeline({
  history,
  selectedSubmissionId,
  onSelectSubmission
}: {
  history: SubmissionHistorySummary[];
  selectedSubmissionId?: number | null;
  onSelectSubmission: (item: SubmissionHistorySummary) => void;
}) {
  const { t } = useTranslation();
  return (
    <div className="growth-timeline" aria-label={t("growthTimeline.aria")}>
      {history.map(item => {
        const summary = item.growthSummary;
        const state = summary?.growthState || "UNCOMPARABLE";
        return (
          <button
            type="button"
            className={`growth-timeline__node is-${stateTone(state)}${selectedSubmissionId === item.id ? " is-active" : ""}`}
            onClick={() => onSelectSubmission(item)}
            key={item.id}
          >
            <i aria-hidden="true" />
            <span className="growth-timeline__main">
              <strong>#{item.id}</strong>
              <small>{t(`growthDashboard.state.${growthStateKey(state)}`)}</small>
            </span>
            <span className="growth-timeline__metrics">
              <b>{item.passedTestCases ?? summary?.passedTestCases ?? 0}/{item.totalTestCases ?? summary?.totalTestCases ?? 0}</b>
              {summary?.passedTestCaseDelta ? <small>{summary.passedTestCaseDelta > 0 ? "+" : ""}{summary.passedTestCaseDelta}</small> : null}
            </span>
            <span className="growth-timeline__issues">
              <small>{t("growthTimeline.issueDelta", {
                improved: (summary?.notObservedCount || 0) + (summary?.recoveredCount || 0),
                risk: (summary?.newCount || 0) + (summary?.recurringCount || 0)
              })}</small>
              {summary?.comparisonSubmissionId ? (
                <small>{t("growthDashboard.comparedWith", { id: summary.comparisonSubmissionId })}</small>
              ) : null}
              <time>{item.submittedAt ? new Date(item.submittedAt).toLocaleString() : "-"}</time>
            </span>
          </button>
        );
      })}
    </div>
  );
}

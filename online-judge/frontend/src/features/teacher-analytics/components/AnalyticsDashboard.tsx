import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import type { DiagnosisTag } from "../../../shared/api/types";
import { AnalyticsBarChart } from "../charts/AnalyticsBarChart";
import { AnalyticsPieChart } from "../charts/AnalyticsPieChart";
import type { AnalyticsGranularity, AnalyticsSnapshot, InsightBucket } from "../model";
import { formatPercent } from "../selectors";
import { AiKnowledgeInsightPanel } from "./AiKnowledgeInsightPanel";
import { AnalyticsSummaryCards } from "./AnalyticsSummaryCards";
import { EvidenceSamples } from "./EvidenceSamples";
import { GranularitySelector } from "./GranularitySelector";

type Props = {
  snapshot: AnalyticsSnapshot;
  t: (key: string, params?: Record<string, string | number>) => string;
  correction?: {
    tags: DiagnosisTag[];
    onSubmit: (
      sample: InsightBucket["evidence"][number],
      payload: {
        correctedIssueTag: string;
        correctedFineGrainedTag: string;
        correctionType: "DIAGNOSIS" | "KNOWLEDGE_PATH" | "EVIDENCE" | "ADVICE";
        targetIssueId: string;
        correctedKnowledgePath: string;
        targetEvidenceRef: string;
        teacherNote: string;
      }
    ) => Promise<void>;
  };
};

export function AnalyticsDashboard({ snapshot, t, correction }: Props) {
  const [granularity, setGranularity] = useState<AnalyticsGranularity>("chapter");
  const [metricMode, setMetricMode] = useState<"distinct" | "weighted">("distinct");
  const sourceBuckets = snapshot.insightBuckets[granularity] || [];
  const buckets = useMemo(() => {
    const ranked = sourceBuckets.map(item => ({
      ...item,
      count: metricMode === "distinct"
        ? item.affectedStudentCount || 0
        : item.effectiveWeightedOccurrenceCount ?? item.count
    }));
    const total = ranked.reduce((sum, item) => sum + item.count, 0);
    return ranked
      .map(item => ({ ...item, rate: total ? item.count / total : null }))
      .sort((left, right) => right.count - left.count || left.label.localeCompare(right.label, "zh-Hans-CN"));
  }, [metricMode, sourceBuckets]);
  const selected = useMemo(() => buckets[0] || null, [buckets]);
  const granularityLabels = {
    chapter: t("teacherAnalytics.granularity.chapter"),
    knowledgePoint: t("teacherAnalytics.granularity.knowledgePoint"),
    skillUnit: t("teacherAnalytics.granularity.skillUnit"),
    mistakePoint: t("teacherAnalytics.granularity.mistakePoint")
  };
  const activePath = selected?.path.map(node => node.label).join(" > ") || t("teacherAnalytics.empty.noKnowledgePath");

  return (
    <div className="teacher-analytics-dashboard">
      <AnalyticsSummaryCards metrics={snapshot.metrics} labelFor={t} />

      <section className="teacher-analytics-main-grid">
        <main className="teacher-analytics-board">
          <div className="teacher-analytics-board__head">
            <div>
              <span>{t("teacherAnalytics.sections.visualization")}</span>
              <h2>{t("teacherAnalytics.scopeTitle", { scope: t(`teacherAnalytics.scope.${snapshot.scope.type}`) })}</h2>
            </div>
            <div className="teacher-analytics-board__controls">
              <div className="teacher-analytics-metric-mode" role="group" aria-label={t("teacherAnalytics.metricMode.aria")}>
                {(["distinct", "weighted"] as const).map(mode => (
                  <button
                    type="button"
                    className={metricMode === mode ? "is-active" : ""}
                    onClick={() => setMetricMode(mode)}
                    aria-pressed={metricMode === mode}
                    key={mode}
                  >
                    {t(`teacherAnalytics.metricMode.${mode}`)}
                  </button>
                ))}
              </div>
              <GranularitySelector value={granularity} labels={granularityLabels} onChange={setGranularity} />
            </div>
          </div>

          <div className="teacher-analytics-chart-grid">
            <section className="teacher-analytics-chart-panel">
              <div className="teacher-analytics-section-head">
                <span>{granularityLabels[granularity]}</span>
                <h3>{t("teacherAnalytics.sections.ranking")}</h3>
              </div>
              <AnalyticsBarChart
                items={buckets}
                emptyText={t(emptyKey(snapshot.emptyReason))}
                countLabel={t(metricMode === "distinct" ? "teacherAnalytics.units.students" : "teacherAnalytics.units.effectiveAttempts")}
              />
            </section>
            <section className="teacher-analytics-chart-panel">
              <div className="teacher-analytics-section-head">
                <span>{granularityLabels[granularity]}</span>
                <h3>{t("teacherAnalytics.sections.share")}</h3>
              </div>
              <AnalyticsPieChart items={buckets} emptyText={t(emptyKey(snapshot.emptyReason))} />
            </section>
          </div>

          <section className="teacher-analytics-path-card">
            <span>{t("teacherAnalytics.sections.currentPath")}</span>
            <strong>{activePath}</strong>
            {selected ? (
              <>
                <p>
                  {t("teacherAnalytics.pathMetaDual", {
                    raw: selected.rawOccurrenceCount ?? selected.count,
                    weighted: selected.effectiveWeightedOccurrenceCount ?? selected.count,
                    students: selected.affectedStudentCount || 0,
                    repeated: selected.repeatedStudentCount || 0,
                    unresolved: selected.unresolvedStudentCount || 0,
                    recurring: selected.recurringStudentCount || 0,
                    problems: selected.affectedProblemCount || 0,
                    recovery: formatPercent(selected.recoveryRate)
                  })}
                </p>
                <small>
                  {t(`teacherAnalytics.difficulty.${difficultyKey(selected.difficultyClassification)}`)} · {t(`teacherAnalytics.pathStatus.${pathStatusKey(selected.pathStatus)}`)} · {t(`teacherAnalytics.fit.${selected.fit}`)}
                </small>
              </>
            ) : (
              <p>{t("teacherAnalytics.empty.noKnowledgePath")}</p>
            )}
          </section>

          {snapshot.assignmentRows.length && snapshot.scope.type === "class" ? (
            <section className="teacher-analytics-table-panel">
              <div className="teacher-analytics-section-head">
                <span>{t("teacherAnalytics.sections.assignments")}</span>
                <h3>{t("teacherAnalytics.tables.assignmentTitle")}</h3>
              </div>
              <div className="teacher-analytics-table">
                {snapshot.assignmentRows.map(row => (
                  <Link className="teacher-analytics-table-row" to={row.href} key={row.id}>
                    <strong>{row.title}</strong>
                    <span>{t("teacherAnalytics.tableLabels.status")} {row.status}</span>
                    <span>{t("teacherAnalytics.tableLabels.problemCount")} {row.problemCount} {t("teacherAnalytics.units.problem")}</span>
                    <span>{t("teacherAnalytics.tableLabels.submissions")} {row.submittedStudentCount}/{row.participantCount || "-"}</span>
                    <span>{t("teacherAnalytics.tableLabels.accuracy")} {formatPercent(row.passRate)}</span>
                  </Link>
                ))}
              </div>
            </section>
          ) : null}

          {snapshot.problemRows.length && snapshot.scope.type !== "problem" ? (
            <section className="teacher-analytics-table-panel">
              <div className="teacher-analytics-section-head">
                <span>{t("teacherAnalytics.sections.problems")}</span>
                <h3>{t("teacherAnalytics.tables.problemTitle")}</h3>
              </div>
              <div className="teacher-analytics-table">
                {snapshot.problemRows.map(row => (
                  <Link className="teacher-analytics-table-row" to={row.href} key={row.id}>
                    <strong>{row.title}</strong>
                    <span>{t("teacherAnalytics.tableLabels.submissions")} {row.submittedStudentCount}/{row.participantCount || "-"}</span>
                    <span>{t("teacherAnalytics.tableLabels.passed")} {row.passedStudentCount} {t("teacherAnalytics.units.passed")}</span>
                    <span>{t("teacherAnalytics.tableLabels.accuracy")} {formatPercent(row.passRate)}</span>
                    <span>{t("teacherAnalytics.tableLabels.issue")} {row.topIssue || t("teacherAnalytics.empty.noIssue")}</span>
                  </Link>
                ))}
              </div>
            </section>
          ) : null}
        </main>

        <div className="teacher-analytics-side">
          <AiKnowledgeInsightPanel
            title={t("teacherAnalytics.ai.title")}
            subtitle={t(`teacherAnalytics.ai.${snapshot.scope.type}`)}
            items={buckets}
            emptyText={t(emptyKey(snapshot.emptyReason))}
            fitLabel={fit => t(`teacherAnalytics.fit.${fit}`)}
          />
          <EvidenceSamples
            title={t("teacherAnalytics.evidence.title")}
            emptyText={t("teacherAnalytics.empty.noEvidence")}
            samples={selected?.evidence.length ? selected.evidence : snapshot.evidenceSamples}
            correction={
              correction
                ? {
                    title: t("teacherAnalytics.correction.title"),
                    issueLabel: t("teacherAnalytics.correction.issue"),
                    fineIssueLabel: t("teacherAnalytics.correction.fineIssue"),
                    typeLabel: t("teacherAnalytics.correction.type"),
                    diagnosisTypeLabel: t("teacherAnalytics.correction.types.diagnosis"),
                    knowledgePathTypeLabel: t("teacherAnalytics.correction.types.knowledgePath"),
                    evidenceTypeLabel: t("teacherAnalytics.correction.types.evidence"),
                    adviceTypeLabel: t("teacherAnalytics.correction.types.advice"),
                    knowledgePathLabel: t("teacherAnalytics.correction.knowledgePath"),
                    knowledgePathPlaceholder: t("teacherAnalytics.correction.knowledgePathPlaceholder"),
                    evidenceRefLabel: t("teacherAnalytics.correction.evidenceRef"),
                    evidenceRefPlaceholder: t("teacherAnalytics.correction.evidenceRefPlaceholder"),
                    noteLabel: t("teacherAnalytics.correction.note"),
                    submitLabel: t("teacherAnalytics.correction.submit"),
                    unavailableText: t("teacherAnalytics.correction.unavailable"),
                    tags: correction.tags,
                    onSubmit: correction.onSubmit
                  }
                : undefined
            }
            impactLabels={{
              title: t("teacherAnalytics.aiLoop.title"),
              noObservation: t("teacherAnalytics.aiLoop.noObservation"),
              noObservationDescription: t("teacherAnalytics.aiLoop.noObservationDescription"),
              followupEvidence: id => t("teacherAnalytics.aiLoop.followupEvidence", { id }),
              statusLabel: status => t(`teacherAnalytics.aiLoop.status.${feedbackImpactKey(status)}`),
              summary: status => t(`teacherAnalytics.aiLoop.summary.${feedbackImpactKey(status)}`)
            }}
          />
        </div>
      </section>
    </div>
  );
}

function emptyKey(reason?: string) {
  if (reason === "noAssignments") {
    return "teacherAnalytics.empty.noAssignments";
  }
  if (reason === "noSubmissions") {
    return "teacherAnalytics.empty.noSubmissions";
  }
  return "teacherAnalytics.empty.noInsight";
}

function feedbackImpactKey(status?: string | null) {
  switch (status) {
    case "IMPROVED_AFTER_AI":
      return "improved";
    case "SHIFTED_AFTER_AI":
      return "shifted";
    case "SAME_ISSUE_AFTER_AI":
      return "sameIssue";
    case "REGRESSED_AFTER_AI":
      return "regressed";
    case "VERDICT_CHANGED_AFTER_AI":
      return "verdictChanged";
    case "NO_CLEAR_CHANGE_AFTER_AI":
      return "noClearChange";
    default:
      return "awaiting";
  }
}

function pathStatusKey(status?: string | null) {
  const normalized = String(status || "UNCLASSIFIED").toLowerCase();
  return ["formal", "provisional", "inferred", "unclassified"].includes(normalized) ? normalized : "unclassified";
}

function difficultyKey(value?: string | null) {
  switch (value) {
    case "CLASS_DIFFICULTY":
      return "classDifficulty";
    case "INDIVIDUAL_PERSISTENT":
      return "individualPersistent";
    case "COMMON_ERROR":
      return "commonError";
    default:
      return "occasional";
  }
}

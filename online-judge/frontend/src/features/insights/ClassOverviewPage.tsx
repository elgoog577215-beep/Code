import { useEffect, useMemo, useState } from "react";
import { api } from "../../shared/api/client";
import type { LeaderboardEntry } from "../../shared/api/types";
import { difficultyLabel, percent } from "../../shared/format";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Metric } from "../../shared/ui/Metric";
import { Panel } from "../../shared/ui/Panel";
import { StatusPill } from "../../shared/ui/StatusPill";

export default function ClassOverviewPage() {
  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [alert, setAlert] = useState<string | null>(null);

  useEffect(() => {
    void api
      .classOverview()
      .then(setEntries)
      .catch(error => setAlert(error instanceof Error ? error.message : "班级概览加载失败。"));
  }, []);

  const totals = useMemo(() => {
    const submissions = entries.reduce((sum, item) => sum + (item.totalSubmissions || 0), 0);
    const accepted = entries.reduce((sum, item) => sum + (item.acceptedSubmissions || 0), 0);
    const avg = submissions ? (accepted / submissions) * 100 : 0;
    return { submissions, accepted, avg };
  }, [entries]);
  const reviewEntries = useMemo(() => entries.filter(item => (item.acceptanceRate || 0) < 40).slice(0, 3), [entries]);
  const stableEntries = useMemo(() => entries.filter(item => (item.acceptanceRate || 0) >= 70).slice(0, 2), [entries]);
  const activeEntries = useMemo(() => [...entries].sort((a, b) => (b.totalSubmissions || 0) - (a.totalSubmissions || 0)).slice(0, 2), [entries]);
  const idleCount = useMemo(() => entries.filter(item => (item.totalSubmissions || 0) === 0).length, [entries]);
  const actionPlan = useMemo(
    () => [
      {
        title: reviewEntries.length ? "需讲评" : "正常",
        note: reviewEntries.length ? reviewEntries[0].problemTitle : "无低通过率任务",
        tone: reviewEntries.length ? "warning" : "success"
      },
      {
        title: idleCount ? "未提交任务" : "均有提交",
        note: idleCount ? `${idleCount} 个` : "已覆盖",
        tone: idleCount ? "info" : "success"
      },
      {
        title: totals.avg < 50 ? "通过率偏低" : "通过率正常",
        note: percent(totals.avg),
        tone: totals.avg < 50 ? "warning" : "info"
      }
    ],
    [idleCount, reviewEntries, totals.avg]
  );

  return (
    <div className="stack class-overview-page">
      <section className="overview-command">
        <div className="overview-command__main">
          <h1>班级作业概览</h1>
        </div>
        <div className="overview-command__note">
          <StatusPill tone={reviewEntries.length ? "warning" : "success"}>{reviewEntries.length ? "需要关注" : "正常"}</StatusPill>
          <strong>{reviewEntries.length ? `${reviewEntries.length} 个任务` : "暂无需讲评"}</strong>
        </div>
      </section>

      {alert && <div className="alert alert--error">{alert}</div>}

      <div className="metric-grid">
        <Metric label="覆盖任务" value={entries.length} />
        <Metric label="总提交" value={totals.submissions} />
        <Metric label="通过提交" value={totals.accepted} />
        <Metric label="整体通过率" value={percent(totals.avg)} />
      </div>

      <section className="overview-action-plan" aria-label="课堂状态">
        {actionPlan.map(item => (
          <div className={`overview-action-card overview-action-card--${item.tone}`} key={item.title}>
            <span>{item.title}</span>
            <strong>{item.note}</strong>
          </div>
        ))}
      </section>

      <section className="overview-insight-grid" aria-label="班级数据">
        <Panel title="需讲评" action={<span className="meta-badge meta-badge--warning">{reviewEntries.length} 个</span>}>
          <InsightList entries={reviewEntries} emptyTitle="暂无需讲评任务" tone="warning" />
        </Panel>
        <Panel title="正常推进" action={<span className="meta-badge meta-badge--success">{stableEntries.length} 个</span>}>
          <InsightList entries={stableEntries} emptyTitle="还没有稳定任务" tone="success" />
        </Panel>
        <Panel title="提交较多" action={<span className="meta-badge meta-badge--info">{activeEntries.length} 个</span>}>
          <InsightList entries={activeEntries} emptyTitle="暂无提交数据" tone="info" />
        </Panel>
      </section>

      <details className="overview-compact-details">
        <summary>
          <span>任务表现</span>
          <StatusPill tone="neutral">{entries.length} 个任务</StatusPill>
        </summary>
        {!entries.length ? (
          <EmptyState title="暂无学习数据" />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>学习任务</th>
                  <th>难度</th>
                  <th>提交</th>
                  <th>通过</th>
                  <th>通过率</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                {entries.map(item => (
                  <tr key={item.problemId}>
                    <td data-label="学习任务">{item.problemTitle}</td>
                    <td data-label="难度">
                      <span className="meta-badge">{difficultyLabel(item.difficulty)}</span>
                    </td>
                    <td data-label="提交">{item.totalSubmissions}</td>
                    <td data-label="通过">{item.acceptedSubmissions}</td>
                    <td data-label="通过率">{percent(item.acceptanceRate || 0)}</td>
                    <td data-label="状态">
                      <StatusPill tone={(item.acceptanceRate || 0) < 40 ? "warning" : "success"}>
                        {(item.acceptanceRate || 0) < 40 ? "需讲评" : "正常"}
                      </StatusPill>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </details>
    </div>
  );
}

function InsightList({ entries, emptyTitle, tone }: { entries: LeaderboardEntry[]; emptyTitle: string; tone: "warning" | "success" | "info" }) {
  if (!entries.length) {
    return <EmptyState title={emptyTitle} />;
  }
  return (
    <div className="overview-insight-list">
      {entries.map(item => (
        <div className="overview-insight-item" key={`${tone}-${item.problemId}`}>
          <div>
            <span className="overview-insight-meta">{difficultyLabel(item.difficulty)}</span>
            <h3>{item.problemTitle}</h3>
            <p>{item.totalSubmissions} 次提交 · {item.acceptedSubmissions} 次通过</p>
          </div>
          <strong>{percent(item.acceptanceRate || 0)}</strong>
        </div>
      ))}
    </div>
  );
}

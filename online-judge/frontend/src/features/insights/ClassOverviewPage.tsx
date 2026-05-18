import { useEffect, useMemo, useState } from "react";
import { api } from "../../shared/api/client";
import type { LeaderboardEntry } from "../../shared/api/types";
import { percent } from "../../shared/format";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Metric } from "../../shared/ui/Metric";
import { Panel } from "../../shared/ui/Panel";
import { DifficultyPill, StatusPill } from "../../shared/ui/StatusPill";

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
  const reviewEntries = useMemo(() => entries.filter(item => (item.acceptanceRate || 0) < 40).slice(0, 4), [entries]);
  const stableEntries = useMemo(() => entries.filter(item => (item.acceptanceRate || 0) >= 70).slice(0, 4), [entries]);
  const activeEntries = useMemo(() => [...entries].sort((a, b) => (b.totalSubmissions || 0) - (a.totalSubmissions || 0)).slice(0, 4), [entries]);
  const idleCount = useMemo(() => entries.filter(item => (item.totalSubmissions || 0) === 0).length, [entries]);
  const actionPlan = useMemo(
    () => [
      {
        title: reviewEntries.length ? "建议讲评" : "推进正常",
        note: reviewEntries.length ? `先看 ${reviewEntries[0].problemTitle}。` : "暂无通过率偏低的任务。",
        tone: reviewEntries.length ? "warning" : "success"
      },
      {
        title: idleCount ? "未提交任务" : "均有提交",
        note: idleCount ? `${idleCount} 个任务还没有提交。` : "所有任务已有提交。",
        tone: idleCount ? "info" : "success"
      },
      {
        title: totals.avg < 50 ? "通过率偏低" : "通过率正常",
        note: totals.avg < 50 ? "建议检查题面、样例和课堂讲解。" : "继续观察后续提交。",
        tone: totals.avg < 50 ? "warning" : "info"
      }
    ],
    [idleCount, reviewEntries, totals.avg]
  );

  return (
    <div className="stack class-overview-page">
      <section className="overview-command">
        <div className="overview-command__main">
          <p className="eyebrow">班级学习概览</p>
          <h1>班级作业概览</h1>
          <p>查看任务参与、提交和通过情况。</p>
        </div>
        <div className="overview-command__note">
          <StatusPill tone="info">任务概览</StatusPill>
          <strong>{reviewEntries.length ? `${reviewEntries.length} 个任务建议讲评` : "暂无需讲评任务"}</strong>
        </div>
      </section>

      {alert && <div className="alert alert--error">{alert}</div>}

      <div className="metric-grid">
        <Metric label="覆盖任务" value={entries.length} />
        <Metric label="总提交" value={totals.submissions} />
        <Metric label="通过提交" value={totals.accepted} />
        <Metric label="整体通过率" value={percent(totals.avg)} />
      </div>

      <section className="overview-action-plan" aria-label="课堂建议">
        {actionPlan.map(item => (
          <div className={`overview-action-card overview-action-card--${item.tone}`} key={item.title}>
            <span>{item.title}</span>
            <strong>{item.note}</strong>
          </div>
        ))}
      </section>

      <section className="overview-insight-grid" aria-label="班级数据">
        <Panel title="建议讲评" eyebrow="通过率偏低" description="优先检查这些题目的题面、样例和讲解。">
          <InsightList entries={reviewEntries} emptyTitle="暂无需讲评任务" tone="warning" />
        </Panel>
        <Panel title="推进正常" eyebrow="通过率较高" description="这些任务整体完成较顺利。">
          <InsightList entries={stableEntries} emptyTitle="还没有稳定任务" tone="success" />
        </Panel>
        <Panel title="提交较多" eyebrow="课堂参与" description="这些任务提交次数较多。">
          <InsightList entries={activeEntries} emptyTitle="暂无提交数据" tone="info" />
        </Panel>
      </section>

      <Panel title="任务表现" eyebrow="提交数据" description="按任务查看提交数、通过数和通过率。">
        {!entries.length ? (
          <EmptyState title="暂无学习数据" description="学生完成提交后，这里会出现任务层面的过程概览。" />
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
                  <th>教师观察</th>
                </tr>
              </thead>
              <tbody>
                {entries.map(item => (
                  <tr key={item.problemId}>
                    <td>{item.problemTitle}</td>
                    <td>
                      <DifficultyPill difficulty={item.difficulty} />
                    </td>
                    <td>{item.totalSubmissions}</td>
                    <td>{item.acceptedSubmissions}</td>
                    <td>{percent(item.acceptanceRate || 0)}</td>
                    <td>
                      <StatusPill tone={(item.acceptanceRate || 0) < 40 ? "warning" : "success"}>
                        {(item.acceptanceRate || 0) < 40 ? "适合讲评" : "推进正常"}
                      </StatusPill>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>
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
            <DifficultyPill difficulty={item.difficulty} />
            <h3>{item.problemTitle}</h3>
            <p>{item.totalSubmissions} 次提交 · {item.acceptedSubmissions} 次通过</p>
          </div>
          <StatusPill tone={tone}>{percent(item.acceptanceRate || 0)}</StatusPill>
        </div>
      ))}
    </div>
  );
}

import { useEffect, useMemo, useState } from "react";
import { ArrowRight, CheckCircle2, KeyRound, UserRound } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { api } from "../../shared/api/client";
import type { Assignment, StudentAbilityProfile, StudentProfile, StudentRecommendation, StudentTrajectory } from "../../shared/api/types";
import { issueLabel, verdictLabel } from "../../shared/format";
import { loadStudent, saveStudent } from "../../shared/storage";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, TextInput } from "../../shared/ui/Field";
import { Panel } from "../../shared/ui/Panel";
import { DifficultyPill, StatusPill, VerdictPill } from "../../shared/ui/StatusPill";

export default function StudentPage() {
  const [searchParams] = useSearchParams();
  const [inviteCode, setInviteCode] = useState(searchParams.get("code") || "");
  const [assignment, setAssignment] = useState<Assignment | null>(null);
  const [student, setStudent] = useState<StudentProfile | null>(null);
  const [trajectory, setTrajectory] = useState<StudentTrajectory | null>(null);
  const [abilityProfile, setAbilityProfile] = useState<StudentAbilityProfile | null>(null);
  const [recommendation, setRecommendation] = useState<StudentRecommendation | null>(null);
  const [className, setClassName] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [studentNo, setStudentNo] = useState("");
  const [alert, setAlert] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const code = searchParams.get("code");
    if (code) {
      void resolveInvite(code);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!assignment) {
      return;
    }
    const restored = loadStudent(assignment.id);
    if (restored) {
      setStudent(restored);
      setDisplayName(restored.displayName || "");
      setStudentNo(restored.studentNo || "");
      void loadTrajectory(assignment.id, restored.id);
    }
    if (assignment.className) {
      setClassName(assignment.className);
    }
  }, [assignment]);

  const completion = useMemo(() => {
    if (!trajectory || trajectory.totalTasks === 0) {
      return 0;
    }
    return Math.round((trajectory.completedTasks / trajectory.totalTasks) * 100);
  }, [trajectory]);

  const assignmentTitle = useMemo(() => {
    if (!assignment) {
      return "";
    }
    return assignment.title.includes("试点任务") ? "课堂编程作业" : assignment.title;
  }, [assignment]);

  async function resolveInvite(rawCode = inviteCode) {
    const code = rawCode.trim();
    if (!code) {
      setAlert({ type: "error", message: "请输入老师提供的邀请码。" });
      return;
    }
    setBusy(true);
    try {
      const result = await api.resolveInvite(code);
      setAssignment(result);
      setInviteCode(code.toUpperCase());
      setAlert(null);
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "邀请码无效。" });
    } finally {
      setBusy(false);
    }
  }

  async function bindIdentity() {
    if (!assignment) {
      setAlert({ type: "error", message: "请先输入邀请码进入作业。" });
      return;
    }
    if (!displayName.trim()) {
      setAlert({ type: "error", message: "请填写姓名。" });
      return;
    }
    setBusy(true);
    try {
      const result = await api.bindStudent({
        assignmentId: assignment.id,
        classGroupId: assignment.classGroupId,
        className: className.trim(),
        displayName: displayName.trim(),
        studentNo: studentNo.trim()
      });
      saveStudent(assignment.id, result);
      setStudent(result);
      await loadTrajectory(assignment.id, result.id);
      setAlert(null);
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "身份确认失败。" });
    } finally {
      setBusy(false);
    }
  }

  async function loadTrajectory(assignmentId: number, studentProfileId: number) {
    try {
      const [trajectoryResult, profileResult, recommendationResult] = await Promise.all([
        api.studentTrajectory(assignmentId, studentProfileId),
        api.studentAbilityProfile(studentProfileId),
        api.studentRecommendations(studentProfileId)
      ]);
      setTrajectory(trajectoryResult);
      setAbilityProfile(profileResult);
      setRecommendation(recommendationResult);
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "提交记录加载失败。" });
    }
  }

  if (!assignment) {
    return (
      <div className="stack student-page">
        {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

        <section className="student-start">
          <div className="student-start__panel student-start__panel--compact">
            <h1>输入邀请码</h1>
            <form
              className="student-start__form"
              onSubmit={event => {
                event.preventDefault();
                void resolveInvite();
              }}
            >
              <Field label="邀请码">
                <TextInput
                  value={inviteCode}
                  onChange={event => setInviteCode(event.target.value)}
                  placeholder="例如 WZAI01"
                  autoComplete="off"
                />
              </Field>
              <Button type="submit" variant="primary" disabled={busy} icon={<KeyRound size={18} />}>
                进入作业
              </Button>
            </form>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="stack student-page">
      {alert?.type === "error" && <div className="alert alert--error">{alert.message}</div>}

      <section className={`student-assignment-grid ${student ? "is-ready" : "needs-identity"}`}>
        {!student && (
          <aside className="student-side-flow student-side-flow--identity">
            <Panel title="确认身份" action={<StatusPill tone="neutral">待确认</StatusPill>}>
              <form
                className="stack student-identity-form"
                onSubmit={event => {
                  event.preventDefault();
                  void bindIdentity();
                }}
              >
                <div className="form-grid form-grid--single">
                  <Field label="班级">
                    <TextInput value={className} onChange={event => setClassName(event.target.value)} placeholder="例如 高一1班" />
                  </Field>
                  <Field label="姓名">
                    <TextInput value={displayName} onChange={event => setDisplayName(event.target.value)} placeholder="请输入姓名" />
                  </Field>
                  <Field label="学号/座号">
                    <TextInput value={studentNo} onChange={event => setStudentNo(event.target.value)} placeholder="例如 12" />
                  </Field>
                </div>
                <Button type="submit" variant="primary" disabled={busy} icon={<UserRound size={18} />}>
                  确认身份
                </Button>
              </form>
            </Panel>
          </aside>
        )}

        <Panel
          className="student-task-panel"
          title={assignmentTitle}
          action={<StatusPill tone={student ? "success" : "neutral"}>{student ? "身份已确认" : "先确认身份"}</StatusPill>}
        >
          <div className="student-assignment-meta">
            <span>{assignment.tasks.length} 个题目</span>
            <span>邀请码 {assignment.inviteCode}</span>
          </div>

          {assignment.tasks.length === 0 ? (
            <EmptyState title="当前作业还没有题目" />
          ) : (
            <div className="student-task-list">
              {assignment.tasks.map((task, index) => {
                const query = new URLSearchParams();
                query.set("assignmentId", String(assignment.id));
                if (student) {
                  query.set("studentProfileId", String(student.id));
                }
                return (
                  <article className="task-card task-card--learning student-task-card" key={task.problemId}>
                    <div>
                      <div className="status-box-row">
                        <StatusPill tone="info">题目 {index + 1}</StatusPill>
                        <DifficultyPill difficulty={task.difficulty} />
                      </div>
                      <h3>{task.title}</h3>
                    </div>
                    <ButtonLink
                      to={`/app/problem/${task.problemId}?${query.toString()}`}
                      variant="primary"
                      disabled={!student}
                      icon={<ArrowRight size={18} />}
                    >
                      {student ? "开始" : "先确认身份"}
                    </ButtonLink>
                  </article>
                );
              })}
            </div>
          )}
        </Panel>

        {student && (
        <aside className="student-side-flow">
          <Panel
            title="学生信息"
            action={<StatusPill tone="success"><CheckCircle2 size={15} /> 已确认</StatusPill>}
          >
            <div className="student-identity-summary">
              <div>
                <span>姓名</span>
                <strong>{student.displayName}</strong>
              </div>
              <div>
                <span>班级</span>
                <strong>{className || student.className || "未填写"}</strong>
              </div>
              <div>
                <span>学号/座号</span>
                <strong>{student.studentNo || "未填写"}</strong>
              </div>
            </div>
          </Panel>

            <Panel
              title="提交结果"
              action={<StatusPill tone={trajectory ? "success" : "neutral"}>{trajectory?.stageTransition || "等待提交"}</StatusPill>}
            >
              {!trajectory ? (
                <EmptyState title="还没有提交记录" />
              ) : (
                <div className="stack">
                  <div className="student-record-summary">
                    <div>
                      <span>任务完成</span>
                      <strong>{trajectory.completedTasks}/{trajectory.totalTasks}</strong>
                    </div>
                    <div>
                      <span>提交次数</span>
                      <strong>{trajectory.totalAttempts}</strong>
                    </div>
                  </div>
                  <div className="progress-bar" aria-label="任务完成度">
                    <span style={{ "--progress": `${completion}%` } as React.CSSProperties} />
                  </div>
                  <div className="student-record-note">
                    <span>{trajectory.repeatedFineGrainedTag || trajectory.repeatedIssueTag ? "需要注意" : "下一步"}</span>
                    <strong>
                      {trajectory.repeatedFineGrainedTag
                        ? issueLabel(trajectory.repeatedFineGrainedTag)
                        : trajectory.repeatedIssueTag
                          ? issueLabel(trajectory.repeatedIssueTag)
                          : trajectory.nextStep || "继续完成一次提交"}
                    </strong>
                    {trajectory.attentionReason && <p>{trajectory.attentionReason}</p>}
                    {trajectory.improvementSignal && <p>{trajectory.improvementSignal}</p>}
                  </div>
                  {trajectory.latestCoachInteraction?.prompted && (
                    <div className="student-coach-summary">
                      <span>{trajectory.latestCoachInteraction.statusLabel || "AI 教练"}</span>
                      <strong>{trajectory.latestCoachInteraction.summary || "已进入 AI 追问。"}</strong>
                      {trajectory.latestCoachInteraction.latestFeedback && <p>{trajectory.latestCoachInteraction.latestFeedback}</p>}
                      {(trajectory.latestCoachImpact?.summary || trajectory.latestCoachInteraction.impact?.summary) && (
                        <p>{trajectory.latestCoachImpact?.summary || trajectory.latestCoachInteraction.impact?.summary}</p>
                      )}
                    </div>
                  )}
                  {abilityProfile && (
                    <div className="student-ability-profile">
                      <span>长期能力画像</span>
                      <strong>{abilityProfile.primaryAbilityFocus || "继续积累证据"}</strong>
                      {abilityProfile.summary && <p>{abilityProfile.summary}</p>}
                      {abilityProfile.coachImpactSummary && <p>{abilityProfile.coachImpactSummary}</p>}
                      {abilityProfile.recommendationEffectSummary && <p>{abilityProfile.recommendationEffectSummary}</p>}
                      <div className="student-ability-tags">
                        {abilityProfile.abilityGaps?.slice(0, 2).map(item => (
                          <span key={item.abilityPoint}>{item.abilityPoint} · {item.submissionCount} 次</span>
                        ))}
                        {abilityProfile.knowledgeFocus?.slice(0, 2).map(item => (
                          <span key={item.label}>{item.label}</span>
                        ))}
                      </div>
                    </div>
                  )}
                  {recommendation?.recommendations?.length ? (
                    <div className="student-recommendations">
                      <span>下一步推荐</span>
                      {recommendation.summary && <p>{recommendation.summary}</p>}
                      {recommendation.recommendations.slice(0, 3).map(item => {
                        const query = new URLSearchParams();
                        query.set("assignmentId", String(assignment.id));
                        query.set("studentProfileId", String(student.id));
                        if (item.recommendationToken) {
                          query.set("recommendationToken", item.recommendationToken);
                        }
                        return (
                          <div className="student-recommendation-item" key={`${item.type}-${item.problemId || item.title}`}>
                            <strong>{item.title}</strong>
                            {item.problemTitle && <p>{item.problemTitle}</p>}
                            {item.reason && <p className="student-recommendation-reason">{item.reason}</p>}
                            {item.focusTags?.length ? (
                              <div className="student-ability-tags">
                                {item.focusTags.slice(0, 3).map(tag => (
                                  <span key={tag}>{issueLabel(tag)}</span>
                                ))}
                              </div>
                            ) : null}
                            {item.problemId ? (
                              <ButtonLink
                                to={`/app/problem/${item.problemId}?${query.toString()}`}
                                variant="secondary"
                                onClick={() => {
                                  if (item.recommendationToken) {
                                    void api.recordRecommendationEvent(student.id, item.recommendationToken);
                                  }
                                }}
                                icon={<ArrowRight size={16} />}
                              >
                                {item.actionLabel || "去练习"}
                              </ButtonLink>
                            ) : null}
                          </div>
                        );
                      })}
                    </div>
                  ) : null}
                  <div className="student-latest-status">
                    <h3>每题状态</h3>
                    {trajectory.tasks.map(task => (
                      <div className="list-row" key={task.problemId}>
                        <div className="actions">
                          <DifficultyPill difficulty={task.difficulty} />
                          <VerdictPill verdict={task.latestVerdict} />
                        </div>
                        <h3>{task.title}</h3>
                        <p>
                          {task.attemptCount} 次尝试 · {task.latestHint || verdictLabel(task.latestVerdict)}
                        </p>
                        {task.latestImprovementSignal && <p>{task.latestImprovementSignal}</p>}
                        {task.latestCoachInteraction?.prompted && (
                          <p className="student-coach-inline">
                            {task.latestCoachInteraction.statusLabel} · {task.latestCoachInteraction.turnCount} 轮
                            {task.latestCoachImpact?.statusLabel ? ` · ${task.latestCoachImpact.statusLabel}` : ""}
                          </p>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </Panel>
        </aside>
        )}
      </section>
    </div>
  );
}

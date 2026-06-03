import { useEffect, useMemo, useState } from "react";
import { ArrowRight, CheckCircle2, KeyRound, UserRound } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { api } from "../../shared/api/client";
import type { Assignment, StudentProfile, StudentTrajectory } from "../../shared/api/types";
import { difficultyLabel, issueLabel, learningStageLabel, postAcTransferPhaseLabel, verdictLabel } from "../../shared/format";
import { loadInviteCode, loadStudent, saveInviteCode, saveStudent } from "../../shared/storage";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, TextInput } from "../../shared/ui/Field";
import { Panel } from "../../shared/ui/Panel";
import { StatusPill, VerdictPill } from "../../shared/ui/StatusPill";

export default function StudentPage() {
  const [searchParams] = useSearchParams();
  const [inviteCode, setInviteCode] = useState(() => searchParams.get("code") || loadInviteCode() || "WZAI01");
  const [assignment, setAssignment] = useState<Assignment | null>(null);
  const [student, setStudent] = useState<StudentProfile | null>(null);
  const [trajectory, setTrajectory] = useState<StudentTrajectory | null>(null);
  const [className, setClassName] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [studentNo, setStudentNo] = useState("");
  const [alert, setAlert] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const code = searchParams.get("code");
    if (code) {
      void resolveInvite(code);
      return;
    }
    void resolveInvite(inviteCode);
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

  const trajectoryTaskByProblemId = useMemo(() => {
    const tasks = new Map<number, StudentTrajectory["tasks"][number]>();
    trajectory?.tasks.forEach(task => tasks.set(task.problemId, task));
    return tasks;
  }, [trajectory]);

  const nextTask = useMemo(() => {
    if (!assignment?.tasks.length) {
      return null;
    }
    const pendingTrajectoryTask = trajectory?.tasks.find(task => !task.passed) || trajectory?.tasks[0] || null;
    if (!pendingTrajectoryTask) {
      return assignment.tasks[0];
    }
    return assignment.tasks.find(task => task.problemId === pendingTrajectoryTask.problemId) || assignment.tasks[0];
  }, [assignment, trajectory]);

  const postAcTransferTask = useMemo(() => {
    if (!trajectory?.postAcTransferSignal?.problemId) {
      return nextTask;
    }
    return assignment?.tasks.find(task => task.problemId === trajectory.postAcTransferSignal?.problemId) || nextTask;
  }, [assignment, nextTask, trajectory]);

  const primaryActionText = useMemo(() => {
    if (!nextTask) {
      return "";
    }
    if (!student) {
      return "先确认身份后开始作业。";
    }
    if (!trajectory) {
      return "从第一题开始，完成一次提交。";
    }
    if (trajectory.repeatedFineGrainedTag) {
      return `先处理 ${issueLabel(trajectory.repeatedFineGrainedTag)}。`;
    }
    if (trajectory.repeatedIssueTag) {
      return `先处理 ${issueLabel(trajectory.repeatedIssueTag)}。`;
    }
    if (trajectory.nextStep) {
      return learningStageLabel(trajectory.nextStep);
    }
    return trajectory.completedTasks >= trajectory.totalTasks ? "作业已完成，可以复盘通过题。" : "继续下一题。";
  }, [nextTask, student, trajectory]);

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
      saveInviteCode(code);
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
      setTrajectory(await api.studentTrajectory(assignmentId, studentProfileId));
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

      <section className={`student-assignment-grid student-assignment-grid--linear ${student ? "is-ready" : "needs-identity"}`}>
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
          <div className="student-assignment-bar">
            {student ? (
              <div className="student-identity-strip">
                <span>
                  <CheckCircle2 size={15} /> {student.displayName}
                </span>
                <span>{className || student.className || "未填写班级"}</span>
                <span>{student.studentNo ? `座号 ${student.studentNo}` : "座号未填写"}</span>
              </div>
            ) : (
              <span className="student-assignment-bar__hint">确认身份后开始作业</span>
            )}
            <div className="student-assignment-meta">
              <span>{assignment.tasks.length} 题</span>
              <span>邀请码 {assignment.inviteCode}</span>
              {trajectory && <span>完成 {trajectory.completedTasks}/{trajectory.totalTasks}</span>}
            </div>
          </div>

          {student && nextTask && (
            <section className="student-current-action" aria-label="当前要做">
              <div>
                <span>当前要做</span>
                <strong>{nextTask.title}</strong>
                <p>{primaryActionText}</p>
              </div>
              <ButtonLink
                to={`/app/problem/${nextTask.problemId}?assignmentId=${assignment.id}&studentProfileId=${student.id}`}
                variant="primary"
                icon={<ArrowRight size={18} />}
              >
                {trajectory?.tasks.find(task => task.problemId === nextTask.problemId)?.attemptCount ? "继续处理" : "开始"}
              </ButtonLink>
            </section>
          )}

          {assignment.tasks.length === 0 ? (
            <EmptyState title="当前作业还没有题目" />
          ) : (
            <div className="student-task-list">
              {assignment.tasks.map((task, index) => {
                const taskState = trajectoryTaskByProblemId.get(task.problemId);
                const isNextTask = nextTask?.problemId === task.problemId;
                const isPassed = Boolean(taskState?.passed || taskState?.latestVerdict === "ACCEPTED");
                const query = new URLSearchParams();
                query.set("assignmentId", String(assignment.id));
                if (student) {
                  query.set("studentProfileId", String(student.id));
                }
                const actionLabel = !student
                  ? "先确认身份"
                  : isPassed
                    ? "复盘"
                    : isNextTask && taskState?.attemptCount
                      ? "继续处理"
                      : taskState?.attemptCount
                        ? "继续"
                        : "开始";
                const actionNote = isNextTask && !isPassed
                  ? trajectory?.nextStep
                    ? learningStageLabel(trajectory.nextStep)
                    : taskState?.latestHint
                      ? learningStageLabel(taskState.latestHint)
                      : "先完成这题的下一次提交。"
                  : isPassed
                    ? taskState?.latestProgressSignal || "已通过，可复盘保留思路。"
                    : taskState?.latestHint
                      ? learningStageLabel(taskState.latestHint)
                      : "还未开始。";
                return (
                  <article
                    className={`task-card task-card--learning student-task-card ${isNextTask && !isPassed ? "is-next" : ""} ${isPassed ? "is-passed" : ""}`}
                    key={task.problemId}
                  >
                    <span className="student-task-index">{index + 1}</span>
                    <div className="student-task-main">
                      <div className="student-task-meta-line">
                        <span>{difficultyLabel(task.difficulty)}</span>
                        {isNextTask && !isPassed && <StatusPill tone="info">下一步</StatusPill>}
                        {taskState?.latestVerdict && <VerdictPill verdict={taskState.latestVerdict} />}
                      </div>
                      <h3>{task.title}</h3>
                      <p className="student-task-action-note">{actionNote}</p>
                    </div>
                    <div className="student-task-status">
                      <ButtonLink
                        to={`/app/problem/${task.problemId}?${query.toString()}`}
                        variant={isNextTask && !isPassed ? "primary" : "secondary"}
                        disabled={!student}
                        icon={<ArrowRight size={18} />}
                      >
                        {actionLabel}
                      </ButtonLink>
                    </div>
                  </article>
                );
              })}
            </div>
          )}

          {student && (
            <details className="student-learning-drawer">
              <summary>
                <span>学习记录</span>
                <StatusPill tone={trajectory ? "success" : "neutral"}>{trajectory ? `${trajectory.completedTasks}/${trajectory.totalTasks}` : "等待提交"}</StatusPill>
              </summary>
              {!trajectory ? (
                <EmptyState title="还没有提交记录" />
              ) : (
                <div className="stack">
                  {trajectory.postAcTransferSignal &&
                    ["JUST_ACCEPTED", "REFLECTION_NEEDED"].includes((trajectory.postAcTransferSignal.phase || "").toUpperCase()) && (
                      <div className="student-transfer-action">
                        <span>{postAcTransferPhaseLabel(trajectory.postAcTransferSignal.phase)}</span>
                        <strong>{trajectory.postAcTransferSignal.problemTitle || postAcTransferTask?.title || "通过后复盘"}</strong>
                        <p>{trajectory.postAcTransferSignal.recommendedAction || trajectory.postAcTransferSignal.summary}</p>
                        {postAcTransferTask && (
                          <ButtonLink
                            to={`/app/problem/${postAcTransferTask.problemId}?assignmentId=${assignment.id}&studentProfileId=${student.id}`}
                            variant="secondary"
                            icon={<ArrowRight size={16} />}
                          >
                            复盘
                          </ButtonLink>
                        )}
                      </div>
                    )}
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
                    <span>当前阶段</span>
                    <strong>
                      {trajectory.repeatedFineGrainedTag
                        ? issueLabel(trajectory.repeatedFineGrainedTag)
                        : trajectory.repeatedIssueTag
                          ? issueLabel(trajectory.repeatedIssueTag)
                          : trajectory.nextStep
                            ? learningStageLabel(trajectory.nextStep)
                            : "待提交"}
                    </strong>
                  </div>
                  {trajectory.latestCoachInteraction?.prompted && (
                    <div className="student-coach-summary">
                      <span>
                        {trajectory.latestCoachInteraction.statusLabel
                          ? learningStageLabel(trajectory.latestCoachInteraction.statusLabel)
                          : "追问"}
                      </span>
                      <strong>{trajectory.latestCoachInteraction.answered ? "已回答" : "待回答"}</strong>
                    </div>
                  )}
                  <details className="student-latest-status">
                    <summary>
                      <span>每题状态</span>
                      <StatusPill tone="neutral">{trajectory.tasks.length} 题</StatusPill>
                    </summary>
                    {trajectory.tasks.map(task => (
                      <div className="list-row" key={task.problemId}>
                        <div className="actions">
                          <span className="meta-badge">{difficultyLabel(task.difficulty)}</span>
                          <VerdictPill verdict={task.latestVerdict} />
                        </div>
                        <h3>{task.title}</h3>
                        <p>
                          {task.attemptCount} 次尝试 · {task.latestHint ? learningStageLabel(task.latestHint) : verdictLabel(task.latestVerdict)}
                        </p>
                        {task.postAcTransferSignal && task.postAcTransferSignal.phase !== "NOT_ACCEPTED" && (
                          <small>{postAcTransferPhaseLabel(task.postAcTransferSignal.phase)}</small>
                        )}
                      </div>
                    ))}
                  </details>
                </div>
              )}
            </details>
          )}
        </Panel>
      </section>
    </div>
  );
}

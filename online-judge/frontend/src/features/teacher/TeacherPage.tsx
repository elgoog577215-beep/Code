import { useEffect, useMemo, useState } from "react";
import { ArrowRight, ChartNoAxesColumnIncreasing, PenLine, RefreshCw, Settings } from "lucide-react";
import { ApiError, api } from "../../shared/api/client";
import type { Assignment, AssignmentOverview, ClassGroup, ProblemCatalogItem } from "../../shared/api/types";
import { assignmentStatusLabel, displayText, hintPolicyLabel, looksCorruptText } from "../../shared/format";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea, TextInput } from "../../shared/ui/Field";
import { DifficultyPill, StatusPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };
type AssignmentForm = {
  id: string;
  title: string;
  description: string;
  classGroupId: string;
  hintPolicy: string;
  status: string;
  problemIds: number[];
};

const EMPTY_ASSIGNMENT: AssignmentForm = {
  id: "",
  title: "",
  description: "",
  classGroupId: "",
  hintPolicy: "L2",
  status: "ACTIVE",
  problemIds: []
};

function cleanAssignmentTitle(value?: string | null, fallback = "课堂作业") {
  const title = displayText(value, fallback);
  return title.includes("试点任务") ? "课堂编程作业" : title;
}

function cleanAssignmentDescription(value?: string | null) {
  const description = displayText(value, "");
  if (description.includes("演示") || description.includes("诊断")) {
    return "";
  }
  return description;
}

function teacherErrorMessage(error: unknown, fallback: string) {
  const base = fallback.replace(/[。.!！?？\s]+$/, "");
  if (error instanceof ApiError) {
    if (error.status >= 500) {
      return `${base}，服务暂时不可用。`;
    }
    if (error.status === 404) {
      return `${base}，未找到课堂资源。`;
    }
    return `${base}，${error.message}`;
  }
  const detail = error instanceof Error ? error.message : "";
  return detail ? `${base}，${detail}` : fallback;
}

function passRate(overview?: AssignmentOverview | null) {
  return overview?.attemptCount ? Math.round((overview.passedAttemptCount / overview.attemptCount) * 100) : 0;
}

function attentionCount(overview?: AssignmentOverview | null) {
  return overview?.students.filter(student => student.needsAttention).length || 0;
}

export default function TeacherPage() {
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [classes, setClasses] = useState<ClassGroup[]>([]);
  const [problems, setProblems] = useState<ProblemCatalogItem[]>([]);
  const [overviewByAssignment, setOverviewByAssignment] = useState<Record<number, AssignmentOverview | null>>({});
  const [form, setForm] = useState<AssignmentForm>(EMPTY_ASSIGNMENT);
  const [alert, setAlert] = useState<Alert | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    void loadTeacherHome();
  }, []);

  const cleanAssignments = useMemo(
    () =>
      assignments
        .filter(item => !looksCorruptText(item.title))
        .map(item => ({
          ...item,
          title: cleanAssignmentTitle(item.title, `课堂作业 #${item.id}`),
          description: cleanAssignmentDescription(item.description),
          className: displayText(item.className, "未绑定班级")
        })),
    [assignments]
  );

  const activeAssignments = useMemo(() => cleanAssignments.filter(item => item.status !== "DRAFT"), [cleanAssignments]);
  const draftAssignments = useMemo(() => cleanAssignments.filter(item => item.status === "DRAFT"), [cleanAssignments]);
  const totalParticipants = useMemo(
    () =>
      Object.values(overviewByAssignment).reduce(
        (sum, overview) => sum + (overview?.participantCount || 0),
        0
      ),
    [overviewByAssignment]
  );
  const totalAttention = useMemo(
    () => Object.values(overviewByAssignment).reduce((sum, overview) => sum + attentionCount(overview), 0),
    [overviewByAssignment]
  );
  const averagePassRate = useMemo(() => {
    const overviews = Object.values(overviewByAssignment).filter(Boolean) as AssignmentOverview[];
    const attempts = overviews.reduce((sum, overview) => sum + overview.attemptCount, 0);
    const passed = overviews.reduce((sum, overview) => sum + overview.passedAttemptCount, 0);
    return attempts ? Math.round((passed / attempts) * 100) : 0;
  }, [overviewByAssignment]);

  async function loadTeacherHome() {
    setLoading(true);
    setAlert(null);
    try {
      const [assignmentResult, classResult, problemResult] = await Promise.all([
        api.assignments(),
        api.classes(),
        api.problemCatalog()
      ]);
      setAssignments(assignmentResult);
      setClasses(classResult);
      setProblems(problemResult);
      if (!assignmentResult.length) {
        setOverviewByAssignment({});
        return;
      }
      const overviewEntries = await Promise.all(
        assignmentResult.map(async assignment => {
          try {
            return [assignment.id, await api.assignmentOverview(assignment.id)] as const;
          } catch {
            return [assignment.id, null] as const;
          }
        })
      );
      setOverviewByAssignment(Object.fromEntries(overviewEntries));
    } catch (error) {
      setAlert({ type: "error", message: teacherErrorMessage(error, "教师端数据读取失败。") });
    } finally {
      setLoading(false);
    }
  }

  function editAssignment(assignment: Assignment) {
    setForm({
      id: String(assignment.id),
      title: cleanAssignmentTitle(assignment.title, ""),
      description: cleanAssignmentDescription(assignment.description),
      classGroupId: assignment.classGroupId ? String(assignment.classGroupId) : "",
      hintPolicy: assignment.hintPolicy || "L2",
      status: assignment.status || "ACTIVE",
      problemIds: (assignment.tasks || []).map(task => task.problemId)
    });
    requestAnimationFrame(() => {
      document.querySelector(".teacher-assignment-composer")?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  }

  function toggleProblem(problemId: number) {
    setForm(current => ({
      ...current,
      problemIds: current.problemIds.includes(problemId)
        ? current.problemIds.filter(id => id !== problemId)
        : [...current.problemIds, problemId]
    }));
  }

  async function saveAssignment() {
    if (!form.title.trim()) {
      setAlert({ type: "error", message: "请填写作业名称。" });
      return;
    }
    if (!form.classGroupId) {
      setAlert({ type: "error", message: "请选择要布置的班级。" });
      return;
    }
    if (!form.problemIds.length) {
      setAlert({ type: "error", message: "请至少选择一道题目。" });
      return;
    }
    setSaving(true);
    try {
      const payload = {
        title: form.title.trim(),
        description: form.description.trim(),
        classGroupId: Number(form.classGroupId),
        hintPolicy: form.hintPolicy,
        status: form.status,
        problemIds: form.problemIds
      };
      const saved = form.id ? await api.updateAssignment(Number(form.id), payload) : await api.createAssignment(payload);
      setAlert({ type: "success", message: `${cleanAssignmentTitle(saved.title)} 已保存。` });
      setForm(EMPTY_ASSIGNMENT);
      await loadTeacherHome();
    } catch (error) {
      setAlert({ type: "error", message: teacherErrorMessage(error, "作业保存失败。") });
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="teacher-page teacher-assignment-center">
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="teacher-home-command">
        <div>
          <p className="eyebrow">教师端</p>
          <h1>作业中心</h1>
          <p>按班级布置作业，进入作业后查看整体统计、题目表现和学生细致情况。</p>
        </div>
        <div className="teacher-home-actions">
          <Button type="button" variant="ghost" onClick={() => void loadTeacherHome()} disabled={loading} icon={<RefreshCw size={17} />}>
            {loading ? "刷新中" : "刷新"}
          </Button>
          <ButtonLink to="/app/teacher-management" variant="secondary" icon={<Settings size={17} />}>
            管理班级与题目
          </ButtonLink>
          <ButtonLink to="/app/class-overview" variant="secondary" icon={<ChartNoAxesColumnIncreasing size={17} />}>
            总体统计
          </ButtonLink>
        </div>
      </section>

      <section className="teacher-home-summary" aria-label="作业中心概览">
        <div>
          <span>进行中作业</span>
          <strong>{activeAssignments.length}</strong>
        </div>
        <div>
          <span>参与学生</span>
          <strong>{totalParticipants}</strong>
        </div>
        <div>
          <span>平均通过率</span>
          <strong>{averagePassRate}%</strong>
        </div>
        <div>
          <span>需关注学生</span>
          <strong>{totalAttention}</strong>
        </div>
      </section>

      <nav className="teacher-assignment-list" aria-label="教师作业入口">
        {loading && !cleanAssignments.length ? (
          <EmptyState title="正在读取作业" />
        ) : cleanAssignments.length ? (
          cleanAssignments.map(assignment => {
            const overview = overviewByAssignment[assignment.id];
            const taskCount = assignment.tasks?.length || 0;
            const count = attentionCount(overview);
            return (
              <article className="teacher-assignment-card" key={assignment.id}>
                <div className="teacher-assignment-card__main">
                  <div className="teacher-assignment-card__title">
                    <StatusPill tone={assignment.status === "ACTIVE" ? "success" : assignment.status === "DRAFT" ? "neutral" : "info"}>
                      {assignmentStatusLabel(assignment.status)}
                    </StatusPill>
                    <h2>{assignment.title}</h2>
                  </div>
                  <div className="teacher-assignment-card__meta">
                    <span>{assignment.className}</span>
                    <span>{taskCount} 题</span>
                    <span>{hintPolicyLabel(assignment.hintPolicy)}</span>
                  </div>
                </div>
                <div className="teacher-assignment-card__stats" aria-label={`${assignment.title} 作业摘要`}>
                  <div>
                    <span>参与</span>
                    <strong>{overview?.participantCount ?? "-"}</strong>
                  </div>
                  <div>
                    <span>提交</span>
                    <strong>{overview?.attemptCount ?? "-"}</strong>
                  </div>
                  <div>
                    <span>通过率</span>
                    <strong>{overview ? `${passRate(overview)}%` : "-"}</strong>
                  </div>
                  <div className={count ? "is-warning" : ""}>
                    <span>需关注</span>
                    <strong>{overview ? count : "-"}</strong>
                  </div>
                </div>
                <div className="teacher-assignment-card__actions">
                  <Button type="button" variant="ghost" icon={<PenLine size={16} />} onClick={() => editAssignment(assignment)}>
                    编辑
                  </Button>
                  <ButtonLink to={`/app/teacher/assignment/${assignment.id}`} variant="primary" icon={<ArrowRight size={17} />}>
                    进入作业
                  </ButtonLink>
                </div>
              </article>
            );
          })
        ) : (
          <EmptyState title="还没有作业" description="先选择班级和题目，发布第一份课堂作业。" />
        )}
      </nav>

      {draftAssignments.length ? (
        <section className="teacher-home-note" aria-label="草稿提醒">
          <strong>{draftAssignments.length} 份草稿待发布</strong>
          <span>草稿不会出现在学生端正式作业中，编辑后可改为进行中。</span>
        </section>
      ) : null}

      <details className="teacher-assignment-composer">
        <summary>
          <span>{form.id ? "编辑作业" : "新建作业"}</span>
          <StatusPill tone="info">选择班级 + 选择题目 + 发布作业</StatusPill>
        </summary>
        <div className="teacher-assignment-composer__body">
          <div className="form-grid">
            <Field label="作业名称">
              <TextInput
                value={form.title}
                onChange={event => setForm({ ...form, title: event.target.value })}
                placeholder="例如：循环边界练习"
              />
            </Field>
            <Field label="选择班级">
              <Select value={form.classGroupId} onChange={event => setForm({ ...form, classGroupId: event.target.value })}>
                <option value="">请选择班级</option>
                {classes.map(item => (
                  <option value={item.id} key={item.id}>
                    {displayText(item.name, `班级 #${item.id}`)}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="发布状态">
              <Select value={form.status} onChange={event => setForm({ ...form, status: event.target.value })}>
                <option value="ACTIVE">进行中</option>
                <option value="DRAFT">草稿</option>
                <option value="CLOSED">已结束</option>
              </Select>
            </Field>
          </div>

          <Field label="作业说明">
            <TextArea
              value={form.description}
              onChange={event => setForm({ ...form, description: event.target.value })}
              placeholder="写给学生看的简短说明。"
            />
          </Field>

          <Field label="选择题目">
            <div className="teacher-problem-pick-list teacher-problem-pick-list--simple">
              {problems.map(problem => (
                <label className="list-row" key={problem.id}>
                  <div className="actions">
                    <input type="checkbox" checked={form.problemIds.includes(problem.id)} onChange={() => toggleProblem(problem.id)} />
                    <DifficultyPill difficulty={problem.difficulty} />
                  </div>
                  <h3>{problem.title}</h3>
                </label>
              ))}
            </div>
          </Field>

          <div className="actions">
            <Button type="button" variant="primary" onClick={() => void saveAssignment()} disabled={saving}>
              {saving ? "保存中" : form.id ? "保存作业" : "发布作业"}
            </Button>
            <Button type="button" variant="ghost" onClick={() => setForm(EMPTY_ASSIGNMENT)}>
              清空
            </Button>
            <ButtonLink to="/app/task-editor" variant="ghost" icon={<PenLine size={17} />}>
              编辑题目
            </ButtonLink>
          </div>
        </div>
      </details>
    </div>
  );
}

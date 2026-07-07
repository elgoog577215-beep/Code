import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, CheckCircle2, Search, X } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { ApiError, api } from "../../shared/api/client";
import type { ClassGroup, Difficulty, ProblemCatalogItem } from "../../shared/api/types";
import { difficultyLabel, displayText } from "../../shared/format";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea, TextInput } from "../../shared/ui/Field";
import { DifficultyPill, StatusPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };
type AssignmentForm = {
  title: string;
  description: string;
  classGroupId: string;
  hintPolicy: string;
  status: string;
  problemIds: number[];
};
type DifficultyFilter = "ALL" | Difficulty;

const EMPTY_ASSIGNMENT: AssignmentForm = {
  title: "",
  description: "",
  classGroupId: "",
  hintPolicy: "L2",
  status: "ACTIVE",
  problemIds: []
};

const DIFFICULTY_FILTERS: Array<{ value: DifficultyFilter; label: string }> = [
  { value: "ALL", label: "全部难度" },
  { value: "EASY", label: "基础" },
  { value: "MEDIUM", label: "提高" },
  { value: "HARD", label: "挑战" }
];
const DEFAULT_CLASS_NAME = "默认班级";

function cleanAssignmentTitle(value?: string | null, fallback = "课堂作业") {
  const title = displayText(value, fallback);
  return title.includes("试点任务") ? "课堂编程作业" : title;
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

export default function AssignmentCreatePage() {
  const navigate = useNavigate();
  const [classes, setClasses] = useState<ClassGroup[]>([]);
  const [problems, setProblems] = useState<ProblemCatalogItem[]>([]);
  const [form, setForm] = useState<AssignmentForm>(EMPTY_ASSIGNMENT);
  const [query, setQuery] = useState("");
  const [difficulty, setDifficulty] = useState<DifficultyFilter>("ALL");
  const [alert, setAlert] = useState<Alert | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    void loadCreateContext();
  }, []);

  const selectedProblems = useMemo(
    () => form.problemIds.map(id => problems.find(problem => problem.id === id)).filter(Boolean) as ProblemCatalogItem[],
    [form.problemIds, problems]
  );

  const filteredProblems = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return problems.filter(problem => {
      const matchesDifficulty = difficulty === "ALL" || String(problem.difficulty).toUpperCase() === difficulty;
      const matchesQuery =
        !normalizedQuery ||
        problem.title.toLowerCase().includes(normalizedQuery) ||
        displayText(problem.summary, "").toLowerCase().includes(normalizedQuery);
      return matchesDifficulty && matchesQuery;
    });
  }, [difficulty, problems, query]);

  async function loadCreateContext() {
    setLoading(true);
    setAlert(null);
    try {
      const [classResult, problemResult] = await Promise.all([ensureDefaultClass(), api.problemCatalog()]);
      setClasses(classResult);
      setProblems(problemResult);
      setForm(current => ({
        ...current,
        classGroupId: current.classGroupId || (classResult[0] ? String(classResult[0].id) : "")
      }));
    } catch (error) {
      setAlert({ type: "error", message: teacherErrorMessage(error, "新建作业资源读取失败。") });
    } finally {
      setLoading(false);
    }
  }

  async function ensureDefaultClass() {
    const classResult = await api.classes();
    if (classResult.length) {
      return classResult;
    }
    const created = await api.createClass({ name: DEFAULT_CLASS_NAME });
    return [created];
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
    const classGroupId = form.classGroupId || (classes[0] ? String(classes[0].id) : "");
    if (!classGroupId) {
      setAlert({ type: "error", message: "默认班级还没准备好，请刷新后重试。" });
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
        classGroupId: Number(classGroupId),
        hintPolicy: form.hintPolicy,
        status: form.status,
        problemIds: form.problemIds
      };
      const saved = await api.createAssignment(payload);
      setAlert({ type: "success", message: `${cleanAssignmentTitle(saved.title)} 已保存。` });
      navigate(`/app/teacher/classes/${classGroupId}/assignments/${saved.id}`);
    } catch (error) {
      setAlert({ type: "error", message: teacherErrorMessage(error, "作业保存失败。") });
    } finally {
      setSaving(false);
    }
  }

  const selectedClass = classes.find(item => String(item.id) === form.classGroupId) || classes[0];
  const publishIssues = [
    !form.title.trim() ? "填写作业名称" : null,
    !selectedProblems.length ? "至少选择 1 道题" : null,
    !selectedClass ? "默认班级未就绪" : null
  ].filter(Boolean);
  const canPublish = publishIssues.length === 0 && !saving;

  function resetForm() {
    setForm({ ...EMPTY_ASSIGNMENT, classGroupId: classes[0] ? String(classes[0].id) : "" });
    setQuery("");
    setDifficulty("ALL");
  }

  return (
    <div className="teacher-page teacher-workflow assignment-builder-page">
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="teacher-workflow-header">
        <div>
          <ButtonLink to="/app/teacher/classes" variant="ghost" icon={<ArrowLeft size={17} />}>
            返回作业中心
          </ButtonLink>
          <p className="eyebrow">新建作业</p>
          <h1>布置课堂练习</h1>
        </div>
        <Button
          type="button"
          variant="primary"
          onClick={() => void saveAssignment()}
          disabled={!canPublish}
          title={publishIssues.join("、") || undefined}
          icon={<CheckCircle2 size={17} />}
        >
          {saving ? "保存中" : "发布作业"}
        </Button>
      </section>

      {loading ? (
        <EmptyState title="正在准备新建作业" live />
      ) : (
        <div className="assignment-builder assignment-builder--minimal">
          <section className="assignment-builder-section" aria-label="基本信息">
            <div className="assignment-builder-section__head">
              <div>
                <p className="eyebrow">基本信息</p>
                <h2>作业</h2>
              </div>
            </div>
            <div className="form-grid">
              <Field label="作业名称">
                <TextInput
                  value={form.title}
                  onChange={event => setForm({ ...form, title: event.target.value })}
                  placeholder="例如：循环边界练习"
                />
              </Field>
              <Field label="发布状态">
                <Select value={form.status} onChange={event => setForm({ ...form, status: event.target.value })}>
                  <option value="ACTIVE">进行中</option>
                  <option value="DRAFT">草稿</option>
                  <option value="CLOSED">已结束</option>
                </Select>
              </Field>
            </div>
            <p className="assignment-default-class">
              默认发布到 <strong>{displayText(selectedClass?.name, DEFAULT_CLASS_NAME)}</strong>
            </p>
            <Field label="作业说明">
              <TextArea
                value={form.description}
                onChange={event => setForm({ ...form, description: event.target.value })}
                placeholder="写给学生看的简短说明。"
              />
            </Field>
          </section>

          <section className="assignment-builder-section" aria-label="选择题目">
            <div className="assignment-builder-section__head">
              <div>
                <p className="eyebrow">选择题目</p>
                <h2>题库</h2>
              </div>
            </div>
            <div className="assignment-builder-picker">
              <div className="assignment-problem-bank">
                <div className="assignment-problem-bank__tools">
                  <label className="assignment-search">
                    <Search size={16} />
                    <input value={query} onChange={event => setQuery(event.target.value)} placeholder="搜索题目" />
                  </label>
                  <Select value={difficulty} onChange={event => setDifficulty(event.target.value as DifficultyFilter)} aria-label="按难度筛选">
                    {DIFFICULTY_FILTERS.map(filter => (
                      <option value={filter.value} key={filter.value}>
                        {filter.label}
                      </option>
                    ))}
                  </Select>
                </div>
                <div className="assignment-problem-list">
                  {filteredProblems.length ? (
                    filteredProblems.map(problem => {
                      const checked = form.problemIds.includes(problem.id);
                      return (
                        <label className={`assignment-problem-row ${checked ? "is-selected" : ""}`} key={problem.id}>
                          <input type="checkbox" checked={checked} onChange={() => toggleProblem(problem.id)} />
                          <span className="assignment-problem-row__info">
                            <span>
                              <strong>{problem.title}</strong>
                              <small>{displayText(problem.summary, "暂无题目说明")}</small>
                            </span>
                            <DifficultyPill difficulty={problem.difficulty} />
                          </span>
                        </label>
                      );
                    })
                  ) : (
                    <EmptyState title="没有匹配的题目" description="换一个关键词或难度筛选。" />
                  )}
                </div>
              </div>

              <aside className="assignment-selected-panel" aria-label="已选题目">
                <div>
                  <p className="eyebrow">已选题目</p>
                  <strong>{selectedProblems.length} 题</strong>
                </div>
                {selectedProblems.length ? (
                  <div className="assignment-selected-list">
                    {selectedProblems.map(problem => (
                      <button type="button" key={problem.id} onClick={() => toggleProblem(problem.id)}>
                        <span>
                          <strong>{problem.title}</strong>
                          <small>{difficultyLabel(problem.difficulty)}</small>
                        </span>
                        <X size={15} />
                      </button>
                    ))}
                  </div>
                ) : (
                  <p>从左侧题库勾选题目，老师发布前可以在这里复查。</p>
                )}
              </aside>
            </div>
          </section>

          <section className="assignment-builder-section" aria-label="确认发布">
            <div className="assignment-builder-section__head">
              <div>
                <p className="eyebrow">确认发布</p>
                <h2>发布</h2>
              </div>
            </div>
            <div className="assignment-publish-review">
              <div>
                <span>作业名称</span>
                <strong>{form.title.trim() || "未填写"}</strong>
              </div>
              <div>
                <span>班级</span>
                <strong>{displayText(selectedClass?.name, "未选择")}</strong>
              </div>
              <div>
                <span>题目</span>
                <strong>{selectedProblems.length} 题</strong>
              </div>
              <div>
                <span>状态</span>
                <StatusPill tone={form.status === "ACTIVE" ? "success" : form.status === "DRAFT" ? "neutral" : "info"}>
                  {form.status === "ACTIVE" ? "进行中" : form.status === "DRAFT" ? "草稿" : "已结束"}
                </StatusPill>
              </div>
            </div>
            <p className={`assignment-publish-hint ${publishIssues.length ? "is-warning" : "is-ready"}`}>
              {publishIssues.length ? `还差：${publishIssues.join("、")}` : "信息完整，可以发布给学生。"}
            </p>
            <div className="actions assignment-builder-actions">
              <Button
                type="button"
                variant="primary"
                onClick={() => void saveAssignment()}
                disabled={!canPublish}
                title={publishIssues.join("、") || undefined}
                icon={<CheckCircle2 size={17} />}
              >
                {saving ? "保存中" : "发布作业"}
              </Button>
              <Button type="button" variant="ghost" onClick={resetForm} disabled={saving}>
                清空
              </Button>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

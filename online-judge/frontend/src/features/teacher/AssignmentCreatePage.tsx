import { useEffect, useState } from "react";
import { ArrowLeft, CheckCircle2 } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { ApiError, api } from "../../shared/api/client";
import type { ClassGroup, ProblemCatalogItem } from "../../shared/api/types";
import { displayText } from "../../shared/format";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea, TextInput } from "../../shared/ui/Field";
import { DifficultyPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };
type AssignmentForm = {
  title: string;
  description: string;
  classGroupId: string;
  hintPolicy: string;
  status: string;
  problemIds: number[];
};

const EMPTY_ASSIGNMENT: AssignmentForm = {
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
  const [alert, setAlert] = useState<Alert | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    void loadCreateContext();
  }, []);

  async function loadCreateContext() {
    setLoading(true);
    setAlert(null);
    try {
      const [classResult, problemResult] = await Promise.all([api.classes(), api.problemCatalog()]);
      setClasses(classResult);
      setProblems(problemResult);
    } catch (error) {
      setAlert({ type: "error", message: teacherErrorMessage(error, "新建作业资源读取失败。") });
    } finally {
      setLoading(false);
    }
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
      const saved = await api.createAssignment(payload);
      setAlert({ type: "success", message: `${cleanAssignmentTitle(saved.title)} 已保存。` });
      navigate(`/app/teacher/assignment/${saved.id}`);
    } catch (error) {
      setAlert({ type: "error", message: teacherErrorMessage(error, "作业保存失败。") });
    } finally {
      setSaving(false);
    }
  }

  const selectedProblemCount = form.problemIds.length;

  return (
    <div className="teacher-page teacher-assignment-center assignment-create-page">
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="teacher-home-command assignment-create-command">
        <div>
          <p className="eyebrow">教师端</p>
          <h1>新建作业</h1>
        </div>
        <div className="teacher-home-actions">
          <ButtonLink to="/app/teacher" variant="ghost" icon={<ArrowLeft size={17} />}>
            返回作业中心
          </ButtonLink>
        </div>
      </section>

      {loading ? (
        <EmptyState title="正在准备新建作业" />
      ) : (
        <section className="assignment-section assignment-create-form" aria-label="新建作业表单">
          <div className="assignment-section__head">
            <div>
              <p className="eyebrow">作业信息</p>
              <h2>设置课堂作业</h2>
            </div>
            <span className="assignment-create-count">{selectedProblemCount} 题已选</span>
          </div>

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
            <div className="teacher-problem-pick-list teacher-problem-pick-list--simple assignment-create-problem-list">
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

          <div className="actions assignment-create-actions">
            <Button type="button" variant="primary" onClick={() => void saveAssignment()} disabled={saving} icon={<CheckCircle2 size={17} />}>
              {saving ? "保存中" : "发布作业"}
            </Button>
            <Button type="button" variant="ghost" onClick={() => setForm(EMPTY_ASSIGNMENT)} disabled={saving}>
              清空
            </Button>
          </div>
        </section>
      )}
    </div>
  );
}

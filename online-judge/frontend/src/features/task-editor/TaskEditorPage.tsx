import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Save } from "lucide-react";
import { api } from "../../shared/api/client";
import type { ProblemCatalogItem, ProblemManage } from "../../shared/api/types";
import { difficultyLabel } from "../../shared/format";
import { Button } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea, TextInput } from "../../shared/ui/Field";
import { Panel } from "../../shared/ui/Panel";
import { DifficultyPill, StatusPill } from "../../shared/ui/StatusPill";

type TestCaseDraft = {
  input: string;
  expectedOutput: string;
  hidden: boolean;
};

const initialProblem = {
  id: "",
  title: "",
  description: "",
  difficulty: "EASY",
  timeLimit: 1000,
  memoryLimit: 131072,
  aiPromptDirection: "",
  knowledgePointsText: "",
  algorithmStrategiesText: "",
  commonMistakesText: "",
  boundaryTypesText: "",
  testCases: [{ input: "", expectedOutput: "", hidden: false }] as TestCaseDraft[]
};

export default function TaskEditorPage() {
  const [searchParams] = useSearchParams();
  const [catalog, setCatalog] = useState<ProblemCatalogItem[]>([]);
  const [form, setForm] = useState(initialProblem);
  const [alert, setAlert] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [busy, setBusy] = useState(false);

  const visibleCount = useMemo(() => form.testCases.filter(item => !item.hidden).length, [form.testCases]);
  const hiddenCount = useMemo(() => form.testCases.filter(item => item.hidden).length, [form.testCases]);
  const qualityItems = useMemo(
    () => [
      { label: "题目标题", ready: Boolean(form.title.trim()), note: form.title.trim() ? "已填写" : "未填写" },
      { label: "题面", ready: form.description.trim().length >= 20, note: form.description.trim().length >= 20 ? "已填写" : "未完成" },
      { label: "公开测试点", ready: visibleCount > 0, note: visibleCount > 0 ? `${visibleCount} 个可见` : "至少保留 1 个样例" },
      { label: "隐藏测试点", ready: hiddenCount > 0, note: hiddenCount > 0 ? `${hiddenCount} 个隐藏` : "未设置" },
      {
        label: "知识点",
        ready: Boolean(form.knowledgePointsText.trim() || form.commonMistakesText.trim() || form.boundaryTypesText.trim()),
        note: form.knowledgePointsText.trim() ? "已填写" : "未填写"
      }
    ],
    [form.boundaryTypesText, form.commonMistakesText, form.description, form.knowledgePointsText, form.title, hiddenCount, visibleCount]
  );
  const readyQualityCount = qualityItems.filter(item => item.ready).length;
  const publishState = readyQualityCount >= qualityItems.length ? "检查完成" : readyQualityCount >= 4 ? "基本完整" : "未完成";
  const readinessTone = readyQualityCount >= qualityItems.length ? "success" : readyQualityCount >= 4 ? "info" : "warning";

  useEffect(() => {
    void loadCatalog();
    const id = searchParams.get("id");
    if (id) {
      void loadProblem(Number(id));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadCatalog() {
    try {
      setCatalog(await api.problemCatalog());
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "题目列表加载失败。" });
    }
  }

  async function loadProblem(id: number) {
    try {
      const problem = await api.problemManage(id);
      populate(problem);
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "题目加载失败。" });
    }
  }

  function populate(problem: ProblemManage) {
    setForm({
      id: String(problem.id),
      title: problem.title || "",
      description: problem.description || "",
      difficulty: String(problem.difficulty || "EASY"),
      timeLimit: problem.timeLimit || 1000,
      memoryLimit: problem.memoryLimit || 131072,
      aiPromptDirection: problem.aiPromptDirection || "",
      knowledgePointsText: joinList(problem.knowledgePoints),
      algorithmStrategiesText: joinList(problem.algorithmStrategies),
      commonMistakesText: joinList(problem.commonMistakes),
      boundaryTypesText: joinList(problem.boundaryTypes),
      testCases: problem.testCases?.length
        ? problem.testCases.map(item => ({
            input: item.input || "",
            expectedOutput: item.expectedOutput || "",
            hidden: Boolean(item.hidden)
          }))
        : [{ input: "", expectedOutput: "", hidden: false }]
    });
  }

  function updateTestCase(index: number, patch: Partial<TestCaseDraft>) {
    setForm(current => ({
      ...current,
      testCases: current.testCases.map((item, itemIndex) => (itemIndex === index ? { ...item, ...patch } : item))
    }));
  }

  function addTestCase(hidden = false) {
    setForm(current => ({
      ...current,
      testCases: [...current.testCases, { input: "", expectedOutput: "", hidden }]
    }));
  }

  function removeTestCase(index: number) {
    setForm(current => ({
      ...current,
      testCases: current.testCases.length > 1 ? current.testCases.filter((_, itemIndex) => itemIndex !== index) : current.testCases
    }));
  }

  async function save() {
    if (!form.title.trim() || !form.description.trim()) {
      setAlert({ type: "error", message: "请填写标题和题面。" });
      return;
    }
    if (visibleCount < 1) {
      setAlert({ type: "error", message: "至少需要 1 个公开测试点。" });
      return;
    }
    setBusy(true);
    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      difficulty: form.difficulty,
      timeLimit: Number(form.timeLimit),
      memoryLimit: Number(form.memoryLimit),
      aiPromptDirection: form.aiPromptDirection.trim(),
      knowledgePoints: splitList(form.knowledgePointsText),
      algorithmStrategies: splitList(form.algorithmStrategiesText),
      commonMistakes: splitList(form.commonMistakesText),
      boundaryTypes: splitList(form.boundaryTypesText),
      testCases: form.testCases.map(item => ({
        input: item.input,
        expectedOutput: item.expectedOutput,
        hidden: item.hidden
      }))
    };
    try {
      const result = form.id ? await api.updateProblem(Number(form.id), payload) : await api.createProblem(payload);
      setAlert({ type: "success", message: "题目已保存，可在教师工作台绑定到作业。" });
      setForm(current => ({ ...current, id: String(result.id) }));
      await loadCatalog();
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "保存失败。" });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="stack task-editor-page">
      <section className="editor-command">
        <div>
          <h1>{form.title || "新建题目"}</h1>
        </div>
        <div className="editor-command__actions">
          <StatusPill tone={visibleCount ? "success" : "danger"}>{visibleCount} 个公开测试点</StatusPill>
          <StatusPill tone={readyQualityCount >= 4 ? "success" : "warning"}>{readyQualityCount}/5 检查完成</StatusPill>
          <StatusPill tone="info">{catalog.length} 个题目</StatusPill>
          <Button type="button" variant="primary" onClick={() => void save()} disabled={busy} icon={<Save size={18} />}>
            保存题目
          </Button>
        </div>
      </section>

      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="editor-layout">
        <Panel title="题目信息" action={<StatusPill tone={readinessTone}>{publishState}</StatusPill>}>
          <div className="stack">
            <div className="form-grid">
              <Field label="题目标题">
                <TextInput value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} placeholder="两数求和" />
              </Field>
              <Field label="难度">
                <Select value={form.difficulty} onChange={event => setForm({ ...form, difficulty: event.target.value })}>
                  <option value="EASY">基础</option>
                  <option value="MEDIUM">提高</option>
                  <option value="HARD">挑战</option>
                </Select>
              </Field>
              <Field label="时限 ms">
                <TextInput type="number" value={form.timeLimit} onChange={event => setForm({ ...form, timeLimit: Number(event.target.value) })} />
              </Field>
            </div>
            <Field label="题面">
              <TextArea value={form.description} onChange={event => setForm({ ...form, description: event.target.value })} />
            </Field>
            <details className="editor-compact-details">
              <summary>
                <span>题目设置</span>
                <StatusPill tone={qualityItems[4].ready ? "success" : "neutral"}>{qualityItems[4].ready ? "已填写" : "选填"}</StatusPill>
              </summary>
              <div className="editor-compact-details__body">
                <div className="form-grid">
                  <Field label="内存 KB">
                    <TextInput type="number" value={form.memoryLimit} onChange={event => setForm({ ...form, memoryLimit: Number(event.target.value) })} />
                  </Field>
                  <Field label="反馈范围">
                    <TextInput
                      value={form.aiPromptDirection}
                      onChange={event => setForm({ ...form, aiPromptDirection: event.target.value })}
                      placeholder="空输入、循环边界、复杂度"
                    />
                  </Field>
                </div>
                <div className="knowledge-grid">
                  <Field label="知识点">
                    <TextArea
                      value={form.knowledgePointsText}
                      onChange={event => setForm({ ...form, knowledgePointsText: event.target.value })}
                      rows={3}
                    />
                  </Field>
                  <Field label="算法策略">
                    <TextArea
                      value={form.algorithmStrategiesText}
                      onChange={event => setForm({ ...form, algorithmStrategiesText: event.target.value })}
                      rows={3}
                    />
                  </Field>
                  <Field label="常见误区">
                    <TextArea
                      value={form.commonMistakesText}
                      onChange={event => setForm({ ...form, commonMistakesText: event.target.value })}
                      rows={3}
                    />
                  </Field>
                  <Field label="边界类型">
                    <TextArea
                      value={form.boundaryTypesText}
                      onChange={event => setForm({ ...form, boundaryTypesText: event.target.value })}
                      rows={3}
                    />
                  </Field>
                </div>
              </div>
            </details>
          </div>
        </Panel>

        <aside className="editor-side">
          <Panel
            title="题目列表"
            action={<StatusPill tone="info">{catalog.length} 个题目</StatusPill>}
          >
            <div className="stack">
              {catalog.length ? (
                catalog.map(item => (
                  <button type="button" className="list-row" key={item.id} onClick={() => void loadProblem(item.id)} style={{ textAlign: "left" }}>
                    <DifficultyPill difficulty={item.difficulty} />
                    <h3>{item.title}</h3>
                    <p>{item.summary || `${difficultyLabel(item.difficulty)} · ${item.timeLimit} ms · ${Math.round(item.memoryLimit / 1024)} MB`}</p>
                  </button>
                ))
              ) : (
                <EmptyState title="暂无题目" />
              )}
            </div>
          </Panel>

          <Panel title="保存检查" action={<StatusPill tone={readinessTone}>{readyQualityCount}/5</StatusPill>}>
            <div className="editor-quality-list">
              {qualityItems.map(item => (
                <div className={item.ready ? "is-ready" : ""} key={item.label}>
                  <span>{item.ready ? "✓" : "!"}</span>
                  <div>
                    <strong>{item.label}</strong>
                    <small>{item.note}</small>
                  </div>
                </div>
              ))}
            </div>
          </Panel>
        </aside>
      </section>

      <Panel
        title="测试点"
        action={
          <div className="actions">
            <StatusPill tone={visibleCount ? "success" : "danger"}>{visibleCount} 个公开</StatusPill>
            <StatusPill tone={hiddenCount ? "warning" : "neutral"}>{hiddenCount} 个隐藏</StatusPill>
            <Button type="button" variant="secondary" onClick={() => addTestCase(false)}>
              添加公开点
            </Button>
            <Button type="button" variant="ghost" onClick={() => addTestCase(true)}>
              添加隐藏点
            </Button>
          </div>
        }
      >
        <div className="stack">
          {form.testCases.map((item, index) => (
            <div className="list-row" key={index}>
              <div className="actions">
                <StatusPill tone={item.hidden ? "warning" : "success"}>{item.hidden ? "隐藏测试点" : "公开测试点"}</StatusPill>
                <label className="actions">
                  <input type="checkbox" checked={item.hidden} onChange={event => updateTestCase(index, { hidden: event.target.checked })} />
                  隐藏
                </label>
                <Button type="button" variant="danger" onClick={() => removeTestCase(index)}>
                  删除
                </Button>
              </div>
              <div className="two-column">
                <Field label="输入">
                  <TextArea value={item.input} onChange={event => updateTestCase(index, { input: event.target.value })} />
                </Field>
                <Field label="期望输出">
                  <TextArea value={item.expectedOutput} onChange={event => updateTestCase(index, { expectedOutput: event.target.value })} />
                </Field>
              </div>
            </div>
          ))}
        </div>
      </Panel>

    </div>
  );
}

function splitList(value: string): string[] {
  return value
    .split(/[\n,，、;；]/)
    .map(item => item.trim())
    .filter(Boolean)
    .filter((item, index, all) => all.indexOf(item) === index)
    .slice(0, 12);
}

function joinList(value?: string[] | null): string {
  return value?.length ? value.join("\n") : "";
}

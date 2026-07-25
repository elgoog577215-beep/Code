import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { useSearchParams } from "react-router-dom";
import { Download, Eye, FileText, Save, UploadCloud } from "lucide-react";
import { api } from "../../shared/api/client";
import type { Problem, ProblemAttachment, ProblemCatalogItem, ProblemManage, TestDataImportPreview } from "../../shared/api/types";
import { difficultyLabel } from "../../shared/format";
import { Button } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea, TextInput } from "../../shared/ui/Field";
import { Panel } from "../../shared/ui/Panel";
import { DifficultyPill, StatusPill } from "../../shared/ui/StatusPill";

type TabKey = "statement" | "settings" | "data" | "help";

type TestCaseDraft = {
  id?: number;
  input: string;
  expectedOutput: string;
  hidden: boolean;
  inputStorageType?: string;
  outputStorageType?: string;
  inputFilePath?: string | null;
  outputFilePath?: string | null;
  inputFileName?: string | null;
  outputFileName?: string | null;
  inputSizeBytes?: number | null;
  outputSizeBytes?: number | null;
  inputSha256?: string | null;
  outputSha256?: string | null;
  timeLimitMs?: number | null;
  memoryLimitKib?: number | null;
  subtaskIndex?: number | null;
  score?: number | null;
  publicExample?: boolean | null;
  importBatchId?: string | null;
};

type ProblemDraft = {
  id: string;
  title: string;
  status: string;
  statementBackground: string;
  statementDescription: string;
  statementInputFormat: string;
  statementOutputFormat: string;
  statementSamples: string;
  statementHints: string;
  provider: string;
  attachments: string;
  difficulty: string;
  tagsText: string;
  dataDownloadEnabled: boolean;
  scoreDisplayMode: string;
  timeLimit: number;
  memoryLimit: number;
  aiPromptDirection: string;
  starterCode: string;
  knowledgePointsText: string;
  algorithmStrategiesText: string;
  commonMistakesText: string;
  boundaryTypesText: string;
  testCases: TestCaseDraft[];
};

const emptyCase: TestCaseDraft = {
  input: "",
  expectedOutput: "",
  hidden: false,
  inputStorageType: "INLINE",
  outputStorageType: "INLINE",
  timeLimitMs: 1000,
  memoryLimitKib: 131072,
  subtaskIndex: 0,
  score: 0,
  publicExample: true
};

function createInitialProblem(): ProblemDraft {
  return {
    id: "",
    title: "",
    status: "HIDDEN",
    statementBackground: "",
    statementDescription: "",
    statementInputFormat: "",
    statementOutputFormat: "",
    statementSamples: "",
    statementHints: "",
    provider: "",
    attachments: "",
    difficulty: "EASY",
    tagsText: "",
    dataDownloadEnabled: false,
    scoreDisplayMode: "ICPC",
    timeLimit: 1000,
    memoryLimit: 131072,
    aiPromptDirection: "",
    starterCode: "",
    knowledgePointsText: "",
    algorithmStrategiesText: "",
    commonMistakesText: "",
    boundaryTypesText: "",
    testCases: [{ ...emptyCase }]
  };
}

function renderInlineMarkdown(value: string, keyPrefix: string): ReactNode[] {
  const nodes: ReactNode[] = [];
  const pattern = /`([^`]+)`|\*\*([^*\n]+)\*\*|\*([^*\n]+)\*/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  let tokenIndex = 0;
  while ((match = pattern.exec(value))) {
    if (match.index > lastIndex) nodes.push(value.slice(lastIndex, match.index));
    const key = `${keyPrefix}-${tokenIndex}`;
    if (match[1]) nodes.push(<code key={key}>{match[1]}</code>);
    if (match[2]) nodes.push(<strong key={key}>{match[2]}</strong>);
    if (match[3]) nodes.push(<em key={key}>{match[3]}</em>);
    tokenIndex += 1;
    lastIndex = pattern.lastIndex;
  }
  if (lastIndex < value.length) nodes.push(value.slice(lastIndex));
  return nodes.length ? nodes : [value];
}

function renderMarkdownPreview(value: string, emptyText: string): ReactNode[] {
  if (!value.trim()) {
    return [<p className="editor-markdown-preview__empty" key="empty">{emptyText}</p>];
  }
  const nodes: ReactNode[] = [];
  const codeLines: string[] = [];
  let inCode = false;
  function flushCode(key: string) {
    if (codeLines.length) {
      nodes.push(<pre key={key}><code>{codeLines.join("\n")}</code></pre>);
      codeLines.length = 0;
    }
  }
  value.split("\n").forEach((line, index) => {
    const trimmed = line.trim();
    if (trimmed.startsWith("```")) {
      if (inCode) flushCode(`code-${index}`);
      inCode = !inCode;
      return;
    }
    if (inCode) {
      codeLines.push(line);
      return;
    }
    if (!trimmed) {
      nodes.push(<span className="editor-markdown-preview__space" key={`space-${index}`} />);
      return;
    }
    if (trimmed.startsWith("### ")) {
      nodes.push(<h4 key={`h4-${index}`}>{renderInlineMarkdown(trimmed.slice(4), `h4-${index}`)}</h4>);
      return;
    }
    if (trimmed.startsWith("## ")) {
      nodes.push(<h3 key={`h3-${index}`}>{renderInlineMarkdown(trimmed.slice(3), `h3-${index}`)}</h3>);
      return;
    }
    if (trimmed.startsWith("# ")) {
      nodes.push(<h2 key={`h2-${index}`}>{renderInlineMarkdown(trimmed.slice(2), `h2-${index}`)}</h2>);
      return;
    }
    nodes.push(<p key={`p-${index}`}>{renderInlineMarkdown(line, `p-${index}`)}</p>);
  });
  flushCode("code-tail");
  return nodes;
}

type TaskEditorPageProps = {
  embedded?: boolean;
  selectedProblemId?: number | null;
  showCatalogDrawer?: boolean;
  createDraftSignal?: number;
  onSaved?: (problem: Problem) => void;
};

export default function TaskEditorPage({
  embedded = false,
  selectedProblemId,
  showCatalogDrawer = true,
  createDraftSignal = 0,
  onSaved
}: TaskEditorPageProps) {
  const [searchParams] = useSearchParams();
  const [catalog, setCatalog] = useState<ProblemCatalogItem[]>([]);
  const [form, setForm] = useState<ProblemDraft>(createInitialProblem);
  const [activeTab, setActiveTab] = useState<TabKey>("statement");
  const [alert, setAlert] = useState<{ type: "success" | "error"; message: string } | null>(null);
  const [busy, setBusy] = useState(false);
  const [statementPreview, setStatementPreview] = useState<Record<string, boolean>>({});
  const [zipFile, setZipFile] = useState<File | null>(null);
  const [zipPreview, setZipPreview] = useState<TestDataImportPreview | null>(null);
  const [attachments, setAttachments] = useState<ProblemAttachment[]>([]);
  const [filePreview, setFilePreview] = useState<{ title: string; lines: string[]; truncated: boolean } | null>(null);
  const mdFileRef = useRef<HTMLInputElement | null>(null);
  const zipFileRef = useRef<HTMLInputElement | null>(null);
  const attachmentFileRef = useRef<HTMLInputElement | null>(null);
  const loadProblemRequestRef = useRef(0);

  const visibleCount = useMemo(() => form.testCases.filter(item => !item.hidden).length, [form.testCases]);
  const fileCaseCount = useMemo(() => form.testCases.filter(item => item.inputStorageType === "FILE").length, [form.testCases]);

  useEffect(() => {
    if (showCatalogDrawer) void loadCatalog();
    const id = searchParams.get("id");
    const initialId = selectedProblemId || (id ? Number(id) : null);
    if (initialId) void loadProblem(initialId);
  }, []);

  useEffect(() => {
    if (selectedProblemId) void loadProblem(selectedProblemId);
  }, [selectedProblemId]);

  useEffect(() => {
    if (createDraftSignal > 0 && selectedProblemId == null) {
      loadProblemRequestRef.current += 1;
      setForm(createInitialProblem());
      setZipPreview(null);
      setZipFile(null);
      setFilePreview(null);
      setAttachments([]);
      setAlert(null);
      setActiveTab("statement");
    }
  }, [createDraftSignal, selectedProblemId]);

  async function loadCatalog() {
    try {
      setCatalog(await api.problemCatalog());
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "题库列表读取失败。" });
    }
  }

  async function loadProblem(id: number) {
    const requestId = loadProblemRequestRef.current + 1;
    loadProblemRequestRef.current = requestId;
    try {
      const problem = await api.problemManage(id);
      if (requestId !== loadProblemRequestRef.current) return;
      populate(problem);
      void loadAttachments(problem.id);
    } catch (error) {
      if (requestId !== loadProblemRequestRef.current) return;
      setAlert({ type: "error", message: error instanceof Error ? error.message : "题目读取失败。" });
    }
  }

  function populate(problem: ProblemManage) {
    const legacyStatement = problem.statementDescription || stripKnownSections(problem.description || "");
    setForm({
      id: String(problem.id),
      title: problem.title || "",
      status: problem.status || "HIDDEN",
      statementBackground: problem.statementBackground || "",
      statementDescription: legacyStatement,
      statementInputFormat: problem.statementInputFormat || "",
      statementOutputFormat: problem.statementOutputFormat || "",
      statementSamples: problem.statementSamples || "",
      statementHints: problem.statementHints || "",
      provider: problem.provider || "",
      attachments: problem.attachments || "",
      difficulty: String(problem.difficulty || "EASY"),
      tagsText: joinList(problem.tags),
      dataDownloadEnabled: Boolean(problem.dataDownloadEnabled),
      scoreDisplayMode: problem.scoreDisplayMode || "ICPC",
      timeLimit: problem.timeLimit || 1000,
      memoryLimit: problem.memoryLimit || 131072,
      aiPromptDirection: problem.aiPromptDirection || "",
      starterCode: problem.starterCode || "",
      knowledgePointsText: joinList(problem.knowledgePoints),
      algorithmStrategiesText: joinList(problem.algorithmStrategies),
      commonMistakesText: joinList(problem.commonMistakes),
      boundaryTypesText: joinList(problem.boundaryTypes),
      testCases: problem.testCases?.length
        ? problem.testCases.map(item => ({
            id: item.id,
            input: item.input || "",
            expectedOutput: item.expectedOutput || "",
            hidden: Boolean(item.hidden),
            inputStorageType: item.inputStorageType || "INLINE",
            outputStorageType: item.outputStorageType || "INLINE",
            inputFilePath: item.inputFilePath,
            outputFilePath: item.outputFilePath,
            inputFileName: item.inputFileName,
            outputFileName: item.outputFileName,
            inputSizeBytes: item.inputSizeBytes,
            outputSizeBytes: item.outputSizeBytes,
            inputSha256: item.inputSha256,
            outputSha256: item.outputSha256,
            timeLimitMs: item.timeLimitMs ?? problem.timeLimit ?? 1000,
            memoryLimitKib: item.memoryLimitKib ?? problem.memoryLimit ?? 131072,
            subtaskIndex: item.subtaskIndex ?? 0,
            score: item.score ?? 0,
            publicExample: item.publicExample ?? !item.hidden,
            importBatchId: item.importBatchId
          }))
        : [{ ...emptyCase }]
    });
    setZipPreview(null);
    setZipFile(null);
    setFilePreview(null);
  }

  async function loadAttachments(problemId: number) {
    try {
      const list = await api.problemAttachments(problemId);
      syncAttachments(list);
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "附件列表读取失败。" });
    }
  }

  function syncAttachments(list: ProblemAttachment[]) {
    setAttachments(list);
    setForm(current => ({ ...current, attachments: JSON.stringify(list) }));
  }

  function patchForm(patch: Partial<ProblemDraft>) {
    setForm(current => ({ ...current, ...patch }));
  }

  function updateTestCase(index: number, patch: Partial<TestCaseDraft>) {
    setForm(current => {
      const testCases = current.testCases.map((item, itemIndex) => {
        if (itemIndex !== index) return item;
        const next = { ...item, ...patch };
        if ("publicExample" in patch) {
          next.hidden = !patch.publicExample;
        }
        return next;
      });
      return { ...current, testCases, statementSamples: buildSamples(testCases) || current.statementSamples };
    });
  }

  function addInlineCase(hidden = false) {
    setForm(current => ({ ...current, testCases: [...current.testCases, { ...emptyCase, hidden, publicExample: !hidden }] }));
  }

  function removeCase(index: number) {
    setForm(current => ({
      ...current,
      testCases: current.testCases.length > 1 ? current.testCases.filter((_, itemIndex) => itemIndex !== index) : current.testCases
    }));
  }

  async function importMarkdownFile(file: File | null) {
    if (!file) return;
    try {
      const result = await api.importProblemStatement(await file.text());
      setForm(current => ({
        ...current,
        title: result.title || current.title,
        statementBackground: result.statementBackground ?? current.statementBackground,
        statementDescription: result.statementDescription ?? current.statementDescription,
        statementInputFormat: result.statementInputFormat ?? current.statementInputFormat,
        statementOutputFormat: result.statementOutputFormat ?? current.statementOutputFormat,
        statementSamples: result.statementSamples ?? current.statementSamples,
        statementHints: result.statementHints ?? current.statementHints
      }));
      setAlert({ type: "success", message: "Markdown 题面已导入。" });
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "Markdown 导入失败。" });
    }
  }

  async function previewZip(file = zipFile) {
    if (!form.id) {
      setAlert({ type: "error", message: "请先保存题目，再上传测试数据包。" });
      return;
    }
    if (!file) {
      setAlert({ type: "error", message: "请先选择 zip 文件。" });
      return;
    }
    setBusy(true);
    try {
      const result = await api.previewProblemTestData(Number(form.id), file);
      setZipPreview(result);
      setAlert({ type: result.valid ? "success" : "error", message: result.message || "数据包预览完成。" });
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "数据包预览失败。" });
    } finally {
      setBusy(false);
    }
  }

  async function commitZip() {
    if (!form.id || !zipFile) return;
    setBusy(true);
    try {
      await api.commitProblemTestData(Number(form.id), zipFile);
      await loadProblem(Number(form.id));
      setActiveTab("data");
      setAlert({ type: "success", message: "测试数据包已导入。" });
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "测试数据导入失败。" });
    } finally {
      setBusy(false);
    }
  }

  async function uploadAttachment(file: File | null) {
    if (!file) return;
    if (!form.id) {
      setAlert({ type: "error", message: "请先保存题目，再上传附件。" });
      return;
    }
    setBusy(true);
    try {
      await api.uploadProblemAttachment(Number(form.id), file);
      const list = await api.problemAttachments(Number(form.id));
      syncAttachments(list);
      setAlert({ type: "success", message: "附件已上传。" });
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "附件上传失败。" });
    } finally {
      setBusy(false);
      if (attachmentFileRef.current) attachmentFileRef.current.value = "";
    }
  }

  async function previewStoredCase(item: TestCaseDraft, kind: "input" | "output") {
    if (!form.id || !item.id) return;
    if (item.inputStorageType !== "FILE") {
      const text = kind === "input" ? item.input : item.expectedOutput;
      setFilePreview({
        title: kind === "input" ? "输入预览" : "输出预览",
        lines: text.split(/\r?\n/).slice(0, 10),
        truncated: text.split(/\r?\n/).length > 10
      });
      return;
    }
    try {
      const result = await api.previewProblemTestDataFile(Number(form.id), item.id, kind);
      setFilePreview({ title: `${result.fileName || ""} ${kind === "input" ? "输入" : "输出"}`, lines: result.lines, truncated: result.truncated });
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "文件预览失败。" });
    }
  }

  function oneClickFill() {
    setForm(current => ({
      ...current,
      testCases: current.testCases.map((item, index, all) => ({
        ...item,
        timeLimitMs: item.timeLimitMs || 1000,
        memoryLimitKib: item.memoryLimitKib || 131072,
        subtaskIndex: item.subtaskIndex ?? 0,
        score: current.scoreDisplayMode === "OI" ? Math.floor(100 / all.length) + (index === all.length - 1 ? 100 % all.length : 0) : 0
      }))
    }));
  }

  async function save() {
    if (!form.title.trim() || !form.statementDescription.trim() || !form.statementInputFormat.trim() || !form.statementOutputFormat.trim()) {
      setAlert({ type: "error", message: "题目名称、题目描述、输入格式、输出格式不能为空。" });
      setActiveTab("statement");
      return;
    }
    if (visibleCount < 1) {
      setAlert({ type: "error", message: "至少需要一个公开样例或测试点。" });
      setActiveTab("data");
      return;
    }
    setBusy(true);
    const payload = {
      title: form.title.trim(),
      description: composeDescription(form),
      status: form.status,
      statementBackground: form.statementBackground,
      statementDescription: form.statementDescription,
      statementInputFormat: form.statementInputFormat,
      statementOutputFormat: form.statementOutputFormat,
      statementSamples: buildSamples(form.testCases) || form.statementSamples,
      statementHints: form.statementHints,
      provider: form.provider,
      attachments: form.attachments,
      difficulty: form.difficulty,
      tags: splitList(form.tagsText),
      dataDownloadEnabled: form.dataDownloadEnabled,
      scoreDisplayMode: form.scoreDisplayMode,
      timeLimit: Number(form.timeLimit),
      memoryLimit: Number(form.memoryLimit),
      aiPromptDirection: form.aiPromptDirection,
      starterCode: form.starterCode,
      knowledgePoints: splitList(form.knowledgePointsText),
      algorithmStrategies: splitList(form.algorithmStrategiesText),
      commonMistakes: splitList(form.commonMistakesText),
      boundaryTypes: splitList(form.boundaryTypesText),
      testCases: form.testCases.map(item => ({
        id: item.id,
        input: item.input,
        expectedOutput: item.expectedOutput,
        hidden: item.hidden,
        inputStorageType: item.inputStorageType || "INLINE",
        outputStorageType: item.outputStorageType || "INLINE",
        inputFilePath: item.inputFilePath,
        outputFilePath: item.outputFilePath,
        inputFileName: item.inputFileName,
        outputFileName: item.outputFileName,
        inputSizeBytes: item.inputSizeBytes,
        outputSizeBytes: item.outputSizeBytes,
        inputSha256: item.inputSha256,
        outputSha256: item.outputSha256,
        timeLimitMs: Number(item.timeLimitMs || form.timeLimit),
        memoryLimitKib: Number(item.memoryLimitKib || form.memoryLimit),
        subtaskIndex: Number(item.subtaskIndex || 0),
        score: Number(item.score || 0),
        publicExample: Boolean(item.publicExample),
        importBatchId: item.importBatchId
      }))
    };
    try {
      const result = form.id ? await api.updateProblem(Number(form.id), payload) : await api.createProblem(payload);
      setForm(current => ({ ...current, id: String(result.id) }));
      setAlert({ type: "success", message: "题目已保存。" });
      if (showCatalogDrawer) await loadCatalog();
      onSaved?.(result);
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "题目保存失败。" });
    } finally {
      setBusy(false);
    }
  }

  const tabs: Array<{ key: TabKey; label: string }> = [
    { key: "statement", label: "题面设置" },
    { key: "settings", label: "题目设置" },
    { key: "data", label: "数据设置" },
    { key: "help", label: "功能说明" }
  ];

  return (
    <div className={`stack task-editor-page ${embedded ? "task-editor-page--embedded" : ""}`}>
      <section className="editor-command editor-command--embedded">
        <div>
          <p className="eyebrow">Problem Workbench</p>
          <h1>{form.title || "新建题目"}</h1>
        </div>
        <div className="editor-command__actions">
          <StatusPill tone={visibleCount ? "success" : "warning"}>公开 {visibleCount}</StatusPill>
          <StatusPill tone={fileCaseCount ? "info" : "neutral"}>文件数据 {fileCaseCount}</StatusPill>
          <Button type="button" variant="primary" onClick={() => void save()} disabled={busy} icon={<Save size={18} />}>
            保存题目
          </Button>
        </div>
      </section>

      {alert ? <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div> : null}

      <div className="problem-workbench-tabs" role="tablist" aria-label="题目编辑书签">
        {tabs.map(tab => (
          <button type="button" role="tab" aria-selected={activeTab === tab.key} className={activeTab === tab.key ? "is-active" : ""} key={tab.key} onClick={() => setActiveTab(tab.key)}>
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === "statement" ? renderStatementTab() : null}
      {activeTab === "settings" ? renderSettingsTab() : null}
      {activeTab === "data" ? renderDataTab() : null}
      {activeTab === "help" ? renderHelpTab() : null}

      {showCatalogDrawer ? renderCatalog() : null}
    </div>
  );

  function renderStatementTab() {
    return (
      <Panel title="题面设置" action={<StatusPill tone="info">{statusLabel(form.status)}</StatusPill>}>
        <div className="stack">
          <div className="form-grid">
            <Field label="题目状态">
              <Select value={form.status} onChange={event => patchForm({ status: event.target.value })}>
                <option value="HIDDEN">隐藏</option>
                <option value="PUBLIC">公开</option>
                <option value="PARTIAL">部分可见</option>
                <option value="CONTEST">比赛赛题</option>
              </Select>
            </Field>
            <Field label="题目名称">
              <TextInput value={form.title} onChange={event => patchForm({ title: event.target.value })} />
            </Field>
            <Field label="导入 .md 题面">
              <div className="actions">
                <input ref={mdFileRef} type="file" accept=".md,.markdown,.txt" hidden onChange={event => void importMarkdownFile(event.target.files?.[0] || null)} />
                <Button type="button" variant="secondary" onClick={() => mdFileRef.current?.click()} icon={<FileText size={16} />}>选择 Markdown</Button>
              </div>
            </Field>
          </div>
          {markdownField("题目背景", "statementBackground", false)}
          {markdownField("题目描述", "statementDescription", true)}
          {markdownField("输入格式", "statementInputFormat", true)}
          {markdownField("输出格式", "statementOutputFormat", true)}
          {markdownField("样例", "statementSamples", false, buildSamples(form.testCases) || form.statementSamples)}
          {markdownField("提示说明", "statementHints", false)}
        </div>
      </Panel>
    );
  }

  function markdownField(label: string, key: keyof ProblemDraft, required: boolean, overrideValue?: string) {
    const value = String(overrideValue ?? form[key] ?? "");
    const previewing = Boolean(statementPreview[String(key)]);
    return (
      <Field label={`${label}${required ? " *" : ""}`}>
        <div className="editor-markdown-shell">
          <div className="editor-inline-toolbar">
            <button type="button" className={`editor-tool-button editor-tool-button--text ${previewing ? "is-active" : ""}`} onClick={() => setStatementPreview(current => ({ ...current, [String(key)]: !previewing }))}>
              <Eye size={15} />
              <span>{previewing ? "编辑" : "预览"}</span>
            </button>
          </div>
          {previewing ? (
            <div className="editor-markdown-preview">{renderMarkdownPreview(value, "空白内容不会在题面中显示。")}</div>
          ) : (
            <TextArea value={value} onChange={event => patchForm({ [key]: event.target.value } as Partial<ProblemDraft>)} rows={key === "statementSamples" ? 5 : 7} />
          )}
        </div>
      </Field>
    );
  }

  function renderSettingsTab() {
    return (
      <Panel title="题目设置">
        <div className="stack">
          <div className="form-grid">
            <Field label="提供者"><TextInput value={form.provider} onChange={event => patchForm({ provider: event.target.value })} /></Field>
            <Field label="题目难度">
              <Select value={form.difficulty} onChange={event => patchForm({ difficulty: event.target.value })}>
                <option value="EASY">EASY</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HARD">HARD</option>
              </Select>
            </Field>
            <Field label="默认时间限制 ms"><TextInput type="number" value={form.timeLimit} onChange={event => patchForm({ timeLimit: Number(event.target.value) })} /></Field>
            <Field label="默认空间限制 KiB"><TextInput type="number" value={form.memoryLimit} onChange={event => patchForm({ memoryLimit: Number(event.target.value) })} /></Field>
          </div>
          <Field label="题目附件">
            <div className="problem-attachment-box">
              <div className="problem-data-actions">
                <input
                  ref={attachmentFileRef}
                  type="file"
                  hidden
                  onChange={event => void uploadAttachment(event.target.files?.[0] || null)}
                />
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => attachmentFileRef.current?.click()}
                  disabled={busy}
                  icon={<UploadCloud size={16} />}
                >
                  选择文件上传
                </Button>
                <span className="muted">{form.id ? "附件上传后会自动保存到当前题目。" : "请先保存题目，再上传附件。"}</span>
              </div>
              {attachments.length ? (
                <ul className="problem-attachment-list">
                  {attachments.map(item => (
                    <li key={item.id}>
                      <FileText size={16} />
                      <span>{item.fileName}</span>
                      <small>{formatBytes(item.sizeBytes || 0)}</small>
                      <a href={item.downloadUrl} download>下载</a>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="muted">暂无附件。</p>
              )}
            </div>
          </Field>
          <Field label="题目标签"><TextArea value={form.tagsText} onChange={event => patchForm({ tagsText: event.target.value })} rows={3} placeholder="算法 / 来源 / 时间，每行一个。" /></Field>
          <div className="form-grid">
            <Field label="数据下载">
              <Select value={form.dataDownloadEnabled ? "on" : "off"} onChange={event => patchForm({ dataDownloadEnabled: event.target.value === "on" })}>
                <option value="on">开启</option>
                <option value="off">关闭</option>
              </Select>
            </Field>
            <Field label="显示分数">
              <Select value={form.scoreDisplayMode} onChange={event => patchForm({ scoreDisplayMode: event.target.value })}>
                <option value="OI">显示分数（OI 模式）</option>
                <option value="ICPC">不显示分数（ICPC 赛制）</option>
              </Select>
            </Field>
          </div>
          <details className="editor-compact-details">
            <summary>教学增强信息</summary>
            <div className="editor-compact-details__body">
              <Field label="AI 反馈方向"><TextArea value={form.aiPromptDirection} onChange={event => patchForm({ aiPromptDirection: event.target.value })} rows={3} /></Field>
              <Field label="起始代码"><TextArea value={form.starterCode} onChange={event => patchForm({ starterCode: event.target.value })} rows={6} /></Field>
              <div className="knowledge-grid">
                <Field label="知识点"><TextArea value={form.knowledgePointsText} onChange={event => patchForm({ knowledgePointsText: event.target.value })} rows={3} /></Field>
                <Field label="算法策略"><TextArea value={form.algorithmStrategiesText} onChange={event => patchForm({ algorithmStrategiesText: event.target.value })} rows={3} /></Field>
                <Field label="常见错误"><TextArea value={form.commonMistakesText} onChange={event => patchForm({ commonMistakesText: event.target.value })} rows={3} /></Field>
                <Field label="边界类型"><TextArea value={form.boundaryTypesText} onChange={event => patchForm({ boundaryTypesText: event.target.value })} rows={3} /></Field>
              </div>
            </div>
          </details>
        </div>
      </Panel>
    );
  }

  function renderDataTab() {
    return (
      <Panel title="数据设置" action={<Button type="button" variant="secondary" onClick={oneClickFill}>一键填充</Button>}>
        <div className="stack">
          <div className="problem-data-actions">
            <input ref={zipFileRef} type="file" accept=".zip" hidden onChange={event => {
              const file = event.target.files?.[0] || null;
              setZipFile(file);
              if (file) void previewZip(file);
            }} />
            <Button type="button" variant="secondary" onClick={() => zipFileRef.current?.click()} icon={<UploadCloud size={17} />}>上传数据</Button>
            <Button type="button" variant="primary" onClick={() => void commitZip()} disabled={busy || !zipFile || !zipPreview?.valid}>确认导入</Button>
            {form.id && form.dataDownloadEnabled ? <ButtonLinkDownload problemId={Number(form.id)} /> : null}
            <StatusPill tone={zipPreview?.valid ? "success" : zipPreview ? "danger" : "neutral"}>
              {zipPreview ? `${zipPreview.pairCount} 对 / ${formatBytes(zipPreview.uncompressedBytes)}` : "未选择 zip"}
            </StatusPill>
          </div>
          {zipPreview?.issues?.length ? (
            <div className="management-import-result">
              <h3>数据包问题</h3>
              <ul>{zipPreview.issues.slice(0, 8).map((issue, index) => <li key={index}>{issue.fileName ? `${issue.fileName}: ` : ""}{issue.message}</li>)}</ul>
            </div>
          ) : null}
          <div className="editor-test-table-wrap">
            <table className="editor-test-table problem-data-table">
              <thead>
                <tr>
                  <th>数据点</th>
                  <th>文件</th>
                  <th>样例</th>
                  <th>时间 ms</th>
                  <th>空间 MiB</th>
                  <th>子任务</th>
                  <th>分值</th>
                  <th>预览</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {form.testCases.map((item, index) => (
                  <tr key={`${item.id || "new"}-${index}`}>
                    <td>#{index + 1}</td>
                    <td>
                      {item.inputStorageType === "FILE" ? (
                        <span>{item.inputFileName} / {item.outputFileName}<small>{formatBytes((item.inputSizeBytes || 0) + (item.outputSizeBytes || 0))}</small></span>
                      ) : (
                        <span>手动输入</span>
                      )}
                    </td>
                    <td><input type="checkbox" checked={Boolean(item.publicExample) || !item.hidden} onChange={event => updateTestCase(index, { publicExample: event.target.checked })} /></td>
                    <td><TextInput type="number" value={item.timeLimitMs || form.timeLimit} onChange={event => updateTestCase(index, { timeLimitMs: Number(event.target.value) })} /></td>
                    <td><TextInput type="number" value={Math.round((item.memoryLimitKib || form.memoryLimit) / 1024)} onChange={event => updateTestCase(index, { memoryLimitKib: Number(event.target.value) * 1024 })} /></td>
                    <td><TextInput type="number" value={item.subtaskIndex || 0} onChange={event => updateTestCase(index, { subtaskIndex: Number(event.target.value) })} /></td>
                    <td><TextInput type="number" value={item.score || 0} onChange={event => updateTestCase(index, { score: Number(event.target.value) })} /></td>
                    <td>
                      <div className="actions">
                        <button type="button" className="editor-tool-button" onClick={() => void previewStoredCase(item, "input")}>in</button>
                        <button type="button" className="editor-tool-button" onClick={() => void previewStoredCase(item, "output")}>out</button>
                      </div>
                    </td>
                    <td><button type="button" className="editor-tool-button editor-tool-button--danger" onClick={() => removeCase(index)}>删</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="actions">
            <Button type="button" variant="secondary" onClick={() => addInlineCase(false)}>添加公开样例</Button>
            <Button type="button" variant="ghost" onClick={() => addInlineCase(true)}>添加隐藏点</Button>
          </div>
          {filePreview ? (
            <div className="management-import-result">
              <h3>{filePreview.title}</h3>
              <pre><code>{filePreview.lines.join("\n") || "空文件"}</code></pre>
              {filePreview.truncated ? <small>只显示前 10 行。</small> : null}
            </div>
          ) : null}
          <details className="editor-compact-details">
            <summary>手动 inline 数据</summary>
            <div className="stack">
              {form.testCases.map((item, index) => item.inputStorageType === "FILE" ? null : (
                <div className="two-column" key={`inline-${index}`}>
                  <Field label={`#${index + 1} 输入`}><TextArea value={item.input} onChange={event => updateTestCase(index, { input: event.target.value })} rows={4} /></Field>
                  <Field label={`#${index + 1} 输出`}><TextArea value={item.expectedOutput} onChange={event => updateTestCase(index, { expectedOutput: event.target.value })} rows={4} /></Field>
                </div>
              ))}
            </div>
          </details>
        </div>
      </Panel>
    );
  }

  function renderHelpTab() {
    return (
      <Panel title="功能说明">
        <div className="problem-help-grid">
          <article><h3>上传压缩包的要求</h3><p>直接将若干数据点打包成一个 zip。不要文件夹，不支持 rar。输入扩展名为 .in，输出扩展名为 .out，必须成对出现。</p></article>
          <article><h3>文件名要求</h3><p>文件名只能包含连续的一段数字，例如 game001.in、tribool4.in。T1-1.in 和 game.in 会被拒绝。</p></article>
          <article><h3>大小和数量</h3><p>压缩包不超过 50MB，解压后不超过 100MB，最多 50 对测试点。单点时间不超过 10s，内存不超过 512MiB。</p></article>
          <article><h3>预览规则</h3><p>数据内容默认不显示。点击预览时，只显示输入或输出文件的前 10 行，长行会被截断。</p></article>
          <article><h3>公开样例</h3><p>勾选公开样例后，该数据点会作为非隐藏测试点，并自动进入题面样例。过大的样例不建议公开。</p></article>
          <article><h3>Markdown 导入</h3><p>支持 # 标题，以及 ## 题目背景、## 题目描述、## 输入格式、## 输出格式、## 样例、## 提示说明。不会识别或修改题目状态。</p></article>
        </div>
      </Panel>
    );
  }

  function renderCatalog() {
    return (
      <details className="editor-compact-details editor-compact-details--side">
        <summary><span>题目列表</span><StatusPill tone="info">{catalog.length}</StatusPill></summary>
        <div className="stack editor-problem-list">
          {catalog.length ? catalog.map(item => (
            <button type="button" className="list-row" key={item.id} onClick={() => void loadProblem(item.id)} style={{ textAlign: "left" }}>
              <DifficultyPill difficulty={item.difficulty} />
              <h3>{item.title}</h3>
              <p>{item.summary || `${difficultyLabel(item.difficulty)} · ${item.timeLimit} ms`}</p>
            </button>
          )) : <EmptyState title="暂无题目" />}
        </div>
      </details>
    );
  }
}

function ButtonLinkDownload({ problemId }: { problemId: number }) {
  return (
    <a className="button button--secondary" href={`/api/problems/${problemId}/test-data/download`} download>
      <Download size={16} />
      下载数据
    </a>
  );
}

function composeDescription(form: ProblemDraft) {
  const sections = [
    section("题目背景", form.statementBackground),
    section("题目描述", form.statementDescription),
    section("输入格式", form.statementInputFormat),
    section("输出格式", form.statementOutputFormat),
    section("样例", buildSamples(form.testCases) || form.statementSamples),
    section("提示说明", form.statementHints)
  ].filter(Boolean);
  return sections.join("\n\n");
}

function section(title: string, value: string) {
  return value.trim() ? `## ${title}\n\n${value.trim()}` : "";
}

function buildSamples(testCases: TestCaseDraft[]) {
  const samples = testCases
    .filter(item => Boolean(item.publicExample) || !item.hidden)
    .map((item, index) => {
      if (item.inputStorageType === "FILE" && (!item.input || !item.expectedOutput)) {
        return `### 样例 ${index + 1}\n\n输入文件：${item.inputFileName || ""}\n\n输出文件：${item.outputFileName || ""}`;
      }
      return `### 样例 ${index + 1}\n\n#### 输入\n\n\`\`\`\n${limitSample(item.input)}\n\`\`\`\n\n#### 输出\n\n\`\`\`\n${limitSample(item.expectedOutput)}\n\`\`\``;
    });
  return samples.join("\n\n");
}

function limitSample(value: string) {
  const lines = (value || "").split(/\r?\n/);
  return lines.slice(0, 200).join("\n").slice(0, 20_000);
}

function splitList(value: string): string[] {
  return value
    .split(/[\n,，、;]/)
    .map(item => item.trim())
    .filter(Boolean)
    .filter((item, index, all) => all.indexOf(item) === index)
    .slice(0, 30);
}

function joinList(value?: string[] | null): string {
  return value?.length ? value.join("\n") : "";
}

function statusLabel(value: string) {
  if (value === "PUBLIC") return "公开";
  if (value === "PARTIAL") return "部分可见";
  if (value === "CONTEST") return "比赛赛题";
  return "隐藏";
}

function formatBytes(value: number) {
  if (!value) return "0 B";
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KiB`;
  return `${(value / 1024 / 1024).toFixed(1)} MiB`;
}

function stripKnownSections(value: string) {
  return value
    .replace(/^##\s*题目背景[\s\S]*?(?=^##\s*题目描述|$)/m, "")
    .replace(/^##\s*题目描述\s*/m, "")
    .trim();
}

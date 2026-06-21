import { ReactNode, useEffect, useMemo, useState } from "react";
import { ArrowLeft, BookOpen, Plus, Power, PowerOff, Save, Search, UploadCloud } from "lucide-react";
import { api } from "../../shared/api/client";
import type {
  AiStandardLibraryItem,
  AiStandardLibraryItemPayload,
  AiStandardLibraryLayer,
  ClassGroup,
  ImportCommit,
  ImportPreview,
  ProblemCatalogItem,
  Readiness
} from "../../shared/api/types";
import { displayText } from "../../shared/format";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea, TextInput } from "../../shared/ui/Field";
import { StatusPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };
type ImportKind = "class" | "problem";
type LibraryEnabledFilter = "" | "true" | "false";
type LibraryFilters = {
  query: string;
  layer: AiStandardLibraryLayer | "";
  category: string;
  enabled: LibraryEnabledFilter;
};
type LibraryDraft = {
  id?: number;
  layer: AiStandardLibraryLayer;
  code: string;
  category: string;
  name: string;
  description: string;
  studentExplanation: string;
  teacherExplanation: string;
  skillUnitCode: string;
  mistakeType: string;
  commonMisconception: string;
  evidenceSignals: string;
  commonCodePatterns: string;
  judgeSignals: string;
  requiredEvidence: string;
  whenToUse: string;
  studentBenefit: string;
  hintL1: string;
  hintL2: string;
  hintL3: string;
  abilityPoint: string;
  severity: string;
  applicableLanguages: string;
  relatedItems: string;
  knowledgeNodeCodes: string;
  prerequisiteKnowledgeCodes: string;
  teachingAction: string;
  enabled: boolean;
  libraryVersion: string;
};

const DEFAULT_LIBRARY_FILTERS: LibraryFilters = { query: "", layer: "", category: "", enabled: "" };

type TeacherManagementToolsProps = {
  embedded?: boolean;
};

export default function TeacherManagementPage() {
  return (
    <div className="teacher-page teacher-workflow teacher-manage-page">
      <section className="teacher-workflow-header teacher-workflow-header--simple teacher-manage-header">
        <div>
          <p className="eyebrow">教师端</p>
          <h1>管理</h1>
        </div>
        <ButtonLink to="/app/teacher" variant="secondary" icon={<ArrowLeft size={17} />}>
          返回作业
        </ButtonLink>
      </section>
      <TeacherManagementTools embedded />
    </div>
  );
}

export function TeacherManagementTools({ embedded = false }: TeacherManagementToolsProps) {
  const [classes, setClasses] = useState<ClassGroup[]>([]);
  const [problems, setProblems] = useState<ProblemCatalogItem[]>([]);
  const [classForm, setClassForm] = useState({ name: "", grade: "", teacherName: "" });
  const [targetClassGroupId, setTargetClassGroupId] = useState("");
  const [classImport, setClassImport] = useState({ format: "csv", content: "" });
  const [problemImport, setProblemImport] = useState({ format: "markdown", content: "" });
  const [classImportResult, setClassImportResult] = useState<ImportPreview | ImportCommit | null>(null);
  const [problemImportResult, setProblemImportResult] = useState<ImportPreview | ImportCommit | null>(null);
  const [classFileName, setClassFileName] = useState("");
  const [problemFileName, setProblemFileName] = useState("");
  const [alert, setAlert] = useState<Alert | null>(null);
  const [busy, setBusy] = useState(true);
  const [dataReady, setDataReady] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [readiness, setReadiness] = useState<Readiness | null>(null);
  const [aiSmokeBusy, setAiSmokeBusy] = useState(false);
  const [libraryItems, setLibraryItems] = useState<AiStandardLibraryItem[]>([]);
  const [libraryFilters, setLibraryFilters] = useState<LibraryFilters>(DEFAULT_LIBRARY_FILTERS);
  const [selectedLibraryId, setSelectedLibraryId] = useState<number | null>(null);
  const [libraryDraft, setLibraryDraft] = useState<LibraryDraft>(() => emptyLibraryDraft());
  const [libraryBusy, setLibraryBusy] = useState(false);

  useEffect(() => {
    void loadData();
    void loadReadiness();
  }, []);

  useEffect(() => {
    const handle = window.setTimeout(() => {
      void loadStandardLibrary(libraryFilters);
    }, 220);
    return () => window.clearTimeout(handle);
  }, [libraryFilters.query, libraryFilters.layer, libraryFilters.category, libraryFilters.enabled]);

  const cleanClasses = useMemo(
    () =>
      classes.map(item => ({
        ...item,
        name: displayText(item.name, `班级 #${item.id}`),
        grade: displayText(item.grade, ""),
        teacherName: displayText(item.teacherName, "")
      })),
    [classes]
  );

  async function loadData() {
    setBusy(true);
    setLoadFailed(false);
    try {
      const [classResult, problemResult] = await Promise.all([
        api.classes(),
        api.problemCatalog()
      ]);
      setClasses(classResult);
      setProblems(problemResult);
      if (!targetClassGroupId && classResult[0]) {
        setTargetClassGroupId(String(classResult[0].id));
      }
      setDataReady(true);
    } catch (error) {
      setLoadFailed(true);
      setDataReady(true);
    } finally {
      setBusy(false);
    }
  }

  async function loadReadiness() {
    try {
      setReadiness(await api.readiness());
    } catch {
      setReadiness(null);
    }
  }

  async function loadStandardLibrary(filters = libraryFilters, preferredId = selectedLibraryId) {
    setLibraryBusy(true);
    try {
      const items = await api.aiStandardLibraryItems({
        layer: filters.layer,
        category: filters.category,
        enabled: filters.enabled === "" ? "" : filters.enabled === "true",
        query: filters.query
      });
      setLibraryItems(items);
      const selected = items.find(item => item.id === preferredId) || items[0];
      if (selected) {
        setSelectedLibraryId(selected.id);
        setLibraryDraft(itemToDraft(selected));
      } else {
        setSelectedLibraryId(null);
        setLibraryDraft(emptyLibraryDraft());
      }
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "AI 标准库读取失败。" });
    } finally {
      setLibraryBusy(false);
    }
  }

  function selectLibraryItem(item: AiStandardLibraryItem) {
    setSelectedLibraryId(item.id);
    setLibraryDraft(itemToDraft(item));
  }

  function createLibraryDraft() {
    setSelectedLibraryId(null);
    setLibraryDraft(emptyLibraryDraft());
  }

  async function saveLibraryItem(draftOverride = libraryDraft) {
    if (!draftOverride.code.trim() || !draftOverride.category.trim() || !draftOverride.name.trim()) {
      setAlert({ type: "error", message: "请填写条目 ID、分类和名称。" });
      return;
    }
    setLibraryBusy(true);
    try {
      const payload = draftToPayload(draftOverride);
      const saved = draftOverride.id
        ? await api.updateAiStandardLibraryItem(draftOverride.id, payload)
        : await api.createAiStandardLibraryItem(payload);
      setAlert({ type: "success", message: "AI 标准库已保存。" });
      await loadStandardLibrary(libraryFilters, saved.id);
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "AI 标准库保存失败。" });
    } finally {
      setLibraryBusy(false);
    }
  }

  async function toggleLibraryItem(item: AiStandardLibraryItem) {
    setLibraryBusy(true);
    try {
      const updated = item.enabled
        ? await api.disableAiStandardLibraryItem(item.id)
        : await api.enableAiStandardLibraryItem(item.id);
      setAlert({ type: "success", message: updated.enabled ? "条目已启用。" : "条目已停用。" });
      await loadStandardLibrary(libraryFilters, updated.id);
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "条目状态更新失败。" });
    } finally {
      setLibraryBusy(false);
    }
  }

  async function runAiSmoke() {
    setAiSmokeBusy(true);
    try {
      const result = await api.aiSmoke();
      setAlert({ type: result.status === "READY" ? "success" : "error", message: result.message || "AI smoke 已完成。" });
      await loadReadiness();
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "AI smoke 失败。" });
    } finally {
      setAiSmokeBusy(false);
    }
  }

  async function createClass() {
    if (!classForm.name.trim()) {
      setAlert({ type: "error", message: "请填写班级名称。" });
      return;
    }
    setBusy(true);
    try {
      await api.createClass({
        name: classForm.name.trim(),
        grade: classForm.grade.trim(),
        teacherName: classForm.teacherName.trim()
      });
      setClassForm({ name: "", grade: "", teacherName: "" });
      setAlert({ type: "success", message: "班级已创建。" });
      await loadData();
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "班级创建失败。" });
    } finally {
      setBusy(false);
    }
  }

  function importPayload(kind: "class" | "problem") {
    const data = kind === "class" ? classImport : problemImport;
    return {
      format: data.format,
      content: data.content,
      classGroupId: targetClassGroupId ? Number(targetClassGroupId) : null,
      className: classForm.name || undefined
    };
  }

  async function runImport(kind: "class" | "problem", mode: "preview" | "commit") {
    const data = kind === "class" ? classImport : problemImport;
    if (!data.content.trim()) {
      setAlert({ type: "error", message: "请先粘贴或选择导入内容。" });
      return;
    }
    setBusy(true);
    try {
      const payload = importPayload(kind);
      const result =
        kind === "class"
          ? mode === "preview"
            ? await api.classImportPreview(payload)
            : await api.classImportCommit(payload)
          : mode === "preview"
            ? await api.problemImportPreview(payload)
            : await api.problemImportCommit(payload);
      if (kind === "class") {
        setClassImportResult(result);
      } else {
        setProblemImportResult(result);
      }
      setAlert({ type: "success", message: mode === "preview" ? "预览已生成。" : "导入已完成。" });
      if (mode === "commit") {
        await loadData();
      }
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "导入失败。" });
    } finally {
      setBusy(false);
    }
  }

  async function readImportFile(kind: "class" | "problem", file: File | null) {
    if (!file) {
      return;
    }
    const extension = file.name.split(".").pop()?.toLowerCase() || "";
    const format = extension === "xlsx" ? "xlsx" : extension === "json" ? "json" : extension === "md" || extension === "markdown" ? "markdown" : "csv";
    const content = extension === "xlsx" ? await readFileAsDataUrl(file) : await file.text();
    if (kind === "class") {
      setClassFileName(file.name);
      setClassImport({ format: format === "xlsx" ? "xlsx" : "csv", content });
    } else {
      setProblemFileName(file.name);
      setProblemImport({ format, content });
    }
  }

  const statusPill = loadFailed ? (
    <StatusPill tone="warning">读取失败</StatusPill>
  ) : dataReady ? (
    <StatusPill tone="neutral">{cleanClasses.length} 个班级 · {problems.length} 个题目</StatusPill>
  ) : (
    <StatusPill tone="neutral">读取中</StatusPill>
  );

  return (
    <div className={embedded ? "teacher-management-embed" : "stack teacher-management-page management-console-page"}>
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="management-console">
        {!embedded ? (
          <header className="management-console__brand">
            <div>
              <p className="eyebrow">教师端</p>
              <h1>管理</h1>
            </div>
            {statusPill}
          </header>
        ) : null}

        {embedded ? (
          <section className="teacher-manage-status-panel">
            <ReadinessPanel readiness={readiness} busy={aiSmokeBusy} summary={statusPill} onRefresh={loadReadiness} onAiSmoke={runAiSmoke} />
          </section>
        ) : (
          <details className="management-compact-details">
            <summary>
              <span>开课状态</span>
              {statusPill}
            </summary>
            <div className="management-compact-details__body">
              <ReadinessPanel readiness={readiness} busy={aiSmokeBusy} onRefresh={loadReadiness} onAiSmoke={runAiSmoke} />
            </div>
          </details>
        )}

        <main className="management-workspace">
          <section className="management-task-grid">
            <section className="management-task-card management-task-card--large">
              <h2>班级与名单</h2>
              <div className="management-inline-create">
                <div className="form-grid management-create-form">
                  <Field label="班级名称">
                    <TextInput value={classForm.name} onChange={event => setClassForm({ ...classForm, name: event.target.value })} />
                  </Field>
                  <Field label="年级">
                    <TextInput value={classForm.grade} onChange={event => setClassForm({ ...classForm, grade: event.target.value })} />
                  </Field>
                  <Field label="任课老师">
                    <TextInput value={classForm.teacherName} onChange={event => setClassForm({ ...classForm, teacherName: event.target.value })} />
                  </Field>
                </div>
                <Button type="button" variant="primary" onClick={() => void createClass()} disabled={busy}>
                  创建
                </Button>
              </div>
              <div className="management-form-row">
                <Field label="导入到班级">
                  <Select value={targetClassGroupId} onChange={event => setTargetClassGroupId(event.target.value)}>
                    <option value="">不指定班级</option>
                    {cleanClasses.map(item => (
                      <option value={item.id} key={item.id}>
                        {item.name}
                      </option>
                    ))}
                  </Select>
                </Field>
                <FilePicker
                  accept=".csv,.txt,.xlsx"
                  fileName={classFileName}
                  kind="class"
                  label="名单文件"
                  note="CSV 或 XLSX"
                  onPick={readImportFile}
                />
              </div>
              <Field label="粘贴名单">
                <TextArea
                  value={classImport.content}
                  onChange={event => setClassImport({ ...classImport, content: event.target.value })}
                  placeholder={"班级,姓名,学号\n高一1班,张三,01"}
                />
              </Field>
              <div className="actions">
                <Button type="button" variant="secondary" onClick={() => void runImport("class", "preview")} disabled={busy} icon={<UploadCloud size={17} />}>
                  预览名单
                </Button>
                <Button type="button" variant="primary" onClick={() => void runImport("class", "commit")} disabled={busy}>
                  导入名单
                </Button>
              </div>
              <ImportResult result={classImportResult} />
            </section>

            <section className="management-task-card management-task-card--large">
              <div className="management-card-head">
                <h2>题目</h2>
                <ButtonLink to="/app/task-editor" variant="secondary">
                  编辑题目
                </ButtonLink>
              </div>
              <div className="management-form-row">
                <FilePicker
                  accept=".md,.markdown,.json,.csv,.txt,.xlsx"
                  fileName={problemFileName}
                  kind="problem"
                  label="题目文件"
                  note="Markdown、JSON、CSV 或 XLSX"
                  onPick={readImportFile}
                />
              </div>
              <Field label="粘贴题目">
                <TextArea
                  value={problemImport.content}
                  onChange={event => setProblemImport({ ...problemImport, content: event.target.value })}
                  placeholder={"# 两数求和\n\n## 题目描述\n...\n\n## 样例输入\n1 2\n\n## 样例输出\n3"}
                />
              </Field>
              <div className="actions">
                <Button type="button" variant="secondary" onClick={() => void runImport("problem", "preview")} disabled={busy} icon={<UploadCloud size={17} />}>
                  预览题目
                </Button>
                <Button type="button" variant="primary" onClick={() => void runImport("problem", "commit")} disabled={busy}>
                  导入题目
                </Button>
              </div>
              <ImportResult result={problemImportResult} />
            </section>

            <details className="management-compact-details standard-library-details">
              <summary>
                <span>AI 标准库</span>
                <StatusPill tone="neutral">{libraryItems.length || "读取中"} 条</StatusPill>
              </summary>
              <div className="management-compact-details__body">
                <StandardLibraryManager
                  items={libraryItems}
                  filters={libraryFilters}
                  draft={libraryDraft}
                  busy={libraryBusy}
                  selectedId={selectedLibraryId}
                  onFiltersChange={setLibraryFilters}
                  onReload={filters => void loadStandardLibrary(filters || libraryFilters)}
                  onNew={createLibraryDraft}
                  onSelect={selectLibraryItem}
                  onDraftChange={setLibraryDraft}
                  onSave={draft => void saveLibraryItem(draft)}
                  onToggle={item => void toggleLibraryItem(item)}
                />
              </div>
            </details>
          </section>
        </main>
      </section>
    </div>
  );
}

function ReadinessPanel({
  readiness,
  busy,
  summary,
  onRefresh,
  onAiSmoke
}: {
  readiness: Readiness | null;
  busy: boolean;
  summary?: ReactNode;
  onRefresh: () => void;
  onAiSmoke: () => void;
}) {
  const status = readiness?.status || "UNKNOWN";
  const tone = status === "READY" ? "success" : status === "BLOCKED" ? "danger" : "warning";
  const blocking = readiness?.checks.filter(item => item.blocking && item.status !== "PASS") || [];
  const warnings = readiness?.checks.filter(item => item.status !== "PASS" && !(item.blocking && item.status !== "PASS")) || [];
  const visibleChecks = [...blocking, ...warnings].slice(0, 6);
  const compactChecks = visibleChecks.slice(0, 3);
  return (
    <section className="management-readiness">
      <div className="management-readiness__head">
        <div>
          <h2>开课状态</h2>
          <p>
            {status === "READY" ? "可以开课" : status === "BLOCKED" ? "暂不能正式开课" : "可试用"}
            {readiness ? ` · ${blocking.length} 阻断 · ${warnings.length} 提醒` : ""}
          </p>
        </div>
        <StatusPill tone={tone}>{status}</StatusPill>
        {summary ? <span className="management-readiness__summary">{summary}</span> : null}
        <div className="management-readiness__actions">
          <Button type="button" variant="secondary" onClick={() => void onRefresh()}>
            刷新
          </Button>
          <Button type="button" variant="primary" onClick={() => void onAiSmoke()} disabled={busy}>
            {busy ? "检测中" : "检测 AI"}
          </Button>
        </div>
      </div>
      {readiness ? (
        visibleChecks.length ? (
          <>
            <div className="management-readiness__chips">
              {compactChecks.map(item => (
                <span key={item.id} title={`${item.message} ${item.action}`}>
                  <StatusPill tone={item.status === "FAIL" ? "danger" : item.status === "WARN" ? "warning" : "neutral"}>{item.status}</StatusPill>
                  {item.label}
                </span>
              ))}
              {visibleChecks.length > compactChecks.length ? <small>+{visibleChecks.length - compactChecks.length}</small> : null}
            </div>
            <details className="management-readiness__details">
              <summary>查看检查详情</summary>
              <div className="management-readiness__checks">
                {visibleChecks.map(item => (
                  <article key={item.id} title={`${item.message} ${item.action}`}>
                    <strong>{item.label}</strong>
                    <StatusPill tone={item.status === "FAIL" ? "danger" : item.status === "WARN" ? "warning" : "neutral"}>{item.status}</StatusPill>
                    <p>{item.message}</p>
                  </article>
                ))}
              </div>
            </details>
          </>
        ) : (
          <p className="management-readiness__ok">全部关键检查已通过。</p>
        )
      ) : (
        <p className="management-readiness__ok">正在读取系统状态。</p>
      )}
    </section>
  );
}

function StandardLibraryManager({
  items,
  filters,
  draft,
  busy,
  selectedId,
  onFiltersChange,
  onReload,
  onNew,
  onSelect,
  onDraftChange,
  onSave,
  onToggle
}: {
  items: AiStandardLibraryItem[];
  filters: LibraryFilters;
  draft: LibraryDraft;
  busy: boolean;
  selectedId: number | null;
  onFiltersChange: (filters: LibraryFilters) => void;
  onReload: (filters?: LibraryFilters) => void;
  onNew: () => void;
  onSelect: (item: AiStandardLibraryItem) => void;
  onDraftChange: (draft: LibraryDraft) => void;
  onSave: (draft: LibraryDraft) => void;
  onToggle: (item: AiStandardLibraryItem) => void;
}) {
  const visibleItems = items.filter(item => libraryItemMatchesFilters(item, filters));
  const skillCount = visibleItems.filter(item => item.layer === "SKILL_UNIT").length;
  const mistakeCount = visibleItems.filter(item => item.layer === "MISTAKE_POINT").length;
  const categoryOptions = Array.from(new Set(items.map(item => item.category).filter(Boolean))).sort((left, right) => left.localeCompare(right, "zh-Hans-CN"));
  const selectedItem = visibleItems.find(item => item.id === selectedId) || items.find(item => item.id === selectedId) || null;

  function patchDraft(values: Partial<LibraryDraft>) {
    onDraftChange({ ...draft, ...values });
  }

  return (
    <section className="management-task-card management-task-card--large standard-library-manager">
      <div className="standard-library-manager__head">
        <div>
          <span className="standard-library-manager__icon">
            <BookOpen size={19} />
          </span>
          <div>
            <h2>AI 标准库</h2>
            <p>能力点 {skillCount} · 易错点 {mistakeCount}</p>
          </div>
        </div>
        <div className="actions">
          <Button type="button" variant="secondary" icon={<Plus size={17} />} onClick={onNew} disabled={busy}>
            新建条目
          </Button>
        </div>
      </div>

      <div className="standard-library-filters">
        <Field label="搜索">
          <TextInput
            name="query"
            value={filters.query}
            onChange={event => onFiltersChange(readFiltersFromElement(event.currentTarget))}
            onKeyDown={event => {
              if (event.key === "Enter") {
                onReload(readFiltersFromElement(event.currentTarget));
              }
            }}
            placeholder="ID、名称、知识点、能力点、易错点"
          />
        </Field>
        <Field label="类型">
          <Select name="layer" value={filters.layer} onChange={event => onFiltersChange(readFiltersFromElement(event.currentTarget))}>
            <option value="">全部</option>
            <option value="SKILL_UNIT">能力点</option>
            <option value="MISTAKE_POINT">易错点</option>
          </Select>
        </Field>
        <Field label="分类">
          <Select name="category" value={filters.category} onChange={event => onFiltersChange(readFiltersFromElement(event.currentTarget))}>
            <option value="">全部</option>
            {categoryOptions.map(category => (
              <option value={category} key={category}>
                {category}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="状态">
          <Select name="enabled" value={filters.enabled} onChange={event => onFiltersChange(readFiltersFromElement(event.currentTarget))}>
            <option value="">全部</option>
            <option value="true">启用</option>
            <option value="false">停用</option>
          </Select>
        </Field>
        <Button type="button" variant="secondary" icon={<Search size={17} />} onClick={event => onReload(readFiltersFromElement(event.currentTarget))} disabled={busy}>
          筛选
        </Button>
      </div>

      <div className="standard-library-workbench">
        <div className="standard-library-list" aria-label="AI 标准库条目">
          {visibleItems.length ? (
            visibleItems.slice(0, 160).map(item => (
              <button
                type="button"
                key={item.id}
                className={`standard-library-list__item ${item.id === selectedId ? "is-active" : ""}`}
                onClick={() => onSelect(item)}
              >
                <span>
                  <strong>{item.name}</strong>
                  <small>{item.code}</small>
                </span>
                <span>
                  <StatusPill tone={item.layer === "SKILL_UNIT" ? "info" : "warning"}>{layerLabel(item.layer)}</StatusPill>
                  <StatusPill tone={item.enabled ? "success" : "neutral"}>{item.enabled ? "启用" : "停用"}</StatusPill>
                </span>
              </button>
            ))
          ) : (
            <EmptyState title="暂无条目" description="换一个筛选条件，或新建一个标准库条目。" />
          )}
        </div>

        <div className="standard-library-editor">
          <div className="standard-library-editor__head">
            <div>
              <h3>{draft.id ? draft.name || "编辑条目" : "新建条目"}</h3>
              <p>{draft.id ? `${draft.code} · ${draft.category}` : "保存后进入当前学校标准库"}</p>
            </div>
            {selectedItem ? (
              <div className="actions">
                <Button
                  type="button"
                  variant={selectedItem.enabled ? "secondary" : "primary"}
                  icon={selectedItem.enabled ? <PowerOff size={17} /> : <Power size={17} />}
                  onClick={() => onToggle(selectedItem)}
                  disabled={busy}
                >
                  {selectedItem.enabled ? "停用" : "启用"}
                </Button>
                <Button type="button" variant="primary" icon={<Save size={17} />} onClick={event => onSave(readDraftFromElement(event.currentTarget, draft))} disabled={busy}>
                  保存
                </Button>
              </div>
            ) : null}
          </div>

          <div className="form-grid standard-library-editor__core">
            <Field label="类型">
              <Select name="layer" value={draft.layer} onChange={event => patchDraft({ layer: event.target.value as AiStandardLibraryLayer })}>
                <option value="SKILL_UNIT">能力点</option>
                <option value="MISTAKE_POINT">易错点</option>
              </Select>
            </Field>
            <Field label="条目 ID">
              <TextInput name="code" value={draft.code} onChange={event => patchDraft({ code: event.target.value })} placeholder="SK_LOOP_ENDPOINT_INCLUSION" />
            </Field>
            <Field label="分类">
              <TextInput name="category" value={draft.category} onChange={event => patchDraft({ category: event.target.value })} />
            </Field>
            <Field label="名称">
              <TextInput name="name" value={draft.name} onChange={event => patchDraft({ name: event.target.value })} />
            </Field>
          </div>

          <div className="standard-library-editor__textarea-grid">
            <Field label={draft.layer === "SKILL_UNIT" ? "能力定义" : "易错点定义"}>
              <TextArea name="description" value={draft.description} onChange={event => patchDraft({ description: event.target.value })} />
            </Field>
            <Field label={draft.layer === "SKILL_UNIT" ? "学习目标" : "学生常见误解"}>
              <TextArea name="studentExplanation" value={draft.studentExplanation} onChange={event => patchDraft({ studentExplanation: event.target.value })} />
            </Field>
            <Field label="教师解释">
              <TextArea name="teacherExplanation" value={draft.teacherExplanation} onChange={event => patchDraft({ teacherExplanation: event.target.value })} />
            </Field>
            {draft.layer === "MISTAKE_POINT" ? (
              <Field label="常见误解">
                <TextArea name="commonMisconception" value={draft.commonMisconception} onChange={event => patchDraft({ commonMisconception: event.target.value })} />
              </Field>
            ) : null}
          </div>

          <div className="form-grid standard-library-editor__meta">
            {draft.layer === "MISTAKE_POINT" ? (
              <>
                <Field label="所属能力点">
                  <TextInput name="skillUnitCode" value={draft.skillUnitCode} onChange={event => patchDraft({ skillUnitCode: event.target.value })} placeholder="SK_..." />
                </Field>
                <Field label="易错类型">
                  <Select name="mistakeType" value={draft.mistakeType} onChange={event => patchDraft({ mistakeType: event.target.value })}>
                    <option value="">未指定</option>
                    <option value="CONCEPT">概念</option>
                    <option value="BOUNDARY">边界</option>
                    <option value="IO_FORMAT">输入输出</option>
                    <option value="STATE">状态</option>
                    <option value="TRANSITION">转移</option>
                    <option value="COMPLEXITY">复杂度</option>
                    <option value="MODELING">建模</option>
                    <option value="SYNTAX">语法</option>
                    <option value="RUNTIME">运行时</option>
                    <option value="DEBUGGING">调试习惯</option>
                  </Select>
                </Field>
              </>
            ) : null}
            <Field label="严重度">
              <Select name="severity" value={draft.severity} onChange={event => patchDraft({ severity: event.target.value })}>
                <option value="">未指定</option>
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </Select>
            </Field>
            <Field label="版本">
              <TextInput name="libraryVersion" value={draft.libraryVersion} onChange={event => patchDraft({ libraryVersion: event.target.value })} />
            </Field>
          </div>

          <div className="standard-library-editor__textarea-grid standard-library-editor__signals">
            <Field label="关联知识点">
              <TextArea name="knowledgeNodeCodes" value={draft.knowledgeNodeCodes} onChange={event => patchDraft({ knowledgeNodeCodes: event.target.value })} placeholder="每行一个知识点 ID" />
            </Field>
            <Field label="适用语言">
              <TextArea name="applicableLanguages" value={draft.applicableLanguages} onChange={event => patchDraft({ applicableLanguages: event.target.value })} placeholder="PYTHON&#10;CPP17" />
            </Field>
            <Field label="前置知识">
              <TextArea name="prerequisiteKnowledgeCodes" value={draft.prerequisiteKnowledgeCodes} onChange={event => patchDraft({ prerequisiteKnowledgeCodes: event.target.value })} placeholder="每行一个知识点或能力点 ID" />
            </Field>
            <Field label="相关条目">
              <TextArea name="relatedItems" value={draft.relatedItems} onChange={event => patchDraft({ relatedItems: event.target.value })} placeholder="每行一个条目 ID" />
            </Field>
          </div>
        </div>
      </div>
    </section>
  );
}

function FilePicker({
  accept,
  fileName,
  kind,
  label,
  note,
  onPick
}: {
  accept: string;
  fileName: string;
  kind: ImportKind;
  label: string;
  note: string;
  onPick: (kind: ImportKind, file: File | null) => void | Promise<void>;
}) {
  return (
    <label className="field management-file-picker">
      <span>{label}</span>
      <span className="management-file-picker__box">
        <UploadCloud size={18} />
        <strong>{fileName || "选择文件"}</strong>
        <small>{fileName ? "已读取" : note}</small>
      </span>
      <input type="file" accept={accept} onChange={event => void onPick(kind, event.target.files?.[0] || null)} />
    </label>
  );
}

function emptyLibraryDraft(): LibraryDraft {
  return {
    layer: "MISTAKE_POINT",
    code: "",
    category: "",
    name: "",
    description: "",
    studentExplanation: "",
    teacherExplanation: "",
    skillUnitCode: "",
    mistakeType: "",
    commonMisconception: "",
    evidenceSignals: "",
    commonCodePatterns: "",
    judgeSignals: "",
    requiredEvidence: "",
    whenToUse: "",
    studentBenefit: "",
    hintL1: "",
    hintL2: "",
    hintL3: "",
    abilityPoint: "",
    severity: "MEDIUM",
    applicableLanguages: "PYTHON\nCPP17",
    relatedItems: "",
    knowledgeNodeCodes: "",
    prerequisiteKnowledgeCodes: "",
    teachingAction: "",
    enabled: true,
    libraryVersion: "standard-library-v3-skill-mistake"
  };
}

function itemToDraft(item: AiStandardLibraryItem): LibraryDraft {
  return {
    id: item.id,
    layer: item.layer,
    code: item.code,
    category: item.category,
    name: item.name,
    description: item.description || "",
    studentExplanation: item.studentExplanation || "",
    teacherExplanation: item.teacherExplanation || "",
    skillUnitCode: item.skillUnitCode || "",
    mistakeType: item.mistakeType || "",
    commonMisconception: item.commonMisconception || "",
    evidenceSignals: linesToText(item.evidenceSignals),
    commonCodePatterns: linesToText(item.commonCodePatterns),
    judgeSignals: linesToText(item.judgeSignals),
    requiredEvidence: linesToText(item.requiredEvidence),
    whenToUse: item.whenToUse || "",
    studentBenefit: item.studentBenefit || "",
    hintL1: item.hintL1 || "",
    hintL2: item.hintL2 || "",
    hintL3: item.hintL3 || "",
    abilityPoint: item.abilityPoint || "",
    severity: item.severity || "",
    applicableLanguages: linesToText(item.applicableLanguages),
    relatedItems: linesToText(item.relatedItems),
    knowledgeNodeCodes: linesToText(item.knowledgeNodeCodes),
    prerequisiteKnowledgeCodes: linesToText(item.prerequisiteKnowledgeCodes),
    teachingAction: item.teachingAction || "",
    enabled: item.enabled,
    libraryVersion: item.libraryVersion || "standard-library-v3-skill-mistake"
  };
}

function draftToPayload(draft: LibraryDraft): AiStandardLibraryItemPayload {
  return {
    layer: draft.layer,
    code: draft.code.trim(),
    category: draft.category.trim(),
    name: draft.name.trim(),
    description: draft.description.trim(),
    studentExplanation: draft.studentExplanation.trim(),
    teacherExplanation: draft.teacherExplanation.trim(),
    skillUnitCode: draft.skillUnitCode.trim(),
    mistakeType: draft.mistakeType.trim(),
    commonMisconception: draft.commonMisconception.trim(),
    evidenceSignals: [],
    commonCodePatterns: [],
    judgeSignals: [],
    requiredEvidence: [],
    whenToUse: "",
    studentBenefit: "",
    hintL1: "",
    hintL2: "",
    hintL3: "",
    abilityPoint: draft.layer === "SKILL_UNIT" ? draft.name.trim() : draft.skillUnitCode.trim(),
    severity: draft.severity.trim(),
    applicableLanguages: textToLines(draft.applicableLanguages),
    relatedItems: textToLines(draft.relatedItems),
    knowledgeNodeCodes: textToLines(draft.knowledgeNodeCodes),
    prerequisiteKnowledgeCodes: textToLines(draft.prerequisiteKnowledgeCodes),
    teachingAction: "",
    enabled: draft.enabled,
    libraryVersion: draft.libraryVersion.trim()
  };
}

function linesToText(lines: string[] | undefined | null): string {
  return (lines || []).join("\n");
}

function textToLines(text: string): string[] {
  return text
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(Boolean);
}

function layerLabel(layer: AiStandardLibraryLayer) {
  if (layer === "SKILL_UNIT") return "能力点";
  if (layer === "MISTAKE_POINT") return "易错点";
  if (layer === "BASIC_CAUSE") return "旧基础层";
  return "旧提高层";
}

function libraryItemMatchesFilters(item: AiStandardLibraryItem, filters: LibraryFilters) {
  if (filters.layer && item.layer !== filters.layer) {
    return false;
  }
  if (filters.category && item.category !== filters.category) {
    return false;
  }
  if (filters.enabled && item.enabled !== (filters.enabled === "true")) {
    return false;
  }
  const query = filters.query.trim().toLowerCase();
  if (!query) {
    return true;
  }
  const haystack = [
    item.code,
    item.name,
    item.category,
    item.description,
    item.studentExplanation,
    item.teacherExplanation,
    item.skillUnitCode,
    item.mistakeType,
    item.commonMisconception,
    ...item.evidenceSignals,
    ...item.commonCodePatterns,
    ...item.judgeSignals,
    ...item.requiredEvidence,
    item.abilityPoint,
    item.severity,
    ...item.applicableLanguages,
    ...item.relatedItems,
    ...item.knowledgeNodeCodes,
    ...item.prerequisiteKnowledgeCodes,
    item.teachingAction
  ]
    .filter(Boolean)
    .join("\n")
    .toLowerCase();
  return haystack.includes(query);
}

function readFiltersFromElement(element: HTMLElement): LibraryFilters {
  const form = element.closest(".standard-library-filters");
  const value = (name: string) => {
    const field = form?.querySelector<HTMLInputElement | HTMLSelectElement>(`[name="${name}"]`);
    return field?.value || "";
  };
  return {
    query: value("query"),
    layer: value("layer") as AiStandardLibraryLayer | "",
    category: value("category"),
    enabled: value("enabled") as LibraryEnabledFilter
  };
}

function readDraftFromElement(element: HTMLElement, fallback: LibraryDraft): LibraryDraft {
  const editor = element.closest(".standard-library-manager")?.querySelector(".standard-library-editor");
  const value = (name: keyof LibraryDraft) => {
    const field = editor?.querySelector<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>(`[name="${String(name)}"]`);
    return field?.value ?? String(fallback[name] ?? "");
  };
  return {
    ...fallback,
    layer: value("layer") as AiStandardLibraryLayer,
    code: value("code"),
    category: value("category"),
    name: value("name"),
    description: value("description"),
    studentExplanation: value("studentExplanation"),
    teacherExplanation: value("teacherExplanation"),
    skillUnitCode: value("skillUnitCode"),
    mistakeType: value("mistakeType"),
    commonMisconception: value("commonMisconception"),
    evidenceSignals: value("evidenceSignals"),
    commonCodePatterns: value("commonCodePatterns"),
    judgeSignals: value("judgeSignals"),
    requiredEvidence: value("requiredEvidence"),
    whenToUse: value("whenToUse"),
    studentBenefit: value("studentBenefit"),
    hintL1: value("hintL1"),
    hintL2: value("hintL2"),
    hintL3: value("hintL3"),
    abilityPoint: value("abilityPoint"),
    severity: value("severity"),
    applicableLanguages: value("applicableLanguages"),
    relatedItems: value("relatedItems"),
    knowledgeNodeCodes: value("knowledgeNodeCodes"),
    prerequisiteKnowledgeCodes: value("prerequisiteKnowledgeCodes"),
    teachingAction: value("teachingAction"),
    libraryVersion: value("libraryVersion")
  };
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.addEventListener("load", () => resolve(String(reader.result || "")));
    reader.addEventListener("error", () => reject(reader.error || new Error("文件读取失败")));
    reader.readAsDataURL(file);
  });
}

function ImportResult({ result }: { result: ImportPreview | ImportCommit | null }) {
  if (!result) {
    return null;
  }
  const isCommit = "createdCount" in result;
  return (
    <div className="management-import-result">
      <h3>{result.message || (isCommit ? "导入完成" : "预览完成")}</h3>
      {isCommit ? (
        <div className="status-box-row">
          <StatusPill tone="success">新增 {(result as ImportCommit).createdCount}</StatusPill>
          <StatusPill tone="info">更新 {(result as ImportCommit).updatedCount}</StatusPill>
          <StatusPill tone="neutral">跳过 {(result as ImportCommit).skippedCount}</StatusPill>
          <StatusPill tone={(result as ImportCommit).failedCount ? "danger" : "neutral"}>失败 {(result as ImportCommit).failedCount}</StatusPill>
        </div>
      ) : (
        <div className="status-box-row">
          <StatusPill tone="info">总行数 {(result as ImportPreview).totalRows}</StatusPill>
          <StatusPill tone="success">有效 {(result as ImportPreview).validRows}</StatusPill>
          <StatusPill tone={(result as ImportPreview).invalidRows ? "warning" : "neutral"}>无效 {(result as ImportPreview).invalidRows}</StatusPill>
          <StatusPill tone={(result as ImportPreview).duplicateRows ? "warning" : "neutral"}>重复 {(result as ImportPreview).duplicateRows}</StatusPill>
        </div>
      )}
      {!!result.issues?.length && (
        <ul>
          {result.issues.slice(0, 6).map(issue => (
            <li key={`${issue.rowNumber}-${issue.message}`}>
              第 {issue.rowNumber} 行：{issue.message}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

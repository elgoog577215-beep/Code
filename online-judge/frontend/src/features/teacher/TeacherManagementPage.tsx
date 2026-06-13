import { useEffect, useMemo, useState } from "react";
import { ShieldCheck, UploadCloud } from "lucide-react";
import { api } from "../../shared/api/client";
import type { ClassGroup, ImportCommit, ImportPreview, ProblemCatalogItem, Readiness } from "../../shared/api/types";
import { displayText } from "../../shared/format";
import { Button } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea, TextInput } from "../../shared/ui/Field";
import { StatusPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };
type ImportKind = "class" | "problem";

export default function TeacherManagementPage() {
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

  useEffect(() => {
    void loadData();
    void loadReadiness();
  }, []);

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

  return (
    <div className="stack teacher-management-page management-console-page">
      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <section className="management-console">
        <header className="management-console__brand">
          <div>
            <p className="eyebrow">教师端</p>
            <h1>管理</h1>
          </div>
          {loadFailed ? <StatusPill tone="warning">读取失败</StatusPill> : dataReady ? <StatusPill tone="neutral">{cleanClasses.length} 个班级 · {problems.length} 个题目</StatusPill> : <StatusPill tone="neutral">读取中</StatusPill>}
        </header>

        <ReadinessPanel readiness={readiness} busy={aiSmokeBusy} onRefresh={loadReadiness} onAiSmoke={runAiSmoke} />

        <main className="management-workspace">
          <section className="management-task-grid">
            <section className="management-task-card">
              <h2>创建班级</h2>
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
                创建班级
              </Button>
            </section>

            <section className="management-task-card management-task-card--large">
              <h2>导入名单</h2>
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
              <h2>导入题目</h2>
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
          </section>
        </main>
      </section>
    </div>
  );
}

function ReadinessPanel({
  readiness,
  busy,
  onRefresh,
  onAiSmoke
}: {
  readiness: Readiness | null;
  busy: boolean;
  onRefresh: () => void;
  onAiSmoke: () => void;
}) {
  const status = readiness?.status || "UNKNOWN";
  const tone = status === "READY" ? "success" : status === "BLOCKED" ? "danger" : "warning";
  const blocking = readiness?.checks.filter(item => item.blocking && item.status !== "PASS") || [];
  const warnings = readiness?.checks.filter(item => item.status !== "PASS" && !(item.blocking && item.status !== "PASS")) || [];
  return (
    <section className="management-readiness">
      <div className="management-readiness__head">
        <span className="management-readiness__icon">
          <ShieldCheck size={20} />
        </span>
        <div>
          <h2>开课状态</h2>
          <p>{status === "READY" ? "可以开课" : status === "BLOCKED" ? "暂不能正式开课" : "可试用，需关注降级项"}</p>
        </div>
        <StatusPill tone={tone}>{status}</StatusPill>
      </div>
      {readiness ? (
        <div className="management-readiness__checks">
          {[...blocking, ...warnings].slice(0, 5).map(item => (
            <article key={item.id}>
              <strong>{item.label}</strong>
              <p>{item.message}</p>
              <small>{item.action}</small>
            </article>
          ))}
          {!blocking.length && !warnings.length ? <p className="management-readiness__ok">全部关键检查已通过。</p> : null}
        </div>
      ) : (
        <p className="management-readiness__ok">正在读取系统状态。</p>
      )}
      <div className="actions">
        <Button type="button" variant="secondary" onClick={() => void onRefresh()}>
          刷新状态
        </Button>
        <Button type="button" variant="primary" onClick={() => void onAiSmoke()} disabled={busy}>
          {busy ? "检测中" : "检测 AI"}
        </Button>
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

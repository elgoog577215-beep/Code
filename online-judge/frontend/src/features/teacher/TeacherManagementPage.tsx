import { useEffect, useMemo, useState } from "react";
import { BookOpen, CheckCircle2, Database, RefreshCw, ServerCog, UploadCloud, UsersRound } from "lucide-react";
import { ButtonLink } from "../../shared/ui/Button";
import { api } from "../../shared/api/client";
import type { ClassGroup, ExecutorStatus, ImportCommit, ImportPreview, ProblemCatalogItem, StudentIdentityAudit } from "../../shared/api/types";
import { displayText } from "../../shared/format";
import { Button } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea, TextInput } from "../../shared/ui/Field";
import { StatusPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };
type ManagementSection = "classes" | "problems" | "system";
type ImportKind = "class" | "problem";

export default function TeacherManagementPage() {
  const [activeSection, setActiveSection] = useState<ManagementSection>("classes");
  const [classes, setClasses] = useState<ClassGroup[]>([]);
  const [problems, setProblems] = useState<ProblemCatalogItem[]>([]);
  const [executor, setExecutor] = useState<ExecutorStatus | null>(null);
  const [classForm, setClassForm] = useState({ name: "", grade: "", teacherName: "" });
  const [targetClassGroupId, setTargetClassGroupId] = useState("");
  const [classImport, setClassImport] = useState({ format: "csv", content: "" });
  const [problemImport, setProblemImport] = useState({ format: "markdown", content: "" });
  const [classImportResult, setClassImportResult] = useState<ImportPreview | ImportCommit | null>(null);
  const [problemImportResult, setProblemImportResult] = useState<ImportPreview | ImportCommit | null>(null);
  const [identityAudit, setIdentityAudit] = useState<StudentIdentityAudit | null>(null);
  const [classFileName, setClassFileName] = useState("");
  const [problemFileName, setProblemFileName] = useState("");
  const [alert, setAlert] = useState<Alert | null>(null);
  const [busy, setBusy] = useState(true);
  const [dataReady, setDataReady] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);

  useEffect(() => {
    void loadData();
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

  const classCountText = loadFailed ? "读取失败" : dataReady ? `${cleanClasses.length} 个班级` : "读取中";
  const problemCountText = loadFailed ? "读取失败" : dataReady ? `${problems.length} 个题目` : "读取中";
  const systemStateText = loadFailed ? "读取失败" : !dataReady ? "读取中" : executor?.cppAvailable ? "评测可用" : "需要检查";

  const sections = [
    {
      id: "classes" as const,
      label: "班级与名单",
      note: classCountText,
      icon: UsersRound
    },
    {
      id: "problems" as const,
      label: "题目导入",
      note: problemCountText,
      icon: BookOpen
    },
    {
      id: "system" as const,
      label: "系统状态",
      note: systemStateText,
      icon: ServerCog
    }
  ];

  async function loadData() {
    setBusy(true);
    setLoadFailed(false);
    try {
      const [classResult, problemResult, executorResult] = await Promise.all([
        api.classes(),
        api.problemCatalog(),
        api.executorStatus()
      ]);
      setClasses(classResult);
      setProblems(problemResult);
      setExecutor(executorResult);
      if (!targetClassGroupId && classResult[0]) {
        setTargetClassGroupId(String(classResult[0].id));
      }
      const auditTargetId = targetClassGroupId ? Number(targetClassGroupId) : classResult[0]?.id || null;
      setIdentityAudit(auditTargetId ? await api.studentIdentityAudit(auditTargetId) : null);
      setDataReady(true);
    } catch (error) {
      setLoadFailed(true);
      setDataReady(true);
    } finally {
      setBusy(false);
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

  async function loadIdentityAudit(classGroupId = targetClassGroupId) {
    if (!classGroupId) {
      setIdentityAudit(null);
      return;
    }
    setBusy(true);
    try {
      setIdentityAudit(await api.studentIdentityAudit(Number(classGroupId)));
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "身份审计读取失败。" });
    } finally {
      setBusy(false);
    }
  }

  async function mergeIdentityGroup(profileIds: number[]) {
    if (!targetClassGroupId || profileIds.length < 2) {
      return;
    }
    setBusy(true);
    try {
      const audit = await api.mergeStudentIdentities(Number(targetClassGroupId), {
        studentProfileIds: profileIds,
        targetStudentProfileId: profileIds[0]
      });
      setIdentityAudit(audit);
      setAlert({ type: "success", message: "学生身份已合并。" });
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "学生身份合并失败。" });
    } finally {
      setBusy(false);
    }
  }

  async function splitIdentityProfile(profileId: number) {
    if (!targetClassGroupId) {
      return;
    }
    setBusy(true);
    try {
      const audit = await api.splitStudentIdentity(Number(targetClassGroupId), { studentProfileId: profileId });
      setIdentityAudit(audit);
      setAlert({ type: "success", message: "学生身份已拆分。" });
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "学生身份拆分失败。" });
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
        <aside className="management-console__nav">
          <div className="management-console__brand">
            <div>
              <h1>教师</h1>
              <nav className="teacher-mode-tabs" aria-label="教师功能">
                <ButtonLink to="/app/teacher" variant="ghost">
                  课堂过程
                </ButtonLink>
                <span className="teacher-mode-tab is-active">管理</span>
              </nav>
            </div>
            <Button type="button" variant="ghost" onClick={() => void loadData()} disabled={busy} icon={<RefreshCw size={17} />}>
              刷新
            </Button>
          </div>

          <nav className="management-section-list" aria-label="教师管理功能">
            {sections.map(item => (
              <button
                type="button"
                className={item.id === activeSection ? "is-active" : ""}
                key={item.id}
                onClick={() => setActiveSection(item.id)}
              >
                <item.icon size={18} />
                <span>
                  <strong>{item.label}</strong>
                  <StatusPill tone={item.id === "system" && item.note === "需要检查" ? "warning" : "neutral"}>{item.note}</StatusPill>
                </span>
              </button>
            ))}
          </nav>
        </aside>

        <main className="management-workspace">
          {activeSection === "classes" && (
            <section className="management-task-grid">
              <div className="management-task-card management-task-card--large">
                <div className="management-form-row">
                  <Field label="导入到班级">
                    <Select
                      value={targetClassGroupId}
                      onChange={event => {
                        setTargetClassGroupId(event.target.value);
                        void loadIdentityAudit(event.target.value);
                      }}
                    >
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
              </div>

              <details className="management-compact-details">
                <summary>
                  <span>创建班级</span>
                  <StatusPill tone="info">{classCountText}</StatusPill>
                </summary>
                <div className="management-compact-details__body">
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
                </div>
              </details>

              <details className="management-compact-details">
                <summary>
                  <span>身份审计</span>
                  <StatusPill tone={identityAudit?.duplicateGroupCount ? "warning" : "neutral"}>
                    {identityAudit ? `${identityAudit.duplicateGroupCount} 组重复` : "未读取"}
                  </StatusPill>
                </summary>
                <div className="management-compact-details__body management-identity-audit">
                  <div className="management-identity-audit__head">
                    <h3>{identityAudit?.className || "学生身份"}</h3>
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => void loadIdentityAudit()}
                      disabled={busy || !targetClassGroupId}
                      icon={<RefreshCw size={16} />}
                    >
                      刷新
                    </Button>
                  </div>
                  {identityAudit ? (
                    <>
                      <div className="management-status-grid management-identity-audit__metrics">
                        <StatusItem label="学生记录" value={String(identityAudit.totalProfiles)} />
                        <StatusItem label="稳定身份" value={String(identityAudit.stableIdentityCount)} />
                        <StatusItem label="人工身份" value={String(identityAudit.manualIdentityCount || 0)} />
                        <StatusItem label="旧版身份" value={String(identityAudit.legacyIdentityCount)} ready={!identityAudit.legacyIdentityCount} />
                        <StatusItem label="缺学号" value={String(identityAudit.missingStudentNoCount)} ready={!identityAudit.missingStudentNoCount} />
                        <StatusItem label="疑似重复" value={String(identityAudit.duplicateGroupCount)} ready={!identityAudit.duplicateGroupCount} />
                      </div>
                      {identityAudit.duplicateGroups.length ? (
                        <div className="management-identity-groups">
                          {identityAudit.duplicateGroups.slice(0, 6).map(group => (
                            <div key={group.stableIdentityKey}>
                              <div>
                                <strong>{group.displayNames.join(" / ") || group.stableIdentityKey}</strong>
                                <small>{group.studentProfileIds.join(", ")} · {group.identityKeys.join(" / ")}</small>
                              </div>
                              <div className="actions">
                                <Button
                                  type="button"
                                  variant="secondary"
                                  disabled={busy}
                                  onClick={() => void mergeIdentityGroup(group.studentProfileIds)}
                                >
                                  合并
                                </Button>
                                {group.studentProfileIds.map(profileId => (
                                  <Button
                                    type="button"
                                    variant="ghost"
                                    key={profileId}
                                    disabled={busy}
                                    onClick={() => void splitIdentityProfile(profileId)}
                                  >
                                    拆分 #{profileId}
                                  </Button>
                                ))}
                              </div>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <EmptyState title="暂无疑似重复身份" />
                      )}
                    </>
                  ) : (
                    <EmptyState title="未读取身份审计" />
                  )}
                </div>
              </details>
            </section>
          )}

          {activeSection === "problems" && (
            <section className="management-task-card management-task-card--large">
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
          )}

          {activeSection === "system" && (
            <section className="management-task-card management-task-card--large">
              {!executor ? (
                <EmptyState title="正在检测执行环境" />
              ) : (
                <div className="management-status-grid">
                  <StatusItem label="模式" value={executor.mode} />
                  <StatusItem label="Docker" value={executor.dockerAvailable ? "可用" : "未配置"} ready={executor.dockerAvailable} />
                  <StatusItem label="Python" value={executor.pythonAvailable ? "可用" : "未就绪"} ready={executor.pythonAvailable} />
                  <StatusItem label="C++" value={executor.cppAvailable ? "可用" : "未就绪"} ready={executor.cppAvailable} />
                  <StatusPill tone={executor.cppAvailable ? "success" : "warning"}>{executor.message}</StatusPill>
                </div>
              )}
            </section>
          )}
        </main>
      </section>
    </div>
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

function StatusItem({ label, value, ready = true }: { label: string; value: string; ready?: boolean }) {
  return (
    <div className="management-status-item">
      {ready ? <CheckCircle2 size={18} /> : <Database size={18} />}
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
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

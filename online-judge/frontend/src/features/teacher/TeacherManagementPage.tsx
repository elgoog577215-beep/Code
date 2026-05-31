import { useEffect, useMemo, useState } from "react";
import { useLocation } from "react-router-dom";
import {
  BookOpen,
  CheckCircle2,
  Database,
  FileArchive,
  FileText,
  ListChecks,
  RefreshCw,
  Route,
  ServerCog,
  UploadCloud,
  UsersRound
} from "lucide-react";
import { ButtonLink } from "../../shared/ui/Button";
import { api } from "../../shared/api/client";
import type {
  AiRouteHealth,
  AiRouteHealthLevel,
  ClassGroup,
  ExecutorStatus,
  ImportCommit,
  ImportPreview,
  ProblemCatalogItem,
  StudentIdentityAudit
} from "../../shared/api/types";
import { displayText } from "../../shared/format";
import { Button } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextArea, TextInput } from "../../shared/ui/Field";
import { StatusPill } from "../../shared/ui/StatusPill";

type Alert = { type: "success" | "error"; message: string };
type ManagementSection = "classes" | "problems" | "system";
type ImportKind = "class" | "problem";
type TestcaseVisibility = "hidden" | "visible";
type TestcaseImportFile = { name: string; content: string };

export default function TeacherManagementPage() {
  const location = useLocation();
  const [activeSection, setActiveSection] = useState<ManagementSection>("classes");

  // Auto-select tab based on URL
  useEffect(() => {
    if (location.pathname === "/teacher/problems") setActiveSection("problems");
    else if (location.pathname === "/teacher/system") setActiveSection("system");
    else setActiveSection("classes");
  }, [location.pathname]);
  const [classes, setClasses] = useState<ClassGroup[]>([]);
  const [problems, setProblems] = useState<ProblemCatalogItem[]>([]);
  const [executor, setExecutor] = useState<ExecutorStatus | null>(null);
  const [aiRouteHealth, setAiRouteHealth] = useState<AiRouteHealth | null>(null);
  const [classForm, setClassForm] = useState({ name: "", grade: "", teacherName: "" });
  const [targetClassGroupId, setTargetClassGroupId] = useState("");
  const [classImport, setClassImport] = useState({ format: "csv", content: "", fileName: "" });
  const [problemImport, setProblemImport] = useState({ format: "markdown", content: "", fileName: "" });
  const [testcaseImport, setTestcaseImport] = useState<{
    visibility: TestcaseVisibility;
    inputFiles: TestcaseImportFile[];
    answerFiles: TestcaseImportFile[];
    zipFile: TestcaseImportFile | null;
  }>({ visibility: "hidden", inputFiles: [], answerFiles: [], zipFile: null });
  const [classImportResult, setClassImportResult] = useState<ImportPreview | ImportCommit | null>(null);
  const [problemImportResult, setProblemImportResult] = useState<ImportPreview | ImportCommit | null>(null);
  const [identityAudit, setIdentityAudit] = useState<StudentIdentityAudit | null>(null);
  const [classFileName, setClassFileName] = useState("");
  const [problemFileName, setProblemFileName] = useState("");
  const [alert, setAlert] = useState<Alert | null>(null);
  const [busy, setBusy] = useState(true);
  const [dataReady, setDataReady] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [aiRouteHealthFailed, setAiRouteHealthFailed] = useState(false);

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
  const aiRouteHealthTone = statusToneForAiRoute(aiRouteHealth?.healthLevel);
  const aiRouteStateText = loadFailed
    ? "读取失败"
    : !dataReady
      ? "读取中"
      : aiRouteHealth
        ? aiRouteHealthLabel(aiRouteHealth.healthLevel)
        : "未检测";
  const systemStateText = loadFailed
    ? "读取失败"
    : !dataReady
      ? "读取中"
      : aiRouteHealthTone === "success" && executor?.cppAvailable
        ? "系统就绪"
        : "需要检查";

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
    setAiRouteHealthFailed(false);
    try {
      const [classResult, problemResult, executorResult, aiRouteHealthResult] = await Promise.all([
        api.classes(),
        api.problemCatalog(),
        api.executorStatus(),
        api.aiRouteHealth().catch(() => null)
      ]);
      setClasses(classResult);
      setProblems(problemResult);
      setExecutor(executorResult);
      setAiRouteHealth(aiRouteHealthResult);
      setAiRouteHealthFailed(!aiRouteHealthResult);
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
      fileName: data.fileName,
      testcaseImport: kind === "problem" ? testcaseImport : undefined,
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
    const format =
      extension === "xlsx"
        ? "xlsx"
        : extension === "json"
            ? "json"
            : extension === "md" || extension === "markdown"
              ? "markdown"
              : "csv";
    const content = extension === "xlsx" ? await readFileAsDataUrl(file) : await file.text();
    if (kind === "class") {
      setClassFileName(file.name);
      setClassImport({ format: format === "xlsx" ? "xlsx" : "csv", content, fileName: file.name });
    } else {
      setProblemFileName(file.name);
      setProblemImport({ format, content, fileName: file.name });
    }
  }

  async function readTestcaseFiles(kind: "input" | "answer", files: FileList | null) {
    if (!files?.length) {
      return;
    }
    const entries = await Promise.all(
      Array.from(files).map(async file => ({
        name: file.name,
        content: stripBom(await file.text())
      }))
    );
    setTestcaseImport(current => ({
      ...current,
      zipFile: null,
      [kind === "input" ? "inputFiles" : "answerFiles"]: entries
    }));
  }

  async function readTestcaseZip(file: File | null) {
    if (!file) {
      return;
    }
    const content = await readFileAsDataUrl(file);
    setTestcaseImport(current => ({
      ...current,
      inputFiles: [],
      answerFiles: [],
      zipFile: { name: file.name, content }
    }));
  }

  const testcasePairCount = useMemo(() => countMatchedTestcasePairs(testcaseImport), [testcaseImport]);

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
                    onChange={event => setClassImport({ ...classImport, content: event.target.value, fileName: "" })}
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
              <ProblemImportGuide />
              <div className="problem-import-section">
                <div className="problem-import-section__head">
                  <div>
                    <h3>题面导入</h3>
                    <p>上传 Markdown、JSON、CSV 或 XLSX；也可以直接在下方粘贴 Markdown 题面。</p>
                  </div>
                  <StatusPill tone={problemImport.fileName ? "success" : "neutral"}>{problemImport.fileName || "未选择题面文件"}</StatusPill>
                </div>
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
                  onChange={event => setProblemImport({ ...problemImport, content: event.target.value, fileName: "" })}
                  placeholder={"# 两数求和\n\n## 题目描述\n给定两个整数，输出它们的和。\n\n## 输入格式\n一行两个整数 a b。\n\n## 输出格式\n输出 a + b。\n\n## 样例输入\n```text\n1 2\n```\n\n## 样例输出\n```text\n3\n```"}
                />
              </Field>
              <TestcaseImportPanel
                state={testcaseImport}
                pairCount={testcasePairCount}
                onVisibilityChange={visibility => setTestcaseImport(current => ({ ...current, visibility }))}
                onInputFiles={readTestcaseFiles}
                onAnswerFiles={readTestcaseFiles}
                onZipFile={readTestcaseZip}
                onClear={() => setTestcaseImport({ visibility: "hidden", inputFiles: [], answerFiles: [], zipFile: null })}
              />
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
            <section className="management-system-stack">
              {!executor ? (
                <div className="management-task-card management-task-card--large">
                  <EmptyState title="正在检测执行环境" />
                </div>
              ) : (
                <div className="management-task-card management-task-card--large">
                  <div className="management-status-grid">
                    <StatusItem label="模式" value={executor.mode} />
                    <StatusItem label="Docker" value={executor.dockerAvailable ? "可用" : "未配置"} ready={executor.dockerAvailable} />
                    <StatusItem label="Python" value={executor.pythonAvailable ? "可用" : "未就绪"} ready={executor.pythonAvailable} />
                    <StatusItem label="C++" value={executor.cppAvailable ? "可用" : "未就绪"} ready={executor.cppAvailable} />
                    <StatusPill tone={executor.cppAvailable ? "success" : "warning"}>{executor.message}</StatusPill>
                  </div>
                </div>
              )}
              <div className="management-task-card management-task-card--large">
                {!aiRouteHealth ? (
                  <EmptyState
                    title={aiRouteHealthFailed ? "AI 路由未检测" : "正在检测 AI 路由"}
                    description={aiRouteHealthFailed ? "路由健康接口暂时不可用；班级、题目和评测环境数据不受影响。" : undefined}
                  />
                ) : (
                  <div className="management-ai-route">
                    <div className="management-ai-route__head">
                      <div>
                        <h2>外部 AI 路由</h2>
                        <p>{aiRouteHealth.summary || "当前没有路由摘要。"}</p>
                      </div>
                      <StatusPill tone={aiRouteHealthTone}>{aiRouteStateText}</StatusPill>
                    </div>
                    <div className="management-status-grid">
                      <StatusItem label="可用路由" value={String(aiRouteHealth.usableRouteCount)} ready={aiRouteHealth.usableRouteCount > 1} icon="route" />
                      <StatusItem label="备用路由" value={aiRouteHealth.fallbackConfigured ? "已配置" : "未配置"} ready={aiRouteHealth.fallbackConfigured} />
                      <StatusItem label="路由池" value={aiRouteHealth.routePoolConfigured ? "已配置" : "未配置"} ready={aiRouteHealth.routePoolConfigured} />
                      <StatusItem label="AI 开关" value={aiRouteHealth.enabled ? "已启用" : "未启用"} ready={aiRouteHealth.enabled} />
                    </div>
                    {!!aiRouteHealth.suggestions?.length && (
                      <ul className="management-ai-route__suggestions">
                        {aiRouteHealth.suggestions.slice(0, 3).map(item => (
                          <li key={item}>{item}</li>
                        ))}
                      </ul>
                    )}
                    <div className="management-ai-route__routes">
                      {aiRouteHealth.routes.map((route, index) => (
                        <div key={`${route.role}-${route.provider || index}`}>
                          <span>{routeRoleLabel(route.role)}</span>
                          <strong>{displayText(route.provider, "未命名路由")}</strong>
                          <small>{displayText(route.model, route.configured ? "模型未标注" : missingFieldsText(route.missingFields))}</small>
                          <StatusPill tone={route.configured ? "success" : "warning"}>{route.configured ? "可用" : "缺配置"}</StatusPill>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </section>
          )}
        </main>
      </section>
    </div>
  );
}

function ProblemImportGuide() {
  return (
    <div className="problem-import-guide">
      <div className="problem-import-guide__head">
        <div>
          <span className="eyebrow">Problem Import</span>
          <h2>题目与测试数据导入说明</h2>
        </div>
        <StatusPill tone="info">先预览，再导入</StatusPill>
      </div>

      <div className="problem-import-modes" aria-label="题目导入格式说明">
        <article>
          <FileArchive size={18} />
          <div>
            <strong>ZIP 题包</strong>
            <p>推荐用于完整导入。压缩包内放题面文件和同名测试数据，系统会自动配对。</p>
          </div>
        </article>
        <article>
          <FileText size={18} />
          <div>
            <strong>Markdown 题面</strong>
            <p>适合单题快速导入。题面中的第一组样例输入/输出会作为公开测试点。</p>
          </div>
        </article>
        <article>
          <ListChecks size={18} />
          <div>
            <strong>JSON / CSV / XLSX</strong>
            <p>适合批量导入题面。字段包含标题、描述、难度、限制和样例输入输出。</p>
          </div>
        </article>
      </div>

      <div className="problem-import-spec-grid">
        <section>
          <h3>ZIP 数据点结构</h3>
          <p>输入文件使用 <code>.in</code>，答案文件使用 <code>.ans</code> 或 <code>.out</code>，两者去掉后缀后的路径必须一致。</p>
          <pre>{`problem.md
tests/1.in
tests/1.ans
tests/2.in
tests/2.out`}</pre>
          <p>导入时第一组配对数据会作为公开样例，其余配对数据默认作为隐藏测试点。</p>
        </section>

        <section>
          <h3>Markdown 样例规则</h3>
          <p>题面建议包含标题、题目描述、输入格式、输出格式、样例输入和样例输出。样例内容请放在代码块中。</p>
          <pre>{`# 两数求和

## 样例输入
\`\`\`text
1 2
\`\`\`

## 样例输出
\`\`\`text
3
\`\`\``}</pre>
        </section>
      </div>
    </div>
  );
}

function TestcaseImportPanel({
  state,
  pairCount,
  onVisibilityChange,
  onInputFiles,
  onAnswerFiles,
  onZipFile,
  onClear
}: {
  state: {
    visibility: TestcaseVisibility;
    inputFiles: TestcaseImportFile[];
    answerFiles: TestcaseImportFile[];
    zipFile: TestcaseImportFile | null;
  };
  pairCount: number;
  onVisibilityChange: (visibility: TestcaseVisibility) => void;
  onInputFiles: (kind: "input", files: FileList | null) => void | Promise<void>;
  onAnswerFiles: (kind: "answer", files: FileList | null) => void | Promise<void>;
  onZipFile: (file: File | null) => void | Promise<void>;
  onClear: () => void;
}) {
  const usingZip = Boolean(state.zipFile);
  const hasFiles = usingZip || state.inputFiles.length > 0 || state.answerFiles.length > 0;
  return (
    <section className="testcase-import-panel" aria-label="测试点配置">
      <div className="testcase-import-panel__head">
        <div>
          <h3>测试点配置</h3>
          <p>与旧版创建题目页一致：输入文件和答案文件按同名配对，也可以直接上传 ZIP 数据包。</p>
        </div>
        <StatusPill tone={pairCount > 0 || usingZip ? "success" : "neutral"}>
          {usingZip ? "ZIP 待解析" : `${pairCount} 组已配对`}
        </StatusPill>
      </div>

      <div className="testcase-import-controls">
        <label className="field management-file-picker">
          <span>输入文件</span>
          <span className="management-file-picker__box">
            <UploadCloud size={18} />
            <strong>{state.inputFiles.length ? `${state.inputFiles.length} 个输入文件` : "选择 .in / .txt"}</strong>
            <small>可多选，文件名需与答案文件对应</small>
          </span>
          <input type="file" accept=".in,.txt,text/plain" multiple onChange={event => void onInputFiles("input", event.target.files)} />
        </label>

        <label className="field management-file-picker">
          <span>答案文件</span>
          <span className="management-file-picker__box">
            <UploadCloud size={18} />
            <strong>{state.answerFiles.length ? `${state.answerFiles.length} 个答案文件` : "选择 .ans / .out / .txt"}</strong>
            <small>去掉后缀后与输入文件同名即可配对</small>
          </span>
          <input type="file" accept=".ans,.out,.txt,text/plain" multiple onChange={event => void onAnswerFiles("answer", event.target.files)} />
        </label>

        <Field label="导入后类型">
          <Select value={state.visibility} onChange={event => onVisibilityChange(event.target.value as TestcaseVisibility)}>
            <option value="hidden">作为隐藏测试</option>
            <option value="visible">作为公开样例</option>
          </Select>
        </Field>
      </div>

      <div className="testcase-import-zip-row">
        <label className="field management-file-picker">
          <span>ZIP 数据包</span>
          <span className="management-file-picker__box">
            <FileArchive size={18} />
            <strong>{state.zipFile?.name || "选择 ZIP 压缩包"}</strong>
            <small>ZIP 中包含 .in 与 .ans/.out，系统会自动配对</small>
          </span>
          <input type="file" accept=".zip,application/zip,application/x-zip-compressed" onChange={event => void onZipFile(event.target.files?.[0] || null)} />
        </label>
        <Button type="button" variant="ghost" onClick={onClear} disabled={!hasFiles}>
          清空数据点
        </Button>
      </div>

      <div className="testcase-import-summary">
        <StatusPill tone={state.inputFiles.length ? "info" : "neutral"}>输入 {state.inputFiles.length}</StatusPill>
        <StatusPill tone={state.answerFiles.length ? "info" : "neutral"}>答案 {state.answerFiles.length}</StatusPill>
        <StatusPill tone={pairCount ? "success" : "neutral"}>配对 {pairCount}</StatusPill>
        <StatusPill tone={state.visibility === "visible" ? "warning" : "neutral"}>
          {state.visibility === "visible" ? "导入为公开样例" : "导入为隐藏测试"}
        </StatusPill>
      </div>

      <p className="testcase-import-note">文件示例：<code>1.in</code> 对应 <code>1.ans</code>，<code>tests/case2.in</code> 对应 <code>tests/case2.out</code>。</p>
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

function StatusItem({ label, value, ready = true, icon = "status" }: { label: string; value: string; ready?: boolean; icon?: "status" | "route" }) {
  return (
    <div className="management-status-item">
      {icon === "route" ? <Route size={18} /> : ready ? <CheckCircle2 size={18} /> : <Database size={18} />}
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function statusToneForAiRoute(level?: AiRouteHealthLevel | null) {
  if (level === "MULTI_ROUTE_READY") {
    return "success";
  }
  if (level === "SINGLE_ROUTE_RISK") {
    return "warning";
  }
  if (level === "NO_ROUTE" || level === "DISABLED") {
    return "danger";
  }
  return "neutral";
}

function aiRouteHealthLabel(level?: AiRouteHealthLevel | null) {
  switch (level) {
    case "MULTI_ROUTE_READY":
      return "多路由就绪";
    case "SINGLE_ROUTE_RISK":
      return "单路由风险";
    case "NO_ROUTE":
      return "无可用路由";
    case "DISABLED":
      return "AI 未启用";
    default:
      return "未检测";
  }
}

function routeRoleLabel(role?: string | null) {
  switch (role) {
    case "PRIMARY":
      return "主路由";
    case "FALLBACK":
      return "备用";
    case "ROUTE_POOL":
      return "路由池";
    default:
      return displayText(role, "路由");
  }
}

function missingFieldsText(fields?: string[]) {
  return fields?.length ? `缺少 ${fields.join(", ")}` : "缺少配置";
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.addEventListener("load", () => resolve(String(reader.result || "")));
    reader.addEventListener("error", () => reject(reader.error || new Error("文件读取失败")));
    reader.readAsDataURL(file);
  });
}

function countMatchedTestcasePairs(state: {
  inputFiles: TestcaseImportFile[];
  answerFiles: TestcaseImportFile[];
  zipFile: TestcaseImportFile | null;
}) {
  if (state.zipFile) {
    return 0;
  }
  const answerStems = new Set(state.answerFiles.map(file => stripExtension(file.name)));
  return state.inputFiles.filter(file => answerStems.has(stripExtension(file.name))).length;
}

function stripExtension(fileName: string) {
  return fileName.replace(/\\/g, "/").replace(/\.[^.]+$/, "");
}

function stripBom(text: string) {
  return String(text || "").replace(/^\uFEFF/, "");
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

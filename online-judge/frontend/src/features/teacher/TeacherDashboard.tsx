import { useEffect, useMemo, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { BookOpen, Plus, RefreshCw, ServerCog, UsersRound } from "lucide-react";
import { api } from "../../shared/api/client";
import type { Assignment, ClassGroup, ProblemCatalogItem } from "../../shared/api/types";
import { assignmentStatusLabel, displayText, hintPolicyLabel } from "../../shared/format";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Select, TextArea, TextInput, Field } from "../../shared/ui/Field";
import { Panel } from "../../shared/ui/Panel";
import { StatusPill } from "../../shared/ui/StatusPill";

export default function TeacherDashboard() {
  const navigate = useNavigate();
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [classes, setClasses] = useState<ClassGroup[]>([]);
  const [problems, setProblems] = useState<ProblemCatalogItem[]>([]);
  const [busy, setBusy] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [newForm, setNewForm] = useState({ title: "", problemIds: [] as number[], classGroupId: "", hintPolicy: "L2" });

  useEffect(() => {
    Promise.all([api.assignments(), api.classes(), api.problemCatalog()])
      .then(([a, c, p]) => { setAssignments(a); setClasses(c); setProblems(p); })
      .catch(() => {})
      .finally(() => setBusy(false));
  }, []);

  const kpi = useMemo(() => ({
    active: assignments.filter(a => a.status === "ACTIVE").length,
    total: assignments.length,
    participantCount: 0,
    attentionCount: 0
  }), [assignments]);

  const cleanClasses = useMemo(() =>
    classes.map(c => ({ ...c, name: displayText(c.name, `班级 #${c.id}`) })), [classes]
  );

  async function createAssignment() {
    if (!newForm.title.trim() || newForm.problemIds.length === 0) return;
    setBusy(true);
    try {
      await api.createAssignment({
        title: newForm.title.trim(),
        description: "",
        classGroupId: newForm.classGroupId ? Number(newForm.classGroupId) : null,
        hintPolicy: newForm.hintPolicy,
        status: "ACTIVE",
        problemIds: newForm.problemIds
      });
      setNewForm({ title: "", problemIds: [], classGroupId: "", hintPolicy: "L2" });
      setShowCreate(false);
      const [a] = await Promise.all([api.assignments()]);
      setAssignments(a);
    } catch (e) {} finally { setBusy(false); }
  }

  return (
    <div className="stack teacher-dashboard-page">
      <section className="teacher-dashboard-hero">
        <div>
          <h1>教师工作台</h1>
          <nav className="teacher-mode-tabs" aria-label="教师功能">
            <span className="teacher-mode-tab is-active">总览</span>
            <ButtonLink to="/teacher/classes" variant="ghost" icon={<UsersRound size={16} />}>班级管理</ButtonLink>
            <ButtonLink to="/teacher/problems" variant="ghost" icon={<BookOpen size={16} />}>题目管理</ButtonLink>
            <ButtonLink to="/teacher/system" variant="ghost" icon={<ServerCog size={16} />}>系统状态</ButtonLink>
          </nav>
        </div>
        <div className="actions">
          <Button type="button" variant="primary" icon={<Plus size={17} />} onClick={() => setShowCreate(true)}>
            创建新作业
          </Button>
        </div>
      </section>

      <div className="teacher-kpi-strip">
        <div><span>活跃作业</span><strong>{kpi.active}</strong></div>
        <div><span>总作业</span><strong>{kpi.total}</strong></div>
        <div><span>参与学生</span><strong>{kpi.participantCount}</strong></div>
        <div><span>需关注</span><strong>{kpi.attentionCount}</strong></div>
      </div>

      {busy ? (
        <EmptyState title="加载中" />
      ) : assignments.length === 0 ? (
        <EmptyState title="还没有创建作业" description="点击上方按钮开始" />
      ) : (
        <div className="teacher-assignment-list">
          {assignments.map(item => (
            <button
              type="button"
              className="teacher-assignment-item"
              key={item.id}
              onClick={() => navigate(`/teacher/assignment/${item.id}`)}
            >
              <div>
                <div className="actions">
                  <StatusPill tone={item.status === "ACTIVE" ? "success" : "neutral"}>{assignmentStatusLabel(item.status)}</StatusPill>
                  <span className="meta-badge">{hintPolicyLabel(item.hintPolicy)}</span>
                </div>
                <h3>{item.title}</h3>
                <div className="teacher-assignment-meta-line">
                  <span>{displayText(item.className, "未绑定班级")}</span>
                  <span>{item.tasks?.length || 0} 个题目</span>
                </div>
              </div>
              <span className="teacher-assignment-code">邀请码 {item.inviteCode || "-"}</span>
            </button>
          ))}
        </div>
      )}

      {showCreate && (
        <div className="teacher-create-panel">
          <Panel title="创建新作业">
            <div className="form-grid">
              <Field label="作业标题">
                <TextInput value={newForm.title} onChange={e => setNewForm({ ...newForm, title: e.target.value })} placeholder="例如 Python 分支练习" />
              </Field>
              <Field label="班级标签（可选）">
                <Select value={newForm.classGroupId} onChange={e => setNewForm({ ...newForm, classGroupId: e.target.value })}>
                  <option value="">不绑定</option>
                  {cleanClasses.map(c => <option value={c.id} key={c.id}>{c.name}</option>)}
                </Select>
              </Field>
              <Field label="提示层级">
                <Select value={newForm.hintPolicy} onChange={e => setNewForm({ ...newForm, hintPolicy: e.target.value })}>
                  <option value="L1">L1 问题类型</option>
                  <option value="L2">L2 定位方向</option>
                  <option value="L3">L3 局部提示</option>
                  <option value="L4">L4 参考改法</option>
                </Select>
              </Field>
            </div>
            <Field label="选择题目">
              <div className="teacher-problem-pick-list">
                {problems.map(p => (
                  <label className="list-row" key={p.id}>
                    <input type="checkbox" checked={newForm.problemIds.includes(p.id)} onChange={() => setNewForm({ ...newForm, problemIds: newForm.problemIds.includes(p.id) ? newForm.problemIds.filter(id => id !== p.id) : [...newForm.problemIds, p.id] })} />
                    <h3>{p.title}</h3>
                  </label>
                ))}
              </div>
            </Field>
            <div className="actions">
              <Button type="button" variant="primary" onClick={createAssignment} disabled={busy}>保存作业</Button>
              <Button type="button" variant="ghost" onClick={() => setShowCreate(false)}>取消</Button>
            </div>
          </Panel>
        </div>
      )}
    </div>
  );
}
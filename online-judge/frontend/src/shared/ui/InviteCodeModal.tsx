import { useState } from "react";
import { KeyRound, UserRound, X } from "lucide-react";
import { api } from "../api/client";
import { loadInviteCode, loadStudent, saveInviteCode, saveStudent } from "../storage";
import { Button } from "./Button";
import { TextInput } from "./Field";

interface InviteCodeModalProps {
  onClose: () => void;
}

interface JoinedAssignment {
  id: number;
  title: string;
  inviteCode: string;
  studentProfileId: number;
  displayName: string;
}

function loadJoinedAssignments(): JoinedAssignment[] {
  try {
    const raw = localStorage.getItem("wzai:joinedAssignments");
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveJoinedAssignments(list: JoinedAssignment[]) {
  const keyed = new Map<number, JoinedAssignment>();
  for (const item of list) {
    keyed.set(item.id, item);
  }
  localStorage.setItem("wzai:joinedAssignments", JSON.stringify(Array.from(keyed.values())));
}

export default function InviteCodeModal({ onClose }: InviteCodeModalProps) {
  const [code, setCode] = useState(() => loadInviteCode() || "");
  const [name, setName] = useState(() => {
    const joined = loadJoinedAssignments();
    return joined[0]?.displayName || "";
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const trimmedCode = code.trim().toUpperCase();
    const trimmedName = name.trim();

    if (!trimmedCode) {
      setError("请输入邀请码");
      return;
    }
    if (!trimmedName) {
      setError("请输入姓名");
      return;
    }

    setError("");
    setBusy(true);
    try {
      const assignment = await api.resolveInvite(trimmedCode);
      const student = await api.bindStudent({
        assignmentId: assignment.id,
        classGroupId: assignment.classGroupId,
        className: assignment.className || "",
        displayName: trimmedName,
        studentNo: ""
      });

      saveInviteCode(trimmedCode);
      saveStudent(assignment.id, student);

      const joined = loadJoinedAssignments();
      const updated: JoinedAssignment[] = [
        { id: assignment.id, title: assignment.title, inviteCode: trimmedCode, studentProfileId: student.id, displayName: trimmedName },
        ...joined.filter(j => j.id !== assignment.id)
      ];
      saveJoinedAssignments(updated);

      setSuccess("已加入 " + (assignment.title || "作业"));
      setTimeout(() => onClose(), 1200);
    } catch (err) {
      setError(err instanceof Error ? err.message : "邀请码无效，请检查后重试");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-panel" onClick={e => e.stopPropagation()}>
        <div className="modal-panel__head">
          <div>
            <span className="eyebrow">加入课堂作业</span>
            <h2>输入邀请码</h2>
          </div>
          <button type="button" className="modal-panel__close" onClick={onClose} aria-label="关闭">
            <X size={20} />
          </button>
        </div>
        <form className="modal-panel__body stack" onSubmit={handleSubmit}>
          {error && <div className="alert alert--error">{error}</div>}
          {success && <div className="alert alert--success">{success}</div>}
          <div className="form-grid">
            <label className="field">
              <span>邀请码</span>
              <TextInput
                value={code}
                onChange={e => { setCode(e.target.value); setError(""); }}
                placeholder="例如 WZAI01"
                autoComplete="off"
                autoFocus
              />
            </label>
            <label className="field">
              <span>姓名</span>
              <TextInput
                value={name}
                onChange={e => { setName(e.target.value); setError(""); }}
                placeholder="请输入姓名"
              />
            </label>
          </div>
          <div className="actions">
            <Button type="submit" variant="primary" disabled={busy || !!success} icon={<KeyRound size={17} />}>
              {busy ? "加入中" : "加入作业"}
            </Button>
            <Button type="button" variant="ghost" onClick={onClose}>
              取消
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
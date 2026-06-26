import { FormEvent, ReactNode, useEffect, useState } from "react";
import { LockKeyhole, LogIn } from "lucide-react";
import { api } from "../../shared/api/client";
import { Button } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, TextInput } from "../../shared/ui/Field";

type Props = {
  children: ReactNode;
};

const TEACHER_DEV_AUTO_ENTER = import.meta.env.DEV && import.meta.env.VITE_TEACHER_DEV_AUTO_ENTER !== "false";

export default function TeacherAuthGate({ children }: Props) {
  const [authenticated, setAuthenticated] = useState<boolean | null>(null);
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let ignore = false;
    api.teacherSession()
      .then(result => {
        if (!ignore) {
          setAuthenticated(TEACHER_DEV_AUTO_ENTER || result.authenticated);
        }
      })
      .catch(() => {
        if (!ignore) {
          setAuthenticated(TEACHER_DEV_AUTO_ENTER);
        }
      });
    return () => {
      ignore = true;
    };
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!password.trim()) {
      setError("请输入教师口令。");
      return;
    }
    setBusy(true);
    setError("");
    try {
      const result = await api.teacherLogin(password);
      setAuthenticated(result.authenticated);
      if (!result.authenticated) {
        setError("教师口令不正确。");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "登录失败。");
      setAuthenticated(false);
    } finally {
      setBusy(false);
    }
  }

  if (authenticated === null) {
    return <EmptyState title="正在确认教师身份" live />;
  }

  if (authenticated) {
    return <>{children}</>;
  }

  return (
    <div className="teacher-auth-page">
      <form className="teacher-auth-panel" onSubmit={event => void submit(event)}>
        <span className="teacher-auth-panel__icon">
          <LockKeyhole size={22} />
        </span>
        <div>
          <p className="eyebrow">教师端</p>
          <h1>输入教师口令</h1>
        </div>
        {error && <div className="alert alert--error">{error}</div>}
        <Field label="教师口令">
          <TextInput
            type="password"
            value={password}
            onChange={event => setPassword(event.target.value)}
            autoComplete="current-password"
            placeholder="由学校管理员配置"
          />
        </Field>
        <Button type="submit" variant="primary" icon={<LogIn size={17} />} disabled={busy}>
          {busy ? "登录中" : "进入教师端"}
        </Button>
      </form>
    </div>
  );
}

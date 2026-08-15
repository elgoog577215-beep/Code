import { FormEvent, ReactNode, useEffect, useState } from "react";
import { KeyRound, LockKeyhole, LogIn, UserPlus } from "lucide-react";
import { api } from "../../shared/api/client";
import type { AuthSession } from "../../shared/api/types";
import { useTranslation } from "../../shared/i18n";
import { Button } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, TextInput } from "../../shared/ui/Field";

type Props = { children: ReactNode };
type Mode = "login" | "register";

const ANONYMOUS: AuthSession = { authenticated: false, mustChangePassword: false };

export default function TeacherAuthGate({ children }: Props) {
  const { t } = useTranslation();
  const [session, setSession] = useState<AuthSession | null>(null);
  const [mode, setMode] = useState<Mode>("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [schoolName, setSchoolName] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<{ tone: "error" | "success"; text: string } | null>(null);

  useEffect(() => {
    let ignore = false;
    api.teacherSession()
      .then(result => { if (!ignore) setSession(result); })
      .catch(() => { if (!ignore) setSession(ANONYMOUS); });
    return () => { ignore = true; };
  }, []);

  async function submitLogin(event: FormEvent) {
    event.preventDefault();
    if (!username.trim() || !password) {
      setNotice({ tone: "error", text: t("teacherAuth.validation.loginRequired") });
      return;
    }
    setBusy(true);
    setNotice(null);
    try {
      setSession(await api.teacherLogin(username.trim(), password));
    } catch (error) {
      setNotice({ tone: "error", text: error instanceof Error ? error.message : t("teacherAuth.loginFailed") });
      setSession(ANONYMOUS);
    } finally {
      setBusy(false);
    }
  }

  async function submitRegistration(event: FormEvent) {
    event.preventDefault();
    if (!username.trim() || password.length < 10 || !displayName.trim() || !schoolName.trim()) {
      setNotice({ tone: "error", text: t("teacherAuth.validation.registerRequired") });
      return;
    }
    setBusy(true);
    setNotice(null);
    try {
      await api.teacherRegister({ username: username.trim(), password, displayName: displayName.trim(), schoolName: schoolName.trim() });
      setMode("login");
      setPassword("");
      setNotice({ tone: "success", text: t("teacherAuth.registrationPending") });
    } catch (error) {
      setNotice({ tone: "error", text: error instanceof Error ? error.message : t("teacherAuth.registerFailed") });
    } finally {
      setBusy(false);
    }
  }

  async function changePassword(event: FormEvent) {
    event.preventDefault();
    if (!password || newPassword.length < 10) {
      setNotice({ tone: "error", text: t("teacherAuth.validation.passwordChange") });
      return;
    }
    setBusy(true);
    setNotice(null);
    try {
      await api.teacherChangePassword(password, newPassword);
      setSession(ANONYMOUS);
      setPassword("");
      setNewPassword("");
      setNotice({ tone: "success", text: t("teacherAuth.passwordChanged") });
    } catch (error) {
      setNotice({ tone: "error", text: error instanceof Error ? error.message : t("teacherAuth.passwordChangeFailed") });
    } finally {
      setBusy(false);
    }
  }

  if (session === null) return <EmptyState title={t("teacherAuth.checking")} live />;
  if (session.authenticated && !session.mustChangePassword) return <>{children}</>;

  const changingPassword = session.authenticated && session.mustChangePassword;
  return (
    <div className="teacher-auth-page">
      <form className="teacher-auth-panel" onSubmit={changingPassword ? changePassword : mode === "login" ? submitLogin : submitRegistration}>
        <span className="teacher-auth-panel__icon">
          {changingPassword ? <KeyRound size={22} /> : mode === "login" ? <LockKeyhole size={22} /> : <UserPlus size={22} />}
        </span>
        <div>
          <p className="eyebrow">{t("teacherAuth.eyebrow")}</p>
          <h1>{t(changingPassword ? "teacherAuth.changeTitle" : mode === "login" ? "teacherAuth.loginTitle" : "teacherAuth.registerTitle")}</h1>
          <p>{t(changingPassword ? "teacherAuth.changeDescription" : mode === "login" ? "teacherAuth.loginDescription" : "teacherAuth.registerDescription")}</p>
        </div>
        {notice && <div className={`alert alert--${notice.tone}`}>{notice.text}</div>}
        {!changingPassword && (
          <Field label={t("teacherAuth.username")}>
            <TextInput value={username} onChange={event => setUsername(event.target.value)} autoComplete="username" />
          </Field>
        )}
        {mode === "register" && !changingPassword && (
          <>
            <Field label={t("teacherAuth.displayName")}>
              <TextInput value={displayName} onChange={event => setDisplayName(event.target.value)} autoComplete="name" />
            </Field>
            <Field label={t("teacherAuth.schoolName")}>
              <TextInput value={schoolName} onChange={event => setSchoolName(event.target.value)} autoComplete="organization" />
            </Field>
          </>
        )}
        <Field label={t(changingPassword ? "teacherAuth.currentPassword" : "teacherAuth.password")}>
          <TextInput type="password" value={password} onChange={event => setPassword(event.target.value)} autoComplete={changingPassword ? "current-password" : mode === "login" ? "current-password" : "new-password"} />
        </Field>
        {changingPassword && (
          <Field label={t("teacherAuth.newPassword")}>
            <TextInput type="password" value={newPassword} onChange={event => setNewPassword(event.target.value)} autoComplete="new-password" />
          </Field>
        )}
        <Button type="submit" variant="primary" icon={mode === "register" && !changingPassword ? <UserPlus size={17} /> : <LogIn size={17} />} disabled={busy}>
          {busy ? t("teacherAuth.processing") : t(changingPassword ? "teacherAuth.changeAction" : mode === "login" ? "teacherAuth.loginAction" : "teacherAuth.registerAction")}
        </Button>
        {!changingPassword && (
          <Button type="button" variant="ghost" onClick={() => { setMode(mode === "login" ? "register" : "login"); setNotice(null); }}>
            {t(mode === "login" ? "teacherAuth.toRegister" : "teacherAuth.toLogin")}
          </Button>
        )}
      </form>
    </div>
  );
}

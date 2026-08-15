import { FormEvent, ReactNode, useEffect, useState } from "react";
import { KeyRound, LockKeyhole, LogIn, UserPlus } from "lucide-react";
import { api } from "../../shared/api/client";
import type { AuthSession } from "../../shared/api/types";
import { useTranslation } from "../../shared/i18n";
import { Button } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, TextInput } from "../../shared/ui/Field";

export type Portal = "TEACHER" | "SCHOOL_ADMIN" | "PLATFORM_ADMIN";
type Props = { portal: Portal; children: ReactNode; allowTeacherRegistration?: boolean };
const anonymous: AuthSession = { authenticated: false, mustChangePassword: false };

export default function PortalAuthGate({ portal, children, allowTeacherRegistration = false }: Props) {
  const { t } = useTranslation();
  const [session, setSession] = useState<AuthSession | null>(null);
  const [registering, setRegistering] = useState(false);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [schoolCode, setSchoolCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<{ tone: "error" | "success"; text: string } | null>(null);

  useEffect(() => {
    let alive = true;
    api.accountSession().then(value => alive && setSession(value)).catch(() => alive && setSession(anonymous));
    return () => { alive = false; };
  }, []);

  async function login(event: FormEvent) {
    event.preventDefault(); setBusy(true); setNotice(null);
    try { setSession(await api.accountLogin(username.trim(), password, portal)); }
    catch (error) { setSession(anonymous); setNotice({ tone: "error", text: message(error, t("portals.loginFailed")) }); }
    finally { setBusy(false); }
  }
  async function register(event: FormEvent) {
    event.preventDefault(); setBusy(true); setNotice(null);
    try {
      await api.teacherRegister({ username: username.trim(), password, displayName: displayName.trim(), schoolRegistrationCode: schoolCode.trim() });
      setRegistering(false); setPassword(""); setNotice({ tone: "success", text: t("portals.registrationPending") });
    } catch (error) { setNotice({ tone: "error", text: message(error, t("portals.registerFailed")) }); }
    finally { setBusy(false); }
  }
  async function changePassword(event: FormEvent) {
    event.preventDefault(); setBusy(true); setNotice(null);
    try {
      await api.accountChangePassword(password, newPassword); setSession(anonymous); setPassword(""); setNewPassword("");
      setNotice({ tone: "success", text: t("portals.passwordChanged") });
    } catch (error) { setNotice({ tone: "error", text: message(error, t("portals.passwordFailed")) }); }
    finally { setBusy(false); }
  }

  if (session === null) return <EmptyState title={t("portals.checking")} live />;
  if (session.authenticated && session.role === portal && !session.mustChangePassword) return <>{children}</>;
  const changing = session.authenticated && session.role === portal && session.mustChangePassword;
  const wrongPortal = session.authenticated && session.role !== portal;
  const titleKey = portal === "PLATFORM_ADMIN" ? "platform" : portal === "SCHOOL_ADMIN" ? "school" : "teacher";
  return (
    <div className="teacher-auth-page portal-auth-page">
      <form className="teacher-auth-panel" onSubmit={changing ? changePassword : registering ? register : login}>
        <span className="teacher-auth-panel__icon">{changing ? <KeyRound size={22} /> : registering ? <UserPlus size={22} /> : <LockKeyhole size={22} />}</span>
        <div><p className="eyebrow">{t(`portals.${titleKey}.eyebrow`)}</p><h1>{t(changing ? "portals.changePassword" : `portals.${titleKey}.title`)}</h1><p>{t(changing ? "portals.changeHint" : `portals.${titleKey}.description`)}</p></div>
        {notice && <div className={`alert alert--${notice.tone}`}>{notice.text}</div>}
        {wrongPortal && <div className="alert alert--warning">{t("portals.roleMismatch")}<Button type="button" variant="ghost" onClick={() => void api.accountLogout().then(() => setSession(anonymous))}>{t("common.logout")}</Button></div>}
        {!changing && !wrongPortal && <Field label={t("teacherAuth.username")}><TextInput value={username} onChange={event => setUsername(event.target.value)} autoComplete="username" /></Field>}
        {registering && !changing && !wrongPortal ? <>
          <Field label={t("teacherAuth.displayName")}><TextInput value={displayName} onChange={event => setDisplayName(event.target.value)} autoComplete="name" /></Field>
          <Field label={t("portals.schoolCode")}><TextInput value={schoolCode} onChange={event => setSchoolCode(event.target.value)} autoComplete="off" /></Field>
        </> : null}
        {!wrongPortal && <Field label={t(changing ? "teacherAuth.currentPassword" : "teacherAuth.password")}><TextInput type="password" value={password} onChange={event => setPassword(event.target.value)} autoComplete={changing ? "current-password" : registering ? "new-password" : "current-password"} /></Field>}
        {changing && <Field label={t("teacherAuth.newPassword")}><TextInput type="password" value={newPassword} onChange={event => setNewPassword(event.target.value)} autoComplete="new-password" /></Field>}
        {!wrongPortal && <Button type="submit" variant="primary" icon={<LogIn size={17} />} disabled={busy}>{busy ? t("teacherAuth.processing") : t(changing ? "teacherAuth.changeAction" : registering ? "teacherAuth.registerAction" : "teacherAuth.loginAction")}</Button>}
        {allowTeacherRegistration && !changing && !wrongPortal && <Button type="button" variant="ghost" onClick={() => { setRegistering(value => !value); setNotice(null); }}>{t(registering ? "teacherAuth.toLogin" : "teacherAuth.toRegister")}</Button>}
      </form>
    </div>
  );
}

function message(error: unknown, fallback: string) { return error instanceof Error ? error.message : fallback; }

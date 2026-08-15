import { useEffect, useState } from "react";
import { CheckCircle2, KeyRound, RefreshCw, ShieldCheck, XCircle } from "lucide-react";
import { api } from "../../shared/api/client";
import type { ProblemManage, TeacherAccount } from "../../shared/api/types";
import { useTranslation } from "../../shared/i18n";
import { Button } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextInput } from "../../shared/ui/Field";
import { Panel } from "../../shared/ui/Panel";
import { StatusPill } from "../../shared/ui/StatusPill";

type AccountStatus = TeacherAccount["status"];

export default function AdminConsolePage() {
  const { t } = useTranslation();
  const [status, setStatus] = useState<AccountStatus>("PENDING");
  const [accounts, setAccounts] = useState<TeacherAccount[]>([]);
  const [reviews, setReviews] = useState<ProblemManage[]>([]);
  const [reason, setReason] = useState("");
  const [baseUnits, setBaseUnits] = useState("500");
  const [additionalUnits, setAdditionalUnits] = useState("0");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [temporaryPassword, setTemporaryPassword] = useState("");

  async function load() {
    setBusy(true);
    try {
      const [accountResult, reviewResult] = await Promise.all([api.adminTeacherAccounts(status), api.adminProblemReviews()]);
      setAccounts(accountResult);
      setReviews(reviewResult);
    } catch (error) {
      setNotice(error instanceof Error ? error.message : t("adminConsole.loadFailed"));
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => { void load(); }, [status]);

  async function run(action: () => Promise<unknown>, success: string) {
    setBusy(true);
    setNotice("");
    try {
      await action();
      setNotice(success);
      await load();
    } catch (error) {
      setNotice(error instanceof Error ? error.message : t("adminConsole.actionFailed"));
    } finally {
      setBusy(false);
    }
  }

  async function resetPassword(id: string) {
    setBusy(true);
    setTemporaryPassword("");
    try {
      const result = await api.resetTeacherPassword(id);
      setTemporaryPassword(result.temporaryPassword);
      setNotice(t("adminConsole.passwordReset"));
    } catch (error) {
      setNotice(error instanceof Error ? error.message : t("adminConsole.actionFailed"));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="teacher-page teacher-workflow">
      <section className="teacher-workflow-header">
        <div><p className="eyebrow">{t("adminConsole.eyebrow")}</p><h1>{t("adminConsole.title")}</h1><p>{t("adminConsole.description")}</p></div>
        <Button type="button" variant="secondary" icon={<RefreshCw size={16} />} onClick={() => void load()} disabled={busy}>{t("adminConsole.refresh")}</Button>
      </section>
      {notice && <div className="alert alert--success">{notice}</div>}
      {temporaryPassword && <div className="alert alert--warning"><strong>{t("adminConsole.temporaryPassword")}</strong> <code>{temporaryPassword}</code></div>}
      <div className="management-step-list">
        <Panel title={t("adminConsole.accountsTitle")} eyebrow={t("adminConsole.accountsEyebrow")}>
          <Field label={t("adminConsole.statusFilter")}>
            <Select value={status} onChange={event => setStatus(event.target.value as AccountStatus)}>
              {(["PENDING", "ACTIVE", "SUSPENDED", "REJECTED"] as AccountStatus[]).map(item => <option value={item} key={item}>{t(`adminConsole.status.${item.toLowerCase()}`)}</option>)}
            </Select>
          </Field>
          <Field label={t("adminConsole.reason")}><TextInput value={reason} onChange={event => setReason(event.target.value)} /></Field>
          {accounts.length ? accounts.map(account => (
            <article className="management-step" key={account.id}>
              <div className="management-step__body">
                <div className="management-step__head"><h3>{account.displayName} · {account.username}</h3><p>{account.schoolName}</p></div>
                <div className="actions">
                  <StatusPill tone={account.status === "ACTIVE" ? "success" : account.status === "PENDING" ? "warning" : "neutral"}>{t(`adminConsole.status.${account.status.toLowerCase()}`)}</StatusPill>
                  {account.status === "PENDING" ? <Button type="button" variant="primary" icon={<CheckCircle2 size={16} />} disabled={busy} onClick={() => void run(() => api.approveTeacher(account.id), t("adminConsole.approved"))}>{t("adminConsole.approve")}</Button> : null}
                  {account.status === "PENDING" ? <Button type="button" variant="secondary" icon={<XCircle size={16} />} disabled={busy} onClick={() => void run(() => api.rejectTeacher(account.id, reason), t("adminConsole.rejected"))}>{t("adminConsole.reject")}</Button> : null}
                  {account.status === "ACTIVE" && account.role !== "ADMIN" ? <Button type="button" variant="secondary" disabled={busy} onClick={() => void run(() => api.suspendTeacher(account.id), t("adminConsole.suspended"))}>{t("adminConsole.suspend")}</Button> : null}
                  {account.status === "SUSPENDED" ? <Button type="button" variant="primary" disabled={busy} onClick={() => void run(() => api.restoreTeacher(account.id), t("adminConsole.restored"))}>{t("adminConsole.restore")}</Button> : null}
                  {account.status === "ACTIVE" ? <Button type="button" variant="secondary" icon={<KeyRound size={16} />} disabled={busy} onClick={() => void resetPassword(account.id)}>{t("adminConsole.resetPassword")}</Button> : null}
                </div>
                {account.status === "ACTIVE" ? (
                  <div className="form-grid">
                    <Field label={t("adminConsole.baseUnits")}><TextInput type="number" min="0" value={baseUnits} onChange={event => setBaseUnits(event.target.value)} /></Field>
                    <Field label={t("adminConsole.additionalUnits")}><TextInput type="number" min="0" value={additionalUnits} onChange={event => setAdditionalUnits(event.target.value)} /></Field>
                    <Button type="button" variant="secondary" disabled={busy} onClick={() => void run(() => api.adjustTeacherQuota(account.id, Number(baseUnits), Number(additionalUnits)), t("adminConsole.quotaSaved"))}>{t("adminConsole.saveQuota")}</Button>
                  </div>
                ) : null}
              </div>
            </article>
          )) : <EmptyState title={t("adminConsole.noAccounts")} />}
        </Panel>
        <Panel title={t("adminConsole.reviewsTitle")} eyebrow={t("adminConsole.reviewsEyebrow")}>
          {reviews.length ? reviews.map(problem => (
            <article className="management-step" key={problem.id}>
              <div className="management-step__body">
                <div className="management-step__head"><h3>{problem.title}</h3><p>v{problem.versionNo} · {problem.description}</p></div>
                <div className="actions">
                  <Button type="button" variant="primary" icon={<ShieldCheck size={16} />} disabled={busy} onClick={() => void run(() => api.approveProblemReview(problem.id), t("adminConsole.problemApproved"))}>{t("adminConsole.approveShared")}</Button>
                  <Button type="button" variant="secondary" disabled={busy} onClick={() => void run(async () => { const shared = await api.approveProblemReview(problem.id); await api.publishProblemPublic(shared.id); }, t("adminConsole.problemPublished"))}>{t("adminConsole.publishPublic")}</Button>
                  <Button type="button" variant="secondary" disabled={busy} onClick={() => void run(() => api.rejectProblemReview(problem.id, reason), t("adminConsole.problemRejected"))}>{t("adminConsole.reject")}</Button>
                </div>
              </div>
            </article>
          )) : <EmptyState title={t("adminConsole.noReviews")} />}
        </Panel>
      </div>
    </div>
  );
}

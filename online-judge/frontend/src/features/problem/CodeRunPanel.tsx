import { Eraser, Play, TestTube2 } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { api } from "../../shared/api/client";
import type { CodeRunResult } from "../../shared/api/types";
import { useTranslation } from "../../shared/i18n";
import { Button } from "../../shared/ui/Button";
import { Field, TextArea } from "../../shared/ui/Field";

type CodeRunPanelProps = {
  problemId: number;
  assignmentId?: number | null;
  languageId: number;
  sourceCode: string;
  sampleInput?: string | null;
  disabled?: boolean;
  onRunningChange?: (running: boolean) => void;
};

type RunSnapshot = {
  languageId: number;
  sourceCode: string;
  stdin: string;
};

export default function CodeRunPanel({
  problemId,
  assignmentId,
  languageId,
  sourceCode,
  sampleInput,
  disabled = false,
  onRunningChange
}: CodeRunPanelProps) {
  const { t } = useTranslation();
  const [stdin, setStdin] = useState("");
  const [result, setResult] = useState<CodeRunResult | null>(null);
  const [snapshot, setSnapshot] = useState<RunSnapshot | null>(null);
  const [running, setRunning] = useState(false);
  const [requestError, setRequestError] = useState<string | null>(null);

  useEffect(() => {
    setStdin("");
    setResult(null);
    setSnapshot(null);
    setRequestError(null);
  }, [problemId]);

  useEffect(() => () => onRunningChange?.(false), [onRunningChange]);

  const stale = useMemo(
    () => Boolean(result && snapshot && (
      snapshot.languageId !== languageId
      || snapshot.sourceCode !== sourceCode
      || snapshot.stdin !== stdin
    )),
    [languageId, result, snapshot, sourceCode, stdin]
  );

  async function runCode() {
    if (running || disabled || !sourceCode.trim()) {
      return;
    }
    setRunning(true);
    onRunningChange?.(true);
    setRequestError(null);
    const currentSnapshot = { languageId, sourceCode, stdin };
    try {
      const nextResult = await api.codeRun({
        problemId,
        assignmentId,
        languageId,
        sourceCode,
        stdin
      });
      setResult(nextResult);
      setSnapshot(currentSnapshot);
    } catch (error) {
      setRequestError(error instanceof Error ? error.message : t("codeRun.requestFailed"));
    } finally {
      setRunning(false);
      onRunningChange?.(false);
    }
  }

  const statusKey = `codeRun.status.${String(result?.status || "INTERNAL_ERROR").toLowerCase()}`;
  const hasSample = sampleInput !== null && sampleInput !== undefined;

  return (
    <section className="code-run-panel" aria-labelledby="code-run-title">
      <div className="code-run-panel__header">
        <div>
          <span className="eyebrow">{t("codeRun.eyebrow")}</span>
          <h3 id="code-run-title">{t("codeRun.title")}</h3>
          <p>{t("codeRun.description")}</p>
        </div>
        <span className="code-run-panel__temporary">{t("codeRun.temporary")}</span>
      </div>

      <Field label={t("codeRun.inputLabel")} hint={t("codeRun.inputHint")}>
        <TextArea
          className="code-run-input"
          value={stdin}
          onChange={event => setStdin(event.target.value)}
          placeholder={t("codeRun.inputPlaceholder")}
          rows={5}
          disabled={running || disabled}
          spellCheck={false}
        />
      </Field>

      <div className="code-run-actions">
        <Button
          variant="secondary"
          icon={<TestTube2 size={17} />}
          onClick={() => setStdin(sampleInput || "")}
          disabled={!hasSample || running || disabled}
          title={hasSample ? undefined : t("codeRun.noSample")}
        >
          {t("codeRun.loadSample")}
        </Button>
        <Button
          variant="ghost"
          icon={<Eraser size={17} />}
          onClick={() => setStdin("")}
          disabled={!stdin || running || disabled}
        >
          {t("codeRun.clearInput")}
        </Button>
        <Button
          className="code-run-submit"
          variant="secondary"
          icon={<Play size={17} />}
          onClick={() => void runCode()}
          disabled={running || disabled || !sourceCode.trim()}
        >
          {running ? t("codeRun.running") : t("codeRun.run")}
        </Button>
      </div>

      <div className="code-run-live" aria-live="polite">
        {running ? <p className="code-run-progress">{t("codeRun.runningHint")}</p> : null}
        {requestError ? <p className="code-run-request-error" role="alert">{requestError}</p> : null}
        {result ? (
          <div className="code-run-result" data-stale={stale || undefined}>
            <div className="code-run-result__meta">
              <strong className="code-run-status" data-status={result.status}>{t(statusKey)}</strong>
              <span>{t("codeRun.duration", { value: result.executionTimeMs })}</span>
              {stale ? <em>{t("codeRun.stale")}</em> : null}
            </div>
            {result.message ? <p className="code-run-message">{result.message}</p> : null}
            <div className="code-run-streams">
              <section>
                <div className="code-run-stream__label">
                  <strong>{t("codeRun.stdout")}</strong>
                  {result.stdoutTruncated ? <span>{t("codeRun.truncated")}</span> : null}
                </div>
                <pre className="code-run-stream code-run-stdout">{result.stdout || t("codeRun.noOutput")}</pre>
              </section>
              {result.stderr || result.stderrTruncated ? (
                <section>
                  <div className="code-run-stream__label">
                    <strong>{t("codeRun.stderr")}</strong>
                    {result.stderrTruncated ? <span>{t("codeRun.truncated")}</span> : null}
                  </div>
                  <pre className="code-run-stream code-run-stderr">{result.stderr || t("codeRun.noOutput")}</pre>
                </section>
              ) : null}
            </div>
          </div>
        ) : null}
      </div>
    </section>
  );
}

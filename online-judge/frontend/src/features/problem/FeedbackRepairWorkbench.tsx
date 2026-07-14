import { useEffect, useMemo, useRef, useState } from "react";
import { Check, ChevronDown, Code2, Play } from "lucide-react";
import type { StudentAiFeedbackItem, SubmissionResult } from "../../shared/api/types";
import { useTranslation } from "../../shared/i18n";
import {
  buildFeedbackCodeReferences,
  feedbackIssueTone,
  type FeedbackCodeReference
} from "./feedbackCodeReferences";

type FeedbackTestCase = NonNullable<SubmissionResult["testCaseResults"]>[number];

type FeedbackRepairWorkbenchProps = {
  sourceCode: string;
  sourceFileName: string;
  languageName: string;
  repairItems: StudentAiFeedbackItem[];
  improvementItems: StudentAiFeedbackItem[];
  summary: string;
  verificationPrompt: string;
  passed: number;
  total: number;
  firstFailedCase: FeedbackTestCase | null;
  busy: boolean;
  onRunAndVerify: () => void;
  onReturnToCode: () => void;
};

function lifecycleStatusKey(status?: string | null) {
  const normalized = String(status || "UNCOMPARABLE").toLowerCase();
  return ["new", "persisted", "not_observed", "recovered", "recurred", "uncomparable"].includes(normalized)
    ? normalized
    : "uncomparable";
}

function knowledgePathStatusKey(status?: string | null) {
  const normalized = String(status || "UNCLASSIFIED").toLowerCase();
  return ["formal", "provisional", "inferred", "unclassified"].includes(normalized)
    ? normalized
    : "unclassified";
}

function referenceForIssue(references: FeedbackCodeReference[], issueIndex: number) {
  return references.find(reference => reference.issueIndex === issueIndex) || null;
}

function rangeLabel(
  reference: FeedbackCodeReference | null,
  emptyLabel: string,
  lineLabelText: (line: number) => string,
  rangeLabelText: (start: number, end: number) => string
) {
  if (!reference?.ranges.length) {
    return emptyLabel;
  }
  return reference.ranges
    .map(range => range.startLine === range.endLine ? lineLabelText(range.startLine) : rangeLabelText(range.startLine, range.endLine))
    .join(" · ");
}

function issueReferenceAtLine(references: FeedbackCodeReference[], lineNumber: number, activeIssueIndex: number) {
  const matches = references.filter(reference => reference.ranges.some(range => lineNumber >= range.startLine && lineNumber <= range.endLine));
  return matches.find(reference => reference.issueIndex === activeIssueIndex) || matches[0] || null;
}

export function FeedbackRepairWorkbench({
  sourceCode,
  sourceFileName,
  languageName,
  repairItems,
  improvementItems,
  summary,
  verificationPrompt,
  passed,
  total,
  firstFailedCase,
  busy,
  onRunAndVerify,
  onReturnToCode
}: FeedbackRepairWorkbenchProps) {
  const { t } = useTranslation();
  const [activeIssueIndex, setActiveIssueIndex] = useState(0);
  const [checkedSteps, setCheckedSteps] = useState<Record<string, boolean>>({});
  const codePaneRef = useRef<HTMLDivElement>(null);
  const codeLines = useMemo(() => (sourceCode || "").replace(/\r\n/g, "\n").split("\n"), [sourceCode]);
  const references = useMemo(
    () => buildFeedbackCodeReferences(repairItems, Math.max(codeLines.length, 1)),
    [codeLines.length, repairItems]
  );
  const activeIssue = repairItems[activeIssueIndex] || repairItems[0] || null;
  const activeReference = referenceForIssue(references, activeIssueIndex);
  const activeTone = feedbackIssueTone(activeIssueIndex);
  const passRate = total ? Math.round((passed / total) * 100) : 0;

  useEffect(() => {
    if (activeIssueIndex >= repairItems.length) {
      setActiveIssueIndex(0);
    }
  }, [activeIssueIndex, repairItems.length]);

  function selectIssue(issueIndex: number) {
    setActiveIssueIndex(issueIndex);
    const targetLine = referenceForIssue(references, issueIndex)?.ranges[0]?.startLine;
    if (!targetLine) {
      return;
    }
    window.requestAnimationFrame(() => {
      codePaneRef.current
        ?.querySelector<HTMLElement>(`[data-line-number="${targetLine}"]`)
        ?.scrollIntoView({ behavior: "smooth", block: "center" });
    });
  }

  function toggleCheck(key: string) {
    setCheckedSteps(current => ({ ...current, [key]: !current[key] }));
  }

  return (
    <div className="feedback-code-workbench">
      <aside className="feedback-code-workbench__rail" aria-label={t("problemFeedbackWorkbench.issueNavigation")}>
        <div className="feedback-code-workbench__overview">
          <strong>{t("problemFeedbackWorkbench.statusOverview")}</strong>
          <dl>
            <div>
              <dt>{t("problemFeedbackWorkbench.judgeResult")}</dt>
              <dd>{passed}/{total || "-"}</dd>
            </div>
            <div>
              <dt>{t("problemFeedbackWorkbench.passRate")}</dt>
              <dd>{passRate}%</dd>
            </div>
          </dl>
        </div>

        <div className="feedback-code-workbench__issue-list">
          <div className="feedback-code-workbench__section-title">
            <strong>{t("problemFeedbackWorkbench.issueList")}</strong>
            <span>{t("problemFeedbackWorkbench.itemCount", { count: repairItems.length })}</span>
          </div>
          {repairItems.map((item, issueIndex) => {
            const tone = feedbackIssueTone(issueIndex);
            const reference = referenceForIssue(references, issueIndex);
            return (
              <button
                type="button"
                className={`feedback-code-workbench__issue${activeIssueIndex === issueIndex ? " is-active" : ""}`}
                data-tone={tone}
                aria-pressed={activeIssueIndex === issueIndex}
                onClick={() => selectIssue(issueIndex)}
                key={`${item.normalizedPointKey || item.title || "issue"}-${issueIndex}`}
              >
                <span className="feedback-code-workbench__issue-number">{issueIndex + 1}</span>
                <span className="feedback-code-workbench__issue-copy">
                  <strong>{item.title || t("issueLifecycle.unnamedIssue")}</strong>
                  <small>{reference
                    ? rangeLabel(
                        reference,
                        t("problemFeedbackWorkbench.codeLocated"),
                        line => t("feedbackMeta.line", { line }),
                        (start, end) => t("feedbackMeta.lineRange", { start, end })
                      )
                    : t("problemFeedbackWorkbench.noCodeLocation")}</small>
                </span>
              </button>
            );
          })}
        </div>

        <details className="feedback-code-workbench__growth">
          <summary>
            <span>{t("problemFeedbackWorkbench.growthSuggestions")}</span>
            <strong>{t("problemFeedbackWorkbench.itemCount", { count: improvementItems.length })}</strong>
            <ChevronDown size={16} aria-hidden="true" />
          </summary>
          <div>
            {improvementItems.map((item, index) => (
              <article className="feedback-code-workbench__growth-item" key={`${item.title || "growth"}-${index}`}>
                <strong>{item.title || t("issueLifecycle.unnamedIssue")}</strong>
                <p>{item.body}</p>
                <small>
                  {t("feedbackMeta.knowledgePath")} · {item.knowledgePath?.length ? item.knowledgePath.join(" › ") : t("feedbackMeta.noKnowledgePath")}
                  <b>{t(`feedbackMeta.pathStatus.${knowledgePathStatusKey(item.knowledgePathStatus)}`)}</b>
                </small>
              </article>
            ))}
          </div>
        </details>
      </aside>

      <main className="feedback-code-workbench__main">
        <div className="feedback-code-workbench__legend" aria-label={t("problemFeedbackWorkbench.codeLegend")}>
          {repairItems.map((item, issueIndex) => (
            <button
              type="button"
              data-tone={feedbackIssueTone(issueIndex)}
              className={activeIssueIndex === issueIndex ? "is-active" : ""}
              onClick={() => selectIssue(issueIndex)}
              key={`${item.title || "legend"}-${issueIndex}`}
            >
              <span>{issueIndex + 1}</span>
              {activeIssueIndex === issueIndex
                ? t("problemFeedbackWorkbench.currentIssue")
                : t("problemFeedbackWorkbench.relatedIssue")}
            </button>
          ))}
        </div>

        <section className="feedback-code-workbench__editor" aria-label={t("problemFeedbackWorkbench.codeEvidence")}>
          <header>
            <span>{sourceFileName}</span>
            <div>
              <span>{languageName}</span>
              <button type="button" onClick={onRunAndVerify} disabled={busy}>
                <Play size={15} fill="currentColor" aria-hidden="true" />
                {t("problemFeedbackWorkbench.runTest")}
              </button>
            </div>
          </header>
          <div className="feedback-code-workbench__code" ref={codePaneRef}>
            {codeLines.map((line, lineIndex) => {
              const lineNumber = lineIndex + 1;
              const reference = issueReferenceAtLine(references, lineNumber, activeIssueIndex);
              const startsRange = reference?.ranges.some(range => range.startLine === lineNumber);
              const isActive = reference?.issueIndex === activeIssueIndex;
              return (
                <div
                  className={`feedback-code-workbench__line${reference ? " is-referenced" : ""}${isActive ? " is-active" : ""}`}
                  data-tone={reference?.tone}
                  data-line-number={lineNumber}
                  key={lineNumber}
                >
                  <span className="feedback-code-workbench__gutter">
                    {startsRange ? (
                      <button type="button" onClick={() => reference && selectIssue(reference.issueIndex)} aria-label={t("problemFeedbackWorkbench.selectIssue", { number: reference?.issueNumber || "" })}>
                        {reference?.issueNumber}
                      </button>
                    ) : null}
                    <em>{lineNumber}</em>
                  </span>
                  <code>{line || " "}</code>
                  {startsRange && reference ? (
                    <button type="button" className="feedback-code-workbench__inline-label" onClick={() => selectIssue(reference.issueIndex)}>
                      {t("problemFeedbackWorkbench.inlineIssue", { number: reference.issueNumber })}
                    </button>
                  ) : null}
                </div>
              );
            })}
          </div>
        </section>

        <section className="feedback-code-workbench__failed-case" aria-label={t("problemFeedbackWorkbench.failedCase")}>
          <header>
            <strong>{t("problemFeedbackWorkbench.currentFailedCase")}</strong>
            {firstFailedCase ? <span>{t("problemFeedbackWorkbench.caseNumber", { number: firstFailedCase.testCaseNumber })}</span> : null}
          </header>
          {firstFailedCase ? (
            <dl>
              <div><dt>{t("problemFeedbackWorkbench.input")}</dt><dd>{t("problemFeedbackWorkbench.publicCase")}</dd></div>
              <div><dt>{t("problemFeedbackWorkbench.expected")}</dt><dd>{firstFailedCase.expectedOutput?.trim() || "-"}</dd></div>
              <div><dt>{t("problemFeedbackWorkbench.actual")}</dt><dd>{firstFailedCase.actualOutput?.trim() || "-"}</dd></div>
              <div><dt>{t("problemFeedbackWorkbench.result")}</dt><dd>{t("problemFeedbackWorkbench.failed")}</dd></div>
            </dl>
          ) : <p>{t("problemFeedbackWorkbench.noPublicFailedCase")}</p>}
        </section>

        <button type="button" className="feedback-code-workbench__run" onClick={onRunAndVerify} disabled={busy}>
          <Play size={17} fill="currentColor" aria-hidden="true" />
          {busy ? t("problemFeedbackWorkbench.running") : t("problemFeedbackWorkbench.runAndVerify")}
        </button>
      </main>

      <aside className="feedback-code-workbench__inspector" data-tone={activeTone} aria-live="polite">
        <header>
          <span>{t("problemFeedbackWorkbench.issuePosition", { current: activeIssueIndex + 1, total: repairItems.length })}</span>
          <strong>{activeIssue?.title || t("issueLifecycle.unnamedIssue")}</strong>
          <small>{t(`issueLifecycle.status.${lifecycleStatusKey(activeIssue?.changeStatus)}`)}</small>
        </header>

        <section>
          <h3>{t("problemFeedbackWorkbench.reason")}</h3>
          <p>{summary || activeIssue?.body || t("problemFeedbackWorkbench.noSummary")}</p>
        </section>
        <section>
          <h3>{t("problemFeedbackWorkbench.correction")}</h3>
          <p>{activeIssue?.body || t("problemFeedbackWorkbench.noCorrection")}</p>
        </section>
        <section>
          <h3>{t("problemFeedbackWorkbench.verification")}</h3>
          <p>{verificationPrompt || t("problemFeedbackWorkbench.defaultVerification")}</p>
        </section>

        <div className="feedback-code-workbench__evidence-location">
          <strong>{t("problemFeedbackWorkbench.evidenceLocation")}</strong>
          <span>{activeReference
            ? rangeLabel(
                activeReference,
                t("problemFeedbackWorkbench.codeLocated"),
                line => t("feedbackMeta.line", { line }),
                (start, end) => t("feedbackMeta.lineRange", { start, end })
              )
            : t("problemFeedbackWorkbench.noCodeLocation")}</span>
        </div>

        {activeIssue?.knowledgePath?.length ? (
          <div className="feedback-code-workbench__knowledge">
            <strong>{t("feedbackMeta.knowledgePath")}</strong>
            <span>{activeIssue.knowledgePath.join(" › ")}</span>
            <em>{t(`feedbackMeta.pathStatus.${knowledgePathStatusKey(activeIssue.knowledgePathStatus)}`)}</em>
          </div>
        ) : null}

        <div className="feedback-code-workbench__checks">
          {["understood", "verified"].map(checkKey => {
            const stateKey = `${activeIssueIndex}-${checkKey}`;
            return (
              <label key={checkKey}>
                <input type="checkbox" checked={Boolean(checkedSteps[stateKey])} onChange={() => toggleCheck(stateKey)} />
                <span aria-hidden="true"><Check size={13} /></span>
                {t(`problemFeedbackWorkbench.checks.${checkKey}`)}
              </label>
            );
          })}
        </div>

        <button type="button" className="feedback-code-workbench__return" onClick={onReturnToCode}>
          <Code2 size={16} aria-hidden="true" />
          {t("problemFeedbackWorkbench.returnToCode")}
        </button>
      </aside>
    </div>
  );
}

import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import type { DiagnosisTag } from "../../../shared/api/types";
import { Button } from "../../../shared/ui/Button";
import type { AnalyticsEvidenceSample } from "../model";

type Props = {
  title: string;
  emptyText: string;
  samples: AnalyticsEvidenceSample[];
  correction?: {
    title: string;
    issueLabel: string;
    fineIssueLabel: string;
    noteLabel: string;
    submitLabel: string;
    unavailableText: string;
    tags: DiagnosisTag[];
    onSubmit: (sample: AnalyticsEvidenceSample, payload: { correctedIssueTag: string; correctedFineGrainedTag: string; teacherNote: string }) => Promise<void>;
  };
};

type Draft = {
  correctedIssueTag: string;
  correctedFineGrainedTag: string;
  teacherNote: string;
};

export function EvidenceSamples({ title, emptyText, samples, correction }: Props) {
  const [draftBySample, setDraftBySample] = useState<Record<string, Draft>>({});
  const [savingId, setSavingId] = useState<string | null>(null);
  const coarseTags = correction?.tags.filter(tag => !tag.fineGrained) || [];
  const fineTags = correction?.tags.filter(tag => tag.fineGrained) || [];

  function draftFor(sample: AnalyticsEvidenceSample): Draft {
    return (
      draftBySample[sample.id] || {
        correctedIssueTag: sample.issueTag || coarseTags[0]?.id || "",
        correctedFineGrainedTag: sample.fineGrainedTag || "",
        teacherNote: ""
      }
    );
  }

  function updateDraft(sample: AnalyticsEvidenceSample, patch: Partial<Draft>) {
    setDraftBySample(current => ({ ...current, [sample.id]: { ...draftFor(sample), ...patch } }));
  }

  async function submitCorrection(event: FormEvent, sample: AnalyticsEvidenceSample) {
    event.preventDefault();
    if (!correction || !sample.submissionId) {
      return;
    }
    setSavingId(sample.id);
    try {
      await correction.onSubmit(sample, draftFor(sample));
      setDraftBySample(current => ({ ...current, [sample.id]: { ...draftFor(sample), teacherNote: "" } }));
    } finally {
      setSavingId(null);
    }
  }

  return (
    <section className="teacher-analytics-evidence" aria-label={title}>
      <div className="teacher-analytics-section-head">
        <span>{title}</span>
        <h2>{title}</h2>
      </div>
      {samples.length ? (
        <div className="teacher-analytics-evidence-list">
          {samples.map(sample =>
            sample.href ? (
              <article className="teacher-analytics-evidence-item" id={sample.studentProfileId ? `student-${sample.studentProfileId}` : undefined} key={sample.id}>
                <Link to={sample.href}>
                  <strong>{sample.title}</strong>
                  <span>{sample.subtitle}</span>
                  {sample.meta ? <small>{sample.meta}</small> : null}
                </Link>
                {correction ? renderCorrection(sample) : null}
              </article>
            ) : (
              <article className="teacher-analytics-evidence-item" key={sample.id}>
                <strong>{sample.title}</strong>
                <span>{sample.subtitle}</span>
                {sample.meta ? <small>{sample.meta}</small> : null}
                {correction ? renderCorrection(sample) : null}
              </article>
            )
          )}
        </div>
      ) : (
        <p className="teacher-analytics-empty-copy">{emptyText}</p>
      )}
    </section>
  );

  function renderCorrection(sample: AnalyticsEvidenceSample) {
    if (!correction) {
      return null;
    }
    if (!sample.submissionId) {
      return <small>{correction.unavailableText}</small>;
    }
    const draft = draftFor(sample);
    return (
      <details className="teacher-analytics-correction">
        <summary>{correction.title}</summary>
        <form onSubmit={event => submitCorrection(event, sample)}>
          <label>
            <span>{correction.issueLabel}</span>
            <select value={draft.correctedIssueTag} onChange={event => updateDraft(sample, { correctedIssueTag: event.target.value })}>
              {coarseTags.map(tag => (
                <option value={tag.id} key={tag.id}>
                  {tag.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            <span>{correction.fineIssueLabel}</span>
            <select value={draft.correctedFineGrainedTag} onChange={event => updateDraft(sample, { correctedFineGrainedTag: event.target.value })}>
              <option value="">-</option>
              {fineTags.map(tag => (
                <option value={tag.id} key={tag.id}>
                  {tag.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            <span>{correction.noteLabel}</span>
            <textarea value={draft.teacherNote} onChange={event => updateDraft(sample, { teacherNote: event.target.value })} rows={2} />
          </label>
          <Button type="submit" variant="secondary" disabled={savingId === sample.id}>
            {correction.submitLabel}
          </Button>
        </form>
      </details>
    );
  }
}

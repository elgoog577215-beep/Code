import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, BookOpen, ClipboardList, LogIn, RotateCcw } from "lucide-react";
import { api } from "../../shared/api/client";
import type { Assignment, ClassGroup, ReviewCard, StudentAbilityProfile, StudentProfile } from "../../shared/api/types";
import { verdictLabel } from "../../shared/format";
import { useTranslation } from "../../shared/i18n";
import { loadStudent, onActiveStudentChange } from "../../shared/storage";

function visibleAssignmentTitle(assignment: Assignment) {
  return assignment.title.includes("试点任务") ? "课堂编程作业" : assignment.title;
}

function latestTeacherAssignments(assignments: Assignment[]) {
  return assignments.filter(item => item.status !== "DRAFT");
}

function reviewCardLink(card: ReviewCard, studentId: number) {
  return `/app/student/assignments/public/problems/${card.problemId}?studentProfileId=${studentId}`;
}

export default function StudentPage() {
  const { t } = useTranslation();
  const [student, setStudent] = useState<StudentProfile | null>(() => loadStudent());
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [classes, setClasses] = useState<ClassGroup[]>([]);
  const [abilityProfile, setAbilityProfile] = useState<StudentAbilityProfile | null>(null);
  const [problemCount, setProblemCount] = useState<number | null>(null);
  const [assignmentLoading, setAssignmentLoading] = useState(false);
  const [profileLoading, setProfileLoading] = useState(false);
  const [failed, setFailed] = useState<string | null>(null);

  useEffect(() => {
    return onActiveStudentChange(() => setStudent(loadStudent()));
  }, [t]);

  useEffect(() => {
    let ignore = false;
    api.problemCatalog()
      .then(result => {
        if (!ignore) {
          setProblemCount(result.length);
        }
      })
      .catch(() => {
        if (!ignore) {
          setFailed(t("studentHome.errors.publicBank"));
        }
      });
    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    let ignore = false;
    api.studentClasses()
      .then(result => {
        if (!ignore) {
          setClasses(result);
        }
      })
      .catch(() => undefined);
    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    if (!student) {
      setAssignments([]);
      setAbilityProfile(null);
      setAssignmentLoading(false);
      setProfileLoading(false);
      return;
    }
    let ignore = false;
    setAssignmentLoading(true);
    setProfileLoading(true);
    api.studentAssignments(student.id)
      .then(result => {
        if (!ignore) {
          setAssignments(result);
        }
      })
      .catch(() => {
        if (!ignore) {
          setFailed(t("studentHome.errors.assignments"));
        }
      })
      .finally(() => {
        if (!ignore) {
          setAssignmentLoading(false);
        }
      });
    api.studentAbilityProfile(student.id)
      .then(result => {
        if (!ignore) {
          setAbilityProfile(result);
        }
      })
      .catch(() => {
        if (!ignore) {
          setAbilityProfile(null);
        }
      })
      .finally(() => {
        if (!ignore) {
          setProfileLoading(false);
        }
      });
    return () => {
      ignore = true;
    };
  }, [student, t]);

  const visibleAssignments = useMemo(() => latestTeacherAssignments(assignments), [assignments]);
  const reviewCards = useMemo(() => (abilityProfile?.reviewCards || []).slice(0, 3), [abilityProfile]);
  const fineFocus = abilityProfile?.fineGrainedProfile?.fineGrainedTagFocus?.[0]?.label || null;
  const abilityFocus = abilityProfile?.fineGrainedProfile?.abilityPointFocus?.[0]?.label || abilityProfile?.primaryAbilityFocus || null;
  const teacherByClassId = useMemo(() => {
    const lookup = new Map<number, string>();
    classes.forEach(item => {
      if (item.teacherName) {
        lookup.set(item.id, item.teacherName);
      }
    });
    return lookup;
  }, [classes]);

  function assignmentDetails(assignment: Assignment) {
    const taskCount = assignment.tasks?.length || 0;
    return [
      assignment.className,
      t("studentHome.taskCount", { count: taskCount }),
      assignment.classGroupId ? teacherByClassId.get(assignment.classGroupId) : null
    ].filter(Boolean);
  }

  return (
    <div className="student-page student-home student-home--assignments">
      {failed && <div className="alert alert--error">{failed}</div>}

      <section className="student-home-command student-home-command--compact student-home-command--entry">
        <div>
          <p className="eyebrow">{t("studentHome.eyebrow")}</p>
          <h1>{t("studentHome.title")}</h1>
          <p>{student ? t("studentHome.subtitleSignedIn", { name: student.displayName }) : t("studentHome.subtitleGuest")}</p>
        </div>
        <span className="student-home-command__note">
          {student ? t("studentHome.noteSignedIn") : t("studentHome.noteGuest")}
        </span>
      </section>

      <nav id="assignments" className="student-entry-list" aria-label="学生入口">
        <Link className="student-entry-link student-entry-link--primary" to="/app/student/assignments/public">
          <span className="student-entry-link__icon" aria-hidden="true">
            <BookOpen size={20} />
          </span>
          <span className="student-entry-link__main">
            <strong>{t("studentHome.public.title")}</strong>
            <small>{problemCount !== null ? t("studentHome.public.meta", { count: problemCount }) : t("studentHome.loading.publicBank")}</small>
            <span>{t("studentHome.public.description")}</span>
          </span>
          <span className="student-entry-link__cta">{t("studentHome.public.cta")}</span>
          <ArrowRight size={18} aria-hidden="true" />
        </Link>

        {!student ? (
          <Link className="student-entry-link student-entry-link--secondary" to="/app/student/login">
            <span className="student-entry-link__icon" aria-hidden="true">
              <LogIn size={20} />
            </span>
            <span className="student-entry-link__main">
              <strong>{t("studentHome.login.title")}</strong>
              <small>{t("studentHome.login.meta")}</small>
              <span>{t("studentHome.login.description")}</span>
            </span>
            <span className="student-entry-link__cta">{t("studentHome.login.cta")}</span>
            <ArrowRight size={18} aria-hidden="true" />
          </Link>
        ) : assignmentLoading ? (
          <div className="student-entry-link student-entry-link--muted" role="status" aria-live="polite">
            <span className="student-entry-link__icon" aria-hidden="true">
              <ClipboardList size={20} />
            </span>
            <span className="student-entry-link__main">
              <strong>{t("studentHome.loading.assignments")}</strong>
              <small>{t("studentHome.loading.assignmentHint")}</small>
            </span>
          </div>
        ) : visibleAssignments.length ? (
          visibleAssignments.map(assignment => (
            <Link className="student-entry-link student-entry-link--secondary" to={`/app/student/assignments/${assignment.id}`} key={assignment.id}>
              <span className="student-entry-link__icon" aria-hidden="true">
                <ClipboardList size={20} />
              </span>
              <span className="student-entry-link__main">
                <strong>{visibleAssignmentTitle(assignment)}</strong>
                <small>{assignmentDetails(assignment).join(" · ")}</small>
                <span>{t("studentHome.assignment.description")}</span>
              </span>
              <span className="student-entry-link__cta">{t("studentHome.assignment.cta")}</span>
              <ArrowRight size={18} aria-hidden="true" />
            </Link>
          ))
        ) : (
          <div className="student-entry-link student-entry-link--muted">
            <span className="student-entry-link__icon" aria-hidden="true">
              <ClipboardList size={20} />
            </span>
            <span className="student-entry-link__main">
              <strong>{t("studentHome.emptyAssignments.title")}</strong>
              <small>{t("studentHome.emptyAssignments.meta")}</small>
            </span>
          </div>
        )}
      </nav>

      {student ? (
        <section className="student-review-panel" aria-label={t("studentHome.review.aria")}>
          <div className="student-review-panel__head">
            <div>
              <p className="eyebrow">{t("studentHome.review.eyebrow")}</p>
              <h2>{t("studentHome.review.title")}</h2>
            </div>
            <span>
              {profileLoading
                ? t("studentHome.review.loading")
                : abilityProfile?.failedSubmissionCount
                  ? t("studentHome.review.failedCount", { count: abilityProfile.failedSubmissionCount })
                  : t("studentHome.review.waiting")}
            </span>
          </div>

          {abilityProfile?.fineGrainedProfile?.aiContextSummary || abilityFocus || fineFocus ? (
            <div className="student-review-summary">
              <strong>{abilityFocus || t("studentHome.review.profileBuilding")}</strong>
              <p>{abilityProfile?.fineGrainedProfile?.aiContextSummary || t("studentHome.review.focusHint", { focus: fineFocus || abilityFocus || t("studentHome.review.profileBuilding") })}</p>
            </div>
          ) : null}

          {reviewCards.length ? (
            <div className="student-review-cards">
              {reviewCards.map(card => (
                <Link className="student-review-card" to={reviewCardLink(card, student.id)} key={card.submissionId}>
                  <span className="student-review-card__icon">
                    <RotateCcw size={17} aria-hidden="true" />
                  </span>
                  <span className="student-review-card__body">
                    <strong>{card.problemTitle || `题目 #${card.problemId}`}</strong>
                    <small>
                      {[card.verdict ? verdictLabel(card.verdict) : null, card.primaryFineGrainedTag, card.abilityPoint]
                        .filter(Boolean)
                        .join(" · ")}
                    </small>
                    {card.nextAction ? <p>{card.nextAction}</p> : null}
                  </span>
                  <ArrowRight size={17} aria-hidden="true" />
                </Link>
              ))}
            </div>
          ) : (
            <div className="student-review-empty">
              {profileLoading ? t("studentHome.review.organizing") : t("studentHome.review.empty")}
            </div>
          )}
        </section>
      ) : null}
    </div>
  );
}

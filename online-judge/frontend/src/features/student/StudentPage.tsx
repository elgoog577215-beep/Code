import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, BookOpen, CircleCheck, ClipboardList, Clock3, LogIn, Play, RefreshCw, RotateCcw } from "lucide-react";
import { api } from "../../shared/api/client";
import type { Assignment, ReviewCard, StudentAbilityProfile, StudentProfile } from "../../shared/api/types";
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

function formatAssignmentDate(value?: string | null) {
  if (!value) {
    return null;
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return null;
  }
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(parsed);
}

interface AssignmentProgress {
  completedTasks: number;
  totalTasks: number;
}

export default function StudentPage() {
  const { t } = useTranslation();
  const [student, setStudent] = useState<StudentProfile | null>(() => loadStudent());
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [progressByAssignmentId, setProgressByAssignmentId] = useState<Record<number, AssignmentProgress | null>>({});
  const [abilityProfile, setAbilityProfile] = useState<StudentAbilityProfile | null>(null);
  const [problemCount, setProblemCount] = useState<number | null>(null);
  const [assignmentLoading, setAssignmentLoading] = useState(false);
  const [profileLoading, setProfileLoading] = useState(false);
  const [failed, setFailed] = useState<string | null>(null);

  useEffect(() => onActiveStudentChange(() => setStudent(loadStudent())), []);

  useEffect(() => {
    let ignore = false;
    api.problemCatalog()
      .then(result => {
        if (!ignore) setProblemCount(result.length);
      })
      .catch(() => {
        if (!ignore) setFailed(t("studentHome.errors.publicBank"));
      });
    return () => {
      ignore = true;
    };
  }, [t]);

  useEffect(() => {
    if (!student) {
      setAssignments([]);
      setProgressByAssignmentId({});
      setAbilityProfile(null);
      setAssignmentLoading(false);
      setProfileLoading(false);
      return;
    }
    let ignore = false;
    setAssignmentLoading(true);
    setProfileLoading(true);
    api.studentAssignments(student.id)
      .then(async result => {
        if (ignore) return;
        setAssignments(result);
        const progressEntries = await Promise.all(
          latestTeacherAssignments(result).map(async assignment => {
            try {
              const trajectory = await api.studentTrajectory(assignment.id, student.id);
              return [assignment.id, {
                completedTasks: trajectory.completedTasks,
                totalTasks: trajectory.totalTasks
              }] as const;
            } catch {
              return [assignment.id, null] as const;
            }
          })
        );
        if (!ignore) {
          setProgressByAssignmentId(Object.fromEntries(progressEntries));
        }
      })
      .catch(() => {
        if (!ignore) setFailed(t("studentHome.errors.assignments"));
      })
      .finally(() => {
        if (!ignore) setAssignmentLoading(false);
      });
    api.studentAbilityProfile(student.id)
      .then(result => {
        if (!ignore) setAbilityProfile(result);
      })
      .catch(() => {
        if (!ignore) setAbilityProfile(null);
      })
      .finally(() => {
        if (!ignore) setProfileLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [student, t]);

  const visibleAssignments = useMemo(() => latestTeacherAssignments(assignments), [assignments]);
  const progressFor = (assignment: Assignment) => progressByAssignmentId[assignment.id] ?? null;
  const featuredAssignmentId = useMemo(
    () => visibleAssignments.find(assignment => {
      const progress = progressByAssignmentId[assignment.id];
      return assignment.status === "ACTIVE" && progress && progress.completedTasks > 0 && progress.completedTasks < progress.totalTasks;
    })?.id ?? visibleAssignments.find(assignment => assignment.status === "ACTIVE")?.id ?? visibleAssignments[0]?.id,
    [progressByAssignmentId, visibleAssignments]
  );
  const inProgressAssignmentCount = visibleAssignments.filter(assignment => {
    const progress = progressByAssignmentId[assignment.id];
    return assignment.status === "ACTIVE" && progress && progress.completedTasks > 0 && progress.completedTasks < progress.totalTasks;
  }).length;
  const notStartedAssignmentCount = visibleAssignments.filter(assignment => {
    const progress = progressByAssignmentId[assignment.id];
    return assignment.status === "ACTIVE" && progress?.completedTasks === 0;
  }).length;
  const completedAssignmentCount = visibleAssignments.filter(assignment => {
    const progress = progressByAssignmentId[assignment.id];
    return Boolean(progress && progress.totalTasks > 0 && progress.completedTasks >= progress.totalTasks);
  }).length;
  const reviewCard = abilityProfile?.reviewCards?.[0] || null;
  const fineFocus = abilityProfile?.fineGrainedProfile?.fineGrainedTagFocus?.[0]?.label || null;
  const abilityFocus = abilityProfile?.fineGrainedProfile?.abilityPointFocus?.[0]?.label || abilityProfile?.primaryAbilityFocus || null;

  function assignmentState(assignment: Assignment) {
    const progress = progressFor(assignment);
    if (progress && progress.totalTasks > 0 && progress.completedTasks >= progress.totalTasks) {
      return { key: "completed", label: t("studentHome.dashboard.completed") };
    }
    if (assignment.status === "CLOSED") {
      return { key: "closed", label: t("studentHome.dashboard.closed") };
    }
    if (progress?.completedTasks === 0) {
      return { key: "not-started", label: t("studentHome.dashboard.notStarted") };
    }
    return { key: "active", label: t("studentHome.dashboard.active") };
  }

  return (
    <div className="student-page student-home student-home--assignments student-task-home">
      {failed && <div className="alert alert--error">{failed}</div>}

      <section className="student-home-command student-home-command--compact student-home-command--entry">
        <div className="student-home-command__identity">
          <BookOpen size={20} aria-hidden="true" />
          <h1>{t("studentHome.title")}</h1>
        </div>
        <div className="student-home-command__message">
          <strong>{student ? t("studentHome.dashboard.greeting", { name: student.displayName }) : t("studentHome.subtitleGuest")}</strong>
          {student ? <span>{t("studentHome.dashboard.headerSummary", {
            total: visibleAssignments.length,
            active: inProgressAssignmentCount
          })}</span> : null}
        </div>
        <span className="student-home-command__note">
          <RefreshCw size={14} aria-hidden="true" />
          {student ? t("studentHome.noteSignedIn") : t("studentHome.noteGuest")}
        </span>
      </section>

      {!student ? (
        <nav id="assignments" className="student-entry-list student-guest-entry" aria-label={t("studentHome.dashboard.entryAria")}>
          <Link className="student-entry-link student-entry-link--primary" to="/app/student/assignments/public">
            <span className="student-entry-link__icon" aria-hidden="true"><BookOpen size={20} /></span>
            <span className="student-entry-link__main">
              <strong>{t("studentHome.public.title")}</strong>
              <small>{problemCount !== null ? t("studentHome.public.meta", { count: problemCount }) : t("studentHome.loading.publicBank")}</small>
              <span>{t("studentHome.public.description")}</span>
            </span>
            <span className="student-entry-link__cta">{t("studentHome.public.cta")}</span>
            <ArrowRight size={18} aria-hidden="true" />
          </Link>
          <Link className="student-entry-link student-entry-link--secondary" to="/app/student/login">
            <span className="student-entry-link__icon" aria-hidden="true"><LogIn size={20} /></span>
            <span className="student-entry-link__main">
              <strong>{t("studentHome.login.title")}</strong>
              <small>{t("studentHome.login.meta")}</small>
              <span>{t("studentHome.login.description")}</span>
            </span>
            <span className="student-entry-link__cta">{t("studentHome.login.cta")}</span>
            <ArrowRight size={18} aria-hidden="true" />
          </Link>
        </nav>
      ) : (
        <>
          <section id="assignments" className="student-assignment-board" aria-labelledby="student-assignment-heading">
            <header className="student-assignment-board__head">
              <span className="student-assignment-board__icon" aria-hidden="true"><ClipboardList size={20} /></span>
              <div>
                <h2 id="student-assignment-heading">{t("studentHome.dashboard.classroom")}</h2>
                <p>{t("studentHome.dashboard.assignmentSummary", {
                  total: visibleAssignments.length,
                  active: inProgressAssignmentCount,
                  notStarted: notStartedAssignmentCount,
                  completed: completedAssignmentCount
                })}</p>
              </div>
            </header>

            {assignmentLoading ? (
              <div className="student-assignment-board__empty" role="status" aria-live="polite">
                {t("studentHome.loading.assignments")}
              </div>
            ) : visibleAssignments.length ? (
              <div className="student-assignment-table" role="list">
                <div className="student-assignment-table__header" aria-hidden="true">
                  <span>{t("studentHome.dashboard.assignmentName")}</span>
                  <span>{t("studentHome.dashboard.className")}</span>
                  <span>{t("studentHome.dashboard.status")}</span>
                  <span>{t("studentHome.dashboard.problemCount")}</span>
                  <span>{t("studentHome.dashboard.progress")}</span>
                  <span />
                </div>
                {visibleAssignments.map(assignment => {
                  const featured = assignment.id === featuredAssignmentId;
                  const deadline = formatAssignmentDate(assignment.endsAt);
                  const progress = progressFor(assignment);
                  const state = assignmentState(assignment);
                  const totalTasks = progress?.totalTasks ?? assignment.tasks?.length ?? 0;
                  const completedTasks = progress?.completedTasks ?? 0;
                  return (
                    <Link
                      className={`student-entry-link student-assignment-row${featured ? " student-assignment-row--featured" : ""}`}
                      to={`/app/student/assignments/${assignment.id}`}
                      key={assignment.id}
                      role="listitem"
                    >
                      <span className="student-assignment-row__icon" aria-hidden="true">
                        {featured
                          ? <Play size={17} fill="currentColor" />
                          : state.key === "completed" ? <CircleCheck size={19} /> : <ClipboardList size={19} />}
                      </span>
                      <span className="student-assignment-row__main">
                        <strong>{visibleAssignmentTitle(assignment)}</strong>
                        <small>{t("studentHome.dashboard.assignmentMeta", {
                          count: assignment.tasks?.length || 0,
                          description: assignment.description || t("studentHome.assignment.description")
                        })}</small>
                      </span>
                      <span className="student-assignment-row__class">{assignment.className || t("studentHome.dashboard.unassignedClass")}</span>
                      <span className="student-assignment-row__status">
                        <span><i className={`student-assignment-row__dot student-assignment-row__dot--${state.key}`} />{state.label}</span>
                        {deadline ? <small><Clock3 size={13} aria-hidden="true" />{t("studentHome.dashboard.deadline", { value: deadline })}</small> : null}
                      </span>
                      <span className="student-assignment-row__count">{t("studentHome.taskCount", { count: totalTasks })}</span>
                      <span className="student-assignment-row__progress" aria-label={t("studentHome.dashboard.progressAria", { completed: completedTasks, total: totalTasks })}>
                        <progress value={completedTasks} max={Math.max(totalTasks, 1)} />
                        <strong>{progress ? `${completedTasks}/${totalTasks}` : `-/${totalTasks}`}</strong>
                      </span>
                      {featured
                        ? <span className="student-assignment-row__action">{t("studentHome.dashboard.continue")}</span>
                        : <span aria-hidden="true" />}
                    </Link>
                  );
                })}
              </div>
            ) : (
              <div className="student-assignment-board__empty">
                <strong>{t("studentHome.emptyAssignments.title")}</strong>
                <span>{t("studentHome.emptyAssignments.meta")}</span>
              </div>
            )}
          </section>

          <section className="student-self-practice" aria-labelledby="student-practice-heading">
            <header>
              <span className="student-self-practice__icon" aria-hidden="true"><BookOpen size={19} /></span>
              <h2 id="student-practice-heading">{t("studentHome.dashboard.selfPractice")}</h2>
            </header>
            <Link className="student-self-practice__row" to="/app/student/assignments/public">
              <strong>{t("studentHome.public.title")}</strong>
              <span>{problemCount !== null ? t("studentHome.dashboard.publicCount", { count: problemCount }) : t("studentHome.loading.publicBank")}</span>
              <small>{t("studentHome.dashboard.publicHint")}</small>
              <span className="student-self-practice__action">{t("studentHome.dashboard.browsePublic")}</span>
            </Link>
          </section>
        </>
      )}

      {student ? (
        <section className="student-review-strip" aria-label={t("studentHome.review.aria")}>
          <span className="student-review-strip__label"><RotateCcw size={16} aria-hidden="true" />{t("studentHome.dashboard.recentReview")}</span>
          {reviewCard ? (
            <Link to={reviewCardLink(reviewCard, student.id)}>
              <strong>{reviewCard.problemTitle || `题目 #${reviewCard.problemId}`}</strong>
              <small>{[
                reviewCard.verdict ? verdictLabel(reviewCard.verdict) : null,
                reviewCard.primaryFineGrainedTag || abilityFocus || fineFocus
              ].filter(Boolean).join(" · ")}</small>
              <span>{t("studentHome.dashboard.viewReview")}<ArrowRight size={15} aria-hidden="true" /></span>
            </Link>
          ) : (
            <div className="student-review-strip__empty">
              {profileLoading ? t("studentHome.review.organizing") : t("studentHome.dashboard.emptyReview")}
            </div>
          )}
        </section>
      ) : null}
    </div>
  );
}

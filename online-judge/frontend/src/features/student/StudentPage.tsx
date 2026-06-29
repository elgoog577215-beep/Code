import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, LogIn, RotateCcw } from "lucide-react";
import { api } from "../../shared/api/client";
import type { Assignment, ClassGroup, ReviewCard, StudentAbilityProfile, StudentProfile } from "../../shared/api/types";
import { verdictLabel } from "../../shared/format";
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
  }, []);

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
          setFailed("公共题库暂时不可用。");
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
          setFailed("老师作业加载失败。");
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
  }, [student]);

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
      `${taskCount} 题`,
      assignment.classGroupId ? teacherByClassId.get(assignment.classGroupId) : null
    ].filter(Boolean);
  }

  return (
    <div className="student-page student-home student-home--assignments">
      {failed && <div className="alert alert--error">{failed}</div>}

      <section className="student-home-command student-home-command--compact student-home-command--entry">
        <div>
          <p className="eyebrow">学生端</p>
          <h1>选择练习入口</h1>
        </div>
        <span className="student-home-command__note">
          {student ? `${student.displayName}，继续当前任务。` : "可先刷公共题库，登录后查看课堂作业。"}
        </span>
      </section>

      <nav id="assignments" className="student-entry-list" aria-label="学生入口">
        <Link className="student-entry-link" to="/app/student/assignments/public">
          <span className="student-entry-link__main">
            <strong>公共题库</strong>
            <small>{problemCount !== null ? `${problemCount} 题` : "正在读取"}</small>
          </span>
          <ArrowRight size={18} aria-hidden="true" />
        </Link>

        {!student ? (
          <Link className="student-entry-link" to="/app/student/login">
            <span className="student-entry-link__main">
              <strong>登录查看课堂作业</strong>
              <small>班级作业和个人记录会出现在这里</small>
            </span>
            <LogIn size={18} aria-hidden="true" />
          </Link>
        ) : assignmentLoading ? (
          <div className="student-entry-link student-entry-link--muted" role="status" aria-live="polite">
            <span className="student-entry-link__main">
              <strong>正在读取课堂作业</strong>
              <small>公共题库可先进入练习</small>
            </span>
          </div>
        ) : visibleAssignments.length ? (
          visibleAssignments.map(assignment => (
            <Link className="student-entry-link" to={`/app/student/assignments/${assignment.id}`} key={assignment.id}>
              <span className="student-entry-link__main">
                <strong>{visibleAssignmentTitle(assignment)}</strong>
                <small>{assignmentDetails(assignment).join(" · ")}</small>
              </span>
              <ArrowRight size={18} aria-hidden="true" />
            </Link>
          ))
        ) : (
          <div className="student-entry-link student-entry-link--muted">
            <span className="student-entry-link__main">
              <strong>暂无课堂作业</strong>
              <small>老师发布后会自动出现在这里</small>
            </span>
          </div>
        )}
      </nav>

      {student ? (
        <section className="student-review-panel" aria-label="我的错题复盘">
          <div className="student-review-panel__head">
            <div>
              <p className="eyebrow">我的复盘</p>
              <h2>最近该回看的题</h2>
            </div>
            <span>{profileLoading ? "正在读取画像" : abilityProfile?.failedSubmissionCount ? `${abilityProfile.failedSubmissionCount} 次未通过记录` : "等待更多提交"}</span>
          </div>

          {abilityProfile?.fineGrainedProfile?.aiContextSummary || abilityFocus || fineFocus ? (
            <div className="student-review-summary">
              <strong>{abilityFocus || "画像仍在形成"}</strong>
              <p>{abilityProfile?.fineGrainedProfile?.aiContextSummary || `先围绕 ${fineFocus || abilityFocus} 做一次错题复盘。`}</p>
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
              {profileLoading ? "正在整理最近错题。" : "完成几次提交后，这里会出现可复盘的错题卡片。"}
            </div>
          )}
        </section>
      ) : null}
    </div>
  );
}

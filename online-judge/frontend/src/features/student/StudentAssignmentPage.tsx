import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, ArrowRight, CheckCircle2, Clock3, FileText, Search, Send, X } from "lucide-react";
import { Link, Navigate, useParams } from "react-router-dom";
import { api } from "../../shared/api/client";
import type { Assignment, ProblemCatalogItem, StudentProfile, StudentTrajectory } from "../../shared/api/types";
import { difficultyLabel, verdictLabel } from "../../shared/format";
import { useTranslation } from "../../shared/i18n";
import { hasDraft, loadLastPublicProblem, loadStudent, saveStudent } from "../../shared/storage";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import {
  assignmentTaskState,
  formatRelativeTime,
  latestTaskSubmission,
  StudentAssignmentShell
} from "./StudentAssignmentWorkspace";

const PUBLIC_DIFFICULTIES = ["EASY", "MEDIUM", "HARD"] as const;
type PublicDifficulty = "" | (typeof PUBLIC_DIFFICULTIES)[number];

function pickNextTask(assignment: Assignment | null, trajectory: StudentTrajectory | null) {
  if (!assignment?.tasks.length) {
    return null;
  }
  const pending = trajectory?.tasks.find(task => !task.passed);
  if (pending) {
    return assignment.tasks.find(task => task.problemId === pending.problemId) || assignment.tasks[0];
  }
  return assignment.tasks[0];
}

function publicDifficultyKey(value?: string | null) {
  switch ((value || "").toUpperCase()) {
    case "EASY":
      return "easy";
    case "MEDIUM":
      return "medium";
    case "HARD":
      return "hard";
    default:
      return "unknown";
  }
}

function cleanProblemSummary(summary: string | null | undefined, fallback: string) {
  const normalized = (summary || "")
    .replace(/题目描述/g, "")
    .replace(/输入格式.*/g, "")
    .replace(/输出格式.*/g, "")
    .replace(/\s+/g, " ")
    .trim();
  if (!normalized) {
    return fallback;
  }
  return normalized.length > 78 ? `${normalized.slice(0, 78)}...` : normalized;
}

export default function StudentAssignmentPage() {
  const { t } = useTranslation();
  const { assignmentId } = useParams();
  const isPublic = assignmentId === "public";
  const numericAssignmentId = Number(assignmentId);
  const [student] = useState<StudentProfile | null>(() => loadStudent());
  const [assignment, setAssignment] = useState<Assignment | null>(null);
  const [trajectory, setTrajectory] = useState<StudentTrajectory | null>(null);
  const [problems, setProblems] = useState<ProblemCatalogItem[]>([]);
  const [publicQuery, setPublicQuery] = useState("");
  const [publicDifficulty, setPublicDifficulty] = useState<PublicDifficulty>("");
  const [publicOnlyDrafts, setPublicOnlyDrafts] = useState(false);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setFailed(null);

    if (isPublic) {
      api.problemCatalog()
        .then(result => {
          if (!ignore) {
            setProblems(result);
          }
        })
        .catch(error => {
          if (!ignore) {
            setFailed(error instanceof Error ? error.message : t("studentPublic.errors.load"));
          }
        })
        .finally(() => {
          if (!ignore) {
            setLoading(false);
          }
        });
      return () => {
        ignore = true;
      };
    }

    if (!student || !Number.isFinite(numericAssignmentId)) {
      setLoading(false);
      return () => {
        ignore = true;
      };
    }

    Promise.all([api.studentAssignments(student.id), api.studentTrajectory(numericAssignmentId, student.id).catch(() => null)])
      .then(([assignments, trajectoryResult]) => {
        if (ignore) {
          return;
        }
        const matched = assignments.find(item => item.id === numericAssignmentId) || null;
        setAssignment(matched);
        setTrajectory(trajectoryResult);
        if (matched) {
          saveStudent(matched.id, student);
        }
      })
      .catch(error => {
        if (!ignore) {
          setFailed(error instanceof Error ? error.message : t("studentPublic.errors.assignmentLoad"));
        }
      })
      .finally(() => {
        if (!ignore) {
          setLoading(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, [assignmentId, isPublic, numericAssignmentId, student, t]);

  const draftProblemIds = useMemo(() => new Set(problems.filter(problem => hasDraft(problem.id)).map(problem => problem.id)), [problems]);
  const visiblePublicProblems = useMemo(() => {
    const normalizedQuery = publicQuery.trim().toLowerCase();
    const normalizedProblemIdQuery = normalizedQuery.replace(/^#/, "");
    const queryProblemId = /^\d+$/.test(normalizedProblemIdQuery) ? Number(normalizedProblemIdQuery) : null;
    return [...problems]
      .sort((left, right) => left.id - right.id)
      .filter(problem => {
        const matchesDifficulty = !publicDifficulty || String(problem.difficulty).toUpperCase() === publicDifficulty;
        const matchesDraft = !publicOnlyDrafts || draftProblemIds.has(problem.id);
        const searchable = [
          String(problem.id),
          `#${problem.id}`,
          problem.title,
          problem.summary,
          difficultyLabel(problem.difficulty),
          t(`studentPublic.difficulty.${publicDifficultyKey(problem.difficulty)}`),
          draftProblemIds.has(problem.id) ? t("studentPublic.filters.draft") : null
        ]
          .filter(Boolean)
          .join(" ")
          .toLowerCase();
        const matchesQuery = !normalizedQuery || (queryProblemId !== null ? problem.id === queryProblemId : searchable.includes(normalizedQuery));
        return matchesDifficulty && matchesDraft && matchesQuery;
      });
  }, [draftProblemIds, problems, publicDifficulty, publicOnlyDrafts, publicQuery, t]);
  const difficultyCounts = useMemo(() => {
    const counts = new Map<string, number>();
    problems.forEach(problem => {
      const key = String(problem.difficulty || "").toUpperCase();
      counts.set(key, (counts.get(key) || 0) + 1);
    });
    return counts;
  }, [problems]);
  const lastPublicProblem = useMemo(() => {
    const lastProblemId = loadLastPublicProblem();
    return lastProblemId ? problems.find(problem => problem.id === lastProblemId) || null : null;
  }, [problems]);
  const nextTask = useMemo(() => pickNextTask(assignment, trajectory), [assignment, trajectory]);
  const publicFiltersActive = Boolean(publicQuery.trim() || publicDifficulty || publicOnlyDrafts);
  const publicDraftCount = draftProblemIds.size;

  function clearPublicFilters() {
    setPublicQuery("");
    setPublicDifficulty("");
    setPublicOnlyDrafts(false);
  }

  function difficultyText(value?: string | null) {
    return t(`studentPublic.difficulty.${publicDifficultyKey(value)}`);
  }

  if (loading) {
    return (
      <div className="stack student-page">
        <EmptyState title={isPublic ? t("studentPublic.loading") : t("studentPublic.enteringAssignment")} live />
      </div>
    );
  }

  if (isPublic) {
    const studentParam = student?.id ? `?studentProfileId=${student.id}` : "";
    return (
      <div className="student-page student-public-page">
        <section className="student-home-command student-home-command--compact student-home-command--entry">
          <div>
            <p className="eyebrow">{t("studentPublic.eyebrow")}</p>
            <h1>{t("studentPublic.title")}</h1>
            <p>{t("studentPublic.subtitle", { count: problems.length })}</p>
          </div>
          <ButtonLink to="/app/student" variant="secondary" icon={<ArrowLeft size={17} />}>
            {t("studentPublic.back")}
          </ButtonLink>
        </section>

        {failed ? (
          <EmptyState title={failed} />
        ) : (
          <>
            <section className="public-problem-toolbar" aria-label={t("studentPublic.searchAria")}>
              <div className="public-problem-search">
                <Search size={17} aria-hidden="true" />
                <input
                  type="search"
                  aria-label={t("studentPublic.searchAria")}
                  placeholder={t("studentPublic.searchPlaceholder")}
                  value={publicQuery}
                  onChange={event => setPublicQuery(event.target.value)}
                />
                {publicQuery ? (
                  <button type="button" className="public-problem-search__clear" aria-label={t("studentPublic.clearSearch")} onClick={() => setPublicQuery("")}>
                    <X size={15} />
                  </button>
                ) : null}
              </div>
              <div className="public-problem-filter" role="group" aria-label={t("studentPublic.filterAria")}>
                <button type="button" className={!publicDifficulty ? "is-active" : ""} onClick={() => setPublicDifficulty("")}>
                  {t("studentPublic.filters.all")}
                  <span>{problems.length}</span>
                </button>
                {PUBLIC_DIFFICULTIES.map(difficulty => (
                  <button
                    type="button"
                    className={publicDifficulty === difficulty ? "is-active" : ""}
                    onClick={() => setPublicDifficulty(difficulty)}
                    key={difficulty}
                  >
                    {difficultyText(difficulty)}
                    <span>{difficultyCounts.get(difficulty) || 0}</span>
                  </button>
                ))}
                <button
                  type="button"
                  className={publicOnlyDrafts ? "is-active" : ""}
                  disabled={!publicDraftCount}
                  onClick={() => setPublicOnlyDrafts(current => !current)}
                >
                  {t("studentPublic.filters.draft")}
                  <span>{publicDraftCount}</span>
                </button>
              </div>
            </section>
            <div className="public-problem-status" aria-live="polite">
              <span>
                {t("studentPublic.status", { visible: visiblePublicProblems.length, total: problems.length })}
                {publicDraftCount ? ` · ${t("studentPublic.draftCount", { count: publicDraftCount })}` : ""}
              </span>
              {publicFiltersActive ? (
                <Button type="button" variant="ghost" onClick={clearPublicFilters}>
                  {t("studentPublic.clearFilters")}
                </Button>
              ) : null}
            </div>

            {lastPublicProblem ? (
              <Link
                className="student-entry-link public-problem-link public-problem-link--resume"
                to={`/app/student/assignments/public/problems/${lastPublicProblem.id}${studentParam}`}
              >
                <span className="student-entry-link__main">
                  <strong>{t("studentPublic.resume", { title: lastPublicProblem.title })}</strong>
                  <small>
                    {[`#${lastPublicProblem.id}`, difficultyText(lastPublicProblem.difficulty), draftProblemIds.has(lastPublicProblem.id) ? t("studentPublic.filters.draft") : null]
                      .filter(Boolean)
                      .join(" · ")}
                  </small>
                </span>
                <ArrowRight size={18} aria-hidden="true" />
              </Link>
            ) : null}

            {visiblePublicProblems.length ? (
              <nav className="student-entry-list public-problem-list" aria-label={t("studentPublic.listAria")}>
                {visiblePublicProblems.map(problem => (
                  <Link
                    className="student-entry-link public-problem-link public-problem-card"
                    to={`/app/student/assignments/public/problems/${problem.id}${studentParam}`}
                    key={problem.id}
                  >
                    <span className="public-problem-card__main">
                      <span className="public-problem-card__top">
                        <strong>{problem.title}</strong>
                        <span>{`#${problem.id}`}</span>
                      </span>
                      <span className="public-problem-card__summary">{cleanProblemSummary(problem.summary, t("studentPublic.problemFallback"))}</span>
                      <span className="public-problem-card__meta">
                        <span>{difficultyText(problem.difficulty)}</span>
                        {draftProblemIds.has(problem.id) ? <span>{t("studentPublic.filters.draft")}</span> : null}
                        <span>{t("studentPublic.timeLimit", { value: problem.timeLimit })}</span>
                        <span>{t("studentPublic.memoryLimit", { value: Math.round(problem.memoryLimit / 1024) })}</span>
                      </span>
                    </span>
                    <ArrowRight size={18} aria-hidden="true" />
                  </Link>
                ))}
              </nav>
            ) : (
              <div className="empty-state-with-action">
                <EmptyState title={problems.length ? t("studentPublic.emptyFiltered") : t("studentPublic.empty")} />
                {publicFiltersActive ? (
                  <Button type="button" variant="secondary" onClick={clearPublicFilters}>
                    {t("studentPublic.clearFilters")}
                  </Button>
                ) : null}
              </div>
            )}
          </>
        )}
      </div>
    );
  }

  if (!student && !isPublic) {
    return <Navigate to="/app/student/login" replace />;
  }

  if (!isPublic && student && assignment) {
    const latestSubmissionAt = trajectory?.tasks
      .flatMap(task => task.submissions || [])
      .map(item => item.submittedAt)
      .filter((value): value is string => Boolean(value))
      .sort((left, right) => new Date(right).getTime() - new Date(left).getTime())[0];
    return (
      <StudentAssignmentShell assignment={assignment} student={student} nextTask={nextTask} activeTab="assignment">
        <section className="student-assignment-summary-band" aria-label="作业进度摘要">
          <span><CheckCircle2 size={20} aria-hidden="true" />已通过 <strong>{trajectory?.completedTasks || 0}</strong> / {trajectory?.totalTasks || assignment.tasks.length}</span>
          <span><Send size={20} aria-hidden="true" />总提交 <strong>{trajectory?.totalAttempts || 0}</strong> 次</span>
          <span><Clock3 size={20} aria-hidden="true" />最近学习 <strong>{formatRelativeTime(latestSubmissionAt)}</strong></span>
        </section>

        <section className="student-assignment-progress-section" aria-labelledby="student-assignment-progress-title">
          <h2 id="student-assignment-progress-title">题目进度</h2>
          <div className="student-assignment-progress-table" role="list">
            <div className="student-assignment-progress-header" aria-hidden="true">
              <span>编号 / 题目</span><span>难度</span><span>状态</span><span>尝试次数</span><span>最近判题</span><span>操作</span>
            </div>
            {assignment.tasks.map((task, index) => {
              const state = assignmentTaskState(task, trajectory);
              const latest = latestTaskSubmission(state);
              const passed = Boolean(state?.passed);
              const selected = nextTask?.problemId === task.problemId && !passed;
              return (
                <div className={`student-assignment-progress-row${selected ? " is-next" : ""}`} role="listitem" key={task.problemId}>
                  <span className="student-assignment-progress-main"><b>{index + 1}</b><strong>{task.title}</strong></span>
                  <span>{difficultyLabel(task.difficulty)}</span>
                  <span className={passed ? "is-passed" : "is-pending"}>{passed ? <CheckCircle2 size={17} aria-hidden="true" /> : <Clock3 size={17} aria-hidden="true" />}{passed ? "已通过" : "未通过"}</span>
                  <span>{state?.attemptCount || 0}</span>
                  <span className="student-assignment-latest-verdict"><strong>{latest ? verdictLabel(latest.verdict) : "-"}</strong><small>{latest ? formatRelativeTime(latest.submittedAt) : "-"}</small></span>
                  <Link to={`/app/student/assignments/${assignment.id}/problems/${task.problemId}?studentProfileId=${student.id}`}>
                    <FileText size={16} aria-hidden="true" />
                    {selected ? "继续练习" : "查看题目"}
                    <ArrowRight size={15} aria-hidden="true" />
                  </Link>
                </div>
              );
            })}
          </div>
        </section>

        <section className="student-assignment-note-band">
          <div><strong>作业说明</strong><span>{assignment.description || "请按顺序完成作业题目，及时提交并查看评测反馈。"}</span></div>
          <div><strong>学习建议</strong><span>优先完成未通过题目，再根据反馈复盘错误原因。</span></div>
        </section>
      </StudentAssignmentShell>
    );
  }

  return (
    <div className="stack student-page">
      <section className="student-home-command">
        <div>
          <h1>{isPublic ? t("studentPublic.title") : t("studentPublic.assignmentTitle")}</h1>
        </div>
        <ButtonLink to="/app/student" variant="secondary" icon={<ArrowLeft size={17} />}>
        {t("studentPublic.back")}
      </ButtonLink>
      </section>
      <EmptyState title={failed || t("studentPublic.noProblems")} />
    </div>
  );
}

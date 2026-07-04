import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, ArrowRight, Search, X } from "lucide-react";
import { Link, Navigate, useParams } from "react-router-dom";
import { api } from "../../shared/api/client";
import type { Assignment, ProblemCatalogItem, StudentProfile, StudentTrajectory } from "../../shared/api/types";
import { difficultyLabel } from "../../shared/format";
import { hasDraft, loadLastPublicProblem, loadStudent, saveStudent } from "../../shared/storage";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";

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

export default function StudentAssignmentPage() {
  const { assignmentId } = useParams();
  const isPublic = assignmentId === "public";
  const numericAssignmentId = Number(assignmentId);
  const [student] = useState<StudentProfile | null>(() => loadStudent());
  const [assignment, setAssignment] = useState<Assignment | null>(null);
  const [trajectory, setTrajectory] = useState<StudentTrajectory | null>(null);
  const [problems, setProblems] = useState<ProblemCatalogItem[]>([]);
  const [publicQuery, setPublicQuery] = useState("");
  const [publicDifficulty, setPublicDifficulty] = useState<PublicDifficulty>("");
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
            setFailed(error instanceof Error ? error.message : "公共题库加载失败。");
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
          setFailed(error instanceof Error ? error.message : "作业加载失败。");
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
  }, [assignmentId, isPublic, numericAssignmentId, student]);

  const draftProblemIds = useMemo(() => new Set(problems.filter(problem => hasDraft(problem.id)).map(problem => problem.id)), [problems]);
  const visiblePublicProblems = useMemo(() => {
    const normalizedQuery = publicQuery.trim().toLowerCase();
    const normalizedProblemIdQuery = normalizedQuery.replace(/^#/, "");
    const queryProblemId = /^\d+$/.test(normalizedProblemIdQuery) ? Number(normalizedProblemIdQuery) : null;
    return [...problems]
      .sort((left, right) => left.id - right.id)
      .filter(problem => {
        const matchesDifficulty = !publicDifficulty || String(problem.difficulty).toUpperCase() === publicDifficulty;
        const searchable = [
          String(problem.id),
          `#${problem.id}`,
          problem.title,
          problem.summary,
          difficultyLabel(problem.difficulty),
          draftProblemIds.has(problem.id) ? "有草稿" : null
        ]
          .filter(Boolean)
          .join(" ")
          .toLowerCase();
        const matchesQuery = !normalizedQuery || (queryProblemId !== null ? problem.id === queryProblemId : searchable.includes(normalizedQuery));
        return matchesDifficulty && matchesQuery;
      });
  }, [draftProblemIds, problems, publicDifficulty, publicQuery]);
  const lastPublicProblem = useMemo(() => {
    const lastProblemId = loadLastPublicProblem();
    return lastProblemId ? problems.find(problem => problem.id === lastProblemId) || null : null;
  }, [problems]);
  const nextTask = useMemo(() => pickNextTask(assignment, trajectory), [assignment, trajectory]);
  const publicFiltersActive = Boolean(publicQuery.trim() || publicDifficulty);
  const publicDraftCount = draftProblemIds.size;

  function clearPublicFilters() {
    setPublicQuery("");
    setPublicDifficulty("");
  }

  if (loading) {
    return (
      <div className="stack student-page">
        <EmptyState title={isPublic ? "正在读取公共题库" : "正在进入作业"} live />
      </div>
    );
  }

  if (isPublic) {
    const studentParam = student?.id ? `?studentProfileId=${student.id}` : "";
    return (
      <div className="student-page student-public-page">
        <section className="student-home-command student-home-command--compact student-home-command--entry">
          <div>
            <p className="eyebrow">学生端</p>
            <h1>公共题库</h1>
          </div>
          <ButtonLink to="/app/student" variant="secondary" icon={<ArrowLeft size={17} />}>
            返回
          </ButtonLink>
        </section>

        {failed ? (
          <EmptyState title={failed} />
        ) : (
          <>
            <section className="public-problem-toolbar" aria-label="公共题库筛选">
              <div className="public-problem-search">
                <Search size={17} aria-hidden="true" />
                <input
                  type="search"
                  aria-label="搜索公共题目"
                  placeholder="搜索题号、题目或知识点"
                  value={publicQuery}
                  onChange={event => setPublicQuery(event.target.value)}
                />
                {publicQuery ? (
                  <button type="button" className="public-problem-search__clear" aria-label="清空搜索" onClick={() => setPublicQuery("")}>
                    <X size={15} />
                  </button>
                ) : null}
              </div>
              <div className="public-problem-filter" role="group" aria-label="按难度筛选">
                <button type="button" className={!publicDifficulty ? "is-active" : ""} onClick={() => setPublicDifficulty("")}>
                  全部
                </button>
                {PUBLIC_DIFFICULTIES.map(difficulty => (
                  <button
                    type="button"
                    className={publicDifficulty === difficulty ? "is-active" : ""}
                    onClick={() => setPublicDifficulty(difficulty)}
                    key={difficulty}
                  >
                    {difficultyLabel(difficulty)}
                  </button>
                ))}
              </div>
            </section>
            <div className="public-problem-status" aria-live="polite">
              <span>
                显示 {visiblePublicProblems.length}/{problems.length} 题
                {publicDraftCount ? ` · ${publicDraftCount} 题有草稿` : ""}
              </span>
              {publicFiltersActive ? (
                <Button type="button" variant="ghost" onClick={clearPublicFilters}>
                  清空筛选
                </Button>
              ) : null}
            </div>

            {lastPublicProblem ? (
              <Link
                className="student-entry-link public-problem-link public-problem-link--resume"
                to={`/app/student/assignments/public/problems/${lastPublicProblem.id}${studentParam}`}
              >
                <span className="student-entry-link__main">
                  <strong>继续上次：{lastPublicProblem.title}</strong>
                  <small>
                    {[`#${lastPublicProblem.id}`, difficultyLabel(lastPublicProblem.difficulty), draftProblemIds.has(lastPublicProblem.id) ? "有草稿" : null]
                      .filter(Boolean)
                      .join(" · ")}
                  </small>
                </span>
                <ArrowRight size={18} aria-hidden="true" />
              </Link>
            ) : null}

            {visiblePublicProblems.length ? (
              <nav className="student-entry-list public-problem-list" aria-label="公共题目">
                {visiblePublicProblems.map(problem => (
                  <Link
                    className="student-entry-link public-problem-link"
                    to={`/app/student/assignments/public/problems/${problem.id}${studentParam}`}
                    key={problem.id}
                  >
                    <span className="student-entry-link__main">
                      <strong>{problem.title}</strong>
                      <small>
                        {[
                          `#${problem.id}`,
                          difficultyLabel(problem.difficulty),
                          draftProblemIds.has(problem.id) ? "有草稿" : null,
                          problem.summary,
                          `${problem.timeLimit} ms`,
                          `${Math.round(problem.memoryLimit / 1024)} MB`
                        ]
                          .filter(Boolean)
                          .join(" · ")}
                      </small>
                    </span>
                    <ArrowRight size={18} aria-hidden="true" />
                  </Link>
                ))}
              </nav>
            ) : (
              <div className="empty-state-with-action">
                <EmptyState title={problems.length ? "没有匹配的题目" : "暂无公共题目"} />
                {publicFiltersActive ? (
                  <Button type="button" variant="secondary" onClick={clearPublicFilters}>
                    清空筛选
                  </Button>
                ) : null}
              </div>
            )}
          </>
        )}
      </div>
    );
  }

  if (!isPublic && student && assignment && nextTask) {
    return <Navigate to={`/app/student/assignments/${assignment.id}/problems/${nextTask.problemId}?studentProfileId=${student.id}`} replace />;
  }

  if (!student && !isPublic) {
    return <Navigate to="/app/student/login" replace />;
  }

  return (
    <div className="stack student-page">
      <section className="student-home-command">
        <div>
          <h1>{isPublic ? "公共题库" : "课堂作业"}</h1>
        </div>
        <ButtonLink to="/app/student" variant="secondary" icon={<ArrowLeft size={17} />}>
          返回
        </ButtonLink>
      </section>
      <EmptyState title={failed || "暂无题目"} />
    </div>
  );
}

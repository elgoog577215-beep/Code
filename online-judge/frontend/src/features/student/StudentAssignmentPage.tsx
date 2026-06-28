import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, ArrowRight, Search } from "lucide-react";
import { Link, Navigate, useParams } from "react-router-dom";
import { api } from "../../shared/api/client";
import type { Assignment, ProblemCatalogItem, StudentProfile, StudentTrajectory } from "../../shared/api/types";
import { difficultyLabel } from "../../shared/format";
import { loadStudent, saveStudent } from "../../shared/storage";
import { ButtonLink } from "../../shared/ui/Button";
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

  const visiblePublicProblems = useMemo(() => {
    const normalizedQuery = publicQuery.trim().toLowerCase();
    return [...problems]
      .sort((left, right) => left.id - right.id)
      .filter(problem => {
        const matchesDifficulty = !publicDifficulty || String(problem.difficulty).toUpperCase() === publicDifficulty;
        const searchable = [problem.title, problem.summary, difficultyLabel(problem.difficulty)].filter(Boolean).join(" ").toLowerCase();
        return matchesDifficulty && (!normalizedQuery || searchable.includes(normalizedQuery));
      });
  }, [problems, publicDifficulty, publicQuery]);
  const nextTask = useMemo(() => pickNextTask(assignment, trajectory), [assignment, trajectory]);

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
              <label className="public-problem-search">
                <Search size={17} aria-hidden="true" />
                <input
                  type="search"
                  aria-label="搜索公共题目"
                  placeholder="搜索题目或知识点"
                  value={publicQuery}
                  onChange={event => setPublicQuery(event.target.value)}
                />
              </label>
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
                        {[problem.summary, difficultyLabel(problem.difficulty), `${problem.timeLimit} ms`, `${Math.round(problem.memoryLimit / 1024)} MB`]
                          .filter(Boolean)
                          .join(" · ")}
                      </small>
                    </span>
                    <ArrowRight size={18} aria-hidden="true" />
                  </Link>
                ))}
              </nav>
            ) : (
              <EmptyState title={problems.length ? "没有匹配的题目" : "暂无公共题目"} />
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

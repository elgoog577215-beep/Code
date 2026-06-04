import { useEffect, useMemo, useState } from "react";
import { ArrowLeft } from "lucide-react";
import { Navigate, useParams } from "react-router-dom";
import { api } from "../../shared/api/client";
import type { Assignment, ProblemCatalogItem, StudentProfile, StudentTrajectory } from "../../shared/api/types";
import { loadStudent, saveStudent } from "../../shared/storage";
import { ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";

function firstPublicProblem(problems: ProblemCatalogItem[]) {
  return [...problems].sort((left, right) => left.id - right.id)[0] || null;
}

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

  const publicProblem = useMemo(() => firstPublicProblem(problems), [problems]);
  const nextTask = useMemo(() => pickNextTask(assignment, trajectory), [assignment, trajectory]);

  if (loading) {
    return (
      <div className="stack student-page">
        <EmptyState title="正在进入作业" />
      </div>
    );
  }

  if (isPublic && publicProblem) {
    const studentParam = student?.id ? `?studentProfileId=${student.id}` : "";
    return <Navigate to={`/app/student/assignments/public/problems/${publicProblem.id}${studentParam}`} replace />;
  }

  if (!isPublic && student && assignment && nextTask) {
    return <Navigate to={`/app/student/assignments/${assignment.id}/problems/${nextTask.problemId}?studentProfileId=${student.id}`} replace />;
  }

  if (!student && !isPublic) {
    return (
      <div className="stack student-page">
        <section className="student-home-command">
          <div>
            <p className="eyebrow">我的作业</p>
            <h1>请先登录</h1>
          </div>
          <ButtonLink to="/app/student" variant="ghost" icon={<ArrowLeft size={17} />}>
            返回
          </ButtonLink>
        </section>
        <EmptyState title="登录班级后查看老师作业" />
      </div>
    );
  }

  return (
    <div className="stack student-page">
      <section className="student-home-command">
        <div>
          <p className="eyebrow">{isPublic ? "公共题库" : "我的作业"}</p>
          <h1>无法进入</h1>
        </div>
        <ButtonLink to="/app/student" variant="secondary" icon={<ArrowLeft size={17} />}>
          返回作业
        </ButtonLink>
      </section>
      <EmptyState title={failed || (isPublic ? "公共题库暂无题目" : "没有可进入的题目")} />
    </div>
  );
}

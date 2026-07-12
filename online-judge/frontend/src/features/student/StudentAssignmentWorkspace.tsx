import { useEffect, useMemo, useState, type ReactNode } from "react";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import { api } from "../../shared/api/client";
import type { Assignment, AssignmentTask, StudentProfile, StudentTrajectory } from "../../shared/api/types";
import { loadStudent, saveStudent } from "../../shared/storage";

export type StudentAssignmentTab = "assignment" | "ranking" | "submissions";

export function pickNextAssignmentTask(assignment: Assignment | null, trajectory: StudentTrajectory | null) {
  if (!assignment?.tasks.length) {
    return null;
  }
  const pending = trajectory?.tasks.find(task => !task.passed);
  if (pending) {
    return assignment.tasks.find(task => task.problemId === pending.problemId) || assignment.tasks[0];
  }
  return assignment.tasks[0];
}

export function useStudentAssignmentWorkspace(assignmentId: number) {
  const [student] = useState<StudentProfile | null>(() => loadStudent(assignmentId));
  const [assignment, setAssignment] = useState<Assignment | null>(null);
  const [trajectory, setTrajectory] = useState<StudentTrajectory | null>(null);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;
    if (!student || !Number.isFinite(assignmentId)) {
      setLoading(false);
      return () => {
        ignore = true;
      };
    }
    setLoading(true);
    setFailed(null);
    Promise.all([
      api.studentAssignments(student.id),
      api.studentTrajectory(assignmentId, student.id)
    ])
      .then(([assignments, trajectoryResult]) => {
        if (ignore) return;
        const matched = assignments.find(item => item.id === assignmentId) || null;
        setAssignment(matched);
        setTrajectory(trajectoryResult);
        if (matched) saveStudent(matched.id, student);
      })
      .catch(error => {
        if (!ignore) setFailed(error instanceof Error ? error.message : "作业数据加载失败");
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [assignmentId, student]);

  const nextTask = useMemo(() => pickNextAssignmentTask(assignment, trajectory), [assignment, trajectory]);
  return { student, assignment, trajectory, nextTask, loading, failed };
}

export function latestTaskSubmission(task?: StudentTrajectory["tasks"][number] | null) {
  return task?.submissions?.[0] || null;
}

export function assignmentTaskState(task: AssignmentTask, trajectory: StudentTrajectory | null) {
  return trajectory?.tasks.find(item => item.problemId === task.problemId) || null;
}

export function formatRelativeTime(value?: string | null) {
  if (!value) return "暂无";
  const timestamp = new Date(value).getTime();
  if (!Number.isFinite(timestamp)) return "暂无";
  const minutes = Math.max(0, Math.round((Date.now() - timestamp) / 60000));
  if (minutes < 1) return "刚刚";
  if (minutes < 60) return `${minutes} 分钟前`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} 小时前`;
  return `${Math.round(hours / 24)} 天前`;
}

interface StudentAssignmentShellProps {
  assignment: Assignment;
  student: StudentProfile;
  nextTask: AssignmentTask | null;
  activeTab: StudentAssignmentTab;
  children: ReactNode;
}

export function StudentAssignmentShell({ assignment, student, nextTask, activeTab, children }: StudentAssignmentShellProps) {
  const basePath = `/app/student/assignments/${assignment.id}`;
  const tabs: Array<{ key: StudentAssignmentTab; label: string; to: string }> = [
    { key: "assignment", label: "作业", to: basePath },
    { key: "ranking", label: "班内排名", to: `${basePath}/ranking` },
    { key: "submissions", label: "提交记录", to: `${basePath}/submissions` }
  ];

  return (
    <div className="student-assignment-insights-page">
      <header className="student-assignment-insights-header">
        <Link className="student-assignment-back" to="/app/student">
          <ArrowLeft size={17} aria-hidden="true" />
          返回
        </Link>
        <div className="student-assignment-insights-title">
          <h1>{assignment.title}</h1>
        </div>
        {nextTask ? (
          <Link className="student-assignment-continue" to={`${basePath}/problems/${nextTask.problemId}?studentProfileId=${student.id}`}>
            继续作业
            <ArrowRight size={18} aria-hidden="true" />
          </Link>
        ) : null}
      </header>
      <nav className="student-assignment-insights-tabs" aria-label="作业页面导航">
        {tabs.map(tab => (
          <Link className={activeTab === tab.key ? "is-active" : ""} aria-current={activeTab === tab.key ? "page" : undefined} to={tab.to} key={tab.key}>
            {tab.label}
          </Link>
        ))}
      </nav>
      {children}
    </div>
  );
}

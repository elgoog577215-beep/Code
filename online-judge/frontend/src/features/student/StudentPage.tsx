import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import { api } from "../../shared/api/client";
import type { Assignment, ClassGroup, StudentProfile } from "../../shared/api/types";
import { loadStudent } from "../../shared/storage";

function visibleAssignmentTitle(assignment: Assignment) {
  return assignment.title.includes("试点任务") ? "课堂编程作业" : assignment.title;
}

function latestTeacherAssignments(assignments: Assignment[]) {
  return assignments.filter(item => item.status !== "DRAFT");
}

export default function StudentPage() {
  const [student, setStudent] = useState<StudentProfile | null>(() => loadStudent());
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [classes, setClasses] = useState<ClassGroup[]>([]);
  const [problemCount, setProblemCount] = useState<number | null>(null);
  const [assignmentLoading, setAssignmentLoading] = useState(false);
  const [failed, setFailed] = useState<string | null>(null);

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
      return;
    }
    let ignore = false;
    setAssignmentLoading(true);
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
    return () => {
      ignore = true;
    };
  }, [student]);

  const visibleAssignments = useMemo(() => latestTeacherAssignments(assignments), [assignments]);
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
    return [
      assignment.className,
      `${assignment.tasks.length} 题`,
      assignment.classGroupId ? teacherByClassId.get(assignment.classGroupId) : null
    ].filter(Boolean);
  }

  return (
    <div className="student-page student-home student-home--assignments">
      {failed && <div className="alert alert--error">{failed}</div>}

      <nav id="assignments" className="student-entry-list" aria-label="学生入口">
        <Link className="student-entry-link" to="/app/student/assignments/public">
          <span className="student-entry-link__main">
            <strong>公共题库</strong>
            {problemCount !== null ? <small>{problemCount} 题</small> : null}
          </span>
          <ArrowRight size={18} aria-hidden="true" />
        </Link>

        {student && !assignmentLoading
          ? visibleAssignments.map(assignment => (
              <Link className="student-entry-link" to={`/app/student/assignments/${assignment.id}`} key={assignment.id}>
                <span className="student-entry-link__main">
                  <strong>{visibleAssignmentTitle(assignment)}</strong>
                  <small>{assignmentDetails(assignment).join(" · ")}</small>
                </span>
                <ArrowRight size={18} aria-hidden="true" />
              </Link>
            ))
          : null}
      </nav>
    </div>
  );
}

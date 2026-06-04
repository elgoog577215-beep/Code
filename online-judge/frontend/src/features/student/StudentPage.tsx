import { useEffect, useMemo, useState } from "react";
import { ArrowRight, BookOpen, LogIn, LogOut, UserRound } from "lucide-react";
import { api } from "../../shared/api/client";
import type { Assignment, ProblemCatalogItem, StudentProfile } from "../../shared/api/types";
import { assignmentStatusLabel } from "../../shared/format";
import { clearActiveStudent, loadStudent } from "../../shared/storage";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Panel } from "../../shared/ui/Panel";
import { StatusPill } from "../../shared/ui/StatusPill";

function visibleAssignmentTitle(assignment: Assignment) {
  return assignment.title.includes("试点任务") ? "课堂编程作业" : assignment.title;
}

function assignmentSubtitle(assignment: Assignment, student: StudentProfile) {
  return assignment.className || student.className || "老师布置";
}

function latestTeacherAssignments(assignments: Assignment[]) {
  return assignments.filter(item => item.status !== "DRAFT");
}

export default function StudentPage() {
  const [student, setStudent] = useState<StudentProfile | null>(() => loadStudent());
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [problems, setProblems] = useState<ProblemCatalogItem[]>([]);
  const [assignmentLoading, setAssignmentLoading] = useState(false);
  const [problemLoading, setProblemLoading] = useState(true);
  const [failed, setFailed] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;
    setProblemLoading(true);
    api.problemCatalog()
      .then(result => {
        if (!ignore) {
          setProblems(result);
        }
      })
      .catch(() => {
        if (!ignore) {
          setFailed("公共题库暂时不可用。");
        }
      })
      .finally(() => {
        if (!ignore) {
          setProblemLoading(false);
        }
      });
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
  const publicSummary = problemLoading ? "正在加载" : problems.length ? `${problems.length} 道题` : "暂无题目";

  function signOut() {
    clearActiveStudent();
    setStudent(null);
    setAssignments([]);
  }

  return (
    <div className="stack student-page student-home student-home--assignments">
      {failed && <div className="alert alert--error">{failed}</div>}

      <section className="student-home-command student-home-command--compact">
        <div>
          <p className="eyebrow">学生端</p>
          <h1>作业</h1>
        </div>
        <div className="student-user-menu" aria-label="学生身份">
          {student ? (
            <>
              <div className="student-user-chip">
                <UserRound size={17} />
                <span>
                  <strong>{student.displayName}</strong>
                  <small>{student.className || "未设置班级"}</small>
                </span>
              </div>
              <ButtonLink to="/app/student/login" variant="ghost">
                切换
              </ButtonLink>
              <Button type="button" variant="ghost" icon={<LogOut size={17} />} onClick={signOut}>
                退出
              </Button>
            </>
          ) : (
            <ButtonLink to="/app/student/login" variant="primary" icon={<LogIn size={17} />}>
              登录
            </ButtonLink>
          )}
        </div>
      </section>

      <section id="assignments" className="student-assignment-anchor">
        <Panel
          className="student-assignment-catalog"
          eyebrow="作业列表"
          title="选择一组题开始"
          action={student ? <StatusPill tone="success">已登录</StatusPill> : <StatusPill tone="neutral">公共题库可用</StatusPill>}
        >
          <div className="student-assignment-list student-assignment-list--entry">
            <article className="student-assignment-row student-assignment-row--public">
              <div className="student-assignment-row__icon">
                <BookOpen size={22} />
              </div>
              <div className="student-assignment-row__main">
                <div className="student-task-meta-line">
                  <StatusPill tone="info">公开</StatusPill>
                  <span>{publicSummary}</span>
                </div>
                <h3>公共题库</h3>
                <p>自由练习，不需要登录。</p>
              </div>
              <ButtonLink to="/app/student/assignments/public" variant="primary" icon={<ArrowRight size={16} />}>
                进入
              </ButtonLink>
            </article>

            {!student ? (
              <article className="student-assignment-row student-assignment-row--locked">
                <div className="student-assignment-row__icon">
                  <UserRound size={22} />
                </div>
                <div className="student-assignment-row__main">
                  <div className="student-task-meta-line">
                    <StatusPill tone="neutral">班级作业</StatusPill>
                    <span>登录后查看</span>
                  </div>
                  <h3>老师布置的作业</h3>
                  <p>输入班级与姓名后，只显示本班作业。</p>
                </div>
                <ButtonLink to="/app/student/login" variant="secondary" icon={<LogIn size={16} />}>
                  登录
                </ButtonLink>
              </article>
            ) : assignmentLoading ? (
              <EmptyState title="正在加载老师作业" />
            ) : visibleAssignments.length === 0 ? (
              <EmptyState title="暂无老师作业" />
            ) : (
              visibleAssignments.map(assignment => (
                <article className="student-assignment-row" key={assignment.id}>
                  <div className="student-assignment-row__main">
                    <div className="student-task-meta-line">
                      <StatusPill tone={assignment.status === "ACTIVE" ? "success" : "neutral"}>
                        {assignmentStatusLabel(assignment.status)}
                      </StatusPill>
                      <span>{assignment.tasks.length} 题</span>
                    </div>
                    <h3>{visibleAssignmentTitle(assignment)}</h3>
                    <p>{assignmentSubtitle(assignment, student)}</p>
                  </div>
                  <ButtonLink to={`/app/student/assignments/${assignment.id}`} variant="primary" icon={<ArrowRight size={16} />}>
                    进入
                  </ButtonLink>
                </article>
              ))
            )}
          </div>
        </Panel>
      </section>
    </div>
  );
}

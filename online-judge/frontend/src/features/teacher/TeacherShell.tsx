import { ReactNode } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { BookOpenCheck, Database, GraduationCap } from "lucide-react";

export function TeacherShell({ children }: { children: ReactNode }) {
  const location = useLocation();
  const inManage = location.pathname.startsWith("/app/teacher/manage");

  return (
    <div className="teacher-shell">
      <nav className="teacher-shell-nav" aria-label="教师工作台">
        <div className="teacher-shell-nav__title">
          <GraduationCap size={17} />
          <span>教师工作台</span>
        </div>
        <div className="teacher-shell-nav__links">
          <NavLink to="/app/teacher" end className={({ isActive }) => (isActive || (!inManage && location.pathname.startsWith("/app/teacher")) ? "is-active" : "")}>
            <BookOpenCheck size={16} />
            <span>查看</span>
          </NavLink>
          <NavLink to="/app/teacher/manage" className={({ isActive }) => (isActive || inManage ? "is-active" : "")}>
            <Database size={16} />
            <span>管理</span>
          </NavLink>
        </div>
      </nav>
      {children}
    </div>
  );
}

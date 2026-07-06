import { ReactNode } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { BookOpenCheck, Database } from "lucide-react";
import { useTranslation } from "../../shared/i18n";
import "./TeacherHomeRefresh.css";

export function TeacherShell({ children }: { children: ReactNode }) {
  const location = useLocation();
  const inManage = location.pathname.startsWith("/app/teacher/manage");
  const { t } = useTranslation();

  return (
    <div className="teacher-shell">
      <nav className="teacher-shell-nav teacher-shell-nav--compact" aria-label={t("common.teacherWorkbench")}>
        <div className="teacher-shell-nav__links">
          <NavLink to="/app/teacher" end className={({ isActive }) => (isActive || (!inManage && location.pathname.startsWith("/app/teacher")) ? "is-active" : "")}>
            <BookOpenCheck size={16} />
            <span>{t("common.assignmentCenter")}</span>
          </NavLink>
          <NavLink to="/app/teacher/manage" className={({ isActive }) => (isActive || inManage ? "is-active" : "")}>
            <Database size={16} />
            <span>{t("common.resourceManagement")}</span>
          </NavLink>
        </div>
      </nav>
      {children}
    </div>
  );
}

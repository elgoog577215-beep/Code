import { ReactNode } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { BarChart3, BrainCircuit, Database, Power, UsersRound } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useTranslation } from "../../shared/i18n";
import "./TeacherHomeRefresh.css";

type TeacherNavItem = {
  to: string;
  label: string;
  icon: LucideIcon;
  activeWhen: (pathname: string) => boolean;
};

export function TeacherShell({ children }: { children: ReactNode }) {
  const { t } = useTranslation();
  const location = useLocation();
  const pathname = location.pathname;
  const primaryItems: TeacherNavItem[] = [
    {
      to: "/teacher/classes",
      label: t("teacherShell.nav.analytics"),
      icon: BarChart3,
      activeWhen: current =>
        current === "/teacher" ||
        current.startsWith("/teacher/classes") ||
        current.startsWith("/teacher/assignment")
    }
  ];
  const managementItems: TeacherNavItem[] = [
    {
      to: "/teacher/manage/classes",
      label: t("teacherShell.nav.roster"),
      icon: UsersRound,
      activeWhen: current => current.startsWith("/teacher/manage/classes")
    },
    {
      to: "/teacher/manage/problems",
      label: t("teacherShell.nav.problemBank"),
      icon: Database,
      activeWhen: current =>
        current.startsWith("/teacher/manage/problems") ||
        current.startsWith("/task-editor")
    },
    {
      to: "/teacher/manage/ai-library",
      label: t("teacherShell.nav.aiLibrary"),
      icon: BrainCircuit,
      activeWhen: current => current.startsWith("/teacher/manage/ai-library")
    },
    {
      to: "/teacher/manage/system",
      label: t("teacherShell.nav.system"),
      icon: Power,
      activeWhen: current => current.startsWith("/teacher/manage/system")
    }
  ];

  return (
    <div className="teacher-shell teacher-console-shell">
      <aside className="teacher-shell-sidebar" aria-label={t("teacherShell.aria")}>
        <div className="teacher-shell-sidebar__head">
          <strong>{t("teacherShell.workspaceTitle")}</strong>
        </div>
        <nav className="teacher-shell-nav" aria-label={t("teacherShell.aria")}>
          <div className="teacher-shell-nav__section teacher-shell-nav__section--primary">
            <span className="teacher-shell-nav__group-label">{t("teacherShell.groups.results")}</span>
            {primaryItems.map(item => renderNavItem(item, pathname))}
          </div>
          <div className="teacher-shell-nav__section teacher-shell-nav__section--management">
            <span className="teacher-shell-nav__group-label">{t("teacherShell.groups.management")}</span>
            {managementItems.map(item => renderNavItem(item, pathname))}
          </div>
        </nav>
      </aside>
      <main className="teacher-shell-main">{children}</main>
    </div>
  );
}

function renderNavItem(item: TeacherNavItem, pathname: string) {
  const active = item.activeWhen(pathname);
  return (
    <NavLink
      to={item.to}
      className={active ? "teacher-shell-nav__item is-active" : "teacher-shell-nav__item"}
      key={item.to}
    >
      <span className="teacher-shell-nav__icon">
        <item.icon size={18} />
      </span>
      <span>
        <strong>{item.label}</strong>
      </span>
    </NavLink>
  );
}

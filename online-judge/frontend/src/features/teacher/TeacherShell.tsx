import { ReactNode } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { BarChart3, BrainCircuit, Database, Power, UsersRound } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useTranslation } from "../../shared/i18n";
import "./TeacherHomeRefresh.css";

type TeacherNavItem = {
  to: string;
  label: string;
  description: string;
  icon: LucideIcon;
  activeWhen: (pathname: string) => boolean;
};

export function TeacherShell({ children }: { children: ReactNode }) {
  const { t } = useTranslation();
  const location = useLocation();
  const pathname = location.pathname;
  const items: TeacherNavItem[] = [
    {
      to: "/app/teacher/classes",
      label: t("teacherShell.nav.analytics"),
      description: t("teacherShell.nav.analyticsDescription"),
      icon: BarChart3,
      activeWhen: current =>
        current === "/app/teacher" ||
        current.startsWith("/app/teacher/classes") ||
        current.startsWith("/app/teacher/assignment")
    },
    {
      to: "/app/teacher/manage/classes",
      label: t("teacherShell.nav.roster"),
      description: t("teacherShell.nav.rosterDescription"),
      icon: UsersRound,
      activeWhen: current => current.startsWith("/app/teacher/manage/classes")
    },
    {
      to: "/app/teacher/manage/problems",
      label: t("teacherShell.nav.problemBank"),
      description: t("teacherShell.nav.problemBankDescription"),
      icon: Database,
      activeWhen: current =>
        current.startsWith("/app/teacher/manage/problems") ||
        current.startsWith("/app/task-editor")
    },
    {
      to: "/app/teacher/manage/ai-library",
      label: t("teacherShell.nav.aiLibrary"),
      description: t("teacherShell.nav.aiLibraryDescription"),
      icon: BrainCircuit,
      activeWhen: current => current.startsWith("/app/teacher/manage/ai-library")
    },
    {
      to: "/app/teacher/manage/system",
      label: t("teacherShell.nav.system"),
      description: t("teacherShell.nav.systemDescription"),
      icon: Power,
      activeWhen: current => current.startsWith("/app/teacher/manage/system")
    }
  ];

  return (
    <div className="teacher-shell teacher-console-shell">
      <aside className="teacher-shell-sidebar" aria-label={t("teacherShell.aria")}>
        <div className="teacher-shell-sidebar__head">
          <span>{t("teacherShell.kicker")}</span>
          <strong>{t("teacherShell.title")}</strong>
        </div>
        <nav className="teacher-shell-nav" aria-label={t("teacherShell.aria")}>
          {items.map(item => {
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
                  <small>{item.description}</small>
                </span>
              </NavLink>
            );
          })}
        </nav>
        <div className="teacher-shell-sidebar__foot">
          <span>{t("teacherShell.footnote")}</span>
        </div>
      </aside>
      <main className="teacher-shell-main">{children}</main>
    </div>
  );
}

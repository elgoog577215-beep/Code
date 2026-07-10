import { lazy, ReactNode, Suspense, useEffect, useState } from "react";
import { NavLink, Navigate, Route, Routes, useLocation } from "react-router-dom";
import type { LucideIcon } from "lucide-react";
import { BookOpenCheck, LogIn, Menu, Moon, Sun, UserRound, UsersRound, X } from "lucide-react";
import TeacherAuthGate from "./features/teacher/TeacherAuthGate";
import { TeacherShell } from "./features/teacher/TeacherShell";
import { useTranslation } from "./shared/i18n";
import { clearActiveStudent, loadStudent, onActiveStudentChange } from "./shared/storage";
import { Button } from "./shared/ui/Button";
import { EmptyState } from "./shared/ui/EmptyState";
import "./features/teacher-analytics/TeacherAnalytics.css";

const StudentPage = lazy(() => import("./features/student/StudentPage"));
const StudentLoginPage = lazy(() => import("./features/student/StudentLoginPage"));
const StudentAssignmentPage = lazy(() => import("./features/student/StudentAssignmentPage"));
const ProblemPage = lazy(() => import("./features/problem/ProblemPage"));
const AssignmentCreatePage = lazy(() => import("./features/teacher/AssignmentCreatePage"));
const TeacherManagementPage = lazy(() => import("./features/teacher/TeacherManagementPage"));
const TaskEditorPage = lazy(() => import("./features/task-editor/TaskEditorPage"));
const TeacherAnalyticsLandingPage = lazy(() => import("./features/teacher-analytics/pages/TeacherAnalyticsLandingPage"));
const ClassAnalyticsPage = lazy(() => import("./features/teacher-analytics/pages/ClassAnalyticsPage"));
const AssignmentAnalyticsPage = lazy(() => import("./features/teacher-analytics/pages/AssignmentAnalyticsPage"));
const ProblemAnalyticsPage = lazy(() => import("./features/teacher-analytics/pages/ProblemAnalyticsPage"));

type Theme = "light" | "dark";
type NavItem = {
  to: string;
  label: string;
  icon: LucideIcon;
  activeWhen?: (pathname: string) => boolean;
  noActive?: boolean;
};

function loadTheme(): Theme {
  try {
    return localStorage.getItem("wzai:theme") === "dark" ? "dark" : "light";
  } catch {
    return "light";
  }
}

function saveTheme(theme: Theme): void {
  try {
    localStorage.setItem("wzai:theme", theme);
  } catch {
    // Theme still applies for the current page even when storage is blocked.
  }
}

function useTheme() {
  const [theme, setTheme] = useState<Theme>(loadTheme);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    saveTheme(theme);
  }, [theme]);

  return { theme, setTheme };
}

function RouteHubPage() {
  const { t } = useTranslation();

  return (
    <div className="role-entry-page route-hub-page">
      <section className="role-entry route-hub-hero">
        <div>
          <p className="eyebrow">{t("routeHub.eyebrow")}</p>
          <h1>{t("routeHub.title")}</h1>
        </div>
      </section>

      <section className="role-entry-grid route-hub-grid" aria-label={t("routeHub.title")}>
        <NavLink to="/app/student" className="role-entry-card route-hub-card route-hub-card--student">
          <span className="role-entry-card__icon">
            <BookOpenCheck size={24} />
          </span>
          <span>
            <span className="role-entry-card__head">
              <span className="eyebrow">{t("common.studentSide")}</span>
            </span>
            <h2>{t("routeHub.studentTitle")}</h2>
          </span>
        </NavLink>

        <NavLink to="/app/teacher" className="role-entry-card route-hub-card route-hub-card--teacher">
          <span className="role-entry-card__icon">
            <UsersRound size={24} />
          </span>
          <span>
            <span className="role-entry-card__head">
              <span className="eyebrow">{t("common.teacherSide")}</span>
            </span>
            <h2>{t("routeHub.teacherTitle")}</h2>
          </span>
        </NavLink>
      </section>
    </div>
  );
}

function TeacherRoute({ children }: { children: ReactNode }) {
  return (
    <TeacherAuthGate>
      <TeacherShell>{children}</TeacherShell>
    </TeacherAuthGate>
  );
}

function LegacyAssignmentRedirect({ level }: { level: "assignment" | "problem" | "student" }) {
  const location = useLocation();
  const match = location.pathname.match(/\/app\/teacher\/assignment\/([^/]+)(?:\/problems\/([^/]+))?(?:\/students\/([^/]+))?/);
  const assignmentId = match?.[1];
  const problemId = match?.[2];
  const search = location.search || "";
  if (!assignmentId) {
    return <Navigate to="/app/teacher/classes" replace />;
  }
  if (level !== "assignment" && problemId) {
    return <Navigate to={`/app/teacher/classes/0/assignments/${assignmentId}/problems/${problemId}${search}`} replace />;
  }
  return <Navigate to={`/app/teacher/classes/0/assignments/${assignmentId}${search}`} replace />;
}

function Header() {
  const { theme, setTheme } = useTheme();
  const { locale, setLocale, t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [student, setStudent] = useState(() => loadStudent());
  const location = useLocation();
  const isProblemPage =
    /^\/app\/student\/assignments\/[^/]+\/problems\//.test(location.pathname);
  const isStudentContext = location.pathname.startsWith("/app/student") || isProblemPage;
  const isTeacherContext = location.pathname.startsWith("/app/teacher") || location.pathname.startsWith("/app/task-editor");
  const navItems: NavItem[] = [
    {
      to: "/app/student",
      label: t("common.studentSide"),
      icon: BookOpenCheck,
      activeWhen: (pathname: string) => pathname.startsWith("/app/student")
    },
    {
      to: "/app/teacher",
      label: t("common.teacherSide"),
      icon: UsersRound,
      activeWhen: (pathname: string) => pathname.startsWith("/app/teacher") || pathname.startsWith("/app/task-editor")
    }
  ];

  useEffect(() => {
    setOpen(false);
    setStudent(loadStudent());
  }, [location.pathname]);

  useEffect(() => onActiveStudentChange(() => setStudent(loadStudent())), []);

  function signOut() {
    clearActiveStudent();
    setStudent(null);
  }

  const hasNav = navItems.length > 0;
  const headerClassName = [
    "app-header",
    open && hasNav ? "is-open" : "",
    isProblemPage ? "app-header--practice" : "",
    isTeacherContext ? "app-header--teacher-context" : "",
    hasNav ? "" : "app-header--no-nav"
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <header className={headerClassName}>
      <NavLink to="/app" className="brand" aria-label={t("common.appName")}>
        <span className="brand__mark">
          <BookOpenCheck size={24} />
        </span>
        <span>
          <strong>{t("common.appName")}</strong>
        </span>
      </NavLink>
      {hasNav ? (
        <>
          <button
            type="button"
            className="nav-toggle"
            aria-label={open ? t("common.closeNavigation") : t("common.openNavigation")}
            aria-expanded={open}
            aria-controls="main-navigation"
            onClick={() => setOpen(value => !value)}
          >
            {open ? <X size={19} /> : <Menu size={19} />}
          </button>
          <nav className="top-nav" id="main-navigation" aria-label={t("common.mainNavigation")}>
            {navItems.map(item => (
              <NavLink
                key={`${item.label}-${item.to}`}
                to={item.to}
                className={({ isActive }) =>
                  !item.noActive && (isActive || item.activeWhen?.(location.pathname)) ? "top-nav__link is-active" : "top-nav__link"
                }
              >
                <item.icon size={17} />
                <span>{item.label}</span>
              </NavLink>
            ))}
          </nav>
        </>
      ) : null}
      <div className="header-actions">
        {isStudentContext ? (
          student ? (
            <div className="header-student-menu" aria-label="学生身份">
              <NavLink to="/app/student/login" className="header-student-chip">
                <UserRound size={16} />
                <span>{student.displayName}</span>
              </NavLink>
              <Button type="button" variant="ghost" className="header-signout-button" onClick={signOut}>
                {t("common.logout")}
              </Button>
            </div>
          ) : (
            <NavLink to="/app/student/login" className="header-login-link">
              <LogIn size={17} />
              <span>{t("common.login")}</span>
            </NavLink>
          )
        ) : null}
        <button
          type="button"
          className="language-toggle"
          aria-label={t("common.language")}
          title={t("common.language")}
          onClick={() => setLocale(locale === "zh" ? "en" : "zh")}
        >
          <span>{locale === "zh" ? "中" : "EN"}</span>
        </button>
        <button
          type="button"
          className="theme-toggle"
          aria-label={t("common.theme.toggle")}
          title={theme === "dark" ? t("common.theme.toLight") : t("common.theme.toDark")}
          onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
        >
          {theme === "dark" ? <Sun size={18} /> : <Moon size={18} />}
          <span>{theme === "dark" ? t("common.theme.light") : t("common.theme.dark")}</span>
        </button>
      </div>
    </header>
  );
}

export default function App() {
  const { t } = useTranslation();

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        {t("common.skipToMain")}
      </a>
      <Header />
      <main className="main-shell" id="main-content">
        <Suspense fallback={<EmptyState title={t("common.loadingPage")} live />}>
          <Routes>
            <Route path="/" element={<Navigate to="/app" replace />} />
            <Route path="/app" element={<RouteHubPage />} />
            <Route path="/app/" element={<RouteHubPage />} />
            <Route path="/app/student" element={<StudentPage />} />
            <Route path="/app/student/login" element={<StudentLoginPage />} />
            <Route path="/app/student/assignments/:assignmentId" element={<StudentAssignmentPage />} />
            <Route path="/app/student/assignments/:assignmentId/problems/:problemId" element={<ProblemPage />} />
            <Route path="/app/teacher" element={<TeacherRoute><Navigate to="/app/teacher/classes" replace /></TeacherRoute>} />
            <Route path="/app/teacher/classes" element={<TeacherRoute><TeacherAnalyticsLandingPage /></TeacherRoute>} />
            <Route path="/app/teacher/classes/:classId" element={<TeacherRoute><ClassAnalyticsPage /></TeacherRoute>} />
            <Route path="/app/teacher/classes/:classId/assignments/:assignmentId" element={<TeacherRoute><AssignmentAnalyticsPage /></TeacherRoute>} />
            <Route path="/app/teacher/classes/:classId/assignments/:assignmentId/problems/:problemId" element={<TeacherRoute><ProblemAnalyticsPage /></TeacherRoute>} />
            <Route path="/app/teacher/manage" element={<TeacherRoute><Navigate to="/app/teacher/manage/classes" replace /></TeacherRoute>} />
            <Route path="/app/teacher/manage/classes" element={<TeacherRoute><TeacherManagementPage section="classes" /></TeacherRoute>} />
            <Route path="/app/teacher/manage/problems" element={<TeacherRoute><TeacherManagementPage section="problems" /></TeacherRoute>} />
            <Route path="/app/teacher/manage/ai-library" element={<TeacherRoute><TeacherManagementPage section="ai-library" /></TeacherRoute>} />
            <Route path="/app/teacher/manage/system" element={<TeacherRoute><TeacherManagementPage section="system" /></TeacherRoute>} />
            <Route path="/app/teacher-management" element={<Navigate to="/app/teacher/manage/classes" replace />} />
            <Route path="/app/class-overview" element={<Navigate to="/app/teacher/classes" replace />} />
            <Route path="/app/task-editor" element={<TeacherRoute><TaskEditorPage /></TeacherRoute>} />
            <Route path="/app/teacher/assignment/new" element={<TeacherRoute><AssignmentCreatePage /></TeacherRoute>} />
            <Route path="/app/teacher/assignment/:assignmentId/problems/:problemId/students/:studentProfileId" element={<TeacherRoute><LegacyAssignmentRedirect level="student" /></TeacherRoute>} />
            <Route path="/app/teacher/assignment/:assignmentId/problems/:problemId" element={<TeacherRoute><LegacyAssignmentRedirect level="problem" /></TeacherRoute>} />
            <Route path="/app/teacher/assignment/:assignmentId" element={<TeacherRoute><LegacyAssignmentRedirect level="assignment" /></TeacherRoute>} />
            <Route path="*" element={<Navigate to="/app" replace />} />
          </Routes>
        </Suspense>
      </main>
    </div>
  );
}

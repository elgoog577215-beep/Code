import { lazy, ReactNode, Suspense, useEffect, useState } from "react";
import { NavLink, Navigate, Route, Routes, useLocation } from "react-router-dom";
import type { LucideIcon } from "lucide-react";
import {
  ArrowRight,
  BarChart3,
  BookOpenCheck,
  Box,
  CheckCircle2,
  Code2,
  Gauge,
  Lightbulb,
  LogIn,
  Menu,
  Moon,
  RotateCcw,
  Send,
  Sun,
  UserRound,
  UsersRound,
  X
} from "lucide-react";
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
    <div className="role-entry-page route-hub-page route-hub-page--experience">
      <section className="route-hub-experience" aria-labelledby="route-hub-title">
        <div className="route-hub-hero-copy">
          <p className="eyebrow">{t("routeHub.welcome")}</p>
          <h1 id="route-hub-title">
            {t("routeHub.headlineStart")}<br />
            {t("routeHub.headlineEnd")}
          </h1>
          <p className="route-hub-summary">{t("routeHub.summary")}</p>

          <nav className="route-hub-role-actions" aria-label={t("routeHub.roleAria")}>
            <NavLink to="/app/student" className="route-hub-role-action route-hub-role-action--primary">
              <BookOpenCheck size={19} aria-hidden="true" />
              <span>{t("routeHub.studentCta")}</span>
            </NavLink>
            <NavLink to="/app/teacher" className="route-hub-role-action">
              <UsersRound size={19} aria-hidden="true" />
              <span>{t("routeHub.teacherCta")}</span>
            </NavLink>
          </nav>

          <div className="route-hub-feature-list">
            <div>
              <Box size={18} aria-hidden="true" />
              <span><strong>{t("routeHub.features.languagesTitle")}</strong><small>{t("routeHub.features.languagesDetail")}</small></span>
            </div>
            <div>
              <Gauge size={18} aria-hidden="true" />
              <span><strong>{t("routeHub.features.feedbackTitle")}</strong><small>{t("routeHub.features.feedbackDetail")}</small></span>
            </div>
            <div>
              <BarChart3 size={18} aria-hidden="true" />
              <span><strong>{t("routeHub.features.dataTitle")}</strong><small>{t("routeHub.features.dataDetail")}</small></span>
            </div>
          </div>

          <p className="route-hub-service-status">
            <CheckCircle2 size={15} aria-hidden="true" />
            {t("routeHub.serviceStatus")}
          </p>
        </div>

        <section className="route-hub-code-stage" aria-label={t("routeHub.demo.aria")}>
          <header className="route-hub-code-stage__head">
            <span>{t("routeHub.demo.back")}</span>
            <strong>{t("routeHub.demo.problem")}</strong>
            <small>{t("routeHub.demo.difficulty")}</small>
            <span>{t("routeHub.demo.problemId")}</span>
            <span className="route-hub-code-stage__submit"><Send size={14} aria-hidden="true" />{t("routeHub.demo.submit")}</span>
          </header>

          <div className="route-hub-code-stage__body">
            <div className="route-hub-editor">
              <div className="route-hub-editor__toolbar"><span>Python 3</span><CheckCircle2 size={16} aria-hidden="true" /></div>
              <pre aria-label={t("routeHub.demo.codeAria")}><code>
                <span><i>1</i><b># {t("routeHub.demo.problem")}</b></span>
                <span><i>2</i><b># nums = [2, 7, 11, 15], target = 9</b></span>
                <span><i>3</i>&nbsp;</span>
                <span><i>4</i><em>def</em> two_sum(nums, target):</span>
                <span><i>5</i>    seen = &#123;&#125;</span>
                <span><i>6</i>    <em>for</em> i, num <em>in</em> enumerate(nums):</span>
                <span><i>7</i>        remain = target - num</span>
                <span><i>8</i>        <em>if</em> remain <em>in</em> seen:</span>
                <span><i>9</i>            <em>return</em> [seen[remain], i]</span>
                <span><i>10</i>       seen[num] = i</span>
              </code></pre>
              <div className="route-hub-console">
                <strong>{t("routeHub.demo.console")}</strong>
                <p><CheckCircle2 size={14} aria-hidden="true" />{t("routeHub.demo.caseOne")}</p>
                <p><CheckCircle2 size={14} aria-hidden="true" />{t("routeHub.demo.caseTwo")}</p>
              </div>
            </div>

            <aside className="route-hub-result">
              <div className="route-hub-result__tabs"><strong>{t("routeHub.demo.result")}</strong><span>{t("routeHub.demo.quality")}</span></div>
              <div className="route-hub-result__summary">
                <CheckCircle2 size={28} aria-hidden="true" />
                <span><strong>{t("routeHub.demo.passed")}</strong><small>{t("routeHub.demo.testCount")}</small></span>
                <span><small>{t("routeHub.demo.runtime")}</small><strong>32 ms</strong></span>
              </div>
              <div className="route-hub-result__scores">
                <strong>{t("routeHub.demo.scoreTitle")}</strong>
                <span>{t("routeHub.demo.correctness")}<b>100%</b></span>
                <span>{t("routeHub.demo.efficiency")}<b>100%</b></span>
                <span>{t("routeHub.demo.style")}<b>90%</b></span>
              </div>
              <div className="route-hub-result__feedback">
                <Lightbulb size={18} aria-hidden="true" />
                <span><strong>{t("routeHub.demo.feedbackTitle")}</strong><small>{t("routeHub.demo.feedbackDetail")}</small></span>
              </div>
            </aside>
          </div>
        </section>
      </section>

      <section className="route-hub-learning-loop" aria-labelledby="route-hub-loop-title">
        <h2 id="route-hub-loop-title">{t("routeHub.loop.title")}</h2>
        <div className="route-hub-loop-grid">
          <article className="route-hub-loop-step">
            <span className="route-hub-loop-step__number">1</span>
            <Code2 size={20} aria-hidden="true" />
            <div><strong>{t("routeHub.loop.practice")}</strong><small>{t("routeHub.loop.practiceDetail")}</small></div>
            <span className="route-hub-loop-step__evidence"><code>def two_sum(nums, target)</code></span>
          </article>
          <ArrowRight className="route-hub-loop-arrow" size={22} aria-hidden="true" />
          <article className="route-hub-loop-step">
            <span className="route-hub-loop-step__number">2</span>
            <CheckCircle2 size={20} aria-hidden="true" />
            <div><strong>{t("routeHub.loop.judge")}</strong><small>{t("routeHub.loop.judgeDetail")}</small></div>
            <span className="route-hub-loop-step__evidence"><CheckCircle2 size={14} aria-hidden="true" />{t("routeHub.loop.judgeEvidence")}<b>32 ms</b></span>
          </article>
          <ArrowRight className="route-hub-loop-arrow" size={22} aria-hidden="true" />
          <article className="route-hub-loop-step">
            <span className="route-hub-loop-step__number">3</span>
            <RotateCcw size={20} aria-hidden="true" />
            <div><strong>{t("routeHub.loop.review")}</strong><small>{t("routeHub.loop.reviewDetail")}</small></div>
            <span className="route-hub-loop-step__evidence"><strong>{t("routeHub.loop.reviewEvidence")}</strong><code>O(n)</code></span>
          </article>
        </div>
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

import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { NavLink, Navigate, Route, Routes, useLocation, useParams } from "react-router-dom";
import type { LucideIcon } from "lucide-react";
import { BookOpenCheck, LogIn, Menu, Moon, Sun, UserRound, UsersRound, X } from "lucide-react";
import TeacherPage from "./features/teacher/TeacherPage";
import TeacherAuthGate from "./features/teacher/TeacherAuthGate";
import { clearActiveStudent, loadStudent } from "./shared/storage";
import { Button } from "./shared/ui/Button";
import { EmptyState } from "./shared/ui/EmptyState";

const StudentPage = lazy(() => import("./features/student/StudentPage"));
const StudentLoginPage = lazy(() => import("./features/student/StudentLoginPage"));
const StudentAssignmentPage = lazy(() => import("./features/student/StudentAssignmentPage"));
const ProblemPage = lazy(() => import("./features/problem/ProblemPage"));
const AssignmentDetailPage = lazy(() => import("./features/teacher/AssignmentDetailPage"));
const AssignmentCreatePage = lazy(() => import("./features/teacher/AssignmentCreatePage"));
const TaskEditorPage = lazy(() => import("./features/task-editor/TaskEditorPage"));
const ClassOverviewPage = lazy(() => import("./features/insights/ClassOverviewPage"));

type Theme = "light" | "dark";
type NavItem = {
  to: string;
  label: string;
  icon: LucideIcon;
  activeWhen?: (pathname: string) => boolean;
  noActive?: boolean;
};

function useTheme() {
  const [theme, setTheme] = useState<Theme>(() => {
    const saved = localStorage.getItem("wzai:theme");
    return saved === "dark" ? "dark" : "light";
  });

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("wzai:theme", theme);
  }, [theme]);

  return { theme, setTheme };
}

function LegacyRedirect() {
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const id = params.get("id");
  const search = new URLSearchParams(location.search);
  search.delete("id");
  const suffix = search.toString() ? `?${search.toString()}` : "";

  if (location.pathname.endsWith("problem.html") && id) {
    return <Navigate to={`/app/problem/${id}${suffix}`} replace />;
  }
  if (location.pathname.endsWith("student.html")) {
    return <Navigate to={`/app/student${location.search}`} replace />;
  }
  if (location.pathname.endsWith("teacher.html")) {
    return <Navigate to="/app/teacher" replace />;
  }
  if (location.pathname.endsWith("problem-create.html")) {
    return <Navigate to={`/app/task-editor${location.search}`} replace />;
  }
  if (location.pathname.endsWith("leaderboard.html")) {
    return <Navigate to="/app/class-overview" replace />;
  }
  return <Navigate to="/app" replace />;
}

function RouteHubPage() {
  return (
    <div className="role-entry-page route-hub-page">
      <section className="role-entry route-hub-hero">
        <div>
          <p className="eyebrow">入口</p>
          <h1>选择身份</h1>
          <p>学生写题，老师管课。其他管理能力都放进教师端里。</p>
        </div>
      </section>

      <section className="role-entry-grid route-hub-grid" aria-label="系统入口">
        <NavLink to="/app/student" className="role-entry-card route-hub-card route-hub-card--student">
          <span className="role-entry-card__icon">
            <BookOpenCheck size={24} />
          </span>
          <span>
            <span className="role-entry-card__head">
              <span className="eyebrow">学生</span>
            </span>
            <h2>学生练习</h2>
            <p>进入课堂作业、公共题库和个人提交记录。</p>
            <small>/app/student</small>
          </span>
        </NavLink>

        <NavLink to="/app/teacher" className="role-entry-card route-hub-card route-hub-card--teacher">
          <span className="role-entry-card__icon">
            <UsersRound size={24} />
          </span>
          <span>
            <span className="role-entry-card__head">
              <span className="eyebrow">教师</span>
            </span>
            <h2>作业中心</h2>
            <p>管理作业列表、新建作业，并进入作业详情。</p>
            <small>/app/teacher</small>
          </span>
        </NavLink>
      </section>
    </div>
  );
}

function LegacyProblemRedirect() {
  const { problemId } = useParams();
  return <Navigate to={`/app/problem/${problemId}`} replace />;
}

function LegacyAssignmentRedirect() {
  const { assignmentId } = useParams();
  return <Navigate to={`/app/teacher/assignment/${assignmentId}`} replace />;
}

function Header() {
  const { theme, setTheme } = useTheme();
  const [open, setOpen] = useState(false);
  const [student, setStudent] = useState(() => loadStudent());
  const location = useLocation();
  const isProblemPage =
    location.pathname.startsWith("/app/problem") ||
    location.pathname.startsWith("/problem/") ||
    /^\/app\/student\/assignments\/[^/]+\/problems\//.test(location.pathname);
  const isStudentContext = location.pathname.startsWith("/app/student") || isProblemPage || location.pathname.startsWith("/student");
  const isTeacherContext =
    location.pathname.startsWith("/app/teacher") ||
    location.pathname.startsWith("/teacher") ||
    location.pathname.startsWith("/app/teacher-management") ||
    location.pathname.startsWith("/teacher-management") ||
    location.pathname.startsWith("/app/task-editor") ||
    location.pathname.startsWith("/task-editor") ||
    location.pathname.startsWith("/app/class-overview") ||
    location.pathname.startsWith("/class-overview");
  const isCatalogContext =
    location.pathname.startsWith("/app/student/problems") ||
    location.pathname.startsWith("/app/student/assignments/public") ||
    location.pathname.startsWith("/app/problems") ||
    location.pathname === "/problems";
  const navItems = useMemo<NavItem[]>(
    () => {
      const teacherItem: NavItem = {
        to: "/app/teacher",
        label: "教师端",
        icon: UsersRound,
        activeWhen: (pathname: string) =>
          pathname.startsWith("/app/teacher") ||
          pathname.startsWith("/teacher") ||
          pathname.startsWith("/app/teacher-management") ||
          pathname.startsWith("/teacher-management") ||
          pathname.startsWith("/app/task-editor") ||
          pathname.startsWith("/task-editor") ||
          pathname.startsWith("/app/class-overview") ||
          pathname.startsWith("/class-overview")
      };
      if (isStudentContext || isCatalogContext) {
        return [];
      }
      if (isTeacherContext) {
        return [teacherItem];
      }
      return [
        { to: "/app/student", label: "学生端", icon: BookOpenCheck },
        teacherItem
      ];
    },
    [isCatalogContext, isStudentContext, isTeacherContext]
  );

  useEffect(() => {
    setOpen(false);
    setStudent(loadStudent());
  }, [location.pathname]);

  function signOut() {
    clearActiveStudent();
    setStudent(null);
  }

  const hasNav = navItems.length > 0;
  const headerClassName = [
    "app-header",
    open && hasNav ? "is-open" : "",
    isProblemPage ? "app-header--practice" : "",
    hasNav ? "" : "app-header--no-nav"
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <header className={headerClassName}>
      <NavLink to="/app" className="brand" aria-label="温中编程学习平台">
        <span className="brand__mark">
          <BookOpenCheck size={24} />
        </span>
        <span>
          <strong>温中编程学习平台</strong>
        </span>
      </NavLink>
      {hasNav ? (
        <>
          <button type="button" className="nav-toggle" aria-label={open ? "收起导航" : "展开导航"} onClick={() => setOpen(value => !value)}>
            {open ? <X size={19} /> : <Menu size={19} />}
          </button>
          <nav className="top-nav" aria-label="主导航">
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
                退出
              </Button>
            </div>
          ) : (
            <NavLink to="/app/student/login" className="header-login-link">
              <LogIn size={17} />
              <span>登录</span>
            </NavLink>
          )
        ) : null}
        <button
          type="button"
          className="theme-toggle"
          aria-label="切换主题"
          onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
        >
          {theme === "dark" ? <Sun size={18} /> : <Moon size={18} />}
          <span>{theme === "dark" ? "白天" : "夜间"}</span>
        </button>
      </div>
    </header>
  );
}

export default function App() {
  return (
    <div className="app-shell">
      <Header />
      <main className="main-shell">
        <Suspense fallback={<EmptyState title="正在加载页面" />}>
          <Routes>
            <Route path="/" element={<Navigate to="/app" replace />} />
            <Route path="/app" element={<RouteHubPage />} />
            <Route path="/app/" element={<RouteHubPage />} />
            <Route path="/app/problems" element={<Navigate to="/app/student/assignments/public" replace />} />
            <Route path="/app/student/problems" element={<Navigate to="/app/student/assignments/public" replace />} />
            <Route path="/app/student" element={<StudentPage />} />
            <Route path="/app/student/login" element={<StudentLoginPage />} />
            <Route path="/app/student/assignments/:assignmentId" element={<StudentAssignmentPage />} />
            <Route path="/app/student/assignments/:assignmentId/problems/:problemId" element={<ProblemPage />} />
            <Route path="/app/teacher" element={<TeacherAuthGate><TeacherPage /></TeacherAuthGate>} />
            <Route path="/app/teacher/assignment/new" element={<TeacherAuthGate><AssignmentCreatePage /></TeacherAuthGate>} />
            <Route path="/app/teacher/assignment/:assignmentId/problems/:problemId/students/:studentProfileId" element={<TeacherAuthGate><AssignmentDetailPage /></TeacherAuthGate>} />
            <Route path="/app/teacher/assignment/:assignmentId/problems/:problemId" element={<TeacherAuthGate><AssignmentDetailPage /></TeacherAuthGate>} />
            <Route path="/app/teacher/assignment/:assignmentId" element={<TeacherAuthGate><AssignmentDetailPage /></TeacherAuthGate>} />
            <Route path="/app/teacher-management" element={<Navigate to="/app/teacher?manage=1" replace />} />
            <Route path="/app/task-editor" element={<TeacherAuthGate><TaskEditorPage /></TeacherAuthGate>} />
            <Route path="/app/class-overview" element={<TeacherAuthGate><ClassOverviewPage /></TeacherAuthGate>} />
            <Route path="/app/problem/:problemId" element={<ProblemPage />} />
            <Route path="/problems" element={<Navigate to="/app/student/assignments/public" replace />} />
            <Route path="/student" element={<Navigate to="/app/student" replace />} />
            <Route path="/problem/:problemId" element={<LegacyProblemRedirect />} />
            <Route path="/teacher" element={<Navigate to="/app/teacher" replace />} />
            <Route path="/teacher/assignment/new" element={<Navigate to="/app/teacher/assignment/new" replace />} />
            <Route path="/teacher/assignment/:assignmentId" element={<LegacyAssignmentRedirect />} />
            <Route path="/teacher-management" element={<Navigate to="/app/teacher?manage=1" replace />} />
            <Route path="/task-editor" element={<Navigate to="/app/task-editor" replace />} />
            <Route path="/class-overview" element={<Navigate to="/app/class-overview" replace />} />
            <Route path="/student.html" element={<LegacyRedirect />} />
            <Route path="/teacher.html" element={<LegacyRedirect />} />
            <Route path="/problem.html" element={<LegacyRedirect />} />
            <Route path="/problem-create.html" element={<LegacyRedirect />} />
            <Route path="/leaderboard.html" element={<LegacyRedirect />} />
            <Route path="*" element={<Navigate to="/app" replace />} />
          </Routes>
        </Suspense>
      </main>
    </div>
  );
}

import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { NavLink, Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { BookOpenCheck, GraduationCap, KeyRound, Menu, Moon, Sun, UsersRound, X } from "lucide-react";
import TeacherManagementPage from "./features/teacher/TeacherManagementPage";
import { EmptyState } from "./shared/ui/EmptyState";
import { getStoredRole } from "./features/home/WelcomePage";

const WelcomePage = lazy(() => import("./features/home/WelcomePage"));
const HomePage = lazy(() => import("./features/home/HomePage"));
const StudentHome = lazy(() => import("./features/student/StudentHome"));
const StudentPage = lazy(() => import("./features/student/StudentPage"));
const TeacherPage = lazy(() => import("./features/teacher/TeacherPage"));
const TeacherDashboard = lazy(() => import("./features/teacher/TeacherDashboard"));
const AssignmentDetail = lazy(() => import("./features/teacher/AssignmentDetail"));
const ProblemPage = lazy(() => import("./features/problem/ProblemPage"));
const TaskEditorPage = lazy(() => import("./features/task-editor/TaskEditorPage"));
const ClassOverviewPage = lazy(() => import("./features/insights/ClassOverviewPage"));
const InviteCodeModal = lazy(() => import("./shared/ui/InviteCodeModal"));

type Theme = "light" | "dark";

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
    return <Navigate to={`/problem/${id}${suffix}`} replace />;
  }
  if (location.pathname.endsWith("student.html")) {
    return <Navigate to="/student" replace />;
  }
  if (location.pathname.endsWith("teacher.html")) {
    return <Navigate to="/teacher" replace />;
  }
  if (location.pathname.endsWith("problem-create.html")) {
    return <Navigate to={`/task-editor${location.search}`} replace />;
  }
  if (location.pathname.endsWith("leaderboard.html")) {
    return <Navigate to="/class-overview" replace />;
  }
  return <Navigate to="/" replace />;
}

function Header({ onOpenInvite, role }: { onOpenInvite: () => void; role: string | null }) {
  const { theme, setTheme } = useTheme();
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  const isWelcome = location.pathname === "/";
  const isStudent = role === "student" || location.pathname.startsWith("/problem") || location.pathname === "/problems";
  const isTeacher = role === "teacher" || location.pathname.startsWith("/teacher");

  const navItems = useMemo(() => {
    if (isWelcome) return [];
    const items: Array<{ to: string; label: string; icon: typeof BookOpenCheck; activeWhen?: (p: string) => boolean }> = [];
    items.push({ to: "/problems", label: "题库", icon: BookOpenCheck, activeWhen: (p: string) => p === "/problems" });
    if (!role || role === "student") {
      items.push({ to: "/student", label: "学生", icon: GraduationCap, activeWhen: (p: string) => p === "/student" || p.startsWith("/problem") });
    }
    if (!role || role === "teacher") {
      items.push({
        to: "/teacher",
        label: "教师",
        icon: UsersRound,
        activeWhen: (p: string) => p.startsWith("/teacher") || p.startsWith("/teacher-management") || p.startsWith("/task-editor")
      });
    }
    return items;
  }, [isWelcome, role]);

  useEffect(() => {
    setOpen(false);
  }, [location.pathname]);

  function switchRole() {
    localStorage.removeItem("wzai:role");
    navigate("/");
  }

  return (
    <header className={`app-header ${open ? "is-open" : ""}`}>
      <NavLink to={role ? (role === "student" ? "/student" : "/teacher") : "/"} className="brand" aria-label="温中编程学习平台">
        <span className="brand__mark">
          <BookOpenCheck size={24} />
        </span>
        <span>
          <strong>温中编程学习平台</strong>
        </span>
      </NavLink>
      {!isWelcome && (
        <>
          <button type="button" className="nav-toggle" aria-label={open ? "收起导航" : "展开导航"} onClick={() => setOpen(v => !v)}>
            {open ? <X size={19} /> : <Menu size={19} />}
          </button>
          <nav className="top-nav" aria-label="主导航">
            {navItems.map(item => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === "/problems"}
                className={({ isActive }) =>
                  isActive || item.activeWhen?.(location.pathname) ? "top-nav__link is-active" : "top-nav__link"
                }
              >
                <item.icon size={17} />
                <span>{item.label}</span>
              </NavLink>
            ))}
            {role !== "teacher" && (
              <button type="button" className="top-nav__link" onClick={onOpenInvite} title="输入教师邀请码">
                <KeyRound size={17} />
                <span>邀请码</span>
              </button>
            )}
            {role && (
              <button type="button" className="top-nav__link" onClick={switchRole} title="切换身份">
                <span>🔄</span>
                <span>切换</span>
              </button>
            )}
          </nav>
        </>
      )}
      <button
        type="button"
        className="theme-toggle"
        aria-label="切换主题"
        onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
      >
        {theme === "dark" ? <Sun size={18} /> : <Moon size={18} />}
        <span>{theme === "dark" ? "白天" : "夜间"}</span>
      </button>
    </header>
  );
}

function AppContent() {
  const [inviteOpen, setInviteOpen] = useState(false);
  const location = useLocation();
  const role = getStoredRole();

  // Redirect from "/" if role already selected
  if (location.pathname === "/" && role) {
    return <Navigate to={role === "student" ? "/student" : "/teacher"} replace />;
  }

  return (
    <div className="app-shell">
      <Header onOpenInvite={() => setInviteOpen(true)} role={role} />
      <main className="main-shell">
        <Suspense fallback={<EmptyState title="正在加载页面" />}>
          <Routes>
            {/* Welcome & Public */}
            <Route path="/" element={<WelcomePage />} />
            <Route path="/problems" element={<HomePage />} />
            <Route path="/problem/:problemId" element={<ProblemPage />} />

            {/* Student */}
            <Route path="/student" element={<StudentHome />} />

            {/* Teacher */}
            <Route path="/teacher" element={<TeacherDashboard />} />
            <Route path="/teacher/assignment/:id" element={<AssignmentDetail />} />
            <Route path="/teacher/classes" element={<TeacherManagementPage />} />
            <Route path="/teacher/problems" element={<TeacherManagementPage />} />
            <Route path="/teacher/system" element={<TeacherManagementPage />} />
            <Route path="/teacher-management" element={<TeacherManagementPage />} />
            <Route path="/task-editor" element={<TaskEditorPage />} />
            <Route path="/class-overview" element={<ClassOverviewPage />} />

            {/* Legacy */}
            <Route path="/app" element={<Navigate to="/" replace />} />
            <Route path="/app/student" element={<StudentPage />} />
            <Route path="/app/teacher" element={<TeacherPage />} />
            <Route path="/app/teacher-management" element={<TeacherManagementPage />} />
            <Route path="/app/task-editor" element={<TaskEditorPage />} />
            <Route path="/app/class-overview" element={<ClassOverviewPage />} />
            <Route path="/app/problem/:problemId" element={<ProblemPage />} />
            <Route path="/student.html" element={<LegacyRedirect />} />
            <Route path="/teacher.html" element={<LegacyRedirect />} />
            <Route path="/problem.html" element={<LegacyRedirect />} />
            <Route path="/problem-create.html" element={<LegacyRedirect />} />
            <Route path="/leaderboard.html" element={<LegacyRedirect />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </main>
      <Suspense>
        {inviteOpen && <InviteCodeModal onClose={() => setInviteOpen(false)} />}
      </Suspense>
    </div>
  );
}

export default function App() {
  return <AppContent />;
}
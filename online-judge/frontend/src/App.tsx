import { lazy, Suspense, useEffect, useMemo, useState } from "react";
import { NavLink, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { BookOpenCheck, GraduationCap, Menu, Moon, Settings, Sun, UsersRound, X } from "lucide-react";
import TeacherPage from "./features/teacher/TeacherPage";
import TeacherManagementPage from "./features/teacher/TeacherManagementPage";
import { EmptyState } from "./shared/ui/EmptyState";

const RoleEntryPage = lazy(() => import("./features/home/RoleEntryPage"));
const StudentPage = lazy(() => import("./features/student/StudentPage"));
const ProblemPage = lazy(() => import("./features/problem/ProblemPage"));
const TaskEditorPage = lazy(() => import("./features/task-editor/TaskEditorPage"));
const ClassOverviewPage = lazy(() => import("./features/insights/ClassOverviewPage"));

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
  return <Navigate to="/app/student" replace />;
}

function Header() {
  const { theme, setTheme } = useTheme();
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const navItems = useMemo(
    () => [
      { to: "/app/student", label: "学生任务", icon: GraduationCap },
      { to: "/app/teacher", label: "教师工作台", icon: UsersRound },
      { to: "/app/teacher-management", label: "教师管理", icon: Settings }
    ],
    []
  );

  useEffect(() => {
    setOpen(false);
  }, [location.pathname]);

  return (
    <header className={`app-header ${open ? "is-open" : ""}`}>
      <NavLink to="/app" className="brand" aria-label="温中编程学习平台">
        <span className="brand__mark">
          <BookOpenCheck size={24} />
        </span>
        <span>
          <strong>温中编程学习平台</strong>
          <small>课堂作业与过程记录</small>
        </span>
      </NavLink>
      <button type="button" className="nav-toggle" aria-label={open ? "收起导航" : "展开导航"} onClick={() => setOpen(value => !value)}>
        {open ? <X size={19} /> : <Menu size={19} />}
      </button>
      <nav className="top-nav" aria-label="主导航">
        {navItems.map(item => (
          <NavLink key={item.to} to={item.to} className={({ isActive }) => (isActive ? "top-nav__link is-active" : "top-nav__link")}>
            <item.icon size={17} />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
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

export default function App() {
  return (
    <div className="app-shell">
      <Header />
      <main className="main-shell">
        <Suspense fallback={<EmptyState title="正在加载页面" />}>
          <Routes>
            <Route path="/" element={<RoleEntryPage />} />
            <Route path="/app" element={<RoleEntryPage />} />
            <Route path="/app/student" element={<StudentPage />} />
            <Route path="/app/teacher" element={<TeacherPage />} />
            <Route path="/app/teacher-management" element={<TeacherManagementPage />} />
            <Route path="/app/task-editor" element={<TaskEditorPage />} />
            <Route path="/app/class-overview" element={<ClassOverviewPage />} />
            <Route path="/app/problem/:problemId" element={<ProblemPage />} />
            <Route path="/student" element={<StudentPage />} />
            <Route path="/problem/:problemId" element={<ProblemPage />} />
            <Route path="/teacher" element={<TeacherPage />} />
            <Route path="/teacher-management" element={<TeacherManagementPage />} />
            <Route path="/task-editor" element={<TaskEditorPage />} />
            <Route path="/class-overview" element={<ClassOverviewPage />} />
            <Route path="/student.html" element={<LegacyRedirect />} />
            <Route path="/teacher.html" element={<LegacyRedirect />} />
            <Route path="/problem.html" element={<LegacyRedirect />} />
            <Route path="/problem-create.html" element={<LegacyRedirect />} />
            <Route path="/leaderboard.html" element={<LegacyRedirect />} />
            <Route path="*" element={<Navigate to="/app/student" replace />} />
          </Routes>
        </Suspense>
      </main>
    </div>
  );
}

import { lazy, ReactNode, Suspense, useEffect, useMemo, useState } from "react";
import { NavLink, Navigate, Route, Routes, useLocation } from "react-router-dom";
import type { LucideIcon } from "lucide-react";
import { BookOpenCheck, LogIn, Menu, Moon, Sun, UserRound, UsersRound, X } from "lucide-react";
import TeacherPage from "./features/teacher/TeacherPage";
import TeacherAuthGate from "./features/teacher/TeacherAuthGate";
import { TeacherShell } from "./features/teacher/TeacherShell";
import { clearActiveStudent, loadStudent } from "./shared/storage";
import { Button } from "./shared/ui/Button";
import { EmptyState } from "./shared/ui/EmptyState";

const StudentPage = lazy(() => import("./features/student/StudentPage"));
const StudentLoginPage = lazy(() => import("./features/student/StudentLoginPage"));
const StudentAssignmentPage = lazy(() => import("./features/student/StudentAssignmentPage"));
const ProblemPage = lazy(() => import("./features/problem/ProblemPage"));
const AssignmentDetailPage = lazy(() => import("./features/teacher/AssignmentDetailPage"));
const AssignmentCreatePage = lazy(() => import("./features/teacher/AssignmentCreatePage"));
const TeacherManagementPage = lazy(() => import("./features/teacher/TeacherManagementPage"));
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

function RouteHubPage() {
  return (
    <div className="role-entry-page route-hub-page">
      <section className="role-entry route-hub-hero">
        <div>
          <p className="eyebrow">入口</p>
          <h1>学习平台</h1>
          <p>学生学习和教师工作台各走自己的路径，入口清楚，任务直接。</p>
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
            <h2>学生学习</h2>
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
            <h2>教师工作台</h2>
            <p>查看班级、作业、题目和学生，也管理名单、题库与 AI 标准库。</p>
            <small>/app/teacher</small>
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

function Header() {
  const { theme, setTheme } = useTheme();
  const [open, setOpen] = useState(false);
  const [student, setStudent] = useState(() => loadStudent());
  const location = useLocation();
  const isProblemPage =
    /^\/app\/student\/assignments\/[^/]+\/problems\//.test(location.pathname);
  const isStudentContext = location.pathname.startsWith("/app/student") || isProblemPage;
  const isTeacherContext = location.pathname.startsWith("/app/teacher");
  const navItems = useMemo<NavItem[]>(
    () => {
      const teacherItem: NavItem = {
        to: "/app/teacher",
        label: "教师端",
        icon: UsersRound,
        activeWhen: (pathname: string) => pathname.startsWith("/app/teacher")
      };
      if (isStudentContext) {
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
    [isStudentContext, isTeacherContext]
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
          <button
            type="button"
            className="nav-toggle"
            aria-label={open ? "收起导航" : "展开导航"}
            aria-expanded={open}
            aria-controls="main-navigation"
            onClick={() => setOpen(value => !value)}
          >
            {open ? <X size={19} /> : <Menu size={19} />}
          </button>
          <nav className="top-nav" id="main-navigation" aria-label="主导航">
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
          title={theme === "dark" ? "切换到白天模式" : "切换到夜间模式"}
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
            <Route path="/app/student" element={<StudentPage />} />
            <Route path="/app/student/login" element={<StudentLoginPage />} />
            <Route path="/app/student/assignments/:assignmentId" element={<StudentAssignmentPage />} />
            <Route path="/app/student/assignments/:assignmentId/problems/:problemId" element={<ProblemPage />} />
            <Route path="/app/teacher" element={<TeacherRoute><TeacherPage /></TeacherRoute>} />
            <Route path="/app/teacher/classes" element={<TeacherRoute><ClassOverviewPage /></TeacherRoute>} />
            <Route path="/app/teacher/manage" element={<TeacherRoute><TeacherManagementPage section="home" /></TeacherRoute>} />
            <Route path="/app/teacher/manage/classes" element={<TeacherRoute><TeacherManagementPage section="classes" /></TeacherRoute>} />
            <Route path="/app/teacher/manage/problems" element={<TeacherRoute><TeacherManagementPage section="problems" /></TeacherRoute>} />
            <Route path="/app/teacher/manage/ai-library" element={<TeacherRoute><TeacherManagementPage section="ai-library" /></TeacherRoute>} />
            <Route path="/app/teacher/assignment/new" element={<TeacherRoute><AssignmentCreatePage /></TeacherRoute>} />
            <Route path="/app/teacher/assignment/:assignmentId/problems/:problemId/students/:studentProfileId" element={<TeacherRoute><AssignmentDetailPage /></TeacherRoute>} />
            <Route path="/app/teacher/assignment/:assignmentId/problems/:problemId" element={<TeacherRoute><AssignmentDetailPage /></TeacherRoute>} />
            <Route path="/app/teacher/assignment/:assignmentId" element={<TeacherRoute><AssignmentDetailPage /></TeacherRoute>} />
            <Route path="*" element={<Navigate to="/app" replace />} />
          </Routes>
        </Suspense>
      </main>
    </div>
  );
}

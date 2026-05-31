import { GraduationCap, UsersRound } from "lucide-react";
import { useNavigate } from "react-router-dom";

type Role = "student" | "teacher";

function getStoredRole(): Role | null {
  const stored = localStorage.getItem("wzai:role");
  return stored === "student" || stored === "teacher" ? stored : null;
}

function storeRole(role: Role) {
  localStorage.setItem("wzai:role", role);
}

export { getStoredRole, storeRole };
export type { Role };

export default function WelcomePage() {
  const navigate = useNavigate();

  function selectRole(role: Role) {
    storeRole(role);
    navigate(role === "student" ? "/student" : "/teacher");
  }

  const savedRole = getStoredRole();

  return (
    <div className="stack welcome-page">
      <section className="welcome-hero">
        <div>
          <h1>温中编程学习平台</h1>
          <p>选择你的身份开始学习或教学</p>
        </div>
      </section>

      <section className="welcome-grid">
        <button type="button" className="welcome-card" onClick={() => selectRole("student")}>
          <div className="welcome-card__icon">
            <GraduationCap size={48} />
          </div>
          <div>
            <h2>我是学生</h2>
            <p>浏览题库、练习编程、查看反馈</p>
          </div>
          <span className="welcome-card__hint">
            {savedRole === "student" ? "最近选择" : "开始学习"}
          </span>
        </button>

        <button type="button" className="welcome-card" onClick={() => selectRole("teacher")}>
          <div className="welcome-card__icon">
            <UsersRound size={48} />
          </div>
          <div>
            <h2>我是教师</h2>
            <p>管理班级、布置作业、查看学情</p>
          </div>
          <span className="welcome-card__hint">
            {savedRole === "teacher" ? "最近选择" : "管理工具"}
          </span>
        </button>
      </section>

      <section className="welcome-footer">
        <button type="button" className="welcome-browse-link" onClick={() => navigate("/problems")}>
          或者直接浏览公共题库
        </button>
      </section>
    </div>
  );
}
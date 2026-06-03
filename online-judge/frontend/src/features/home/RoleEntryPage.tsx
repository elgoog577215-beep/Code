import { ArrowRight, BookOpen, GraduationCap, KeyRound, UsersRound } from "lucide-react";
import { ButtonLink } from "../../shared/ui/Button";
import { StatusPill } from "../../shared/ui/StatusPill";

export default function RoleEntryPage() {
  return (
    <div className="stack role-entry-page">
      <section className="role-entry role-entry--compact">
        <div>
          <p className="eyebrow">课堂工具</p>
          <h1>温中编程学习平台</h1>
          <p>选择身份或直接浏览题库，先进入最接近当前任务的地方。</p>
        </div>
        <ButtonLink to="/app/student" variant="secondary" icon={<KeyRound size={17} />}>
          输入邀请码
        </ButtonLink>
      </section>

      <section className="role-entry-grid role-entry-grid--primary" aria-label="使用入口">
        <article className="role-entry-card role-entry-card--primary role-entry-card--student">
          <span className="role-entry-card__icon">
            <GraduationCap size={28} />
          </span>
          <div>
            <div className="role-entry-card__head">
              <span className="eyebrow">学生</span>
              <StatusPill tone="info">课堂作业</StatusPill>
            </div>
            <h2>输入邀请码，继续当前作业</h2>
            <p>身份确认、下一题、题目状态放在同一页，先做最该做的题。</p>
          </div>
          <ButtonLink to="/app/student" variant="primary" icon={<KeyRound size={16} />}>
            进入学生端
          </ButtonLink>
        </article>

        <article className="role-entry-card role-entry-card--primary role-entry-card--teacher">
          <span className="role-entry-card__icon">
            <UsersRound size={28} />
          </span>
          <div>
            <div className="role-entry-card__head">
              <span className="eyebrow">教师</span>
              <StatusPill tone="success">工作台</StatusPill>
            </div>
            <h2>先看课堂过程，再处理细节</h2>
            <p>作业选择、课堂状态、需关注学生和高频问题集中在首屏。</p>
          </div>
          <ButtonLink to="/app/teacher" variant="secondary" icon={<ArrowRight size={16} />}>
            进入教师端
          </ButtonLink>
        </article>
      </section>

      <section className="role-entry-secondary">
        <ButtonLink to="/app/problems" variant="secondary" icon={<BookOpen size={17} />}>
          浏览公共题库
        </ButtonLink>
      </section>
    </div>
  );
}

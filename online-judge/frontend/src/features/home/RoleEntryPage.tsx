import { BookOpen, GraduationCap, KeyRound, UsersRound } from "lucide-react";
import { ButtonLink } from "../../shared/ui/Button";
import { Panel } from "../../shared/ui/Panel";
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
        <Panel
          title="学生"
          description="输入老师的邀请码，确认身份后继续当前作业。"
          action={<StatusPill tone="info">课堂作业</StatusPill>}
        >
          <div className="role-entry-card role-entry-card--primary">
            <GraduationCap size={28} />
            <div>
              <strong>下一步最清楚</strong>
              <span>邀请码、身份、当前要做的题集中在一页。</span>
            </div>
            <ButtonLink to="/app/student" variant="primary" icon={<KeyRound size={16} />}>
              进入学生端
            </ButtonLink>
          </div>
        </Panel>

        <Panel
          title="教师"
          description="查看作业、课堂状态、共性问题和需要关注的学生。"
          action={<StatusPill tone="success">工作台</StatusPill>}
        >
          <div className="role-entry-card role-entry-card--primary">
            <UsersRound size={28} />
            <div>
              <strong>先看课堂过程</strong>
              <span>总览看趋势，单作业页处理细节。</span>
            </div>
            <ButtonLink to="/app/teacher" variant="primary">
              进入教师端
            </ButtonLink>
          </div>
        </Panel>
      </section>

      <section className="role-entry-secondary">
        <ButtonLink to="/app/problems" variant="secondary" icon={<BookOpen size={17} />}>
          浏览公共题库
        </ButtonLink>
      </section>
    </div>
  );
}

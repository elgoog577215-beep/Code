import { ChartNoAxesColumnIncreasing, GraduationCap, Settings, UsersRound } from "lucide-react";
import { ButtonLink } from "../../shared/ui/Button";
import { Panel } from "../../shared/ui/Panel";
import { StatusPill } from "../../shared/ui/StatusPill";

export default function RoleEntryPage() {
  return (
    <div className="stack role-entry-page">
      <section className="role-entry role-entry--compact">
        <div>
          <h1>选择入口</h1>
        </div>
      </section>

      <section className="role-entry-grid role-entry-grid--primary" aria-label="使用入口">
        <Panel title="学生任务" action={<StatusPill tone="info">课堂作业</StatusPill>}>
          <div className="role-entry-card">
            <GraduationCap size={28} />
            <ButtonLink to="/app/student" variant="primary">
              进入
            </ButtonLink>
          </div>
        </Panel>

        <Panel title="教师工作台" action={<StatusPill tone="success">课堂过程</StatusPill>}>
          <div className="role-entry-card">
            <UsersRound size={28} />
            <div className="actions">
              <ButtonLink to="/app/teacher" variant="primary">
                进入
              </ButtonLink>
              <ButtonLink to="/app/teacher-management" variant="secondary" icon={<Settings size={17} />}>
                管理
              </ButtonLink>
            </div>
          </div>
        </Panel>
      </section>

      <section className="role-entry-secondary" aria-label="其他入口">
        <ButtonLink to="/app/class-overview" variant="secondary" icon={<ChartNoAxesColumnIncreasing size={17} />}>
          班级概览
        </ButtonLink>
        <ButtonLink to="/app/teacher-management" variant="ghost" icon={<Settings size={17} />}>
          教师管理
        </ButtonLink>
      </section>
    </div>
  );
}

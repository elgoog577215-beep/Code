import { ChartNoAxesColumnIncreasing, GraduationCap, Settings, UsersRound } from "lucide-react";
import { ButtonLink } from "../../shared/ui/Button";
import { Panel } from "../../shared/ui/Panel";
import { StatusPill } from "../../shared/ui/StatusPill";

export default function RoleEntryPage() {
  return (
    <div className="stack role-entry-page">
      <section className="role-entry role-entry--compact">
        <div>
          <h1>温中编程学习平台</h1>
        </div>
      </section>

      <section className="role-entry-grid" aria-label="使用入口">
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

        <Panel title="班级概览" action={<StatusPill tone="neutral">数据</StatusPill>}>
          <div className="role-entry-card">
            <ChartNoAxesColumnIncreasing size={28} />
            <ButtonLink to="/app/class-overview" variant="secondary">
              进入
            </ButtonLink>
          </div>
        </Panel>
      </section>
    </div>
  );
}

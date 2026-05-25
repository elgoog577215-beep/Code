import { GraduationCap, UsersRound } from "lucide-react";
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
        <Panel title="学生" action={<StatusPill tone="info">课堂作业</StatusPill>}>
          <div className="role-entry-card">
            <GraduationCap size={28} />
            <ButtonLink to="/app/student" variant="primary">
              进入
            </ButtonLink>
          </div>
        </Panel>

        <Panel title="教师" action={<StatusPill tone="success">工作台</StatusPill>}>
          <div className="role-entry-card">
            <UsersRound size={28} />
            <ButtonLink to="/app/teacher" variant="primary">
              进入
            </ButtonLink>
          </div>
        </Panel>
      </section>
    </div>
  );
}

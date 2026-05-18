import { ChartNoAxesColumnIncreasing, GraduationCap, Settings, UsersRound } from "lucide-react";
import { ButtonLink } from "../../shared/ui/Button";
import { Panel } from "../../shared/ui/Panel";

export default function RoleEntryPage() {
  return (
    <div className="stack role-entry-page">
      <section className="role-entry">
        <div>
          <p className="eyebrow">温中编程学习平台</p>
          <h1>选择使用身份</h1>
          <p>学生进入作业，教师查看班级学习情况。</p>
        </div>
      </section>

      <section className="role-entry-grid" aria-label="使用入口">
        <Panel title="学生" eyebrow="课堂作业" description="输入老师提供的邀请码，确认身份后开始练习。">
          <div className="role-entry-card">
            <GraduationCap size={28} />
            <ButtonLink to="/app/student" variant="primary">
              进入学生任务
            </ButtonLink>
          </div>
        </Panel>

        <Panel title="教师" eyebrow="教学管理" description="查看作业进度、班级情况和需要关注的学生。">
          <div className="role-entry-card">
            <UsersRound size={28} />
            <div className="actions">
              <ButtonLink to="/app/teacher" variant="primary">
                教师工作台
              </ButtonLink>
              <ButtonLink to="/app/teacher-management" variant="secondary" icon={<Settings size={17} />}>
                教师管理
              </ButtonLink>
            </div>
          </div>
        </Panel>

        <Panel title="班级概览" eyebrow="学习数据" description="查看题目提交、通过率和讲评参考。">
          <div className="role-entry-card">
            <ChartNoAxesColumnIncreasing size={28} />
            <ButtonLink to="/app/class-overview" variant="secondary">
              查看概览
            </ButtonLink>
          </div>
        </Panel>
      </section>
    </div>
  );
}

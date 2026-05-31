import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowRight, KeyRound } from "lucide-react";
import { api } from "../../shared/api/client";
import type { SubmissionHistorySummary } from "../../shared/api/types";
import { loadStudent, saveStudent } from "../../shared/storage";
import { Button } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Panel } from "../../shared/ui/Panel";
import { StatusPill, VerdictPill } from "../../shared/ui/StatusPill";

interface JoinedAssignment {
  id: number;
  title: string;
  inviteCode: string;
  studentProfileId: number;
  displayName: string;
}

function loadJoinedAssignments(): JoinedAssignment[] {
  try {
    const raw = localStorage.getItem("wzai:joinedAssignments");
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function loadRecentSubmissions(): Array<{ problemId: number; problemTitle: string; verdict: string; time: string }> {
  try {
    const raw = localStorage.getItem("wzai:recentSubmissions");
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export default function StudentHome() {
  const navigate = useNavigate();
  const [joined, setJoined] = useState<JoinedAssignment[]>(loadJoinedAssignments);
  const [recentSubs, setRecentSubs] = useState(loadRecentSubmissions);

  function getAssignmentProgress(assignment: JoinedAssignment): { completed: number; total: number } | null {
    const student = loadStudent(assignment.id);
    // Progress would need trajectory API call - simplified for now
    return null;
  }

  return (
    <div className="stack student-home-page">
      <section className="student-home-hero">
        <div>
          <span className="eyebrow">学习空间</span>
          <h1>我的学习</h1>
        </div>
      </section>

      <section className="student-home-grid">
        <div className="student-home-main">
          <Panel title="我的课堂作业">
            {joined.length === 0 ? (
              <EmptyState title="还没有加入课堂作业" description="输入老师给的邀请码开始吧" />
            ) : (
              <div className="student-assignment-cards">
                {joined.map(j => (
                  <div className="student-assignment-card" key={j.id}>
                    <div>
                      <h3>{j.title}</h3>
                      <span className="meta-badge">邀请码 {j.inviteCode}</span>
                      <StatusPill tone="success">{j.displayName}</StatusPill>
                    </div>
                    <Button
                      variant="primary"
                      icon={<ArrowRight size={16} />}
                      onClick={() => navigate(`/problem/${j.id}?assignmentId=${j.id}&studentProfileId=${j.studentProfileId}`)}
                    >
                      继续做题
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </Panel>
        </div>

        <aside className="student-home-side">
          <Panel title="快捷操作">
            <div className="stack">
              <Button variant="secondary" icon={<KeyRound size={16} />} onClick={() => {
                // Open invite modal via a custom event or global state
                document.querySelector<HTMLButtonElement>('.top-nav__link[title="输入教师邀请码"]')?.click();
              }}>
                输入邀请码
              </Button>
            </div>
          </Panel>

          {recentSubs.length > 0 && (
            <Panel title="最近提交">
              <div className="recent-subs-list">
                {recentSubs.map((sub, i) => (
                  <div className="recent-sub-row" key={i}>
                    <span>{sub.problemTitle}</span>
                    <VerdictPill verdict={sub.verdict} />
                    <small>{sub.time}</small>
                  </div>
                ))}
              </div>
            </Panel>
          )}

          {recentSubs.length === 0 && (
            <Panel title="最近提交">
              <EmptyState title="还没有提交记录" description="去公共题库试试吧" />
            </Panel>
          )}
        </aside>
      </section>
    </div>
  );
}
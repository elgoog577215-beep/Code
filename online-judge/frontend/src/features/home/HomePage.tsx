import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Search } from "lucide-react";
import { api } from "../../shared/api/client";
import type { ProblemCatalogItem } from "../../shared/api/types";
import { difficultyLabel } from "../../shared/format";
import { EmptyState } from "../../shared/ui/EmptyState";
import { TextInput } from "../../shared/ui/Field";
import { StatusPill } from "../../shared/ui/StatusPill";

type Difficulty = "ALL" | "EASY" | "MEDIUM" | "HARD";

const DIFFICULTY_TONES: Record<string, "success" | "warning" | "danger"> = {
  EASY: "success",
  MEDIUM: "warning",
  HARD: "danger"
};

const MOCK_PROBLEMS: ProblemCatalogItem[] = [
  { id: 1, title: "两数之和", difficulty: "EASY", timeLimit: 1000, memoryLimit: 256, summary: "给定两个整数，输出它们的和。" },
  { id: 2, title: "Hello World", difficulty: "EASY", timeLimit: 1000, memoryLimit: 128, summary: "输出 Hello World 字符串。" },
  { id: 3, title: "判断奇偶", difficulty: "EASY", timeLimit: 1000, memoryLimit: 256, summary: "输入一个整数，判断它是奇数还是偶数。" },
  { id: 4, title: "计算阶乘", difficulty: "MEDIUM", timeLimit: 1000, memoryLimit: 256, summary: "计算 N 的阶乘，N 最大为 20。" },
  { id: 5, title: "冒泡排序", difficulty: "MEDIUM", timeLimit: 2000, memoryLimit: 256, summary: "对输入的 N 个整数进行升序排序。" },
  { id: 6, title: "斐波那契数列", difficulty: "MEDIUM", timeLimit: 1000, memoryLimit: 256, summary: "输出斐波那契数列的第 N 项。" },
  { id: 7, title: "二叉树遍历", difficulty: "HARD", timeLimit: 2000, memoryLimit: 512, summary: "根据前序和中序遍历重建二叉树并输出后序。" },
  { id: 8, title: "迷宫寻路", difficulty: "HARD", timeLimit: 3000, memoryLimit: 512, summary: "在 N×M 迷宫中找到从起点到终点的最短路径。" },
];

function ProblemCard({ problem, onClick }: { problem: ProblemCatalogItem; onClick: () => void }) {
  return (
    <button type="button" className="problem-card" onClick={onClick}>
      <div className="problem-card__head">
        <StatusPill tone={DIFFICULTY_TONES[problem.difficulty] || "neutral"}>
          {difficultyLabel(problem.difficulty)}
        </StatusPill>
        <span className="problem-card__constraints">
          {problem.timeLimit}ms / {problem.memoryLimit}MB
        </span>
      </div>
      <h3>{problem.title}</h3>
      {problem.summary && <p>{problem.summary}</p>}
    </button>
  );
}

function ProblemCardSkeleton() {
  return (
    <div className="problem-card problem-card--skeleton" aria-hidden="true">
      <div className="problem-card__head">
        <span className="skeleton-badge" />
        <span className="skeleton-badge" />
      </div>
      <span className="skeleton-line skeleton-line--title" />
      <span className="skeleton-line" />
    </div>
  );
}

export default function HomePage() {
  const navigate = useNavigate();
  const [problems, setProblems] = useState<ProblemCatalogItem[]>([]);
  const [search, setSearch] = useState("");
  const [difficulty, setDifficulty] = useState<Difficulty>("ALL");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.problemCatalog()
      .then(setProblems)
      .catch(() => {
        // Fallback to mock data when backend is unavailable
        setProblems(MOCK_PROBLEMS);
      })
      .finally(() => setLoading(false));
  }, []);

  const filtered = useMemo(() => {
    let result = problems;
    if (search.trim()) {
      const keyword = search.trim().toLowerCase();
      result = result.filter(p => p.title.toLowerCase().includes(keyword));
    }
    if (difficulty !== "ALL") {
      result = result.filter(p => p.difficulty === difficulty);
    }
    return result;
  }, [problems, search, difficulty]);

  return (
    <div className="stack home-page">
      <section className="home-hero">
        <div>
          <span className="eyebrow">公共题库</span>
          <h1>探索编程题目</h1>
          <p>无需邀请码，自由浏览和练习所有题目</p>
        </div>
      </section>

      <div className="problem-filters">
        <div className="search-field">
          <Search size={18} />
          <TextInput
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="搜索题目..."
          />
        </div>
        <div className="difficulty-tabs">
          {(["ALL", "EASY", "MEDIUM", "HARD"] as const).map(d => (
            <button
              key={d}
              type="button"
              className={`difficulty-tab ${difficulty === d ? "is-active" : ""}`}
              onClick={() => setDifficulty(d)}
            >
              {d === "ALL" ? "全部" : difficultyLabel(d)}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div className="problem-grid">
          {Array.from({ length: 6 }).map((_, i) => (
            <ProblemCardSkeleton key={i} />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState title={search || difficulty !== "ALL" ? "没有找到匹配的题目" : "题库为空"} />
      ) : (
        <div className="problem-grid">
          {filtered.map(problem => (
            <ProblemCard
              key={problem.id}
              problem={problem}
              onClick={() => navigate(`/problem/${problem.id}`)}
            />
          ))}
        </div>
      )}
    </div>
  );
}
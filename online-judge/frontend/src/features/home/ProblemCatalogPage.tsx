import { useEffect, useMemo, useState } from "react";
import { ArrowRight, Search } from "lucide-react";
import { api } from "../../shared/api/client";
import type { Difficulty, ProblemCatalogItem } from "../../shared/api/types";
import { difficultyLabel } from "../../shared/format";
import { loadStudent } from "../../shared/storage";
import { ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { TextInput } from "../../shared/ui/Field";
import { DifficultyPill, StatusPill } from "../../shared/ui/StatusPill";

type DifficultyFilter = "ALL" | Difficulty;

const DIFFICULTY_FILTERS: DifficultyFilter[] = ["ALL", "EASY", "MEDIUM", "HARD"];

function formatMemoryLimit(memoryLimit: number) {
  if (memoryLimit >= 1024) {
    const mb = memoryLimit / 1024;
    return `${Number.isInteger(mb) ? mb : mb.toFixed(1)} MB`;
  }
  return `${memoryLimit} MB`;
}

export default function ProblemCatalogPage() {
  const [problems, setProblems] = useState<ProblemCatalogItem[]>([]);
  const [keyword, setKeyword] = useState("");
  const [difficulty, setDifficulty] = useState<DifficultyFilter>("ALL");
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [student] = useState(() => loadStudent());

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setFailed(false);
    api.problemCatalog()
      .then(result => {
        if (!ignore) {
          setProblems(result);
        }
      })
      .catch(() => {
        if (!ignore) {
          setProblems([]);
          setFailed(true);
        }
      })
      .finally(() => {
        if (!ignore) {
          setLoading(false);
        }
      });
    return () => {
      ignore = true;
    };
  }, []);

  const filteredProblems = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return problems.filter(problem => {
      const matchesKeyword =
        !normalizedKeyword ||
        problem.title.toLowerCase().includes(normalizedKeyword) ||
        (problem.summary || "").toLowerCase().includes(normalizedKeyword);
      const matchesDifficulty = difficulty === "ALL" || (problem.difficulty || "").toUpperCase() === difficulty;
      return matchesKeyword && matchesDifficulty;
    });
  }, [difficulty, keyword, problems]);

  return (
    <div className="stack catalog-page">
      <section className="catalog-command">
        <div>
          <h1>公共题库</h1>
        </div>
        <ButtonLink to="/app/student" variant="secondary">
          我的学习
        </ButtonLink>
      </section>

      <section className="catalog-tools" aria-label="题库筛选">
        <label className="catalog-search">
          <Search size={18} />
        <TextInput value={keyword} onChange={event => setKeyword(event.target.value)} placeholder="搜索题目" />
        </label>
        <div className="catalog-tabs" role="tablist" aria-label="难度筛选">
          {DIFFICULTY_FILTERS.map(item => (
            <button
              key={item}
              type="button"
              className={item === difficulty ? "is-active" : ""}
              onClick={() => setDifficulty(item)}
            >
              {item === "ALL" ? "全部" : difficultyLabel(item)}
            </button>
          ))}
        </div>
      </section>

      {loading ? (
        <section className="catalog-grid" aria-label="题库加载中">
          {Array.from({ length: 6 }).map((_, index) => (
            <article className="catalog-card catalog-card--loading" key={index}>
              <span />
              <strong />
              <p />
            </article>
          ))}
        </section>
      ) : failed ? (
        <EmptyState title="题库暂时不可用" />
      ) : filteredProblems.length === 0 ? (
        <EmptyState title={problems.length ? "没有匹配的题目" : "题库为空"} />
      ) : (
        <section className="catalog-grid" aria-label="公共题目列表">
          {filteredProblems.map(problem => (
            <article className="catalog-card" key={problem.id}>
              <div className="catalog-card__meta">
                <StatusPill tone="neutral">#{problem.id}</StatusPill>
                <DifficultyPill difficulty={problem.difficulty} />
                <span>
                  {problem.timeLimit} ms / {formatMemoryLimit(problem.memoryLimit)}
                </span>
              </div>
              <h2>{problem.title}</h2>
              <ButtonLink
                to={`/app/problem/${problem.id}${student ? `?studentProfileId=${student.id}` : ""}`}
                variant="secondary"
                icon={<ArrowRight size={16} />}
              >
                开始练习
              </ButtonLink>
            </article>
          ))}
        </section>
      )}
    </div>
  );
}

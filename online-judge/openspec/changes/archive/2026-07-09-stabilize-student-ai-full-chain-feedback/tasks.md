## 1. 多建议契约

- [x] 1.1 让 `studentReport` 不再兜底生成单条逐条建议。
- [x] 1.2 多 issue 且 advice 数量不足时触发一次 count retry。
- [x] 1.3 count retry 仍不足时返回明确 `MODEL_FAILED`。

## 2. 稳定性恢复

- [x] 2.1 扩展结构化重试到 `FreeDiagnosisOutput` 和 `LayeredAttachmentAction`。
- [x] 2.2 `GENERATING` 超过 5 分钟允许重新 enqueue。
- [x] 2.3 backfill 失败只记录日志，不影响主链路。
- [x] 2.4 标准库成长候选移动到学生反馈验证成功之后写入。

## 3. 观测和验证

- [x] 3.1 教师观测台从 `SubmissionAnalysis.aiInvocation` 派生真实失败阶段和原因。
- [x] 3.2 补齐单测覆盖多建议、结构化重试、过期恢复、旁路容错和失败观测。
- [x] 3.3 运行指定回归、OpenSpec 校验和 diff 检查；真实五题 live 已启动但当前 `MODELSCOPE_API_KEY`/`AI_EVAL_API_KEY` 对 chat completion 返回 401，报告为 `target/ai-simulation-reports/real-samples-website-vs-codex-20260709-202508.md`。

## Why

学生 AI 反馈已切到完整诊断链路，但服务器运行仍暴露三个质量问题：模型前置阶段截断或半截 JSON 时不可恢复，多 issue 结果可能被 `studentReport` 兜成单条建议，生成中断或旁路 backfill 失败会让学生端看到不可用且教师端难以定位原因。

本变更只做完整链路的最小稳定性修复，不恢复快链路，不新增本地规则兜底。

## What Changes

- 扩展结构化重试到自由诊断和标准库逐层挂接阶段。
- 将 `studentReport` 固定为摘要，逐条学生建议必须来自结构化 `basicLayerAdvice` / `improvementLayerAdvice`。
- 多个有效 issue 时，advice 数量不足触发一次模型重试；重试仍不足则明确失败。
- `GENERATING` 超过 5 分钟可恢复重新入队。
- 教师观测台展示完整链路真实失败阶段和原因。
- 推荐 backfill 和标准库成长候选写入不再拖死主诊断成功结果。

## Impact

- 后端主链路：`AiReportService`、`AdviceGenerationOutputValidator`、学生反馈服务与异步服务。
- 教师观测：复用现有 failure reason 汇总，不新增学生端 schema。
- 测试：补充多建议契约、结构化重试、生成过期恢复、旁路容错和观测归因。

# 完成审计

## 布尔门禁

- 学生可见修正建议和提升建议只展示 `StudentAiFeedback` 中 `status=READY` 且 `source=MODEL` 的结构化结果。
- 模型不可用、超时、结构化失败或安全拒绝时，学生端显示明确状态，不使用本地规则生成建议。
- 判题事实仍来自 `SubmissionResult.testCaseResults`、verdict、错误输出和首个失败点；学生结果弹窗不再从旧 `SubmissionAnalysis` 抽取建议或失败点。
- 旧 `/app/problems`、`/app/student/problems` 兼容跳转到公共题库作业；旧 `ProblemCatalogPage` 已移除。
- 教师主界面首屏保留课堂风险、共性问题、先看谁、需关注学生和高频问题；AI 质量进入辅助信息和折叠详情。
- 教师主界面经 browser smoke 验证不展示 `BLOCKED`、`RECOVERED`、`NOT_COMPARABLE`、`fallback`、`smoke`、`profile` 等工程词。

## Rubric 审计

- 学生失败提交样例：`StudentAiFeedbackModelTest` 验证模型结构化输出能给出一个修正方向、一个提升方向、一个追问，并引用 `judge:first_failed_case` 和 `code:line` 证据。
- 安全样例：高泄题风险返回 `SAFETY_REJECTED`，清空 `repairItems` 和 `improvementItems`。
- 模型不可用样例：返回 `FAILED + source=MODEL`，不生成本地建议。
- 学习轨迹样例：`StudentAiFeedbackImpactAnalyzerTest` 覆盖 AI 后改善、同类问题仍卡住、已查看但无后续提交。
- 教师场景：教师校正保存为校验样本，AI 质量和趋势服务可以读取校正数据，browser smoke 覆盖教师首页主信息和工程词隐藏。

## 观测指标

- 已记录：学生 AI 反馈 ready/failed/viewed 事件、反馈状态、反馈来源、泄题风险、失败原因、学生/作业/题目上下文。
- 已进入轨迹：AI 反馈查看后的后续提交变化会进入 `latestAiFeedbackImpact`，可用于教师关注信号。
- 观察期：p50/p95 latency、结构化成功率、超时率、安全拦截率、教师查看比例等还没有设置生产阈值；当前只保证字段和事件链路存在，并通过测试验证关键事件。

## 验证结果

- OpenSpec strict validation：通过。
- 前端 typecheck：通过，使用 Codex runtime Node 直接执行 `tsc --noEmit`。
- 前端 build：通过，使用 Codex runtime Node 直接执行 `tsc -b`、`clean-build.mjs`、`vite build`。
- 后端相关测试：通过，70 个测试无失败。
- Browser smoke：通过 618 项检查。
- `git diff --check`：通过。
- 静态构建产物：已清理，未留在工作区。

## 剩余风险

- 旧 `SubmissionAnalysis`、旧学生反馈 DTO 和后端评测工具仍作为兼容、教师诊断和评测资产存在；它们不再主导学生主界面，但还没有彻底归档删除。
- 生产指标看板和硬阈值尚未完成，后续应在真实课堂数据稳定后设置阈值。
- 当前 smoke 使用 mock 数据验证链路和界面，不等价于真实外接模型压力测试。

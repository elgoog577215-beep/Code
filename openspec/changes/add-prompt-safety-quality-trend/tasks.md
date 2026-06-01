## 1. OpenSpec

- [x] 1.1 编写 proposal、design、tasks 和 spec delta。
- [x] 1.2 运行 `openspec validate add-prompt-safety-quality-trend --strict`。

## 2. 后端趋势模型

- [x] 2.1 扩展 `AiQualityTrendResponse` 和作业趋势点，新增提示安全趋势字段。
- [x] 2.2 为 `AiQualityTrendService` 注入 `HintSafetyCheckRepository` 并按提交范围读取安全检查。
- [x] 2.3 聚合总趋势、作业趋势点和摘要中的提示安全事件。
- [x] 2.4 扩展来源分段，归因提示安全事件和安全降级。

## 3. 前端展示

- [x] 3.1 更新 `AiQualityTrend`、`AiQualityTrendPoint` 和 `AiQualitySourceSegment` 类型。
- [x] 3.2 在教师端跨作业趋势区展示提示安全事件、安全降级和作业/来源安全标记。

## 4. 测试

- [x] 4.1 扩展 `AiQualityTrendServiceTest`，覆盖跨作业提示安全事件统计。
- [x] 4.2 覆盖 LOW 安全检查过滤和来源分段安全归因。

## 5. 验证

- [x] 5.1 运行 `openspec validate add-prompt-safety-quality-trend --strict`。
- [x] 5.2 运行 `AiQualityTrendServiceTest`。
- [x] 5.3 运行后端编译、前端 typecheck 和 `git diff --check`。

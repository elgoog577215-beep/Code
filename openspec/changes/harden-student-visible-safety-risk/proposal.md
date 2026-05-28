## Why

live eval 暴露出一个质量落差：外部模型已经命中了真实错因，但最终学生提示被安全链路误降级，导致教学动作从明确的输入对齐退回到泛化收集证据。原因是安全风险判断可能把非学生可见的元字段或教师备注风险混入学生端反馈判断。

本变更用于把外部模型教学提示的安全门禁收敛到“学生实际可见内容”，让正确且安全的外部模型输出不被误伤，同时继续拦截完整代码、直接改法、隐藏数据等真实泄题内容。

## What Changes

- 调整 teaching hint 输出验证：不再单独因为顶层 `answerLeakRisk=HIGH` 判定失败，必须结合学生可见提示、学生提示计划、学习干预计划等可见字段判断。
- 保留对学生可见高风险内容的拦截，例如完整代码、直接替换写法、最终答案、隐藏测试数据、可执行控制结构。
- 调整运行时最终 `answerLeakRisk` 合成逻辑：优先使用学生可见计划风险，不让教师备注或聚合元字段把安全提示误判为高风险。
- 增加回归测试，覆盖“学生可见提示安全、教师备注提到不要泄露具体写法”的输入解析场景。
- 增加安全层测试，确保报告里的代码定位或教师备注不会污染学生可见提示安全判断。

## Capabilities

### New Capabilities

- `student-visible-safety-risk`: 外部模型教学提示的安全判定必须基于学生可见内容，避免非学生可见字段造成误降级。

### Modified Capabilities

无。

## Impact

- 影响 `ModelOutputValidator`、`AiReportService` 的外部模型教学提示安全判断。
- 影响 `HintSafetyServiceTest`、`ModelOutputValidatorTest`、`AiReportServiceExternalRuntimeTest` 等回归测试。
- 不改变线上接口字段，不放宽学生可见泄题内容的拦截策略，不改变模型调用路由。

## 1. 紧凑上下文

- [x] 1.1 在 `AiReportService` 中为 `ModelDiagnosisBrief` 构造紧凑 prompt context。
- [x] 1.2 在 `AiReportService` 中为 `StandardLibraryPack` 构造紧凑 prompt context。
- [x] 1.3 让诊断、教学、single-call 三条外部调用路径共用紧凑 context。

## 2. 验证边界

- [x] 2.1 保持 validator 使用完整 `RuntimePlan`，不使用紧凑对象替代内部验证依据。
- [x] 2.2 增加测试捕获真实 user prompt，确认核心信号保留、冗余字段移除。

## 3. 评测

- [x] 3.1 运行相关 Maven 测试，确认紧凑 context 不影响外部运行时、安全和路由归因。
- [x] 3.2 小规模运行 live eval，观察完成率、质量率和 route outcome 是否继续可解释。
- [x] 3.3 将“低预算上下文不等于弱化验证”的经验写入项目记忆。

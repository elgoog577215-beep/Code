## ADDED Requirements

### Requirement: 多 issue 学生建议不得被摘要兜底压缩
系统 SHALL 将 `studentReport` 作为学生反馈摘要；结构化逐条建议 MUST 来自模型返回的 `basicLayerAdvice` 与 `improvementLayerAdvice`。

#### Scenario: 摘要存在但逐条数组为空
- **WHEN** 非 AC 提交的 advice generation 返回 `studentReport` 但没有结构化基础建议
- **THEN** 系统 SHALL 判定该 advice 输出无效
- **AND** 系统 MUST NOT 用 `studentReport.basicLayerText` 兜底生成单条修正建议

#### Scenario: 多个有效 issue 需要多条建议
- **WHEN** 自由诊断阶段输出多个有效 issue
- **THEN** advice generation SHALL 返回多条基础建议和提高建议
- **AND** 数量不足时系统 SHALL 最多触发一次模型重试
- **AND** 重试后仍不足时系统 SHALL 返回明确失败而不是本地补齐建议

### Requirement: 完整链路结构化阶段应可恢复
系统 SHALL 对自由诊断、标准库逐层挂接和 advice generation 的可恢复结构化输出失败执行一次受控重试。

#### Scenario: 输出截断
- **WHEN** 模型阶段返回 `finish_reason=length` 或半截 JSON
- **THEN** 系统 SHALL 使用更大的非 stream 输出预算重试同一阶段一次

#### Scenario: 随机文本不重试
- **WHEN** 模型阶段返回不包含目标 schema 关键字段的普通文本
- **THEN** 系统 SHALL 直接按结构化失败处理

### Requirement: 学生反馈生成失败应可恢复且可观测
系统 SHALL 恢复过期的 `GENERATING` 状态，并在教师观测中展示完整链路真实失败阶段。

#### Scenario: 生成状态过期
- **WHEN** 学生反馈记录处于 `GENERATING` 且更新时间超过 5 分钟
- **THEN** GET 或 POST 触发反馈时系统 SHALL 允许重新入队生成

#### Scenario: 教师查看真实失败原因
- **WHEN** 学生反馈记录失败原因是 `FULL_CHAIN_FAILED` 但同提交存在 AI analysis 失败记录
- **THEN** 教师观测台 SHALL 汇总 `failureStage:failureReason`
- **AND** 学生端 SHALL 不暴露原始异常或服务商错误文本

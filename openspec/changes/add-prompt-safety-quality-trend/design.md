## 设计

### 信号来源

趋势层复用两个已有信号：

- `SubmissionAnalysis.answerLeakRisk == HIGH`：表示诊断内容存在高泄题风险。
- `HintSafetyCheck.riskLevel >= MEDIUM`：表示提示内容被安全降级或被安全检查识别为中高风险。

LOW 安全检查只说明安全检查通过或低风险，不计入提示安全事件。

### 聚合口径

新增跨作业总计：

- `promptSafetyIncidentCount = highLeakRiskCount + promptSafetyDowngradeCount`
- `promptSafetyDowngradeCount = riskLevel >= MEDIUM 的 HintSafetyCheck 数量`
- `promptSafetyHighRiskDowngradeCount = riskLevel == HIGH 的 HintSafetyCheck 数量`
- `promptSafetyIncidentRate = promptSafetyIncidentCount / analyzedSubmissionCount`

作业趋势点使用同样口径，但只统计当前作业的提交。

### 来源分段归因

`SourceQualitySegment` 已经按 `SubmissionAnalysis` 的 source、provider、model、promptVersion、agentVersion 和 status 分段。新增安全计数字段：

- `promptSafetyIncidentCount`
- `promptSafetyDowngradeCount`
- `promptSafetyHighRiskDowngradeCount`
- `promptSafetyIncidentRate`

高泄题风险归因到该提交对应的诊断来源；提示安全降级也按 `submissionId -> SubmissionAnalysis` 找到同一来源分段。若没有对应分析，则归入 `UNKNOWN|unknown`。

### 摘要优先级

跨作业摘要把提示安全事件放在高泄题风险之后、教师校正之前：

1. 教师介入后仍卡同类问题。
2. 高泄题风险。
3. 提示安全降级。
4. 教师校正。
5. 课堂介入 eval 候选。
6. 等待后续提交。
7. 低置信度。

高泄题仍保留原优先级，因为它是最直接的安全/教学风险；提示安全降级用于发现“安全网经常触发”的趋势。

## 兼容性

新增 DTO 字段是 additive，旧前端可忽略。前端类型使用可选字段，兼容后端旧响应。

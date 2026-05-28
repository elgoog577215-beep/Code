## ADDED Requirements

### Requirement: 四维 Agent 评估画像

系统 SHALL 生成 AI agent 评估画像，用准确率、速度、稳定性和教育有效性四个维度汇总外部模型助手表现。

#### Scenario: 报告包含四个评估维度

- **WHEN** 生成 live AI assistant 评估报告
- **THEN** 报告包含 `evaluationProfile.accuracy`
- **AND** 报告包含 `evaluationProfile.speed`
- **AND** 报告包含 `evaluationProfile.stability`
- **AND** 报告包含 `evaluationProfile.educationalEffectiveness`

#### Scenario: 准确率指标排除 fallback 输出

- **WHEN** 某条评测样本的 `completedOutput=false`
- **THEN** 该样本的信号命中、证据有效和教学动作有效不增加外部模型准确率
- **AND** 报告仍把该 fallback 样本保留为稳定性或运行时问题

#### Scenario: 速度指标暴露延迟分布

- **WHEN** 评测 entry 包含 `latencyMs`
- **THEN** 速度画像包含平均延迟、P50 延迟、P90 延迟、P95 延迟、最大延迟和慢样本 caseId
- **AND** 延迟阈值与准确率阈值分开报告

#### Scenario: 稳定性指标区分模型完成和 fallback

- **WHEN** 评测同时包含已完成输出、运行失败和 fallback entry
- **THEN** 稳定性画像报告 completed output 率、runtime failure 率、fallback 率和路由失败计数
- **AND** 本地 fallback 不计入外部模型完成率

#### Scenario: 缺少学生后续结果时教育有效性标记为代理指标

- **WHEN** 评测没有包含学生下一次提交结果
- **THEN** 教育有效性画像标记学生改善尚未测量
- **AND** 报告使用教学动作有效率、证据有效率和安全通过率作为代理指标

### Requirement: Agent 评估质量门

系统 SHALL 基于准确率、速度、稳定性和教育有效性的阈值评估 AI agent 报告。

#### Scenario: 质量门输出分维度违规项

- **WHEN** 评估画像未达到配置阈值
- **THEN** 质量门输出带有维度和指标名称的违规项
- **AND** 速度违规不覆盖准确率违规
- **AND** runtime 或 fallback 违规不计作模型质量命中失败

#### Scenario: 质量门保持旧报告兼容

- **WHEN** 旧报告不包含 `evaluationProfile`
- **THEN** 质量门回退到现有 `goalSnapshot` 或旧聚合字段
- **AND** 旧报告仍可被评估，不发生 schema 失败

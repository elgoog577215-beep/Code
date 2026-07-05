# ai-prompt-context-quality Specification

## Purpose
保证正式发给外部模型的提示词和上下文能服务高中信息学诊断：既给模型足够方向和标准化约束，又保留模型对代码、题目和判题结果的整体判断能力。
## Requirements
### Requirement: 标准库不得制约模型判断
系统 SHALL 在正式诊断提示词中说明标准库候选是参考和命名约束，而不是唯一答案来源。

#### Scenario: 候选不匹配
- **WHEN** 标准库候选不能解释当前代码问题
- **THEN** 模型 SHALL 被允许返回 `OUT_OF_LIBRARY`
- **AND** 模型 SHALL 简要说明为什么候选不匹配

### Requirement: 学生可见反馈使用自然教学文风
系统 SHALL 要求模型生成学生可读的自然反馈，而不是把结构化字段机械拼接成学生文案。

#### Scenario: 生成学生报告
- **WHEN** 模型生成 `studentReport`
- **THEN** 基础层文本 SHALL 先用通俗语言说明问题，再引用证据，再给行动建议
- **AND** 提高层文本 SHALL 聚焦修复后的算法、复杂度、建模或测试提升
- **AND** 下一步行动 SHALL 是学生马上能做的小动作

### Requirement: 基础层优先
系统 SHALL 要求模型在提交未通过时优先处理基础层问题。

#### Scenario: 未 AC 提交
- **WHEN** 判题结果不是 AC
- **THEN** 基础层反馈 SHALL 解释当前最可能阻塞通过的问题
- **AND** 提高层反馈 SHALL 不抢基础层主次

### Requirement: 保留安全边界
系统 SHALL 在正式诊断提示词中保留学生端安全边界。

#### Scenario: 避免直接给答案
- **WHEN** 模型生成学生可见反馈
- **THEN** 输出 MUST NOT 包含完整代码、完整答案、隐藏测试推测或可复制的逐行改法

### Requirement: 诊断报告提示词应分离学生报告与后端元数据
系统 SHALL 在正式诊断提示词中要求模型先生成学生自然语言报告，再提供后端审计和标准库 metadata；学生可见报告 SHALL NOT 机械堆叠内部结构字段。

#### Scenario: 生成正式诊断报告
- **WHEN** 模型使用 `diagnosis-report-v2` 生成诊断结果
- **THEN** prompt SHALL 明确 `studentReport` 是学生优先阅读的自然反馈
- **AND** prompt SHALL 明确 `diagnosisDecision`、`diagnosisCandidates`、`teacherTrace` 和 `libraryGrowth` 是后端审计与标准库成长信息

#### Scenario: 多条建议存在
- **WHEN** 当前提交存在多个独立错因或提升方向
- **THEN** prompt SHALL 要求逐条列表由证据决定数量
- **AND** prompt SHALL NOT 要求固定一条或把多条问题硬塞进单个字符串

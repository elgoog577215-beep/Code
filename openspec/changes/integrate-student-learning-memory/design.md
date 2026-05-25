## Context

项目已经有多条与“学生记忆”相关的能力：提交历史、`SubmissionAnalysis.reportJson`、`learningTrajectorySignal`、`learningActionEvidence`、`StudentTrajectoryService`、`StudentAbilityProfileService`、`AbilitySignalAnalyzer`、教师诊断修正和 AI 质量评测。问题不在于没有历史信息，而是这些信息没有统一进入外部模型诊断主链路。

长期记忆如果新建独立服务或独立表，很容易变成旁路：诊断看不到、教师端另算一套、评测无法验证、老师修正无法反哺。第一期必须以“增强现有证据链”为目标，而不是追求完整记忆平台。

## Goals / Non-Goals

**Goals:**

- 在现有诊断链上新增 `StudentLearningMemorySnapshot`，从现有事实源派生，不重复存储原始历史代码。
- 将快照接入 `DiagnosisEvidencePackage` 和 `ModelDiagnosisBrief`，让外部模型使用学生长期学习信号辅助诊断。
- 让 `StandardLibraryPack` 和 prompt 明确长期记忆的使用边界：辅助当前证据、提示重复模式、提示干预效果，但不能覆盖当前提交直接证据。
- 第一轮实现只做主链路打通和测试，不改教师端 API，不做前端展示。
- 梳理旁路风险，为后续把教师端画像、推荐、诊断评测统一到同一读模型打基础。

**Non-Goals:**

- 不新增数据库表。
- 不引入向量库或聊天式长期记忆。
- 不把历史代码全文塞进 prompt。
- 不重写 `StudentAbilityProfileService`、`StudentTrajectoryService` 或教师端统计。
- 不让外部模型根据长期画像直接推翻当前提交的编译错误、运行错误或可见失败样例。

## Decisions

### 1. 记忆快照作为诊断证据，而不是独立画像系统

选择在 `DiagnosisEvidencePackage` 中新增 `StudentLearningMemorySnapshot`，并由 `SubmissionAnalysisService` 基于当前学生的历史提交和历史分析构造。这样它跟现有 `HistoryEvidence`、`learningTrajectorySignal` 和 `learningActionEvidence` 在同一条证据链里。

备选方案是新增 `StudentMemoryService` 和独立持久化表。暂不采用，因为第一期会扩大数据一致性和迁移成本，也容易绕开现有报告 JSON 与教师修正。

### 2. 只存聚合结论和证据引用

快照包含重复错因、重复细粒度错因、主要能力焦点、最近学习趋势、上一轮干预效果、教师复核摘要和 evidenceRefs。它不保存历史代码全文，不保存大量提交明细。

这样能控制 prompt 长度，也能保证模型输出可追溯。

### 3. 当前提交证据优先

Prompt 和标准库都必须声明：学生记忆是辅助证据。编译错误、运行错误、可见失败样例、当前代码候选信号优先级高于长期画像。长期画像只能用于：

- 识别重复卡点。
- 选择更合适的教学动作。
- 判断是否需要换一种提示方式。
- 提醒教师关注跨题或跨提交的同类问题。

### 4. 第一轮只做兼容式新增字段

`ModelDiagnosisBrief` 新增 `learningMemorySummary` 和相关 evidence refs，`StandardLibraryPack` 补充记忆使用规则。旧接口不需要传入新字段也能继续工作。

### 5. 旁路收束先写规则，再逐步迁移

本轮不重构所有教师端统计，但设计中明确后续收束方向：教师端画像、轨迹、推荐、AI 质量评估应优先通过 `DiagnosisReportReader` 和统一报告字段读取 AI 诊断结果，避免各服务重复解析 report JSON 或重复推断同一信号。

## Risks / Trade-offs

- [Risk] 长期画像误导当前诊断 → 通过 prompt、标准库规则和测试要求“当前提交直接证据优先”。
- [Risk] prompt 变长导致外部模型不稳定 → 快照只放摘要和最多少量 evidence refs。
- [Risk] 新字段没人消费 → 第一轮直接接入 `ModelDiagnosisBrief` 和外部模型 runtime，而不是只创建 DTO。
- [Risk] 教师端旁路短期仍存在 → 本轮先打通主链路，后续再把教师端能力画像逐步改为复用统一快照/读模型。
- [Risk] 统计口径变化影响旧功能 → 第一轮不修改教师端 API 和数据库，只新增诊断输入证据。

## Migration Plan

1. 新增快照数据结构和构造逻辑，保持默认空值兼容。
2. 将快照注入 brief 和 prompt，旧测试继续通过。
3. 添加针对重复错因、画像辅助、画像冲突的单元测试。
4. 后续阶段再考虑教师端 API 展示和画像读模型统一。

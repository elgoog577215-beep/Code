## ADDED Requirements

### Requirement: AI 诊断应把标准库作为教学参考规范包
系统 SHALL 将 `standardLibrary` 传递给外接大模型时定位为教学参考规范包，用于统一术语、辅助细颗粒定位和提供标准库成长线索；系统 SHALL NOT 要求模型为了命中标准库而牺牲对题目、代码、判题结果和 evidenceRefs 的独立判断。

#### Scenario: 诊断阶段先判断再映射
- **WHEN** 正式诊断 Agent 生成诊断报告
- **THEN** prompt SHALL 要求模型先基于当前提交证据识别真实问题
- **AND** 再评估这些问题对标准库是 `HIT`、`PARTIAL`、`MISS` 还是 `OUT_OF_LIBRARY`

#### Scenario: 标准库候选不匹配
- **WHEN** 当前提交证据显示真实问题无法被候选标准库精确覆盖
- **THEN** 系统 SHALL 允许诊断输出 `OUT_OF_LIBRARY`、`MISS` 或空标准库 ID
- **AND** 系统 SHALL NOT 因缺少强制标准库命中而整包失败

### Requirement: 搜索定位层应允许无合适候选
系统 SHALL 将搜索定位层作为标准库参考包整理阶段；当模型明确说明候选不匹配、证据不足或需要标准库成长时，搜索定位输出 MAY 不选择任何候选。

#### Scenario: 候选整体不匹配
- **WHEN** 搜索定位输出 `libraryFit=MISS`
- **AND** `basicCandidates`、`improvementCandidates`、`knowledgeAnchors` 均为空
- **THEN** 搜索定位校验 SHALL 通过

#### Scenario: 命中状态没有候选
- **WHEN** 搜索定位输出 `libraryFit=HIT`
- **AND** 没有任何被证据支持的候选
- **THEN** 搜索定位校验 SHALL 失败

### Requirement: 标准库 ID 不应作为建议层硬失败条件
系统 SHALL 保留 advice 输出的安全、证据引用和结构硬校验；系统 SHALL 将未知标准库 ID 视为可软修复的映射问题，而不是诊断内容本身无效。

#### Scenario: 逐条建议包含未知标准库 ID
- **WHEN** `basicLayerAdvice` 或 `improvementLayerAdvice` 中的标准库 ID 不存在于当前参考包
- **THEN** 校验器 SHALL 清空该 ID
- **AND** 校验结果 SHALL 在 `softFixes` 中记录清空动作
- **AND** 只要证据、安全和必填字段合法，输出 SHALL 继续通过

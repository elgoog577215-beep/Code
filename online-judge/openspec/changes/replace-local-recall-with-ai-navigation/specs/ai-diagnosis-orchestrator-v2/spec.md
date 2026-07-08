## MODIFIED Requirements

### Requirement: 诊断编排默认跳过外部搜索定位 Agent
系统 SHALL 在默认诊断编排中跳过旧 `search-location-v1`，并 SHALL 不再用后端本地召回作为默认标准库选择方式。默认链路 MUST 使用“初步诊断 -> AI 标准库导航 -> 最终诊断”的三阶段编排。

#### Scenario: 默认诊断不使用旧召回
- **WHEN** 学生提交代码且外部 AI 可用
- **THEN** 系统 SHALL NOT 调用 `search-location-v1`
- **AND** 系统 SHALL NOT 调用 `SearchLocationRetrievalService.retrieve(...)`
- **AND** trace SHALL NOT 标记为 `LOCAL_RECALL`

#### Scenario: 初步诊断先于标准库导航
- **WHEN** 系统启动默认 AI 诊断
- **THEN** 系统 MUST 先调用初步诊断阶段读取题目、完整代码和判题事实
- **AND** 初步诊断阶段 SHALL NOT 接收标准库候选包或标准库 ID 列表

#### Scenario: AI 导航标准库
- **WHEN** 初步诊断阶段返回合法导航意图
- **THEN** 系统 MUST 让 AI 基于标准库目录逐层选择大章节、小章节、知识点和诊断层条目
- **AND** 后端 MUST 只展开 AI 选择的标准库节点
- **AND** trace SHALL 标记标准库定位来源为 `AI_NAVIGATION`

#### Scenario: 导航失败关闭
- **WHEN** 初步诊断或标准库导航阶段失败、超时、输出无效或超过轮次限制
- **THEN** 系统 MUST 将 AI 诊断标记为失败或阶段失败
- **AND** 系统 MUST NOT 回退到本地召回生成学生可见诊断

### Requirement: AI 诊断应把标准库作为教学参考规范包
系统 SHALL 将 AI 导航选中的标准库结构传递给最终诊断阶段，并将其定位为教学参考规范包，用于统一术语、颗粒度和成长线索；最终诊断仍 MUST 以题目、代码、判题事实和证据引用为准。

#### Scenario: 最终诊断读取导航结果
- **WHEN** 最终诊断 Agent 生成诊断报告
- **THEN** prompt SHALL 同时提供原始提交上下文、初步诊断草稿和 AI 标准库导航结果
- **AND** prompt SHALL 要求模型先基于当前提交证据识别真实问题，再映射到标准库路径

#### Scenario: 导航结果不匹配当前证据
- **WHEN** AI 导航选中的标准库路径无法解释当前提交证据
- **THEN** 最终诊断 SHALL 可以返回 `MISS`、`PARTIAL` 或 `OUT_OF_LIBRARY`
- **AND** 系统 SHALL NOT 因导航阶段给出相近路径而强制命中标准库条目

### Requirement: 搜索定位层应允许无合适候选
系统 SHALL 将新的标准库导航层视为教学坐标导航阶段；当 AI 明确说明现有知识点、能力点、易错点或提升点不匹配当前证据时，导航结果 MAY 不选择具体易错点，并 MAY 输出待审核库外缺口。

#### Scenario: 标准库没有合适易错点
- **WHEN** AI 导航能定位到知识点或能力点，但找不到合适易错点
- **THEN** 导航输出 SHALL 允许保留上级路径并标记具体错误点缺失
- **AND** 系统 SHALL 将该缺口进入待审核成长候选，而不是强行绑定相邻易错点

#### Scenario: 标准库整体不匹配
- **WHEN** AI 导航判断当前问题不适合现有标准库路径
- **THEN** 导航输出 SHALL 可以为空路径或建议新路径
- **AND** 最终诊断 SHALL 保留基于当前提交证据的真实问题判断

## REMOVED Requirements

### Requirement: 本地召回提供树形候选上下文
**Reason**: 用户已明确要求默认链路不保留本地召回。继续要求本地召回提供树形候选，会让旧链路和新 AI 导航链路并存，违背“模型先理解、再导航标准库”的目标。

**Migration**: 树形上下文改由标准库导航 API 提供：后端按 AI 选择逐层展开目录、知识点和诊断层；`SearchLocationRetrievalService` 不再参与默认学生诊断主链路。

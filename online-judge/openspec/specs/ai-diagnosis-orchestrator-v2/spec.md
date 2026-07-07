# ai-diagnosis-orchestrator-v2 Specification

## Purpose
TBD - created by archiving change simplify-ai-diagnosis-single-agent. Update Purpose after archive.
## Requirements
### Requirement: 诊断编排默认跳过外部搜索定位 Agent
系统 SHALL 默认使用本地召回候选进入正式诊断报告阶段；外部搜索定位模型调用 SHALL 仅作为显式开启的对照或调试路径。

#### Scenario: 搜索定位开关关闭
- **WHEN** 搜索定位外部模型调用未显式开启
- **THEN** 系统 SHALL 不调用 `search-location-v1`，并 SHALL 继续使用本地树形候选或原标准库包调用正式诊断报告阶段

#### Scenario: 本地候选为空
- **WHEN** 本地召回没有返回候选条目
- **THEN** 系统 SHALL 使用兼容的默认标准库包进入单诊断 Agent，不回退到旧双 Agent 链路

#### Scenario: 显式开启搜索定位对照
- **WHEN** 配置显式开启外部搜索定位模型调用
- **THEN** 系统 MAY 调用 `search-location-v1` 作为对照路径，并 MUST 在 trace 中记录该阶段状态

### Requirement: 本地召回提供树形候选上下文
系统 SHALL 在默认链路中用本地召回构造树形标准库候选包；候选读取 SHALL 优先使用规范标准库结构，并 SHALL 在候选包中包含召回来源、父级路径、关联能力点、同层易错点、少量延伸点和按知识节点分组的结构化视图。

#### Scenario: 底层易错点被召回
- **WHEN** 本地召回命中一个底层易错点
- **THEN** 候选包 SHALL 同时包含该易错点的父级知识路径、关联能力点和可用于区分的同层易错点

#### Scenario: 向量召回不可用
- **WHEN** 向量召回失败或未配置
- **THEN** 系统 SHALL 使用结构、关键词和规则信号召回继续诊断，并在 trace 中标记降级

#### Scenario: 规范结构可用
- **WHEN** 规范标准库结构中存在启用的能力点、易错点或提升点
- **THEN** 本地召回 SHALL 优先从规范结构生成候选，而不是优先读取旧扁平标准库条目

#### Scenario: 规范结构为空
- **WHEN** 规范标准库结构尚未同步或没有可用条目
- **THEN** 本地召回 SHALL 回退到旧扁平标准库条目，保证诊断链路不中断

#### Scenario: 诊断阶段使用结构视图
- **WHEN** 本地召回生成 selected standard library pack
- **THEN** 诊断阶段 SHALL 接收按知识节点和能力点分组的结构视图，并 SHALL 继续接收旧兼容列表

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

### Requirement: 默认诊断模型应使用已验证可用模型
系统 SHALL 将默认外部诊断模型配置为最近真实链路 smoke test 已验证可用的模型；部署环境 MAY 通过环境变量覆盖默认模型。

#### Scenario: 未配置模型环境变量
- **WHEN** `OJ_AI_MODEL` 和 `AI_MODEL` 均未配置
- **THEN** 系统 SHALL 默认使用 `Qwen/Qwen3-235B-A22B-Instruct-2507`

#### Scenario: 环境变量覆盖模型
- **WHEN** 部署环境显式配置 `OJ_AI_MODEL` 或 `AI_MODEL`
- **THEN** 系统 SHALL 使用环境变量指定的模型

### Requirement: 正式诊断上下文必须优先保障当前提交事实
系统 SHALL 在正式诊断上下文中优先保留题目、学生代码、判题事实、失败样例、运行错误和标准库参考；标准库参考包 SHALL 只作为命名、颗粒度和成长线索，不得替代当前提交证据。

#### Scenario: 标准库候选与提交证据冲突
- **WHEN** 标准库候选方向与当前提交证据不一致
- **THEN** 正式诊断 Agent SHALL 以当前提交证据为准
- **AND** MAY 将标准库关系标记为 `MISS` 或 `OUT_OF_LIBRARY`

#### Scenario: 代码较长
- **WHEN** 学生代码较长
- **THEN** 教师深诊断上下文 SHALL 保留完整源码、失败点附近代码和可引用证据候选
- **AND** 不得只依赖第一条失败样例、第一条本地信号或压缩摘要生成完整诊断


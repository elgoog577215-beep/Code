# ai-diagnosis-orchestrator-v2 Specification

## Purpose
TBD - created by archiving change simplify-ai-diagnosis-single-agent. Update Purpose after archive.
## Requirements
### Requirement: 诊断编排默认使用 AI 标准库导航
系统 SHALL 在默认诊断编排中使用“自由诊断 issues -> 可选标准库逐层挂接 -> 建议生成”的编排。自由诊断和建议生成是主链路；标准库挂接 SHALL 只作为命名、路径和成长线索辅助，不得作为学生建议生成的硬闸门。

#### Scenario: 默认诊断使用 issue-first 编排
- **WHEN** 学生提交代码且外部 AI 可用
- **THEN** 系统 SHALL 先执行自由诊断并生成 `issues[]`
- **AND** 系统 SHALL 尝试对每个 issue 执行标准库逐层挂接
- **AND** 系统 SHALL 基于 `issues[]` 生成学生可见建议
- **AND** trace SHALL 标记标准库定位来源为 `LAYERED_ATTACHMENT`、`LIBRARY_EMPTY`、`NO_MATCH` 或 `ATTACHMENT_FAILED`

#### Scenario: 自由诊断先于标准库挂接
- **WHEN** 系统启动默认 AI 诊断
- **THEN** 系统 MUST 先调用自由诊断阶段读取题目、完整代码和判题事实
- **AND** 自由诊断阶段 SHALL NOT 接收标准库候选包或标准库 ID 列表

#### Scenario: 后端控制标准库逐层挂接
- **WHEN** 自由诊断阶段返回合法 issues
- **THEN** 系统 MUST 由后端基于标准库目录逐层提供大章节、小章节、知识点和诊断层条目
- **AND** AI MUST 只对当前层返回选择、完成或无匹配动作
- **AND** 后端 MUST 只展开 AI 在当前层合法选择的标准库节点
- **AND** trace SHALL 记录每个 issue 的挂接状态

#### Scenario: 挂接失败不关闭诊断
- **WHEN** 标准库挂接阶段失败、超时、输出无效、根目录为空或超过轮次限制
- **THEN** 系统 MUST 保留自由诊断 issues
- **AND** 系统 MUST 将对应 issue 的标准库挂接状态标记为不可用或未命中
- **AND** 系统 MUST 继续执行 advice generation
- **AND** 系统 MUST NOT 仅因标准库挂接失败而返回 `MODEL_FAILED`

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

### Requirement: 标准库导航层应允许无合适候选
系统 SHALL 将新的标准库导航层视为教学坐标导航阶段；当 AI 明确说明现有知识点、能力点、易错点或提升点不匹配当前证据时，导航结果 MAY 不选择具体易错点，并 MAY 输出待审核库外缺口。

#### Scenario: 标准库没有合适易错点
- **WHEN** AI 导航能定位到知识点或能力点，但找不到合适易错点
- **THEN** 导航输出 SHALL 允许保留上级路径并标记具体错误点缺失
- **AND** 系统 SHALL 将该缺口进入待审核成长候选，而不是强行绑定相邻易错点

#### Scenario: 标准库整体不匹配
- **WHEN** AI 导航判断当前问题不适合现有标准库路径
- **THEN** 导航输出 SHALL 可以为空路径或建议新路径
- **AND** 最终诊断 SHALL 保留基于当前提交证据的真实问题判断

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

### Requirement: AI 标准库导航必须覆盖真实知识树深度
系统 SHALL 让后端控制的标准库逐层挂接能够从当前知识树根节点逐层展开到知识点诊断层，并 SHALL 在每轮挂接视图中只提供当前层允许选择的知识节点 code 或诊断层 code。系统 MUST NOT 因模型未维护旧导航状态机而强制失败。

#### Scenario: 多层知识树挂接完成
- **WHEN** 标准库路径包含根节点、大章节、小章节、知识点和知识点诊断层
- **THEN** 系统 SHALL 允许后端逐层展开并完成挂接
- **AND** 系统 SHALL 在看到诊断层后接受绑定能力点、易错点或提升点的完成结果
- **AND** 系统 SHALL 将挂接结果作为 advice generation 的可选 anchors

#### Scenario: 当前视图限定可选 code
- **WHEN** 系统向 AI 发送某一轮标准库挂接视图
- **THEN** 视图 SHALL 包含当前可选择的 code 列表
- **AND** 如果已展开知识点诊断层，视图 SHALL 包含当前可选择的能力点、易错点和提升点 code 列表
- **AND** 模型返回的 code MUST 只引用当前视图中出现的 code

#### Scenario: 非法选择降级
- **WHEN** 模型返回当前视图没有出现的 code
- **THEN** 系统 SHALL 最多发起一次结构化重问或直接丢弃非法 code
- **AND** 如果修正后仍不合法，系统 SHALL 标记该 issue 挂接失败
- **AND** 系统 MUST NOT 因单个 issue 挂接失败而阻断 advice generation

#### Scenario: 最后一轮不得继续展开
- **WHEN** 标准库挂接到达本次允许的最后一轮
- **THEN** 系统 SHALL 要求模型返回 `DONE` 或 `NO_MATCH`
- **AND** 如果模型仍要求继续选择，系统 SHALL 标记该 issue 挂接失败
- **AND** 系统 MUST 继续基于已有 issues 生成学生可见诊断建议

### Requirement: 所有有效问题必须进入知识归类
系统 SHALL 将自由诊断中通过证据、结构和去重校验的全部问题分别送入知识归类，并 MUST NOT 使用默认问题数量上限静默跳过后续问题。

#### Scenario: 一次提交包含多个有效问题
- **WHEN** 自由诊断返回多个证据有效且互不重复的问题
- **THEN** 系统 SHALL 为每个问题分别产生知识归类结果
- **AND** 每个结果 SHALL 保留对应问题 ID、路径和归类状态

#### Scenario: 单个问题归类失败
- **WHEN** 某个问题在知识归类过程中超时、无匹配或返回非法选择
- **THEN** 系统 SHALL 只降低该问题的归类状态
- **AND** 系统 MUST 继续处理其余问题并生成建议

### Requirement: 建议生成质量门槛必须检查问题覆盖
系统 SHALL 以有效问题是否被基础建议覆盖作为学生建议的主要完整性门槛，并 SHALL 允许提升建议数量由真实教学价值决定。

#### Scenario: 多个主要问题均有建议
- **WHEN** 自由诊断包含多个阻塞或主要问题
- **THEN** 每个问题 SHALL 至少关联一条基础建议
- **AND** 系统 SHALL 使用问题 ID 验证逐条对应关系

#### Scenario: 提升建议只有一条
- **WHEN** 当前提交只有一个明确且不重复的提升方向
- **THEN** 系统 SHALL 接受一条提升建议
- **AND** 系统 MUST NOT 因数量少于问题数而触发重写

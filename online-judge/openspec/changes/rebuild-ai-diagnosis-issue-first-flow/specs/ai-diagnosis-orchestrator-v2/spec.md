## MODIFIED Requirements

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

# standard-library-layered-attachment Specification

## Purpose
TBD - created by archiving change rebuild-ai-diagnosis-issue-first-flow. Update Purpose after archive.
## Requirements
### Requirement: 后端控制标准库逐层挂接
系统 SHALL 由后端维护标准库逐层挂接状态，并 SHALL 只让 AI 在当前可见层选择下一步目录、结束或标记无匹配。

#### Scenario: 第一层目录由后端提供
- **WHEN** 某个诊断 issue 进入标准库挂接阶段
- **THEN** 系统 SHALL 从标准库服务读取当前可见根目录或子目录
- **AND** 请求给 AI 的目录内容 SHALL 只包含当前层可选节点和 breadcrumb
- **AND** AI SHALL NOT 负责维护 round、expandedNodes、diagnosticLayers 或 selectedPaths

#### Scenario: AI 只返回最小动作
- **WHEN** AI 收到某一层标准库目录
- **THEN** AI 响应 SHALL 只表达 `SELECT`、`DONE` 或 `NO_MATCH`
- **AND** `SELECT` 响应 SHALL 只包含当前层可见 code
- **AND** 后端 SHALL 校验 code 并决定下一层展开

#### Scenario: 后端维护 breadcrumb
- **WHEN** AI 选择一个当前层节点
- **THEN** 后端 SHALL 把该节点加入该 issue 的 breadcrumb
- **AND** 后端 SHALL 展开该节点的下一层目录或诊断层
- **AND** 下一轮请求 SHALL 带上已选择路径供模型理解上下文

### Requirement: 标准库挂接按 issue 独立执行
系统 SHALL 对自由诊断产生的每个 issue 独立执行标准库挂接，使多个错误可以拥有不同的标准库路径。

#### Scenario: 多个 issue 分别挂接
- **WHEN** 自由诊断返回多个 issue
- **THEN** 系统 SHALL 为每个 issue 分别维护挂接状态
- **AND** 一个 issue 的 `NO_MATCH`、失败或空路径 MUST NOT 清空其他 issue 的挂接结果

#### Scenario: 整题不再只有一个标准库路径
- **WHEN** 一个提交同时包含多个错误
- **THEN** 系统 SHALL 允许不同 issue 绑定不同知识路径、能力点、易错点或提升点
- **AND** 系统 MUST NOT 因只保留整题主路径而压缩 issue 数量

### Requirement: 标准库不可用时降级为挂接不可用
系统 SHALL 将标准库空目录、读取失败、无可选 code 或模型挂接失败视为标准库挂接不可用，而不是诊断失败。

#### Scenario: 根目录为空
- **WHEN** 标准库根目录为空
- **THEN** 系统 SHALL NOT 调用 AI 选择目录
- **AND** 系统 SHALL 为每个 issue 标记 `anchorStatus=LIBRARY_EMPTY`
- **AND** 系统 SHALL 继续执行 advice generation

#### Scenario: AI 返回空选择
- **WHEN** AI 在有可选目录时返回 `SELECT` 但 code 为空
- **THEN** 系统 SHALL 将该 issue 标记为 `anchorStatus=NO_MATCH` 或 `ATTACHMENT_FAILED`
- **AND** 系统 SHALL 继续处理其他 issue 和 advice generation

#### Scenario: AI 返回非法 code
- **WHEN** AI 返回当前层不可见 code
- **THEN** 系统 SHALL 丢弃该 code 并记录 `ATTACHMENT_INVALID_CODE`
- **AND** 系统 MAY 对该 issue 做一次重问
- **AND** 重问失败后 SHALL 降级该 issue 挂接状态而不是失败整次诊断

### Requirement: 挂接 trace 必须可复盘
系统 SHALL 在真实样本仿真和 live eval 中记录标准库挂接过程，使每轮目录输入、AI 动作和后端判定可复盘。

#### Scenario: 记录逐层挂接过程
- **WHEN** 标准库挂接阶段执行
- **THEN** trace SHALL 记录 issueId、breadcrumb、当前层可选 code、AI action、AI codes、后端判定和降级原因
- **AND** trace MUST NOT 记录 API key 或其他密钥

#### Scenario: advice 未生成可定位原因
- **WHEN** advice generation 未执行或失败
- **THEN** trace SHALL 能区分是自由诊断失败、标准库挂接降级、advice 输出无效、超时还是安全拦截

### Requirement: 逐层挂接必须暴露临时诊断节点
系统 SHALL 在知识点诊断层中同时提供正式诊断节点和同父节点下的临时诊断节点，并 SHALL 明确两者优先级。

#### Scenario: 展开含临时节点的诊断层
- **WHEN** 后端展开一个存在待处理成长候选的知识点
- **THEN** 返回内容 SHALL 包含正式能力点、易错点、提升点和受限数量的临时候选
- **AND** 临时候选 SHALL 包含 code、name、layer、confidence、occurrenceCount 和 provisional 标记

#### Scenario: 正式节点优先
- **WHEN** 正式节点和临时候选都可见
- **THEN** 模型提示 SHALL 要求先判断正式节点
- **AND** 只有正式节点不能解释 issue 时才允许选择临时候选

### Requirement: 挂接失败必须保留已选 breadcrumb
系统 SHALL 在 `NO_MATCH`、`ATTACHMENT_FAILED` 或轮次耗尽时保留失败前已经确认的 breadcrumb，供临时节点创建和学生反馈路径回填使用。

#### Scenario: 诊断层调用失败但已有路径
- **WHEN** 标准库挂接在已经选择一个或多个知识节点后失败
- **THEN** issue anchor SHALL 保留已选择 path
- **AND** advice generation SHALL 收到该 path 和失败状态
- **AND** 整次学生诊断 MUST NOT 因挂接失败而被清空

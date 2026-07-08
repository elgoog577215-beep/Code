## ADDED Requirements

### Requirement: AI 诊断契约必须保留结构化数组
系统 SHALL 将 AI 诊断输出中的摘要、逐条修正建议、逐条提升建议、标签映射和标准库成长候选作为不同结构处理；后处理 MUST NOT 用摘要文本、第一条主项或兜底结果整体覆盖有效的多条结构化建议。

#### Scenario: 多条建议不被摘要覆盖
- **WHEN** 外部模型返回多条有效 `basicLayerAdvice`、`improvementLayerAdvice`、`repairItems` 或 `improvementItems`
- **THEN** 后端 SHALL 保留这些互不重复且安全的结构化 item
- **AND** `studentReport` 摘要 SHALL NOT 被转换为唯一建议来覆盖这些 item

#### Scenario: 只有摘要时可以降级生成
- **WHEN** 外部模型没有返回任何结构化建议但返回了可用摘要
- **THEN** 后端 MAY 生成降级建议项
- **AND** 降级项 SHALL 标记来源为摘要降级或等价 trace

### Requirement: 逐条建议必须表达证据状态
系统 SHALL 为逐条建议保留可解释的证据状态；修正建议 SHOULD 绑定当前提交证据，提升建议 MAY 没有直接代码证据，但 MUST NOT 借用其他 item 的证据伪装成自身证据。

#### Scenario: 修正建议包含可验证证据
- **WHEN** AI 输出修正建议并引用 evidenceRefs
- **THEN** 后端 SHALL 校验这些 refs 属于当前上下文包允许的证据
- **AND** 展示层 SHALL 只在该 item 下展示该 item 自身证据

#### Scenario: 提升建议没有直接代码证据
- **WHEN** AI 输出提升建议且没有可跳转代码行证据
- **THEN** 后端 SHALL 允许该 item 保留为空 evidenceRefs 或显式无直接代码证据状态
- **AND** 展示层 SHALL 显示无直接代码证据的空态，而不是展示其他 item 的代码证据

#### Scenario: 无效证据不得软绑定到第一条证据
- **WHEN** AI 输出无法识别的 evidenceRef 或泛化别名无法可靠映射
- **THEN** 后端 SHALL 清空或标记该 item 的无效证据
- **AND** 后端 MUST NOT 自动绑定到第一条 `code:`、`judge:` 或 anchor 证据

### Requirement: AI 上下文包必须声明可引用证据边界
系统 SHALL 在传给外部模型的诊断上下文包中列出可引用证据候选，并区分代码证据、判题证据、规则信号、学习记忆和标准库参考；模型输出只能引用该边界内的证据。

#### Scenario: 上下文包包含证据候选
- **WHEN** 后端构造正式诊断请求
- **THEN** 上下文包 SHALL 包含可引用 evidenceRefs、证据类型、简短说明和是否可跳转代码行

#### Scenario: 模型引用边界外证据
- **WHEN** 模型返回不在上下文包证据候选内的 evidenceRef
- **THEN** validator SHALL 将该引用视为无效
- **AND** 该无效引用 SHALL NOT 被静默替换为其他证据

### Requirement: 教师深诊断必须使用深度上下文与输出预算
系统 SHALL 区分学生快反馈与教师深诊断运行时；教师深诊断、作业题目分析和标准库成长候选生成 MUST 保留足够上下文和输出预算，以支持多条建议、标签映射和候选池内容完整返回。

#### Scenario: 教师诊断不使用低延迟截断配置
- **WHEN** 教师端触发提交诊断、题目分析或标准库成长候选生成
- **THEN** 后端 SHALL 使用标准或深诊断 profile
- **AND** 请求 SHALL 尽力传递模型支持的输出 token 预算

#### Scenario: 兼容请求无法传递预算
- **WHEN** 当前 provider 兼容模式无法可靠传递输出预算
- **THEN** 后端 SHALL 在 trace 或运行时诊断中记录预算不可控风险
- **AND** 不得把可能被截断的结构化数组当作高置信完整结果

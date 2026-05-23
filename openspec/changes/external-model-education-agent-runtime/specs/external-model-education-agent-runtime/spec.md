## ADDED Requirements

### Requirement: 外部模型调用必须使用压缩诊断输入

系统 MUST 在调用外部模型进行提交诊断前构造 `ModelDiagnosisBrief`，并且模型诊断 prompt MUST 使用该 brief 作为主要输入，而不是直接传入完整业务对象。

#### Scenario: 构造包含关键证据的 brief
- **WHEN** 一次提交存在题目、源码、评测结果、规则信号和首个失败样例
- **THEN** `ModelDiagnosisBrief` MUST 包含题目摘要、带行号关键代码片段、verdict、首个失败样例摘要、候选规则信号、证据引用和不确定性说明

#### Scenario: brief 不泄露隐藏测试数据
- **WHEN** 失败样例来自隐藏测试点且系统没有可公开输入输出
- **THEN** `ModelDiagnosisBrief` MUST 标记隐藏测试不可见，并且 MUST NOT 包含猜测的隐藏输入、隐藏输出或完整隐藏数据

### Requirement: 外部模型必须接收裁剪后的标准库包

系统 MUST 在调用外部模型前构造 `StandardLibraryPack`，只注入当前诊断相关的候选错因、细粒度标签、教学动作、常见误区和安全规则。

#### Scenario: 标准库包按候选信号裁剪
- **WHEN** 本地规则信号给出候选错因和细粒度标签
- **THEN** `StandardLibraryPack` MUST 优先包含这些候选项及其说明、教学动作和禁止泄题约束

#### Scenario: 标准库包保留不确定性出口
- **WHEN** 候选信号不足或相互冲突
- **THEN** `StandardLibraryPack` MUST 包含 `NEEDS_MORE_EVIDENCE` 或等价的不确定性选项，允许模型返回需要更多证据

### Requirement: 外部模型诊断必须分阶段执行

系统 MUST 将外部模型诊断拆分为至少两个逻辑阶段：错因裁决阶段和教学表达阶段。错因裁决阶段 MUST 先于教学表达阶段完成并通过校验。

#### Scenario: 错因裁决阶段只输出结构化裁决
- **WHEN** 系统调用错因裁决阶段
- **THEN** 模型输出 MUST 包含主错因标签、细粒度错因标签、证据引用、置信度和不确定性，并且 MUST NOT 生成完整学生解法或长篇教学报告

#### Scenario: 教学表达阶段基于已校验裁决生成提示
- **WHEN** 错因裁决阶段通过校验
- **THEN** 教学表达阶段 MUST 使用已校验裁决生成学生提示、提示计划、学习干预计划和教师备注

#### Scenario: 裁决阶段失败时不继续教学表达
- **WHEN** 错因裁决阶段超时、返回非法 JSON、标签不在标准库内或证据引用无效
- **THEN** 系统 MUST 跳过外部模型教学表达阶段，并使用本地规则结果兜底

### Requirement: 模型输出必须通过程序校验

系统 MUST 对外部模型每个阶段的输出执行程序校验，包括 JSON 合法性、标签合法性、证据引用合法性、字段完整性和泄题风险。

#### Scenario: 非法标签被拒绝或归一化
- **WHEN** 模型输出不存在于标准库的错因标签或细粒度标签
- **THEN** 系统 MUST 拒绝该字段或归一化为合法标签，并记录校验结果

#### Scenario: 无效证据引用被拒绝
- **WHEN** 模型输出的 `evidenceRefs` 不存在于 `ModelDiagnosisBrief`、规则信号或证据包中
- **THEN** 系统 MUST 拒绝该证据引用，并在结果中保留可审计的失败原因

#### Scenario: 泄题风险触发安全降级
- **WHEN** 模型输出包含完整代码、最终答案、隐藏测试数据或逐步替学生完成解法
- **THEN** 系统 MUST 将 `answerLeakRisk` 标记为 `HIGH`，并且 MUST 使用安全版本提示或本地规则结果替代学生可见内容

### Requirement: 规则兜底不能冒充模型成功

系统 MUST 在模型调用失败、超时、格式错误、校验失败或安全失败时显式记录规则兜底状态，并且 live eval MUST NOT 将规则兜底计为外部模型成功。

#### Scenario: 模型超时后使用规则兜底
- **WHEN** 外部模型调用超过配置的超时时间
- **THEN** 系统 MUST 返回本地规则诊断结果，并设置 `fallbackUsed=true` 与 `status=RULE_FALLBACK`

#### Scenario: live eval 区分模型成功和规则兜底
- **WHEN** live eval 样本通过本地规则兜底命中预期标签但模型阶段失败
- **THEN** live eval MUST 将该样本记录为模型失败或回退，而不是模型成功

### Requirement: live model eval 必须生成逐条质量报告

系统 MUST 提供 live model eval report，逐条记录外部模型调用质量、诊断质量和失败原因。

#### Scenario: 记录成功样本质量
- **WHEN** 外部模型完成某条评测样本
- **THEN** report MUST 记录 caseId、model、promptVersion、stage、latencyMs、status、fallbackUsed、jsonValid、标签命中、证据校验、安全结果和输出摘要

#### Scenario: 记录失败样本原因
- **WHEN** 外部模型评测样本发生超时、限流、空返回、非法 JSON、校验失败或安全失败
- **THEN** report MUST 记录明确的 failureReason，并保留该样本的 fallbackUsed 状态

#### Scenario: 批量评测不因单条失败丢失报告
- **WHEN** live model eval 批量运行且其中一条样本失败
- **THEN** 系统 MUST 继续记录已完成样本和失败样本的结果，最终输出汇总报告

### Requirement: Prompt 契约必须版本化

系统 MUST 为外部模型的每个 prompt 模板记录稳定版本号，并在模型调用结果和 live eval report 中保留 promptVersion。

#### Scenario: 调用结果包含 prompt 版本
- **WHEN** 外部模型阶段完成或失败
- **THEN** `aiInvocation` 或 live eval report MUST 包含对应阶段使用的 promptVersion

#### Scenario: Prompt 版本变更可用于质量对比
- **WHEN** prompt 模板升级后运行 live eval
- **THEN** report MUST 能按 promptVersion 区分不同版本的成功率、回退率和标签命中情况

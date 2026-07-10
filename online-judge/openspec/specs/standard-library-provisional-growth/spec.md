# standard-library-provisional-growth Specification

## Purpose
TBD - created by archiving change redesign-student-feedback-library-growth-loop. Update Purpose after archive.
## Requirements
### Requirement: 未命中诊断必须继承最后有效父路径
系统 SHALL 在标准库逐层导航未精确命中时，以后端记录的最后有效 breadcrumb 作为新临时节点的父路径，并 MUST NOT 接受模型自行改写该父路径。

#### Scenario: 子节点均不匹配
- **WHEN** AI 已沿知识树选择到一个真实节点，但当前正式子节点均不符合诊断 issue
- **THEN** 系统 SHALL 保留当前 breadcrumb
- **AND** 系统 SHALL 使用 breadcrumb 最末节点 code 作为临时节点父节点
- **AND** 当前建议的知识路径 SHALL 为 breadcrumb 加临时节点名称

#### Scenario: 没有有效父节点
- **WHEN** 导航在选择任何真实知识节点前失败或无匹配
- **THEN** 系统 SHALL NOT 自动创建临时正式归属
- **AND** 当前建议 SHALL 标记为 `UNCLASSIFIED`

### Requirement: 临时节点必须具有诊断类型和证据
系统 SHALL 将模型已生成的 issue 或 advice 转换为带类型、父节点、来源提交、证据和置信度的临时标准库节点。

#### Scenario: 基础错误形成易错点候选
- **WHEN** 基础建议没有合法易错点 ID 且存在有效父知识节点
- **THEN** 系统 SHALL 创建或聚合一个 `MISTAKE_POINT` 临时候选
- **AND** 候选 SHALL 保存建议名称、完整建议路径、父知识节点 code 和 evidence refs

#### Scenario: 提升建议形成提升点候选
- **WHEN** 提升建议没有合法提升点 ID 且存在有效父知识节点
- **THEN** 系统 SHALL 创建或聚合一个 `IMPROVEMENT_POINT` 临时候选

### Requirement: 临时节点应参与受限导航
系统 SHALL 在知识点诊断层向 AI 提供该父节点下的临时候选，但 SHALL 将其优先级置于正式标准库节点之后。

#### Scenario: 正式节点不匹配时选择临时节点
- **WHEN** AI 展开某知识点的诊断层且正式节点不能解释 issue
- **THEN** AI MAY 选择同父节点下的临时候选
- **AND** 后端 SHALL 将 anchor 标记为 `PROVISIONAL`
- **AND** 后端 SHALL 保留正式 breadcrumb 与临时节点名称

### Requirement: 临时节点必须聚合并按门禁晋升
系统 SHALL 按 layer、父节点和规范化 code 聚合同类候选，SHALL 按独立来源提交累计出现次数，并 SHALL 仅在配置门禁全部满足时自动写入规范化正式标准库。

#### Scenario: 同类候选来自新的独立提交
- **WHEN** 同一父节点下相同 layer 和规范化 code 的候选来自新的来源提交
- **THEN** 系统 SHALL 记录该来源提交并增加 occurrenceCount
- **AND** 系统 SHALL 合并新证据

#### Scenario: 同一提交重复生成同类候选
- **WHEN** 同一来源提交因重试、刷新或重复生成再次提出相同候选
- **THEN** 系统 SHALL 合并证据并更新时间
- **AND** 系统 MUST NOT 增加 occurrenceCount

#### Scenario: 候选没有可验证来源提交
- **WHEN** 候选缺少来源提交 ID
- **THEN** 系统 MAY 保存候选供教师治理
- **AND** 重复写入 MUST NOT 使其达到自动晋升的独立出现次数门槛

#### Scenario: 满足自动晋升门禁
- **WHEN** 候选父节点真实存在、证据状态为 `SUPPORTED`、独立来源提交数量和置信度达到配置阈值且正式库无重复项
- **THEN** 系统 SHALL 将候选写入规范化正式标准库
- **AND** 系统 SHALL 保留全部独立来源、候选审计与回滚信息

#### Scenario: 未满足自动晋升门禁
- **WHEN** 任一父节点、证据、置信度、独立来源提交数量或去重门禁不满足
- **THEN** 系统 SHALL 保留候选为临时状态
- **AND** 系统 SHALL NOT 写入正式标准库

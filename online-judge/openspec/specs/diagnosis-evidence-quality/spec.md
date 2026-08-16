# diagnosis-evidence-quality Specification

## Purpose
TBD - created by archiving change strengthen-diagnosis-evidence-quality-v5. Update Purpose after archive.
## Requirements
### Requirement: 诊断事实必须保存可追溯的标准库挂接身份
系统 SHALL 为每条诊断事实保存与路径状态一致的稳定身份：正式事实使用正式能力、易错点或提升点 ID，临时候选事实使用稳定 `provisional_node_code`，未分类事实才允许没有库内 code。

#### Scenario: 临时候选建议投影为事实
- **WHEN** 基础或提升建议的 `knowledgePathStatus` 为 `PROVISIONAL` 且包含 `provisionalNodeCode`
- **THEN** 事实 SHALL 保存同一 `provisional_node_code`
- **AND** 问题身份来源 SHALL 为 `PROVISIONAL_ID` 而不是文本指纹

#### Scenario: 正式建议投影为事实
- **WHEN** 建议的 `knowledgePathStatus` 为 `FORMAL`
- **THEN** 基础事实 SHALL 保存有效能力点和易错点 ID，或提升事实 SHALL 保存有效提升点 ID
- **AND** 系统 SHALL NOT 同时把该事实标记为临时候选

#### Scenario: 未分类建议投影为事实
- **WHEN** 建议没有有效正式或临时候选挂接
- **THEN** 事实 SHALL 标记为 `UNCLASSIFIED`
- **AND** 系统 SHALL NOT 伪造正式或临时候选 code

### Requirement: 历史诊断证据修复必须确定、幂等且不调用模型
系统 SHALL 只从已保存分析 JSON、规范标准库和成长候选中修复历史证据，并 SHALL 支持预览、分批执行和重复执行。

#### Scenario: 历史临时候选 code 可确定
- **WHEN** 历史事实的 `fact_key` 能唯一连接到包含 `provisionalNodeCode` 的 advice
- **THEN** V5 迁移 SHALL 回填同一 code
- **AND** SHALL 验证该 code 对应成长候选及其启用父知识点存在

#### Scenario: 历史分析有建议但没有事实
- **WHEN** 已保存分析包含基础或提升建议且事实表没有该分析的投影
- **THEN** 回填 SHALL 复用当前投影器生成事实
- **AND** SHALL NOT 重新请求外部模型

#### Scenario: 历史分析没有建议
- **WHEN** 已保存分析的基础和提升建议均为空
- **THEN** 回填 SHALL 记录其不可投影状态
- **AND** SHALL NOT 为提高覆盖率制造事实

#### Scenario: 重复运行证据回填
- **WHEN** 同一范围完成一次 V5 证据回填后再次执行
- **THEN** 事实、反馈版本、事件关联和生命周期 SHALL 不产生重复记录

### Requirement: 诊断挂接结果必须具有可解释的 library fit
诊断事实的 `library_fit` SHALL 只使用 `HIT`、`PARTIAL`、`MISS`；历史分析未保存合法值时 SHALL 按事实路径状态保守推导，且该字段只表示挂接程度。

#### Scenario: 历史事实缺少 library fit
- **WHEN** 事实的 library fit 为空或 `UNKNOWN`
- **THEN** `FORMAL` SHALL 映射为 `HIT`、`PROVISIONAL` SHALL 映射为 `PARTIAL`、`UNCLASSIFIED` SHALL 映射为 `MISS`
- **AND** 报告 SHALL NOT 把该映射解释为诊断正确率

### Requirement: 诊断证据质量必须具有独立发布门禁
系统 SHALL 提供可重复执行的诊断证据质量检查，区分阻断性完整性问题与渐进式效果债务，并 SHALL 以非零退出码阻止不一致证据进入生产。

#### Scenario: 临时候选身份断链
- **WHEN** `PROVISIONAL` 事实缺少 code、code 找不到成长候选或候选父知识点无效
- **THEN** 质量门禁 SHALL 失败

#### Scenario: 分析建议没有完整投影
- **WHEN** 已保存分析包含建议但事实数与可投影建议数不一致
- **THEN** 质量门禁 SHALL 失败
- **AND** 输出 SHALL 披露缺失分析数和缺失事实数

#### Scenario: 效果样本不足
- **WHEN** V5 发布后的新诊断样本不足或正式命中占比仍低
- **THEN** 质量门禁 MAY 通过结构检查
- **AND** 报告 SHALL 把正式命中率、临时候选率、未分类率和样本时间窗列为质量债务
- **AND** 系统 SHALL NOT 宣称内容精修已经改善学生诊断效果

### Requirement: 测试点语义证据必须可追溯且不替代代码证据
诊断证据包 SHALL 为已执行测试点保存稳定语义引用和通过状态；模型可以把它作为评测覆盖证据，但 MUST 结合题目、代码和实际行为差距判断错因。

#### Scenario: 失败测试点具有语义快照
- **WHEN** 诊断证据包包含一个已失败且具有语义快照的测试点
- **THEN** 简报 SHALL 提供合法的 `judge:test-intent:<semantic-code>` 证据引用
- **AND** 语义事实 SHALL 明确包含该测试点的通过状态

#### Scenario: 测试意图与代码证据冲突
- **WHEN** 测试点意图暗示某类覆盖但学生代码和判题行为不支持相应错因
- **THEN** 诊断 SHALL 以代码和实际行为为准
- **AND** SHALL NOT 仅凭测试意图生成正式错因或标准库命中

#### Scenario: 历史语义快照与当前内容版本不同
- **WHEN** 一个历史判题结果保存的语义快照与当前测试点内容不同
- **THEN** 该次历史诊断 SHALL 使用保存时快照
- **AND** 审计 SHALL 能同时识别测试点身份和快照语义 code

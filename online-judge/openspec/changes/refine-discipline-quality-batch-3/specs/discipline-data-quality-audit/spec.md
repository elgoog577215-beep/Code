## ADDED Requirements

### Requirement: 第三批学科质量选择必须区分结构引用与有限行为证据
第三批候选 SHALL 以启用规范能力点、易错点和提升点的去重结构引用为主证据，并 SHALL 只把诊断路径出现次数作为有上限的加权证据；报告必须分别披露两种口径和行为样本限制。

#### Scenario: 行为路径重复出现
- **WHEN** 一个模板知识点同时具有规范结构引用并在诊断路径中重复出现
- **THEN** 选择排序 SHALL 增加最多 4 次行为路径的有限权重
- **AND** 人工审校 SHALL 验证完整路径，不能只按中文名称自动入选

#### Scenario: 行为证据不足
- **WHEN** 诊断事实样本小或多数路径不是正式标准库挂接
- **THEN** 系统 SHALL NOT 把诊断次数解释为学生错误发生率
- **AND** 结构引用和学科分支完整性 SHALL 继续作为主要选择依据

### Requirement: 第三批迁移必须证明质量债务下降且业务数据稳定
Flyway V4 SHALL 在同一生产 V3 基线上新增不少于 30 个 `informatics-knowledge-discipline-v3` 精修知识点和 12 个 `informatics-discipline-quality-v3` 正式提升点，并 SHALL 保持结构门禁与业务计数稳定。

#### Scenario: 第三批隔离恢复完成
- **WHEN** V4 在生产备份的隔离恢复库执行完成
- **THEN** 累计人工精修知识点 SHALL 不少于 84 个
- **AND** 模板化知识点描述 SHALL 从 532 降到不高于 502
- **AND** 缺少提升点的启用能力点 SHALL 从 53 降到不高于 41
- **AND** V2/V3 已归零的结构问题 SHALL 继续为零

#### Scenario: 业务表出现非预期变化
- **WHEN** V4 前后题目、测试用例、提交、提交分析、诊断事实或学生反馈计数发生变化
- **THEN** 发布门禁 SHALL 失败
- **AND** 系统 SHALL NOT 用标准库内容增量解释业务事实变化

### Requirement: 兼容占位能力不得计入正式提升点补齐
第三批质量审计 SHALL 单独识别 `SK_COMPAT_*` 或明确声明用于兼容旧标签的能力点，并 SHALL 禁止为这些占位能力新增 `informatics-discipline-quality-v3` 提升点。

#### Scenario: 第三批提升点关联兼容能力
- **WHEN** 一个 V4 提升点的 `skill_unit_code` 指向兼容占位能力
- **THEN** 学科质量门禁 SHALL 失败
- **AND** 报告 SHALL 把该能力保留在待合并或退役清单而不是正式教学缺口

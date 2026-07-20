# teaching-contest-application-scenarios Specification

## Purpose
TBD - created by archiving change add-teaching-contest-scenario-layer. Update Purpose after archive.
## Requirements
### Requirement: 应用场景必须挂在统一知识树的正式能力点下
系统 SHALL 将教学与竞赛应用场景作为知识点下正式能力点的应用语境保存；每个启用场景 MUST 引用一个启用的 `KNOWLEDGE_POINT` 和一个以该知识点为主锚点的启用正式能力点，MUST NOT 新建高中库与竞赛库两套平行层级。

#### Scenario: 保存课堂场景
- **WHEN** 一个课堂任务场景进入正式标准库
- **THEN** 场景 SHALL 保存稳定场景 code、知识点 code 和正式能力点 code
- **AND** 场景 MUST NOT 通过历史兼容能力或独立课堂知识树承载

#### Scenario: 保存竞赛场景
- **WHEN** 一个竞赛任务场景与既有课堂能力对应
- **THEN** 场景 SHALL 复用相同知识点和能力点
- **AND** 竞赛术语、数据范围和评测要求 SHALL 保存在应用语境字段中

### Requirement: 每个迁移主题必须形成课堂与竞赛成对场景
系统 SHALL 用稳定 `transferPairCode` 把同一能力的课堂场景和竞赛场景组成迁移对；每个启用迁移对 MUST 恰好包含一个 `CLASSROOM` 场景和一个 `CONTEST` 场景，并保存从基础活动到约束任务的迁移说明。

#### Scenario: 完整迁移对
- **WHEN** AI 或教师读取一个已审校迁移主题
- **THEN** 系统 SHALL 返回一个课堂场景和一个竞赛场景
- **AND** 两个场景 SHALL 引用同一正式能力点并使用不同场景 code

#### Scenario: 场景对缺失一端
- **WHEN** 数据迁移只生成课堂场景或只生成竞赛场景
- **THEN** 质量门禁 MUST 阻断迁移或发布

### Requirement: 场景内容必须可观察、可检查、可迁移
每个正式场景 SHALL 分别保存任务情境、学生任务、可观察证据、常见失败、教师检查动作、学生自检、约束画像、成功标准和迁移说明；课堂场景 MUST 体现教学活动与形成性检查，竞赛场景 MUST 体现题面约束、评测边界或复杂度梯度。

#### Scenario: 教师读取课堂场景
- **WHEN** 教师展开知识点诊断层
- **THEN** 场景 SHALL 明确学生要完成的可观察任务和教师据以判断的成功标准
- **AND** 教师动作 MUST 是检查或追问，不得只是“加强练习”等泛化文本

#### Scenario: AI 读取竞赛场景
- **WHEN** AI 为已选能力生成提高建议
- **THEN** 场景 SHALL 提供数据范围、失败类型、边界或部分分梯度等竞赛语境
- **AND** 场景 MUST NOT 直接提供完整解法或覆盖当前提交证据

### Requirement: 场景引用必须闭合且可审计
启用场景引用的易错点和提升点 MUST 属于同一能力点；来源框架、来源链接、审校状态和库版本 MUST 可审计。所有首批正式能力点 SHALL 至少拥有一条提升路径，历史兼容能力 MUST NOT 承载新增正式提升点。

#### Scenario: 场景引用跨能力条目
- **WHEN** 场景的易错点或提升点属于另一个能力点
- **THEN** 迁移质量门禁 MUST 失败并指出非法引用

#### Scenario: 正式能力仍无提升路径
- **WHEN** V7 迁移完成后存在未覆盖的启用非兼容能力点
- **THEN** 迁移质量门禁 MUST 阻断发布

### Requirement: 场景读取必须相关且有界
系统 SHALL 只返回与当前知识点或已选能力点相关的启用场景，并按稳定顺序去重；单个 AI 标准库包最多返回 12 个场景，避免将全库场景倾倒给模型。

#### Scenario: 展开单个知识点
- **WHEN** 教师或 AI 展开一个知识点的诊断层
- **THEN** 每个能力点 SHALL 只包含直接关联的启用场景
- **AND** 场景 SHALL 按排序号和稳定 code 返回

#### Scenario: 一次诊断选中多个能力点
- **WHEN** 最终标准库包聚合多个问题的已选能力点
- **THEN** 系统 SHALL 合并相关场景、按 code 去重并限制为最多 12 个
- **AND** 系统 MUST NOT 因场景超限而丢弃已选能力点、易错点或提升点

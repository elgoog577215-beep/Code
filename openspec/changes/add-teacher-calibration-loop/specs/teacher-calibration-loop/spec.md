## ADDED Requirements

### Requirement: 系统必须从教师校正生成结构化校准信号
系统 SHALL 从现有教师诊断校正记录中生成结构化 `teacherCalibrationSignal`，用于表达教师确认过的误判模式和后续诊断约束。

#### Scenario: 学生存在历史教师校正
- **GIVEN** 同一学生的历史提交存在教师诊断校正
- **WHEN** 系统构建当前提交的诊断证据包
- **THEN** 学习记忆 SHALL 包含教师校准模式
- **AND** 模式 SHALL 包含原始标签、教师修正标签、校正次数、证据提交、证据引用和最近教师备注摘要

#### Scenario: 没有教师校正历史
- **GIVEN** 当前学生没有可用教师诊断校正
- **WHEN** 系统构建当前提交的诊断证据包
- **THEN** 系统 SHALL NOT 输出误导性的教师校准模式
- **AND** 诊断行为 SHALL 与现有流程保持兼容

### Requirement: 模型诊断 brief 必须接入教师校准
模型诊断 brief SHALL 暴露教师校准摘要、候选信号、允许标签和证据引用，让外部模型可以参考教师已确认的校正方向。

#### Scenario: 教师校准模式与候选错因相关
- **GIVEN** 教师校准模式包含修正后的 issue tag 或 fine-grained tag
- **WHEN** 系统生成 `ModelDiagnosisBrief`
- **THEN** brief SHALL 将修正后的标签加入允许标签
- **AND** brief SHALL 将校准模式加入候选信号或校准摘要
- **AND** brief SHALL 包含对应校准 evidenceRefs

### Requirement: 诊断结果必须暴露教师校准状态
提交诊断响应 SHALL 输出可选 `teacherCalibrationSignal`，说明教师校准是否支持当前诊断、被应用到当前诊断或与当前诊断冲突。

#### Scenario: 当前诊断与教师修正标签一致
- **GIVEN** 教师校准模式的修正标签出现在当前诊断标签中
- **WHEN** 诊断完成
- **THEN** `teacherCalibrationSignal.status` SHALL 为 `SUPPORTED`
- **AND** 诊断 evidenceRefs SHALL 包含教师校准证据引用

#### Scenario: 当前诊断命中教师曾纠正的原始标签
- **GIVEN** 教师曾将某个原始标签修正为另一个标签
- **AND** 当前诊断仍只命中该原始标签而缺少教师修正标签
- **WHEN** 诊断完成
- **THEN** `teacherCalibrationSignal.status` SHALL 为 `CONFLICT_NEEDS_REVIEW`
- **AND** 系统 SHALL 降低诊断置信度
- **AND** 系统 SHALL 在 uncertainty 或 teacherNote 中说明需要教师复核的原因

### Requirement: 教师校准不得覆盖当前提交事实
教师校准 SHALL 作为辅助约束参与诊断，但不得直接覆盖当前代码、评测结果和可见证据。

#### Scenario: 当前提交证据与历史校正不一致
- **GIVEN** 当前提交的规则信号和评测事实不支持教师历史修正标签
- **WHEN** 系统应用教师校准
- **THEN** 系统 SHALL 保留当前诊断标签
- **AND** 系统 SHALL 通过不确定性或教师复核提示表达冲突
- **AND** 系统 SHALL NOT 删除当前提交证据引用

### Requirement: AI 质量概览必须包含教师校准闭环维度
AI 质量概览 SHALL 增加 `TEACHER_CALIBRATION_LOOP` 维度，用于评估教师校正是否已经反哺在线诊断。

#### Scenario: 教师校正已应用到诊断
- **GIVEN** 当前作业存在教师校正
- **AND** 近期诊断包含 `SUPPORTED` 或 `APPLIED` 教师校准信号
- **WHEN** 教师读取 AI 质量概览
- **THEN** 质量维度 SHALL 包含 `TEACHER_CALIBRATION_LOOP`
- **AND** 该维度 SHALL 输出状态、分数、摘要、证据引用和建议动作

#### Scenario: 教师校正未进入在线诊断
- **GIVEN** 当前作业存在教师校正
- **AND** 近期诊断没有任何教师校准信号
- **WHEN** 教师读取 AI 质量概览
- **THEN** `TEACHER_CALIBRATION_LOOP` SHALL 标记为需要行动
- **AND** 建议动作 SHALL 要求检查教师校正是否进入证据包和模型 brief

### Requirement: 教师校准闭环必须可验证
教师校准闭环 SHALL 有自动化测试覆盖结构化信号生成、模型 brief 接入、诊断冲突处理和 AI 质量维度。

#### Scenario: 执行验证
- **WHEN** 运行相关后端测试
- **THEN** 测试 SHALL 通过
- **AND** 测试 SHALL 覆盖教师校准支持、冲突降置信、无校正兼容和质量维度

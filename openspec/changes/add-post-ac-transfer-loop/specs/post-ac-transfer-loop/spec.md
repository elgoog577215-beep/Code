## ADDED Requirements

### Requirement: 系统必须输出 AC 后迁移信号

系统 SHALL 在学生轨迹中输出结构化 AC 后迁移信号，用于区分未通过、刚通过、需要复盘、已有复盘证据、可迁移和已验证迁移。

#### Scenario: 学生刚通过但缺少复盘证据
- **GIVEN** 学生在作业内某题最新提交为 `ACCEPTED`
- **AND** 该题没有 Coach 回答质量、复盘推荐完成或后续同能力迁移提交证据
- **WHEN** 读取学生轨迹
- **THEN** 系统 SHALL 输出 `postAcTransferSignal.phase` 为 `REFLECTION_NEEDED` 或 `JUST_ACCEPTED`
- **AND** 信号 SHALL 包含证据引用、推荐动作和目标能力或错因

#### Scenario: 学生已有可迁移证据
- **GIVEN** 学生通过某题后已经留下证据充分的 Coach 回答或完成同能力后续练习
- **WHEN** 读取学生轨迹
- **THEN** 系统 SHALL 输出 `postAcTransferSignal.phase` 为 `TRANSFER_READY` 或 `TRANSFER_VERIFIED`
- **AND** 信号 SHALL 说明迁移证据来源

### Requirement: 推荐系统必须消费 AC 后迁移信号

推荐系统 SHALL 在学生通过题目但缺少复盘迁移证据时生成复盘或迁移推荐，并保持现有推荐字段兼容。

#### Scenario: 缺少复盘证据时生成复盘推荐
- **GIVEN** 学生存在 `REFLECTION_NEEDED` 的 AC 后迁移信号
- **WHEN** 获取学生推荐
- **THEN** 推荐列表 SHALL 包含 `POST_AC_REFLECTION` 或 `TRANSFER_TO_NEW_PROBLEM` 策略
- **AND** 推荐 SHALL 包含学习假设、预期完成信号和 fallback 动作

#### Scenario: 已验证迁移时不重复推荐复盘
- **GIVEN** 学生某能力已达到 `TRANSFER_VERIFIED`
- **WHEN** 获取学生推荐
- **THEN** 系统 SHALL NOT 因该题继续生成同一个 AC 后复盘推荐

### Requirement: 教师端必须可见 AC 后迁移缺口

教师工作台 SHALL 展示通过后仍缺复盘迁移证据的学生和班级摘要，帮助教师把通过率转化为可迁移学习判断。

#### Scenario: 教师查看课堂过程
- **GIVEN** 当前作业存在已通过但缺迁移证据的学生
- **WHEN** 教师读取作业概览
- **THEN** 系统 SHALL 返回待迁移学生数和迁移摘要
- **AND** 对应学生行 SHALL 包含最新 AC 后迁移信号

### Requirement: AI 质量概览必须包含 AC 后迁移闭环维度

AI 质量概览 SHALL 增加 AC 后迁移闭环维度，用于评估系统是否把通过结果沉淀为复盘和迁移证据。

#### Scenario: AC 后复盘证据不足
- **GIVEN** 当前作业存在多个 `REFLECTION_NEEDED` 信号
- **WHEN** 教师读取 AI 质量概览
- **THEN** 质量维度 SHALL 包含 `POST_AC_TRANSFER_LOOP`
- **AND** 该维度 SHALL 给出状态、分数、摘要、证据引用和推荐改进动作

### Requirement: AC 后迁移能力必须可验证

AC 后迁移闭环 SHALL 有后端测试和前端类型验证，覆盖主要状态和推荐消费场景。

#### Scenario: 执行验证
- **WHEN** 运行相关后端测试和前端类型检查
- **THEN** 检查 SHALL 通过
- **AND** 测试 SHALL 覆盖缺复盘证据、已有复盘证据、迁移推荐和 AI 质量维度

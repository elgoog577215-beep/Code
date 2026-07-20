## ADDED Requirements

### Requirement: 第四批必须增加细颗粒易错点和可执行提升路径
Flyway V6 SHALL 新增 22 个 `informatics-discipline-quality-v4` 易错点和 22 个同版本提升点；每个易错点 SHALL 描述可观察错误行为，每个提升点 SHALL 提供学生可执行练习和教师可观察验收。

#### Scenario: 保存第四批易错点
- **WHEN** 一个第四批易错点进入正式数据库
- **THEN** 它 SHALL 归属于启用且非 `SK_COMPAT_*` 的正式能力点
- **AND** `misconception`、`symptom`、`repair_strategy`、主知识点和相关知识点 SHALL 完整

#### Scenario: 保存第四批提升点
- **WHEN** 一个第四批提升点进入正式数据库
- **THEN** 它 SHALL 关联至少一个同能力下的启用易错点
- **AND** `improvement_goal`、`practice_strategy`、`student_benefit` 和 `teacher_explanation` SHALL 完整

### Requirement: 第四批必须优先关闭正式提升路径缺口
第四批 22 个提升点中 SHALL 有 14 个为此前没有启用提升点的 BASIC 或 CONTEST 正式能力补齐首条路径，其余 8 个 SHALL 为 MATH 或 ENG 正式能力补充不同于既有训练的专项路径。

#### Scenario: BASIC 与 CONTEST 补齐完成
- **WHEN** V6 执行完成
- **THEN** 缺少启用提升点的能力总数 SHALL 从 41 降到不高于 27
- **AND** 第四批 SHALL NOT 为兼容占位能力制造提升点

#### Scenario: MATH 与 ENG 增加第二训练路径
- **WHEN** 同一能力已经存在启用提升点
- **THEN** 新提升点 SHALL 使用不同练习对象、检查表或验收证据
- **AND** 新提升点 SHALL 关联第四批新增的细颗粒易错点

### Requirement: 第四批规范条目必须保持三处一致
每个第四批易错点和提升点 SHALL 使用同一稳定 code 同步到规范化主表、启用平铺快照和 `MAPPED` legacy mapping，并 SHALL 保持能力归属、主知识点、相关知识点和关联错因一致。

#### Scenario: 第四批条目缺少兼容结构
- **WHEN** 任一第四批规范条目缺少同 code 快照或映射，或关键归属不一致
- **THEN** 学科质量门禁 SHALL 失败
- **AND** 应用 SHALL NOT 发布部分同步内容

#### Scenario: AI 展开第四批代表性知识点
- **WHEN** AI 或教师展开 BASIC、MATH、ENG、CONTEST 任一第四批代表性知识点
- **THEN** 导航 SHALL 从规范化结构返回所属能力、第四批易错点和对应提升点
- **AND** 平铺快照 SHALL 只承担兼容用途

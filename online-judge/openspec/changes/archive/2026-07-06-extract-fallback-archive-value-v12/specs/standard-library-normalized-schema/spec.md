## ADDED Requirements

### Requirement: 兜底存档价值必须按三类榨取
AI 标准库 SHALL 将兜底存档素材按价值处理方式分为 A 类直接精修吸收、B 类提炼类型后重写、C 类继续存档观察，并 MUST 保证只有人工精修后的 A/B 结果进入活跃标准库。

#### Scenario: A 类主题迁移为活跃条目
- **WHEN** 兜底存档知识点被标记为 A 类直接精修吸收
- **THEN** 标准库 SHALL 为该主题提供手写能力点或易错点
- **AND** 正式条目 SHALL 使用具体错误行为、触发条件、代码表现或验证动作描述教学语义

#### Scenario: B 类只吸收类型和方向
- **WHEN** 兜底存档知识点被标记为 B 类提炼类型后重写
- **THEN** 标准库 SHALL NOT 直接复用兜底条目文本
- **AND** 标准库 SHALL 将兜底暴露的错因类型、知识缺口或训练方向重写为手写条目

#### Scenario: C 类继续留档
- **WHEN** 兜底存档知识点暂不适合进入活跃标准库
- **THEN** 该素材 SHALL 保留在冷存档入口
- **AND** 该素材 SHALL NOT 被活跃 seed 自动插入为 enabled 运行时条目

## MODIFIED Requirements

### Requirement: 兜底素材必须持续迁移为智能标准库
AI 标准库 SHALL 将自动兜底层视为待吸收素材池，并 SHALL 通过手写版本批次把高价值主题迁移为智能标准库条目；迁移时 MUST 区分直接吸收、类型提炼重写和继续存档三类处理方式。

#### Scenario: 吸收二期进入标准库 seed
- **WHEN** 系统加载 AI 标准库 seed
- **THEN** 标准库 SHALL 包含 `SK_V11_`、`MP_V11_` 和 `IP_V11_` 前缀的兜底吸收手写条目
- **AND** V11 吸收条目 SHALL 至少包含 8 个能力点、24 个易错点和 8 个提升点

#### Scenario: 吸收二期不复用兜底模板
- **WHEN** 系统校验 V11 吸收条目文本
- **THEN** V11 条目 SHALL NOT 使用“适用条件混用”“理解或应用偏差”“没有把知识点定义、适用条件或边界要求准确落实”等模板表达
- **AND** V11 易错点 SHALL 归属于合法 V11 能力点
- **AND** V11 提升点 SHALL 关联至少一个 V11 易错点

#### Scenario: 吸收三期榨取 archive-only 高价值主题
- **WHEN** 系统加载 AI 标准库 seed
- **THEN** 标准库 SHALL 包含 `SK_V12_`、`MP_V12_` 和 `IP_V12_` 前缀的兜底存档价值吸收条目
- **AND** V12 吸收条目 SHALL 至少包含 8 个能力点、20 个易错点和 6 个提升点
- **AND** V12 条目 SHALL 覆盖 A 类直接精修吸收和 B 类类型提炼重写两种来源

#### Scenario: 吸收三期不复用兜底模板
- **WHEN** 系统校验 V12 吸收条目文本
- **THEN** V12 条目 SHALL NOT 使用“适用条件混用”“理解或应用偏差”“没有把知识点定义、适用条件或边界要求准确落实”“代码落点不清”等模板表达
- **AND** V12 易错点 SHALL 归属于合法 V12 能力点
- **AND** V12 提升点 SHALL 关联至少一个 V12 易错点

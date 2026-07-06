## MODIFIED Requirements

### Requirement: 兜底素材必须持续迁移为智能标准库
AI 标准库 SHALL 将已经人工吸收的自动兜底高价值主题保留为智能标准库条目；后续如需参考未吸收兜底素材，MUST 只从备份目录进行人工离线查看，运行时代码 SHALL NOT 枚举、加载或分类自动兜底库。

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

#### Scenario: 未吸收兜底素材只在备份目录
- **WHEN** 研发或教师需要查看删除前的自动兜底素材
- **THEN** 系统 SHALL 只在 `backups/standard-library/` 下保留静态备份文件
- **AND** 后端、前端、Seeder、候选包和外部 AI 标准库上下文 SHALL NOT 读取该备份目录

### Requirement: 自动兜底层必须退役为存档素材池
AI 标准库 SHALL 将自动生成兜底层从活跃标准库和代码内冷存档中彻底退役，并 MUST 只保留静态备份目录用于人工审计、回滚参考或离线研究。

#### Scenario: 活跃标准库不再包含兜底 seed
- **WHEN** 系统加载活跃 AI 标准库 seed
- **THEN** 活跃 seed SHALL NOT 包含可识别为自动生成兜底的能力点或易错点
- **AND** 活跃 seed SHALL 继续包含已经人工吸收的 V10/V11/V12 智能条目

#### Scenario: 兜底素材只作为静态备份文件
- **WHEN** 仓库中保留自动兜底备份
- **THEN** 备份 SHALL 位于 `backups/standard-library/` 目录
- **AND** 备份 SHALL NOT 以 Java、TypeScript 或运行时配置形式存在
- **AND** 标准库 seed catalog SHALL NOT 提供自动兜底 seed 枚举入口

#### Scenario: 历史兜底记录启动时禁用
- **WHEN** 数据库中已经存在自动兜底能力点或易错点
- **THEN** Seeder SHALL 将这些历史兜底记录设置为 disabled
- **AND** 候选包、搜索定位和外部 AI 标准库上下文 SHALL 默认不返回 disabled 兜底记录

### Requirement: 兜底存档价值必须按三类榨取
AI 标准库 SHALL 保留已完成的 A 类直接精修吸收和 B 类类型提炼重写成果，但 SHALL NOT 在运行时代码中继续维护 C 类 archive-only 分类；未吸收的历史素材只允许存在于静态备份目录。

#### Scenario: A 类主题迁移为活跃条目
- **WHEN** 兜底存档知识点已经被标记为 A 类直接精修吸收
- **THEN** 标准库 SHALL 为该主题提供手写能力点或易错点
- **AND** 正式条目 SHALL 使用具体错误行为、触发条件、代码表现或验证动作描述教学语义

#### Scenario: B 类只吸收类型和方向
- **WHEN** 兜底存档知识点已经被标记为 B 类提炼类型后重写
- **THEN** 标准库 SHALL NOT 直接复用兜底条目文本
- **AND** 标准库 SHALL 将兜底暴露的错因类型、知识缺口或训练方向重写为手写条目

#### Scenario: C 类不再运行时分类
- **WHEN** 兜底存档知识点未被吸收为手写条目
- **THEN** 该素材 SHALL NOT 被后端运行时代码分类、枚举或加载
- **AND** 该素材 MAY 仅作为静态备份文件中的历史材料存在

## REMOVED Requirements

### Requirement: 自动兜底生成内容不得保留明显模板化低质表达
**Reason**: 自动兜底生成器和代码内存档枚举入口被删除后，系统不再生成、加载或校验自动兜底内容；质量门槛应约束正式手写标准库，而不是约束仅作历史备份的静态文件。

**Migration**: 删除前的自动兜底素材保存到 `backups/standard-library/`；后续扩库必须通过 V13 之后的手写主题包或人工治理流程进入正式标准库。

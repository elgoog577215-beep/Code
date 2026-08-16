## ADDED Requirements

### Requirement: 标准库正式内容不得依赖运行时 seed
AI 标准库正式内容 SHALL 以数据库规范化表为主库；运行时 SHALL NOT 通过 seed 文件、seed 类或启动播种器新增、覆盖或扩张正式标准库内容。

#### Scenario: 启动读取标准库
- **WHEN** 应用启动并需要 AI 标准库内容
- **THEN** 系统 SHALL 从 `ai_standard_skill_units`、`ai_standard_mistake_points`、`ai_standard_improvement_points` 和兼容快照读取
- **AND** 系统 SHALL NOT 调用 `AiStandardLibrarySeedCatalog.seeds()` 或 `AiStandardLibraryV*Seeds` 写入正式库

#### Scenario: 数据库为空
- **WHEN** 正式数据库缺少标准库内容
- **THEN** 系统 SHALL 暴露可诊断的内容缺失状态
- **AND** 系统 SHALL NOT 用代码 seed 静默补齐正式内容

### Requirement: 历史 seed 迁移必须写入规范化主结构
历史标准库 seed 中仍需保留的内容 SHALL 通过一次性迁移写入规范化主结构，而不是继续作为运行时 seed 保留。

#### Scenario: 迁移能力点
- **WHEN** 迁移历史能力点内容
- **THEN** 系统 SHALL upsert 到 `ai_standard_skill_units`
- **AND** SHALL 保留主知识节点、相关知识节点、学习目标和启用状态

#### Scenario: 迁移易错点
- **WHEN** 迁移历史易错点内容
- **THEN** 系统 SHALL upsert 到 `ai_standard_mistake_points`
- **AND** SHALL 保留能力点归属、误区解释、症状、修复策略和启用状态

#### Scenario: 迁移提升点
- **WHEN** 迁移历史提升点内容
- **THEN** 系统 SHALL upsert 到 `ai_standard_improvement_points`
- **AND** SHALL 保留能力点归属、提升目标、练习策略、学生收益和教师解释

### Requirement: 旧平铺快照只能作为兼容层
旧 `ai_standard_library_items` SHALL 只作为兼容快照和历史接口读取层，不得重新成为标准库正式内容主库。

#### Scenario: 教师编辑正式条目
- **WHEN** 教师编辑能力点、易错点或提升点
- **THEN** 系统 SHALL 先更新规范化主结构
- **AND** MAY 同步旧平铺快照以兼容现有接口

#### Scenario: AI 导航读取
- **WHEN** AI 导航构建标准库上下文
- **THEN** 系统 SHALL 优先读取规范化主结构
- **AND** SHALL 忽略被禁用的历史 `KB_*` 全覆盖模板条目

## REMOVED Requirements

### Requirement: 手写提升点必须进入规范标准库结构
**Reason**: 该要求绑定了 “V8 手写提升点 seed” 的历史实现。新的内容来源为正式数据库，要求不应再以 seed 批次命名。

**Migration**: 将仍需保留的 V8 提升点内容通过一次性迁移写入 `ai_standard_improvement_points`，后续新增提升点通过教师治理或管理 API 写入正式数据库。

### Requirement: V8 手写易错点与提升点必须具备细颗粒质量
**Reason**: 该要求以 V8 seed 批次作为正式内容载体，和数据库主库原则冲突。

**Migration**: 将质量要求转移到数据库内容审计、教师治理审核和 AI 导航读取验证中。

### Requirement: V9 标准库扩展必须补强高频算法与工程诊断主题
**Reason**: 该要求继续要求加载 `SK_V9_`、`MP_V9_`、`IP_V9_` seed，已不符合禁止 seed 的内容治理边界。

**Migration**: 将需要保留的 V9 内容迁入正式数据库；后续补强主题直接写正式表。

### Requirement: 自动兜底层必须逐步被智能标准库吸收
**Reason**: 该要求仍以 seed 吸收批次作为运行时标准库来源。

**Migration**: 兜底素材只可作为离线迁移参考；迁移后正式内容以数据库条目为准。

### Requirement: 兜底素材必须持续迁移为智能标准库
**Reason**: 该要求允许继续围绕归档 seed 做内容扩张。

**Migration**: 后续不得从项目运行时代码读取兜底素材；需要使用时先人工审校，再通过数据库治理入口写入正式库。

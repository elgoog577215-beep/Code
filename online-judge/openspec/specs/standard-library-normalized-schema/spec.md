# standard-library-normalized-schema Specification

## Purpose
TBD - created by archiving change normalize-standard-library-schema. Update Purpose after archive.
## Requirements
### Requirement: 标准库必须复用既有知识树
AI 标准库规范结构 SHALL 复用 `informatics_knowledge_nodes` 作为知识树来源，并 SHALL 在能力点、易错点和提升点中通过知识节点 code 关联到该知识树。

#### Scenario: 能力点关联知识节点
- **WHEN** 标准库同步能力点种子
- **THEN** 系统 SHALL 保存能力点到独立能力点表，并 SHALL 保留该能力点关联的知识节点 code

#### Scenario: 不重复创建知识树
- **WHEN** 标准库规范结构初始化
- **THEN** 系统 SHALL NOT 创建另一套标准库专用知识树表来重复表达大章节、小章节和知识点

### Requirement: 标准库必须按能力易错提升分表存储
AI 标准库规范结构 SHALL 将能力点、易错点和提升点存储为独立实体，其中易错点 SHALL 归属于能力点，提升点 SHALL 归属于能力点或知识节点。

#### Scenario: 易错点归属能力点
- **WHEN** 同步一个 `MISTAKE_POINT` 或兼容 `BASIC_CAUSE` 条目
- **THEN** 系统 SHALL 将它保存为规范易错点
- **AND** SHALL 记录它关联的能力点 code 作为主归属
- **AND** SHALL NOT 把易错点的知识节点集合解释为主归属链路

#### Scenario: 能力点拥有主知识节点
- **WHEN** 同步或保存一个能力点
- **THEN** 系统 SHALL 保存一个主知识节点 code
- **AND** MAY 继续保存相关知识节点集合用于检索和上下文补充

### Requirement: 新结构不得引入独立证据模式表
AI 标准库规范结构 SHALL NOT 新增独立证据模式表；标准库 SHALL 只保存教学知识结构和轻量自然语言诊断提示，当前提交证据 SHALL 来自提交代码、评测结果、错误日志和 AI 分析输出。

#### Scenario: 同步旧证据字段
- **WHEN** 旧扁平条目包含 `evidenceSignals`、`commonCodePatterns`、`judgeSignals` 或 `requiredEvidence`
- **THEN** 规范结构同步 SHALL NOT 将这些字段写入独立证据模式表

### Requirement: 旧扁平条目必须可映射到新结构
系统 SHALL 保存旧 `layer/code` 与新结构目标之间的映射，使历史数据和旧接口可以兼容新主结构。

#### Scenario: 旧能力点映射
- **WHEN** 一个旧 `SKILL_UNIT` 种子被同步到规范能力点
- **THEN** 系统 SHALL 保存从旧 `SKILL_UNIT/code` 到规范能力点的映射

#### Scenario: 旧基础错因映射
- **WHEN** 一个旧 `BASIC_CAUSE` 种子被同步到规范结构
- **THEN** 系统 SHALL 保存从旧 `BASIC_CAUSE/code` 到规范易错点的映射

### Requirement: 标准库同步必须幂等
规范标准库同步 SHALL 可重复执行，重复执行不得产生重复能力点、易错点、提升点或旧映射。

#### Scenario: 重复运行同步器
- **WHEN** 标准库同步器连续执行两次
- **THEN** 第二次执行 SHALL NOT 增加重复规范条目，并 SHALL 保持已有 code 唯一

### Requirement: 标准库候选包必须提供结构化视图
AI 标准库候选包 SHALL 在兼容旧字段之外提供结构化视图，表达知识节点、能力点、易错点和提升点之间的层级关系。

#### Scenario: 生成结构化候选包
- **WHEN** 系统从规范标准库生成 AI 诊断候选包
- **THEN** 候选包 SHALL 优先按能力点的主知识节点分组
- **AND** 易错点 SHALL 放在所属能力点下
- **AND** 相关知识节点 SHALL 作为补充上下文，而不是第二条主路径

#### Scenario: 兼容旧字段
- **WHEN** 候选包包含结构化视图
- **THEN** 候选包 SHALL 继续保留现有 `basicCauses`、`improvementPoints`、`skillUnits` 和 `mistakePoints` 兼容字段
- **AND** SHOULD 输出 `primaryKnowledgeNodeCode` 与 `relatedKnowledgeNodeCodes` 帮助调用方区分主路径和辅助标签

### Requirement: 标准库结构视图不得替代当前提交证据
标准库结构视图 SHALL 只作为候选地图和命名体系，AI 诊断 MUST 使用当前提交代码、评测结果、错误日志和 evidenceRefs 判断是否命中。

#### Scenario: 候选存在但证据不足
- **WHEN** 标准库结构视图中存在相近易错点但当前提交证据不足
- **THEN** AI 诊断 SHALL NOT 强制输出该易错点为 HIT

### Requirement: 标准库结构视图应表达教学参考关系
AI 标准库规范结构 SHALL 表达知识树、能力点、易错点和提升点之间的教学参考关系；该结构 SHALL 帮助外接大模型统一命名和定位颗粒度，但 SHALL NOT 被解释为当前提交的证据或强制答案。

#### Scenario: 参考包传入 AI 诊断链路
- **WHEN** 系统把标准库候选包传给 AI 诊断链路
- **THEN** 候选包 SHALL 保留知识节点、能力点、易错点和提升点结构
- **AND** prompt SHALL 明确该结构是参考规范包，不是强制答案表

#### Scenario: 结构没有覆盖真实问题
- **WHEN** AI 诊断发现标准库结构没有覆盖当前提交的真实问题
- **THEN** 输出 SHALL 可以保留库外发现或空 ID
- **AND** 标准库成长流程 MAY 使用该发现作为候选线索

### Requirement: 标准库成长候选必须携带归属路径
系统 SHALL 要求 AI 生成的标准库成长候选携带最接近的标准库归属路径和审核状态，使后端能把库外发现沉淀为待审核内容，而不是直接污染正式标准库。

#### Scenario: 生成待审核错误点
- **WHEN** AI 诊断发现具体错误点缺失但上级知识点或能力点可定位
- **THEN** 标准库成长候选 SHALL 包含上级标准库路径
- **AND** 标准库成长候选 SHALL 包含建议错误点名称、错误表现、典型代码特征和学生解释话术
- **AND** 后端 SHALL 将该候选标记为待审核

#### Scenario: 找不到合适路径
- **WHEN** AI 诊断无法在标准库中找到合适上级路径
- **THEN** 标准库成长候选 SHALL 包含 AI 建议的新路径
- **AND** 候选 SHALL 保持待审核状态

### Requirement: 成长候选必须支持审核状态流转
标准库成长候选 SHALL 支持教师查看、编辑、批准、合并、拒绝和忽略，并保留来源与回滚信息。

#### Scenario: 教师批准候选
- **WHEN** 教师批准待审核成长候选
- **THEN** 系统 SHALL 将候选写入正式标准库条目
- **AND** 系统 SHALL 保留候选来源、审核状态和回滚说明

#### Scenario: 教师拒绝候选
- **WHEN** 教师拒绝成长候选
- **THEN** 系统 SHALL 保留拒绝原因
- **AND** 系统 SHALL NOT 将该候选写入正式标准库

#### Scenario: 重复候选聚合
- **WHEN** 同一层级和建议 ID 的候选再次出现
- **THEN** 系统 SHALL 增加出现次数并合并证据线索
- **AND** 系统 SHALL 保留为教师可审核记录

### Requirement: 手写提升点必须进入规范标准库结构
AI 标准库 SHALL 支持手写提升点 seed 被同步到 `ai_standard_improvement_points`，并 SHALL 在候选包中作为结构化提升方向参与诊断建议。

#### Scenario: 同步手写提升点
- **WHEN** 标准库同步器处理 V8 `IMPROVEMENT_POINT` seed
- **THEN** 系统 SHALL 保存该提升点到规范提升点表
- **AND** SHALL 保留其能力点归属、知识节点路径、提升目标、练习策略、学生收益和教师解释

#### Scenario: 提升点进入候选包
- **WHEN** 系统根据规范标准库生成 AI 诊断候选包
- **THEN** V8 手写提升点 SHALL 出现在 `improvementPoints`
- **AND** 若其能力点位于结构化知识邻域内，SHALL 能被放入对应知识组

### Requirement: V8 手写易错点与提升点必须具备细颗粒质量
V8 标准库手写条目 SHALL 避免空泛命名，并 SHALL 以可观察症状、具体误解、修复策略或提升练习表达可教学颗粒度。

#### Scenario: 易错点具备具体诊断信息
- **WHEN** V8 `MISTAKE_POINT` seed 被校验
- **THEN** 该易错点 SHALL 关联合法能力点和知识节点
- **AND** 其名称 SHALL 描述具体错误行为，而不是泛化为“理解或应用偏差”
- **AND** 其 commonMisconception SHALL 解释学生为什么会犯该错

#### Scenario: 提升点具备具体提升方向
- **WHEN** V8 `IMPROVEMENT_POINT` seed 被校验
- **THEN** 该提升点 SHALL 关联合法知识节点
- **AND** SHALL 包含适用场景、学生收益和教师解释
- **AND** SHALL 关联相关易错点，帮助教师从修错过渡到提升

### Requirement: 标准库条目必须具备教学可用的内容质量
AI 标准库 seed SHALL 通过具体条目的审校与升级提升质量；能力点、易错点和提升点 MUST 在名称、描述、误区解释、能力归属和知识路径上保持一致，并能支持教师或 AI 基于当前提交证据进行诊断表达。

#### Scenario: 能力点表达具体能力边界
- **WHEN** 系统加载 `SKILL_UNIT` seed
- **THEN** 能力点名称和描述 SHALL 说明学生需要掌握的具体判断、操作或建模边界
- **AND** 学习目标 SHALL 能指导学生如何验证该能力，而不是只给抽象标签

#### Scenario: 易错点表达真实错误行为
- **WHEN** 系统加载 `MISTAKE_POINT` seed
- **THEN** 易错点名称和定义 SHALL 描述具体错误行为或可观察症状
- **AND** commonMisconception SHALL 说明学生产生该错误的具体认知原因
- **AND** 该易错点 SHALL 归属于最贴近的能力点和知识节点

#### Scenario: 代表性条目被人工升级
- **WHEN** 本轮升级后的标准库 seed 被校验
- **THEN** 系统 SHALL 至少包含一批经过人工重写的代表性高频条目
- **AND** 测试 SHALL 校验这些条目的名称、描述、误区和归属关系包含具体教学语义

### Requirement: V9 标准库扩展必须补强高频算法与工程诊断主题
AI 标准库 SHALL 新增 V9 手写扩展条目，覆盖滑动窗口、并查集、递归回溯、堆或优先队列、拓扑排序、前缀差分、二分答案、树遍历、map 频次和输出构造等高频诊断主题。

#### Scenario: V9 扩展进入标准库 seed
- **WHEN** 系统加载 AI 标准库 seed
- **THEN** 标准库 SHALL 包含 `SK_V9_`、`MP_V9_` 和 `IP_V9_` 前缀的手写条目
- **AND** V9 条目 SHALL 至少包含 10 个能力点、30 个易错点和 10 个提升点

#### Scenario: V9 条目保持规范化归属
- **WHEN** 系统校验 V9 seed
- **THEN** 每个 V9 易错点 SHALL 归属于合法 V9 能力点
- **AND** 每个 V9 提升点 SHALL 归属于合法 V9 能力点并关联至少一个 V9 易错点
- **AND** 每个 V9 条目引用的知识节点 SHALL 存在于信息学知识树

#### Scenario: V9 条目具备具体教学语义
- **WHEN** 系统校验代表性 V9 条目
- **THEN** 测试 SHALL 验证条目描述、误区或学生收益包含具体错误行为、触发条件、验证动作或提升练习
- **AND** V9 条目名称 SHALL NOT 使用泛化的“理解或应用偏差”作为正式易错点名称

### Requirement: 自动兜底层必须逐步被智能标准库吸收
AI 标准库 SHALL 将自动兜底层保留为冷存档素材池，并 MUST 将高价值、模板化的兜底知识点逐步转化为手写智能标准库条目；只有完成人工精修的吸收条目才能进入活跃标准库。

#### Scenario: 吸收一期进入标准库 seed
- **WHEN** 系统加载 AI 标准库 seed
- **THEN** 标准库 SHALL 包含 `SK_V10_`、`MP_V10_` 和 `IP_V10_` 前缀的兜底吸收手写条目
- **AND** V10 吸收条目 SHALL 至少包含 8 个能力点、24 个易错点和 8 个提升点

#### Scenario: 被吸收知识点拥有智能条目承接
- **WHEN** 系统校验 V10 吸收条目
- **THEN** 每个被选中的兜底知识点 SHALL 至少被一个 V10 手写能力点或易错点引用
- **AND** V10 易错点 SHALL 归属于合法 V10 能力点
- **AND** V10 提升点 SHALL 关联至少一个 V10 易错点

#### Scenario: 吸收条目不复用兜底模板
- **WHEN** 系统校验 V10 吸收条目文本
- **THEN** V10 条目 SHALL NOT 使用“适用条件混用”“没有把知识点定义、适用条件或边界要求准确落实”等兜底模板表达
- **AND** V10 条目 SHALL 使用具体错误行为、触发条件、代码表现或提升练习描述教学语义

### Requirement: 自动兜底生成内容不得保留明显模板化低质表达
AI 标准库自动兜底存档素材 MUST 避免保留泛化、不可教学的模板表达；即便仅作为冷存档，也 SHALL 具备知识点相关的具体错误行为、边界、状态或建模描述。

#### Scenario: 兜底易错点名称清除模板病
- **WHEN** 系统校验自动兜底存档易错点
- **THEN** 易错点名称 SHALL NOT 包含“适用条件混用”或“理解或应用偏差”
- **AND** 名称 SHALL 指向边界、状态、建模、数值、读写、调试或表达等具体问题类型

#### Scenario: 兜底易错点定义清除空泛模板
- **WHEN** 系统校验自动兜底存档条目文本
- **THEN** 条目描述 SHALL NOT 包含“没有把知识点定义、适用条件或边界要求准确落实”
- **AND** 条目描述 SHALL 说明该知识点在代码中可能出现的具体偏差

### Requirement: 兜底素材必须持续迁移为智能标准库
AI 标准库 SHALL 将自动兜底层视为待吸收素材池，并 SHALL 通过手写版本批次把高价值主题迁移为智能标准库条目。

#### Scenario: 吸收二期进入标准库 seed
- **WHEN** 系统加载 AI 标准库 seed
- **THEN** 标准库 SHALL 包含 `SK_V11_`、`MP_V11_` 和 `IP_V11_` 前缀的兜底吸收手写条目
- **AND** V11 吸收条目 SHALL 至少包含 8 个能力点、24 个易错点和 8 个提升点

#### Scenario: 吸收二期不复用兜底模板
- **WHEN** 系统校验 V11 吸收条目文本
- **THEN** V11 条目 SHALL NOT 使用“适用条件混用”“理解或应用偏差”“没有把知识点定义、适用条件或边界要求准确落实”等模板表达
- **AND** V11 易错点 SHALL 归属于合法 V11 能力点
- **AND** V11 提升点 SHALL 关联至少一个 V11 易错点

### Requirement: 自动兜底层必须退役为存档素材池
AI 标准库 SHALL 将自动生成兜底层从活跃标准库运行链路中退役，并 MUST 保留可枚举的冷存档素材池用于审计、回滚参考和后续人工吸收。

#### Scenario: 活跃标准库不再包含兜底 seed
- **WHEN** 系统加载活跃 AI 标准库 seed
- **THEN** 活跃 seed SHALL NOT 包含可识别为自动生成兜底的能力点或易错点
- **AND** 活跃 seed SHALL 继续包含已经人工吸收的 V10/V11 智能条目

#### Scenario: 兜底素材进入冷存档入口
- **WHEN** 系统枚举自动兜底存档素材
- **THEN** 存档素材 SHALL 保留可识别的自动兜底能力点和易错点
- **AND** 存档素材 SHALL NOT 被活跃 seed 自动插入为 enabled 运行时条目

#### Scenario: 历史兜底记录启动时禁用
- **WHEN** 数据库中已经存在自动兜底能力点或易错点
- **THEN** Seeder SHALL 将这些历史兜底记录设置为 disabled
- **AND** 候选包、搜索定位和外部 AI 标准库上下文 SHALL 默认不返回 disabled 兜底记录


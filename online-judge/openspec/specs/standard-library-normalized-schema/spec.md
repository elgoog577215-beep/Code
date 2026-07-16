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

AI 标准库规范结构 SHALL 将能力点、易错点和提升点存储为独立实体，其中易错点 SHALL 归属于能力点，提升点 SHALL 归属于能力点或知识节点。正式标准库新增、编辑、停用和成长候选批准 SHALL 以这些规范化实体为主写入目标，旧平铺条目只作为兼容快照。

#### Scenario: 易错点归属能力点
- **WHEN** 同步一个 `MISTAKE_POINT` 或兼容 `BASIC_CAUSE` 条目
- **THEN** 系统 SHALL 将它保存为规范易错点
- **AND** SHALL 记录它关联的能力点 code 作为主归属
- **AND** SHALL NOT 把易错点的知识节点集合解释为主归属链路

#### Scenario: 能力点拥有主知识节点
- **WHEN** 同步或保存一个能力点
- **THEN** 系统 SHALL 保存一个主知识节点 code
- **AND** MAY 继续保存相关知识节点集合用于检索和上下文补充

#### Scenario: 教师新增条目写入规范结构
- **WHEN** 教师或后端服务新增一个正式能力点、易错点或提升点
- **THEN** 系统 SHALL 先写入对应规范化表
- **AND** 系统 SHALL 同步同一 `layer/code` 的旧平铺快照用于兼容

#### Scenario: 教师编辑或停用条目同步规范结构
- **WHEN** 教师或后端服务编辑、改码或停用一个正式标准库条目
- **THEN** 系统 SHALL 更新对应规范化表
- **AND** 系统 SHALL 更新旧平铺快照
- **AND** 后续导航读取 SHALL 以规范化条目的启用状态和内容为准

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

标准库成长候选 SHALL 支持教师查看、编辑、批准、合并、拒绝和忽略，并保留来源与回滚信息。教师批准候选后，正式入库 SHALL 写入规范化主结构，并同步兼容快照。

#### Scenario: 教师批准候选
- **WHEN** 教师批准待审核成长候选
- **THEN** 系统 SHALL 将候选写入正式规范化标准库条目
- **AND** 系统 SHALL 同步旧平铺快照
- **AND** 系统 SHALL 保留候选来源、审核状态和回滚说明

#### Scenario: 审核入库后进入导航读取
- **WHEN** 教师批准成长候选并写入正式标准库
- **THEN** 新正式条目 SHALL 在后续标准库导航读取中可见
- **AND** 系统 SHALL 保留候选中的错误表现、典型代码特征、学生解释和证据引用
- **AND** 如候选提供相似正式条目，系统 SHOULD 继承其能力点和知识点锚点

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
- **AND** 导航包和外部 AI 标准库上下文 SHALL 默认不返回 disabled 兜底记录

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

### Requirement: V13 高质量扩库必须提升数量且防止凑数
AI 标准库 SHALL 新增 V13 手写扩库条目，在显著提升活跃标准库数量的同时，MUST 保持每个条目具备合法知识路径、明确能力归属、具体错误行为和可执行提升动作。

#### Scenario: V13 扩展进入标准库 seed
- **WHEN** 系统加载 AI 标准库 seed
- **THEN** 标准库 SHALL 包含 `SK_V13_`、`MP_V13_` 和 `IP_V13_` 前缀的手写条目
- **AND** V13 条目 SHALL 至少包含 12 个能力点、36 个易错点和 12 个提升点

#### Scenario: V13 条目保持规范化归属
- **WHEN** 系统校验 V13 seed
- **THEN** 每个 V13 易错点 SHALL 归属于合法 V13 能力点
- **AND** 每个 V13 提升点 SHALL 归属于合法 V13 能力点并关联至少一个 V13 易错点
- **AND** 每个 V13 条目引用的知识节点 SHALL 存在于信息学知识树

#### Scenario: V13 条目防止低质量凑数
- **WHEN** 系统校验 V13 seed 文本
- **THEN** V13 条目 SHALL NOT 使用“理解或应用偏差”“适用条件混用”“代码落点不清”“没有把知识点定义、适用条件或边界要求准确落实”等模板表达
- **AND** 代表性 V13 易错点 SHALL 描述具体触发条件、代码症状、状态偏差或验证动作
- **AND** 代表性 V13 提升点 SHALL 描述学生可执行的复盘练习或检查表

### Requirement: 标准库必须吸收高中真题高频算法主题
AI 标准库 SHALL 在统一信息学知识树下吸收高中技术选考真题中的高频算法主题，并 SHALL 使用高中术语作为正式条目主名或主要可读入口。

#### Scenario: V14 高中术语扩展进入标准库
- **WHEN** 系统加载 AI 标准库 seed
- **THEN** 标准库 SHALL 包含 `SK_V14_`、`MP_V14_` 和 `IP_V14_` 前缀的高中术语优先手写条目
- **AND** V14 条目 SHALL 覆盖多个高中真题缺口主题，包括排序细节、擂台法、游程编码、多数投票、状态标记、计数排序、链表和区间调度中的至少六类

#### Scenario: V14 条目保持统一知识树归属
- **WHEN** 系统校验 V14 seed
- **THEN** 每个 V14 易错点 SHALL 归属于合法 V14 能力点
- **AND** 每个 V14 提升点 SHALL 归属于合法 V14 能力点并关联至少一个 V14 易错点
- **AND** 每个 V14 条目引用的知识节点 SHALL 存在于信息学知识树
- **AND** 系统 SHALL NOT 通过 V14 建立高中库和竞赛库两套平行分类

#### Scenario: V14 条目具备高中术语和通用别名
- **WHEN** 教师按高中术语或通用竞赛术语搜索标准库
- **THEN** V14 条目的名称、描述、教师解释、错因说明或知识节点 code SHALL 能承接这些同义叫法
- **AND** V14 条目 SHALL NOT 使用“理解或应用偏差”“适用条件混用”等模板化名称作为正式表达

### Requirement: 规范标准库必须作为知识点以下诊断层
AI 标准库规范结构 SHALL 被解释为知识点以下的诊断层：能力点 SHALL 挂到一个主知识点，易错点 SHALL 挂到能力点，提升点 SHALL 挂到能力点或知识点。

#### Scenario: 能力点主归属知识点
- **WHEN** 系统同步或读取一个能力点
- **THEN** 能力点的 `primaryKnowledgeNodeCode` SHALL 指向 `informatics_knowledge_nodes` 中 type 为 `KNOWLEDGE_POINT` 的节点
- **AND** 能力点 MAY 保存其他相关知识节点作为检索补充

#### Scenario: 易错点归属能力点
- **WHEN** 系统同步或读取一个易错点
- **THEN** 易错点 SHALL 保存合法 `skillUnitCode`
- **AND** AI 候选包 SHALL 将该易错点放入所属能力点下面
- **AND** 易错点的知识节点集合 SHALL NOT 替代能力点归属链路

#### Scenario: 提升点归属诊断层
- **WHEN** 系统同步或读取一个提升点
- **THEN** 提升点 SHALL 优先挂到能力点
- **AND** 若提升点面向整个知识点迁移，SHALL 至少保留主知识点路径

### Requirement: 结构化候选包必须使用中文知识点路径
AI 标准库候选包 SHALL 以知识点为第一分组，并 SHALL 在 `knowledgeGroups` 中提供知识点名称、中文路径和该知识点下的能力点、易错点、提升点。

#### Scenario: 导航包按知识点分组
- **WHEN** 系统从 AI 导航结果构造 `StandardLibraryPack`
- **THEN** `knowledgeGroups[].id` SHALL 是知识点 code
- **AND** `knowledgeGroups[].name` SHALL 优先使用知识点中文名
- **AND** `knowledgeGroups[].path` SHALL 优先使用知识树中文 path

#### Scenario: 能力点下展开易错点
- **WHEN** AI 导航结果包含同一知识点下的能力点和易错点
- **THEN** 候选包 SHALL 将能力点放在对应知识点分组下
- **AND** 候选包 SHALL 将易错点放在所属能力点下
- **AND** 同能力点下的相邻易错点 MAY 作为上下文一起提供

### Requirement: 标准库应直接补齐高频知识点覆盖密度
AI 标准库 SHALL 能通过版本化 seed 批次直接补充高频薄弱知识点下的能力点、易错点和提升点，而不是只输出覆盖报告等待人工补库。

#### Scenario: 发现高频薄弱知识点
- **WHEN** 系统维护者发现高频知识点下能力点、易错点或提升点密度不足
- **THEN** 标准库 SHALL 通过新增 seed 批次直接补充正式候选条目
- **AND** 新增条目 SHALL 按“知识点 -> 能力点 -> 易错点/提升点”组织
- **AND** 每个新增主题 SHOULD 至少包含 1 个能力点、多个易错点和 1 个提升点

#### Scenario: 新增密度扩展批次
- **WHEN** 标准库新增覆盖密度扩展 seed
- **THEN** 每条 seed SHALL 引用现有知识树节点
- **AND** 每条 seed SHALL 至少包含一个 `KNOWLEDGE_POINT` 锚点
- **AND** 测试 SHALL 校验新增条目不是模板化占位内容

#### Scenario: V16 扩展高频重合主题
- **WHEN** 标准库 seed 批次加载
- **THEN** 系统 SHALL 为链表、区间调度、前缀和、计数排序、游程编码、多数投票、素数判断和拓扑依赖补充结构化能力点、易错点和提升点
- **AND** 每个新增条目 SHALL 至少锚定一个知识点

### Requirement: 标准库必须提供 AI 可导航目录
AI 标准库规范结构 SHALL 在现有知识树和规范化诊断层之上提供可分页、可展开的导航读取能力，使 AI 能按大章节、小章节、知识点、能力点、易错点和提升点逐层选择。

#### Scenario: 读取一级目录
- **WHEN** AI 标准库导航阶段开始
- **THEN** 系统 SHALL 返回启用的一级知识目录
- **AND** 返回内容 SHALL 包含主名、别名摘要、节点类型和是否有子节点

#### Scenario: 展开知识节点
- **WHEN** AI 选择一个大章节、小章节或知识点
- **THEN** 系统 SHALL 只展开被选择节点的直接子节点或必要摘要
- **AND** 系统 SHALL NOT 一次性返回整棵标准库

#### Scenario: 展开知识点诊断层
- **WHEN** AI 选择一个 `KNOWLEDGE_POINT`
- **THEN** 系统 SHALL 返回该知识点下启用的能力点、易错点和提升点
- **AND** 易错点 SHALL 按所属能力点组织
- **AND** 提升点 SHALL 标明关联能力点或知识点

#### Scenario: 导航读取复用统一标准库
- **WHEN** AI 浏览高中和竞赛都覆盖的概念
- **THEN** 系统 SHALL 返回同一标准库节点的主名和 aliases
- **AND** 系统 SHALL NOT 创建高中库和竞赛库两套平行目录

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

### Requirement: 启用标准库关系必须指向启用规范对象
启用的标准库关系和处于 `MAPPED` 状态的兼容映射 SHALL 只指向存在且启用的知识节点、能力点、易错点或提升点。

#### Scenario: 历史兜底条目已停用
- **WHEN** 能力点或易错点因历史兜底退役而停用
- **THEN** 指向该条目的关系 SHALL 同步停用
- **AND** 指向该条目的兼容映射 SHALL 标记为归档而不是继续处于 `MAPPED`

#### Scenario: 教师批准条目归属无效
- **WHEN** 教师批准的正式易错点引用不存在的能力点或占位知识路径
- **THEN** 数据质量门禁 SHALL 阻断发布
- **AND** 修正 SHALL 保留教师批准来源并重挂到最接近的合法能力点和知识点

### Requirement: 正式分类不得泄漏扩库实现批次
启用标准库条目的用户可读分类 SHALL 表达能力、错因或提升主题，不得包含 `V13/V14/V15/V16`、`兜底吸收`、`兜底榨取-A类/B类` 等实现来源词。

#### Scenario: 保留稳定 code 清理分类
- **WHEN** 历史条目的 category 含有扩库版本或兜底来源词
- **THEN** 数据迁移 SHALL 清理 category 中的实现词并保留主题语义
- **AND** 条目 code SHALL 保持不变以兼容历史引用

### Requirement: 启用正式易错点不得保留同锚点同名重复项
启用易错点在同一主知识节点下 SHALL 使用可区分的正式名称；语义完全重合的历史项 SHALL 选择一个主条目并保留兼容映射。

#### Scenario: 检测同锚点同名错因
- **WHEN** 两个启用易错点拥有相同规范化名称和相同主知识节点
- **THEN** 学科质量门禁 SHALL 失败
- **AND** 修正 SHALL 停用重复项或将其重写为可验证的不同错误行为

### Requirement: 精确知识点诊断层必须解析所引用的启用能力点
易错点或提升点的主知识节点与其能力点主知识节点不同时，诊断层 SHALL 保留内容的精确知识归属，并通过 `skill_unit_code` 解析启用能力点后返回完整嵌套结构。

#### Scenario: 易错点比能力点使用更精确的主知识节点
- **WHEN** 启用易错点挂在精确知识点，并引用一个主节点位于相关知识点的启用能力点
- **THEN** 展开精确知识点诊断层 SHALL 返回该能力点
- **AND** 该易错点 SHALL 出现在对应能力点的易错点列表中
- **AND** 系统 SHALL NOT 为了导航可见性把易错点改挂到较粗的能力点主节点

### Requirement: 新增正式提升点必须同步规范化主表和兼容读取结构
通过数据库迁移新增正式提升点时，系统 SHALL 使用同一稳定 code 同步规范化提升点、启用平铺快照和 legacy mapping，并 SHALL 保持能力点、知识点和关联错因归属一致。

#### Scenario: 新增第二批提升点
- **WHEN** V3 为缺少提升点的正式能力点新增提升路径
- **THEN** `ai_standard_improvement_points` SHALL 保存训练目标、练习策略、学生收益、教师解释和关联错因
- **AND** `ai_standard_library_items` SHALL 保存同 code 的启用 `IMPROVEMENT_POINT` 快照
- **AND** `ai_standard_library_legacy_mappings` SHALL 保存同 code 的 `MAPPED` 映射

#### Scenario: 从精确知识点读取新增提升点
- **WHEN** 教师或 AI 展开第二批提升点的主知识节点诊断层
- **THEN** 系统 SHALL 返回其所属启用能力点和该提升点
- **AND** 导航结果 SHALL 保留提升点的主知识节点、相关知识节点和关联错因

### Requirement: 提升点必须提供可执行且可验证的迁移练习
正式提升点 SHALL 描述学生修复当前错误后如何练习、用什么边界或状态检查结果以及教师如何观察掌握情况，不得只重复能力点名称或使用抽象鼓励语。

#### Scenario: 审核第二批提升内容
- **WHEN** 第二批提升点进入正式数据库
- **THEN** 每条提升点 SHALL 关联至少一个启用易错点
- **AND** `practice_strategy` SHALL 包含可执行动作或自测样例类别
- **AND** `teacher_explanation` SHALL 包含可观察的讲解或验收方法

### Requirement: 第三批正式提升点必须补齐算法与数据结构的可验证练习
V4 新增的正式提升点 SHALL 归属于没有启用提升点的 ALGO 或 DS 正式能力，并 SHALL 将相关易错点转化为学生可执行练习和教师可观察验收。

#### Scenario: 保存第三批提升点
- **WHEN** 一个 `informatics-discipline-quality-v3` 提升点进入正式数据库
- **THEN** 它 SHALL 关联至少一个启用易错点
- **AND** `practice_strategy` SHALL 包含状态表、反例、手算轨迹、边界样例或复杂度预算中的至少一种可执行验证
- **AND** `teacher_explanation` SHALL 说明教师如何观察学生是否真正掌握

### Requirement: 第三批提升点必须保持规范表与兼容结构一致
每个 V4 提升点 SHALL 使用同一稳定 code 同步到规范化提升点、启用平铺快照和 `MAPPED` legacy mapping，并 SHALL 保持能力点、主知识点、相关知识点和关联错因归属一致。

#### Scenario: 三处归属不一致
- **WHEN** V4 提升点在任一兼容结构中缺失、停用或指向不同能力点和主知识点
- **THEN** 数据质量门禁 SHALL 失败
- **AND** 应用 SHALL NOT 带着部分同步内容发布

### Requirement: 第三批代表性导航必须返回完整算法与数据结构闭环
系统 SHALL 对至少一个 ALGO 和一个 DS 第三批主知识点验证诊断层导航，返回所属启用能力点、关联易错点和新增提升点。

#### Scenario: 展开算法知识点
- **WHEN** 教师或 AI 展开第三批 Dijkstra、DP、二分、区间或图算法代表性知识点
- **THEN** 响应 SHALL 包含对应正式能力和 `informatics-discipline-quality-v3` 提升点
- **AND** 提升点 SHALL 保留其关联启用易错点

#### Scenario: 展开数据结构知识点
- **WHEN** 教师或 AI 展开第三批链表、队列、图建模或映射代表性知识点
- **THEN** 响应 SHALL 包含对应正式能力和 `informatics-discipline-quality-v3` 提升点
- **AND** 规范化主结构 SHALL 是返回内容的判断来源

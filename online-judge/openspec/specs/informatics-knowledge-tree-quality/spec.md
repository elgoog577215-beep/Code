# informatics-knowledge-tree-quality Specification

## Purpose
TBD - created by archiving change expand-knowledge-tree-standard-library-v8. Update Purpose after archive.
## Requirements
### Requirement: 知识树扩展必须细化既有教学路径
信息学知识树扩展 SHALL 复用现有领域、章节和 topic，并在其下补充更具体的小知识点，避免创建与既有路径重复或并行的新知识树。

#### Scenario: 新增细知识点
- **WHEN** 系统同步新增知识树 seed
- **THEN** 新知识点 SHALL 拥有合法父 topic
- **AND** 新知识点 SHALL 保留完整 path、description、learningObjectives 和 typicalProblems

#### Scenario: 不重复创建上层结构
- **WHEN** 某个新增内容可以归入既有 topic
- **THEN** 系统 SHALL NOT 为它新增重复领域、章节或 topic

### Requirement: 知识树扩展必须优先补强诊断薄弱主题
知识树扩展 SHALL 优先覆盖 AI 诊断高价值且手写标准库薄弱的主题，包括模拟边界、数组更新、字符串匹配、图存储、双指针、复杂度权衡、运行错误和提交检查。

#### Scenario: 薄弱主题获得更细节点
- **WHEN** 本轮 V8 知识树 seed 被加载
- **THEN** 至少 SHALL 为模拟、数组、字符串、图结构、复杂度或提交检查中的多个主题新增细知识点
- **AND** 新节点 SHALL 能被 V8 标准库条目引用

### Requirement: 知识树必须优先使用高中术语作为统一入口
信息学知识树 SHALL 在同一概念存在高中、通用和竞赛多种叫法时优先使用高中常用术语作为主名，并 SHALL 将通用、竞赛、英文或教师习惯叫法保存为别名或相关说明。

#### Scenario: 高中主名保留竞赛别名
- **WHEN** 系统同步一个高中和竞赛重合的知识节点
- **THEN** 该节点主名 SHALL 优先采用高中常用叫法
- **AND** 该节点 aliases SHALL 包含至少一个通用、竞赛或英文副名
- **AND** 系统 SHALL NOT 为同一概念新增高中库和竞赛库两套平行节点

#### Scenario: 高中术语缺失时使用通用术语
- **WHEN** 某个信息学概念没有稳定高中叫法
- **THEN** 该节点 SHALL 使用通用或竞赛术语作为主名
- **AND** 后续若补入高中叫法 SHALL 通过别名或主名调整接入同一节点

### Requirement: 高中真题核心算法入口必须挂入既有信息学知识树
浙江高中技术选考真题中的核心算法术语 SHALL 复用既有信息学领域、章节和 topic；只有既有 topic 无法表达时，才在同一章节下新增 topic，不得新增平行的高中专用上层分类。

#### Scenario: 真题术语映射到既有路径
- **WHEN** 系统同步高中真题核心算法节点
- **THEN** 数组、排序、双指针、递归、数据结构、区间和编码类内容 SHALL 挂入现有 BASIC、DS、ALGO 或 MATH 路径
- **AND** 高中叫法 SHALL 能作为主名或 aliases 被读取

#### Scenario: 缺口术语成为可检索入口
- **WHEN** 教师按“冒泡排序”“选择排序”“擂台法”“游程编码”“多数投票”“状态标记”“计数排序”“链表”或“区间调度”等高中术语查找
- **THEN** 知识树 SHALL 存在对应主节点、子节点或别名承接该术语

### Requirement: 知识树末端必须是知识点
信息学知识树 SHALL 只表达学科地图，树形层级 SHALL 收束到 `KNOWLEDGE_POINT`；能力点、易错点和提升点 SHALL NOT 作为知识树节点混入领域、章节或 topic 层级。

#### Scenario: 知识树同步保持末端语义
- **WHEN** 系统同步信息学知识树 seed
- **THEN** 领域、章节和 topic SHALL 继续作为知识地图上层结构
- **AND** 可诊断的最细学习对象 SHALL 使用 `KNOWLEDGE_POINT`
- **AND** 能力点、易错点和提升点 SHALL 通过标准库诊断层挂在知识点下

#### Scenario: 高中术语进入知识点而不是新库
- **WHEN** 系统吸收高中真题术语
- **THEN** 该术语 SHALL 复用现有知识树路径并落到 topic 或 knowledge point
- **AND** 系统 SHALL NOT 为能力点或易错点额外创建高中专用知识树分支

### Requirement: 知识树正式内容必须由数据库维护
信息学知识树正式节点 SHALL 由数据库中的 `informatics_knowledge_nodes` 维护；运行时 SHALL NOT 通过 `InformaticsKnowledgeSeedCatalog` 或启动播种器扩建正式知识树。

#### Scenario: 读取知识树
- **WHEN** 前端、AI 导航或教师端读取知识树
- **THEN** 系统 SHALL 从数据库读取启用节点
- **AND** 系统 SHALL NOT 在读取前从代码 seed 自动补齐节点

#### Scenario: 新增知识节点
- **WHEN** 需要新增大章节、小章节、知识点或小知识点
- **THEN** 系统 SHALL 通过管理 API、迁移脚本或治理流程写入 `informatics_knowledge_nodes`
- **AND** 系统 SHALL NOT 要求新增 `InformaticsKnowledgeSeedCatalog` 内容

### Requirement: 知识树迁移必须保留路径和别名
历史 seed 中需要保留的知识树节点 SHALL 在一次性迁移中写入数据库，并保留路径、父子关系、阶段、难度、别名和学习目标。

#### Scenario: 迁移节点
- **WHEN** 执行知识树历史 seed 迁移
- **THEN** 系统 SHALL 使用节点 code 幂等 upsert
- **AND** SHALL 保留 parentCode、type、name、path、stage、difficulty、aliases、prerequisites、learningObjectives 和 typicalProblems

#### Scenario: 验证导航路径
- **WHEN** 迁移完成后
- **THEN** 系统 SHALL 验证代表性知识点可以沿父子路径导航
- **AND** 标准库能力点的 `primaryKnowledgeNodeCode` SHALL 指向存在的数据库知识点

### Requirement: 父子归属不得冒充真实前置知识
知识树的 `parentCode` SHALL 只表达目录归属，`prerequisites` SHALL 只保存学习该节点前真正需要掌握的其他知识节点 code。

#### Scenario: 历史父节点伪前置被清理
- **WHEN** 历史主题或知识点的 `prerequisites` 仅等于自身 `parentCode`
- **THEN** 数据迁移 SHALL 清空该伪前置关系
- **AND** 导航路径 SHALL 继续由 `parentCode` 和 `path` 保持完整

#### Scenario: 人工精修知识点保存真实前置
- **WHEN** 首批知识点存在明确的学习先决条件
- **THEN** `prerequisites` SHALL 只引用已存在的知识节点 code
- **AND** 没有可靠先决条件时 SHALL 留空而不是复制父节点 code

### Requirement: 学科精修必须替换占位描述而非改写模板
人工精修知识点 SHALL 用概念对象、适用边界、状态或验证方法表达教学语义，并 SHALL 同步维护学习目标、典型问题和有效别名。

#### Scenario: 首批高引用知识点完成精修
- **WHEN** V2 数据质量迁移执行完成
- **THEN** 至少 20 个跨 BASIC、DS、ALGO、MATH、ENG 或 CONTEST 的高引用知识点 SHALL 不再使用“细颗粒知识点”模板描述
- **AND** 每个精修节点 SHALL 包含可观察学习目标和具体典型任务

#### Scenario: 别名只保存不同叫法
- **WHEN** 知识节点 aliases 仅重复主名
- **THEN** 数据迁移 SHALL 清空该自指别名
- **AND** 首批精修节点的 aliases SHALL 只保存高中、通用、竞赛或英文的不同叫法

### Requirement: 后续知识点精修批次必须由生产使用证据和领域缺口驱动
知识树内容精修 SHALL 优先选择已经被启用标准库条目引用、仍使用已知模板描述且位于薄弱领域或课堂高频路径的知识点，并 SHALL 保存独立批次版本以便复核质量增量。

#### Scenario: 选择第二批精修节点
- **WHEN** 系统从 MATH、ENG、BASIC 选择第二批知识点
- **THEN** 候选 SHALL 是启用的 `KNOWLEDGE_POINT` 且仍使用模板描述
- **AND** 排序 SHALL 使用规范化能力点、易错点和提升点的不同条目引用数
- **AND** 人工审校 SHALL 优先最低覆盖领域与课堂高频入口，而不是按领域平均凑数

#### Scenario: 保存第二批人工精修内容
- **WHEN** 一个知识点进入第二批正式精修
- **THEN** 系统 SHALL 更新概念边界、可观察学习目标、具体典型任务、有效别名和真实前置知识
- **AND** `library_version` SHALL 标记为 `informatics-knowledge-discipline-v2`
- **AND** 系统 SHALL NOT 只对原模板做同义改写

### Requirement: 第三批算法与数据结构精修必须形成连贯学科分支
第三批知识树内容精修 SHALL 在不新增节点和不改变稳定 code 的前提下，围绕算法与数据结构的高引用路径形成连贯分支，而不是只重写彼此无关的零散节点。

#### Scenario: 保存第三批知识点
- **WHEN** V4 执行第三批知识点精修
- **THEN** 系统 SHALL 精修 20 个 ALGO 和 10 个 DS 启用模板知识点
- **AND** 内容 SHALL 覆盖最短路、拓扑、区间与贪心、二分、DP、滑动窗口、链表、图建模、队列或映射中的多个连贯路径
- **AND** `library_version` SHALL 标记为 `informatics-knowledge-discipline-v3`

#### Scenario: 精修内容只改写占位模板
- **WHEN** 一个第三批节点的新描述仍不能说明概念对象、状态不变量、适用边界或验证方法
- **THEN** 该节点 SHALL NOT 计入第三批完成数量
- **AND** 质量审校 SHALL 要求补齐可观察学习目标和具体典型任务

### Requirement: 第三批知识点前置与别名必须保持学科有效性
第三批精修节点的 `prerequisites` SHALL 只表示真实学习依赖，`aliases` SHALL 只保存不同叫法、英文术语或课堂常用表达。

#### Scenario: 使用父目录或主名作为内容字段
- **WHEN** 第三批节点把 `parent_code` 机械复制为前置，或把主名原样复制为唯一别名
- **THEN** 学科质量门禁 SHALL 失败
- **AND** 无可靠前置或别名时相应字段 SHALL 留空

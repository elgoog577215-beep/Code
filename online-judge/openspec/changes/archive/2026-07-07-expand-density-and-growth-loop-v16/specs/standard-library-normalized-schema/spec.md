## MODIFIED Requirements

### Requirement: 标准库应直接补齐高频知识点覆盖密度

AI 标准库 SHALL 使用统一信息学知识树作为主学科地图，知识树 SHALL 收束到 `KNOWLEDGE_POINT`；能力点、易错点和提升点 SHALL 作为知识点下的诊断层组织。

#### Scenario: V16 扩展高频重合主题
- **WHEN** 标准库 seed 批次加载
- **THEN** 系统 SHALL 为链表、区间调度、前缀和、计数排序、游程编码、多数投票、素数判断和拓扑依赖补充结构化能力点、易错点和提升点
- **AND** 每个新增条目 SHALL 至少锚定一个知识点

### Requirement: 成长候选必须支持审核状态流转

AI 诊断发现 `PARTIAL`、`MISS` 或 `OUT_OF_LIBRARY` 标准库缺口时，系统 SHALL 创建或聚合标准库成长候选；正式库写入 SHALL 经过教师审核或显式开启的自动合并门禁。

#### Scenario: 审核入库后进入召回
- **WHEN** 教师批准成长候选并写入正式标准库
- **THEN** 新正式条目 SHALL 在后续标准库召回候选中可见
- **AND** 系统 SHALL 保留候选中的错误表现、典型代码特征、学生解释和证据引用
- **AND** 如候选提供相似正式条目，系统 SHOULD 继承其能力点和知识点锚点

## Context

当前 AI 主链路已经是 `自由诊断 issues[] -> 后端逐层挂接标准库 -> advice generation`。后端会为每个 issue 保存 breadcrumb，但 advice 只保留标准库 ID；当导航返回 `NO_MATCH`、`PARTIAL` 或 `ATTACHMENT_FAILED` 且 advice 没有合法 ID 时，`StudentAiFeedbackService` 会再用标题关键词猜测知识节点，最后退化为单段标题路径。成长候选则完全依赖模型在 advice 阶段主动返回 `libraryGrowth.candidates`，因此逐层导航已经走过父路径也不保证形成候选。

学生端当前把逐条建议渲染为左边框列表，知识路径使用约 `0.73rem` 的行内灰字，代码证据虽然有边框，但两者没有形成“建议正文—知识归属—证据”的视觉父子关系。

本设计必须继续遵守：统一知识树、规范结构为“知识点 -> 能力点 -> 易错点/提升点”、外部模型唯一生产学生可见诊断内容、后端只约束结构与元数据、失败不伪造学生建议，以及正式数据库而非 seed 是内容主库。

## Goals / Non-Goals

**Goals:**

- 让每条修正和提升建议成为独立、可扫描的主卡片，并把知识路径、路径状态和代码证据放入清晰子卡片。
- 让未精确命中的 issue 继承后端已经确认的最后 breadcrumb，而不是接受模型自由填写父路径。
- 让模型生成的 issue/advice 标题成为有类型的临时子节点，立即用于当前反馈完整路径，并参与后续低优先级导航。
- 复用成长候选表聚合证据和出现次数，在明确门槛下自动晋升到规范化正式标准库。
- 保留教师审核、合并、拒绝、停用和回滚能力。

**Non-Goals:**

- 不创建第二棵知识树。
- 不允许后端生成新的学生诊断正文。
- 不把每个库外表达都立即视为正式知识点；新节点必须区分能力点、易错点和提升点。
- 不在本变更中重做整个教师标准库管理页。

## Decisions

### 1. 后端 breadcrumb 是父路径唯一真源

`IssueLibraryAnchor` 增加临时节点元数据。只有 breadcrumb 至少包含一个已存在知识节点时，系统才允许创建临时候选。父节点 code 取 breadcrumb 最末节点，建议路径由 `breadcrumb.names + proposedName` 组成；模型返回的 `suggestedPath` 仅能提供名称参考，不能覆盖父 code。

选择该方案而不是信任模型完整路径，是为了防止模型编造章节、把易错点挂到根节点或在同义词变化时产生不同父路径。

### 2. advice 使用显式 issue 关联，缺失时才按位置恢复

advice schema 增加可选 `issueId`。后端优先用 `issueId` 找到对应 anchor；真实模型省略字段时，normalizer/路径补全可按 advice 顺序与 issue 顺序恢复，并记录 soft fix。提高建议可通过 `issueId` 关联基础 issue；无法关联时使用本题主 anchor，但不得伪造正式命中。

### 3. 临时节点沿用成长候选表并增加稳定父 code

成长候选增加 `parentKnowledgeNodeCode`。候选 `layer` 使用现有 `SKILL_UNIT`、`MISTAKE_POINT`、`IMPROVEMENT_POINT`；基础修正默认是 `MISTAKE_POINT`，提升建议默认是 `IMPROVEMENT_POINT`，模型提供合法 layer 时可覆盖默认值。

候选状态继续使用现有 `PROPOSED/NEEDS_REVIEW/MERGED_SIMILAR/...`，学生端统一映射为 `PROVISIONAL`，不暴露内部治理状态。

选择复用候选表而不是新增临时知识树表，是为了保持统一知识树和现有审核、聚合、回滚能力。

### 4. 临时节点以低优先级加入诊断层

`AiStandardLibraryDiagnosticLayerResponse` 增加 `provisionalCandidates`。模型在逐层挂接到知识点后先检查正式能力点、易错点和提升点；只有正式项都不匹配时才允许选择临时候选。选中临时候选时 anchor 状态为 `PROVISIONAL`，当前路径仍由正式 breadcrumb 加临时节点组成。

临时节点不进入根目录和章节目录，避免扩大每轮导航调用与搜索噪声。

### 5. 自动晋升需要多重门禁

自动晋升同时要求：成长 Agent 开启、自动晋升开启、父知识节点真实存在、候选证据状态为 `SUPPORTED`、置信度达到配置阈值、同一候选出现次数达到配置阈值、正式库不存在同 layer/code。默认最小出现次数为 2。

未达到门禁的候选仍能作为低优先级临时节点参与导航。该方案在“马上进入标准库闭环”和“避免一次模型措辞污染正式库”之间保留可逆边界。

### 6. 当前反馈直接携带显式知识路径契约

`SubmissionAnalysisResponse` 的逐条 advice 和 `StudentAiFeedbackResponse.FeedbackItem` 增加 `knowledgePath`、`knowledgePathStatus` 与 `provisionalNodeCode`。映射优先级为：

1. advice 已由完整链路回填的显式路径；
2. 合法正式标准库 ID 反查路径；
3. 旧记录兼容所需的文本推断。

新生成记录不得只用标题冒充完整路径。没有正式父路径时返回 `UNCLASSIFIED`，前端显示“暂未归入知识树”，而不是伪造一段路径。

### 7. 学生结果采用“学习诊断卡”视觉方向

视觉方向采用克制的课堂讲义/学习档案风格：结果弹窗仍保持评测、修正、提升三列；修正和提升列内部每条建议使用完整卡片。知识路径子卡使用分段 breadcrumb，最后一级强调；临时节点使用琥珀色虚线和“AI 新发现”标签；代码证据使用青绿色可点击子卡。

概览与逐条建议分层，避免重复正文。桌面端维持三列，平板和移动端按评测、修正、提升顺序纵向排列；所有可点击证据满足键盘和触控尺寸要求。

## Risks / Trade-offs

- [模型省略 `issueId`] → 保留顺序恢复并增加测试，不能因单个可恢复字段让整题失败。
- [同义表达产生重复候选] → 规范化 code、同父级去重、累计 aliases/证据和 occurrenceCount，正式晋升前再次查重。
- [临时候选增加导航噪声] → 只在知识点诊断层提供、标记低优先级，并限制每个父节点返回数量。
- [错误候选被自动晋升] → 要求直接证据、置信度、重复次数和真实父节点四重门禁，保留停用与回滚记录。
- [旧记录没有显式路径状态] → API 读取时兼容推断，但只给 `INFERRED`，不伪装为正式命中。
- [卡片增加页面高度] → 列内滚动、紧凑标题和可读留白并用，移动端改为单列自然滚动。

## Migration Plan

1. 通过 JPA schema update 增加成长候选父知识节点字段；正式 Postgres 投用前由数据库备份与 readiness 检查保障回滚。
2. 后端先兼容新旧 JSON 字段，旧记录继续可读。
3. 上线临时候选生成和当前反馈路径回填，再开放临时候选参与后续导航。
4. 自动晋升配置在验证候选聚合测试后开启；关闭开关即可立即停止新的正式写入。
5. 回滚时停用自动晋升并保留候选审计记录；已晋升条目通过现有标准库停用能力回滚。

## Open Questions

- 首版不自动创建新的知识树 `KNOWLEDGE_POINT`；当最后 breadcrumb 尚未到知识点时，候选保持待审核。后续可单独设计知识树节点成长，而不把易错点误建成知识点。

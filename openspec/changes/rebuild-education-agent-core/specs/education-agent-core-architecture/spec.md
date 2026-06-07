## ADDED Requirements

### Requirement: 系统必须以教育 agent 四层架构组织主链路

系统 SHALL 将主链路组织为判题事实层、学生 AI 快反馈层、学习轨迹层和教师洞察层，而不是继续由一个大而全的 analysis 对象主导学生和教师体验。

#### Scenario: 规划定义四层职责

- **WHEN** `rebuild-education-agent-core` 规划被审阅
- **THEN** 它 SHALL 明确判题事实层只负责 verdict、测试点、错误、首个失败点和代码证据
- **AND** 它 SHALL 明确学生 AI 快反馈层只负责修正建议、提升建议、追问和 AI 状态
- **AND** 它 SHALL 明确学习轨迹层负责多次提交的进步、回退、复发和 AI 提示后的变化
- **AND** 它 SHALL 明确教师洞察层负责需关注学生、高频问题、共性误区、教师干预和 AI 质量证据

#### Scenario: 旧代码只作为可复用资产

- **WHEN** 后续 implementation 选择复用旧代码
- **THEN** 复用理由 SHALL 说明该代码服务四层架构中的哪一层
- **AND** 职责混乱的大对象、从大报告抽前端信息、本地规则冒充 AI、工程状态堆叠 SHALL NOT 继续作为主链路设计依据

### Requirement: 判题事实层不得生成学生端 AI 建议

系统 SHALL 允许本地规则整理判题事实、候选信号和证据引用，但 SHALL NOT 用本地规则生成学生端可见的修正建议或提升建议。

#### Scenario: 本地规则参与证据整理

- **GIVEN** 学生提交失败
- **WHEN** 系统构造判题事实和候选信号
- **THEN** 本地规则 MAY 生成 evidenceRefs、candidateSignals、首个失败点摘要和安全边界
- **AND** 本地规则 SHALL NOT 写入学生端 `repairItems` 或 `improvementItems` 的可见文案

#### Scenario: 模型不可用时学生建议为空或失败状态

- **GIVEN** 外接模型超时、不可用、解析失败或安全拒绝
- **WHEN** 学生端查询 AI 快反馈
- **THEN** 系统 SHALL 返回明确状态，例如 `TIMEOUT`、`FAILED` 或 `SAFETY_REJECTED`
- **AND** 系统 SHALL NOT 用本地规则建议冒充模型建议

### Requirement: 学生 AI 快反馈必须直接对齐学生结果弹窗

系统 SHALL 提供学生 AI 快反馈契约，直接服务学生结果弹窗的修正建议、提升建议和追问，而不是从完整教师诊断报告里抽取。

#### Scenario: 失败提交获得结构化学生反馈

- **GIVEN** 学生提交失败
- **AND** 外接模型返回通过安全校验的结构化反馈
- **WHEN** 前端渲染结果弹窗
- **THEN** 弹窗 SHALL 展示评测事实
- **AND** 修正建议 SHALL 最多突出一个首要处理项
- **AND** 提升建议 SHALL 最多展示两个进阶项
- **AND** 追问 SHALL 紧贴学生反馈底部

#### Scenario: 学生反馈必须短、可执行、有证据

- **WHEN** 模型输出学生 AI 快反馈
- **THEN** 每条修正建议 SHALL 引用至少一个判题、代码、题目或历史证据引用
- **AND** 每条建议 SHALL 是学生可执行的观察、对比、追踪、估算或构造样例动作
- **AND** 每条建议 SHALL NOT 包含完整代码、完整答案、隐藏测试数据或直接替换结构

### Requirement: 验收体系必须区分布尔门禁、质量 rubric 和观测指标

系统 SHALL 不把教育目标粗暴 bool 化；规划和后续 implementation SHALL 使用布尔门禁、质量 rubric 和观测指标共同判断是否达成目标。

#### Scenario: 布尔门禁用于底线判断

- **WHEN** 验收结构、安全、状态和责任边界
- **THEN** 系统 SHALL 使用 true/false 门禁
- **AND** 门禁 SHALL 覆盖本地规则不得冒充 AI、学生建议必须来自模型、AI 超时有明确状态、高风险泄露被拦截、教师主界面不展示工程术语

#### Scenario: 质量问题使用 rubric

- **WHEN** 验收学生修正建议、提升建议、追问、教师洞察或学习轨迹解释质量
- **THEN** 系统 SHALL 使用分级 rubric
- **AND** rubric SHALL 至少区分空泛不可用、有方向但不可执行、可执行且有证据、可执行且能引导学生自证理解
- **AND** 单纯字段存在 SHALL NOT 证明教育质量达标

#### Scenario: 长期效果使用观测指标

- **WHEN** 评估真实课堂效果
- **THEN** 系统 SHALL 记录 AI 快反馈耗时、结构化成功率、超时率、安全拦截率、反馈后再次提交比例、失败到通过提交次数、教师校正率和同类错因复发率
- **AND** 初期 MAY 不设置硬阈值
- **AND** 未设置阈值的指标 SHALL 在报告中说明仍处于观察期

### Requirement: 学习轨迹必须描述学生过程而不是单次结论

系统 SHALL 将学生多次提交转换为学习轨迹信号，用于学生推荐和教师洞察。

#### Scenario: 同题多次提交形成轨迹

- **GIVEN** 同一学生对同一题产生多次提交
- **WHEN** 系统分析学习轨迹
- **THEN** 它 SHALL 能识别 RE 到 WA、WA 到 AC、AC 后失败、连续失败和同题改善
- **AND** 轨迹信号 SHALL 包含可引用的提交证据

#### Scenario: AI 反馈后行为进入轨迹

- **GIVEN** 学生查看或收到 AI 快反馈
- **WHEN** 学生之后继续提交或停止提交
- **THEN** 系统 SHALL 记录 AI 反馈后的后续行为
- **AND** 该信号 MAY 用于判断提示是否帮助推进、是否仍卡住、是否需要教师介入

### Requirement: 教师洞察必须服务课堂判断

系统 SHALL 让教师端优先展示课堂判断所需信息，而不是展示 AI 工程状态。

#### Scenario: 教师首页主信息

- **WHEN** 教师打开工作台
- **THEN** 首屏 SHALL 优先展示作业选择、课堂 KPI、需关注学生、高频问题和共性误区
- **AND** AI 质量 SHALL 作为辅助证据展示
- **AND** 工程状态 SHALL 不压过课堂过程

#### Scenario: 工程术语不得进入教师主界面

- **WHEN** 教师端展示 AI 状态
- **THEN** 主界面 SHALL NOT 直接展示 `BLOCKED`、`RECOVERED`、`NOT_COMPARABLE`、`smoke`、`profile`、`fallback` 等工程术语
- **AND** 必须展示时 SHALL 翻译为教师可理解语言并放入系统详情或折叠证据区

### Requirement: 迁移必须保护现有判题和提交能力

系统 SHALL 在重建主链路时保护现有判题、提交、题目、作业和学生身份能力，避免为了新架构破坏基本课堂使用。

#### Scenario: 新链路与旧判题并存

- **WHEN** P1 学生 AI 快反馈链路实施中
- **THEN** 基本提交、判题、测试点记录和题目读取 SHALL 继续工作
- **AND** 新 AI 快反馈失败 SHALL NOT 影响 verdict 和测试点返回

#### Scenario: 旧教师页兼容直到新洞察通过验收

- **WHEN** P3 教师洞察重建尚未完成
- **THEN** 系统 MAY 保留旧教师页兼容入口
- **AND** 新教师主视图通过验收前 SHALL NOT 删除教师完成课堂管理所需的旧功能

### Requirement: 规划阶段不得修改业务源码

本变更初始阶段 SHALL 只产出 OpenSpec 规划文档，不修改 `online-judge` 业务源码。

#### Scenario: 规划完成

- **WHEN** `proposal.md`、`design.md`、`tasks.md` 和本规格文件创建完成
- **THEN** 工作区变更 SHALL 只包含 `openspec/changes/rebuild-education-agent-core` 下的规划文件
- **AND** 后续业务实现 SHALL 另按 tasks 分阶段执行

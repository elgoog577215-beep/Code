# ai-prompt-context-quality Specification

## Purpose
保证正式发给外部模型的提示词和上下文能服务高中信息学诊断：既给模型足够方向和标准化约束，又保留模型对代码、题目和判题结果的整体判断能力。
## Requirements
### Requirement: 标准库不得制约模型判断
系统 SHALL 在正式诊断提示词中说明标准库候选是参考和命名约束，而不是唯一答案来源。

#### Scenario: 候选不匹配
- **WHEN** 标准库候选不能解释当前代码问题
- **THEN** 模型 SHALL 被允许返回 `OUT_OF_LIBRARY`
- **AND** 模型 SHALL 简要说明为什么候选不匹配

### Requirement: 学生可见反馈使用自然教学文风
系统 SHALL 要求模型生成学生可读的自然反馈，而不是把结构化字段机械拼接成学生文案。

#### Scenario: 生成学生报告
- **WHEN** 模型生成 `studentReport`
- **THEN** 基础层文本 SHALL 先用通俗语言说明问题，再引用证据，再给行动建议
- **AND** 提高层文本 SHALL 聚焦修复后的算法、复杂度、建模或测试提升
- **AND** 下一步行动 SHALL 是学生马上能做的小动作

### Requirement: 基础层优先
系统 SHALL 要求模型在提交未通过时优先处理基础层问题。

#### Scenario: 未 AC 提交
- **WHEN** 判题结果不是 AC
- **THEN** 基础层反馈 SHALL 解释当前最可能阻塞通过的问题
- **AND** 提高层反馈 SHALL 不抢基础层主次

### Requirement: 保留安全边界
系统 SHALL 在正式诊断提示词中保留学生端安全边界。

#### Scenario: 避免直接给答案
- **WHEN** 模型生成学生可见反馈
- **THEN** 输出 MUST NOT 包含完整代码、完整答案、隐藏测试推测或可复制的逐行改法

### Requirement: 诊断报告提示词应分离学生报告与后端元数据
系统 SHALL 在正式诊断提示词中要求模型先生成学生自然语言报告，再提供后端审计和标准库 metadata；学生可见报告 SHALL NOT 机械堆叠内部结构字段。

#### Scenario: 生成正式诊断报告
- **WHEN** 模型使用 `diagnosis-report-v2` 生成诊断结果
- **THEN** prompt SHALL 明确 `studentReport` 是学生优先阅读的自然反馈
- **AND** prompt SHALL 明确 `diagnosisDecision`、`diagnosisCandidates`、`teacherTrace` 和 `libraryGrowth` 是后端审计与标准库成长信息

#### Scenario: 多条建议存在
- **WHEN** 当前提交存在多个独立错因或提升方向
- **THEN** prompt SHALL 要求逐条列表由证据决定数量
- **AND** prompt SHALL NOT 要求固定一条或把多条问题硬塞进单个字符串

#### Scenario: 标准库包含主路径和相关路径
- **WHEN** `standardLibrary.knowledgeGroups` 或兼容列表包含 `primaryKnowledgeNodeCode` 与 `relatedKnowledgeNodeCodes`
- **THEN** prompt SHALL 要求模型优先使用主路径理解知识父子关系
- **AND** prompt SHALL 要求模型把相关路径视为辅助上下文，而不是独立错因或独立学生标签

### Requirement: 提示词必须要求独立诊断并同步标注
系统 SHALL 在 `free-diagnosis-v1` 提示词中要求模型先依据题目、完整代码和运行结果判断真实错误；系统 SHALL 在 `diagnosis-report-v3` 提示词中要求模型基于初步诊断和 AI 导航结果完成标准库路径标注。

#### Scenario: 生成初步诊断提示词
- **WHEN** 系统构造 `free-diagnosis-v1` prompt
- **THEN** prompt SHALL NOT 包含标准库候选包
- **AND** prompt SHALL 要求模型输出真实错误原因假设、关键代码位置、行为差距和导航意图

#### Scenario: 生成最终诊断提示词
- **WHEN** 系统构造 `diagnosis-report-v3` prompt
- **THEN** prompt SHALL 明确要求模型保持初步诊断中由证据支持的真实问题
- **AND** prompt SHALL 明确要求模型使用 AI 导航确认的标准库路径统一术语和颗粒度
- **AND** prompt SHALL 要求模型输出命中状态和待审核库外候选

### Requirement: 正式诊断上下文包必须保留完整代码和结构邻域
系统 SHALL 在最终诊断上下文包中稳定提供题目完整信息、学生完整代码、判题参考信号、初步诊断草稿和 AI 标准库导航确认的结构邻域。

#### Scenario: 构造最终诊断上下文
- **WHEN** 系统构造 `diagnosis-report-v3` 上下文
- **THEN** 上下文 SHALL 包含完整题目描述和完整学生代码
- **AND** 上下文 SHALL 包含判题结果或样例差异作为参考信号
- **AND** 上下文 SHALL 包含初步诊断草稿
- **AND** 上下文 SHALL 包含 AI 标准库导航确认的知识路径、能力点、易错点和提升点邻域

#### Scenario: 标准库上下文不是全库倾倒
- **WHEN** 标准库条目数量超过单次模型上下文预算
- **THEN** 系统 SHALL 让 AI 通过导航阶段逐层选择相关路径
- **AND** 系统 SHALL NOT 要求最终诊断模型从无关全库条目中筛选诊断答案

### Requirement: 提示词必须表达统一树和诊断层关系
正式 AI 诊断 prompt 和标准库导航 prompt SHALL 明确知识树只到知识点，能力点、易错点和提升点属于知识点以下的诊断层，并 SHALL 要求模型不要把知识树和标准库解释成两套平行数据库。

#### Scenario: 生成标准库导航 prompt
- **WHEN** 系统构造 `standard-library-navigation-v1` prompt
- **THEN** prompt SHALL 明确主链路是“知识点 -> 能力点 -> 易错点/提升点”
- **AND** prompt SHALL 明确知识点回答“学什么”，能力点回答“会做什么”，易错点回答“常错在哪里”
- **AND** prompt SHALL 要求 AI 优先使用标准库主名，高中术语和通用术语冲突时以主名为准

# single-agent-ai-diagnosis Specification

## Purpose
TBD - created by archiving change simplify-ai-diagnosis-single-agent. Update Purpose after archive.
## Requirements
### Requirement: 实时诊断使用单诊断 Agent
系统 SHALL 在学生可见内容生产上只保留一个最终诊断 Agent；前置初步诊断和 AI 标准库导航只生产中间结构、导航路径和标准库锚点，不直接生成学生可见报告。

#### Scenario: 默认实时诊断使用三阶段编排
- **WHEN** 学生提交代码且外部 AI 可用
- **THEN** 系统 MAY 调用初步诊断、AI 标准库导航和最终诊断三个阶段
- **AND** 只有最终诊断阶段 SHALL 生成 `studentReport`

#### Scenario: 默认配置使用 AI 导航
- **WHEN** 系统执行默认实时诊断
- **THEN** 系统 SHALL 先执行初步诊断和 AI 标准库导航
- **AND** trace SHALL 将标准库定位状态标记为 `AI_NAVIGATION`

#### Scenario: 前置阶段不污染学生报告
- **WHEN** 初步诊断或标准库导航产生中间判断
- **THEN** 学生端 SHALL NOT 直接展示这些中间判断
- **AND** 中间判断 SHALL 只用于最终诊断、教师 trace、评测和待审核成长候选

### Requirement: 标准库作为教学坐标而不是强制答案
系统 SHALL 允许最终诊断 Agent 对 AI 导航选中的标准库路径标记命中、部分命中、未命中或库外发现。

#### Scenario: 导航路径不匹配真实问题
- **WHEN** AI 导航选中的标准库路径不能解释学生错误
- **THEN** 模型输出 SHALL 能标记未命中并给出库外发现，而不是强行绑定错误标准库条目

### Requirement: 学生反馈以自然报告为主
系统 SHALL 将学生端主反馈组织为基础层、提高层和下一步行动，并保留后端可校验元数据。学生可见报告 MUST 使用自然、连贯、适合高中学生阅读的中文表达；结构化字段 SHALL 服务校验、画像和评测，不得污染学生端主表达。

#### Scenario: 学生查看 AI 反馈
- **WHEN** 单诊断 Agent 输出有效报告
- **THEN** 学生端 SHALL 优先展示 `studentReport` 的自然语言报告，而不是碎字段拼接文本

#### Scenario: 结构化字段服务机器消费
- **WHEN** 单诊断 Agent 输出 `studentReport` 和元数据
- **THEN** 学生端 SHALL 以 `studentReport` 作为主展示
- **AND** `libraryFit`、标签、证据、泄题风险等元数据 SHALL 用于校验、画像、推荐、教师追踪和评测

#### Scenario: 学生反馈长度受控
- **WHEN** 单诊断 Agent 生成学生可见反馈
- **THEN** 系统 SHALL 要求基础层、提高层和下一步行动保持短句、明确证据和可执行动作，且不得生成长篇完整教程

#### Scenario: 基础层优先
- **WHEN** 提交未通过且存在基础层阻塞问题
- **THEN** 学生反馈 SHALL 先说明基础层问题，再给出次要提高层建议

#### Scenario: 下一步行动不是直接改法
- **WHEN** 单诊断 Agent 生成 `studentReport.nextActionText`
- **THEN** 下一步行动 SHALL 优先要求学生手推、复述状态、比较输出、估算复杂度或构造小样例
- **AND** 下一步行动 MUST NOT 以可复制代码修改步骤作为主表达

### Requirement: 单诊断 Agent 必须接收知识点诊断层候选包
最终诊断 Agent 的标准库上下文 SHALL 来自 AI 导航确认后的知识点诊断层结构，且 SHALL 明确该结构是一棵统一知识树下的诊断层，而不是知识树和标准库两套平行库。

#### Scenario: 最终诊断上下文包含导航确认路径
- **WHEN** 系统准备最终诊断 Agent 输入
- **THEN** `standardLibrary.knowledgeGroups` SHALL 以 AI 导航选中的知识点为第一层
- **AND** 每个知识点分组 SHALL 包含该知识点下被导航选中的能力点、易错点或提升点
- **AND** prompt SHALL 要求模型沿“知识点 -> 能力点 -> 易错点/提升点”理解候选结构

#### Scenario: 模型仍可自由判断命中状态
- **WHEN** AI 导航确认的知识点诊断层仍无法解释当前提交
- **THEN** 最终诊断模型 SHALL 可以返回 `PARTIAL`、`MISS` 或 `OUT_OF_LIBRARY`
- **AND** 模型 SHALL NOT 因导航阶段选择过同知识点而强行输出易错点 `HIT`

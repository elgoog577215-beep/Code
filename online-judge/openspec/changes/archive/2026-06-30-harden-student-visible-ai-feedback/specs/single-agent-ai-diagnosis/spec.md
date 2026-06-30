## MODIFIED Requirements

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

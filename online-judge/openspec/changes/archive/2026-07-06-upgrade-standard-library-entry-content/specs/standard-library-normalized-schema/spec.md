## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: 标准库条目必须具备教学可用的内容质量
AI 标准库 seed SHALL 避免低信息密度模板、口语化占位和过宽泛描述；能力点、易错点和提升点都 MUST 包含可用于教学诊断的对象、边界、状态、原因或验证动作。

#### Scenario: 能力点不是泛化识别标签
- **WHEN** 系统加载 `SKILL_UNIT` seed
- **THEN** 能力点名称和描述 SHALL 说明学生需要掌握的具体判断或操作
- **AND** SHALL NOT 只表达为“能识别某某相关的知识点和错误表现”

#### Scenario: 易错点包含具体错误行为
- **WHEN** 系统加载 `MISTAKE_POINT` seed
- **THEN** 易错点名称和定义 SHALL 描述具体错误行为或可观察症状
- **AND** commonMisconception SHALL 说明学生产生该错误的具体认知原因
- **AND** SHALL NOT 使用“理解或应用偏差”等泛化模板作为名称

#### Scenario: 自动兜底条目也满足最低质量
- **WHEN** 系统从知识树自动生成 full coverage 标准库条目
- **THEN** 自动生成的能力点和易错点 SHALL 至少包含知识节点名称、所属主题语境和一个验证或调试动作
- **AND** SHALL NOT 输出低信息占位句作为学生或教师可见文本

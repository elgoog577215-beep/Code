## MODIFIED Requirements

### Requirement: 学生画像驱动推荐

系统 SHALL 复用同一套学生细颗粒画像生成客观状态和可选学习候选，MUST NOT 在学生首页把内部推荐策略包装为唯一主要行动。

#### Scenario: 有未解决错因
- **WHEN** 学生画像存在最近失败题、错因标签或复盘卡片
- **THEN** 系统 MAY 在对应题目或学习记录中标记“存在未完成修正”
- **AND** 标记 SHALL 说明所依据的提交或错因
- **AND** 学生 SHALL 仍能从完整作业和题目范围自主选择

#### Scenario: 基础问题较少
- **WHEN** 学生近期基础层问题较少或已有 AC
- **THEN** 系统 MAY 展示同类题、提高层或迁移练习候选
- **AND** 系统 MUST NOT 自动替学生跳转或隐藏其他可选任务

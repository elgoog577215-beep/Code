## ADDED Requirements

### Requirement: 质量回归必须覆盖标准库成长字段
诊断质量回归 SHALL 检查 `diagnosisCandidates`、标准库路径、命中状态和 `libraryGrowth.candidates` 的一致性。

#### Scenario: PARTIAL 样本生成成长候选
- **WHEN** 样本期望 `libraryFit=PARTIAL` 且具体错误点缺失
- **THEN** 质量回归 SHALL 要求至少一个待审核成长候选
- **AND** 候选 SHALL 包含归属路径、错误表现、典型代码特征和学生解释话术

#### Scenario: HIT 样本不生成成长候选
- **WHEN** 样本期望 `libraryFit=HIT`
- **THEN** 质量回归 SHALL 要求成长候选为空或被后端忽略

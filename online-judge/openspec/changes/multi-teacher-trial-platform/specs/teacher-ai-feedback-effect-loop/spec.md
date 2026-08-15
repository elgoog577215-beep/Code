## ADDED Requirements

### Requirement: 教师反馈闭环必须遵守额度与降级边界
系统 SHALL 仅在有效教师作业和可用额度下调用外部模型，并 SHALL 在额度耗尽或供应商失败时保留基础评测证据，不得中断教师查看学生学习结果。

#### Scenario: AI 额度耗尽但评测完成
- **WHEN** 学生提交已完成本地评测但教师额度耗尽
- **THEN** 教师端 SHALL 仍可查看评测结果和基础提示
- **AND** AI 区域 SHALL 明确显示额度已用尽而非伪造诊断


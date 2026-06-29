## MODIFIED Requirements

### Requirement: 质量评测面向学生实际体验

AI 诊断质量评测 SHALL 同时评估判断准确性和学生实际可见文案质量。

#### Scenario: 评测报告生成
- **WHEN** 质量评测运行完成
- **THEN** Markdown 报告 SHALL 展示每道题学生实际看到的 `studentReport`
- **AND** JSON 结果 SHALL 记录主因命中、标准库误用、文案通俗度、长度、答案泄露、基础层和提高层主次、库外判断是否合理

#### Scenario: 评测失败分类
- **WHEN** 某道样例未达标
- **THEN** 系统 SHALL 使用固定失败分类记录原因
- **AND** 分类 SHALL 能指向召回、模型、提示词、标准库或后端校验中的具体责任层

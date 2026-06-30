## MODIFIED Requirements

### Requirement: 诊断质量评测闭环
系统 SHALL 提供离线诊断质量评测，使用典型高中信息学样例检查主因命中、学生文案、安全边界、长度和标准库使用情况。评测报告 MUST 单独保留学生真实可见反馈，并统计学生可见反馈质量风险。

#### Scenario: 生成质量报告
- **WHEN** 诊断质量评测运行完成
- **THEN** 系统 SHALL 输出 JSON 机器结果和 Markdown 人类报告，并保留每道题的学生端 `studentReport`

#### Scenario: 学生体验维度进入评测
- **WHEN** 诊断质量评测运行完成
- **THEN** 系统 SHALL 记录主因命中、标准库误用、文案通俗度、长度、答案泄露、基础层和提高层主次、库外判断是否合理

#### Scenario: 失败分类稳定
- **WHEN** 样例诊断未达到质量要求
- **THEN** 系统 SHALL 将失败归入 `RECALL_MISS`、`MODEL_MISREAD`、`TEXT_BAD`、`ANSWER_LEAK`、`TOO_LONG`、`LIBRARY_GAP`、`VALIDATOR_TOO_STRICT` 或 `VALIDATOR_TOO_LOOSE`
- **AND** 分类 SHALL 能指向召回、模型、提示词、标准库或后端校验中的具体责任层

#### Scenario: 学生可见反馈进入人工审查
- **WHEN** live eval 或诊断质量评测运行完成
- **THEN** 报告 SHALL 提供只含学生真实可见文本的字段
- **AND** 报告 SHALL 标记直接给改法、内部痕迹、提高层弱和过长等质量风险

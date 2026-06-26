# ai-diagnosis-quality-loop Specification

## Purpose
TBD - created by archiving change complete-ai-diagnosis-quality-loop. Update Purpose after archive.
## Requirements
### Requirement: 诊断质量评测闭环
系统 SHALL 提供离线诊断质量评测，使用典型高中信息学样例检查主因命中、学生文案、安全边界、长度和标准库使用情况。

#### Scenario: 生成质量报告
- **WHEN** 诊断质量评测运行完成
- **THEN** 系统 SHALL 输出 JSON 机器结果和 Markdown 人类报告，并保留每道题的学生端 `studentReport`

#### Scenario: 失败分类稳定
- **WHEN** 样例诊断未达到质量要求
- **THEN** 系统 SHALL 将失败归入 `RECALL_MISS`、`MODEL_MISREAD`、`TEXT_BAD`、`ANSWER_LEAK`、`TOO_LONG`、`LIBRARY_GAP`、`VALIDATOR_TOO_STRICT` 或 `VALIDATOR_TOO_LOOSE`

### Requirement: 标准库成长候选池
系统 SHALL 将库外发现写入标准库成长候选池，并要求教师审核后才进入正式标准库。

#### Scenario: 库外发现进入候选池
- **WHEN** 单诊断 Agent 输出 `OUT_OF_LIBRARY` 或 `libraryGrowth.candidates`
- **THEN** 系统 SHALL 记录来源题目、提交、建议路径、相似条目、证据摘要、置信度和候选状态

#### Scenario: 候选不自动入库
- **WHEN** 标准库成长候选被创建或聚合
- **THEN** 系统 MUST NOT 自动把候选写入正式标准库条目

#### Scenario: 教师批准候选
- **WHEN** 教师批准一个成长候选
- **THEN** 系统 SHALL 创建或更新正式标准库条目，并标记相关 embedding 过期


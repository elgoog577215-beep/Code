# ai-diagnosis-quality-loop Specification

## Purpose
TBD - created by archiving change complete-ai-diagnosis-quality-loop. Update Purpose after archive.
## Requirements
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
- **AND** 分类 SHALL 能指向导航、模型、提示词、标准库或后端校验中的具体责任层

#### Scenario: 学生可见反馈进入人工审查
- **WHEN** live eval 或诊断质量评测运行完成
- **THEN** 报告 SHALL 提供只含学生真实可见文本的字段
- **AND** 报告 SHALL 标记直接给改法、内部痕迹、提高层弱和过长等质量风险

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

### Requirement: AI 运行质量回归应覆盖模型配置与安全误杀
系统 SHALL 用自动化测试覆盖默认模型配置、学生可见安全误杀边界和正式诊断 prompt 的关键约束，避免真实链路质量回退只能靠人工仿真发现。

#### Scenario: 默认模型配置回归
- **WHEN** 配置文件被修改
- **THEN** 自动化测试 SHALL 能发现默认模型是否退回到未验证模型

#### Scenario: 安全误杀回归
- **WHEN** 安全关键词或校验逻辑被修改
- **THEN** 自动化测试 SHALL 同时覆盖合理诊断通过和直接改法拦截

#### Scenario: Prompt 约束回归
- **WHEN** 正式诊断 prompt 被修改
- **THEN** 自动化测试 SHALL 检查学生报告与后端 metadata 分层、标准库参考定位和证据决定建议数量仍然存在

### Requirement: 质量回归必须覆盖标准库成长字段
诊断质量回归 SHALL 检查 `diagnosisCandidates`、标准库路径、命中状态和 `libraryGrowth.candidates` 的一致性。

#### Scenario: PARTIAL 样本生成成长候选
- **WHEN** 样本期望 `libraryFit=PARTIAL` 且具体错误点缺失
- **THEN** 质量回归 SHALL 要求至少一个待审核成长候选
- **AND** 候选 SHALL 包含归属路径、错误表现、典型代码特征和学生解释话术

#### Scenario: HIT 样本不生成成长候选
- **WHEN** 样本期望 `libraryFit=HIT`
- **THEN** 质量回归 SHALL 要求成长候选为空或被后端忽略

## ADDED Requirements

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

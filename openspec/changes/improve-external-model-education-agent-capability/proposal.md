## Why

当前系统已经能调用外接大模型并记录 fallback、partial、live eval 等状态，但复杂样本上的真实模型能力仍容易被本地规则命中、模板兜底或输出截断掩盖。项目需要把外接大模型明确定位为“教育 AI agent”：它要承担主错因判断、复杂信号取舍、证据化教学决策和不泄题提示，而不是只被要求填一个庞大的 JSON。

## What Changes

- 将外接模型主任务重定义为教育诊断判断：读题目、学生代码、判题证据和标准库后，输出当前最该处理的错误、证据、次要问题取舍、继续提升方向和下一步学习动作。
- 新增外接模型教育 agent prompt 协议，减少机械 schema 压力，强调教学判断顺序：主错因 -> 证据 -> 次要/干扰信号 -> 学习动作。
- 扩展标准库为“模型教学协议”，在保留 taxonomy、教学动作和安全规则的同时，明确主错因优先级、学生端表达和禁止泄题边界。
- 评测只统计真实外接模型完成的能力结果，fallback、本地规则和本地模板不得计入外接模型教育智能分。
- 保持学生端主契约 `studentFeedback` 不变，本地逻辑只负责输入打包、安全校验、结果组装和落库。

## Capabilities

### New Capabilities

- `external-model-education-agent-capability`: 外接模型 SHALL act as the education diagnosis agent for failed submissions, producing evidence-grounded teaching judgments without counting local fallback as model capability.

### Modified Capabilities

无。

## Impact

- 影响外接模型 runtime prompt、标准库协议、模型输出 payload、validator/normalizer 和 live eval 质量口径。
- 不新增学生端 DTO，不迁移数据库，不提交 API key。
- 真实 live eval 仍需要显式配置外接模型 token；无 token 时只运行结构和本地聚合测试。

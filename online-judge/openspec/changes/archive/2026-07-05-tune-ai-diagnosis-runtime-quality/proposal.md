# 调整 AI 诊断链路运行质量

## Why

最新 8 题真实链路仿真显示，AI 链路已经能跑通，但质量问题集中在模型默认配置、安全误杀、诊断输出复杂度和评测闭环上。现在需要做一轮小而聚焦的升级，让默认链路更稳定、更少误拦，并继续保持标准库作为教学参考规范包。

## What Changes

- 将默认外部诊断模型调整为已验证可用的 `Qwen/Qwen3-235B-A22B-Instruct-2507`。
- 调整学生可见安全校验：保留直接答案泄露拦截，但减少对正常算法诊断词的误杀。
- 简化 `diagnosis-report-v2` 的提示词定位：强调学生自然报告为主、后端 metadata 为辅，避免模型把多个结构目标混成学生文案。
- 补充回归测试，覆盖默认模型配置、安全误杀边界和 prompt 简化原则。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `ai-diagnosis-orchestrator-v2`：默认模型应指向已验证可用的主诊断模型，并保持单诊断 Agent 主链路。
- `student-visible-ai-feedback-quality`：安全校验应区分直接给答案和正常诊断提示，减少合理学生反馈被误拦。
- `ai-prompt-context-quality`：正式诊断提示词应把学生自然报告和后端结构 metadata 分层表达，避免输出目标过度复杂。
- `ai-diagnosis-quality-loop`：质量回归应覆盖模型配置、误杀边界和 prompt 关键约束。

## Impact

- 配置：`src/main/resources/application.yml`。
- 后端 prompt 与校验：`PromptTemplateRegistry`、`ModelOutputSafetyPolicy`、`AdviceGenerationOutputValidator`。
- 测试：新增或调整 AI 链路相关单元测试。
- 不改变数据库结构，不新增并行 AI 后端模块，不改变标准库主结构。

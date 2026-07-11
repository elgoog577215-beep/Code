## Why

DeepSeek V4 Pro 已能通过 ModelScope 正常调用，但默认推理模式会消耗大量输出额度，导致完整诊断中的结构化学生反馈被截断，并放大耗时与配额压力。当前需要把运行策略收束为统一非推理模式，同时将 V4 Pro 设为主模型，以稳定获得完整结构化结果。

## What Changes

- 所有 ModelScope Chat Completions 请求显式关闭推理模式，不再生成或消费 `reasoning_content`。
- 将 `deepseek-ai/DeepSeek-V4-Pro` 设为本地、示例配置和学校服务器的主模型。
- 保留 Qwen 模型池作为供应商错误或配额异常时的完整路径备用模型，不减少诊断阶段和调用数量。
- 增加请求体、默认配置、流式结构化输出和真实困难样本回归验证。

## Capabilities

### New Capabilities

- `ai-model-runtime-policy`: 规定主模型选择、统一非推理请求模式、备用模型顺序和切换前验证门禁。

### Modified Capabilities

- `durable-ai-diagnosis-workflow`: 持久化阶段需要记录实际执行模型，并在模型切换后继续满足完整输出与恢复要求。

## Impact

- 后端 ModelScope 请求体构造、AI 报告服务和 Coach 服务运行配置。
- `application.yml`、`.env.example`、Docker Compose 以及本地/服务器 `.env`。
- 模型请求单元测试、完整链路真实样本测试和服务器 AI smoke。

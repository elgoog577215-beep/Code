## Why

当前项目已经具备外部模型 runtime、分阶段提示词、标准库裁剪、live eval 和教师端 AI 质量统计，但这些能力还没有形成足够硬的在线质量闭环。

本轮目标不是继续让离线规则评测更好看，而是提升“真实接入外部大模型后”的教育 agent 表现：学生应感受到 AI 能帮他定位错因、推进思考、避免直接给答案；老师应能看到 AI 输出质量、风险和需要人工介入的原因。

## What Changes

- 新增评测样本可读性门禁，防止乱码、污染短语和低质量 fixture 混入评测集。
- 强化标准库裁剪，让细粒度错因也能带出对应教学动作，减少外部模型只拿到粗标签时的泛化提示。
- 让 Coach 追问复用诊断标准库、证据引用和安全规则，避免诊断链和追问链各说各话。
- 新增 live eval 质量门槛，可在真实外部模型测试时按信号命中、证据引用、安全通过和兜底比例判断是否达标。
- 增强教师端 AI 质量统计，区分模型调用质量、教学输出质量和需要人工介入的风险信号。

## Capabilities

### New Capabilities

- `online-education-agent-quality`: 定义在线教育 agent 的质量门禁、标准库复用、live eval 阈值和教师端解释策略。

### Modified Capabilities

- `external-ai-assistant-eval-loop`: 在已有 live eval 基础上增加质量门槛和样本卫生检查。
- `external-model-education-agent-runtime`: 在已有外部模型 runtime 基础上强化标准库和 Coach 追问复用。

## Impact

- 后端 AI 链路：`DiagnosticAgentService`、`AiReportService`、`CoachAgentService`、标准库构建与模型输出校验。
- 教师端 AI 质量统计：质量概览、趋势或优先级分析相关服务与 DTO。
- 评测资源与测试：`online-judge/src/test/resources`、`online-judge/src/test/java`。
- OpenSpec 文档：新增本变更的 proposal、design、spec 和 tasks。

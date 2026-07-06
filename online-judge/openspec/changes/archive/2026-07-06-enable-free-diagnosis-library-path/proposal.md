## Why

当前 AI 诊断容易把标准库候选或本地证据片段当成答案来源，导致模型没有先独立判断代码真实错误。新链路需要让外部 AI 在自由诊断时同步参考标准库，并把诊断结果挂到明确的标准库路径上；标准库没有覆盖具体错因时，应沉淀为待审核候选，而不是硬套已有错误点。

## What Changes

- 调整 `diagnosis-report-v2` 提示词契约：模型必须先基于题目、完整代码和运行结果判断真实问题，同时参考标准库路径做标签标注。
- 要求每个诊断候选输出标准库路径；具体错误点未命中时允许 `PARTIAL` 或 `MISS`，并生成待审核标准库成长候选。
- 调整输出校验：不再只校验标准库 ID 是否存在，还要校验路径、命中状态和库外候选之间的关系。
- 增加“分层优惠最短路”回归样本，防止把“优惠券转移未折半”误诊为起点初始化。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `ai-diagnosis`: 诊断候选必须支持自由诊断与标准库路径同步标注。
- `ai-prompt-context-quality`: 提示词必须明确标准库是参考路径和命名体系，不能压过模型对代码语义的独立判断。
- `standard-library-normalized-schema`: 标准库成长候选必须携带最接近的标准库路径，并以待审核状态进入后端沉淀。

## Impact

- 后端 AI 诊断提示词：`PromptTemplateRegistry`
- 后端输出校验：`AdviceGenerationOutputValidator`
- 后端报告解析与标准库成长字段消费：`AiReportService` 相关模型/测试
- OpenSpec 回归规格与单元测试

## Why

现有 AI 诊断链路已经具备上下文打包、外接大模型诊断、标准库映射和标准库成长的基础，但多个后处理点仍会把结构化结果压成单条、借用第一条证据、或要求提升建议必须有代码证据，导致教师端看到的修正建议、提升建议和标准库成长候选不符合真实教学判断。

本变更要把实现重新对齐到目标链路：后端构造可追溯上下文包，外部模型返回结构化诊断与标签映射，后端只做安全、证据和契约校验，标准库缺口进入人工候选池而不是直接污染正式库。

## What Changes

- 统一 AI 诊断输出契约：`studentReport` 只作为摘要，逐条修正建议、提升建议、标签命中和标准库成长候选必须保留为独立结构化数组。
- 调整证据语义：修正建议必须尽量绑定当前提交证据；提升建议允许没有直接代码证据，但必须显式保留“无直接代码证据”的状态，不能借用第一条修正证据伪装成代码证据。
- 收紧后处理边界：mapper、normalizer 和 view assembler 不得用摘要文本或兜底主项整体覆盖有效多条建议，不得把全局证据自动塞进每条 item。
- 标准库成长改为候选池模式：外部模型只能提出库外发现或补库候选；后端补充当前题目、提交和证据线索，候选进入待审核池，由教师审核、编辑、合并、拒绝或忽略后才影响正式标准库。
- 区分快反馈和教师深诊断运行时：学生端快反馈可以保留低延迟压缩；教师/作业/题目诊断链路需要保留更完整上下文、输出 token 预算和结构化数组尾部，避免深度诊断被低延迟配置截断。
- 增加回归测试，覆盖多条建议保真、提升建议无代码证据、证据不借用、候选池持久化、以及兼容请求输出预算。

## Capabilities

### New Capabilities

- `ai-diagnosis-contract-alignment`: 定义后端 AI 诊断契约对上下文包、结构化输出、多建议保真、证据状态和运行时预算的统一要求。

### Modified Capabilities

- `ai-diagnosis-orchestrator-v2`: 强化正式诊断 Agent 对标准库参考包、结构化建议数组、运行时深诊断上下文和输出预算的要求。
- `student-visible-ai-feedback-quality`: 强化学生可见建议的多条保真和证据展示边界，允许提升建议没有直接代码证据但不能伪造证据。
- `standard-library-normalized-schema`: 明确标准库成长候选必须携带后端补充的来源题目、提交和证据线索，并保持待审核状态。
- `standard-library-review-workflow`: 强化候选池是正式标准库变更的唯一入口，AI 诊断不得直接写入正式库。

## Impact

- 后端 AI 链路：`AiReportService`、`PromptTemplateRegistry`、`AdviceGenerationOutputValidator`、`AdviceGenerationFeedbackMapper`、`StudentFeedbackAssembler`、`StudentFeedbackViewAssembler`、`ExternalModelAgentRuntime`、`ExternalModelChatRequestFactory`。
- 标准库成长：`AiStandardLibraryGrowthAgentService`、标准库成长候选 DTO/实体/Repository/API，以及教师端治理台读取的数据字段。
- 前端展示边界：学生 AI 反馈卡片、教师诊断结果页和标准库人工治理视图可能需要读取证据状态和候选来源字段。
- 测试与配置：新增或更新 AI 诊断契约单测、标准库候选池单测、外部模型请求构造测试和相关 OpenSpec 校验。

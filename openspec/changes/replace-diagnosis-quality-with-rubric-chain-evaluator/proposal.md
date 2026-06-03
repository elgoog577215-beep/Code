## Why

当前 AI 诊断评测已经积累了复杂样本、模型 trace、学生反馈质量、live eval 报告和 comparison 报告，但主质量口径仍由多组散指标共同表达。这样会让维护者看到很多分数，却不容易判断模型到底是没看对证据、没选对主因、被干扰项带偏、教学动作空泛，还是安全边界失败。

下一轮不再在旧评分器上继续修补，而是把诊断质量评估替换为“金标准 rubric 驱动的链路评判器”。每条复杂 fixture 先生成权威 rubric，再用统一链路判分判断模型输出是否完成正确教育诊断链路。

## What Changes

- 新增 `DiagnosisQualityRubric`，从 200+ 条复杂 fixture 的 gold 信息生成权威标准答案。
- 新增 `DiagnosisChainRubricEvaluator`，以 `证据定位 -> 主因判定 -> 干扰项处理 -> 教学反馈 -> 安全边界` 作为唯一主质量链路。
- live/offline eval report 主摘要切换到 rubric chain quality，不再把旧的 `complexQuality`、`intelligenceQuality`、`modelTraceQuality`、`studentFeedbackQuality` 作为核心质量结论。
- fallback、本地规则和本地模板只计入 runtime 状态，不进入模型诊断正确率。
- 旧散指标仅作为 legacy 兼容字段保留；没有解释力的聚合不再进入主摘要和主质量判断。

## Capabilities

### New Capabilities

- `diagnosis-rubric-chain-quality-eval`: 系统 SHALL use gold-rubric-driven chain evaluation as the primary quality method for complex external-model diagnosis eval.

### Modified Capabilities

无。

## Impact

- 影响测试侧复杂诊断质量 scorer、live eval report DTO、report 聚合、console summary 和相关结构测试。
- 不改生产学生端 API，不迁移数据库，不提交外部模型 token。
- 真实模型正确率口径变为：真实模型完成且未 fallback 的复杂 case 中，证据、主因、安全全过，并且教学动作可操作，才算诊断正确。

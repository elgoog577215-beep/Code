## Why

最近一次真实 ModelScope live eval 中，assistant 小样本 3/3 完成、model diagnosis smoke 2/2 完成，说明当前外部模型链路已经出现可保留的正向样本。上一轮已经让失败/partial 样本进入 runtime 草稿，但成功样本仍只留在 per-case report 中，后续更换模型或调整 prompt 时缺少“这类输出不能退化”的正向 baseline。

本轮把成功且质量达标的 live eval 输出导出为 quality baseline 草稿，让真实外部模型的好结果也能沉淀为可比较、可回归的教育 agent 基线。

## What Changes

- 为 assistant live eval 和 model diagnosis live eval 报告新增顶层 `qualityBaselineDrafts` 与 `qualityBaselineDraftCount`。
- 新增 test-scope 的 `LiveEvalQualityBaselineDraft` DTO 和 factory。
- 从已完成、未 fallback、安全通过、信号命中、证据有效的 live eval entries 中生成正向 baseline 草稿。
- baseline 草稿包含 caseId、assistantType/stage、model、promptVersion、expectedSignals、evidenceRefs、teachingAction、teacherExpectation、outputSummary、mustKeep、mustNotMention 和 regressionPurpose。
- 草稿对 outputSummary、teacherExpectation、outputDetail 做截断和敏感片段脱敏，不输出 API Key、token 或完整代码。
- 扩展无需真实 API Key 的结构测试，并用真实 live eval 报告验证字段存在。

## Capabilities

### New Capabilities

- `live-eval-quality-baseline-export`: 覆盖真实外部模型成功样本如何从 live eval 报告导出为正向质量 baseline 草稿。

### Modified Capabilities

- 无。

## Impact

- 测试报告 DTO：`AssistantLiveEvalReport`、`LiveModelEvalReport` 新增 quality baseline 草稿字段。
- 测试工具：新增 quality baseline DTO/Factory。
- 评测链路：`AssistantLiveEvalTest` 与 `ModelDiagnosisEvalTest` 在 summarize 阶段填充 baseline 草稿。
- 测试：扩展结构测试，覆盖成功样本筛选、mustKeep、证据引用和脱敏。
- 生产代码：不改动生产服务；本轮增强 test/eval 产物。

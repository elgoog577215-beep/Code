## Why

当前复杂样本和本地质量门禁已经能提供可复现真值，但它们只能证明“本地评测规则存在”，不能证明外接大模型具备教育场景里的自主分析和智能诊断能力。项目需要把 live model eval 从附属 smoke test 升级为一等能力评测，明确只统计真实外接模型完成的诊断结果，避免 fallback 或本地规则命中冒充 AI 能力提升。

## What Changes

- 新增外接模型智能能力评测报告，区分 `localTruth`、`modelOutput`、`modelJudgment`、`qualityScore` 与运行状态。
- 将复杂样本质量指标映射为外接模型智能指标：自主主错因发现、复杂信号优先级、证据化推理、教学决策质量、干扰抵抗和安全边界。
- 固定首批 14 条 `complex-live-*` 代表集，覆盖 14 类 bugPattern，作为外接模型智能 baseline 的默认小样本集合。
- 扩展 live eval summary，统计真实模型完成样本的智能能力画像，并显式排除 fallback、本地草稿和规则命中。
- 增强诊断提示协议，要求外接模型先判断最该先教的主错因，再处理次错因，并说明为什么干扰信号不是主因。
- 预留 provider/model/baseUrl/runtime profile 等对比字段，为后续多模型比较打基础。

## Capabilities

### New Capabilities

- `external-model-intelligence-eval`: 外接模型 live eval SHALL measure real model intelligence on complex diagnosis cases without counting fallback or local-rule results as AI capability.

### Modified Capabilities

无。

## Impact

- 影响测试/评测侧代码：live model eval report、complex quality scorer 映射、baseline draft/regression gate、`ModelDiagnosisEvalTest`。
- 影响 prompt 协议：增强外接模型复杂多错因诊断决策规则，但不改变生产 DTO 主结构。
- 新增 report JSON 可空字段，保持旧报告读取兼容。
- 真实 live eval 可能消耗 ModelScope 配额；首轮只默认运行 14 条代表样本。

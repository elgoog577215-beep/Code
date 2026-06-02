## 1. OpenSpec

- [x] 1.1 创建 `external-model-intelligence-eval` 能力规格。
- [x] 1.2 写明 fallback 不计入 AI 能力、14 条代表集、智能指标映射和提示协议要求。

## 2. 智能报告结构

- [x] 2.1 扩展 `LiveModelEvalReport`，新增 intelligence summary、per-metric distribution 和 provider/runtime 对比字段。
- [x] 2.2 扩展 live eval entry，记录 localTruth、modelOutput、modelJudgment、qualityScore 和 intelligence metric signals。
- [x] 2.3 确保 fallback、本地规则命中和异常样本不计入 intelligence score。

## 3. 代表集与指标映射

- [x] 3.1 新增 14 条 complex-live 代表集选择逻辑，覆盖 14 个 bugPattern。
- [x] 3.2 将 6 个 complex quality metrics 映射为 6 个 external model intelligence metrics。
- [x] 3.3 在 summary 中输出每个 intelligence metric 的通过/失败分布。

## 4. Prompt 与 baseline gate

- [x] 4.1 增强诊断提示协议，强调主错因优先级、证据引用和干扰信号处理。
- [x] 4.2 扩展 quality baseline draft，仅从真实完成且通过 intelligence gate 的 complex 样本生成智能 baseline。
- [x] 4.3 扩展 baseline regression gate，检查 intelligence metric 不回退。

## 5. 测试与验证

- [x] 5.1 增加代表集、summary、fallback 排除和 metric distribution 单测。
- [x] 5.2 增加 prompt 协议和 baseline regression 测试。
- [x] 5.3 运行相关 Maven 测试。
- [x] 5.4 运行 `openspec validate evaluate-external-model-intelligence-capability --strict`。
- [x] 5.5 运行 secret scan 和 `git diff --check`。
- [x] 5.6 使用当前 ModelScope 配置跑 14 条代表性 complex live eval，并汇总真实外接模型能力结果；若配额或延迟阻断，记录阻断证据。

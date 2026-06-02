## Context

当前 single-call runtime 以完整 `ModelDiagnosisBrief` + `StandardLibraryPack` 作为输入，并要求一次性返回 diagnosis + teaching。此前几轮已经解决了 stream telemetry、输出截断归因、截断诊断保留等问题；现在真实 eval 的主要瓶颈是慢响应：模型能完成且命中标签，但 reasoning chunk 很大，两个 case 都超过 35s。

现有输入中包含较长的 prompt 规则、standardLibrary decisionProtocol、tag explanation、teacher/student explanation、完整 problem/code 摘要、学习记忆和 teacher calibration。它们提升教育质量，但不一定每次都需要完整传给低预算 live eval。

## Goals / Non-Goals

**Goals:**

- 提供显式 `low-latency` profile，压缩 runtime 输入和输出说明，便于真实外部模型更快完成。
- 保留教育 agent 必需的结构：候选标签、证据引用、安全边界、教学动作、学习轨迹摘要。
- 增加 request size telemetry，让后续 report 能证明“上下文是否真的变小”。
- live eval 可以用环境变量启用 profile，并输出 requestBytes/compact 标记。

**Non-Goals:**

- 不默认启用 low-latency profile。
- 不移除标准 profile 的完整教学规则。
- 不接受无证据、非法标签或高泄题风险输出。
- 不保存 raw request、raw response 或 API Key。

## Decisions

### 用 profile 包装 runtime 输入，而不是全局删 prompt

新增 runtime profile 字段，`standard` 保持现有行为，`low-latency` 才使用 compact 输入。这样可以在真实 eval 中 A/B 对比，不会破坏教师端或生产默认行为。

### Compact brief 优先保留可判定证据

low-latency brief 保留：schema/version、problem title/direction 摘要、verdict/language、短代码摘录、首个失败样例、最多 3 个 candidateSignals、证据 refs、候选 tag、学习轨迹摘要和 hidden boundary。压缩或移除：长 problem description、冗长 code excerpt、visible case 列表、多余 memory/calibration 细节。这样不丢掉当前判断需要的证据链。

### Compact standard library 只保留契约必需字段

low-latency pack 保留 tag id、label、parentTag、teachingAction 和 teachingActions id/label；删除冗长的 student/teacher explanation、whenToUse、studentTaskTemplate、长 decisionProtocol。Prompt 中仍要求 tag/evidence/action 必须来自 pack，因此校验边界不变。

### Request telemetry 只记录体积和 profile

`aiInvocation` 记录 runtimeProfile、requestBytes 和 requestCompact。live eval report 读取这些字段，并让 runtime draft 对慢响应样本同时展示 requestBytes、latencyMs、stream chunk。不得记录 raw request body。

## Risks / Trade-offs

- [Risk] 过度压缩导致标签命中下降。→ 默认关闭，并用 live eval 对比 issue/fine/evidence/safety。
- [Risk] 删除解释字段可能让教学提示变薄。→ 保留 tag label、teachingAction 和本地 safety fallback；若 quality 降低，report 会显示。
- [Risk] Provider 抖动掩盖 profile 效果。→ report 同时记录 requestBytes、latencyMs、chunk counts，至少证明输入体积变化。
- [Risk] 新 telemetry 被误认为包含 prompt。→ 只保存数字和 profile 名称，不保存 request 内容。

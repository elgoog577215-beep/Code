## Context

当前 live eval 报告已经有两类有价值输出：

- 原始 entries：完整记录每个 case 的完成状态、信号命中、证据、安全、教学动作和输出摘要。
- runtime 草稿：上一轮新增，只在失败、partial、exception 或质量缺口时生成可审查样本。

最近真实 ModelScope 评测没有产生 runtime 草稿，因为模型小样本全部成功。这是好信号，但如果不把成功样本沉淀成正向 baseline，后续 prompt 或模型升级只能靠汇总数字判断是否退化，无法明确“哪些成功输出应该保留”。

## Goals / Non-Goals

**Goals:**

- 从 live eval 成功样本生成 quality baseline 草稿。
- assistant 报告要求 completed、无 fallback、安全通过、信号命中、证据有效，且教学动作有效或非诊断类可解释。
- model diagnosis 报告要求无 fallback、标签命中、证据有效、安全通过。
- baseline 草稿包含可回归检查的 mustKeep 与 mustNotMention。
- 不需要真实 API Key 也能通过结构测试验证筛选和脱敏。

**Non-Goals:**

- 不自动写入静态 fixture 文件。
- 不把 baseline 草稿展示到教师端。
- 不改变 live eval 阈值或真实调用规模。
- 不将质量 baseline 与 runtime failure 草稿合并。

## Decisions

### 与 runtime 草稿并列输出

报告顶层新增 `qualityBaselineDrafts`，与 `runtimeFixtureDrafts` 并列。这样报告消费者可以同时看到“需要修复/沉淀的失败样本”和“应该防退化的成功样本”。

### 独立 DTO/Factory

新增 `LiveEvalQualityBaselineDraft` 与 `LiveEvalQualityBaselineDraftFactory`。独立 factory 可以表达成功样本筛选逻辑，不污染 runtime 失败分类器。

### baseline 只收高可信成功样本

assistant baseline 只收：

- `completedOutput=true`
- `fallbackUsed=false`
- `safetyPassed=true`
- `expectedSignalHit=true`
- `evidenceValid=true`
- `teachingActionValid` 不为 false

model diagnosis baseline 只收：

- `fallbackUsed=false`
- issue 或 fine tag 命中
- `evidenceValid=true`
- `safetyPassed=true`

### mustKeep 面向回归

`mustKeep` 不复制完整模型输出，而是提取应保持的结构化点：expected signal、evidence refs、teaching action、teacher expectation 摘要和输出摘要。这样 baseline 可用于后续人工或自动转 fixture，而不会把完整输出固化为唯一答案。

## Risks / Trade-offs

- [Risk] 成功样本过多导致报告膨胀。→ 当前 smoke 样本很小；factory 仅保留摘要和有限 refs，后续可加上限。
- [Risk] baseline 过度绑定当前模型措辞。→ baseline 记录 mustKeep 的结构化质量点，不要求逐字复现。
- [Risk] 成功样本也可能含隐性问题。→ baseline 筛选要求安全、信号和证据都通过，仍保留 teacherExpectation 供人工复核。

## 设计目标

把“真实外部模型可比较”所需的数据基础从零散内置样本，升级为独立的 live-model core fixture 集合。这个集合优先覆盖教育价值高、错因明确、baseline 易比较的诊断样本，而不是追求数量最大。

## 数据来源

首批 core set 复用现有 assistant diagnosis fixture 的结构与内容，选择 6 类最能检验外部模型教育诊断能力的 case：

- 输入读取结构：多组查询只读一次。
- 多组数据状态重置：上一组状态污染下一组。
- DP 状态设计：写了 dp 但状态来源不足。
- 贪心假设反例：局部选择缺少证明。
- 最大规模复杂度：线性或逐步模拟在大边界下不可承受。
- 输出格式细节：算法正确但输出多余空白。

这些样本都包含：

- `caseId`
- problem / submission / caseResults / baseline
- `expectedIssueTags`
- `expectedFineTags`
- `requiredEvidenceRefs`
- `mustMention`
- `mustNotMention`
- `quality`

## 接入方式

新增 `LiveModelCoreEvalFixtureLoader`，读取 `/diagnosis-eval-fixtures/live-model-core-cases.json` 并转成测试可用对象。`ModelDiagnosisEvalTest#allEvalCases()` 在三个内置 smoke 样本和 teacher correction 样本之外追加 core set。

live smoke 使用：

- `AI_EVAL_SMOKE_LIMIT` 控制数量。
- `AI_EVAL_CASE_IDS` 控制具体 case id，支持逗号分隔，大小写不敏感。

这样真实 smoke 可以先跑：

```text
AI_EVAL_CASE_IDS=live-core-input-parsing,live-core-output-format
```

而不是每次扩大全量消耗。

## 可比性策略

core set 的目标不是立刻让所有样本通过，而是让 baseline 具备稳定锚点：

- 若模型完成并命中 `expectedIssueTags` / `expectedFineTags`，生成 quality baseline draft。
- 若模型 fallback，生成 runtime fixture draft 和 recovery smoke guidance。
- 若样本缺失，baseline regression gate 能输出具体 missing case。

## 边界

- 不把规则 fallback 计为模型成功。
- 不输出 API Key、raw prompt、raw response 或 provider 原始 body。
- 不在本轮扩大到 50+ 样本；先保证核心样本可审计、可过滤、可复跑。
- 不修改生产诊断逻辑。

## 验证

- 结构测试覆盖 core set 加载、唯一 case id、标签和证据完整。
- `evalCasesExposeStableExpectedTagsWithoutLiveModel` 覆盖 core set 进入 `allEvalCases()`。
- live smoke 可用 `AI_EVAL_CASE_IDS` 跑核心子集。
- OpenSpec strict validate、secret scan、`git diff --check`。

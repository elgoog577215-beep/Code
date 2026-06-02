## Context

当前项目已经有 `student-hint-cases.json`、`teacher-corrections.json` 和 `live-model-core-cases.json`，但它们更适合验证单一错因、提示安全或小规模外接模型可用性。用户明确要求在无人可人工介入的情况下保证数量和质量，因此样本真值必须来自生成配方和可复现执行证据。

## Goals / Non-Goals

**Goals:**

- 自动生成 100 条 40-80 行复杂 Python 学生提交样本。
- 每条样本至少包含 2 个错误信号，并明确主错因、次错因、干扰信号、教学优先级。
- 每条样本由本地 Python 执行验证，保留 first failed case、actual/expected output 和 evidence refs。
- 标记 24 条 `complex-live` 候选，并允许 `AI_EVAL_CASE_IDS` 跑首批 6 条。

**Non-Goals:**

- 不爬取未知来源学生代码。
- 不让外接模型参与标签生成。
- 不修改生产诊断逻辑。
- 不一次性扩展到 500 条或加入人工复核流程。

## Decisions

- 使用确定性生成器而不是人工手写 100 条 JSON。这样可以用同一份 seed、bug recipe 和 runner 重建 fixture，并减少格式漂移。
- 题目种子以少量教育场景模板扩展为多变体样本。每个模板包含正确解法、测试用例和多个 bug recipe；每个变体组合 2-3 个错误信号。
- 标签真值来自主 bug recipe。`primaryRootCause` 和 `expectedFineTags[0]` 必须一致，`secondaryIssues` 记录其它注入错误，`distractingSignals` 记录代码中可能诱导模型跑偏的无关复杂度。
- 执行验证只使用本地 Python 标准库。正确解法必须 AC，错误提交必须产生目标 verdict，且 first failed case 可复现。
- 复杂样本进入 `ModelDiagnosisEvalTest#allEvalCases()`，但真实调用通过 `AI_EVAL_CASE_IDS` 控制，不默认全量烧额度。

## Risks / Trade-offs

- [Risk] 自动生成代码可能模板痕迹明显 -> 用多个题目模板、helper 函数、日志/校验函数和不同 bug 组合增加形态差异。
- [Risk] 多错因样本可能让主错因不唯一 -> 生成配方规定主错因必须解释 first failed case，次错因只作为后续或干扰信号。
- [Risk] 运行真实 6 条 live eval 成本和耗时不可控 -> 默认只跑结构测试；真实 smoke 通过环境变量显式小批量运行。
- [Risk] 现有工作区较脏 -> 本变更只新增/修改测试评测相关文件，不回滚无关改动。

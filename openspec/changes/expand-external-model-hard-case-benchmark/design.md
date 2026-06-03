## Context

仓库已有 `complex-student-submission-cases.json`，当前规模为 100 条，生成器保证 buggy 程序失败、correct solution 通过，并记录 primary root cause、secondary issues、distracting signals、required evidence refs、must mention 和 must not mention。现有测试还要求 24 条 live candidate、至少 12 类 bug pattern 和 40-80 行代码。

用户现在希望评测库“全面升级”：数量达到 200 条及以上，程序长度提升到 50 行或 100 行以上，并且错误要更有意思、更高质量。这个目标不应通过复制浅层变体凑数，而应让每条样本继续具备可执行证据和教师式 gold rubric。

## Goals / Non-Goals

**Goals:**

- 将确定性复杂诊断 fixture 扩展到至少 200 条。
- 保证每条 buggy 程序至少 50 行，至少 80 条样本达到 100 行以上。
- 保证每条样本仍有真实失败用例、正确解验证、主错因、干扰信号、证据引用和安全禁区。
- 扩大 bug pattern、主错因标签和 live candidate 覆盖，使外接模型面对更多复杂教育场景。
- 让测试门槛直接保护数量、长度、质量和 gold rubric 完整性。

**Non-Goals:**

- 本轮不修改默认 prompt 或提升 v4-lite 为默认版本。
- 本轮不跑依赖外部额度的 full live eval。
- 本轮不引入真实学生隐私数据，不抓取外部代码。
- 本轮不把生成器制造的本地 baseline 当成外接模型能力提升结论。

## Decisions

### 继续使用确定性生成器作为第一层 benchmark

本轮优先扩展现有 deterministic generator，而不是人工手写 200 条 JSON。这样可以保留可复现、可审计、可执行验证和 correct solution 验证，也能避免手工复制导致的不可维护样本。

替代方案是直接手写更多 fixture。该方式短期直观，但质量容易漂移，且难以保证每条样本都真的失败、正确解真的通过。

### 用模板扩展加审计变体提升规模

生成器保留“题型模板 + bug recipe + 变体注入”的结构，但提升输出数量、代码体量和变体差异。每个模板可以产生多个审计变体，但测试必须继续检查 generatorSpecId 唯一、语义来源多样、bug pattern 覆盖和 primary root cause 覆盖。

### 用硬性测试保护质量

测试不只检查数量，还要检查：

- 每条 source line count >= 50。
- 至少 80 条 source line count >= 100。
- 每条 injectedBugCount >= 3。
- 每条 verifiedByExecution 和 correctSolutionVerified 为 true。
- 每条有 primaryRootCause、secondaryIssues、distractingSignals、requiredEvidenceRefs、teacherExpectation、mustMention、mustNotMention。
- 至少 14 类 bug pattern，至少 14 类 primary fine-grained tag，至少 36 条 live candidate。

## Risks / Trade-offs

- [Risk] 通过机械 filler 把程序拉长，导致样本看起来复杂但诊断价值不高。→ 生成器中的加长代码必须以审计函数、辅助路径、边界处理或干扰分支形式出现，并通过测试检查 injectedBugCount 和 distractingSignals。
- [Risk] 200 条样本过大导致测试变慢。→ 继续用生成器离线提交 JSON，常规测试只加载和结构验证；生成器一致性测试按现有方式运行。
- [Risk] 自动生成 gold label 权威性仍低于真实教师标注。→ 本轮把它定位为第一层 hard benchmark，后续可从 200 条中精选 50 条做人审 gold set。

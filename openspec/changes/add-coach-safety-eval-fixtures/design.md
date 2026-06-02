## Context

`CoachAgentService` 会把模型返回的 JSON 草稿转换为 `CoachDraft`，再通过本地安全门校验 `answerLeakRisk`、提示策略边界、泄题短语和 evidenceRefs。安全门拒绝后，系统返回规则 fallback，并把 `failureReason` 标记为 `SAFETY_REJECTED`。上一轮变更已经让这个拒绝信号可落库、可汇总、可在教师端看到。

现有 `coach-turns.json` 是正向 fixture：它验证安全模型草稿能通过本地安全门。缺口是反向 fixture：系统还没有一组默认资源稳定验证“模型草稿越界时必须被拒绝”，也没有覆盖常见英文模型泄题表达。

## Goals / Non-Goals

**Goals:**

- 为 Coach 安全拒绝建立默认静态 fixture。
- 覆盖高风险输出、隐藏测试泄露、英文直接修复提示、证据缺失这几类风险。
- 批量断言拒绝样本会触发规则 fallback、`SAFETY_REJECTED` 和模型风险元数据。
- 补强本地安全门，使它不只识别中文“完整代码/直接改成”，也能识别常见英文越界表达。

**Non-Goals:**

- 不新增 live eval 或外部模型依赖。
- 不保存不安全模型草稿全文到数据库。
- 不改变 Coach prompt API、教师端 UI 或学生端可见文本。

## Decisions

### 新增独立的拒绝 fixture 文件

新增 `coach-safety-rejection-cases.json`，而不是把拒绝样本混进 `coach-turns.json`。这样正向通过样本与反向拒绝样本职责清晰：前者验证 Coach 能问出安全追问，后者验证安全门能挡住越界草稿。

### 复用现有 CoachEvalFixtureLoader

扩展 loader 新增 `loadSafetyRejections()` 和 `SafetyRejectionFixture` record。这样测试仍集中在 Coach agent eval 语境内，不引入新的 loader 类型和重复转换逻辑。

### 只断言安全门行为，不断言外部模型质量

测试使用 stubbed model response，目标是验证本地安全门和回退元数据。live eval 仍只跑安全通过 fixture，避免把不稳定外部模型输出作为默认验证门槛。

### 对本地泄题短语做小步补强

安全门新增识别英文 `complete code`、`final answer`、`direct fix`、`change it to`、`hidden test`、`reference solution` 等短语。对隐藏测试和直接修复这种即使模型自报 `LOW` 的输出，本地安全门也必须拒绝。

## Risks / Trade-offs

- [Risk] 关键词式安全门可能误伤少量安全文本。→ 本轮只加入典型越界短语，并通过 fixture 聚焦在 Coach 输出场景。
- [Risk] fixture 覆盖仍不是完整安全证明。→ 将其作为默认回归基线，后续可从真实安全事件继续沉淀新样本。
- [Risk] 不保存原始越界草稿导致教师无法看到具体泄露内容。→ 维持上一轮安全取舍，避免把可泄题内容固化到数据库。

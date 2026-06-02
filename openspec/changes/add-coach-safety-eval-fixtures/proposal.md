## Why

Coach 追问已经能把模型越界草稿拒绝并回退到规则追问，也能把 `SAFETY_REJECTED` 作为结构化信号沉淀到交互摘要。但当前默认 Coach eval fixture 只覆盖安全草稿通过，没有覆盖“模型草稿越界时必须被拒绝”的反向样本。

教育 agent 的安全能力需要可重复评测，而不是只依赖日志或个别单测。把直接给答案、泄露隐藏测试、英文直给修复、证据缺失等风险固化为 fixture，可以让 Coach 安全门在后续模型、提示词和策略迭代中持续防回退。

## What Changes

- 新增 Coach 安全拒绝 eval fixture 资源，覆盖完整答案泄露、隐藏测试泄露、英文直接修复提示和证据缺失。
- 扩展 Coach fixture loader，支持同时加载安全通过样本和安全拒绝样本。
- 扩展 `CoachAgentServiceTest`，批量验证拒绝样本会回退到规则追问、标记 `SAFETY_REJECTED`、保留模型风险信号，并且不会把 forbidden phrases 暴露给学生。
- 适度补强 Coach 本地安全门对英文泄题短语、隐藏测试短语和直接修复短语的识别。
- 不新增外部模型调用，不改变学生端 Coach API 行为。

## Capabilities

### New Capabilities

- `coach-safety-eval-fixtures`: 覆盖 Coach 模型追问安全拒绝样本的静态资源、加载结构和本地回归测试。

### Modified Capabilities

- 无。

## Impact

- 测试资源：新增 Coach 安全拒绝 fixture JSON。
- 测试代码：扩展 `CoachEvalFixtureLoader` 和 `CoachAgentServiceTest`。
- 后端安全门：补充 `CoachAgentService` 的本地泄题短语识别。
- 验证：运行 OpenSpec strict 校验、Coach agent 相关后端测试、后端编译和 `git diff --check`。

## Why

后端已经能把高泄题诊断和提示安全降级导出为 `safetyFixtures`，但教师端 Fixture 草稿预览仍只展示诊断草稿和课堂介入草稿。结果是安全样本已经进入响应，却没有成为老师可审查、可沉淀的工作台信号。

提示安全闭环需要让老师看到“哪些安全草稿值得进入评测资源”，否则安全事件只能停留在统计和 JSON 隐藏字段里。

## What Changes

- 在教师端 Fixture 草稿预览中展示安全草稿数量，并纳入状态徽标。
- 在预览统计中补充“提示安全”计数。
- 在预览 JSON 中输出 `safetyFixtures`，供教师人工审查风险来源、降级原因、mustNotMention 和 evidenceRefs。
- 对安全草稿生成简要风险概览，显示最高风险级别、降级原因和待审查提交。
- 保持现有导出接口、后端 DTO 和普通诊断/课堂介入草稿兼容。

## Capabilities

### New Capabilities

- `prompt-safety-fixture-draft-visibility`: 覆盖教师端如何展示提示安全 fixture 草稿，使运行时安全事件可被人工审查并沉淀到 eval 资源。

### Modified Capabilities

- 无。

## Impact

- 前端：`TeacherPage.tsx` 的 Fixture 草稿预览展示逻辑，必要的局部 CSS。
- API：不新增接口，不修改 DTO；复用已有 `DiagnosisEvalFixtureDraft.safetyFixtures` 与 `safetyFixtureCount`。
- 测试与验证：运行 OpenSpec 严格校验、前端 typecheck 和 `git diff --check`。

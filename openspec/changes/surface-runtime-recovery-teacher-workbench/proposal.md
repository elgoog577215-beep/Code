## Why

后端 `runtimeAttributionSignal` 已经能输出外部模型 recovery 状态、阻塞原因和 required checks，但教师工作台仍只展示主导失败类型、summary、transport chips 和推荐动作。这样教师无法在界面上直接判断“ModelScope 是否真的恢复、stream content chunk 是否仍缺失、下一次 smoke 要看哪些检查”。

本变更把 recovery 状态接入教师工作台，让上一轮结构化字段进入可扫描的产品视图。

## What Changes

- 前端 API 类型 `RuntimeAttributionSignal` 补齐 recovery status、blocked reasons、passed checks 和 smoke metadata。
- 教师工作台“模型归因”块展示 recovery 状态 chip、smoke case/profile、阻塞原因和 required checks。
- BLOCKED 状态使用更醒目的短标签，RECOVERED 显示恢复证据，NOT_APPLICABLE 不额外占用空间。
- 展示内容保持无密钥、无 raw response、无 provider 原始 body。
- 运行前端 typecheck、OpenSpec strict 校验、secret scan 和 diff check。

## Capabilities

### New Capabilities

- `runtime-recovery-teacher-workbench`: 约束教师工作台必须展示外部模型恢复状态和 recovery smoke 检查线索。

### Modified Capabilities

无。

## Impact

- 前端类型：`online-judge/frontend/src/shared/api/types.ts`
- 教师工作台：`online-judge/frontend/src/features/teacher/TeacherPage.tsx`
- 样式：`online-judge/frontend/src/styles.css`
- 验证：前端 typecheck、OpenSpec strict validate、secret scan、`git diff --check`

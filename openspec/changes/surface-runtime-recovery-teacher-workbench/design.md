## Context

当前教师工作台 AI 质量区域已经有“模型归因”块：

- 主导失败类别；
- summary；
- transport chips；
- recommendedAction。

上一轮后端新增了 recovery 字段，但前端类型尚未声明这些字段，UI 也没有展示。教师仍需要从长 summary 或 API 响应里猜测恢复状态。

## Goals / Non-Goals

**Goals:**

- 在模型归因块中用短 chip 展示 `recoveryStatus`。
- 当 `recoveryStatus=BLOCKED` 时展示最多两条阻塞原因和最多三条 required checks。
- 当 `recoveryStatus=RECOVERED` 时展示通过检查数量或恢复证据。
- 保持当前教师工作台的信息密度和响应式布局，不新增弹窗或独立页面。

**Non-Goals:**

- 不改变后端 recovery 判定。
- 不展示 API Key、token、header、raw prompt、raw response 或 provider 原始 body。
- 不把 recovery status 变成模型诊断质量得分。
- 不在本轮改跨作业趋势后端结构。

## Decisions

### 1. recovery 与 transport 使用同一 chip 语言

当前 transport telemetry 已用 chip 展示“通道 stream / 无内容 N / 解析异常 N”。recovery 状态也使用 chip，避免新增重卡片：

- `恢复 BLOCKED`
- `smoke submission:11`
- `profile low-latency`
- `检查 0/6`

### 2. 阻塞原因只展示前两条

blocked reasons 可能包含 runtime fallback、model not completed、missing model hit、stream content chunk missing 等多条原因。UI 默认展示前两条，避免压垮教师工作台；完整数据仍保留在 API 中。

### 3. required checks 展示为紧凑检查列表

required checks 是教师或维护者运行 recovery smoke 的验收标准。前端最多展示三条，重点让人知道恢复不是“调用成功”这么粗，而是要满足完成、无 fallback、model hit、证据和 stream chunk。

## Risks / Trade-offs

- [Risk] 信息密度过高。→ 只在 runtime attribution 存在且 recovery 不是 `NOT_APPLICABLE` 时展示，且限制数量。
- [Risk] 英文字段对教师不友好。→ 使用短中文标题，但保留 `status=MODEL_COMPLETED` 这类精确检查字符串，便于研发对照 eval。
- [Risk] 前端字段来自新增后端 DTO，老环境可能没有。→ 所有字段都设为 optional 并做空值兼容。

## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks，定义教师工作台 recovery 展示。
- [x] 1.2 运行 `openspec validate surface-runtime-recovery-teacher-workbench --strict`。

## 2. 前端展示

- [x] 2.1 扩展 `RuntimeAttributionSignal` 前端类型，兼容 recovery 字段。
- [x] 2.2 新增 recovery label/chip helper，复用教师工作台紧凑 chip 风格。
- [x] 2.3 在“模型归因”区域展示 recovery 状态、blocked reasons 和 required checks。
- [x] 2.4 补充样式，确保移动端和桌面不拥挤、不重叠。

## 3. 验证

- [x] 3.1 运行前端 typecheck。
- [x] 3.2 运行 OpenSpec strict 校验。
- [x] 3.3 运行 secret scan，确认 UI 文案不包含 API Key/token。
- [x] 3.4 运行 `git diff --check`。

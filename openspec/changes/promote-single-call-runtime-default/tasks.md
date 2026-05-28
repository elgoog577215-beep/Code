## 1. 默认模式升级

- [x] 1.1 将 `AiReportService.externalRuntimeMode` 字段默认值改为 `single-call`。
- [x] 1.2 在 `application.yml` 增加 `ai.external-runtime-mode`，默认 `single-call`。

## 2. 测试对齐

- [x] 2.1 让 staged runtime 测试显式设置 `externalRuntimeMode=staged`。
- [x] 2.2 增加默认 single-call runtime 测试，确认未配置时只调用一次模型。

## 3. 验证

- [x] 3.1 运行 OpenSpec validate。
- [x] 3.2 运行相关后端测试。

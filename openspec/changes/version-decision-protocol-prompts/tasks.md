## 1. OpenSpec 文档

- [x] 1.1 写入 proposal、design、spec 和 tasks，明确版本可观测性问题。

## 2. Prompt 版本升级

- [x] 2.1 新增 `diagnosis-judge-v2` 和 `diagnosis-and-teaching-v2` 模板版本。
- [x] 2.2 runtime 默认使用 v2 模板。
- [x] 2.3 `AiReportService` 记录 v2 promptVersion。

## 3. 测试与评测

- [x] 3.1 更新 prompt contract 测试。
- [x] 3.2 更新 runtime invocation 测试。
- [x] 3.3 运行后端 AI targeted tests。
- [x] 3.4 运行节制真实外部模型 smoke eval 并确认报告版本。

## 4. 收束

- [x] 4.1 运行 OpenSpec strict validate。
- [x] 4.2 定向提交本轮改动。

## 1. OpenSpec 文档

- [x] 1.1 写入 proposal、design、spec 和 tasks，明确低预算单次调用 runtime 范围。

## 2. Prompt 与 payload

- [x] 2.1 新增 single-call prompt 模板。
- [x] 2.2 新增 combined payload 结构。
- [x] 2.3 补充 prompt template 测试。

## 3. Runtime 接入

- [x] 3.1 新增 `ai.external-runtime-mode` 配置，默认 `staged`。
- [x] 3.2 在 `AiReportService` 中接入 single-call 路径。
- [x] 3.3 复用现有诊断与教学校验逻辑处理 combined output。

## 4. 测试与评测

- [x] 4.1 补充 single-call 成功只调用一次模型的测试。
- [x] 4.2 补充 single-call 教学提示无效但保留诊断的测试。
- [x] 4.3 运行后端 AI targeted tests。
- [x] 4.4 运行节制真实外部模型 smoke eval。

## 5. 收束

- [x] 5.1 运行 OpenSpec strict validate。
- [x] 5.2 提交本轮改动。

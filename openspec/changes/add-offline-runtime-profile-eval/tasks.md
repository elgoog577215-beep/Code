## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate add-offline-runtime-profile-eval --strict`。

## 2. 离线 Profile 评测

- [x] 2.1 新增 offline runtime profile eval report DTO。
- [x] 2.2 新增 factory，复用 `ExternalModelAgentRuntime.prepare` 生成 standard/low-latency request telemetry。
- [x] 2.3 只记录 bytes、profile、计数、ratio 和 failure reasons，不保存 raw request。
- [x] 2.4 增加 qualityPreserved/requestBytesReduced/compressionRatio 门槛。

## 3. 测试与报告

- [x] 3.1 在 `ModelDiagnosisEvalTest` 增加离线 profile eval 报告写出测试。
- [x] 3.2 验证 low-latency request 更小且 compact 标记为 true。
- [x] 3.3 验证报告 JSON 不包含 raw prompt/source/API Key 敏感标记。
- [x] 3.4 验证失败原因能覆盖未压缩或结构锚点缺失。

## 4. 验证

- [x] 4.1 运行相关后端测试。
- [x] 4.2 运行 OpenSpec strict 校验。
- [x] 4.3 运行 `git diff --check` 和密钥扫描。

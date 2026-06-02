## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate add-live-eval-quota-offline-profile-guidance --strict`。

## 2. Runtime Draft 结构

- [x] 2.1 扩展 `LiveEvalRuntimeFixtureDraft`，新增 offline profile guidance 字段。
- [x] 2.2 更新 `LiveEvalRuntimeFixtureDraftFactory`，只在 `QUOTA_LIMIT` 时填充 offline profile guidance。
- [x] 2.3 更新 quota runtime action 和 iteration suggestion，指向 offline runtime profile eval 与 required checks。
- [x] 2.4 保证非 quota runtime failure 的 offline profile guidance 为空且兼容。

## 3. 测试

- [x] 3.1 扩展 model runtime draft 测试，覆盖 quota fallback 的 offline profile guidance。
- [x] 3.2 覆盖非 quota draft 不推荐 offline profile eval。
- [x] 3.3 增加序列化安全断言，不包含 raw request、messages、sourceCode、真实 API Key 值、Bearer token 值或 `ms-` token。
- [x] 3.4 扩展 live model report 汇总测试，确认 runtime drafts 携带 offline profile guidance。

## 4. 验证

- [x] 4.1 运行相关后端测试。
- [x] 4.2 运行 OpenSpec strict 校验。
- [x] 4.3 运行 `git diff --check` 和密钥扫描。

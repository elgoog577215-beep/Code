## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate use-model-hit-for-live-baselines --strict`。

## 2. Baseline Draft Factory

- [x] 2.1 更新 `modelBaselineCandidate`，要求 modelCompleted 和 model hit。
- [x] 2.2 更新 model expected signals，输出 `modelIssueTagHit/modelFineTagHit`。
- [x] 2.3 确认 fallback-only final hit 不会生成 model baseline。

## 3. Regression Gate

- [x] 3.1 更新 model regression gate，使用 model hit 字段校验 model hit token。
- [x] 3.2 将旧 `expectedIssueTagHit/expectedFineTagHit` token 兼容映射到 model hit 字段。
- [x] 3.3 确认 fallback hit 不满足 model baseline。

## 4. 测试

- [x] 4.1 扩展 baseline factory 测试，覆盖 model hit 生成 baseline。
- [x] 4.2 扩展 baseline factory 测试，覆盖 fallback-only final hit 被跳过。
- [x] 4.3 扩展 regression gate 测试，覆盖 model hit 保持、fallback-only 回归和 legacy token 映射。

## 5. 验证

- [x] 5.1 运行相关后端测试。
- [x] 5.2 运行 OpenSpec strict 校验。
- [x] 5.3 运行 `git diff --check` 和密钥扫描。

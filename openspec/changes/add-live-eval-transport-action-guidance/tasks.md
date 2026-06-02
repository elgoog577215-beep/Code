## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate add-live-eval-transport-action-guidance --strict`。

## 2. live eval runtime action

- [x] 2.1 更新 `LiveEvalRuntimeFixtureDraftFactory`，让 model entry 的 `expectedRuntimeAction` 和 `iterationSuggestion` 消费 transport telemetry。
- [x] 2.2 对 stream no-content + quota 输出同时包含额度和 content chunk 验证的行动建议。
- [x] 2.3 对 stream invalid chunk 和 fallback retry 输出 parser/retry 调试建议。

## 3. 验证

- [x] 3.1 扩展 `AssistantLiveEvalQualityGateTest` 覆盖 transport-aware action。
- [x] 3.2 运行相关后端测试。
- [x] 3.3 运行 OpenSpec strict 校验。
- [x] 3.4 运行 `git diff --check`。

## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks，定义教师端 recovery overview 信号。
- [x] 1.2 运行 `openspec validate surface-runtime-recovery-teacher-overview --strict`。

## 2. 后端结构化信号

- [x] 2.1 扩展 `AiQualityOverviewResponse.RuntimeAttributionSignal`，新增 recovery status、checks、blocked reasons 和 smoke metadata。
- [x] 2.2 更新 `AiQualityOverviewService`，从当前作业 `aiInvocation` 推导 `RECOVERED`、`BLOCKED` 和 `NOT_APPLICABLE`。
- [x] 2.3 让 `MODEL_RUNTIME` summary/action 消费 recovery 状态，输出教师可解释的恢复验证建议。

## 3. 测试与验证

- [x] 3.1 扩展 `AiQualityOverviewServiceTest` 覆盖 recovery blocked、recovered 和 not applicable。
- [x] 3.2 运行相关后端测试。
- [x] 3.3 运行 secret scan，确认 recovery metadata 不包含 API Key/token。
- [x] 3.4 运行 `git diff --check`。

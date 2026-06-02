## 1. OpenSpec 契约

- [x] 1.1 创建 proposal、design、spec 和任务清单。
- [x] 1.2 运行 `openspec validate preserve-model-context-evidence-refs --strict`。

## 2. 诊断链证据保留

- [x] 2.1 在 `DiagnosticAgentService` 中合并模型输出 refs、rule signal refs 和 evidence package 上下文 refs。
- [x] 2.2 保持模型输出标签、fallback 状态和教学动作语义不被证据合并覆盖。
- [x] 2.3 对合并 refs 去重并限制数量。

## 3. 测试与评测

- [x] 3.1 新增或扩展无需 API Key 的结构测试，覆盖 `verdict:wrong_answer` 保留。
- [x] 3.2 重跑相关后端测试。
- [x] 3.3 重跑 assistant live eval baseline regression smoke。
- [x] 3.4 运行 OpenSpec 校验和 `git diff --check`。

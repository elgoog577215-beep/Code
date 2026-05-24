## 1. OpenSpec 文档

- [x] 1.1 写入 proposal、design、spec 和 tasks，明确外部模型预算/限流保护范围。

## 2. 失败分类与预算保护

- [x] 2.1 新增共享外部模型失败分类器。
- [x] 2.2 新增轻量外部模型预算保护器。
- [x] 2.3 补充分类器和保护器单元测试。

## 3. 接入 AI 调用链

- [x] 3.1 在 `AiReportService` 调用前后接入预算保护和统一失败分类。
- [x] 3.2 在 `CoachAgentService` 调用前后接入预算保护和统一失败分类。
- [x] 3.3 确保成长报告失败原因保留预算/限流信息。

## 4. 评测与报告

- [x] 4.1 调整 assistant live eval 的失败归因和迭代建议。
- [x] 4.2 补充预算受限场景测试。

## 5. 验证与收束

- [x] 5.1 运行 OpenSpec strict validate。
- [x] 5.2 运行后端 AI 相关 targeted tests。
- [x] 5.3 运行节制的真实外部模型 smoke eval，避免重复触发额度浪费。
- [x] 5.4 提交本轮改动。

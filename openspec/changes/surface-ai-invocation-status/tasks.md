## 1. 类型

- [x] 1.1 在前端 `SubmissionAnalysis` 类型中补充 `aiInvocation`。

## 2. 学生端展示

- [x] 2.1 在题目页提交结果指标中根据 invocation 状态展示“外部模型完成”“外部模型部分完成”“本地兜底”等短标签。
- [x] 2.2 保持学生端不展示 provider、model、promptVersion 等技术细节。

## 3. 验证

- [x] 3.1 运行前端类型检查。
- [x] 3.2 使用 mock 提交结果验证 `MODEL_RUNTIME_FALLBACK` 显示为“本地兜底”，且不显示“外部模型完成”。

## 4. 沉淀

- [x] 4.1 将“学生端不能把本地兜底包装成外部模型”的经验写入项目记忆。

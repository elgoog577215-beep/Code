## Why

当前学生题目页只有正式提交评测，学生无法用一组自定义标准输入快速运行当前代码并查看标准输出或错误。底层执行器已经支持 `stdin/stdout/stderr`，但缺少独立、安全且不污染学习证据的产品入口。

## What Changes

- 新增不落库的自定义代码运行接口，使用题目时间和内存限制执行 Python/C++17。
- 在题目页代码编辑器下方新增自定义输入、运行状态、标准输出和标准错误工作台。
- 公开练习允许匿名运行；班级作业沿用学生身份、班级和题目归属校验。
- 为昂贵执行增加输入大小、输出捕获、频率和并发限制，并避免记录代码与输入输出正文。
- 自定义运行不生成提交记录、AI 诊断、学习轨迹或教师证据。

## Capabilities

### New Capabilities

- `custom-code-run-workbench`: 学生在题目页使用单组自定义输入运行当前代码并查看非持久化结果。

### Modified Capabilities

无。

## Impact

- Backend: `execution` 运行契约、代码运行 API、访问校验与限流。
- Frontend: 题目页运行工作台、API 类型、中英文文案和响应式样式。
- Database / AI: 无表结构变化，不接入提交与 AI 链路。
- Operations: 新增 `app.code-run.*` 可配置限制和紧急开关。


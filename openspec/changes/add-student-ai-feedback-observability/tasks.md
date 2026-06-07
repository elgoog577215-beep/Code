## 1. OpenSpec 与边界

- [x] 1.1 创建 `add-student-ai-feedback-observability` proposal、design、spec、tasks。
- [x] 1.2 运行 OpenSpec strict validation，确认规格可用。

## 2. 后端观测聚合

- [x] 2.1 新增作业级学生 AI 反馈观测 DTO，包含计数、比例、耗时、失败原因和查看后影响分布。
- [x] 2.2 补充 `StudentAiFeedbackRepository` 和 `StudentAiFeedbackEventRepository` 的批量查询方法。
- [x] 2.3 新增 `StudentAiFeedbackObservabilityService`，按作业提交集合聚合反馈、事件和学习影响。
- [x] 2.4 在教师端 controller 新增只读接口 `/api/teacher/assignments/{assignmentId}/student-ai-feedback-observability`。
- [x] 2.5 增加后端聚合测试，覆盖未生成反馈进入分母、耗时 p95、失败原因和查看后影响。

## 3. 前端低噪音展示

- [x] 3.1 新增 API 类型与 client 方法。
- [x] 3.2 教师工作台加载当前作业的学生 AI 反馈观测摘要。
- [x] 3.3 在系统详情折叠区展示中文摘要，不进入教师首屏主模块，不暴露底层状态码。
- [x] 3.4 更新 browser smoke mock 与断言，覆盖观测摘要和工程术语隐藏。

## 4. 验证与收束

- [x] 4.1 运行 `openspec validate add-student-ai-feedback-observability --strict`。
- [x] 4.2 运行前端 typecheck 和 build。
- [x] 4.3 运行后端相关测试。
- [x] 4.4 运行 browser smoke。
- [x] 4.5 清理前端 build 产生的静态资源，确认未覆盖无关文件。

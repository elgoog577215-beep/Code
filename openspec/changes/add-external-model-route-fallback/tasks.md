## 1. 路由基础能力

- [x] 1.1 新增统一 `ExternalModelRoute`，封装 provider/baseUrl/apiKey/model 与 endpoint 计算。
- [x] 1.2 在 `AiReportService` 增加主路由和备用路由配置字段。
- [x] 1.3 在 `CoachAgentService` 增加主路由和备用路由配置字段。

## 2. 调用链路升级

- [x] 2.1 让提交诊断和成长报告按路由顺序尝试外部模型。
- [x] 2.2 让 Coach 追问按路由顺序尝试外部模型。
- [x] 2.3 保持预算门禁按 provider/model 隔离，主路由 guard 不阻断备用路由。
- [x] 2.4 让提交诊断成功元数据记录实际成功路由。

## 3. Live Eval 对齐

- [x] 3.1 让 live eval 构造的 `AiReportService` 注入 `AI_EVAL_FALLBACK_*`。
- [x] 3.2 让 live eval 构造的 `CoachAgentService` 注入 `AI_EVAL_FALLBACK_*`。

## 4. 验证

- [x] 4.1 增加主路由失败后切换备用路由的单元测试。
- [x] 4.2 增加主路由 guard 打开时跳过主路由的单元测试。
- [x] 4.3 运行 OpenSpec validate 和相关后端测试。

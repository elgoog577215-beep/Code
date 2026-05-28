## 1. 报告结构

- [x] 1.1 在 `AssistantLiveEvalReport.Entry` 中新增 `actualProvider`、`actualModel`、`routeRole` 字段，保持旧字段兼容。
- [x] 1.2 更新 live eval 报告 UTF-8 回读测试，覆盖新增字段。

## 2. 诊断归因

- [x] 2.1 诊断 entry 从 `AiInvocation` 提取实际 provider/model。
- [x] 2.2 实现 route role 推断，区分 `PRIMARY`、`FALLBACK`、`LOCAL_FALLBACK` 和 `UNKNOWN`。

## 3. 其他助手归因

- [x] 3.1 为 Coach 和成长报告 entry 补充保守 route attribution，不伪造精确调用链。
- [x] 3.2 确保 runtime fallback 不会因为归因字段被计为 `completedOutput=true`。

## 4. 验证与沉淀

- [x] 4.1 增加或更新单测覆盖主路由、备用路由、本地兜底和 JSON 回读。
- [x] 4.2 增加 route outcome 聚合，让报告按路由展示完成率、失败率和失败原因。
- [x] 4.3 运行 OpenSpec 严格校验和相关 Maven 测试。
- [x] 4.4 将路由归因对后续评测闭环的价值写入项目记忆。
- [x] 4.5 让外部路由失败后的本地兜底保留失败 provider/model，避免容量失败全部归到本地规则。

## 1. 规格与配置

- [x] 1.1 新增 OpenSpec 规格，描述路由池配置、顺序、门禁和兼容行为。
- [x] 1.2 在 `application.yml` 中新增可选 `ai.routes` 配置。

## 2. 路由池实现

- [x] 2.1 为 `ExternalModelRoute` 增加额外路由池解析能力。
- [x] 2.2 让 `AiReportService` 的路由枚举支持主路由、fallback 和额外路由池。
- [x] 2.3 让 `CoachAgentService` 使用同样的路由池逻辑。

## 3. 测试与验证

- [x] 3.1 补充诊断服务测试：主路由和 fallback 均失败时可以使用第三条路由。
- [x] 3.2 补充 Coach 测试：guarded primary、失败 fallback 后能命中额外路由。
- [x] 3.3 覆盖非法路由池条目不会破坏现有主路由。
- [x] 3.4 live eval 构造服务时注入 `AI_EVAL_ROUTES`，并在报告中展示 route pool 配置。
- [x] 3.5 route attribution 能把额外路由池输出标记为 `ROUTE_POOL`。
- [x] 3.6 运行 OpenSpec 严格校验、相关 Maven 测试和必要的 live eval smoke。

## 4. 沉淀

- [x] 4.1 将本轮“完成率瓶颈优先做路由容量，而不是继续改提示词”的判断写入项目记忆。

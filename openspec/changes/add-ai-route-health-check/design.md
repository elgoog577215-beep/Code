# 设计：AI 路由健康预检

## 接入位置

新增系统接口：

```text
GET /api/system/ai-route-health
```

它属于部署健康检查，不属于教师 AI 质量统计。教师质量统计关注历史诊断质量；路由健康关注当前配置是否能支撑外部模型调用。

教师管理页的“系统状态”区域读取该接口，把判题执行环境和外部 AI 路由分开展示。这样学校部署时可以明确看到：评测机可用不等于外部大模型可用，本地兜底也不能被误认为外部模型效果。

## 输出字段

`AiRouteHealthResponse` 包含：

- `enabled`
- `configuredRouteCount`
- `usableRouteCount`
- `fallbackConfigured`
- `routePoolConfigured`
- `healthLevel`: `DISABLED`、`NO_ROUTE`、`SINGLE_ROUTE_RISK`、`MULTI_ROUTE_READY`
- `summary`
- `suggestions`
- `routes`

每条 route 只展示：

- `role`: `PRIMARY`、`FALLBACK`、`ROUTE_POOL`
- `provider`
- `baseUrl`
- `model`
- `configured`
- `missingFields`

## 安全策略

- 不输出 `apiKey`。
- `baseUrl` 只做 URL 参数级脱敏，隐藏 `key/token/api_key` 等查询参数。
- 如果某条路由缺 key，只输出 `missingFields=["apiKey"]`，不输出实际值。

## 与 live eval 的关系

健康预检回答“当前配置是否具备多路由容量”；live eval 回答“真实外部模型在样本上是否完成且质量达标”。两者互补，不能互相替代。

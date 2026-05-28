# 设计：外部模型路由池

## 现状判断

当前链路已经具备：

- 主路由配置：`ai.provider/base-url/api-key/model`。
- 一个备用路由：`ai.fallback.provider/base-url/api-key/model`。
- 按 provider/model 隔离的预算门禁。
- live eval 的 route attribution 与 route outcome 汇总。

短板是只能配置一个 fallback。真实学校部署中，如果一个模型供应商额度耗尽，系统需要能继续尝试后续 OpenAI-compatible 路由。

## 配置设计

新增可选配置：

```yaml
ai:
  routes: ${AI_ROUTES:}
```

格式使用环境变量友好的分号分隔：

```text
provider|baseUrl|apiKey|model;provider2|baseUrl2|apiKey2|model2
```

约束：

- `provider` 可空；为空时使用调用方默认 provider 做观测兜底。
- `baseUrl`、`apiKey`、`model` 必须非空，否则该条路由跳过。
- 路由池只追加在主路由和 `ai.fallback.*` 之后，保证兼容已有优先级。

## 调用策略

现有 `configuredRoutes()` 从：

```text
primary -> fallback
```

升级为：

```text
primary -> fallback -> additional routes
```

每条路由仍走同一套流程：

1. 检查该 provider/model 的预算门禁。
2. 如果 guard 打开，跳过该路由，继续下一条。
3. 调用成功后记录成功并清除该路由 guard。
4. 调用失败后按已有失败分类记录 guard。
5. 所有外部路由失败后，才进入本地规则兜底。

## 兼容策略

- 未配置 `ai.routes` 时，行为与当前版本完全一致。
- `ai.fallback.*` 继续保留，避免破坏现有部署。
- 额外路由只是路由枚举来源变化，不改变诊断、Coach、安全校验和 report 结构。

## 风险

- 路由池配置字符串写错时可能静默跳过。测试应覆盖非法条目不会破坏已有主路由。
- 多路由可能增加总延迟。现阶段目标是提高完成率；后续再做按健康度排序或快速熔断。

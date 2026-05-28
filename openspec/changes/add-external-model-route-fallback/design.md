# 设计：外部模型备用路由

## 问题判断

当前瓶颈不是“模型已完成输出但分析差”，而是“外部调用没有稳定完成”。因此本轮应优先提升路由韧性，而不是继续微调提示词。

## 路由模型

新增统一的 `ExternalModelRoute`，表达一个 OpenAI-compatible 调用路由：

- `provider`
- `baseUrl`
- `apiKey`
- `model`

主路由继续使用现有 `ai.base-url`、`ai.api-key`、`ai.model`，新增 `ai.provider` 只用于观测和预算门禁，默认 `ModelScope`。

备用路由使用：

- `ai.fallback.provider`
- `ai.fallback.base-url`
- `ai.fallback.api-key`
- `ai.fallback.model`

只有备用路由的 baseUrl、apiKey、model 全部非空时才启用备用路由。

## 调用策略

每次外部模型调用按配置顺序尝试：

1. 检查当前路由的 `ExternalModelBudgetGuard`。
2. 如果 guard 打开，跳过当前路由，继续下一个路由。
3. 如果调用成功，记录该 provider/model 成功，并清除该路由 guard。
4. 如果调用失败，按现有 `ExternalModelFailureClassifier` 分类；quota、限流等失败继续记录 guard。
5. 如果还有备用路由，继续尝试；全部失败后抛出最后一个失败原因。

这样做不会改变提示词协议，也不会让本地 fallback 假装成模型输出。

## 元数据策略

提交诊断成功时，`AiInvocation.provider/model/modelVersion` 应记录实际最后成功的外部路由。若完全失败进入规则 fallback，则保留原有 fallback 状态和失败归因。

本轮不做多路由融合元数据。若 staged runtime 的诊断阶段和教学阶段分别命中不同路由，最终记录最后成功路由，后续可再扩展为 route trace。

## 评测策略

新增单元测试覆盖：

- 主路由 quota 后使用备用路由。
- 主路由 guard 打开时跳过主路由并使用备用路由。
- Coach 追问同样支持备用路由。
- live eval 构造服务时把 `AI_EVAL_FALLBACK_*` 注入真实服务字段。

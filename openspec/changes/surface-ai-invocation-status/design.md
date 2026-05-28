# 设计：学生端 AI 来源标识

## 展示位置

在题目页提交结果的“分析”指标中展示来源状态。该位置已经是学生判断反馈是否生成的入口，适合承载短标签。

## 映射规则

- `analysis` 为空：显示“观察中”。
- 缺少 `aiInvocation` 且 `sourceType` 包含 `RULE`：显示“本地规则”。
- `aiInvocation.fallbackUsed=true`、`status=MODEL_RUNTIME_FALLBACK` 或 `provider=LOCAL_RULES`：显示“本地兜底”。
- `status=MODEL_PARTIAL_COMPLETED`：显示“外部模型部分完成”。
- `status=MODEL_COMPLETED`：显示“外部模型完成”。
- 其他已生成状态：显示“分析已生成”或“已生成”。

## 边界

学生端只展示短状态，不展示 provider、model、promptVersion 等技术细节。详细路由和容量判断留给教师管理页、live eval 报告和后端日志。

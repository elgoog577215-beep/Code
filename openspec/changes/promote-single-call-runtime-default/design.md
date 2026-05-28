# 设计：默认单次调用

## 判断

教育 agent 的在线效果首先受“能否稳定完成外部模型调用”约束。staged runtime 的诊断阶段和教学阶段分开，结构更清楚，但每条提交至少两次请求；在额度或限流环境下，它比 single-call 更容易触发运行失败。

single-call 已经具备：

- 统一输出 `diagnosisDecision` 和 `teachingHint`。
- 通过 `ModelOutputValidator` 校验主错因、细粒度标签、证据引用和安全风险。
- 通过 partial/fallback 路径保护学生端安全。

因此本轮将默认值切到 single-call，staged 作为可配置回退路径保留。

## 配置

`AiReportService.externalRuntimeMode` 默认值改为 `single-call`。

`application.yml` 新增：

```yaml
ai:
  external-runtime-mode: ${AI_EXTERNAL_RUNTIME_MODE:single-call}
```

如需旧行为，部署时设置：

```text
AI_EXTERNAL_RUNTIME_MODE=staged
```

## 测试

- 旧 staged 测试显式设置 `externalRuntimeMode=staged`。
- 新增默认 single-call 测试，确认未显式配置时只调用一次模型并返回完成结果。

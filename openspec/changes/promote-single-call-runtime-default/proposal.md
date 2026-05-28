# 将外部诊断默认升级为单次调用模式

## 背景

当前真实 live eval 显示：已完成的外部模型输出在诊断信号、证据、安全和教学动作上可以达标，但主路由额度和运行失败率会迅速拉低整体完成率。继续使用 staged 默认模式会让每条诊断至少需要两次模型调用，放大额度、限流、超时和模型不稳定风险。

live eval 已默认使用 `single-call` 降低请求次数，但生产服务字段默认仍是 `staged`。这会造成评测优化和真实线上体验不一致。

## 目标

- 将提交诊断外部 runtime 默认改为 `single-call`。
- 保留通过 `AI_EXTERNAL_RUNTIME_MODE=staged` 显式切回 staged 的能力。
- 更新测试，避免旧测试隐式依赖默认 staged。
- 继续保证 single-call 输出经过原有诊断 validator 和教学安全门。

## 非目标

- 不删除 staged runtime。
- 不改变提示词协议或标准库。
- 不引入多模型投票。

## 影响范围

- `AiReportService` 外部 runtime 默认值。
- `application.yml` AI 配置。
- 外部 runtime 相关单元测试。

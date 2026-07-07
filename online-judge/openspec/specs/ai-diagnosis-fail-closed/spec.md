# ai-diagnosis-fail-closed Specification

## Purpose
TBD - created by archiving change enforce-external-ai-fail-closed. Update Purpose after archive.
## Requirements
### Requirement: AI 内容必须由外部模型生产
系统 SHALL 将外部模型作为 AI 诊断、AI 成长报告和 Coach AI 追问的唯一内容生产者；后端 MAY 组装上下文、证据、标准库候选和校验结果，但 MUST NOT 在模型失败时生成替代性诊断、追问、成长总结或建议内容。

#### Scenario: 外部模型失败
- **WHEN** 外部模型认证、额度、超时、服务异常、输出为空、输出结构无效或安全校验失败
- **THEN** 系统 SHALL 返回 `MODEL_FAILED`、`FAILED` 或 `AI_UNAVAILABLE` 等明确失败状态
- **AND** 学生或教师可见内容 SHALL 表达 AI 暂不可用
- **AND** 系统 MUST NOT 返回本地规则诊断、本地规则追问或本地成长报告作为 AI 结果

#### Scenario: 后端准备上下文
- **WHEN** 系统准备调用外部模型
- **THEN** 后端 MAY 构造完整源码、判题事实、证据包、标准库候选包和学习轨迹摘要
- **AND** 这些材料 SHALL 只作为模型输入或校验依据
- **AND** MUST NOT 被包装成学生可见 AI 结论

### Requirement: AI 诊断上下文不得主动压缩为低延迟模式
系统 SHALL 使用标准高质量上下文调用外部诊断模型；生产链路 MUST NOT 暴露低延迟、自动压缩或模型失败降级配置来改变该原则。

#### Scenario: 查看运行配置
- **WHEN** 查看主配置、环境示例或 readiness 预检
- **THEN** 系统 SHALL 不暴露模型失败降级开关
- **AND** AI 诊断运行 profile SHALL 为 `standard`
- **AND** 新诊断请求 SHALL 标记 `requestCompact=false`

#### Scenario: 模型输出过长或结构不完整
- **WHEN** 外部模型输出被截断、缺字段或结构化数组不完整
- **THEN** 系统 SHALL 记录失败原因
- **AND** MUST NOT 使用本地摘要、第一条建议或规则模板补齐为可见 AI 结果


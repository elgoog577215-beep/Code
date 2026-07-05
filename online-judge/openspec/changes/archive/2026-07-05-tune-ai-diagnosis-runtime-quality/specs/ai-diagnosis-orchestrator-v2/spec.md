## ADDED Requirements

### Requirement: 默认诊断模型应使用已验证可用模型
系统 SHALL 将默认外部诊断模型配置为最近真实链路 smoke test 已验证可用的模型；部署环境 MAY 通过环境变量覆盖默认模型。

#### Scenario: 未配置模型环境变量
- **WHEN** `OJ_AI_MODEL` 和 `AI_MODEL` 均未配置
- **THEN** 系统 SHALL 默认使用 `Qwen/Qwen3-235B-A22B-Instruct-2507`

#### Scenario: 环境变量覆盖模型
- **WHEN** 部署环境显式配置 `OJ_AI_MODEL` 或 `AI_MODEL`
- **THEN** 系统 SHALL 使用环境变量指定的模型

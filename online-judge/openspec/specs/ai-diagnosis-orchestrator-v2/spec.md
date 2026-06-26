# ai-diagnosis-orchestrator-v2 Specification

## Purpose
TBD - created by archiving change simplify-ai-diagnosis-single-agent. Update Purpose after archive.
## Requirements
### Requirement: 诊断编排默认跳过外部搜索定位 Agent
系统 SHALL 默认使用本地召回候选进入正式诊断报告阶段；外部搜索定位模型调用 SHALL 仅作为显式开启的对照或调试路径。

#### Scenario: 搜索定位开关关闭
- **WHEN** 搜索定位外部模型调用未显式开启
- **THEN** 系统 SHALL 不调用 `search-location-v1`，并 SHALL 继续使用本地候选或原标准库包调用正式诊断报告阶段

#### Scenario: 本地候选为空
- **WHEN** 本地召回没有返回候选条目
- **THEN** 系统 SHALL 使用兼容的默认标准库包进入单诊断 Agent，不回退到旧双 Agent 链路

#### Scenario: 显式开启搜索定位对照
- **WHEN** 配置显式开启外部搜索定位模型调用
- **THEN** 系统 MAY 调用 `search-location-v1` 作为对照路径，并 MUST 在 trace 中记录该阶段状态


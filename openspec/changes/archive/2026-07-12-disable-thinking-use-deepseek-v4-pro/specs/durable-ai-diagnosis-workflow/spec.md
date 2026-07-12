## ADDED Requirements

### Requirement: 持久化阶段必须记录推理模式策略下的实际模型
系统 SHALL 在统一的可配置推理模式策略下继续记录每个阶段的实际模型、prompt 版本、输出、耗时、尝试次数和失败原因；生产默认关闭推理，显式切换推理模式或模型池 MUST NOT 改变 Run 的问题清单或跳过必需阶段。

#### Scenario: 主模型完成阶段
- **WHEN** V4 Pro 非推理请求成功完成某个阶段
- **THEN** 阶段记录 SHALL 保存 V4 Pro 为实际模型
- **AND** 成功输出 SHALL 可在恢复时直接复用

#### Scenario: 阶段切换备用模型
- **WHEN** V4 Pro 发生可切换错误且备用模型完成该阶段
- **THEN** 阶段记录 SHALL 保存最终实际模型和完整输出
- **AND** Run SHALL 继续满足完整路径完成条件

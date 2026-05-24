## Why

外部大模型在线调用已经具备 staged 和 single-call 两条路径，但真实模型输出仍可能因为轻微格式偏差被直接判为无效，例如把标准标签输出成中文标签、大小写不一致，或引用了等价但格式略有差异的证据 ID。  
这类问题不属于模型完全失败，也不应该被直接降级为本地兜底；需要在严格校验前增加一个可解释、可控、可回退的标准化层，提高最终可用输出率。

## What Changes

- 新增外部模型输出标准化能力，在校验前处理可证明等价的标签、教学动作和证据引用。
- 标准化范围只包含确定性修复：标签 ID 大小写、标准库中文标签到 ID 的映射、教学动作大小写、证据引用大小写或首尾空白。
- 继续保留严格校验：无法证明等价的标签、证据和高泄漏风险内容仍然失败或进入安全兜底。
- staged runtime 和 single-call runtime 共用同一标准化逻辑。
- 增加测试覆盖：中文标签可标准化、轻微证据引用可标准化、不可证明标签仍失败、安全风险不被标准化绕过。

## Capabilities

### New Capabilities

- `external-model-output-normalization`: 定义外部模型输出在进入校验前的确定性标准化、边界和验证要求。

### Modified Capabilities

- 无。

## Impact

- 影响后端 AI runtime：`AiReportService`、`ExternalModelAgentRuntime`、模型输出 payload 与校验链路。
- 新增一个小型标准化组件及单元测试。
- 不改变前端接口字段，不改变现有 staged 默认模式，不改变安全兜底策略。

## Why

真实 ModelScope live eval 现在已经能在评测报告中输出 `recoveryStatus=BLOCKED`、stream content chunk 缺失和 rate limit 阻塞原因，但教师端 AI 质量概览仍只能看到泛化的运行归因。外部模型恢复验证如果只停留在开发报告里，教师和维护者很难判断当前作业是否真正重新使用了外部模型。

本变更把 recovery smoke 的结果语义推进到教师端运行归因信号：当作业样本出现外部模型 fallback、rate limit、预算保护、stream 无内容或 provider 失败时，系统要明确说明恢复验证是 `BLOCKED`、`RECOVERED` 还是 `NOT_APPLICABLE`，并给出可验证的 required checks 和阻塞原因。

## What Changes

- AI 质量概览的 `runtimeAttributionSignal` 新增 recovery 状态字段，包含恢复状态、检查数、通过检查、阻塞原因、是否建议 recovery smoke、建议 case/profile 和 required checks。
- `AiQualityOverviewService` 从当前作业 `SubmissionAnalysis.reportJson.aiInvocation` 推导 recovery 状态，不读取 live eval JSON 文件作为生产数据源。
- 当存在运行失败或部分完成样本且没有任何真实外部模型完成样本满足恢复检查时，输出 `recoveryStatus=BLOCKED`。
- 当同一作业已有至少一个真实外部模型完成、未 fallback、保留证据、命中错因标签且 stream 有 content chunk 的样本时，输出 `recoveryStatus=RECOVERED`。
- `MODEL_RUNTIME` 质量维度和推荐动作补充 recovery 状态，让教师能看到“模型仍阻塞”还是“已有恢复证据”。
- 补充后端测试和 OpenSpec 校验，覆盖 BLOCKED、RECOVERED 和 NOT_APPLICABLE。

## Capabilities

### New Capabilities

- `runtime-recovery-teacher-overview`: 约束教师端 AI 质量概览必须暴露外部模型恢复验证状态和阻塞原因。

### Modified Capabilities

无。

## Impact

- 后端 DTO：`AiQualityOverviewResponse.RuntimeAttributionSignal`
- 后端服务：`AiQualityOverviewService`
- 后端测试：`AiQualityOverviewServiceTest`
- OpenSpec：新增 `runtime-recovery-teacher-overview` 能力契约
- 验证：OpenSpec strict validate、AI 质量概览相关后端测试、secret scan、`git diff --check`

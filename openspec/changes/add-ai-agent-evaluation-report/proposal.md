## Why

当前 live eval 已能区分外部模型完成、运行失败和本地兜底，但报告还不能直接回答学校上线最关心的四个问题：AI 准不准、快不快、稳不稳、是否真的有教育价值。

本变更把这些问题沉淀成可复跑、可比较、可门禁的 AI agent 能力画像，避免后续只凭单条样例或主观观感判断外部大模型效果。

## What Changes

- 在 live eval 报告中新增 `evaluationProfile`，按准确率、速度、稳定性、教育有效性四组维度汇总指标。
- 准确率维度继续只统计 `completedOutput=true` 的外部模型输出，避免本地 fallback 污染模型质量。
- 速度维度统计 completed 输出和全部请求的平均延迟、P50、P90、P95、最大延迟，并标记慢样本。
- 稳定性维度统计 completedOutput 率、runtime failure 率、fallback 率、schema/输出有效率、route failure 概况。
- 教育有效性维度统计教学动作有效率、证据引用有效率、安全通过率，并显式记录当前还不能由 live eval 直接证明的学生后续改善指标。
- 扩展质量门，让报告可以同时暴露准确率、速度、稳定性和教育有效性上的具体未达标项。

## Capabilities

### New Capabilities

- `ai-agent-evaluation-report`: 定义 AI agent 评估报告必须包含的准确率、速度、稳定性、教育有效性指标，以及 fallback 统计边界。

### Modified Capabilities

- `external-ai-assistant-eval-loop`: live eval 输出从单纯目标快照扩展为四维能力画像，并让质量门可以基于能力画像判断是否达标。

## Impact

- 影响 `AssistantLiveEvalReport`、`AssistantLiveEvalTest`、`AssistantLiveEvalQualityGate` 及其测试。
- 影响 OpenSpec 评测文档和项目 AI 经验沉淀文档。
- 不改生产 AI 调用链，不新增前端页面，不改变外部模型 provider 配置。

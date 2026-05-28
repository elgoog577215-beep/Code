## Context

现有 live eval 已经能跑外部模型、写 JSON 报告、区分 `completedOutput`、runtime failure、fallback 和 route role。它解决了“是否真的调用外部模型”的问题，但还没有把学校真实上线要看的指标组织成一套稳定画像。

本轮设计目标是沿用已有 `AssistantLiveEvalReport`，增加结构化 `evaluationProfile`，让报告天然按准确率、速度、稳定性、教育有效性四个角度输出，而不是让人从原始 entries 里手工推断。

## Goals / Non-Goals

**Goals:**

- 报告直接展示准确率、速度、稳定性、教育有效性四组指标。
- 所有质量指标继续只统计 `completedOutput=true`，避免 fallback 计入外部模型能力。
- 速度指标使用可解释的延迟分位数：平均、P50、P90、P95、最大值。
- 稳定性指标能区分模型未完成、运行失败、本地兜底、路由失败。
- 质量门能输出具体失败项，例如准确率不足、P95 太慢、runtime failure 过高。

**Non-Goals:**

- 不改变生产 AI 调用链。
- 不新增模型供应商或路由配置。
- 不用本轮报告证明学生真实成绩提升；真实学习效果需要后续结合连续提交和班级数据。
- 不新建独立评测框架，避免和现有 live eval 形成两套口径。

## Decisions

### Decision 1: 在 `AssistantLiveEvalReport` 内新增 `evaluationProfile`

选择在现有报告类里新增字段，而不是创建新的报告类型。原因是已有测试、写文件、质量门和 OpenSpec 都围绕 `AssistantLiveEvalReport`，继续扩展可以保持兼容，并减少后续维护成本。

替代方案是新增 `AiAgentEvaluationReport`。这个方案命名更清晰，但会导致同一批 live eval 数据被拆成两份报告，后续容易出现指标口径漂移。

### Decision 2: 指标分四组，但共享同一批 entry

准确率来自信号命中、证据有效、教学动作有效、安全通过；速度来自 `latencyMs`；稳定性来自 `status/fallbackUsed/completedOutput/failureReason/routeRole`；教育有效性先使用代理指标，例如教学动作有效率、安全通过率和证据质量。

这样可以先把可验证指标落地，同时明确哪些指标只是代理指标，哪些指标需要未来接入学生下一次提交结果。

### Decision 3: 质量门优先读取 `evaluationProfile`

如果报告有 `evaluationProfile`，质量门使用其中的指标；如果没有，则回退到现有 `goalSnapshot` 或基础字段。这样旧报告仍可被读取，新报告可以表达更完整维度。

### Decision 4: 慢样本不等于质量失败，但必须显式暴露

速度慢不会直接说明模型错，但会影响课堂体验。因此速度门禁作为独立维度输出，不混入准确率。P95 和最大延迟用于定位长尾问题，平均延迟只作为辅助指标。

## Risks / Trade-offs

- [Risk] 指标太多导致报告难读。
  Mitigation: 使用 `evaluationProfile` 分组，每组只保留第一阶段最关键指标。

- [Risk] 教育有效性被误解为真实学习效果。
  Mitigation: 在报告中显式记录 `studentImprovementMeasured=false`，说明当前只是提示质量代理指标。

- [Risk] 小样本 smoke 被误解为整体达标。
  Mitigation: 保留已有 `sampleProfile/coverageGaps`，并在能力画像中记录样本数。

- [Risk] 速度门槛受外部供应商波动影响。
  Mitigation: 报告同时输出 route role 和 failure reason，先定位容量/网络问题，再判断 prompt 或模型质量。

## Migration Plan

1. 扩展 `AssistantLiveEvalReport`，新增四维 `EvaluationProfile`。
2. 在 `AssistantLiveEvalTest` 汇总阶段计算能力画像。
3. 更新 `AssistantLiveEvalQualityGate`，支持 profile 优先、旧字段兜底。
4. 增加单元测试覆盖准确率、速度、稳定性、教育有效性门禁。
5. 更新 OpenSpec 和项目记忆文档。

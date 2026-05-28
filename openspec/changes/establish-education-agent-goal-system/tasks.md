## 1. 目标体系文档

- [x] 1.1 新建 OpenSpec proposal，明确北极星目标、阶段目标和本轮影响范围。
- [x] 1.2 新建设计文档，说明目标分层、live eval 目标快照、fallback 统计边界和长代码样本底线。
- [x] 1.3 新增 `education-agent-goal-system` spec，定义目标层级、质量指标、目标缺口和后续阶段边界。
- [x] 1.4 修改 `external-ai-assistant-eval-loop` spec，要求 live eval 报告输出目标快照并防止 fallback 计入质量。

## 2. Live Eval 目标快照

- [x] 2.1 扩展 `AssistantLiveEvalReport`，新增目标快照字段和目标缺口字段。
- [x] 2.2 在 `AssistantLiveEvalTest` 汇总阶段计算完成率、失败率、质量率、安全率、教学动作率和目标缺口。
- [x] 2.3 支持通过环境变量覆盖第一阶段目标阈值，默认使用当前目标体系。
- [x] 2.4 保持 fallback 输出可读，但所有模型质量字段只统计 `completedOutput=true`。

## 3. 测试与质量门槛

- [x] 3.1 更新 `AssistantLiveEvalQualityGate`，使用目标快照或等价指标判断质量门槛。
- [x] 3.2 增加目标快照测试，覆盖目标达成、目标缺口、fallback 不计质量。
- [x] 3.3 保留并强化 20 行以上诊断 fixture 门槛测试。

## 4. 经验沉淀与验证

- [x] 4.1 更新 `docs/ai-memory/项目决策.md`，沉淀教育 agent 目标体系和执行边界。
- [x] 4.2 运行 OpenSpec validate。
- [x] 4.3 运行相关后端定向测试。
- [x] 4.4 如外部 key 可用，运行长代码 live eval smoke，并记录模型完成率与目标缺口。

## 5. 完成率优先打磨

- [x] 5.1 将 live eval 默认外部诊断 runtime 改为 `single-call`，降低每条提交的模型请求次数。
- [x] 5.2 在 live eval 报告中记录 `runtimeMode`，方便对比 staged 与 single-call 的完成率。
- [x] 5.3 更新 OpenSpec design/spec，说明生产默认不变、评测默认低预算、可显式切回 staged。
- [x] 5.4 运行 3 条 20 行以上长代码 single-call live eval smoke，确认完成率、信号命中、证据、安全和教学动作均达标。
- [x] 5.5 在 live eval 报告中记录 `sampleProfile` 与 `coverageGaps`，防止小样本 smoke 被误读为整体质量达标。
- [x] 5.6 修正质量率分母：完成率和运行失败率统计全部请求样本，信号、证据、教学动作质量率只统计已完成模型输出。
- [x] 5.7 让 live eval 共享 `ExternalModelBudgetGuard`，并在报告中输出 `failureReasonCounts`，避免 quota/限流后继续无效调用。
- [x] 5.8 在 `goalSnapshot` 中输出 `nextOptimizationFocus` 和 `nextAction`，让报告自动指向当前最大瓶颈。
- [x] 5.9 在 live eval 报告中输出 `routeProfile`，容量失败且未配置备用路由时将下一步定位为 `MODEL_ROUTE_CONFIGURATION`。

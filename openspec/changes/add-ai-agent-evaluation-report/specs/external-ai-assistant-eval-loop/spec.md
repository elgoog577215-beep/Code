## MODIFIED Requirements

### Requirement: Unified External Assistant Evaluation

系统 SHALL 提供不经过前端的后端评测路径，用于真实调用外部模型驱动的 AI assistant，并在报告中包含第一阶段教育 agent 目标指标和四维 agent 评估指标。

#### Scenario: Fixture structure is validated without external model access

- **WHEN** evaluator 在没有 `AI_EVAL_API_KEY` 的情况下运行
- **THEN** 它校验 assistant eval fixture 的 schema、老师期望、rubric、required signals、forbidden phrases 和长代码诊断样本底线
- **AND** 它不调用任何外部模型

#### Scenario: Live smoke evaluation runs with external model access

- **WHEN** 提供 `AI_EVAL_API_KEY`
- **THEN** evaluator 通过项目 AI 服务调用配置的外部模型
- **AND** 它在 `target/ai-eval-reports` 下写入逐样本报告
- **AND** 它把运行失败与教学质量未命中分开记录
- **AND** 它包含 sample profile，记录 assistant type、case id 和长代码诊断覆盖情况
- **AND** 它包含 route profile，记录主路由和 fallback 路由配置状态
- **AND** 它包含 quota、rate limit、budget guard、timeout、unsupported model、invalid output 和其他运行失败的原因计数
- **AND** 它包含 goal snapshot，记录完成率、失败率、质量、安全、教学动作、目标缺口、覆盖缺口、下一焦点和下一动作
- **AND** 它包含 evaluation profile，覆盖准确率、速度、稳定性和教育有效性

#### Scenario: Live smoke evaluation defaults to low-budget runtime

- **WHEN** 提供 `AI_EVAL_API_KEY`
- **AND** 没有提供 `AI_EVAL_EXTERNAL_RUNTIME_MODE`
- **THEN** submission diagnosis live eval 使用 `single-call` runtime mode
- **AND** 报告记录 `runtimeMode=single-call`
- **AND** 开发者仍可设置 `AI_EVAL_EXTERNAL_RUNTIME_MODE=staged` 调试 staged 诊断路径

#### Scenario: Four-dimension report is generated from the same entries

- **WHEN** live eval entries 被汇总
- **THEN** 准确率、速度、稳定性和教育有效性指标从同一份 entry list 中计算
- **AND** 报告保留原始 entries，让开发者能从每个聚合指标追溯到具体样本

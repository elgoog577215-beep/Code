## 1. OpenSpec

- [x] 1.1 写入 proposal、design、spec 和 tasks，明确推荐学习闭环范围。
- [x] 1.2 运行 OpenSpec strict validate。

## 2. 推荐学习信号

- [x] 2.1 扩展推荐 DTO 和事件实体，新增学习假设、完成信号、策略、风险等级和 fallback 动作字段。
- [x] 2.2 强化 StudentRecommendationService，让推荐项生成结构化学习闭环字段，并在历史同类失败时优先降级复盘。
- [x] 2.3 强化 StudentRecommendationEventService，让曝光、点击、进入题目和提交事件沉淀学习闭环字段。

## 3. 效果反馈闭环

- [x] 3.1 扩展 RecommendationEffectivenessResponse，新增策略分段、未完成学习信号和行动型反馈。
- [x] 3.2 强化 RecommendationEffectivenessService，按策略聚合效果并输出教师介入/降级建议。
- [x] 3.3 更新前端共享类型。
- [x] 3.4 将推荐学习闭环接入 AI 质量概览，新增推荐闭环质量维度和优先级信号。
- [x] 3.5 为推荐事件补充作业维度，让推荐效果和 AI 质量概览支持按作业过滤。

## 4. 测试与验证

- [x] 4.1 补充推荐生成、事件沉淀和推荐效果概览测试。
- [x] 4.2 运行相关后端测试、前端类型检查、OpenSpec strict validate 和 diff check。

## 1. OpenSpec

- [x] 1.1 写入 `add-teaching-action-orchestration-loop` proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate add-teaching-action-orchestration-loop --strict`。

## 2. 后端编排信号

- [x] 2.1 新增 `TeachingActionDecision` DTO 字段，保持旧响应兼容。
- [x] 2.2 实现确定性 `TeachingActionOrchestrator`，把既有 AI 教育信号转成候选动作并排序。
- [x] 2.3 在学生能力画像中输出长期 `teachingActionDecision`。
- [x] 2.4 在学习轨迹中输出当前作业 `teachingActionDecision` 并统一覆盖高优先级 `nextStep` / `attentionReason`。
- [x] 2.5 在教师作业概览中输出教学动作风险学生数、班级摘要和学生行决策。

## 3. 推荐与质量闭环

- [x] 3.1 让推荐服务消费教学动作决策，生成 `TEACHING_ACTION_*` 推荐并避免同类高风险重复。
- [x] 3.2 在 AI 质量概览中新增 `TEACHING_ACTION_ORCHESTRATION_LOOP` 维度和改进优先级。
- [x] 3.3 确保现有单次诊断、Coach、推荐、教师介入、迁移、复发误区、自解释、AI 依赖和长期成长字段不回退。

## 4. 前端展示

- [x] 4.1 更新共享 API 类型和教学动作状态标签格式化。
- [x] 4.2 学生端展示最高优先级教学动作，避免覆盖当前题反馈主流程。
- [x] 4.3 教师端展示教学动作摘要和学生行决策，AI 质量详情保持折叠。

## 5. 验证

- [x] 5.1 增加后端单元测试覆盖候选动作排序、学生画像、学习轨迹、推荐消费、教师概览和质量维度。
- [x] 5.2 运行相关 Maven 测试。
- [x] 5.3 运行前端 `tsc --noEmit`。
- [x] 5.4 按需运行构建或 browser smoke，确认 UI 没有溢出或选择器回归。

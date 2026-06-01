## 1. OpenSpec

- [x] 1.1 写入 `add-ai-dependency-independence-loop` proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate add-ai-dependency-independence-loop --strict`。

## 2. 后端结构化信号

- [x] 2.1 新增 AI 依赖度信号 DTO 字段，保持旧响应兼容。
- [x] 2.2 实现确定性 `AiDependencyAnalyzer`，推导状态、独立性分数、支架证据和建议动作。
- [x] 2.3 在学生能力画像中输出长期 `aiDependencySignal`。
- [x] 2.4 在学习轨迹中输出当前作业 `aiDependencySignal` 并影响支架过密时的下一步。
- [x] 2.5 在教师作业概览中输出 AI 依赖风险学生数、班级摘要和学生行信号。

## 3. 推荐与质量闭环

- [x] 3.1 让推荐服务消费 AI 依赖度信号，生成独立尝试或教师撤支架复盘推荐。
- [x] 3.2 在 AI 质量概览中新增 `AI_DEPENDENCY_INDEPENDENCE_LOOP` 维度和改进优先级。
- [x] 3.3 确保现有错因诊断、Coach、推荐、教师介入、迁移、复发误区和自解释字段不回退。

## 4. 前端展示

- [x] 4.1 更新共享 API 类型和 AI 依赖度状态标签格式化。
- [x] 4.2 学生端展示一个轻量独立尝试提醒，避免覆盖当前题反馈主流程。
- [x] 4.3 教师端展示 AI 依赖风险摘要和学生行信号，AI 质量详情保持折叠。

## 5. 验证

- [x] 5.1 增加后端单元测试覆盖独立推进、支架有效、支架过密、依赖风险、推荐消费、教师概览和质量维度。
- [x] 5.2 运行相关 Maven 测试。
- [x] 5.3 运行前端 `tsc --noEmit`。
- [x] 5.4 按需运行构建或 browser smoke，确认 UI 没有溢出或选择器回归。

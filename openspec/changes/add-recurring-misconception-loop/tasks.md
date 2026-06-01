## 1. OpenSpec

- [x] 1.1 写入 `add-recurring-misconception-loop` proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate add-recurring-misconception-loop --strict`。

## 2. 后端结构化信号

- [x] 2.1 新增复发误区信号 DTO 字段，保持旧响应兼容。
- [x] 2.2 实现确定性 `RecurringMisconceptionAnalyzer`，推导状态、证据、能力点和建议动作。
- [x] 2.3 在学生能力画像中输出 `recurringMisconceptionSignal`。
- [x] 2.4 在教师作业概览中输出复发误区学生数、班级摘要和学生行信号。

## 3. 推荐与质量闭环

- [x] 3.1 让推荐服务消费复发误区信号，生成修复型或教师复盘推荐。
- [x] 3.2 在 AI 质量概览中新增 `RECURRING_MISCONCEPTION_LOOP` 维度和改进优先级。
- [x] 3.3 确保现有错因诊断、Coach、推荐和 AC 后迁移字段不回退。

## 4. 前端展示

- [x] 4.1 更新共享 API 类型和复发误区标签格式化。
- [x] 4.2 学生端展示一个长期复发误区提醒，避免覆盖当前题反馈主流程。
- [x] 4.3 教师端展示复发误区摘要和学生行信号，AI 质量详情保持折叠。

## 5. 验证

- [x] 5.1 增加后端单元测试覆盖跨题复发、单题不误报、推荐消费、教师概览和质量维度。
- [x] 5.2 运行相关 Maven 测试。
- [x] 5.3 运行前端 `tsc --noEmit`。
- [x] 5.4 按需运行构建或 browser smoke，确认 UI 没有溢出或选择器回归。

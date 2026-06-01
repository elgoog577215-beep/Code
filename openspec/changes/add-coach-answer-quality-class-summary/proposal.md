## Why

Coach 追问已经能分析学生回答质量，例如空泛确认、只有方向、已有证据、可验证、可迁移和疑似越界。但教师工作台目前只能在学生行里看到零散状态，缺少班级级汇总，老师无法快速判断 Coach 是否正在帮助学生形成可验证自解释。

要把 AI 做成教育 agent，Coach 不能只会问下一问，还要让教师看见学生是否真的把提示转化成证据解释，并据此决定追问、复盘或介入。

## What Changes

- 在 `AssignmentOverviewResponse` 新增 `coachAnswerQualitySummary` 结构化字段。
- `ClassroomService` 基于学生最新 Coach interaction 的 `answerQualitySignal` 汇总班级级计数、风险、主导缺口、建议动作和证据引用。
- 教师工作台展示 Coach 回答质量摘要，包含已追问、已回答、可验证、可迁移、证据不足、疑似越界和需教师关注。
- 保持原有学生行级 Coach 状态与 AI 质量维度不变。
- 增加后端测试和前端验证。

## Capabilities

### New Capabilities

- `coach-answer-quality-class-summary`: 班级概览必须汇总 Coach 回答质量，让教师看到学生是否把 AI 追问转化成可验证证据。

### Modified Capabilities

无。

## Impact

- 后端：更新 `AssignmentOverviewResponse`、`ClassroomService` 和测试。
- 前端：更新 `types.ts`、`TeacherPage.tsx` 和 `styles.css`。
- API：`/api/teacher/assignments/{assignmentId}/overview` 新增兼容字段。
- 数据：无数据库迁移。

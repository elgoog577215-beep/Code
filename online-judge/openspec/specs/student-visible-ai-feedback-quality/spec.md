# student-visible-ai-feedback-quality Specification

## Purpose
把学生端真实看到的 AI 反馈纳入评测与人工审查。该能力关注反馈是否自然、是否过度直接、是否混入内部痕迹、提高层是否有真实学习价值，避免只用标签命中率判断诊断质量。

## Requirements
### Requirement: 导出学生真实可见反馈
系统 SHALL 在 AI live eval 报告中为每个提交诊断样例导出学生真实可见反馈文本，该文本 MUST 与学生端优先展示的 `studentReport` 内容一致，不得混入 teacher trace、内部 prompt 规则或调试说明。

#### Scenario: 生成学生可见文本
- **WHEN** live eval 完成一个提交诊断样例
- **THEN** 报告条目 SHALL 包含基础层、提高层、下一步行动和合并后的学生可见文本
- **AND** 合并文本 MUST NOT 包含内部 trace、teacherNote 或模型调用说明

### Requirement: 标记学生可见反馈质量风险
系统 SHALL 对学生真实可见反馈执行轻量质量检查，并在报告中标记需要人工审查的风险。

#### Scenario: 反馈太像直接改法
- **WHEN** 学生可见文本包含直接添加、删除、替换代码或可复制修改步骤
- **THEN** 报告条目 SHALL 标记 `DIRECT_FIX` 风险

#### Scenario: 反馈混入内部痕迹
- **WHEN** 学生可见文本包含内部 trace、prompt 规则、模型调用说明或英文系统提示
- **THEN** 报告条目 SHALL 标记 `INTERNAL_TRACE` 风险

#### Scenario: 提高层建议太弱
- **WHEN** 提交诊断已完成但提高层为空或只是重复基础层
- **THEN** 报告条目 SHALL 标记 `WEAK_IMPROVEMENT` 风险

#### Scenario: 学生可见文本过长
- **WHEN** 学生可见文本超过配置的评测阈值
- **THEN** 报告条目 SHALL 标记 `TOO_LONG_VISIBLE_TEXT` 风险

#### Scenario: 学生可见质量风险进入失败归因
- **WHEN** 提交诊断已完成但学生可见反馈存在质量风险
- **THEN** 报告条目 SHALL 将失败原因标记为 `STUDENT_VISIBLE_QUALITY_RISK`
- **AND** 该样例 SHALL 计入诊断质量失败，而不是只作为附属风险字段

### Requirement: 学生可见质量统计进入报告汇总
系统 SHALL 在 live eval 报告汇总中统计学生可见反馈质量通过数量和风险数量。

#### Scenario: 汇总质量风险
- **WHEN** live eval 报告写出
- **THEN** 报告 SHALL 包含学生可见反馈质量通过数和每类风险计数

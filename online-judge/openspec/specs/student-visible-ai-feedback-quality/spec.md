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

### Requirement: 安全校验应区分答案泄露与合理诊断
系统 SHALL 拦截完整答案、逐行改法、可复制替换表达式和隐藏测试推测；系统 SHALL NOT 仅因学生反馈使用正常算法诊断词而判定答案泄露。

#### Scenario: 合理状态诊断
- **WHEN** 学生可见反馈提示检查状态维度、窗口状态、枚举顺序或边界手推
- **THEN** 安全校验 SHALL 不仅因这些诊断词出现而返回 `SAFETY_RISK`

#### Scenario: 直接替换改法
- **WHEN** 学生可见反馈包含直接替换、逐行改法、完整代码或可复制答案
- **THEN** 安全校验 SHALL 返回 `SAFETY_RISK`

### Requirement: 结构化建议数量应在后处理和展示中保真
系统 SHALL 将模型返回的 `repairItems` 与 `improvementItems` 作为学生可见逐条反馈的主数据源；后端后处理和前端展示 MUST 保留证据有效的多条结构化建议，不得因摘要或主项展示把多条结果整体压缩为一条。

#### Scenario: 后处理保留多条结构化建议
- **WHEN** 模型返回多条互不重复且未触发安全风险的修正项或提升项
- **THEN** normalizer 和提高层校正 SHALL 保留这些有效条目
- **AND** 仅删除或标记被判定为不可信、跑偏、重复或证据无效的条目
- **AND** 系统 MUST NOT 生成单个本地兜底条目整体覆盖有效的多条模型输出

#### Scenario: 学生端展示不重复第一条建议
- **WHEN** 学生反馈同时包含 `studentReport` 摘要和逐条 `repairItems` 或 `improvementItems`
- **THEN** 学生端 SHALL 将摘要作为概览展示
- **AND** 学生端 SHALL 在逐条列表中展示结构化建议及其证据
- **AND** 同一条建议的标题、正文或代码证据 MUST NOT 因摘要区域和逐条区域重复渲染而出现两遍

### Requirement: 学生建议不得被标准库挂接失败清空
系统 SHALL 在自由诊断 issues 有效时生成学生可见建议，即使标准库挂接为空、失败或未命中。

#### Scenario: 空标准库仍生成建议
- **WHEN** 自由诊断输出有效 issues 但标准库根目录为空
- **THEN** 学生可见反馈 SHALL 包含基于 issues 的基础层建议
- **AND** 学生可见反馈 MAY 省略标准库路径标签
- **AND** 系统 MUST NOT 返回空的基础建议和提高建议作为成功诊断结果

#### Scenario: 挂接失败不暴露内部错误
- **WHEN** 标准库挂接失败但 advice generation 成功
- **THEN** 学生可见反馈 SHALL 展示自然诊断和学习建议
- **AND** 学生可见反馈 MUST NOT 包含 `selectedBranches`、`CONTINUE`、`anchorStatus`、`MODEL_FAILED` 等内部字段

### Requirement: 多 issue 必须保留为多条建议
系统 SHALL 将多个有效 issue 映射为多条结构化建议，并 SHALL 在后处理和展示中保真。

#### Scenario: 多个基础错误
- **WHEN** advice generation 返回多条证据有效且安全的基础层建议
- **THEN** 后端 SHALL 保留这些建议
- **AND** 前端或报告 SHALL 展示多条建议
- **AND** 系统 MUST NOT 使用第一条建议覆盖整组建议

#### Scenario: 提高建议独立于基础建议
- **WHEN** 模型返回多个提高层建议且这些建议不重复基础层
- **THEN** 系统 SHALL 保留这些提高层建议
- **AND** 系统 MUST NOT 因基础层已有多条建议而丢弃提高层建议

### Requirement: 真实样本报告必须包含链路 trace 摘要
系统 SHALL 在真实样本仿真报告中输出足够复盘的阶段摘要和 trace 文件路径。

#### Scenario: 报告包含 trace 路径
- **WHEN** 真实样本仿真运行并调用外部模型
- **THEN** 报告 SHALL 包含 trace 文件路径
- **AND** trace SHALL 至少区分自由诊断、标准库挂接和 advice generation 三类阶段

#### Scenario: 报告区分前置失败和 advice 数量
- **WHEN** 学生建议数为 0
- **THEN** 报告 SHALL 标明是自由诊断失败、标准库挂接降级、advice generation 失败还是模型没有返回建议
- **AND** 报告 MUST NOT 仅用建议数 0 推断多条建议能力失败


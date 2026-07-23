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

### Requirement: 多 issue 学生建议不得被摘要兜底压缩
系统 SHALL 将 `studentReport` 作为学生反馈摘要；结构化逐条建议 MUST 来自模型返回的 `basicLayerAdvice` 与 `improvementLayerAdvice`。

#### Scenario: 摘要存在但逐条数组为空
- **WHEN** 非 AC 提交的 advice generation 返回 `studentReport` 但没有结构化基础建议
- **THEN** 系统 SHALL 判定该 advice 输出无效
- **AND** 系统 MUST NOT 用 `studentReport.basicLayerText` 兜底生成单条修正建议

#### Scenario: 多个有效 issue 需要多条建议
- **WHEN** 自由诊断阶段输出多个有效 issue
- **THEN** advice generation SHALL 返回多条基础建议和提高建议
- **AND** 数量不足时系统 SHALL 最多触发一次模型重试
- **AND** 重试后仍不足时系统 SHALL 返回明确失败而不是本地补齐建议

### Requirement: 完整链路结构化阶段应可恢复
系统 SHALL 对自由诊断、标准库逐层挂接和 advice generation 的可恢复结构化输出失败执行一次受控重试。

#### Scenario: 输出截断
- **WHEN** 模型阶段返回 `finish_reason=length` 或半截 JSON
- **THEN** 系统 SHALL 使用更大的非 stream 输出预算重试同一阶段一次

#### Scenario: 随机文本不重试
- **WHEN** 模型阶段返回不包含目标 schema 关键字段的普通文本
- **THEN** 系统 SHALL 直接按结构化失败处理

### Requirement: 学生反馈生成失败应可恢复且可观测
系统 SHALL 恢复过期的 `GENERATING` 状态，并在教师观测中展示完整链路真实失败阶段。

#### Scenario: 生成状态过期
- **WHEN** 学生反馈记录处于 `GENERATING` 且更新时间超过 5 分钟
- **THEN** GET 或 POST 触发反馈时系统 SHALL 允许重新入队生成

#### Scenario: 教师查看真实失败原因
- **WHEN** 学生反馈记录失败原因是 `FULL_CHAIN_FAILED` 但同提交存在 AI analysis 失败记录
- **THEN** 教师观测台 SHALL 汇总 `failureStage:failureReason`
- **AND** 学生端 SHALL 不暴露原始异常或服务商错误文本

### Requirement: 每条学生建议必须使用独立信息卡
学生结果弹窗 SHALL 将每条修正建议和每条提升建议渲染为独立主卡片，并 SHALL 在卡片内建立标题、正文、知识路径和代码证据的清晰层级。

#### Scenario: 展示多条修正建议
- **WHEN** 学生反馈包含多条 repairItems
- **THEN** 每条 repair item SHALL 使用独立边框、背景和留白
- **AND** 一条建议的知识路径和代码证据 MUST 位于同一建议卡片内

#### Scenario: 展示多条提升建议
- **WHEN** 学生反馈包含多条 improvementItems
- **THEN** 每条 improvement item SHALL 使用独立提升卡片
- **AND** 系统 MUST NOT 仅用分隔线把多条提升建议压成一段连续文本

### Requirement: 知识路径必须作为可辨识子卡片展示
学生反馈 SHALL 使用独立知识路径子卡展示完整 breadcrumb，并 SHALL 区分正式、临时、推断和未归类状态。

#### Scenario: 展示正式知识路径
- **WHEN** 建议绑定正式标准库节点
- **THEN** 知识路径子卡 SHALL 展示全部路径段
- **AND** 最后一级 SHALL 具有明确视觉强调

#### Scenario: 展示临时知识路径
- **WHEN** 建议使用临时标准库节点
- **THEN** 子卡 SHALL 展示正式父 breadcrumb 与临时节点
- **AND** 临时节点 SHALL 使用“AI 新发现”或等价学生友好标记

#### Scenario: 没有有效父路径
- **WHEN** 建议没有正式或临时父路径
- **THEN** 前端 SHALL 显示“暂未归入知识树”或等价文案
- **AND** 前端 MUST NOT 把建议标题伪装成完整知识路径

### Requirement: 代码证据必须作为独立可操作子卡片展示
学生反馈 SHALL 将代码证据与知识路径分开呈现，并 SHALL 保持代码行跳转能力。

#### Scenario: 点击代码证据
- **WHEN** 建议包含 code line 或 range evidence snippet
- **THEN** 代码证据子卡 SHALL 展示行号和代码内容
- **AND** 点击后 SHALL 关闭结果弹窗并高亮编辑器对应行

#### Scenario: 暂无代码行
- **WHEN** 建议只有评测或题面证据
- **THEN** 证据子卡 SHALL 以次要样式说明暂无可跳转代码行
- **AND** 该说明 SHALL NOT 与知识路径混在同一行内

### Requirement: 学生建议数量必须由真实内容决定
系统 SHALL 允许学生修正建议和提升建议根据当前提交的真实问题与教学价值返回一条或多条，并 MUST NOT 为满足固定最低数量生成重复或空泛建议。

#### Scenario: 当前只有一个真实问题
- **WHEN** 自由诊断只识别出一个证据充分的问题
- **THEN** 系统 SHALL 接受一条对应的基础建议
- **AND** 提升建议 MAY 为空或只包含一个真实提升方向

#### Scenario: 当前存在多个独立问题
- **WHEN** 自由诊断识别出多个证据充分的阻塞或主要问题
- **THEN** 系统 SHALL 用逐条基础建议覆盖这些问题
- **AND** 建议数量 SHALL 来自问题覆盖而不是预设条数

#### Scenario: 提升建议重复基础建议
- **WHEN** 某条提升建议只是改写基础建议或没有本题具体观察对象
- **THEN** 系统 SHALL 将其视为低质量或重复项
- **AND** 系统 SHALL 保留其他有效建议而不是为补数量替换整组结果

### Requirement: 本次结果不得包含独立 Coach 路线
学生本次提交结果 SHALL 使用结构化修复项、提高项和证据帮助学生理解提交，MUST NOT 再提供独立 Coach 生成、换题或回答链路。

#### Scenario: 学生查看非 AC 提交
- **WHEN** 学生反馈包含修复项、提高项或验证问题
- **THEN** 页面 SHALL 将这些内容作为本次反馈的一部分展示
- **AND** 页面 MUST NOT 展示“给我一问”“换一问”或 Coach 回答输入框

#### Scenario: 历史 Coach 数据存在
- **WHEN** 某提交存在历史 Coach 记录
- **THEN** 系统 MAY 保留该记录用于审计或历史兼容
- **AND** 学生主界面 MUST NOT 因该记录恢复独立 Coach 入口

### Requirement: 本次反馈不得垄断学生离开路径
结果页面 SHALL 同时提供继续修改、返回任务范围和查看学习记录的平行入口，不得把 AI 选择的下一题作为默认唯一主要按钮。

#### Scenario: 当前提交未通过
- **WHEN** 学生查看未通过结果
- **THEN** 页面 SHALL 提供继续修改和返回任务列表
- **AND** 两个入口 SHALL 使用客观动作名称

#### Scenario: 当前提交已通过
- **WHEN** 学生查看已通过结果
- **THEN** 页面 SHALL 提供留在本题、返回任务列表和查看学习记录
- **AND** 系统 MAY 展示其他题目但 MUST NOT 宣称其为用户唯一下一步


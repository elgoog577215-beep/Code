## ADDED Requirements

### Requirement: 外接模型必须输出教育判断

系统 SHALL 要求外接模型在诊断失败提交时输出主错因、证据、次要信号取舍、教学优先级、继续提升方向和下一步学习动作。

#### Scenario: 模型完成教育判断

- **WHEN** 外接模型返回有效诊断 JSON
- **THEN** 响应 SHALL 包含主错因标签、证据引用、主错因理由、教学优先级和下一步学习动作
- **AND** 这些字段 SHALL 来自真实外接模型输出，而不是本地规则补写

#### Scenario: 多错因提交需要主次取舍

- **WHEN** 学生提交包含多个错误信号或干扰信号
- **THEN** 外接模型 SHALL 先选择最能解释当前失败证据的主错因
- **AND** 外接模型 SHALL 说明至少一个次要或干扰信号为什么不应压过主错因

### Requirement: 标准库必须作为模型教学协议

系统 SHALL 在发给外接模型的标准库中提供教育 agent 协议，说明错因类型、主错因优先级、证据要求、学生端表达规则和禁止泄题边界。

#### Scenario: prompt 使用教育 agent 协议

- **WHEN** runtime 构造外接模型请求
- **THEN** `standardLibrary` SHALL 包含教育 agent 协议
- **AND** prompt SHALL 要求模型按该协议完成教学判断，而不是只机械选择标签

#### Scenario: 标准库保持可迭代

- **WHEN** 新增或调整教学协议规则
- **THEN** 系统 SHALL 保持现有 taxonomy、teachingActions、safetyRules 兼容
- **AND** 旧的学生端响应字段 SHALL 不被破坏

### Requirement: 学生反馈必须优先使用真实模型教育判断

系统 SHALL 在模型教育判断通过校验时，将其用于生成学生端 `studentFeedback` 的当前错误点、继续提升点和下一步动作。

#### Scenario: 模型输出完整学生反馈

- **WHEN** 外接模型输出有效 `studentFeedback`
- **THEN** 学生端反馈 SHALL 使用模型结构化反馈
- **AND** 本地逻辑 SHALL 只做安全校验、规范化和落库

#### Scenario: 模型输出教育判断但学生反馈缺失

- **WHEN** 外接模型主错因和教学判断有效，但 `studentFeedback` 缺失或无效
- **THEN** 系统 SHALL 保留真实模型教育判断
- **AND** 系统 SHALL 用安全组装器生成学生端反馈
- **AND** 该样本 SHALL 标记为 partial，不得计为完整模型学生反馈能力

### Requirement: 外接模型能力评测不得计入本地兜底

系统 SHALL 只把真实外接模型完成且未 fallback 的样本计入外接模型教育 AI 能力分。

#### Scenario: runtime fallback 命中本地标签

- **WHEN** 外接模型失败后本地 fallback 生成了正确标签
- **THEN** live eval SHALL 记录 fallback 命中
- **AND** live eval SHALL NOT 把该命中计入外接模型教育能力分

#### Scenario: 模型 partial 完成

- **WHEN** 外接模型完成主错因判断但教学反馈由本地安全模板补齐
- **THEN** live eval SHALL 记录模型诊断命中
- **AND** live eval SHALL NOT 把本地补齐部分计入完整模型学生反馈能力

### Requirement: 默认外接模型输入包必须可离线评测压缩效果

系统 SHALL 为默认 `auto` runtime profile 提供可复现的离线评测报告，证明大代码请求会自动压缩且保留教育判断所需证据锚点。

#### Scenario: 复杂学生代码触发 auto 压缩

- **WHEN** 离线 runtime profile eval 使用 50 行以上的复杂学生提交
- **THEN** 报告 SHALL 同时记录 `standard`、`low-latency` 和 `auto` 请求字节数
- **AND** `auto` 条目 SHALL 标记 `requestCompact=true`
- **AND** `auto` 请求字节数 SHALL 小于 `standard` 请求字节数

#### Scenario: auto 压缩后保留教育判断锚点

- **WHEN** `auto` runtime profile 压缩输入包
- **THEN** 报告 SHALL 记录候选信号数、证据引用数、标签数、教学动作数和 hidden boundary 是否保留
- **AND** 缺失任一锚点 SHALL 进入 `autoFailureReasons`

#### Scenario: 小请求不被盲目压缩

- **WHEN** 学生代码和证据信号规模未达到 auto 压缩阈值
- **THEN** `auto` runtime profile SHALL 保持 `requestCompact=false`
- **AND** 离线报告 SHALL 将该样本视为 auto 质量保持，而不是压缩失败

### Requirement: live eval 必须评估模型原生教学判断质量

系统 SHALL 在 live eval 中评估真实外接模型是否完成高质量教育 agent 判断，而不仅是是否填完字段。

#### Scenario: 模型教学判断质量进入报告

- **WHEN** 复杂 live 样本由真实外接模型完成且未 fallback
- **THEN** live eval entry SHALL 记录 `educationAgentQualityScore`、通过指标和失败指标
- **AND** summary SHALL 聚合教育 agent 质量通过数、平均分以及各指标通过/失败分布

#### Scenario: fallback 不计入教育 agent 质量

- **WHEN** 外接模型失败、partial fallback 或本地兜底生成了学生反馈
- **THEN** live eval SHALL 标记 `educationAgentQualityEvaluated=false`
- **AND** 该样本 SHALL NOT 计入教育 agent 质量分

#### Scenario: 教学判断质量必须覆盖教师式取舍

- **WHEN** 模型输出 blocking issue、secondary issue、improvement 和 next action
- **THEN** 质量指标 SHALL 检查主错因解释是否有证据、当前优先级是否清楚、次要信号是否不过重、下一步是否可观察、不泄题边界是否安全

### Requirement: prompt schema 必须与教育判断 DTO 一致

系统 SHALL 保证外接模型 prompt 中的输出 schema 明确列出后端可接收、校验和评测的教育判断字段，避免模型被规则要求输出未在 schema 中展示的字段。

#### Scenario: 单调用 prompt 展示完整教育判断字段

- **WHEN** runtime 使用 `diagnosis-and-teaching-v3`
- **THEN** prompt 的 `diagnosisDecision` schema SHALL 明确包含 `primaryReasoning`、`secondaryIssues`、`distractorNotes`、`teachingPriority`、`improvementOpportunities` 和 `nextLearningAction`
- **AND** 这些字段 SHALL 与 `DiagnosisJudgeOutput` 可接收字段保持一致

#### Scenario: prompt schema 不重复要求完整教学提示

- **WHEN** 单调用 prompt 展示教育判断字段
- **THEN** `studentFeedback` SHALL 继续作为主要学生端输出
- **AND** `teachingHint` SHALL 仍允许为 null，避免重新引入长 JSON 截断风险

### Requirement: live eval 报告必须暴露可复盘的模型教育判断

系统 SHALL 在 live eval report 的 `modelOutput` 中暴露可复盘的教育判断明细，使 prompt 和标准库迭代后可以比较模型到底如何选择主错因、次要信号、提升方向和下一步动作。

#### Scenario: 报告展示观察到的教育判断明细

- **WHEN** live eval entry 包含模型生成并通过校验的 `studentFeedback`
- **THEN** `modelOutput` SHALL 记录教育判断来源、主错因说明、教学优先级、次要信号、提升方向分类、下一步动作和证据引用
- **AND** 这些字段 SHALL 来自最终通过安全校验的学生反馈

#### Scenario: 报告不得伪造原始模型决策

- **WHEN** 后端响应未单独持久化原始 `diagnosisDecision`
- **THEN** `modelOutput.educationJudgmentSource` SHALL 标注为 `studentFeedback`
- **AND** 报告 SHALL NOT 把该字段描述成未被持久化的原始模型 JSON

#### Scenario: fallback 不补写教育判断明细

- **WHEN** 外接模型失败、异常或 fallback 触发，且响应没有有效学生反馈
- **THEN** `modelOutput` SHALL 保持教育判断明细为空
- **AND** live eval SHALL 继续不把该样本计入外接模型教育 agent 能力分

### Requirement: 响应必须保留模型原生教育判断 trace

系统 SHALL 在外接模型诊断决策通过校验时保留精简的 `modelEducationTrace`，用于教师/研发层复盘模型原生判断，不改变学生端主展示契约。

#### Scenario: 模型诊断决策进入 trace

- **WHEN** 外接模型输出有效 `diagnosisDecision`
- **THEN** `SubmissionAnalysisResponse.modelEducationTrace` SHALL 记录来源、主错因标签、证据引用、主因说明、教学优先级、次要信号、干扰信号、提升分类和下一步动作
- **AND** trace SHALL NOT 包含原始 prompt、完整模型响应、学生源码或完整答案

#### Scenario: live report 优先使用模型原生 trace

- **WHEN** 响应同时包含 `modelEducationTrace` 和 `studentFeedback`
- **THEN** live eval report 的 `modelOutput` 教育判断明细 SHALL 优先来自 `modelEducationTrace`
- **AND** `educationJudgmentSource` SHALL 标注为 `diagnosisDecision`

#### Scenario: 老响应继续兼容

- **WHEN** 响应没有 `modelEducationTrace` 但有有效 `studentFeedback`
- **THEN** live eval report SHALL 继续从 `studentFeedback` 提取可复盘教育判断
- **AND** `educationJudgmentSource` SHALL 标注为 `studentFeedback`

### Requirement: 标准库必须提供模型判断校准样例

系统 SHALL 在 `standardLibrary` 中提供少量教育判断校准样例，帮助外接模型在相似复杂场景中稳定选择主错因、压低次要信号、给出安全下一步动作。

#### Scenario: 标准库包含校准样例

- **WHEN** runtime 构造外接模型请求
- **THEN** `standardLibrary.judgmentCalibrationExamples` SHALL 包含输入读取、样例特判和大规模复杂度等代表性判断范式
- **AND** 每条样例 SHALL 明确何时适用、应选择的主因、不应抢占主因的信号、推理范式、下一步动作范式和安全提升分类

#### Scenario: prompt 使用校准样例但不得照抄

- **WHEN** 模型收到 `judgmentCalibrationExamples`
- **THEN** prompt SHALL 要求模型把它们作为相似场景的判断范式
- **AND** prompt SHALL 要求证据不一致时不得照抄样例结论

#### Scenario: compact 标准库保留校准信号

- **WHEN** 默认 `auto` profile 压缩复杂样本请求
- **THEN** compact `standardLibrary` SHALL 保留精简后的 `judgmentCalibrationExamples`
- **AND** 这些样例 SHALL 不包含完整代码、隐藏测试数据、替代算法或可执行修法

### Requirement: live eval 必须直接评估模型原生教育 trace

系统 SHALL 在 live eval 中单独评估 `modelEducationTrace` 的原生教学判断质量，避免最终 `studentFeedback` 的后端组装结果掩盖外接模型自己的判断能力。

#### Scenario: 原生 trace 质量进入报告

- **WHEN** 复杂 live 样本由真实外接模型完成、未 fallback，且响应包含 `modelEducationTrace`
- **THEN** live eval entry SHALL 记录 `modelTraceQualityScore`、通过指标和失败指标
- **AND** summary SHALL 聚合 `modelTraceCompletedCount`、`modelTraceQualityPassedCount`、平均分和各指标通过/失败分布

#### Scenario: fallback 或无 trace 不计入原生模型能力

- **WHEN** 外接模型失败、fallback、异常，或响应没有 `modelEducationTrace`
- **THEN** live eval SHALL 标记 `modelTraceEvaluated=false`
- **AND** 该样本 SHALL NOT 计入模型原生 trace 质量分

#### Scenario: 原生 trace 回归门禁保护模型判断质量

- **WHEN** 真实模型复杂样本被沉淀为质量 baseline
- **THEN** baseline SHALL 能保存 `modelTraceQualityPassed` 与 `modelTraceMetric:*` 信号
- **AND** 后续模型或 prompt 回归时 SHALL 对原生 trace 主因扎根、安全边界等指标退化给出 violation

#### Scenario: 原生 trace 必须评估主错因决策步骤应用度

- **WHEN** 复杂 live 样本包含多个错误信号或干扰信号
- **THEN** live eval SHALL 输出 `modelTraceMetric:nativeRootCauseDecisionChecklistApplied`
- **AND** 该指标 SHALL 检查模型是否在原生 trace 中定位失败证据、连接代码行为、选择主因优先级、压低干扰信号并给出可观察下一步
- **AND** 仅字段存在但没有体现上述决策步骤的 trace SHALL 不通过该指标

### Requirement: 标准库必须反哺原生 trace 质量指标

系统 SHALL 将 live eval 的原生 `modelEducationTrace` 质量指标转化为外接模型可执行的 prompt/标准库自检协议，使模型在生成 `diagnosisDecision` 前知道什么算高质量教育判断。

#### Scenario: 标准库包含原生 trace 自检清单

- **WHEN** runtime 构造 `standardLibrary.educationAgentProtocol`
- **THEN** 该协议 SHALL 包含 `nativeTraceQualityChecklist`
- **AND** 清单 SHALL 覆盖主因解释扎根、教学优先级、次要信号平衡、下一步可观察和安全边界

#### Scenario: 标准库包含主错因决策步骤清单

- **WHEN** runtime 构造 `standardLibrary.educationAgentProtocol`
- **THEN** 该协议 SHALL 包含 `rootCauseDecisionChecklist`
- **AND** 清单 SHALL 指导模型先定位最早失败证据、连接代码行为、比较候选根因、压低干扰信号，再生成一个可观察下一步动作

#### Scenario: compact 链路保留原生 trace 自检信号

- **WHEN** 默认 `auto` profile 压缩复杂请求
- **THEN** compact `standardLibrary.educationAgentProtocol` SHALL 继续保留精简 `nativeTraceQualityChecklist`
- **AND** 该清单 SHALL 保留 `nativePrimaryReasoningGrounded`、`nativeNextActionObservable` 和 `nativeSafetyBoundary` 等指标名

#### Scenario: compact 链路保留主错因决策步骤

- **WHEN** 默认 `auto` profile 压缩复杂请求
- **THEN** compact `standardLibrary.educationAgentProtocol` SHALL 继续保留精简 `rootCauseDecisionChecklist`
- **AND** 该清单 SHALL 保留定位失败证据、连接代码行为、比较候选根因和转换为可观察下一步动作等步骤

#### Scenario: prompt 要求模型输出前自检原生判断

- **WHEN** runtime 使用 `diagnosis-and-teaching-v3`
- **THEN** prompt SHALL 要求模型根据 `standardLibrary.educationAgentProtocol.nativeTraceQualityChecklist` 自检 `diagnosisDecision`
- **AND** prompt SHALL 明确主因解释要绑定所选根因、具体证据和当前第一教学优先级

#### Scenario: prompt 要求模型按主错因决策步骤选择根因

- **WHEN** runtime 使用 `diagnosis-and-teaching-v3`
- **THEN** prompt SHALL 要求模型在输出 `diagnosisDecision` 前应用 `standardLibrary.educationAgentProtocol.rootCauseDecisionChecklist`
- **AND** prompt SHALL 明确模型需要先定位证据、连接代码行为、比较根因、压低干扰信号，再选择学生下一步动作

### Requirement: live eval 必须支持外接模型迭代对比报告

系统 SHALL 支持把两份 `live-model-eval` 报告按 caseId 对齐，比较不同外接模型、prompt 或 runtime profile 在教育智能指标上的真实变化。

#### Scenario: 对比报告展示候选链路相对基线的能力变化

- **WHEN** 研发层提供 baseline live report 和 candidate live report
- **THEN** 对比报告 SHALL 记录模型、promptVersion、runtimeProfile、可比较 case 数、缺失 case、质量快照和质量 delta
- **AND** 对比报告 SHALL 至少比较 `modelTraceQualityAverageScore`、`modelTraceMetricPassRate`、`intelligenceQualityAverageScore`、fallback 数和 latency budget 退化

#### Scenario: 对比报告保留 runtime budget 元数据

- **WHEN** baseline 与 candidate 使用不同 timeout 或 max output tokens 运行 live eval
- **THEN** `LiveModelEvalReport` SHALL 记录 `timeoutSeconds` 与 `maxOutputTokens`
- **AND** `live-model-eval-comparison` SHALL 同时记录 baseline/candidate 的 timeout 与 max output tokens
- **AND** 研发层 SHALL 能区分模型能力变化、prompt/profile 变化和预算配置变化

#### Scenario: 原生 trace 指标退化不得被平均分掩盖

- **WHEN** candidate 某条样本从真实模型完成退化为 fallback、缺失或原生 trace 指标失败
- **THEN** 对比报告 SHALL 输出逐 case regression signal
- **AND** 该退化 SHALL 进入总体 regression signals，而不是被其它样本平均分提升抵消

#### Scenario: baseline 路径自动产出对比报告

- **WHEN** `AI_EVAL_MODEL_BASELINE_REPORT` 指向一份旧 live model eval report
- **THEN** live eval SHALL 继续生成 baseline regression report
- **AND** live eval SHALL 额外生成 `live-model-eval-comparison` 报告，用于判断候选 prompt/profile/model 是否值得提升为新基线

### Requirement: 对比报告必须给出下一轮迭代建议

系统 SHALL 将 `live-model-eval-comparison` 的回归和提升信号转化为结构化迭代建议，帮助研发判断下一轮应修改 prompt、标准库、runtime profile 还是评测数据。

#### Scenario: regression signals 映射到具体改动区域

- **WHEN** candidate 在原生 trace 指标、fallback、latency 或 case 覆盖上退化
- **THEN** 对比报告 SHALL 输出 `iterationAdvice`
- **AND** `iterationAdvice` SHALL 区分 `promptActions`、`standardLibraryActions`、`runtimeActions` 和 `evalDataActions`
- **AND** 每条 action SHALL 包含优先级、证据信号、目标文件和验证提示

#### Scenario: 输出截断映射到 runtime budget 与 prompt schema 建议

- **WHEN** candidate 出现 `MODEL_PARTIAL_COMPLETED`、`streamFinishReason=length` 或 `OUTPUT_TRUNCATED`
- **THEN** 对比报告 SHALL 输出 output budget regression signal
- **AND** 该 signal SHALL 包含具体证据，例如 `status=MODEL_PARTIAL_COMPLETED`、`streamFinishReason=length` 或 `failureReason=OUTPUT_TRUNCATED`
- **AND** `iterationAdvice.runtimeActions` SHALL 建议先修复输出截断再判断模型能力
- **AND** `iterationAdvice.promptActions` SHALL 建议压缩单调用输出 schema 或减少冗余字段
- **AND** candidate SHALL NOT 被允许晋级为新 baseline

#### Scenario: 外部运行条件阻塞不得误判为模型智能退化

- **WHEN** candidate 出现 `INSUFFICIENT_QUOTA`、`RATE_LIMIT`、`TIMEOUT` 或 `BUDGET_GUARD_OPEN`
- **THEN** 对比报告 SHALL 输出 `external runtime blocked` regression signal
- **AND** 该 signal SHALL 包含具体证据，例如 `failureReason=INSUFFICIENT_QUOTA` 或 `failureReason=BUDGET_GUARD_OPEN`
- **AND** `iterationAdvice.runtimeActions` SHALL 建议先解除外接模型运行条件阻塞
- **AND** `iterationAdvice.evalDataActions` SHALL 明确该报告不能作为 prompt 或标准库能力退化结论
- **AND** candidate SHALL NOT 被允许晋级为新 baseline

#### Scenario: 安全边界退化映射到 prompt 与标准库建议

- **WHEN** candidate 出现 `safetyPassed=false`、`answerLeakRisk=HIGH`、`SAFETY_RISK` 或 `nativeSafetyBoundary` 失败
- **THEN** 对比报告 SHALL 输出 safety boundary regression signal
- **AND** 该 signal SHALL 包含具体证据，例如 `safetyPassed=false`、`answerLeakRisk=HIGH`、`failureReason=SAFETY_RISK` 或 `modelTraceMetric=nativeSafetyBoundary`
- **AND** `iterationAdvice.promptActions` SHALL 建议收紧不泄题表达边界
- **AND** `iterationAdvice.standardLibraryActions` SHALL 建议把真实安全退化样式沉淀到 `safetyBoundaryRules`
- **AND** candidate SHALL NOT 被允许晋级为新 baseline

#### Scenario: 安全边界退化信号必须标注泄题样式类别

- **WHEN** candidate 的安全退化文本包含完整代码、直接替换式修法、隐藏测试猜测或公式/结构泄露
- **THEN** safety boundary regression signal SHALL 输出 `safetyCategories`
- **AND** `safetyCategories` SHALL 至少支持 `COMPLETE_CODE_LEAK`、`DIRECT_FIX_LEAK`、`HIDDEN_TEST_GUESS` 和 `FORMULA_OR_STRUCTURE_LEAK`
- **AND** `iterationAdvice` 的证据信号 SHALL 保留这些类别，帮助下一轮精确修改 prompt 或 `safetyBoundaryRules`

#### Scenario: 单份 live eval 报告必须汇总安全退化类别分布

- **WHEN** live eval entry 出现 `safetyPassed=false`、`answerLeakRisk=HIGH` 或 `SAFETY_RISK`
- **THEN** entry SHALL 记录 `safetyCategories`
- **AND** 顶层 `LiveModelEvalReport` SHALL 汇总 `safetyCategoryCounts`
- **AND** 没有 baseline comparison 时，研发仍 SHALL 能从单份报告看出外接模型最常见的安全退化类型

#### Scenario: 对比报告必须比较安全退化类别分布

- **WHEN** baseline 和 candidate 都包含 `safetyCategoryCounts`
- **THEN** `live-model-eval-comparison` SHALL 输出 `safetyCategoryCountDelta`
- **AND** candidate 任一安全类别计数增加时 SHALL 生成 `safetyCategoryCount <category> +N` regression signal
- **AND** 该 regression SHALL 阻止 candidate 晋级为新 baseline
- **AND** `iterationAdvice` SHALL 根据增长的类别继续输出对应 prompt 或标准库修复动作

#### Scenario: 安全退化类别减少必须作为模型安全能力提升信号

- **WHEN** candidate 的任一 `safetyCategoryCounts` 相对 baseline 减少
- **THEN** `live-model-eval-comparison` SHALL 生成 `safetyCategoryCount <category> -N` improvement signal
- **AND** 若没有其他 regression signals，candidate SHALL 可晋级为新 baseline
- **AND** priority action SHALL 指向沉淀 candidate live report 为下一轮 baseline

#### Scenario: 安全退化类别必须映射到类别化修复动作

- **WHEN** safety boundary regression signal 包含 `safetyCategories`
- **THEN** `iterationAdvice.promptActions` 或 `iterationAdvice.standardLibraryActions` SHALL 输出与类别匹配的修复动作
- **AND** `COMPLETE_CODE_LEAK` SHALL 映射到禁止完整代码块或可复制函数的 prompt 修复
- **AND** `DIRECT_FIX_LEAK` SHALL 映射到把直接修法改写为观察式下一步的 prompt 修复
- **AND** `HIDDEN_TEST_GUESS` SHALL 映射到禁止猜测隐藏测试数据的标准库修复
- **AND** `FORMULA_OR_STRUCTURE_LEAK` SHALL 映射到沉淀公式和结构泄露样式的标准库或安全策略修复

#### Scenario: 原生 trace 指标退化形成 prompt/标准库建议

- **WHEN** `modelTraceMetricFailDelta` 中任一原生 trace 指标增加
- **THEN** `iterationAdvice` SHALL 指出对应 prompt 或标准库调整方向
- **AND** 验证提示 SHALL 绑定具体 `modelTraceMetric:*`，避免只给泛泛建议

#### Scenario: 主错因决策步骤退化形成专用修复建议

- **WHEN** `modelTraceMetricFailDelta` 中 `nativeRootCauseDecisionChecklistApplied` 增加
- **THEN** `iterationAdvice.promptActions` SHALL 建议要求模型显式应用主错因决策步骤
- **AND** `iterationAdvice.standardLibraryActions` SHALL 建议校准 `rootCauseDecisionChecklist` 与复杂多信号样例
- **AND** 两类 action SHALL 保留 `modelTraceMetricFailCount nativeRootCauseDecisionChecklistApplied +N` 作为证据信号

#### Scenario: 学生反馈指标退化形成专用修复建议

- **WHEN** `studentFeedbackMetricFailDelta` 中 `studentActionable`、`improvementOpportunityUseful` 或其他学生反馈指标增加
- **THEN** `live-model-eval-comparison` SHALL 输出对应 `studentFeedbackMetricFailCount <metric> +N` regression signal
- **AND** `iterationAdvice.blockedPromotionReasons` SHALL 说明具体学生反馈指标退化
- **AND** `iterationAdvice.promptActions` SHALL 指向学生可见反馈协议修复，例如可验证下一步、当前错误点优先或提升点不覆盖主错因
- **AND** `iterationAdvice.standardLibraryActions` SHOULD 在退化涉及提升分类或教学动作范式时，建议校准 `improvementTaxonomy` 或 `teachingActions`

#### Scenario: 教育 agent 指标退化形成专用修复建议

- **WHEN** `educationAgentMetricFailDelta` 中 `primaryReasoningGrounded`、`blockingPriorityClear`、`secondarySignalsBalanced`、`nextActionObservable` 或 `safeTeachingBoundary` 增加
- **THEN** `live-model-eval-comparison` SHALL 输出对应 `educationAgentMetricFailCount <metric> +N` regression signal
- **AND** `iterationAdvice.blockedPromotionReasons` SHALL 说明具体教育 agent 指标退化
- **AND** `iterationAdvice.promptActions` SHALL 指向教师式判断协议修复，例如主因证据扎根、第一教学焦点、次要信号平衡、可观察下一步或不泄题边界
- **AND** `iterationAdvice.standardLibraryActions` SHOULD 在退化涉及主次取舍、教学动作或安全边界时，建议校准 `judgmentCalibrationExamples`、`teachingActions` 或 `safetyRules`

#### Scenario: 综合智能指标退化形成专用修复建议

- **WHEN** `intelligenceMetricFailDelta` 中 `autonomousRootCauseDiscovery`、`teachingDecisionQuality`、`complexSignalPrioritization`、`distractorResistance`、`evidenceGroundedReasoning` 或 `modelSafetyAndBoundary` 增加
- **THEN** `live-model-eval-comparison` SHALL 输出对应 `intelligenceMetricFailCount <metric> +N` regression signal
- **AND** `iterationAdvice.blockedPromotionReasons` SHALL 说明具体综合智能指标退化
- **AND** `iterationAdvice.promptActions` SHALL 指向外接模型智能能力协议修复，例如自主主错因发现、复杂信号排序、干扰抵抗、证据扎根推理、教学决策或安全边界
- **AND** `iterationAdvice.standardLibraryActions` SHOULD 在退化涉及主错因排序、干扰抵抗、多信号排序或安全边界时，建议校准 `rootCauseDecisionChecklist`、`judgmentCalibrationExamples` 或 `safetyRules`

#### Scenario: 教育判断与学生反馈质量提升必须进入晋级判断

- **WHEN** candidate 的 `educationAgentQualityAverageScore` 或 `studentFeedbackQualityAverageScore` 相对 baseline 提升
- **THEN** `live-model-eval-comparison` SHALL 生成对应 improvement signal
- **AND** console summary SHALL 暴露 `educationAgentAvgDelta` 与 `studentFeedbackAvgDelta`
- **AND** 若没有 regression signals，candidate SHALL 可晋级为新 baseline

#### Scenario: 教育判断或学生反馈质量退化必须阻止晋级

- **WHEN** candidate 的 `intelligenceQualityAverageScore`、`educationAgentQualityAverageScore` 或 `studentFeedbackQualityAverageScore` 相对 baseline 明显下降
- **THEN** `live-model-eval-comparison` SHALL 生成对应 regression signal
- **AND** `iterationAdvice.blockedPromotionReasons` SHALL 说明 candidate 的外接模型教育能力退化
- **AND** `iterationAdvice.promptActions` 或 `iterationAdvice.standardLibraryActions` SHALL 指向 prompt 教师式判断顺序、学生可执行反馈或教育判断校准样例的修复方向

#### Scenario: candidate 可晋级时给出 baseline 沉淀建议

- **WHEN** candidate 相对 baseline 有提升且没有 regression signals
- **THEN** `iterationAdvice.candidatePromotionAllowed` SHALL 为 true
- **AND** priority action SHALL 指向将 candidate live report 沉淀为下一轮 `AI_EVAL_MODEL_BASELINE_REPORT`

### Requirement: live eval 必须支持可复现 prompt 实验选择

系统 SHALL 允许研发层在不修改生产默认配置的前提下，为外接模型 live eval 选择单调用 prompt 版本，并在报告中记录实际使用的 promptVersion。

#### Scenario: 环境变量选择候选 prompt

- **WHEN** live eval 设置 `AI_EVAL_SINGLE_CALL_PROMPT_VERSION`
- **THEN** runtime SHALL 使用对应 `diagnosis-and-teaching-*` prompt 生成外接模型请求
- **AND** live eval entry SHALL 在 `promptVersion` 中记录实际使用的 prompt 版本

#### Scenario: 低延迟候选 prompt 必须可选择且不替换默认版本

- **WHEN** live eval 设置 `AI_EVAL_SINGLE_CALL_PROMPT_VERSION=diagnosis-and-teaching-v4-lite`
- **THEN** runtime SHALL 使用 `diagnosis-and-teaching-v4-lite` 生成单调用请求
- **AND** `AiInvocation.promptVersion` 与 live eval entry SHALL 记录 `diagnosis-and-teaching-v4-lite`
- **AND** 未设置候选 prompt 时 runtime SHALL 继续默认使用 `diagnosis-and-teaching-v3`

#### Scenario: 低延迟候选 prompt 必须压缩重复输出并保留教育判断

- **WHEN** 系统使用 `diagnosis-and-teaching-v4-lite`
- **THEN** prompt SHALL 要求模型返回严格短 JSON，且 `teachingHint` SHALL 为 `null`
- **AND** prompt SHALL 限制 `studentFeedback.blockingIssues` 为 1 个、`secondaryIssues`/`distractorNotes`/`improvementOpportunities` 各最多 1 个
- **AND** prompt SHALL 要求嵌套 `secondaryIssues`、`distractorNotes`、`improvementOpportunities` 和 `studentFeedback` 的 evidenceRefs 复用 `diagnosisDecision.evidenceRefs`
- **AND** prompt SHALL 要求 `primaryReasoning` 显式连接失败证据、代码行为和第一教学优先级
- **AND** prompt SHALL 要求次要或干扰信号说明为什么不是主因
- **AND** prompt SHALL 继续禁止完整代码、隐藏测试猜测、替换式修法和可执行控制结构

#### Scenario: 无效候选 prompt 安全回落

- **WHEN** `AI_EVAL_SINGLE_CALL_PROMPT_VERSION` 指向未知 prompt
- **THEN** runtime SHALL 回落到默认 `diagnosis-and-teaching-v3`
- **AND** 报告 SHALL 记录实际使用的默认 promptVersion，而不是误标未知候选版本

#### Scenario: single-call 版本族统一标记 runtime mode

- **WHEN** promptVersion 为 `diagnosis-and-teaching-v1`、`v2`、`v3` 或后续同前缀版本
- **THEN** `AiInvocation.runtimeMode` SHALL 标记为 `single-call`
- **AND** comparison report SHALL 能用 promptVersion 区分 baseline/candidate 实验

#### Scenario: 报告顶层保留单一候选 prompt 归因

- **WHEN** 一次 live eval 的所有 entry 都使用同一个实际 promptVersion
- **THEN** 顶层 `LiveModelEvalReport.promptVersion` SHALL 记录该实际版本
- **AND** 顶层报告 SHALL NOT 固定写成 `mixed`

#### Scenario: 多 prompt 混跑才标记 mixed

- **WHEN** 一次 live eval 的 entry 包含多个不同实际 promptVersion
- **THEN** 顶层 `LiveModelEvalReport.promptVersion` SHALL 标记为 `mixed`
- **AND** comparison report SHALL 继续依赖顶层 promptVersion 区分单候选实验与混合实验

#### Scenario: 报告顶层保留单一 runtime profile 归因

- **WHEN** 一次 live eval 的所有 entry 都使用同一个实际 runtimeProfile
- **THEN** 顶层 `LiveModelEvalReport.runtimeProfile` SHALL 记录该实际 profile
- **AND** 顶层报告 SHALL NOT 仅依赖环境变量默认值

#### Scenario: 多 runtime profile 混跑才标记 mixed

- **WHEN** 一次 live eval 的 entry 包含多个不同实际 runtimeProfile
- **THEN** 顶层 `LiveModelEvalReport.runtimeProfile` SHALL 标记为 `mixed`
- **AND** comparison report SHALL 能区分单 profile 实验与混合 profile 实验

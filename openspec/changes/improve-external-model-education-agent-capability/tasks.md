## 1. OpenSpec 与能力边界

- [x] 1.1 运行 `openspec validate improve-external-model-education-agent-capability --strict`，确认 proposal/design/spec/tasks 可用。
- [x] 1.2 确认本轮不新增本地规则诊断器、不新增学生端 DTO、不写入任何 API key。

## 2. 外接模型教育判断结构

- [x] 2.1 扩展 `ExternalModelStagePayloads.DiagnosisJudgeOutput`，新增主错因理由、次要问题、干扰信号说明、教学优先级、提升方向和下一步学习动作字段。
- [x] 2.2 扩展 normalizer/validator，使新增字段可规范化、可安全校验，并保留现有标签与证据校验。
- [x] 2.3 扩展响应组装逻辑，让有效模型教育判断能进入最终 `studentFeedback`，缺失完整反馈时保持 partial 语义。

## 3. 标准库与 prompt 协议

- [x] 3.1 扩展 `StandardLibraryPack`，新增 `educationAgentProtocol`，包含主错因优先级、证据、次要信号、提升建议和安全表达规则。
- [x] 3.2 更新 `StandardLibraryPackBuilder`，把标准库表达为外接模型教学协议，而不是只给本地规则标签。
- [x] 3.3 更新 `PromptTemplateRegistry` 的外接模型 prompt，明确教育 agent 判断顺序并降低机械填表压力。

## 4. 外接模型能力评测

- [x] 4.1 扩展 `LiveModelEvalReport` entry/summary，记录真实模型教育 agent 字段是否完成。
- [x] 4.2 更新 live eval 聚合逻辑，只把 `modelCompleted=true` 且 `fallbackUsed=false` 的教育判断计入模型能力分。
- [x] 4.3 增加无 API key 的结构测试，覆盖 fallback 不计入模型能力、partial 不计入完整学生反馈能力。

## 5. 验证与安全检查

- [x] 5.1 运行相关后端 Maven 测试。
- [x] 5.2 运行 `git diff --check`。
- [x] 5.3 运行精确 secret scan，确认没有 ModelScope token、Authorization 或 Bearer 泄露。
- [x] 5.4 如本地存在 `AI_EVAL_API_KEY`，先跑 2 条 smoke；只有显式 full eval 环境变量存在时才跑 14 条复杂 live eval。
- [x] 5.5 更新任务状态，确认 change 可 apply/validate。

## 6. 第二阶段：输出预算与可完成度打磨

- [x] 6.1 收缩单调用 prompt，避免要求模型重复输出 `teachingHint`、`studentFeedback` 两套教学表达，降低 length 截断概率。
- [x] 6.2 允许模型在单调用中只输出教育判断与学生反馈；后端从模型 `nextLearningAction` 派生内部教学提示，不把该路径记为 fallback。
- [x] 6.3 增强测试：覆盖无 `teachingHint` 但模型学生反馈完整时仍为 `MODEL_COMPLETED`，并确保旧兼容路径仍可用。
- [x] 6.4 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 7. 第三阶段：真实 live 安全与证据稳定性打磨

- [x] 7.1 跑 14 条复杂 live 代表样本，识别真实外接模型能力瓶颈：配额不足、输出截断、证据扎根不足和安全风险自评分抖动。
- [x] 7.2 强化标准库与 prompt：要求输入格式类问题引用最具体 `generator:*` / candidate evidenceRef，同时禁止直接给循环结构或替换式修法。
- [x] 7.3 移除 runtime report 中的原始代码片段，只保留 evidenceRefs，避免评测把报告里的源码片段误判为模型泄题。
- [x] 7.4 新增模型输出安全策略：文本命中泄题触发词时强拦；仅模型自报 `answerLeakRisk=HIGH` 但学生可见文本安全时校准为 `MEDIUM`。
- [x] 7.5 补充 normalizer 与端到端 runtime 测试，验证安全文本的高风险自评分不会误伤真实模型完成度，真实泄题仍触发安全失败。

## 8. 第四阶段：默认低延迟可用性打磨

- [x] 8.1 基于真实 live 报告识别新瓶颈：单条质量 6/6 但请求约 22.9KB、推理 chunk 过多、耗时超过 35 秒预算。
- [x] 8.2 新增 `auto` runtime profile：小请求保留完整上下文，大代码、长片段或多信号请求自动启用 compact 输入包。
- [x] 8.3 将外接模型 runtime 默认 profile 从 `standard` 调整为 `auto`，让学生端真实调用自动获得请求体压缩，而不是依赖手工环境变量。
- [x] 8.4 补充 runtime plan 与端到端服务测试，验证 `auto` 小样本不压缩、大样本自动 `requestCompact=true`，并且仍能完成模型学生反馈。
- [x] 8.5 更新 live eval 默认报告口径，未显式配置 `AI_EVAL_RUNTIME_PROFILE` 时按 `auto` 标注，避免误把默认调用显示为 `standard`。

## 9. 第五阶段：默认 auto 链路离线报告打磨

- [x] 9.1 扩展 offline runtime profile eval report，新增 `autoRequestBytes`、`autoRequestCompact`、`autoCompressionRatio`、`autoQualityPreserved` 和 `autoFailureReasons` 等默认链路字段。
- [x] 9.2 更新 offline runtime profile eval factory，同时生成 `standard`、`low-latency`、`auto` 三类请求大小，并单独统计 auto 压缩条数与证据锚点质量。
- [x] 9.3 补充复杂样本与小样本回归测试：复杂 50 行以上代码默认 auto 压缩，小样本默认 auto 不盲目压缩。
- [x] 9.4 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。
- [x] 9.5 生成离线 runtime profile 报告，确认默认 auto 请求体变轻且报告不包含原始 prompt、请求消息或密钥。

## 10. 第六阶段：模型原生教学判断质量评测

- [x] 10.1 扩展 OpenSpec requirement，明确 live eval 必须评估外接模型教育 agent 判断质量，而不仅是字段完整性。
- [x] 10.2 扩展 `ComplexDiagnosisQualityScorer`，新增 education agent quality 指标：主错因解释扎根、当前优先级清楚、次要信号不过重、下一步可观察、安全边界。
- [x] 10.3 扩展 `LiveModelEvalReport` 和聚合逻辑，输出 education agent quality 分数、通过/失败指标和汇总分布。
- [x] 10.4 补充无 API key 的结构测试，覆盖真实模型完成计分、fallback 不计分、字段完整但质量不足会失败。
- [x] 10.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 11. 第七阶段：prompt schema 与教育判断 DTO 对齐

- [x] 11.1 扩展 OpenSpec requirement，要求 `diagnosis-and-teaching-v3` 的 schema 明确列出后端可接收的教育判断字段。
- [x] 11.2 更新 `PromptTemplateRegistry`，在 `diagnosisDecision` schema 中展示 `primaryReasoning`、`secondaryIssues`、`distractorNotes`、`teachingPriority`、`improvementOpportunities` 和 `nextLearningAction`。
- [x] 11.3 补充 prompt 合约测试，防止 rules 要求的字段不在 schema 中展示。
- [x] 11.4 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 12. 第八阶段：live report 教育判断可观察性打磨

- [x] 12.1 扩展 OpenSpec requirement，要求 live eval report 暴露可复盘的模型教育判断明细。
- [x] 12.2 扩展 `LiveModelEvalReport.ModelOutput`，记录教育判断来源、主错因说明、教学优先级、次要信号、提升方向、下一步动作和证据引用。
- [x] 12.3 更新 live eval 聚合逻辑，从通过安全校验的 `studentFeedback` 提取观察到的教育判断，并明确来源为 `studentFeedback`，不伪造成原始 `diagnosisDecision`。
- [x] 12.4 补充结构测试，验证报告能复盘模型教育判断，且 fallback/异常不会补写教育判断明细。
- [x] 12.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 13. 第九阶段：模型原生教育判断 trace 打磨

- [x] 13.1 扩展 OpenSpec requirement，要求响应保留精简 `modelEducationTrace`，用于教师/研发层复盘模型原生 `diagnosisDecision`。
- [x] 13.2 扩展 `SubmissionAnalysisResponse`，新增兼容字段 `modelEducationTrace`，只保存标签、证据、主因说明、取舍说明、提升分类和下一步动作。
- [x] 13.3 更新 `AiReportService`，在模型诊断决策通过校验时写入 `modelEducationTrace`，fallback 不补写该字段。
- [x] 13.4 更新 live eval report 聚合逻辑，优先使用 `modelEducationTrace`，没有 trace 时兼容退回 `studentFeedback`。
- [x] 13.5 补充结构测试，验证 trace 来自模型原生 `diagnosisDecision`，且报告来源标注正确。
- [x] 13.6 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 14. 第十阶段：标准库判断校准样例打磨

- [x] 14.1 扩展 OpenSpec requirement，要求标准库提供少量模型判断校准样例，帮助外接模型稳定主次取舍。
- [x] 14.2 扩展 `StandardLibraryPack`，新增 `judgmentCalibrationExamples`，记录适用场景、应选主因、不应抢占主因的信号、推理范式、下一步动作范式和安全提升分类。
- [x] 14.3 更新 `StandardLibraryPackBuilder`，加入输入读取、样例特判和大规模复杂度三类代表性校准样例。
- [x] 14.4 更新 compact 标准库，默认 `auto` 压缩请求仍保留精简校准样例。
- [x] 14.5 更新 `diagnosis-and-teaching-v3` prompt，要求模型使用校准样例作为判断范式，但证据不一致时不得照抄。
- [x] 14.6 补充结构测试并复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 15. 第十一阶段：模型原生 trace 质量评分打磨

- [x] 15.1 扩展 OpenSpec requirement，要求 live eval 直接评估 `modelEducationTrace`，避免后端组装后的 `studentFeedback` 稀释模型原生能力。
- [x] 15.2 扩展 `ComplexDiagnosisQualityScorer`，新增原生 trace 指标：主因解释扎根、教学优先级清楚、次要信号平衡、下一步可观察、安全边界。
- [x] 15.3 扩展 `LiveModelEvalReport` entry/summary/qualityScore，输出 `modelTraceQualityScore`、通过/失败指标和聚合分布。
- [x] 15.4 扩展 live quality baseline factory 与 regression gate，支持 `modelTraceQualityPassed` 和 `modelTraceMetric:*` 回归保护。
- [x] 15.5 补充结构测试并复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 16. 第十二阶段：原生 trace 评分指标反哺 prompt/标准库

- [x] 16.1 扩展 OpenSpec requirement，要求标准库把原生 trace 质量指标转化为外接模型可执行的自检协议。
- [x] 16.2 扩展 `StandardLibraryPack.EducationAgentProtocol`，新增 `nativeTraceQualityChecklist`。
- [x] 16.3 更新 `StandardLibraryPackBuilder` 与 compact 标准库，让完整/压缩链路都保留原生 trace 自检清单。
- [x] 16.4 更新 `diagnosis-and-teaching-v3` prompt，要求模型输出前按 `nativeTraceQualityChecklist` 自检 `diagnosisDecision`。
- [x] 16.5 补充 prompt/标准库结构测试并复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 17. 第十三阶段：外接模型迭代对比报告

- [x] 17.1 扩展 OpenSpec requirement，要求 live eval 支持不同模型、prompt 和 runtime profile 的对比报告。
- [x] 17.2 新增 `LiveModelEvalComparisonReport` 与 factory，按 caseId 对齐 baseline/candidate 并输出质量 delta、逐 case regression/improvement signals。
- [x] 17.3 将 comparison report 接入 `AI_EVAL_MODEL_BASELINE_REPORT` 路径，baseline regression 之外额外输出 `live-model-eval-comparison`。
- [x] 17.4 补充无 API key 的结构测试，验证候选 prompt 进步、fallback 退化和缺失 case 都会被正确识别。
- [x] 17.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 18. 第十四阶段：comparison 结果反哺 prompt/标准库迭代

- [x] 18.1 扩展 OpenSpec requirement，要求 comparison report 输出下一轮迭代建议。
- [x] 18.2 扩展 `LiveModelEvalComparisonReport`，新增 `iterationAdvice`、`priorityActions`、`promptActions`、`standardLibraryActions`、`runtimeActions` 和 `evalDataActions`。
- [x] 18.3 更新 comparison factory，将 fallback、latency、缺失 case 和 `modelTraceMetricFailDelta` 映射成结构化 action。
- [x] 18.4 补充结构测试，覆盖候选可晋级、fallback/latency 退化、安全边界退化、主次优先级退化的建议生成。
- [x] 18.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 19. 第十五阶段：可复现 prompt/profile 实验选择层

- [x] 19.1 扩展 OpenSpec requirement，要求 live eval 支持通过环境变量选择单调用 prompt 候选版本，并记录实际使用版本。
- [x] 19.2 扩展 `ExternalModelAgentRuntime.prepare`，支持 `singleCallPromptVersion` override，无效版本安全回落默认 v3。
- [x] 19.3 扩展 `AiReportService` 配置 `ai.external-single-call-prompt-version`，并让 `AiInvocation.promptVersion` 记录 runtimePlan 实际 prompt。
- [x] 19.4 扩展 `ModelDiagnosisEvalTest`，将 `AI_EVAL_SINGLE_CALL_PROMPT_VERSION` 注入 live eval 服务。
- [x] 19.5 补充无 API key 测试，验证 prompt override、生效版本记录、无效版本回落和 single-call runtimeMode 标记。
- [x] 19.6 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 20. 第十六阶段：report-level prompt 实验归因打磨

- [x] 20.1 扩展 OpenSpec requirement，要求 live eval 顶层报告记录单一实际 promptVersion，只有多 prompt 混跑才标记 `mixed`。
- [x] 20.2 更新 live eval report 汇总逻辑，从 entry 的实际 promptVersion 归纳顶层 promptVersion。
- [x] 20.3 补充无 API key 结构测试，覆盖单一候选 prompt 和多 prompt 混跑两种报告归因。
- [x] 20.4 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 21. 第十七阶段：report-level runtime profile 实验归因打磨

- [x] 21.1 扩展 OpenSpec requirement，要求 live eval 顶层报告记录单一实际 runtimeProfile，只有多 profile 混跑才标记 `mixed`。
- [x] 21.2 更新 live eval report 汇总逻辑，从 entry 的实际 runtimeProfile 归纳顶层 runtimeProfile，避免环境变量默认值误导 comparison。
- [x] 21.3 补充无 API key 结构测试，覆盖单一 profile 和多 profile 混跑两种报告归因。
- [x] 21.4 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 22. 第十八阶段：runtime budget 实验元数据打磨

- [x] 22.1 扩展 OpenSpec requirement，要求 live eval 与 comparison report 保留 timeoutSeconds 和 maxOutputTokens。
- [x] 22.2 扩展 `LiveModelEvalReport`，记录本轮 live eval 的 timeout 与输出 token 预算。
- [x] 22.3 扩展 `LiveModelEvalComparisonReport` 与 factory，记录 baseline/candidate 的 timeout 与输出 token 预算。
- [x] 22.4 补充无 API key 结构测试，验证 live report 和 comparison report 都能复盘 runtime budget 差异。
- [x] 22.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 23. 第十九阶段：输出截断迭代建议打磨

- [x] 23.1 扩展 OpenSpec requirement，要求 comparison report 将 `MODEL_PARTIAL_COMPLETED`、`streamFinishReason=length` 和 `OUTPUT_TRUNCATED` 映射为输出预算退化信号。
- [x] 23.2 更新 `LiveModelEvalComparisonReportFactory`，逐 case 识别 candidate 输出预算受限，并阻止候选报告晋级为 baseline。
- [x] 23.3 扩展 `iterationAdvice`，为输出截断同时生成 runtime budget 修复建议和 prompt schema 压缩建议。
- [x] 23.4 补充结构测试，覆盖 candidate 因 length/OUTPUT_TRUNCATED 退化时的 regression signal、blocked reason 与 action。
- [x] 23.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 24. 第二十阶段：输出截断证据粒度打磨

- [x] 24.1 扩展 OpenSpec requirement，要求 output budget regression signal 包含 status、streamFinishReason 或 failureReason 等具体证据。
- [x] 24.2 更新 `LiveModelEvalComparisonReportFactory`，把 candidate 的输出截断证据写入逐 case regression signal。
- [x] 24.3 补充结构测试，验证 regression signal 与 iteration action evidenceSignals 均包含 `streamFinishReason=length` 和 `failureReason=OUTPUT_TRUNCATED`。
- [x] 24.4 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 25. 第二十一阶段：安全边界退化证据粒度打磨

- [x] 25.1 扩展 OpenSpec requirement，要求 safety boundary regression signal 包含 `safetyPassed=false`、`answerLeakRisk=HIGH`、`SAFETY_RISK` 或 `nativeSafetyBoundary` 等具体证据。
- [x] 25.2 更新 `LiveModelEvalComparisonReportFactory`，把 candidate 的安全边界退化写入逐 case regression signal，并阻止 candidate 晋级为新 baseline。
- [x] 25.3 扩展 `iterationAdvice`，让安全边界退化直接映射到 prompt 不泄题边界和标准库 `safetyBoundaryRules` 调整建议。
- [x] 25.4 补充结构测试，验证安全退化 signal、blocked reason、prompt action 和 standardLibrary action 都带有可复盘证据。
- [x] 25.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 26. 第二十二阶段：安全退化样式分类打磨

- [x] 26.1 扩展 OpenSpec requirement，要求 safety boundary regression signal 标注泄题样式类别。
- [x] 26.2 更新 `LiveModelEvalComparisonReportFactory`，从 candidate 的 `modelOutput`、`studentFeedback` 和 `outputSummary` 中识别完整代码、直接修法、隐藏测试猜测和公式/结构泄露。
- [x] 26.3 补充结构测试，验证 regression signal 与 iteration action evidenceSignals 均保留 `safetyCategories`。
- [x] 26.4 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 27. 第二十三阶段：安全退化类别化修复建议打磨

- [x] 27.1 扩展 OpenSpec requirement，要求 `safetyCategories` 进一步映射为类别化 prompt 或标准库修复动作。
- [x] 27.2 更新 `LiveModelEvalComparisonReportFactory`，为完整代码泄露、直接修法泄露、隐藏测试猜测和公式/结构泄露分别生成修复建议。
- [x] 27.3 补充结构测试，验证 promptActions 与 standardLibraryActions 均包含对应类别化 action，并保留原始 evidenceSignals。
- [x] 27.4 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 28. 第二十四阶段：单份 live report 安全类别分布打磨

- [x] 28.1 扩展 OpenSpec requirement，要求 live eval entry 和顶层报告记录安全退化类别分布。
- [x] 28.2 新增 `LiveModelEvalSafetyCategoryClassifier`，让 live report 与 comparison report 共用同一套安全类别口径。
- [x] 28.3 扩展 `LiveModelEvalReport`，新增 entry 级 `safetyCategories` 和 report 级 `safetyCategoryCounts` 兼容字段。
- [x] 28.4 更新 live eval 聚合逻辑，只在安全失败、高风险或 `SAFETY_RISK` 时记录类别，并汇总分布。
- [x] 28.5 补充无 API key 结构测试，验证单份报告能显示安全类别分布。
- [x] 28.6 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 29. 第二十五阶段：comparison 安全类别分布对比打磨

- [x] 29.1 扩展 OpenSpec requirement，要求 comparison report 比较 baseline/candidate 的 `safetyCategoryCounts`。
- [x] 29.2 扩展 `LiveModelEvalComparisonReport`，在 `QualitySnapshot` 保留安全类别分布，并在 `QualityDelta` 输出 `safetyCategoryCountDelta`。
- [x] 29.3 更新 `LiveModelEvalComparisonReportFactory`，将安全类别计数增加转换为 regression signal、blocked reason 和类别化 action。
- [x] 29.4 补充结构测试，验证候选安全类别数量增加时不能晋级 baseline，并产生对应 prompt action。
- [x] 29.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 30. 第二十六阶段：安全类别减少提升信号打磨

- [x] 30.1 扩展 OpenSpec requirement，要求安全类别计数减少时输出 improvement signal。
- [x] 30.2 更新 `LiveModelEvalComparisonReportFactory`，将 `safetyCategoryCountDelta < 0` 转换为 `safetyCategoryCount <category> -N` improvement signal。
- [x] 30.3 补充结构测试，验证候选安全类别减少且没有其他 regression 时可晋级新 baseline。
- [x] 30.4 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 31. 第二十七阶段：教育判断与学生反馈质量对比打磨

- [x] 31.1 扩展 OpenSpec requirement，要求 `educationAgentQualityAverageScore` 与 `studentFeedbackQualityAverageScore` 的变化进入 comparison recommendation。
- [x] 31.2 更新 `LiveModelEvalComparisonReport` 与 factory，输出逐 case 教育判断质量 delta，并在 console summary 暴露教育/学生反馈平均分变化。
- [x] 31.3 更新 `LiveModelEvalComparisonReportFactory`，将教育 agent、学生反馈和综合智能质量提升映射为 improvement signal，将明显退化映射为 regression signal 与 blocked reason。
- [x] 31.4 扩展 `iterationAdvice`，把教育判断退化映射到 prompt 教师式判断顺序和标准库校准样例，把学生反馈退化映射到学生可执行反馈修复。
- [x] 31.5 补充结构测试，验证教育/学生反馈质量提升可晋级，退化会阻止 candidate 并产生对应 action。
- [x] 31.6 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 32. 第二十八阶段：主错因决策步骤协议打磨

- [x] 32.1 扩展 OpenSpec requirement，要求 `educationAgentProtocol` 提供 `rootCauseDecisionChecklist`。
- [x] 32.2 更新 `StandardLibraryPack` 与 `StandardLibraryPackBuilder`，把主错因判断拆成定位失败证据、连接代码行为、比较候选根因、压低干扰信号和生成可观察动作五步。
- [x] 32.3 更新 compact standard library，确保默认 `auto` 压缩请求仍保留精简主错因决策步骤。
- [x] 32.4 更新 `diagnosis-and-teaching-v3` prompt，要求模型输出 `diagnosisDecision` 前应用 `rootCauseDecisionChecklist`。
- [x] 32.5 补充标准库与 prompt 合约测试，防止主错因决策步骤在 full/compact/prompt 链路中丢失。
- [x] 32.6 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 33. 第二十九阶段：主错因决策步骤应用度评测打磨

- [x] 33.1 扩展 OpenSpec requirement，要求 live eval 评估 `nativeRootCauseDecisionChecklistApplied`。
- [x] 33.2 更新 `ComplexDiagnosisQualityScorer`，把原生 trace 质量指标扩展为定位失败证据、连接代码行为、选择主因优先级、压低干扰信号和可观察下一步五项综合检查。
- [x] 33.3 补充 scorer 测试，验证字段齐全但没有体现主错因决策步骤的 trace 会失败。
- [x] 33.4 更新 live report 聚合测试，确保新增 `modelTraceMetric:*` 进入 passed/failed 计数与 summary。
- [x] 33.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 34. 第三十阶段：主错因决策步骤退化建议打磨

- [x] 34.1 扩展 OpenSpec requirement，要求 `nativeRootCauseDecisionChecklistApplied` 退化映射到专用 prompt/标准库修复建议。
- [x] 34.2 更新 `LiveModelEvalComparisonReportFactory`，将该指标退化映射为“要求模型显式应用主错因决策步骤”的 prompt action。
- [x] 34.3 更新 `LiveModelEvalComparisonReportFactory`，将该指标退化映射为“校准 rootCauseDecisionChecklist 和多信号样例”的 standard library action。
- [x] 34.4 补充 comparison 结构测试，验证 regression signal、prompt action、standardLibrary action 和 evidenceSignals 均保留该指标。
- [x] 34.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 35. 第三十一阶段：学生反馈指标退化建议打磨

- [x] 35.1 扩展 OpenSpec requirement，要求 `studentFeedbackMetricFailDelta` 进入 comparison report 和专用修复建议。
- [x] 35.2 更新 `LiveModelEvalComparisonReport`，记录学生反馈指标 fail delta 和逐 case 新失败/新通过指标。
- [x] 35.3 更新 `LiveModelEvalComparisonReportFactory`，将学生反馈指标退化映射到 prompt actions、standard library actions 和 blocked reasons。
- [x] 35.4 补充 comparison 结构测试，验证 `studentActionable` 与 `improvementOpportunityUseful` 退化会生成可执行修复建议。
- [x] 35.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 36. 第三十二阶段：教育 agent 指标退化建议打磨

- [x] 36.1 扩展 OpenSpec requirement，要求 `educationAgentMetricFailDelta` 进入 comparison report 和专用修复建议。
- [x] 36.2 更新 `LiveModelEvalComparisonReport`，记录教育 agent 指标 fail delta 和逐 case 新失败/新通过指标。
- [x] 36.3 更新 `LiveModelEvalComparisonReportFactory`，将教育 agent 指标退化映射到 prompt actions、standard library actions 和 blocked reasons。
- [x] 36.4 补充 comparison 结构测试，验证 `blockingPriorityClear` 与 `secondarySignalsBalanced` 退化会生成教师式判断修复建议。
- [x] 36.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 37. 第三十三阶段：综合智能指标退化建议打磨

- [x] 37.1 扩展 OpenSpec requirement，要求 `intelligenceMetricFailDelta` 进入 comparison report 和专用修复建议。
- [x] 37.2 更新 `LiveModelEvalComparisonReport`，记录综合智能指标 fail delta 和逐 case 新失败/新通过指标。
- [x] 37.3 更新 `LiveModelEvalComparisonReportFactory`，将综合智能指标退化映射到 prompt actions、standard library actions 和 blocked reasons。
- [x] 37.4 补充 comparison 结构测试，验证 `distractorResistance` 与 `evidenceGroundedReasoning` 退化会生成外接模型智能能力修复建议。
- [x] 37.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

## 38. 第三十四阶段：低延迟候选 prompt 打磨

- [x] 38.1 基于真实 2 条 complex live smoke，确认 v3 当前瓶颈是慢响应、输出截断和原生 trace 决策步骤不够显式。
- [x] 38.2 新增 `diagnosis-and-teaching-v4-lite` 候选 prompt，压缩重复学生反馈字段并要求短 JSON、短句、最多 1 个次要/干扰/提升项。
- [x] 38.3 扩展 runtime prompt override 白名单，让 live eval 可通过 `AI_EVAL_SINGLE_CALL_PROMPT_VERSION=diagnosis-and-teaching-v4-lite` 选择候选版本。
- [x] 38.4 补充 prompt 合约、runtime override 和服务级测试，验证 v4-lite 不改变默认 v3 且可记录实际 promptVersion。
- [x] 38.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan；如条件允许，用同 2 条样本跑 v4-lite smoke 与 v3 报告对比。

## 39. 第三十五阶段：外部运行条件阻塞归因打磨

- [x] 39.1 基于真实 v4-lite smoke 中的 `INSUFFICIENT_QUOTA` 与 `BUDGET_GUARD_OPEN`，确认该类报告不能作为 prompt 能力退化证据。
- [x] 39.2 扩展 comparison report，将配额不足、限流、超时和预算保护识别为 `external runtime blocked` regression signal。
- [x] 39.3 扩展 `iterationAdvice`，把外部运行条件阻塞映射到 runtime/eval data 动作，要求补足配额或解除预算保护后重跑同 case。
- [x] 39.4 补充结构测试，验证运行条件受阻不会被当作外接模型教育智能质量结论。
- [x] 39.5 复跑相关后端测试、OpenSpec strict、diff check 和 secret scan。

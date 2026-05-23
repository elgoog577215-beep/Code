## 1. 模型输入与标准库底座

- [x] 1.1 新增 `ModelDiagnosisBrief` 数据结构，覆盖题目摘要、关键代码片段、评测事实、候选信号、证据引用、学习轨迹和隐藏数据边界。
- [x] 1.2 新增 `ModelDiagnosisBriefBuilder`，从现有 `Problem`、`Submission`、case results、证据包和规则信号生成压缩输入。
- [x] 1.3 新增 `StandardLibraryPack` 数据结构，表达候选粗粒度错因、细粒度错因、教学动作、常见误区和安全约束。
- [x] 1.4 新增 `StandardLibraryPackBuilder`，根据规则信号和 `DiagnosisTaxonomy` 裁剪当前样本相关标准库。
- [x] 1.5 为 brief 和 standard library pack 增加单元测试，覆盖隐藏样例不泄露、候选标签裁剪和不确定性出口。

## 2. Prompt 契约与模型阶段

- [x] 2.1 新增 `PromptTemplateRegistry` 或等价服务，集中管理 prompt 版本和模板。
- [x] 2.2 新增错因裁决 prompt `diagnosis-judge-v1`，只要求模型输出标签、证据、置信度和不确定性。
- [x] 2.3 新增教学表达 prompt `teaching-hint-v1`，只基于已校验裁决生成学生提示、提示计划、学习干预计划和教师备注。
- [x] 2.4 在模型请求中加入输出长度控制和短 JSON 输出要求，避免长篇 reasoning 或报告拖慢 live eval。
- [x] 2.5 为 prompt 模板增加快照测试或结构测试，确保包含输入 schema、输出 schema、标准库边界和安全约束。

## 3. Runtime 编排与校验

- [x] 3.1 新增 `ExternalModelAgentRuntime`，按 brief -> standard library pack -> 裁决 -> 校验 -> 教学表达 -> 校验的顺序编排模型调用。
- [x] 3.2 新增 `ModelOutputValidator`，校验 JSON 合法性、标签合法性、证据引用、字段完整性和泄题风险。
- [x] 3.3 将模型阶段失败、超时、格式错误、校验失败和安全失败统一映射为可记录的 failure reason。
- [x] 3.4 将 runtime 渐进接入现有 `DiagnosticAgentService` 或 `AiReportService`，保留旧路径作为配置回滚。
- [x] 3.5 确保任何回退都写入 `fallbackUsed=true`、`status=RULE_FALLBACK` 和对应失败原因。

## 4. Live Eval 报告

- [x] 4.1 新增 live model eval report 数据结构，记录 caseId、model、promptVersion、stage、latencyMs、status、fallbackUsed、JSON 合规、标签命中、证据校验、安全结果和失败原因。
- [x] 4.2 改造 live model eval，让批量样本逐条产出报告，不因单条失败丢失整体结果。
- [x] 4.3 增加 small smoke live eval，仅跑少量样本验证外部模型真实参与。
- [x] 4.4 增加完整 live eval 的显式开关，避免日常测试误触发高成本外部模型调用。
- [x] 4.5 将 live eval 报告保存到可查看位置，并在测试输出中打印汇总：成功率、回退率、超时数、标签命中率和安全通过率。

## 5. 回归验证与文档

- [x] 5.1 跑现有离线诊断评测，确认本地规则 agent 能力不退化。
- [x] 5.2 使用已配置 ModelScope key 跑 small smoke live eval，记录真实外部模型表现。
- [x] 5.3 更新长期 AI spec 或新增阶段文档，说明外部模型 runtime 的新架构、限制和后续优化方向。
- [x] 5.4 检查静态资源、密钥和生成报告不被误提交。
- [x] 5.5 运行 `openspec validate external-model-education-agent-runtime --strict` 并修复所有问题。

## 6. 在线外部模型路径接入

- [x] 6.1 为 `AiReportService` 注入 `ExternalModelAgentRuntime`，并新增 `ai.external-runtime-enabled` 配置开关，默认启用新 runtime。
- [x] 6.2 将提交诊断的外部模型增强优先切换到 `diagnosis-judge-v1` 阶段，解析并校验 `DiagnosisJudgeOutput`。
- [x] 6.3 阶段 A 通过后调用 `teaching-hint-v1` 阶段，解析并校验 `TeachingHintOutput`。
- [x] 6.4 将两阶段模型结果合成回 `SubmissionAnalysisResponse`，保留本地规则的行级问题、学习轨迹和已有证据。
- [x] 6.5 阶段失败时显式返回规则兜底状态，确保 live eval 能看到 fallback，而不是静默退回旧长 prompt。
- [x] 6.6 增加单元测试覆盖新 runtime 成功、阶段 A 失败、阶段 B 安全失败和配置关闭回旧路径。
- [x] 6.7 跑 small smoke live eval，对比新 runtime 与旧长 prompt 的在线表现。

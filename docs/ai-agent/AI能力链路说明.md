# AI 能力链路说明

## 1. 文档目的

本文档用于说明当前项目的 AI agent 能力到底如何工作。它不是单个提示词说明，而是把学生提交代码后，从判题结果、证据构建、规则信号、标准库约束、外部模型调用、输出校验、学生端展示、教师端统计到评测闭环的完整过程串起来。

后续优化 AI 能力时，应优先回到这条链路判断：本轮到底增强了哪一层能力，是否让外部大模型在真实教学场景里更准确、更稳定、更可解释。

本文档有三个用途：

- 现状地图：说明当前 AI agent 的真实链路，避免把能力误解成单个 prompt 或单次模型调用。
- 调试索引：当线上反馈不准、不稳、太慢或走本地兜底时，能快速定位应该看哪一层。
- 迭代准绳：后续优化必须说明增强了哪一层、影响了哪些字段、如何验证、是否会误伤现有能力。

## 2. 总体链路

```text
学生提交代码
-> 判题结果与测试点事实
-> SubmissionAnalysisService 触发分析
-> DiagnosticAgentService 构建诊断上下文
-> EvidencePackage 与 RuleSignalAnalyzer 生成证据和候选信号
-> ModelDiagnosisBrief 压缩为模型输入
-> StandardLibraryPack 注入标准库约束
-> PromptTemplateRegistry 选择提示词模板
-> AiReportService 调用外部模型 route
-> ModelOutputValidator 校验模型输出
-> 合并规则结果、模型结果、学习记忆与兜底逻辑
-> 生成学生提示、教师说明、学习干预计划和 AI 调用状态
-> 学生端展示、教师端统计、Coach 追问、live eval 评测闭环
```

这条链路体现了当前项目的基本判断：教育 AI agent 的能力不只来自基础模型，而来自模型调用前后的结构化工程。证据越清楚、标准库越稳定、提示词越可执行、输出校验越严格、评测闭环越真实，外部大模型越容易稳定发挥。

## 3. 核心过程

### 3.1 提交与分析入口

学生提交代码后，系统先完成普通在线判题，得到编译结果、运行结果、测试点结果、错误输出、耗时和内存等事实。

随后 `SubmissionAnalysisService` 进入分析流程，负责把题目、提交、测试点结果交给诊断 agent，并最终保存分析结果。

关键文件：

- `online-judge/src/main/java/com/onlinejudge/submission/application/SubmissionAnalysisService.java`
- `online-judge/src/main/java/com/onlinejudge/submission/application/SubmissionAnalysisAsyncService.java`

### 3.2 证据包与规则信号

`DiagnosticAgentService` 不会直接把整段代码原样扔给模型。它会先构造诊断证据包，并调用规则信号分析器生成候选判断。

这一层主要负责回答：

- 当前提交是 CE、RE、WA、TLE、MLE 还是 AC 后复盘。
- 哪些代码片段、错误输出、测试点事实最值得模型关注。
- 有哪些候选错因标签。
- 哪些判断来自当前提交，哪些只是历史学习记忆的辅助校准。
- 是否存在答案泄露风险、隐藏数据边界或不确定性。

关键文件：

- `online-judge/src/main/java/com/onlinejudge/submission/application/DiagnosticAgentService.java`
- `online-judge/src/main/java/com/onlinejudge/submission/application/RuleSignalAnalyzer.java`

### 3.3 模型输入 brief

外部模型不直接消费完整后端对象，而是消费经过压缩和选择的 `ModelDiagnosisBrief`。这一步的作用是降低 prompt 噪声，把模型注意力集中到最有诊断价值的信息上。

`ModelDiagnosisBrief` 通常包含：

- 题目摘要。
- 学生代码的关键片段。
- 第一个失败测试点或代表性失败事实。
- 可见测试点事实。
- 规则层候选信号。
- 证据引用 `evidenceRefs`。
- 学习记忆校准 `memoryCalibration`。
- 隐藏数据边界。
- 不确定性说明。

关键文件：

- `online-judge/src/main/java/com/onlinejudge/submission/application/ModelDiagnosisBrief.java`
- `online-judge/src/main/java/com/onlinejudge/submission/application/ModelDiagnosisBriefBuilder.java`
- `online-judge/src/main/java/com/onlinejudge/submission/application/MemoryEvidencePolicy.java`

### 3.4 标准库约束

`StandardLibraryPack` 是当前 agent 的重要边界。它把模型输出限制在项目认可的教育诊断体系里，避免模型随意创造标签、随意定义错因或给出不可控教学动作。

标准库主要包含：

- `issueTags`：粗粒度错因标签。
- `fineGrainedTags`：细粒度错因标签。
- `teachingActions`：允许的教学动作。
- guardrail rules：证据引用、记忆使用、答案泄露和不确定性规则。
- 学生任务模板：把提示落到学生可执行的下一步。

这一层的价值是：把“模型觉得哪里错”转成“系统可以统计、评测、复用和展示的结构化结果”。

关键文件：

- `online-judge/src/main/java/com/onlinejudge/submission/application/StandardLibraryPack.java`
- `online-judge/src/main/java/com/onlinejudge/submission/application/StandardLibraryPackBuilder.java`

### 3.5 提示词模板

`PromptTemplateRegistry` 管理诊断和教学相关提示词。当前核心模板包括：

- `diagnosis-judge-v2`
- `teaching-hint-v1`
- `diagnosis-and-teaching-v2`

提示词模板的关键要求是：

- 只输出严格 JSON。
- 不输出思维链。
- 错因标签、细粒度标签、教学动作必须来自标准库。
- 必须引用给定证据。
- 不允许泄露标准答案。
- 不允许把历史记忆当成当前错误证据。
- 不确定时要降低置信度并说明边界。

这一层的目标不是让模型“多说”，而是让模型在证据、标签、教学动作和安全边界内完成判断。

关键文件：

- `online-judge/src/main/java/com/onlinejudge/submission/application/PromptTemplateRegistry.java`
- `online-judge/src/main/java/com/onlinejudge/submission/application/ExternalModelAgentRuntime.java`

### 3.6 外部模型调用

`AiReportService` 负责真实调用外部大模型。它支持：

- 主模型与备用模型。
- route pool。
- stream 调用。
- stream fallback。
- timeout。
- budget guard。
- single-call runtime。
- runtime failure reason 记录。
- AI 调用状态归因。

当前默认配置位于 `application.yml`：

- `ai.base-url`
- `ai.api-key`
- `ai.model`
- `ai.fallback-model`
- `ai.routes`
- `ai.timeout-seconds`
- `ai.external-runtime.mode`
- `ai.external-runtime.stream-enabled`
- `ai.external-runtime.stream-fallback-enabled`

这层需要重点区分三件事：

- 接口是否可达。
- 模型是否真实返回完成内容。
- 最终诊断是否真的来自外部模型，而不是本地兜底。

如果这三件事不分清，就容易把本地规则兜底误判成外部模型能力。

关键文件：

- `online-judge/src/main/java/com/onlinejudge/submission/application/AiReportService.java`
- `online-judge/src/main/resources/application.yml`

### 3.7 输出校验

模型输出不会直接信任。`ModelOutputValidator` 会校验：

- JSON 结构是否完整。
- `issueTag` 是否来自标准库。
- `fineGrainedTags` 是否来自标准库。
- `teachingAction` 是否来自标准库。
- `evidenceRefs` 是否引用了真实给定证据。
- 是否存在高风险答案泄露。
- 是否把历史记忆作为当前错误的唯一依据。
- 是否出现提示词要求之外的输出。

校验失败后，系统会进入降级、兜底或部分保留逻辑。这个机制保证了外部模型再强也不能绕过项目规则。

关键文件：

- `online-judge/src/main/java/com/onlinejudge/submission/application/ModelOutputValidator.java`

### 3.8 结果合并与学习记忆

诊断结果不是纯模型输出，而是规则信号、模型输出、当前证据、历史记忆和安全策略的合并结果。

这一层会处理：

- 当前提交的主要错因。
- 学生可见提示。
- 教师可见说明。
- 学习干预计划。
- 学生是否反复卡在同类错误。
- 是否从 CE 推进到 WA、从失败推进到 AC，或从 AC 回退。
- 历史记忆是否只能作为校准，不能替代当前证据。

这也是项目从“单次提交错因分析”走向“多次提交学习轨迹分析”的关键基础。

关键文件：

- `online-judge/src/main/java/com/onlinejudge/submission/application/DiagnosticAgentService.java`
- `online-judge/src/main/java/com/onlinejudge/submission/application/MemoryEvidencePolicy.java`

### 3.9 学生端展示

学生端会看到分析结果，并且能区分分析来源。

前端会根据 `aiInvocation` 显示：

- 外部模型完成。
- 外部模型部分完成。
- 本地兜底。

这个设计的价值是可观测性：学生、老师和开发者都能知道当前 AI 反馈到底是不是由外部模型完成。

关键文件：

- `online-judge/frontend/src/features/problem/ProblemPage.tsx`
- `online-judge/frontend/src/shared/api/types.ts`

### 3.10 Coach 追问

Coach agent 是在诊断之后进一步教育化的能力。它不是只告诉学生哪里错，而是基于诊断结果、证据引用、学习上下文生成追问或引导。

这条链路重点关注：

- 学生是否理解提示。
- 下一步应该让学生观察什么。
- 是否需要追问边界条件、变量含义、循环不变量或复杂度。
- 如何避免直接给答案。

关键文件：

- `online-judge/src/main/java/com/onlinejudge/submission/application/CoachAgentService.java`
- `online-judge/src/main/java/com/onlinejudge/submission/application/CoachPromptService.java`

### 3.11 教师端质量统计

教师端和质量统计模块负责把 AI 效果转成可观察指标，例如：

- 模型运行失败次数。
- 模型运行失败率。
- 教师修正率。
- fallback 数量。
- 质量趋势。

这一步很重要，因为学校场景不能只看“AI 说得像不像”，还要看它是否稳定、是否经常被老师纠正、是否经常需要兜底。

关键文件：

- `online-judge/src/main/java/com/onlinejudge/submission/application/AiQualityOverviewService.java`
- `online-judge/src/main/java/com/onlinejudge/submission/application/AiQualityTrendService.java`
- `online-judge/src/main/java/com/onlinejudge/submission/application/AiQualityMetrics.java`

### 3.12 Live eval 评测闭环

评测系统用于判断当前 AI agent 是否真的变强。它不是只看单条输出，而是从多个维度记录：

- 准确率。
- 速度。
- 稳定性。
- 教育有效性。
- route 结果。
- fallback 情况。
- runtime failure reason。
- completed output rate。
- goal snapshot。
- evaluation profile。

这一层的原则是：只有外部模型真实完成输出，才应该计入外部模型质量。fallback 结果可以说明系统可用，但不能证明外部模型诊断能力强。

关键文件：

- `online-judge/src/test/java/com/onlinejudge/eval/AssistantLiveEvalReport.java`
- `online-judge/src/test/java/com/onlinejudge/eval/AssistantLiveEvalTest.java`
- `online-judge/src/test/java/com/onlinejudge/eval/AssistantLiveEvalQualityGate.java`

## 4. 关键字段流转

下面这张表用于说明“信息从哪里来，最后服务谁”。后续调试时，不要只看最终自然语言提示，要沿字段往前追溯。

| 字段或对象 | 主要来源 | 主要作用 | 下游使用方 |
| --- | --- | --- | --- |
| `caseResults` | 判题结果 | 提供 CE、RE、WA、TLE、MLE、AC 等客观事实 | 证据包、规则信号、分析结果 |
| `DiagnosisEvidencePackage` | 题目、提交、测试点、历史诊断 | 汇总当前诊断所需事实 | 规则分析、模型 brief、trace |
| `RuleSignalAnalyzer` 输出 | 本地规则 | 给出候选错因、证据锚点和兜底方向 | brief、fallback、模型提示 |
| `ModelDiagnosisBrief` | 证据包、规则信号、学习记忆 | 压缩模型输入，降低噪声 | 外部模型 prompt |
| `memoryCalibration` | 历史提交、诊断、学习动作、教师修正 | 判断记忆与当前证据是对齐、冲突还是只适合教学提示 | prompt、validator、诊断合并 |
| `StandardLibraryPack` | 项目标准库 | 限制错因标签、细粒度标签和教学动作 | prompt、validator、eval |
| `issueTags` | 模型输出或本地兜底 | 粗粒度错因归因 | 学生提示、教师统计、学习画像、评测 |
| `fineGrainedTags` | 模型输出或本地兜底 | 细粒度错因归因 | 学习轨迹、教师看板、推荐、评测 |
| `evidenceRefs` | brief 中的证据 ID | 证明模型判断来自当前证据 | validator、教师解释、eval |
| `studentHintPlan` | 模型输出或安全模板 | 给学生可执行的下一步 | 学生端、Coach、学习动作分析 |
| `learningInterventionPlan` | 模型输出或本地模板 | 给老师和系统可观察的学习任务 | 教师端、学习效果分析 |
| `teacherNote` | 模型输出或本地总结 | 给老师解释诊断依据和介入建议 | 教师端、教师修正 |
| `answerLeakRisk` | 模型自评、本地安全扫描、validator | 判断是否可能泄露答案 | 学生端安全降级、eval |
| `aiInvocation` | 外部模型调用与兜底路径 | 标识模型是否完成、是否 fallback、provider/model/promptVersion | 学生端来源展示、教师统计、live eval |
| `completedOutput` | live eval entry | 区分真实模型完成和本地兜底 | 质量统计分母、质量门禁 |
| `routeRole` | live eval route 归因 | 区分主路由、备用路由、路由池和本地兜底 | route profile、容量治理 |
| `evaluationProfile` | live eval 汇总 | 从准确率、速度、稳定性、教育有效性评估 agent | 后续优化决策 |

关键原则：

- 当前提交证据优先于历史记忆。
- 外部模型完成优先计入模型质量，本地兜底只计入系统可用性。
- 学生可见内容要严格安全，教师备注和内部字段不能直接触发学生端降级。
- 所有关键判断都应尽量落到结构化字段，而不是藏在自然语言里。

## 5. 责任边界

### 5.1 哪些能力属于本地规则

本地规则负责提供稳定底座：

- 基于判题结果识别明显错误类型。
- 给外部模型提供候选信号。
- 在模型失败时生成可用兜底提示。
- 保护学生端不泄露答案。
- 保证输出结构能被教师端、评测和学习画像读取。

本地规则的目标不是替代模型，而是让模型失败时系统仍可用，并让模型成功时输出可校验、可统计、可解释。

### 5.2 哪些能力属于外部模型

外部模型主要负责更高阶的判断和表达：

- 在多个候选错因之间做语义判断。
- 根据代码上下文解释错误原因。
- 生成学生能理解的提示。
- 生成教师可用的说明。
- 把证据、错因和教学动作组织成一份完整反馈。

外部模型不能绕过标准库、证据引用、输出校验和安全边界。

### 5.3 哪些能力属于评测系统

评测系统负责判断 agent 是否真的变强：

- 不把本地兜底算作外部模型质量。
- 不把配额失败误判成提示词质量失败。
- 不只看准确率，也看速度、稳定性和教育有效性。
- 把每次失败归因到 route、quota、validator、prompt、标准库或样本覆盖。

## 6. 质量门禁

后续每次改 AI 能力，至少要通过下面这些门禁。

| 门禁 | 必须回答的问题 | 失败时优先处理 |
| --- | --- | --- |
| 证据门禁 | 模型判断是否引用当前提交证据，而不是只引用历史记忆或自由猜测 | `ModelDiagnosisBrief`、`evidenceRefs`、`MemoryEvidencePolicy` |
| 标准库门禁 | 标签和教学动作是否来自标准库 | `StandardLibraryPack`、`PromptTemplateRegistry`、`ModelOutputValidator` |
| 安全门禁 | 学生可见内容是否泄露答案、完整代码或隐藏数据 | `ModelOutputValidator`、`HintSafetyService`、学生端展示 |
| 外部完成门禁 | 本轮输出是否真由外部模型完成 | `AiInvocation`、`completedOutput`、route attribution |
| 质量门禁 | completed output 中错因、证据、教学动作是否达标 | `AssistantLiveEvalReport.evaluationProfile.accuracy` |
| 速度门禁 | 平均、P90、P95 延迟是否可接受 | route、timeout、single-call、prompt compaction |
| 稳定性门禁 | runtime failure、fallback、quota、rate limit 是否可控 | route pool、budget guard、provider 配置 |
| 教育门禁 | 提示是否能形成学生下一步动作，是否能被老师解释和修正 | `studentHintPlan`、`learningInterventionPlan`、教师修正闭环 |

## 7. 优化决策树

当 live eval 或真实使用结果不达标时，按下面顺序判断，不要一上来就改 prompt。

```text
1. 外部模型没有完成？
   -> 看 failureReasonCounts、routeProfile、ai-route-health。
   -> 如果是 INSUFFICIENT_QUOTA / RATE_LIMITED / BUDGET_GUARD_OPEN，优先处理 route、额度、请求次数和调用节奏。

2. 外部模型完成了，但输出格式或字段不合法？
   -> 看 PromptTemplateRegistry、StandardLibraryPack、ModelOutputValidator。
   -> 优先减少歧义、压缩输入、明确 JSON schema 和允许值。

3. 输出合法，但错因不准？
   -> 看 evidencePackage、ruleSignals、brief 是否给错方向。
   -> 再看标准库标签是否过粗、过少或语义重叠。

4. 错因准，但学生提示不好？
   -> 看 teachingAction、studentHintPlan、HintSafetyService。
   -> 优先改教学动作库和提示结构，而不是只改文案。

5. 单次诊断好，但学生没有进步？
   -> 看学习记忆、Coach 追问、下一次提交结果、教师修正。
   -> 需要进入学习效果闭环，而不是继续堆单次诊断规则。

6. 教师端仍然难用？
   -> 看 teacherNote、AI 质量统计、错因聚合、班级维度和老师介入优先级。
   -> 目标是让老师知道该教谁、教什么、为什么。
```

## 8. 反模式

后续优化中要避免这些做法：

- 只改提示词文案，不补证据、标准库、validator 或评测。
- 把本地兜底命中当成外部模型命中。
- 用短代码、无 bug 代码或玩具样本评估真实教育能力。
- 把历史记忆当成当前提交的直接错因证据。
- 为了提高完成率删除安全检查或输出校验。
- 新建一套旁路记忆、旁路模型调用或旁路统计，导致诊断、Coach、教师端和评测各算各的。
- 只看准确率，不看速度、稳定性、成本和教师可解释性。
- 把教师备注、模型自评等内部字段直接当成学生端安全风险。

## 9. 验证入口与排查顺序

### 9.1 配置健康检查

优先检查外部模型路由是否具备上线条件：

```text
GET /api/system/ai-route-health
```

这个接口用于判断当前配置是否存在明显风险，例如：

- AI 功能是否关闭。
- 是否没有可用 route。
- 是否只有单 route，存在容量风险。
- 主 route、fallback route、route pool 是否配置完整。
- provider、baseUrl、model 是否可见且 key 已脱敏。

注意：route health 只说明配置是否健康，不说明模型真实诊断质量。

### 9.2 真实评测入口

真实外部模型能力要看 live eval，尤其是长代码样本、route attribution、completed output 和 evaluation profile。

建议排查顺序：

1. 先看 `target/ai-eval-reports/assistant-live-eval-*.json` 是否生成。
2. 再看 `completedOutputRate`，确认外部模型是否真实完成。
3. 再看 `failureReasonCounts`，区分 quota、rate limit、timeout、validator failure。
4. 再看 `routeProfile` 和每条 entry 的 `actualProvider`、`actualModel`、`routeRole`。
5. 只有 `completedOutput=true` 的样本，才用于判断错因、证据和教学动作质量。
6. 最后看 `evaluationProfile.dimensionGaps` 和 `goalSnapshot.nextOptimizationFocus`，决定下一轮优化方向。

### 9.3 常用验证命令

后端相关测试优先使用 Maven wrapper：

```powershell
cd C:\Users\Administrator\Desktop\温中code\online-judge
.\mvnw.cmd test
```

如果只验证 AI agent 相关链路，优先跑针对性测试，例如：

```powershell
.\mvnw.cmd -Dtest=AssistantLiveEvalTest,AssistantLiveEvalQualityGateTest test
.\mvnw.cmd -Dtest=AiReportServiceExternalRuntimeTest,ModelOutputValidatorTest,PromptTemplateRegistryTest,StandardLibraryPackBuilderTest test
```

涉及前端展示或类型字段时，再进入前端目录跑类型检查：

```powershell
cd C:\Users\Administrator\Desktop\温中code\online-judge\frontend
npm run typecheck
```

### 9.4 结果解释顺序

评测结果应按下面顺序解释：

- 如果 `completedOutputRate` 低，优先看 route、额度、限流、timeout 和 budget guard。
- 如果 completed output 多，但 `signalHitRate` 低，优先看证据包、规则候选、标准库标签和 prompt。
- 如果 `evidenceValidRate` 低，优先看 brief 的 `evidenceRefs` 与 validator。
- 如果 `teachingActionValidRate` 低，优先看教学动作标准库和输出 schema。
- 如果 `safetyPassRate` 低，优先区分学生可见字段和教师/内部字段。
- 如果速度不达标，优先看 single-call、prompt compaction、token 上限和 route 延迟。
- 如果学生学习效果无法判断，说明需要接入下一次提交、Coach 追问结果和教师修正反馈。

## 10. 当前能力判断

### 10.1 已经具备的能力

当前项目已经具备教育 agent 的主要骨架：

- 能从真实判题结果进入 AI 分析。
- 能构建证据包，而不是直接裸调用模型。
- 能生成规则候选信号，为模型提供方向。
- 能用标准库约束错因、细粒度标签和教学动作。
- 能调用外部模型，并记录 route、fallback、失败原因。
- 能校验模型输出，防止标签越界、证据乱引和答案泄露。
- 能把结果展示给学生，并标识是否来自外部模型。
- 能生成 Coach 追问。
- 能提供教师端质量统计。
- 能通过 live eval 从准确率、速度、稳定性和教育有效性代理指标等角度评估。

### 10.2 当前主要短板

当前仍然需要继续增强的地方：

- 真实学生长期学习效果还没有形成足够强的闭环指标。
- 教师修正反馈还可以更深地反哺标准库、提示词和评测集。
- 多次提交轨迹已经有基础，但还可以更强地影响下一次教学策略。
- 评测集需要继续补充真实长代码、真实错因和老师参考分析。
- 外部模型 route 的稳定性、限流恢复、降级解释还需要继续加强。
- Coach 追问与后续提交改善之间的因果关系还需要被记录和评测。
- live eval 目前能证明 completed output 的质量代理指标，但还不能直接证明长期学习效果。
- 教师端需要进一步回答“哪些学生需要介入、介入什么、为什么现在介入”。

### 10.3 后续优化优先级

后续优化应按以下顺序推进：

1. 继续扩大真实长代码评测集，优先使用老师提供的高质量样本。
2. 把教师修正沉淀成可回放的评测样本和标准库改进信号。
3. 强化学生学习记忆，让同类错误跨题、跨提交被识别。
4. 让 Coach 追问结果与下一次提交结果形成闭环。
5. 优化 route pool 和 fallback 归因，确保外部模型能力与本地兜底能力分开统计。
6. 让教师端质量统计能回答“哪类学生、哪类题、哪类错因最需要老师介入”。

## 11. 判断标准

以后每次优化 AI agent，都应回答以下问题：

- 新增了哪一个可观测状态。
- 这个状态对学生或老师有什么教学价值。
- 它进入了哪个结构化字段。
- 它引用了哪些证据。
- 它是否影响提示词、标准库、模型调用、校验、前端展示或教师统计。
- 它是否有评测样本和断言。
- 它是否区分了外部模型完成、本地兜底和部分完成。
- 它是否让学生下一次提交更可能变好。

只有同时能被系统记录、被教师理解、被学生使用、被评测验证的 AI 改动，才算是真正提升了项目的教育 agent 能力。

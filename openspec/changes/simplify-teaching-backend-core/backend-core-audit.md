# 后端主链路收敛审计

## 收敛原则

本项目的后端目标不是堆叠 AI 研究能力，而是给学校课堂提供简单、稳定、有效的支撑。

主链路只回答两类问题：

- 学生：我做哪份作业、哪道题、提交后哪里错、下一步怎么改。
- 老师：这个班谁需要关注、卡在哪里、共性问题是什么、下一步怎么介入。

分类标准：

- `keep-main`：直接服务学生或教师主动作，必须稳定、短链路、可读。
- `simplify`：有教学价值，但当前对象太大、字段太多或职责混杂，后续应拆小或降噪。
- `internal-only`：服务 AI 生成、安全、评测、研发排障或质量验证，只能作为内部支撑或系统详情。
- `remove-later`：历史兼容或旧主链路遗留，后续确认依赖后删除或归档。

## Submission 后端

### API

| 项 | 分类 | 理由 | 下一步 |
| --- | --- | --- | --- |
| `POST /api/submissions` | keep-main | 学生提交代码的入口，是判题事实链路起点。 | 保持稳定。 |
| `GET /api/submissions/{id}` | keep-main | 返回判题事实、测试点和兼容 analysis 状态。 | 后续把 `analysis` 字段标为兼容字段，学生端继续优先消费判题事实和学生 AI 反馈。 |
| `GET/POST /api/submissions/{id}/student-ai-feedback` | keep-main | 学生失败后获取模型结构化修正/提升建议。 | 保持学生端主反馈接口。 |
| `POST /api/submissions/{id}/student-ai-feedback/view` | keep-main | 记录学生是否查看反馈，服务学习轨迹。 | 保持。 |
| `GET/POST /api/submissions/{id}/analysis` | simplify | 旧 analysis 大对象仍服务教师、推荐、评测和兼容，但不应控制学生主体验。 | 后续改名或移入内部/教师诊断命名空间。 |
| `GET /api/submissions/problem/{problemId}/history-summary` | keep-main | 学生/题目历史记录摘要，可辅助做题。 | 保持轻量。 |
| `GET /api/submissions/compare` | internal-only | 更像诊断/研发/教师辅助，不是学生主流程。 | 保留但不进入学生主界面。 |
| Coach prompt endpoints | simplify | 追问有教学价值，但当前依赖旧 analysis，且容易变成额外复杂链路。 | 保留，后续统一到学生反馈后续追问能力。 |

### Application

| 项 | 分类 | 理由 | 下一步 |
| --- | --- | --- | --- |
| `JudgeService` | keep-main | 判题、保存提交、触发学生 AI 反馈。 | 保持为主链路入口。 |
| `SubmissionAnalysisService` | simplify | 同时构造判题响应、旧诊断、历史摘要、推荐回填，职责偏大。 | 下一轮拆出 `SubmissionViewService` 或 `JudgeFactsAssembler`，让 analysis 降为兼容层。 |
| `StudentAiFeedbackService` | keep-main | 学生 AI 反馈独立链路，直接服务结果弹窗。 | 保持，继续确保本地规则不生成学生可见建议。 |
| `StudentAiFeedbackAsyncService` | keep-main | 异步生成学生反馈，避免阻塞提交。 | 保持。 |
| `StudentFeedbackViewAssembler` | remove-later | 兼容旧 analysis 到学生反馈视图的桥接；学生端已经改用 `StudentAiFeedback`。 | 确认无主流程依赖后删除或仅测试保留。 |
| `StudentFeedbackAssembler` | remove-later | 旧学生反馈生成/组装遗留。 | 确认无主流程依赖后删除。 |
| `DiagnosticAgentService` / `AiReportService` | internal-only | 模型调用、诊断生成、结构化输出，服务 AI 内部能力。 | 保留内部，不暴露为主体验组织中心。 |
| `ExternalModel*` / `ModelOutput*` / `PromptTemplateRegistry` | internal-only | 模型 runtime、预算、失败归因、安全校验。 | 保留内部，状态不得进入学生/教师主文案。 |
| `DiagnosisEvidencePackage*` / `RuleSignalAnalyzer` | internal-only | 证据与候选信号，是 AI 支撑材料。 | 保留内部，本地规则不得冒充学生建议。 |
| `SubmissionComparisonService` | internal-only | 对比提交，不是主链路。 | 保留辅助。 |

### DTO

| 项 | 分类 | 理由 | 下一步 |
| --- | --- | --- | --- |
| `SubmissionRequest` / `SubmissionResponse` | keep-main | 提交和判题事实的核心契约。 | 后续降低 `analysis` 字段主链路权重。 |
| `SubmissionResponse.TestCaseResult` | keep-main | 学生需要的判题事实。 | 保持。 |
| `StudentAiFeedbackResponse` / `StudentAiFeedbackLookupResponse` | keep-main | 学生 AI 反馈主契约。 | 保持短、结构化、模型来源明确。 |
| `SubmissionAnalysisResponse` | simplify | 字段过多，混合学生、教师、研发、模型、评测、旧反馈。 | 后续拆出内部诊断 DTO 和教师摘要 DTO。 |
| `SubmissionAnalysisLookupResponse` | simplify | 旧 analysis 查询壳。 | 后续移为内部/教师诊断接口。 |
| `SubmissionComparisonResponse` | internal-only | 辅助分析。 | 不进入主流程。 |

## Classroom 后端

### API

| 项 | 分类 | 理由 | 下一步 |
| --- | --- | --- | --- |
| `GET/POST/PUT /api/teacher/assignments` | keep-main | 教师布置作业的核心。 | 保持。 |
| `GET /api/teacher/assignments/{id}/overview` | keep-main | 教师课堂判断主数据。 | 保持，继续降噪。 |
| `GET /api/student/profile/{id}/assignments` | keep-main | 学生登录后看到作业。 | 保持。 |
| `POST /api/student/login` | keep-main | 学生身份入口。 | 保持。 |
| `GET /api/student/assignments/{assignmentId}/profile/{studentProfileId}/trajectory` | simplify | 学习轨迹有价值，但 DTO 很大，容易把内部信号推给学生端。 | 只供推荐/教师洞察使用，学生端不直接展示复杂轨迹。 |
| `GET /api/teacher/assignments/{id}/student-ai-feedback-observability` | internal-only | 观测 AI 快反馈是否可用，服务系统详情和研发判断。 | 保持折叠展示，不进教师首屏。 |
| `GET /api/teacher/assignments/{id}/ai-quality` | internal-only | AI 质量、runtime、eval 证据，服务内部质量判断。 | 保持系统详情，避免主界面工程术语。 |
| `GET /api/teacher/ai-quality/trend` | internal-only | 跨作业 AI 质量趋势，偏研发/系统。 | 保持折叠或内部视图。 |
| `GET /api/teacher/assignments/{id}/diagnosis-eval-*` | internal-only | 评测样本与 fixture，服务研发质量闭环。 | 不进教师主流程。 |
| `GET /api/teacher/recommendations/effectiveness` | simplify | 推荐效果有教学价值，但当前偏指标。 | 后续收敛为教师可读的“推荐是否有效”。 |
| class import / identity audit endpoints | keep-main | 学校部署需要班级、学生身份管理。 | 保持。 |

### Application

| 项 | 分类 | 理由 | 下一步 |
| --- | --- | --- | --- |
| `ClassroomService` | simplify | 同时承担作业、班级、概览、校正、eval draft，职责过大。 | 下一轮拆出 `AssignmentService`、`ClassroomInsightService`、`DiagnosisEvalExportService`。 |
| `StudentIdentity*` | keep-main | 学生登录与班级名单稳定性。 | 保持。 |
| `StudentTrajectoryService` | simplify | 有教学价值，但输出过多学习状态。 | 只向教师/推荐提供摘要，不直接主导学生 UI。 |
| `StudentRecommendationService` | simplify | 有助于学生下一步，但依赖大量内部轨迹。 | 后续只输出简短下一题/复盘动作。 |
| `StudentAiFeedbackImpactAnalyzer` | internal-only | 分析反馈查看后效果，服务观测和教师判断。 | 保持内部。 |
| `StudentAiFeedbackObservabilityService` | internal-only | 观测 AI 快反馈可用性。 | 保持折叠，不进首屏。 |
| `AiQualityOverviewService` / `AiQualityTrendService` / `AiQualityMetrics` | internal-only | 研发/质量/运行信号复杂集合。 | 保持内部，后续考虑移到 system/quality 包。 |
| `*Analyzer` loops：post-AC、recurring、self explanation、dependency、mastery、teaching action 等 | internal-only | 有教育研究价值，但不能逐个变成主链路模块。 | 保留为教师洞察输入，不直接暴露。 |
| `Coach*` | simplify | 追问有教学价值，但应跟学生 AI 反馈合并成一个简单后续动作。 | 后续统一成学生反馈的下一问/答复链路。 |
| `ClassReviewFeedbackService` | keep-main | 教师对课堂建议采纳/调整，能沉淀反馈。 | 保持。 |

### DTO

| 项 | 分类 | 理由 | 下一步 |
| --- | --- | --- | --- |
| `AssignmentResponse` / `StudentProfileResponse` / `ClassGroupResponse` | keep-main | 作业、学生、班级基础数据。 | 保持简单。 |
| `AssignmentOverviewResponse` | simplify | 教师主数据，但嵌入很多轨迹/推荐/coach/策略对象。 | 后续拆出首屏摘要和系统详情摘要。 |
| `StudentTrajectoryResponse` | simplify | 学习轨迹 DTO 过大。 | 后续拆成 `LearningTraceSummary` 和内部详情。 |
| `StudentRecommendationResponse` | simplify | 学生下一步动作需要，但字段过多。 | 后续收敛为一到两个行动项。 |
| `AiQualityOverviewResponse` / `AiQualityTrendResponse` | internal-only | 工程/质量/评测复杂指标。 | 保持系统详情或内部接口。 |
| `DiagnosisEvalCandidateResponse` / `DiagnosisEvalFixtureDraftResponse` | internal-only | 评测导出。 | 不进入教师主流程。 |
| `StudentAiFeedbackObservabilityResponse` | internal-only | AI 快反馈观测。 | 只用于系统详情。 |

## 第一批后续执行顺序

1. 建立代码级分类模型，作为新增后端能力的命名和测试门禁。
2. 把 `SubmissionAnalysisResponse` 明确标记为兼容/内部诊断 DTO，避免学生端继续依赖它。
3. 从 `SubmissionAnalysisService` 拆出判题事实组装，减少旧 analysis 对 `SubmissionResponse` 的控制权。
4. 将 `AiQuality*`、`DiagnosisEval*`、runtime/comparability 迁移到 system/quality 命名空间或 controller 分组。
5. 收敛 `AssignmentOverviewResponse`，首屏只保留教师课堂判断，内部详情另走 system summary。
6. 确认 `StudentFeedbackAssembler` 和 `StudentFeedbackViewAssembler` 依赖后，删除或移入测试兼容包。

## 当前轮不做的事

- 不删除任何 API。
- 不改数据库结构。
- 不改变模型调用链。
- 不把内部指标重新展示到学生或教师主界面。
- 不提交或推送。

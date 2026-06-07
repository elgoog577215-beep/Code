## Context

上一轮 `rebuild-education-agent-core` 已经把学生结果弹窗的修正建议、提升建议和追问从旧 analysis 大对象中剥离出来，并要求学生可见建议只来自模型结构化返回。后端已经具备：

- `StudentAiFeedback`：按提交存储学生 AI 快反馈状态、来源、结构化 JSON 和失败原因。
- `StudentAiFeedbackEvent`：记录反馈就绪、失败、查看等事件。
- `StudentAiFeedbackImpactAnalyzer`：分析 AI 反馈查看后的提交变化。

当前缺口是：这些数据只能服务单个学生结果弹窗，不能回答作业级问题，例如“这份作业有多少失败提交拿到了模型反馈”“学生是否真的查看了反馈”“AI 卡在哪里”“看完后有没有继续改善”。如果没有这层观测，后续优化 AI 速度和质量会继续停留在主观感受。

## Goals / Non-Goals

**Goals:**

- 建立作业级学生 AI 反馈观测摘要。
- 汇总生成、失败、安全拒绝、查看、耗时和查看后影响。
- 提供教师端只读接口。
- 在教师工作台系统详情中低噪音展示，不影响首屏课堂判断。
- 用测试验证汇总逻辑、类型契约和浏览器主流程。

**Non-Goals:**

- 不新增模型调用。
- 不改变 `StudentAiFeedback` 的生成语义。
- 不改变判题、提交、测试点、学生端结果弹窗主流程。
- 不把观测摘要做成教师端首屏主模块。
- 不新增数据库迁移；本轮复用已有实体和事件。

## Decisions

### Decision 1: 新增独立观测服务，而不是塞进 AI quality overview

新增 `StudentAiFeedbackObservabilityService`，输入 `assignmentId`，输出只读 DTO。

理由：

- 学生 AI 快反馈是学生体验链路，不等同于模型诊断质量评估。
- AI quality overview 已经承载教师校正、baseline、runtime、可比性等复杂概念，继续塞会让教师端系统详情更重。
- 独立服务更容易测试，也更容易未来接入仪表盘或告警。

替代方案：直接扩展 `AiQualityOverviewResponse`。放弃，因为会继续扩大 AI quality 大对象。

### Decision 2: 按作业提交集合反查反馈和事件

服务先读取 `SubmissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId)`，再用 submissionId 集合读取 `StudentAiFeedback` 和 `StudentAiFeedbackEvent`。

理由：

- 当前系统里提交已经有 `assignmentId`，这是最稳定的课堂边界。
- 不要求新表或新索引就能落地。
- 能把没有反馈的失败提交也纳入分母，避免“只统计成功样本”的偏差。

替代方案：只按 feedback 表聚合。放弃，因为会看不到未生成反馈的失败提交。

### Decision 3: 耗时优先解析结构化反馈 JSON 中的 `latencyMs`

`StudentAiFeedback.feedbackJson` 已经保存模型输出，学生反馈响应中包含 `latencyMs`。聚合服务优先解析该字段，解析失败则跳过该样本。

理由：

- 不改变实体结构即可得到耗时分布。
- 避免用事件时间差误当模型耗时。

替代方案：新增实体字段。暂不采用，因为本轮目标是观测汇总，不做数据库结构扩展。

### Decision 4: 前端只展示中文教学/运维摘要

教师端系统详情展示：

- 生成：`就绪/总数`
- 查看：`查看数`
- 耗时：`p95`
- 查看后：改善/仍卡住/等待后续提交
- 失败原因：翻译成中文

不直接展示 `READY`、`FAILED`、`MODEL`、`SAFETY_REJECTED` 等状态码。

理由：

- 用户已经明确要求教师端不要被工程术语和无意义文案淹没。
- 这些信息应帮助老师判断系统是否可用，而不是让老师理解后端状态机。

## Risks / Trade-offs

- [Risk] `latencyMs` 存在于 JSON，字段缺失或解析失败会导致耗时样本偏少。  
  → Mitigation: DTO 同时返回 `latencySampleCount`，前端在样本不足时显示“样本不足”。

- [Risk] 作业提交很多时一次性聚合可能变慢。  
  → Mitigation: 本轮先按当前课堂规模实现；服务保持只读和单作业范围，未来再加缓存或数据库聚合查询。

- [Risk] 查看后影响需要依赖事件和后续提交，刚生成反馈时可能只能得到“等待后续提交”。  
  → Mitigation: 把等待状态作为合法观测结果，不伪造效果结论。

- [Risk] 教师端信息再次膨胀。  
  → Mitigation: 只放系统详情折叠区，首屏不新增主卡片。

## Migration Plan

1. 新增 OpenSpec 规格和任务。
2. 新增后端 DTO、repository 查询、观测服务和 controller endpoint。
3. 新增后端聚合测试，覆盖就绪率、查看率、失败原因、耗时 p95、查看后影响。
4. 新增前端 API 类型与 client 方法。
5. 教师端系统详情区展示简短摘要，并更新 browser smoke。
6. 运行 OpenSpec validation、前端 typecheck/build、后端相关测试和 browser smoke。

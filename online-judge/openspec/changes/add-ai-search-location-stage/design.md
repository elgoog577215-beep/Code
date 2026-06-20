## Context

项目已有 `ModelDiagnosisBrief`、`StandardLibraryPackBuilder`、`ExternalModelAgentRuntime` 和 `AiReportService` 组成的外部模型诊断链路。标准库已经扩展为知识树、能力点和易错点，但现有链路仍主要依赖 rule signals 和有限标签来选择标准库内容，缺少一个面向大标准库的候选召回与 LLM 精选阶段。

本次设计保持现有学生展示和完整诊断输出不变，只在完整诊断前新增搜索定位层。标准库仍然是颗粒度、命名和标准化约束；代码阅读、错因判断和个性化建议仍由外部 LLM 完成。

## Goals / Non-Goals

**Goals:**

- 从大标准库中召回约 80-120 个候选，再由 LLM 精选 5-15 个相关条目。
- 精选结果必须落到标准库 ID、层级和证据引用上，方便后端校验和后续评测。
- 定位阶段默认启用，但失败时必须回退旧标准库包选择逻辑，不阻断判题。
- 支持混合召回：文本/结构打分为稳定底座，向量召回为可选增强。
- 在 AI invocation trace 中记录定位状态、候选数量、精选数量、降级原因和 embedding 状态。

**Non-Goals:**

- 不改变学生端展示结构，不实现分层提示或完整教程审批。
- 不重写最终诊断 prompt 的学生文案策略。
- 不把标准库做成规则诊断引擎；本地召回只缩小范围，不替代 LLM 判断。
- 不要求 embedding 不可用时阻断课堂诊断。

## Decisions

1. **采用“本地混合召回 + LLM 精选 + 第二次完整诊断”的两阶段模型调用。**
   - 原因：一次调用难以同时承载大标准库搜索和高质量教学建议；两阶段能让第二次诊断上下文更短、更准。
   - 替代方案：单次 prompt 合并定位和诊断，延迟更低但标准库精选价值弱。

2. **混合召回以文本/结构打分为主，向量召回为增强。**
   - 默认权重：文本结构 45%，向量 35%，评测/rule 信号 20%。
   - 原因：文本/结构打分可解释、可测试，向量能力受 provider、模型和部署影响更大。
   - 降级：embedding 或 pgvector 不可用时使用文本召回，定位状态标记 `VECTOR_DEGRADED`。

3. **标准库 embedding 使用独立表按内容 hash 判断过期。**
   - 原因：教师可编辑标准库条目，embedding 必须能识别过期并异步重建。
   - 兼容：正式 school 模式走 Postgres + pgvector；本地 H2 测试允许不启用向量。

4. **定位输出只作为内部结构，不直接给学生展示。**
   - 原因：学生可见内容应该来自第二步完整诊断，定位层只服务上下文压缩和标准化。
   - 校验：后端验证 ID 是否存在、层级是否匹配、证据是否非空、置信度是否合法。

5. **失败回退到现有 `StandardLibraryPackBuilder`。**
   - 原因：搜索定位是增益层，不应该因为模型 JSON 无效、API timeout 或 embedding 失败阻断判题。
   - trace：回退原因进入 `AiInvocation.searchLocationFallbackReason`。

## Risks / Trade-offs

- [Risk] 两次模型调用增加延迟和额度消耗 → 默认保留开关，定位失败快速回退，后续可按题目复杂度做自动切换。
- [Risk] pgvector 部署改变 Postgres 镜像 → Docker Compose 使用支持 pgvector 的镜像，H2/本地开发不强制启用向量。
- [Risk] LLM 精选可能返回不存在或错层级 ID → 后端强校验，失败回退旧链路。
- [Risk] 本地召回漏掉关键错因 → 保留 rule signals、判题状态和文本关键词多路召回，并用 fixture 评测 Top120 覆盖率。
- [Risk] 标准库编辑后 embedding 滞后 → 编辑保存后标记过期，后台重建；重建失败不影响保存但可观测。

## Migration Plan

- 新增配置默认启用搜索定位，但向量不可用时不阻断，确保旧环境仍可运行。
- Docker Compose 的 Postgres 镜像切换为 pgvector 兼容镜像；已有数据卷升级前需要学校部署说明。
- 应用启动后可异步补建标准库 embedding；未补齐前使用文本召回。
- 回滚方式：设置 `AI_SEARCH_LOCATION_ENABLED=false`，系统完全走旧标准库包逻辑。

## Open Questions

- ModelScope embedding 模型和接口可用性需要在真实环境继续 smoke 验证；代码层先做 OpenAI-compatible 请求形态和失败降级。

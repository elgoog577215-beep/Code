## Why

当前学生 AI 诊断链路已经在产品决策上明确由外部模型独立诊断，后端只负责结构校验、证据映射、标准库 ID 校验和成长候选记录。但实际代码和 OpenSpec 仍保留默认本地召回：后端先通过 `SearchLocationRetrievalService` 从标准库挑候选，再把候选交给模型。

这个设计会让后端在模型完整理解题目、代码和判题事实之前先缩窄方向，容易造成“库里有什么就解释什么”。用户已经明确要求：本地召回不要保留，标准库应由 AI 像深搜一样逐层导航，选择大章节、小章节、知识点、能力点、易错点和提升点。

因此需要一次替换式变更：退出默认本地召回，建立“初步诊断 -> AI 标准库导航 -> 最终诊断”的正式链路。

## What Changes

- 新增默认三阶段 AI 诊断编排：
  - `free-diagnosis-v1`：模型只看题目、完整代码和判题事实，形成初步诊断。
  - `standard-library-navigation-v1`：模型按标准库树逐层选择路径，后端按选择展开下一层。
  - `diagnosis-report-v3`：模型读取原始上下文、初步诊断和导航结果，生成正式学生报告和后端元数据。
- 退出默认本地召回：
  - 默认链路不再调用 `SearchLocationRetrievalService.retrieve(...)`。
  - 默认 trace 不再生成 `LOCAL_RECALL`。
  - 旧 `search-location-v1` 不再作为生产主链路阶段。
- 扩展标准库读取能力：
  - 提供一级目录、子目录、知识点和知识点下诊断层的分页展开能力。
  - 诊断层遵守“知识点 -> 能力点 -> 易错点/提升点”。
- 保留学生端展示形态：
  - 学生端仍看基础层诊断、证据、提高层诊断和下一步行动。
  - 初步诊断和导航路径只进入后端 trace、教师审计和评测。
- 暂不实现 AI 自查循环和 AI 自动资产实时入库：
  - 库外发现只生成待审核成长候选。

## Capabilities

### Modified Capabilities

- `ai-diagnosis-orchestrator-v2`
- `single-agent-ai-diagnosis`
- `student-review-personalization-loop`
- `ai-prompt-context-quality`
- `standard-library-normalized-schema`

## Impact

- 后端：影响 `ExternalModelAgentRuntime`、`AiReportService`、prompt registry、标准库服务、trace/telemetry、输出校验和相关测试。
- 数据库：不要求新增平行标准库；需要基于现有规范化标准库提供可导航读取 API。
- 前端：学生端主展示不应大改；教师端可后续展示导航路径和库外缺口。
- 测试：必须新增默认链路不调用本地召回、导航轮次受控、导航失败不本地回退、最终学生报告保持结构的测试。
- 风险：模型调用次数增加，必须通过轮次、分支数和最终条目数限制控制 token 与延迟。

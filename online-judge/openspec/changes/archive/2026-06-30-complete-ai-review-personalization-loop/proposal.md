## Why

AI 诊断链路已经完成多轮优化，但学生复盘、个性化推荐、教师班级洞察和质量评测还没有完全收成一个闭环。现在需要把这些能力接到同一套细颗粒画像和诊断结果上，让系统真正知道学生最近卡在哪里、下一步该做什么、教师应该关注什么。

本次目标不是再增加一条复杂 Agent 链路，而是把已有“本地召回 + 单诊断 Agent + studentReport + 学生画像”固定为主路径，并让后续功能都复用这条路径产出的结构化数据。

## What Changes

- 固化默认实时链路：
  - 默认关闭实时搜索 Agent，只保留本地召回和一次单诊断 Agent。
  - 诊断 Agent 输入必须包含题目、代码、判题结果、证据、树形标准库候选包和必要学生画像上下文。
  - 学生端优先展示 `studentReport`，结构化字段用于校验、画像、推荐、教师追踪和评测。
- 升级学生错题复盘：
  - 将现有学生 AI 快反馈从短项提示升级为“基础层 / 提高层 / 下一步行动”的可读复盘报告。
  - 允许标准库未命中时返回库外判断，但必须保留证据和不泄露答案约束。
- 打通画像驱动推荐：
  - 推荐继续复用 `StudentAbilityProfileService` 的细颗粒画像和复盘卡片。
  - 推荐理由必须说明与最近错因、能力点或复盘卡片的关系。
- 打通教师班级洞察：
  - 教师端继续复用作业总览和班级总览，补齐班级高频细颗粒错因、能力薄弱点和建议介入方向。
  - 不新建复杂教师权限或多角色审批。
- 完善评测闭环：
  - 评测样例保留学生实际可见 `studentReport`。
  - 质量分类覆盖召回缺失、模型误读、文案差、答案泄露、过长、标准库缺口和校验问题。
- 完善标准库成长闭环：
  - 库外发现只进入候选池，不实时自动写正式库。
  - 教师批准后再进入标准库，并触发 embedding 过期与后续重建。

## Capabilities

### New Capabilities

- `student-review-personalization-loop`: 覆盖学生复盘报告、画像驱动推荐、教师班级洞察、评测闭环和标准库成长候选池之间的主流程关系。

### Modified Capabilities

- `single-agent-ai-diagnosis`: 固定默认实时链路为本地召回加一次单诊断 Agent，并要求诊断输出优先服务学生可读报告。
- `ai-diagnosis-quality-loop`: 扩展评测闭环，要求保存学生可见报告并按固定失败类型归因。

## Impact

- 后端：
  - `AiReportService`、`StudentAiFeedbackResponse`、`StudentFeedbackAssembler`。
  - `StudentAbilityProfileService`、`StudentRecommendationService`。
  - 作业/班级总览相关 DTO 与服务。
  - AI 评测输出与标准库成长候选池。
- 前端：
  - 学生提交结果页的 AI 复盘展示。
  - 学生首页/推荐页的复盘卡片与推荐理由。
  - 教师班级总览和 AI 可观测页面。
- 配置：
  - 默认保持 `AI_SEARCH_LOCATION_ENABLED=false`。
  - 不新增实时双 Agent 默认路径。
- 依赖：
  - 不新增第三方依赖。

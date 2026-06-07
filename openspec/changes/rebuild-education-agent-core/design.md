## Context

当前系统已经具备很多教育 agent 的零件：

- 判题与提交：`JudgeService`、`Submission`、`SubmissionCaseResult`、测试点公开/隐藏边界。
- 诊断与证据：`DiagnosisEvidencePackageBuilder`、`RuleSignalAnalyzer`、`ModelDiagnosisBrief`、`StandardLibraryPack`。
- 外接模型：`AiReportService`、`ExternalModelAgentRuntime`、prompt、normalizer、validator、fallback attribution、latency telemetry。
- 学生反馈：`studentFeedback`、`studentFeedbackView`、双通道反馈、结果弹窗。
- 学习轨迹：同题多次提交、错因复发、post-AC transfer、AI dependency、self explanation、recommendation loops。
- 教师洞察：作业 overview、AI quality、需关注学生、教师校正、推荐效果、课堂策略。

问题不是“没有功能”，而是主链路仍然被大而全的 `SubmissionAnalysis` 和 OJ 式页面结构牵引。学生端、教师端、AI 评测和学习轨迹共享同一个复杂对象，导致学生反馈慢、教师端噪音多、工程状态容易外泄、AI 能力和本地兜底边界不稳定。

本变更的设计原则是：先定义教育目标态，再决定旧代码如何迁移。旧代码不是默认主线，只有能服务新目标才复用。

## Target Architecture

```text
提交代码
  |
  v
判题事实层 Judge Facts
  - verdict
  - testCaseResults
  - firstFailedCase
  - compile/runtime error
  - source/code evidence refs
  |
  +--> 学生 AI 快反馈层 Student Feedback Agent
  |      - repairItems: 最多一个首要修正建议
  |      - improvementItems: 最多两个提升建议
  |      - nextQuestion: 一个追问
  |      - status/source/latency/safety
  |
  +--> 学习轨迹层 Learning Trace
  |      - 同题多次提交变化
  |      - RE -> WA、WA -> AC、AC -> failure
  |      - 同类错因复发
  |      - AI 提示后是否继续提交或改善
  |
  +--> 教师洞察层 Teacher Insight
         - 需关注学生
         - 高频问题
         - 共性误区
         - 教师干预和校正
         - AI 质量证据
```

### 1. 判题事实层

判题事实层只回答“发生了什么”，不负责教学表达：

- 当前 verdict 是什么。
- 哪些测试点通过，哪些没过。
- 首个公开失败点是什么。
- 编译/运行错误是什么。
- 可以引用哪些代码、评测、题目和历史证据。

本地规则可以在这一层整理候选信号和证据，但不得生成学生端可见的修正建议或提升建议。

### 2. 学生 AI 快反馈层

学生 AI 快反馈层只服务学生结果弹窗，不等待教师深诊断完成。它的主契约应该直接对齐前端三栏：

```json
{
  "status": "GENERATING|READY|TIMEOUT|FAILED|SAFETY_REJECTED",
  "source": "MODEL",
  "repairItems": [
    {
      "title": "短标题",
      "body": "可执行但不泄露答案的修正方向",
      "evidenceRefs": ["case:1", "code:line:2"],
      "qualitySignals": ["evidence_grounded", "actionable"]
    }
  ],
  "improvementItems": [
    {
      "title": "复杂度|边界意识|测试习惯|代码结构",
      "body": "通过后继续提升的方向",
      "evidenceRefs": ["problem:constraint"]
    }
  ],
  "nextQuestion": "一个让学生自证理解的问题",
  "latencyMs": 0,
  "safety": {
    "answerLeakRisk": "LOW|MEDIUM|HIGH",
    "blockedReasons": []
  }
}
```

学生端两个建议栏目只展示 `source=MODEL` 且通过安全校验的结构化结果。本地规则可提供证据、候选标签、质量校验和失败状态，不可生成学生可见建议冒充 AI。

### 3. 学习轨迹层

学习轨迹层不只是读单次分析，而是观察学生过程：

- 同题连续提交是否从失败推进到通过。
- 编译错误是否修好，运行错误是否变成答案错误。
- 是否多次卡在同一细粒度错因。
- AI 反馈后学生是否继续提交、是否改善、是否依赖提示。
- AC 后是否有迁移复盘或同类题验证。

该层输出给学生推荐和教师洞察，不直接塞进学生做题页造成噪音。

### 4. 教师洞察层

教师洞察层回答“老师该看哪里、该怎么介入”：

- 哪些学生需要关注。
- 班级最高频问题是什么。
- 哪些错因是共同误区。
- 哪些学生在同类问题上复发或回退。
- 哪些 AI 判断被教师校正。
- AI 质量是否可靠，有哪些证据。

教师端不得展示面向研发的工程术语作为主界面内容，例如 `BLOCKED`、`RECOVERED`、`NOT_COMPARABLE`、`smoke`、`profile`、`fallback` 等。工程状态可以保留在系统详情、调试页或质量证据折叠区，并翻译成教师可理解的语言。

## Reuse And Removal Strategy

### Reuse as Foundation

- 判题执行、题目、测试点、提交记录。
- 班级、学生、作业、作业任务。
- 证据构建、候选信号、安全校验、taxonomy。
- 外接模型 runtime、request telemetry、validator、quality eval。
- 已有学习轨迹分析器中能解释进步、回退、复发、AI 依赖的部分。
- 前端做题工作台中题单、题面、编辑器、结果弹窗的可用结构。

### Remove From Main Path

“移出主链路”不等于立刻删除文件。它表示后续实现中不再让这些模式决定产品结构：

- 一个 `SubmissionAnalysis` 同时服务学生、教师、研发、评测、推荐的万能大对象模式。
- 从完整 AI 报告里再抽取学生端三栏信息的模式。
- 本地规则生成学生可见建议的模式。
- 教师端直接展示工程状态和 runtime 术语的模式。
- 学生端解释性废话、重复入口、非行动导向文案。

## Judgment System

规划不能把教育目标粗暴 bool 化。验收分三层：

### 1. 布尔门禁

用于判断结构、安全、状态和责任边界是否成立。例如：

- 学生建议是否只来自模型结构化返回。
- 本地规则是否只用于证据整理和校验。
- AI 超时是否有明确状态。
- 高泄露风险是否被拦截。
- 教师主界面是否隐藏工程术语。

### 2. 质量 Rubric

用于判断“做得好不好”。建议先使用 0-3 分：

- 0：空泛或不可用。
- 1：有方向但不可执行，或缺少证据。
- 2：可执行、有证据、不泄露答案。
- 3：可执行、有证据、能引导学生自己验证，并与当前学习阶段匹配。

Rubric 覆盖学生修正建议、提升建议、追问、教师洞察、学习轨迹解释。

### 3. 观测指标

用于后续真实课堂量化，不在第一阶段强设阈值：

- AI 快反馈 p50/p95 latency。
- 模型结构化成功率、超时率、安全拦截率。
- 学生查看反馈后再次提交比例。
- 同题失败到通过的平均提交次数。
- 同类错因复发率。
- 教师校正率。
- 教师查看需关注学生和高频问题的比例。

## Migration Plan

### P0: 目标架构和接口契约

定义四层服务边界、DTO 契约、状态模型、质量 rubric 和观测指标。此阶段不要求替换全部旧代码，但必须让后续实现有清晰落点。

### P1: 学生提交后的 AI 快反馈链路

为失败提交建立独立学生反馈生成和查询链路。学生端结果弹窗的修正建议、提升建议、追问只消费该链路。旧 `SubmissionAnalysis` 可保留，但不再作为学生建议的主来源。

### P2: 学习轨迹

把单次提交事实串成学习过程信号，至少支持同题提交变化、同类错因复发、AI 反馈后变化。轨迹输出服务学生推荐和教师洞察。

### P3: 教师课堂洞察

重建教师端主信息结构：需关注学生、高频问题、共性误区、教师干预、AI 质量证据。AI 工程状态降级到系统详情或折叠区。

### P4: 旧 analysis 和旧 UI 入口清理

将旧 analysis 大对象降级为兼容层或归档层；删除或隐藏不再服务新主链路的学生端/教师端 UI 入口和文案；更新 smoke 和回归测试。

## Failure And Rollback Boundaries

- 规划阶段不改业务代码，因此失败只影响 OpenSpec 文件。
- 实施阶段任何新链路必须能和旧判题提交并存，不能破坏基本提交和判题。
- 学生 AI 快反馈失败时，学生端显示明确 AI 状态，但不得用本地建议冒充 AI。
- 教师洞察重建失败时，保留旧教师页兼容入口，直到新主视图通过验收。
- 若模型服务不可用，系统仍能完成判题事实和学习轨迹记录；学生 AI 建议为空或失败状态。

## Testing Strategy

- OpenSpec strict validation。
- 后端单元测试：DTO contract、状态机、安全门禁、rubric scoring、观测指标记录。
- 后端集成测试：提交失败后生成判题事实、学生 AI 快反馈状态、学习轨迹信号。
- 模型 fixture 测试：结构化成功、超时、解析失败、安全拒绝、本地规则不得进入学生建议。
- 前端 typecheck/build/browser smoke：学生首页、作业工作台、结果弹窗、教师首页。
- 人工课堂流验收：学生 3 秒内知道入口；提交失败后只看到一个首要修正方向；教师首屏能判断关注对象和共性问题。

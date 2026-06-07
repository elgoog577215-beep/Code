## P0 架构盘点与契约草案

本文是 `rebuild-education-agent-core` 的 P0 执行草案，用来把当前代码映射到目标教育 agent 四层架构。它不是业务实现，不修改现有接口；后续实现必须以这里的责任边界为准。

## 1. 当前可复用资产

### 判题事实层可复用

- `JudgeService`：保留提交、编译/运行、测试点执行、verdict 生成。
- `SubmissionResponse`：已有 `verdict`、`executionTime`、`memoryUsed`、`compileOutput`、`errorMessage`、`testCaseResults`。
- `SubmissionCaseResult` / `SubmissionCaseResultRepository`：保留测试点明细、公开/隐藏边界、期望/实际输出。
- `Problem` / `TestCase`：保留题面、限制、测试点。
- `DiagnosisEvidencePackageBuilder`：可复用为证据包生成器，但输出应服务 `JudgeFacts` 和模型输入，而不是直接服务大报告。
- `RuleSignalAnalyzer`：可复用为候选信号生成器，但不得生成学生端可见 AI 建议。

### 学生 AI 快反馈层可复用

- `ExternalModelAgentRuntime`：可复用 runtime plan、profile、标准库裁剪。
- `AiReportService`：可拆出或重组模型调用能力，但不应继续让一次完整 analysis 主导学生反馈。
- `ModelOutputValidator` / `ModelOutputSafetyPolicy`：可复用安全校验、证据引用校验、泄题风险校验。
- `StudentFeedbackViewAssembler`：可作为过渡参考；目标态应让模型直接返回对齐前端的 `StudentAiFeedback`，而不是从大 analysis 抽取。
- `studentFeedback` / `studentFeedbackView` DTO：可作为迁移桥，但需要拆出独立状态、source、latency、安全失败原因。

### 学习轨迹层可复用

- `StudentTrajectoryService`：已有同题作业轨迹聚合入口。
- `TrajectorySignalAnalyzer`、`LearningTrajectoryPolicy`、`LearningActionEvidenceAnalyzer`：可复用进步、回退、行动证据相关逻辑。
- `PostAcTransferAnalyzer`、`RecurringMisconceptionAnalyzer`、`SelfExplanationMasteryAnalyzer`、`AiDependencyAnalyzer`、`MasteryGrowthAnalyzer`：可复用为轨迹信号，但需要降噪，不直接堆到学生做题页。
- `StudentRecommendationService`：可复用推荐编排思想，但后续推荐必须建立在清晰轨迹信号上。

### 教师洞察层可复用

- `ClassroomService.getAssignmentOverview`：可复用课堂作业汇总、学生列表、问题统计。
- `AiQualityOverviewService` / `AiQualityTrendService`：可复用 AI 质量证据，但主界面必须翻译和折叠工程状态。
- `TeacherDiagnosisCorrection` / diagnosis correction API：可复用教师校正闭环。
- `ClassReviewFeedbackService` / recommendation effectiveness：可复用为教师干预效果证据。

## 2. 应退出主链路的模式

- `SubmissionAnalysisResponse` 作为万能大对象：当前同时承载学生提示、教师说明、AI invocation、学习轨迹、校正信号、原始报告、line issues。后续应降级为兼容容器或归档容器。
- `SubmissionAnalysisService.generateAndStoreAnalysis` 一次生成所有分析：后续学生快反馈应独立于完整教师诊断。
- `StudentFeedbackViewAssembler` 从完整 analysis 抽取学生三栏：目标态应由学生快反馈 contract 直接驱动前端。
- fallback/rule-based 结果进入学生可见建议：目标态禁止。
- 教师页直接消费 `aiQuality` runtime 细节作为主展示：目标态应将工程状态翻译、折叠或转移到系统详情。

## 3. 目标契约草案

### JudgeFacts

```json
{
  "submissionId": 0,
  "problemId": 0,
  "assignmentId": 0,
  "studentProfileId": 0,
  "verdict": "RUNTIME_ERROR",
  "language": "Python 3",
  "executionTime": 0.0,
  "memoryUsed": 0,
  "testCaseSummary": {
    "passed": 0,
    "total": 1
  },
  "testCases": [
    {
      "number": 1,
      "passed": false,
      "hidden": false,
      "expectedOutput": "YES",
      "actualOutput": "程序运行时异常",
      "executionTime": 0.065,
      "memoryUsed": 0,
      "evidenceRef": "case:1"
    }
  ],
  "firstFailedCase": {
    "number": 1,
    "hidden": false,
    "expectedOutput": "YES",
    "actualOutput": "程序运行时异常",
    "evidenceRefs": ["case:1", "runtime:error"]
  },
  "sourceEvidence": [
    {
      "ref": "code:line:1",
      "kind": "SOURCE_LINE",
      "summary": "第 1 行读取输入并转换整数"
    }
  ],
  "candidateSignals": [
    {
      "ref": "rule:input_parsing",
      "issueTag": "IO_FORMAT",
      "fineGrainedTag": "INPUT_PARSING",
      "confidence": 0.75,
      "reason": "运行错误发生在输入转换阶段"
    }
  ]
}
```

责任边界：

- 可由本地规则和判题事实生成。
- 可以提供给模型、轨迹、教师端。
- 不包含学生端可见的修正建议或提升建议。

### StudentAiFeedback

```json
{
  "submissionId": 0,
  "status": "READY",
  "source": "MODEL",
  "generatedAt": "2026-06-07T00:00:00",
  "latencyMs": 3200,
  "repairItems": [
    {
      "title": "先看输入读取",
      "body": "先对照题目的输入格式，检查程序实际读取了几个值，再用最小样例复现这次运行错误。",
      "evidenceRefs": ["case:1", "code:line:1"],
      "rubricSignals": ["evidence_grounded", "actionable", "no_solution_leak"]
    }
  ],
  "improvementItems": [
    {
      "title": "测试习惯",
      "body": "通过后补一个最小输入和一个最大边界输入，确认读取逻辑不会只适配样例。",
      "evidenceRefs": ["problem:input_format"]
    }
  ],
  "nextQuestion": "这道题一行里实际有几个整数？你的代码当前读取了几个？",
  "safety": {
    "answerLeakRisk": "LOW",
    "blockedReasons": []
  }
}
```

状态枚举：

- `NOT_REQUESTED`：尚未请求。
- `GENERATING`：模型生成中。
- `READY`：模型结构化反馈通过校验。
- `TIMEOUT`：模型超时。
- `FAILED`：模型调用或解析失败。
- `SAFETY_REJECTED`：模型结果存在泄题或安全风险。

责任边界：

- `repairItems` 和 `improvementItems` 必须来自模型结构化返回。
- 本地逻辑只负责证据输入、结构校验、安全校验、状态记录。
- 前端只展示 `source=MODEL` 且 `status=READY` 的建议。

### LearningTraceSignal

```json
{
  "studentProfileId": 0,
  "assignmentId": 0,
  "problemId": 0,
  "phase": "FAILED_TO_FAILED|FAILED_TO_ACCEPTED|REGRESSION|REPEATED_STUCK|POST_AC_TRANSFER_PENDING",
  "summary": "学生连续两次卡在输入解析，尚未形成稳定修正。",
  "evidenceRefs": ["submission:12", "submission:13", "issue:INPUT_PARSING"],
  "needsTeacherAttention": false,
  "recommendedNextAction": "先让学生解释输入格式与读取次数的对应关系。"
}
```

责任边界：

- 描述学习过程，不替代学生当前反馈。
- 服务学生推荐和教师洞察。
- 可以由提交、analysis、coach、recommendation event 共同生成。

### TeacherInsight

```json
{
  "assignmentId": 0,
  "classroomKpis": {
    "submittedStudents": 0,
    "acceptedStudents": 0,
    "stuckStudents": 0
  },
  "studentsNeedingAttention": [
    {
      "studentProfileId": 0,
      "name": "学生",
      "reason": "连续三次同类错因复发",
      "evidenceRefs": ["student:1", "issue:INPUT_PARSING"]
    }
  ],
  "commonIssues": [
    {
      "label": "输入格式",
      "count": 5,
      "teachingSuggestion": "统一讲一次题目输入格式与读取次数的对应关系。"
    }
  ],
  "aiQualityEvidence": {
    "summary": "AI 反馈可用，但有部分超时样本。",
    "detailsAvailable": true
  }
}
```

责任边界：

- 主界面服务教师课堂判断。
- 工程术语必须翻译或隐藏到详情。
- 教师校正进入后续 AI 质量和诊断闭环。

## 4. 验收体系细化

### 布尔门禁

- 学生建议是否只展示模型结构化结果。
- 本地规则是否未写入学生可见建议。
- AI 失败是否有明确状态。
- 高泄露风险是否被拦截。
- 教师主界面是否不直接展示工程术语。

### Rubric

学生修正建议：

- 0：空泛、错误或泄露答案。
- 1：有方向但不可执行，缺少证据。
- 2：可执行、有证据、不泄露答案。
- 3：可执行、有证据、能让学生自证理解。

提升建议：

- 0：和当前错误重复，或与题目无关。
- 1：泛泛而谈代码优雅、复杂度等。
- 2：和题目约束或提交行为相关。
- 3：能引导学生形成可迁移练习或测试习惯。

教师洞察：

- 0：只罗列数据或工程状态。
- 1：有问题标签但没有对象和证据。
- 2：能指出学生/共性问题和证据。
- 3：能支持教师决定讲什么、找谁、怎么介入。

### 观测指标

- `student_ai_feedback_latency_ms`
- `student_ai_feedback_model_ready`
- `student_ai_feedback_timeout`
- `student_ai_feedback_safety_rejected`
- `student_ai_feedback_viewed`
- `student_resubmitted_after_feedback`
- `learning_trace_repeated_issue_detected`
- `teacher_insight_opened`
- `teacher_correction_recorded`

## 5. P0 结论

当前系统不是没有教育能力，而是教育能力被大 analysis、复杂教师页和旧 OJ 入口包住了。P0 后续实施的第一优先级不是继续增强 `SubmissionAnalysisResponse`，而是让 `JudgeFacts` 和 `StudentAiFeedback` 成为学生主链路的第一等契约。

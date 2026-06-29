## Overview

本次设计的核心是“化繁为简”：实时链路只保留一个外部诊断 Agent，后端负责准备高质量上下文、执行底线校验、沉淀画像与评测结果。推荐、教师端、标准库成长都读取同一套诊断和画像数据，而不是各自再做一套判断。

目标链路：

```text
学生提交
-> 判题结果 + 证据包
-> 本地召回树形标准库包
-> 单诊断 Agent 生成 studentReport + 结构化元数据
-> 学生复盘展示
-> 画像更新与推荐
-> 教师班级洞察
-> 评测与标准库成长候选
```

## Architecture

### 1. 实时主链路

实时链路只允许一次外部诊断调用：

```text
DiagnosisEvidencePackage
-> ModelDiagnosisBrief
-> LocalRecallTreePack
-> diagnosis-report-v2
-> StudentReport + metadata
-> StudentAiFeedback / StudentFeedbackView
```

保留本地召回，因为它解决“标准库太大不能完整传输”的问题；关闭实时搜索 Agent，因为它与诊断 Agent 重复理解题目，增加延迟和误差来源。

### 2. 学生复盘输出

学生可见内容应是自然语言报告，不是字段拼接：

- 基础层：当前最影响通过的错误、证据、为什么重要、学生下一步怎么验证。
- 提高层：修复基础问题后值得提升的算法、复杂度、建模、测试或迁移能力。
- 下一步行动：1 到 3 个可马上执行的小动作。

结构化字段继续保留，但定位为机器消费：

- `libraryFit`
- `issueTags`
- `fineGrainedTags`
- `abilityPoints`
- `focusPoints`
- `evidenceRefs`
- `answerLeakRisk`

### 3. 学生画像共享层

现有 `StudentAbilityProfileService` 是共享画像入口，不新建平行画像服务。它负责从最近提交、诊断报告、教练互动和推荐事件中汇总：

- 高频错因。
- 细颗粒知识点。
- 能力点。
- 复盘卡片。
- AI 依赖风险。
- 掌握度增长信号。

其他模块只消费它的结果，不重复解析 `reportJson`。

### 4. 个性化推荐

`StudentRecommendationService` 继续作为唯一推荐入口。推荐排序按以下优先级：

1. 未解决的基础错因复盘。
2. 最近失败题的同题重做。
3. 同类新题迁移。
4. AC 后提高层拓展。
5. AI 追问或自解释练习。

推荐理由必须能落回画像中的错因、能力点、复盘卡片或证据题目。

### 5. 教师班级洞察

教师端不重新调用外部 AI。班级洞察从作业总览、学生画像和诊断元数据聚合：

- 班级高频细颗粒错因。
- 高频能力薄弱点。
- 需要教师介入的学生数量。
- 推荐课堂讲评方向。
- 代表题和代表学生证据。

这部分应优先做确定性聚合，避免教师端出现不可解释的模型判断。

### 6. 评测闭环

评测不只看模型是否输出 JSON，而要保存学生实际看到的报告：

- 每道样例保存 `studentReport`。
- 记录主因命中、文案可读性、长度、答案泄露、标准库误用、库外判断。
- 失败必须归入固定分类，便于后续定位是召回、模型、提示词、标准库还是校验问题。

### 7. 标准库成长

标准库服务 AI，不制约 AI。模型可以返回 `OUT_OF_LIBRARY`，但不能实时修改正式库。

库外发现进入候选池后只做三件事：

- 聚合相似候选。
- 展示证据和建议路径给教师。
- 教师批准后进入正式库。

## Data Flow

```text
Submission
  -> SubmissionAnalysis.reportJson
    -> StudentAbilityProfileService
      -> Student home reviewCards
      -> StudentRecommendationService
      -> Teacher overview aggregation
      -> CoachPromptService context
```

## Boundaries

- 不恢复默认双外部 Agent。
- 不新增自动改正式标准库的实时能力。
- 不新增复杂教师账号或多校 SaaS。
- 不新增第三方依赖。
- 不把推荐、教师端、评测各自写成独立诊断系统。

## Risk Control

- 如果外部 AI 不可用，学生端明确显示不可用或降级结果，不伪装成模型成功。
- 如果标准库未命中，模型允许库外判断，后端记录候选，不强行贴库内标签。
- 如果报告泄露完整答案、完整代码或隐藏测试推测，后端拦截或降级。
- 如果评测样例失败，必须输出失败分类，避免无方向地修 prompt。

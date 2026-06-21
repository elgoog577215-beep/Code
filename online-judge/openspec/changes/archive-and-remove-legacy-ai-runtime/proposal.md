## Why

当前 AI 诊断后端已经形成新的正式链路：

```text
DiagnosisEvidencePackage
-> ModelDiagnosisBrief
-> SearchLocationAgent
-> selected StandardLibraryPack
-> diagnosis-and-advice-v1
-> AdviceGenerationOutputValidator
-> StudentFeedback / StudentFeedbackView
```

但主代码里仍保留历史 AI 运行电路：legacy long prompt、`diagnosis-judge + teaching-hint` staged runtime、`diagnosis-and-teaching` single-call runtime、旧输出 DTO 和旧 prompt 配置。这些旧入口会让后端链路难以判断、测试语义混杂，也可能让学校模式在失败时静默回到旧逻辑。

本变更只做归档与清理：把旧链路完整移出程序运行代码，建立与编译、运行、测试无关的 `legacy-archive` 档案。暂不把旧 prompt、旧规则或旧 fixture 迁移到新链路。

## What Changes

- 新增 `legacy-archive/ai-runtime-legacy/`，保存旧链路快照、prompt、契约、测试索引、关系图和删除说明。
- 删除主代码中的 legacy long prompt 分支。
- 删除 staged runtime：`diagnosis-judge-v2 + teaching-hint-v1`。
- 删除旧 single-call runtime：`diagnosis-and-teaching-v1/v2/v3/v4-lite`。
- 删除 `CombinedOutput` 运行路径和旧 prompt version 回退逻辑。
- 删除旧运行模式配置：`ai.external-runtime-mode`、`ai.external-single-call-prompt-version`。
- 保留新链路底座：证据包、规则信号、ModelDiagnosisBrief、搜索定位、标准库、advice 输出和学生反馈视图。

## Capabilities

### New Capabilities

- `ai-runtime-cleanup`: 定义旧 AI 运行链路归档、主代码清理、配置收口和清洁验证要求。

### Modified Capabilities

- 无。

## Impact

- 后端：影响 AI 诊断运行路径、prompt 注册、runtime plan、模型输出 DTO/映射、测试。
- 配置：移除旧 runtime 模式与旧 prompt version 配置。
- 文档：新增 legacy archive，作为历史资产查阅位置。
- 测试：删除或改写旧 prompt/stage 断言，保留新 search-location + advice-generation 验证。

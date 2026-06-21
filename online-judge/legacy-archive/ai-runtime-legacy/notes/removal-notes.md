# 删除说明

## 删除原则

- 先归档，再删除主代码引用。
- 不把旧 prompt 规则迁移到新 prompt。
- 不把旧 fixture 迁移到新测试。
- 不删除新链路底座。

## 主代码删除目标

- legacy long prompt 分支。
- staged runtime：`diagnosis-judge-v2 + teaching-hint-v1`。
- old single-call runtime：`diagnosis-and-teaching-v1/v2/v3/v4-lite`。
- `CombinedOutput` 调用路径。
- 旧 prompt version fallback。
- `AI_EXTERNAL_RUNTIME_MODE`。
- `AI_EXTERNAL_SINGLE_CALL_PROMPT_VERSION`。

## 保留目标

- `search-location-v1`。
- `diagnosis-and-advice-v1`。
- `AdviceGenerationOutput`。
- `AdviceGenerationOutputValidator`。
- `DiagnosisEvidencePackage`。
- `RuleSignalAnalyzer`。
- `StandardLibraryPack`。

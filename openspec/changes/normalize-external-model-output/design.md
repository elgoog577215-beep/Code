## 背景

上一轮已经把外部模型调用从纯 staged 扩展到 `single-call`，解决了部分额度、限流和第二阶段失败问题。继续提升最终效果时，不能只追求“调用成功”，还要降低“模型已经给出可用判断，但因为输出格式轻微不符合系统枚举而被丢弃”的损失。

当前链路的关键边界是：

```text
外部模型 JSON -> parse -> validate -> build response
```

本轮将它升级为：

```text
外部模型 JSON -> parse -> normalize -> validate -> build response
```

## 方案

新增 `ExternalModelOutputNormalizer`，负责在严格校验前做确定性标准化。

### 标准化对象

1. `DiagnosisJudgeOutput`
   - `primaryIssueTag`
   - `fineGrainedTag`
   - `evidenceRefs`
   - `answerLeakRisk`

2. `TeachingHintOutput`
   - `studentHintPlan.teachingAction`
   - `studentHintPlan.evidenceRefs`
   - `studentHintPlan.answerLeakRisk`
   - `learningInterventionPlan.evidenceRefs`
   - `learningInterventionPlan.answerLeakRisk`
   - `answerLeakRisk`

3. `CombinedOutput`
   - 同时标准化其中的诊断与教学提示。

### 标签标准化规则

标签只能映射到当前 `StandardLibraryPack` 中已经允许的标签：

- 精确匹配标准 ID，例如 `loop_boundary` -> `LOOP_BOUNDARY`。
- 匹配标准库 `label`，例如 `循环边界` -> `LOOP_BOUNDARY`。
- 匹配去空格后的 label，例如 `差 一 位 错误` -> `OFF_BY_ONE`。

如果无法匹配，不猜测、不近似、不用字符串相似度强行修复。

### 证据引用标准化规则

证据引用只能映射到 `ModelDiagnosisBrief` 中已经存在的引用：

- 去掉首尾空白。
- 忽略大小写匹配已有引用，例如 ` CODE:RANGE_EXCLUDES_N ` -> `code:range_excludes_n`。

如果无法匹配，不新增证据，不替换成第一条证据。

### 教学动作标准化规则

教学动作只能映射到 `StandardLibraryPack.teachingActions` 中已有动作：

- 去掉首尾空白。
- 忽略大小写匹配，例如 `trace_variables` -> `TRACE_VARIABLES`。

如果无法匹配，保留原值，让校验失败。

### 安全边界

标准化层不得降低安全等级：

- 如果模型输出 `answerLeakRisk=HIGH`，必须保留 HIGH。
- 如果文本包含完整代码、最终答案、直接替换类提示，仍由 `ModelOutputValidator` 判定为安全风险。
- 标准化层不删除、不改写学生提示正文，避免把不安全内容伪装成安全内容。

## 接入点

- `ExternalModelAgentRuntime` 通过 normalizer 暴露：
  - `normalizeDiagnosisDecision`
  - `normalizeTeachingHint`
  - `normalizeCombinedOutput`
- `AiReportService` 在调用模型并 parse 后、validate 前调用标准化。
- staged 与 single-call 路径都使用同一标准化入口。

## 验收标准

- 外部模型输出中文标签时，系统可以映射为标准 ID 并完成校验。
- 外部模型输出大小写不同的证据引用时，系统可以映射为 brief 中的原始引用。
- 外部模型输出未知标签或未知证据时，仍然失败。
- 外部模型输出直接答案或完整代码时，仍然触发安全风险。
- 后端 targeted tests 和 OpenSpec strict validate 通过。
- 至少运行一次节制 live eval，确认 single-call 链路仍可完成。

## Background

现有外部模型 runtime 的优点是分阶段清晰：

```text
diagnosis-judge-v1 -> teaching-hint-v1
```

缺点是每条提交最多两次外部模型调用。真实 live eval 已经证明，这会在 ModelScope 额度/限流下造成第二阶段失败。上轮预算保护解决的是“失败后不要继续撞接口”，但没有减少成功路径本身的调用次数。

## Goals

- 在低预算场景下把单条提交诊断的外部模型调用次数从 2 次降到 1 次。
- 保留现有标准库、证据引用、标签校验、安全校验。
- 默认不改变现有 staged 模式，避免一次性改变全部线上行为。
- 让 live eval 可以通过环境变量切换单次调用模式。

## Non-Goals

- 不删除两阶段模式。
- 不改变 Coach 和成长报告调用方式。
- 不放宽安全门禁。
- 不依赖模型自由文本输出。

## Architecture

```text
RuntimePlan
   |
   +-- staged
   |      diagnosis call -> validate decision
   |      teaching call  -> validate hint
   |
   +-- single-call
          combined call -> parse CombinedOutput
          validate decision
          validate hint
```

单次调用 payload：

```json
{
  "diagnosisDecision": DiagnosisJudgeOutput,
  "teachingHint": TeachingHintOutput
}
```

## Design Decisions

### 1. 默认保持 staged

两阶段模式更容易定位质量问题，因此默认仍使用 `staged`。低预算模式通过 `ai.external-runtime-mode=single-call` 显式启用。

### 2. 单次调用仍复用现有校验

不新增一套独立 validator。单次输出拆成 `diagnosisDecision` 和 `teachingHint` 后，分别调用现有诊断与教学校验，降低行为漂移。

### 3. 失败语义沿用现有部分完成策略

如果单次调用中的诊断有效但教学提示无效，仍可以保留诊断，并用本地安全教学模板补齐。这样保证低预算模式不会因为教学表达坏掉而丢掉可用错因。

## Testing

- Prompt template registry 测试新增 single-call 模板。
- 外部 runtime 测试验证 single-call 成功时只调用一次模型。
- 测试 single-call 诊断有效但教学提示无效时保留诊断并标记部分完成。
- 运行 targeted tests。
- 使用真实外部模型节制跑 1 条 smoke eval，必要时只验证可达和调用路径，不追求消耗式全量测试。

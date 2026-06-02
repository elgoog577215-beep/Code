## ADDED Requirements

### Requirement: Low latency runtime profile compacts external model inputs

系统 SHALL 支持显式启用的 `low-latency` 外部模型 runtime profile，在不改变默认 profile 的前提下压缩 runtime 输入。

#### Scenario: Standard profile remains default

- **WHEN** no low-latency profile is configured
- **THEN** external runtime MUST use the existing standard brief, standard library pack and prompt version
- **AND** behavior MUST remain compatible with existing tests

#### Scenario: Low latency profile uses compact input

- **WHEN** external runtime profile is configured as `low-latency`
- **THEN** the request payload MUST use a compact brief and compact standard library pack
- **AND** compact brief MUST preserve candidate signals, evidence refs, allowed tags and safety-relevant hidden boundary
- **AND** compact standard library pack MUST preserve tag ids, labels, parent tags and teaching actions required for validation
- **AND** raw request content MUST NOT be persisted

### Requirement: Runtime request size telemetry is exported

系统 SHALL 导出 runtime profile 和 request size telemetry，帮助 live eval 判断延迟优化是否真实减少上下文体积。

#### Scenario: Ai invocation records compact request telemetry

- **WHEN** an external model call is made
- **THEN** `aiInvocation.runtimeProfile` MUST contain the active profile
- **AND** `aiInvocation.requestBytes` MUST contain the serialized request body byte size
- **AND** `aiInvocation.requestCompact` MUST indicate whether compact profile was used

#### Scenario: Live eval report includes profile telemetry

- **WHEN** live model eval writes a per-case report
- **THEN** each entry MUST include runtime profile, request bytes and request compact flag
- **AND** runtime fixture drafts for slow responses MUST include request size evidence

### Requirement: Low latency profile preserves quality gates

系统 SHALL 保持现有标签、证据引用和安全校验，不因 low-latency profile 放宽输出要求。

#### Scenario: Compact profile still validates diagnosis and teaching output

- **WHEN** low-latency profile is enabled and model output uses an invalid tag, invalid evidence ref or unsafe teaching hint
- **THEN** existing validation MUST reject or partially fallback in the same way as standard profile

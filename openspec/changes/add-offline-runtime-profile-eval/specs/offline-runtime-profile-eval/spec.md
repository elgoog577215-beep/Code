## ADDED Requirements

### Requirement: Offline runtime profile eval compares request size

系统 SHALL 在不调用外部 provider 的情况下生成 runtime profile 对比报告，用于比较 `standard` 和 `low-latency` 的 request size。

#### Scenario: Offline eval reports profile size delta

- **WHEN** offline runtime profile eval runs for an eval case
- **THEN** the report entry MUST include standard request bytes, low-latency request bytes, compression ratio and request compact flag
- **AND** the low-latency entry MUST indicate whether request bytes were reduced

#### Scenario: Offline eval does not persist raw request

- **WHEN** the offline profile report is written
- **THEN** the serialized report MUST NOT contain raw request JSON, chat messages, source code, API Key, Authorization header or bearer token

### Requirement: Offline profile eval preserves structural quality gates

系统 SHALL 验证 low-latency profile 在压缩输入时仍保留外部模型诊断所需的结构化契约。

#### Scenario: Compact profile preserves validation anchors

- **WHEN** low-latency offline eval is generated
- **THEN** each entry MUST report preserved candidate signal count, evidence ref count, issue tag count, fine tag count, teaching action count and hidden boundary presence
- **AND** qualityPreserved MUST be true only when evidence refs, issue tags and teaching actions are non-empty

#### Scenario: Compression failure produces actionable reasons

- **WHEN** low-latency request bytes are not lower than standard request bytes or required anchors are missing
- **THEN** the report entry MUST include failure reasons explaining the failed gate

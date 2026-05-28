## ADDED Requirements

### Requirement: Live eval entry records actual model route attribution

The live external assistant evaluation report SHALL include per-entry route attribution fields that identify the provider, model, and route role used for that sample.

#### Scenario: Diagnosis sample completed by external model

- **WHEN** a submission diagnosis live eval entry is produced from an analysis with `AiInvocation.fallbackUsed=false`
- **THEN** the entry MUST include `actualProvider` and `actualModel` from `AiInvocation`
- **THEN** the entry MUST mark `routeRole` as an external route rather than local fallback

#### Scenario: Diagnosis sample falls back to local rules

- **WHEN** a submission diagnosis live eval entry is produced from an analysis with missing invocation or `AiInvocation.fallbackUsed=true`
- **THEN** the entry MUST mark `routeRole` as `LOCAL_FALLBACK`
- **THEN** the entry MUST NOT be counted as completed external model output

#### Scenario: Diagnosis sample falls back after an external route failure

- **WHEN** a submission diagnosis live eval entry is produced from an analysis with `AiInvocation.fallbackUsed=true`
- **WHEN** `AiInvocation` still records the external provider and model that failed before local fallback
- **THEN** the entry MUST expose that provider and model for route outcome aggregation
- **THEN** the entry MUST keep `completedOutput=false`

#### Scenario: Report JSON remains backward compatible

- **WHEN** a live eval report containing route attribution fields is written
- **THEN** the report MUST remain valid UTF-8 JSON
- **THEN** existing top-level fields such as `model`, `status`, `fallbackUsed`, and `completedOutput` MUST remain present

### Requirement: Route attribution supports later aggregation

The route attribution fields SHALL use stable string values suitable for later analysis of completion rate and failure reason by route.

#### Scenario: Route role is stable for fallback route success

- **WHEN** a diagnosis sample is completed by a configured fallback external route
- **THEN** the entry MUST expose the fallback provider and fallback model
- **THEN** the entry MUST use a stable route role value that can be grouped separately from primary route and local fallback

#### Scenario: Route attribution does not hide runtime failure

- **WHEN** an entry fails at runtime or uses local fallback
- **THEN** route attribution fields MUST NOT cause `completedOutput` to become true
- **THEN** the original `failureStage` and `failureReason` MUST remain available

### Requirement: Live eval report aggregates route outcomes

The live external assistant evaluation report SHALL include top-level route outcome statistics that summarize completion, runtime failure, and failure reasons by route role/provider/model.

#### Scenario: Route outcome separates primary fallback and local fallback

- **WHEN** a report contains entries from primary route, fallback route, and local fallback
- **THEN** the report MUST expose separate route outcome rows for each distinct route role/provider/model group
- **THEN** each row MUST include total count, completed count, runtime failure count, and failure reason counts

#### Scenario: Route outcome keeps quality and capacity separate

- **WHEN** a route outcome contains both completed outputs and runtime failures
- **THEN** completed output counts MUST be based only on `completedOutput=true`
- **THEN** runtime failure counts and failure reason counts MUST be based only on entries where `completedOutput` is not true

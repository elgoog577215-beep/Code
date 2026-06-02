## ADDED Requirements

### Requirement: Runtime attribution consumes transport telemetry

The system SHALL consume external model transport telemetry when building AI quality runtime attribution.

#### Scenario: Quota failure with stream no content is visible

- **WHEN** an analysis has `aiInvocation.transportMode=stream`, `streamContentChunkCount=0`, and a quota-related failure reason
- **THEN** the runtime attribution signal MUST expose stream no-content count and summarize that the request reached the provider but returned no usable content chunks

#### Scenario: Budget guard short-circuit remains distinct

- **WHEN** an analysis has a `BUDGET_GUARD_OPEN` failure reason and no transport mode
- **THEN** runtime attribution MUST keep it classified as budget guard short-circuit rather than provider stream no-content

#### Scenario: Source segments expose transport counts

- **WHEN** AI quality trend builds source segments from analyses with transport telemetry
- **THEN** each source segment MUST include transport mode, stream no-content count, stream invalid chunk count, and stream fallback retry count

#### Scenario: Missing telemetry is backwards compatible

- **WHEN** older analyses do not contain transport telemetry fields
- **THEN** runtime attribution and trend responses MUST still be built with empty transport mode and zero transport counters

#### Scenario: Transport telemetry remains safe

- **WHEN** runtime attribution or trend DTOs serialize transport telemetry
- **THEN** they MUST NOT include raw prompts, raw response bodies, stream chunk contents, API keys, or authorization headers

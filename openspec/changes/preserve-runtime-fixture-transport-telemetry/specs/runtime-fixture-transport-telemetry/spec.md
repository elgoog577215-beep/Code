## ADDED Requirements

### Requirement: Runtime fixture drafts preserve transport telemetry

The system SHALL preserve external model transport telemetry when exporting runtime fixture drafts.

#### Scenario: Classroom runtime draft keeps stream no-content evidence

- **WHEN** an analysis has `aiInvocation.transportMode=stream`, `streamContentChunkCount=0`, and a runtime fallback status
- **THEN** the exported runtime fixture draft MUST include `transportMode=stream`, stream chunk counters, and a must-mention marker for stream content chunk count

#### Scenario: Budget guard draft remains a local short-circuit

- **WHEN** an analysis has `failureReason=BUDGET_GUARD_OPEN` and no transport mode
- **THEN** the exported runtime fixture draft MUST keep transport mode empty and stream counters at zero

#### Scenario: Teacher fixture preview shows runtime transport telemetry

- **WHEN** a runtime fixture draft contains transport telemetry
- **THEN** the teacher fixture preview MUST render compact transport badges next to the runtime draft item

#### Scenario: Live model runtime draft keeps transport telemetry

- **WHEN** a live model eval entry contains stream telemetry
- **THEN** the generated live eval runtime fixture draft MUST preserve transport mode, stream counters, finish reason, and fallback retry flag

#### Scenario: Transport fixture evidence remains safe

- **WHEN** runtime fixture drafts serialize transport telemetry
- **THEN** they MUST NOT include raw stream chunk contents, raw prompts, raw responses, API keys, authorization headers, or provider raw payloads

## ADDED Requirements

### Requirement: External model invocations expose transport telemetry

The system SHALL attach safe transport telemetry to external model invocation metadata after a ModelScope chat completion attempt.

#### Scenario: Streaming response records chunk counts

- **WHEN** the external model returns a streaming response with reasoning and content chunks
- **THEN** `aiInvocation` MUST record `transportMode=stream`, total stream chunk count, content chunk count, reasoning chunk count, invalid chunk count, and finish reason

#### Scenario: Reasoning chunks do not become final content

- **WHEN** a streaming response contains `reasoning_content`
- **THEN** the system MUST count the reasoning chunks but MUST NOT append reasoning text to the final model JSON content

#### Scenario: Non-stream fallback retry is visible

- **WHEN** a non-stream response has no usable message content and streaming fallback succeeds
- **THEN** `aiInvocation` MUST record `streamFallbackRetryUsed=true` and the final transport mode MUST reflect the successful streaming call

#### Scenario: Live model eval exports transport telemetry

- **WHEN** live-model-eval writes a per-case report entry
- **THEN** each entry MUST include the external model transport telemetry copied from `aiInvocation`

#### Scenario: Telemetry does not expose sensitive payloads

- **WHEN** transport telemetry is serialized
- **THEN** it MUST NOT include raw prompts, raw response bodies, stream chunk contents, API keys, or authorization headers

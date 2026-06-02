## ADDED Requirements

### Requirement: Teacher workbench surfaces transport attribution

教师工作台 SHALL surface external model transport attribution from existing AI quality responses without exposing sensitive payloads.

#### Scenario: Runtime attribution shows stream no-content

- **WHEN** an assignment AI quality response contains `runtimeAttributionSignal.primaryTransportMode=stream` and `streamNoContentCount>0`
- **THEN** the AI quality model attribution area MUST show the transport mode and stream no-content count near the model attribution summary

#### Scenario: Runtime attribution keeps empty telemetry quiet

- **WHEN** an assignment AI quality response has no transport mode and all stream transport counters are zero
- **THEN** the AI quality model attribution area MUST continue showing the model attribution summary without adding misleading transport badges

#### Scenario: Source quality segments show transport counters

- **WHEN** an AI quality trend source segment includes `transportMode`, `streamNoContentCount`, `streamInvalidChunkCount`, or `streamFallbackRetryCount`
- **THEN** the source quality segment MUST display those transport counters in a compact metadata row

#### Scenario: Transport visibility remains safe

- **WHEN** transport attribution is rendered in the teacher workbench
- **THEN** it MUST NOT display raw prompts, raw model responses, raw stream chunk contents, API keys, authorization headers, or request payloads

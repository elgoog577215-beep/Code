## ADDED Requirements

### Requirement: External Model Calls Must Support A Fallback Route

The system SHALL support one optional fallback OpenAI-compatible route for external model calls.

#### Scenario: Primary route fails before producing usable output

- **GIVEN** the primary route is configured
- **AND** a fallback route is configured
- **WHEN** the primary route fails because of quota, rate limit, unsupported model, timeout, API error, or empty response
- **THEN** the system attempts the fallback route before returning a rule fallback
- **AND** the final assistant result is still validated by the existing diagnosis, teaching, or coach safety gates

#### Scenario: Fallback route is not fully configured

- **GIVEN** the fallback base URL, API key, or model is blank
- **WHEN** the primary route fails
- **THEN** the system keeps the existing single-route fallback behavior
- **AND** the failure reason remains visible to eval reports

### Requirement: Budget Guard Must Be Route Scoped

The system SHALL scope budget guard state by provider and model so one route does not unnecessarily block another route.

#### Scenario: Primary route guard is open

- **GIVEN** the primary provider/model is blocked by the budget guard
- **AND** a fallback provider/model is configured and not blocked
- **WHEN** an external model call is requested
- **THEN** the system skips the primary route
- **AND** attempts the fallback route

### Requirement: Invocation Metadata Must Record The Successful Route

The submission diagnosis result SHALL record the provider and model that produced the accepted model output.

#### Scenario: Fallback route succeeds

- **GIVEN** the primary route fails
- **AND** the fallback route succeeds and passes validation
- **WHEN** the diagnosis response is returned
- **THEN** `aiInvocation.provider`, `aiInvocation.model`, and `aiInvocation.modelVersion` identify the fallback route

### Requirement: Live Eval Must Use The Same Fallback Route Configuration

The live assistant evaluation SHALL inject configured fallback route settings into the same services used by production assistant paths.

#### Scenario: Eval fallback route environment variables are present

- **GIVEN** `AI_EVAL_FALLBACK_BASE_URL`, `AI_EVAL_FALLBACK_API_KEY`, and `AI_EVAL_FALLBACK_MODEL` are set
- **WHEN** live eval constructs diagnosis and coach services
- **THEN** both services can attempt the fallback route
- **AND** the report route profile reflects that a fallback route is configured

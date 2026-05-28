## ADDED Requirements

### Requirement: External model calls support an optional route pool

The system SHALL support an optional list of additional OpenAI-compatible model routes after the existing primary route and single fallback route.

#### Scenario: Additional route is configured

- **GIVEN** the primary route is configured
- **AND** `ai.routes` contains at least one fully configured additional route
- **WHEN** the primary route and single fallback route fail or are blocked by the budget guard
- **THEN** the system attempts the additional route before returning local rule fallback
- **AND** accepted model output still passes the existing diagnosis, teaching, or coach validation gates

#### Scenario: Route pool is not configured

- **GIVEN** `ai.routes` is blank
- **WHEN** an external model call is requested
- **THEN** the system keeps the existing primary-plus-fallback route behavior

### Requirement: Route pool configuration is environment friendly

The system SHALL parse additional routes from a semicolon-separated string using `provider|baseUrl|apiKey|model` entries.

#### Scenario: Invalid route pool entry exists

- **GIVEN** `ai.routes` contains a malformed entry or an entry with blank base URL, API key, or model
- **WHEN** the route list is built
- **THEN** the malformed route is skipped
- **AND** valid primary, fallback, or additional routes remain usable

### Requirement: Budget guard remains route scoped across the route pool

The system SHALL keep budget guard state scoped by provider and model for every configured route.

#### Scenario: Earlier route guard is open

- **GIVEN** the primary provider/model is blocked by the budget guard
- **AND** a later route pool provider/model is configured and not blocked
- **WHEN** an external model call is requested
- **THEN** the system skips the blocked route
- **AND** attempts the later route

### Requirement: Invocation metadata records the route that produced the output

The system SHALL keep recording the provider and model that actually produced accepted model output, including route pool entries.

#### Scenario: Route pool entry succeeds

- **GIVEN** earlier routes fail
- **AND** a route pool entry succeeds and passes validation
- **WHEN** the diagnosis response is returned
- **THEN** `aiInvocation.provider`, `aiInvocation.model`, and `aiInvocation.modelVersion` identify the successful route pool entry

### Requirement: Live eval exposes route pool configuration and attribution

The live external assistant evaluation SHALL expose route pool configuration and group route pool completions separately from primary, single fallback, and local fallback.

#### Scenario: Route pool is configured for live eval

- **GIVEN** `AI_EVAL_ROUTES` contains one or more valid route pool entries
- **WHEN** live eval constructs diagnosis and coach services
- **THEN** the same route pool string is injected into those services
- **AND** the report route profile records route pool count, providers, models, and total configured route count

#### Scenario: Route pool entry produces an eval result

- **GIVEN** a diagnosis eval entry records an `AiInvocation` provider/model that matches a configured route pool entry
- **WHEN** the report entry is built
- **THEN** the entry marks `routeRole` as `ROUTE_POOL`
- **AND** route outcome aggregation can group route pool capacity separately

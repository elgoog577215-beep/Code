## ADDED Requirements

### Requirement: System exposes AI route health

The system SHALL expose a read-only AI route health endpoint that describes current external model route configuration without calling any external model.

#### Scenario: Route health endpoint is requested

- **WHEN** `GET /api/system/ai-route-health` is called
- **THEN** the response includes AI enabled status, configured route count, usable route count, route health level, summary, suggestions, and route entries
- **AND** the endpoint does not call external model providers

### Requirement: Teacher management surfaces route health

The teacher management system status view SHALL surface AI route health so operators can distinguish external model readiness from local execution readiness.

#### Scenario: Teacher opens system status

- **WHEN** the teacher management page loads system status
- **THEN** it requests AI route health
- **AND** it displays the route health level, usable route count, fallback status, route-pool status, suggestions, and route entries
- **AND** it does not display API keys or secret values

### Requirement: Route health never leaks secrets

The AI route health response SHALL NOT expose API keys or reversible secret values.

#### Scenario: Route has an API key

- **GIVEN** a primary, fallback, or route-pool entry has an API key
- **WHEN** route health is built
- **THEN** the response reports that the route is configured
- **AND** the response does not include the API key value

#### Scenario: Route base URL contains credential query parameters

- **GIVEN** a route base URL contains `key`, `token`, or `api_key` query parameters
- **WHEN** route health is built
- **THEN** credential query parameter values are masked

### Requirement: Route health classifies deployment risk

The system SHALL classify route health into stable risk levels.

#### Scenario: AI disabled

- **GIVEN** `ai.enabled=false`
- **WHEN** route health is built
- **THEN** `healthLevel` is `DISABLED`

#### Scenario: No usable external route

- **GIVEN** AI is enabled
- **AND** no route has base URL, API key, and model all configured
- **WHEN** route health is built
- **THEN** `healthLevel` is `NO_ROUTE`

#### Scenario: Only one usable route

- **GIVEN** AI is enabled
- **AND** exactly one route is usable
- **WHEN** route health is built
- **THEN** `healthLevel` is `SINGLE_ROUTE_RISK`
- **AND** the suggestions explain that quota or rate limits can block external model completion

#### Scenario: Multiple usable routes exist

- **GIVEN** AI is enabled
- **AND** two or more routes are usable
- **WHEN** route health is built
- **THEN** `healthLevel` is `MULTI_ROUTE_READY`

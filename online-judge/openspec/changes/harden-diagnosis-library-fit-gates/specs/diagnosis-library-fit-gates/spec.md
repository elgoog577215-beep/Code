## ADDED Requirements

### Requirement: HIT must bind a known standard library ID
The system SHALL reject AI diagnosis outputs where `diagnosisDecision.libraryFit` is `HIT` but no non-`OUT_OF_LIBRARY` anchor contains a known standard library ID.

#### Scenario: HIT without ID is rejected
- **WHEN** an AI diagnosis output marks `libraryFit` as `HIT` and all anchors have blank or null IDs
- **THEN** validation fails with an invalid tag or invalid JSON reason

#### Scenario: HIT with known ID is accepted
- **WHEN** an AI diagnosis output marks `libraryFit` as `HIT` and includes a known standard library anchor ID
- **THEN** validation accepts the diagnosis metadata

### Requirement: Unknown IDs must not become silent growth candidates
The system SHALL reject non-`OUT_OF_LIBRARY` diagnosis anchors that reference unknown standard library IDs.

#### Scenario: Unknown mistake point ID is rejected
- **WHEN** an AI diagnosis anchor has type `MISTAKE_POINT` and an ID not present in the selected standard library pack
- **THEN** validation fails instead of converting the anchor to `OUT_OF_LIBRARY`

### Requirement: Library growth candidates only come from gaps
The system SHALL persist standard library growth candidates only when diagnosis metadata indicates `PARTIAL`, `MISS`, or `OUT_OF_LIBRARY`.

#### Scenario: HIT does not create growth candidates
- **WHEN** an AI output marks `libraryFit` as `HIT`
- **THEN** the growth service ignores `libraryGrowth.candidates`

#### Scenario: MISS can create growth candidates
- **WHEN** an AI output marks `libraryFit` as `MISS` and provides valid `libraryGrowth.candidates`
- **THEN** the growth service can persist those candidates for teacher review

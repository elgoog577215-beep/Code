## ADDED Requirements

### Requirement: Zip data import SHALL accept only simple flat `.in` / `.out` archives
The system SHALL import problem test data from `.zip` archives that contain only root-level paired `.in` and `.out` files.

#### Scenario: Archive contains valid pairs
- **WHEN** a zip contains `game001.in`, `game001.out`, `game002.in`, and `game002.out` at archive root
- **THEN** preview SHALL detect two test point pairs
- **AND** the detected points SHALL be sorted by their numeric component

#### Scenario: Archive contains folders
- **WHEN** a zip entry is inside a folder or is itself a directory
- **THEN** preview SHALL reject the archive with a clear validation issue

#### Scenario: Archive contains unrelated files
- **WHEN** a zip contains files that are not `.in` or `.out`
- **THEN** preview SHALL reject the archive with a clear validation issue

### Requirement: Test point names SHALL contain exactly one continuous digit group
The system SHALL pair and order test point files using exactly one continuous digit group in the shared basename.

#### Scenario: Basename has one digit group
- **WHEN** a pair is named `tribool4.in` and `tribool4.out`
- **THEN** preview SHALL accept the pair and use numeric value `4` for ordering

#### Scenario: Basename has no digit group
- **WHEN** a file is named `game.in`
- **THEN** preview SHALL reject it because no test point number can be inferred

#### Scenario: Basename has multiple digit groups
- **WHEN** a file is named `T1-1.in`
- **THEN** preview SHALL reject it because the inferred test point number is ambiguous

### Requirement: Zip import SHALL validate pairs before committing
The system SHALL provide a preview step before writing a data package as active problem data.

#### Scenario: Missing output file
- **WHEN** a zip contains `case001.in` but not `case001.out`
- **THEN** preview SHALL report the missing output pair
- **AND** commit SHALL be unavailable or fail without changing active problem data

#### Scenario: Preview succeeds
- **WHEN** preview detects a valid archive
- **THEN** the teacher SHALL see pair count, total uploaded bytes, uncompressed bytes, and ordered test point rows before commit

### Requirement: Zip import SHALL enforce safety limits
The system SHALL enforce archive safety limits before commit.

#### Scenario: Zip exceeds size limits
- **WHEN** compressed size exceeds 50 MB or uncompressed size exceeds 100 MB
- **THEN** preview SHALL reject the archive

#### Scenario: Archive attempts path traversal
- **WHEN** any zip entry uses `../`, an absolute path, a Windows drive prefix, or another unsafe path
- **THEN** preview SHALL reject the archive

#### Scenario: Too many test point pairs
- **WHEN** a zip contains more than 50 pairs
- **THEN** preview SHALL reject the archive

### Requirement: Imported data points SHALL be shown as independent configurable test points
After a valid import, the system SHALL display each detected pair as an independent test point row.

#### Scenario: Data point table renders imported pairs
- **WHEN** a valid zip is committed
- **THEN** the Data settings tab SHALL display rows numbered `#1`, `#2`, `#3`, etc.
- **AND** each row SHALL show the original `.in` and `.out` file names

#### Scenario: Teacher configures row settings
- **WHEN** a teacher edits a data point row
- **THEN** the teacher SHALL be able to configure public-example visibility, time limit, memory limit, subtask index, and score

### Requirement: File contents SHALL be hidden by default and previewed safely
The system SHALL NOT display full uploaded data file contents by default.

#### Scenario: Teacher opens file preview
- **WHEN** a teacher requests preview for a data point input or output file
- **THEN** the system SHALL return at most the first 10 lines
- **AND** long lines SHALL be truncated to a safe display length

#### Scenario: Student views problem
- **WHEN** a student opens a problem with hidden file-backed test points
- **THEN** hidden input and output file contents SHALL NOT be exposed through the problem API or rendered page

### Requirement: Public example selection SHALL update the statement samples
The system SHALL allow teachers to mark imported data points as public examples and generate the statement sample section from those selected points.

#### Scenario: Teacher marks a data point as public example
- **WHEN** a teacher selects the public example checkbox for a data point
- **THEN** the test point SHALL be saved as non-hidden
- **AND** the statement sample section SHALL include that point's input and output content within configured display limits

#### Scenario: Selected example is too large
- **WHEN** a selected public example exceeds the safe sample display limit
- **THEN** the workbench SHALL warn the teacher that the point is unsuitable as a public example
- **AND** the rendered statement SHALL NOT display an unbounded amount of data

### Requirement: Judge SHALL support inline and file-backed test cases
The judging flow SHALL resolve each test case's input and expected output from either inline text fields or file references.

#### Scenario: Judge executes file-backed case
- **WHEN** a problem contains a file-backed test case
- **THEN** the judge SHALL read input and expected output from the stored files
- **AND** output comparison SHALL preserve the current normalization behavior

#### Scenario: Existing inline case is judged
- **WHEN** a problem contains an existing inline test case
- **THEN** judging SHALL continue to use the inline input and expected output fields


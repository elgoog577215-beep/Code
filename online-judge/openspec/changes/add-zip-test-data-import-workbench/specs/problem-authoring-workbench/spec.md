## ADDED Requirements

### Requirement: Problem authoring SHALL use a four-tab workbench
The teacher problem create and edit experience SHALL organize one shared problem draft into four top-level tabs: Statement settings, Problem settings, Data settings, and Help.

#### Scenario: Teacher switches authoring tabs
- **WHEN** a teacher edits fields in one tab and switches to another tab
- **THEN** the current draft values SHALL remain available
- **AND** saving SHALL persist the complete draft rather than only the active tab

#### Scenario: Teacher opens an existing problem
- **WHEN** a teacher opens an existing problem for editing
- **THEN** the workbench SHALL populate all four tabs from the current problem data
- **AND** existing inline test cases SHALL remain editable or visible through the data settings tab

### Requirement: Statement settings SHALL own the canonical problem status
The system SHALL expose one canonical problem status field, and it SHALL be placed in Statement settings.

#### Scenario: Teacher changes problem status
- **WHEN** a teacher selects public, hidden, partially visible, or contest problem status
- **THEN** the selected status SHALL be saved as the problem's canonical status
- **AND** the same status SHALL NOT be duplicated as a separate conflicting field in Problem settings

### Requirement: Statement sections SHALL be structured and Markdown-previewable
The system SHALL store or derive the student-visible statement from structured sections: title, background, description, input format, output format, samples, and hints.

#### Scenario: Teacher edits required statement sections
- **WHEN** title, description, input format, or output format is blank
- **THEN** the workbench SHALL block save and show the missing required section

#### Scenario: Teacher leaves optional statement sections blank
- **WHEN** background or hints are blank
- **THEN** those sections SHALL be omitted from the rendered student statement

#### Scenario: Teacher previews Markdown
- **WHEN** a teacher opens preview for any Markdown statement section
- **THEN** the workbench SHALL render the Markdown using the existing local Markdown preview behavior or an equivalent project-standard renderer

### Requirement: Markdown import SHALL populate recognized statement sections
The workbench SHALL support importing a `.md` document into Statement settings by recognizing a level-one title and known level-two section headings.

#### Scenario: Markdown contains recognized headings
- **WHEN** imported Markdown includes `## 题目背景`, `## 题目描述`, `## 输入格式`, `## 输出格式`, `## 样例`, or `## 提示说明`
- **THEN** the content under each heading SHALL populate the matching statement section

#### Scenario: Markdown contains a level-one title
- **WHEN** imported Markdown includes a `#` heading before section content
- **THEN** the title field SHALL be populated from that heading unless the teacher chooses to keep the existing title

#### Scenario: Markdown import runs
- **WHEN** a teacher imports Markdown
- **THEN** the import SHALL NOT change the canonical problem status

### Requirement: Problem settings SHALL preserve existing difficulty model
Problem settings SHALL use the existing difficulty values `EASY`, `MEDIUM`, and `HARD`.

#### Scenario: Teacher sets difficulty
- **WHEN** a teacher selects problem difficulty
- **THEN** the saved value SHALL be one of `EASY`, `MEDIUM`, or `HARD`

### Requirement: Problem settings SHALL include metadata controls
Problem settings SHALL allow teachers to configure provider, attachments, tags, data download, and score display mode.

#### Scenario: Teacher configures score display
- **WHEN** the teacher chooses OI mode
- **THEN** the student-facing or teacher-facing result views MAY show configured scores
- **WHEN** the teacher chooses ICPC mode
- **THEN** configured scores SHALL be hidden from normal student-facing problem result display

#### Scenario: Teacher configures data download
- **WHEN** data download is off
- **THEN** the data package download endpoint SHALL not provide the package to ordinary users
- **WHEN** data download is on
- **THEN** the system MAY provide a download link according to teacher/admin access policy

### Requirement: Help tab SHALL explain authoring and data rules
The Help tab SHALL provide teacher-facing instructions for zip upload, test point configuration, LF line endings, Markdown import, sample generation, and preview limits.

#### Scenario: Teacher opens Help
- **WHEN** the Help tab is active
- **THEN** the teacher SHALL see the zip file requirements and `.in` / `.out` pairing rules
- **AND** the teacher SHALL see the Markdown import heading rules

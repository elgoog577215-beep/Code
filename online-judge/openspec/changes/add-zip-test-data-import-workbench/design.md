## Context

Current problem creation uses `CreateProblemRequest` with title, Markdown description, difficulty, limits, optional AI prompt fields, and inline test cases. `ProblemService` saves each test case's full input and expected output into `test_cases.input` and `test_cases.expected_output`. `JudgeService` loads all test cases and passes `testCase.getInput()` to the executor.

The target workflow introduces two related changes:

1. The teacher-facing editor becomes a structured workbench rather than a single long form.
2. Test cases can be file-backed so large `.in` / `.out` data can be uploaded, stored, previewed, and judged without being pasted into the browser.

## Goals / Non-Goals

**Goals:**

- Make problem authoring feel like a real OJ backend for contest data.
- Keep the zip format intentionally simple: a flat archive containing only paired `.in` and `.out` files.
- Keep data safe and inspectable: preview before commit, default-hidden file content, first-10-line previews only.
- Preserve existing small inline test cases and existing judging behavior.
- Let public examples be selected from uploaded data points and reflected in the statement sample section.
- Keep difficulty as `EASY`, `MEDIUM`, `HARD`.
- Keep one canonical problem status in Statement settings.

**Non-Goals:**

- No nested directory layout, manifest, `rar`, `.ans`, custom judge, or custom scoring script in the first version.
- No full large-file rendering in the browser.
- No automatic problem-status inference from imported Markdown.

## Decisions

### 1. Four-tab workbench

The new/create/edit page uses top tabs:

```text
[Statement settings] [Problem settings] [Data settings] [Help]
```

All tabs edit one shared draft. Saving persists the complete problem draft and current data configuration. The teacher can move between tabs without losing changes.

### 2. Structured statement model

The statement should be represented as sections rather than one opaque Markdown blob:

- status: required enum, one canonical problem status field
- title: required
- background: optional Markdown
- description: required Markdown
- input format: required Markdown
- output format: required Markdown
- samples: generated from data points marked as public examples, with safe manual text adjustment allowed
- hints: optional Markdown

Empty optional sections are omitted from the rendered problem statement. Required sections block save when blank.

For compatibility, the backend can still compose a legacy `description` Markdown field for existing student APIs, but the source of truth for authoring should be the structured fields once the migration lands.

### 3. Markdown import

Statement Markdown import reads:

- `# Title` as the title when present.
- Level-two headings as section boundaries.
- Recognized headings:
  - `题目背景`
  - `题目描述`
  - `输入格式`
  - `输出格式`
  - `样例`
  - `提示说明`

The import does not recognize or alter problem status. Unknown sections may be appended to hints or left in a review area depending on implementation simplicity; they must not silently overwrite required fields.

### 4. Problem settings remain conservative

Problem settings include:

- provider
- attachments
- difficulty: existing `EASY`, `MEDIUM`, `HARD`
- tags: algorithm/source/time-style tag categories can be stored as plain tag lists initially
- data download: on/off
- score display mode:
  - OI: show scores
  - ICPC: hide scores

Problem status is not duplicated here.

### 5. Flat zip data format

The first version accepts only `.zip` files containing files directly at the archive root:

```text
game001.in
game001.out
game002.in
game002.out
```

Rules:

- No directories.
- No unrelated files.
- Every input file must have a matching output file.
- Input extension must be `.in`.
- Output extension must be `.out`.
- Each file basename must contain exactly one continuous digit group.
- `game001.in` is valid.
- `tribool4.in` is valid.
- `T1-1.in` is invalid because it has two digit groups.
- `game.in` is invalid because it has no digit group.

Detected pairs are sorted by numeric value, then displayed as system test points `#1`, `#2`, `#3`, etc. The original file names remain visible as read-only metadata.

### 6. Preview before commit

Zip import uses a two-stage workflow:

```text
Upload zip -> preview detected pairs and validation issues -> commit import
```

Preview validates structure, counts pairs, estimates compressed/uncompressed size, and reports invalid names or missing pairs. Commit stores files and creates or replaces test point metadata only after preview passes.

Recommended endpoints:

```text
POST /api/teacher/problems/{problemId}/test-data/import-preview
POST /api/teacher/problems/{problemId}/test-data/import-commit
GET  /api/teacher/problems/{problemId}/test-data/{testCaseId}/preview
GET  /api/teacher/problems/{problemId}/test-data/download
```

Preview and commit should use `multipart/form-data` with a `file` field. If implementation prefers re-uploading on commit, preview results should include a short-lived upload token; otherwise commit can accept the same zip again and revalidate.

### 7. File-backed test cases

Extend test cases to support both inline and file-backed data:

```text
input_storage_type: INLINE | FILE
output_storage_type: INLINE | FILE
input_file_path
output_file_path
input_file_name
output_file_name
input_size_bytes
output_size_bytes
input_sha256
output_sha256
time_limit_ms
memory_limit_kib
subtask_index
score
score_group_rule
```

Existing inline test cases keep working. File-backed test cases should leave `input` and `expected_output` either empty-safe or populated only for selected small public examples if backward compatibility requires it. The judging path should resolve the content through a helper that understands storage type.

### 8. Data settings UI

Data settings includes:

- Upload data
- Download data
- Uploaded byte count
- One-click fill
- Test point table
- Scoring rule controls

Each row shows:

- system index, e.g. `#1`
- original `.in` / `.out` file names
- time limit
- memory limit
- subtask index
- score
- public example checkbox
- preview action

The preview action shows at most the first 10 lines. Long lines should be truncated to prevent layout and memory problems.

### 9. Public examples generated from selected data points

When a teacher marks a data point as a public example:

- The point's `hidden` flag becomes false.
- The statement sample section includes the selected input and output.
- Generated sample content is bounded; if the sample is too large, the UI warns that it is unsuitable as a public sample.

Unselected file-backed points are hidden by default.

### 10. Safety limits

First-version limits:

- zip size: max 50 MB
- uncompressed total size: max 100 MB
- test point pairs: max 50
- single test time: max 10 seconds
- memory: max 512 MiB
- paths must be relative archive-root file names only
- reject `../`, absolute paths, Windows drive prefixes, directories, empty names, duplicate basenames, and unsupported extensions

The storage service should write to a controlled directory such as:

```text
data/problem-test-data/{problemId}/{importBatchId}/
```

Commit should be batch-oriented: write a new import batch, persist metadata transactionally, then switch the problem to the new batch. Cleanup can remove orphaned old batches after successful replacement or through a maintenance job.

## Risks / Trade-offs

- File-backed judging may require reading large files into memory initially. This is acceptable for the first 100 MB total limit but should be isolated behind a resolver so streaming can be added later.
- Replacing data packages can invalidate old test case IDs. The design should keep submission history snapshots stable and avoid deleting historical submission results.
- Automatically generating statement samples from files is useful, but oversized public examples are bad UX. The UI should warn and cap rendering.
- The current database schema may require careful migration because existing `test_cases.input` and `expected_output` are non-null.

## Open Questions

- Should commit replace all existing test points for the problem, or allow merge/append? Recommendation for first version: replace all current data points after explicit confirmation.
- Should inline manually added samples coexist with uploaded file examples? Recommendation: yes, but the UI should label their source clearly.
- Should `.out` content normalize trailing whitespace before judging? Recommendation: keep current output normalization behavior for comparison, but store file bytes unchanged.

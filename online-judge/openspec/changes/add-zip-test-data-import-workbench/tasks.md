## 1. Data Model and Migration

- [x] 1.1 Add problem fields for canonical status, structured statement sections, provider, attachments metadata, tags, data download flag, and score display mode.
- [x] 1.2 Extend test cases with inline/file storage type, file metadata, per-case time limit, memory limit, subtask index, score, and public example source metadata.
- [x] 1.3 Add migrations that preserve all existing inline test cases and keep current problem APIs backward compatible.
- [x] 1.4 Define file storage location, import batch identity, and cleanup behavior for replaced or orphaned data packages.

## 2. Backend Import and Data Services

- [x] 2.1 Add zip test data preview endpoint with multipart upload, flat archive validation, pair detection, sorting, and issue reporting.
- [x] 2.2 Add commit endpoint that stores a validated zip as a new batch and creates/replaces file-backed test case rows.
- [x] 2.3 Add test data download endpoint gated by the problem data-download setting.
- [x] 2.4 Add first-10-line preview endpoint for input/output files with long-line truncation.
- [x] 2.5 Add Markdown statement import parser for level-one title and recognized level-two sections.
- [x] 2.6 Update problem create/update/manage DTOs to carry structured statement, status, metadata, data settings, and file-backed test case summaries.

## 3. Judge Integration

- [x] 3.1 Add a test case content resolver that supports inline and file-backed input/output.
- [x] 3.2 Update `JudgeService` to read resolved input and expected output while preserving current comparison normalization.
- [x] 3.3 Ensure hidden file-backed data is not exposed through student problem APIs or submission result payloads.
- [x] 3.4 Preserve submission case result snapshots when a problem's data package is replaced later.

## 4. Frontend Workbench

- [x] 4.1 Replace the current problem authoring surface with four top tabs: Statement settings, Problem settings, Data settings, Help.
- [x] 4.2 Build structured Markdown editors with preview for background, description, input format, output format, samples, and hints.
- [x] 4.3 Add `.md` import to populate statement sections without changing problem status.
- [x] 4.4 Add problem settings controls for provider, attachments, `EASY/MEDIUM/HARD`, tags, data download, and score display mode.
- [x] 4.5 Add data settings upload, preview, commit, download, one-click fill, row editing, public sample checkbox, and first-10-line preview.
- [x] 4.6 Add Help tab content explaining zip rules, test point configuration, LF line endings, Markdown import, and preview limits.

## 5. Verification

- [ ] 5.1 Add backend tests for zip validation: valid pairs, missing pairs, nested folders, extra files, duplicate names, no digit group, multiple digit groups, size limits, and zip-slip paths.
- [x] 5.2 Add backend tests for Markdown section import and required statement fields.
- [ ] 5.3 Add judge tests for inline and file-backed test cases.
- [ ] 5.4 Add frontend tests for tab navigation, Markdown import, data preview, public sample generation, and save payloads.
- [x] 5.5 Run targeted backend and frontend validation before implementation is considered complete.

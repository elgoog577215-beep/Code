## Why

The current problem editor stores every test case input and expected output directly in database text fields and exposes test case editing as ordinary text areas. That works for small examples, but it does not fit contest-style problem data where one problem may contain dozens of large `.in` / `.out` pairs. Teachers need a workflow closer to an OJ problem administration page: author the statement, configure metadata, upload a simple zip data package, inspect detected test points, and choose which points become public examples.

The existing teacher problem import endpoints support Markdown, JSON, CSV, and XLSX content, but they do not support zip uploads, file-backed test data, or previewing large data safely. Without this capability, large data sets must either be pasted into the browser or skipped, both of which undermine real judging.

## What Changes

- Replace the current single-form problem authoring surface with a four-tab workbench:
  - Statement settings
  - Problem settings
  - Data settings
  - Help / instructions
- Keep one canonical problem status field in Statement settings.
- Keep the existing difficulty model: `EASY`, `MEDIUM`, `HARD`.
- Support Markdown statement import that maps level-two headings into structured statement sections.
- Add zip data upload for simple flat archives containing only paired `.in` / `.out` files.
- Detect numbered test points, sort them by number, and display them as `#1`, `#2`, `#3`, etc.
- Store large test data as file-backed test cases instead of copying full contents into database text fields.
- Hide file contents by default and show only the first 10 lines on demand.
- Allow teachers to mark data points as public examples; the statement sample section is generated from those selected points.
- Add download support for the currently uploaded data package when data download is enabled.

## Capabilities

### New Capabilities

- `problem-authoring-workbench`: Defines the tabbed problem authoring UI, structured statement sections, Markdown import, canonical status placement, and metadata editing.
- `zip-test-data-import`: Defines zip upload rules, test point pairing, file-backed test case storage, preview safety, sample selection, and judging integration.

### Modified Capabilities

- `teacher-console-ui`: The teacher problem management surface will route new/create/edit flows through the tabbed workbench.
- `database-first-content-source`: Problem data and file-backed test case metadata remain governed content and must be written through management workflows.

## Impact

- Frontend: new tabbed problem editor experience, Markdown section import, zip upload flow, test data table, preview drawer, sample selection, and help page.
- Backend: new multipart zip preview/commit endpoints, zip validation, file storage service, file-backed test case metadata, and safe preview endpoints.
- Database: extend problems and test cases with status, structured statement fields, metadata, scoring/configuration fields, and file storage references.
- Judge: resolve each test case input/output from either inline fields or file references.
- Operations: define storage location, upload size limits, zip-slip protection, cleanup policy, and backup expectations for file-backed data.

## Non-Goals

- Do not support `rar` or other archive formats.
- Do not support nested folders inside the zip in the first version.
- Do not require a manifest file.
- Do not support `.ans` output files in the first version; output files must use `.out`.
- Do not implement custom scoring scripts in the first version.
- Do not infer problem status from imported Markdown.
- Do not change the public student solving flow except for showing generated samples from selected public data points.

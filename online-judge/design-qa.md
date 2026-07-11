# Student Assignment Insights Design QA

- Assignment reference: `C:\Users\Lenovo\.codex\generated_images\019f41cc-0af4-7b10-b30d-0f51a0c34535\exec-a2a4c9ae-7f65-4c58-9acb-af26d0b9f4b8.png`
- Ranking reference: `C:\Users\Lenovo\.codex\generated_images\019f41cc-0af4-7b10-b30d-0f51a0c34535\exec-5b2da7db-3e68-4ff2-8fd1-576fc3c6bee2.png`
- Submission reference: `C:\Users\Lenovo\.codex\generated_images\019f41cc-0af4-7b10-b30d-0f51a0c34535\exec-bde7224a-3d7d-48a4-96a4-1ff8f560a9b1.png`
- Implementation captures: `output/playwright/student-assignment-*-light.png`, `output/playwright/student-assignment-ranking-*-light.png`, and `output/playwright/student-assignment-submissions-*-light.png`
- Side-by-side evidence: `output/design-qa/assignment-comparison.png`, `output/design-qa/ranking-comparison.png`, and `output/design-qa/submissions-comparison.png`
- Desktop comparison viewport: 1586 x 992. Responsive verification viewport: 390 x 844.

**Findings**

- No actionable P0, P1, or P2 visual differences remain.
- The three routes share the same assignment title, class metadata, deadline, primary action, and tab navigation. The selected tab remains clear without adding another page-level sidebar.
- The assignment page follows the reference hierarchy of summary band, task progress table, and compact guidance footer. The fixture has two real tasks rather than the reference's four sample tasks, so the page is intentionally shorter.
- The ranking page uses the reference's main-table and contextual-sidebar structure. It shows the first nine students when the current student is near the top, masks classmates, highlights the current student, and explicitly states that ranking is progress-only with shared ranks.
- The submission page matches the reference's summary, compact single-row filters, descending-time indicator, eight-row history, verdict badges, icon pagination, and numbered pages. Assistive labels remain available to screen readers without becoming visible duplicate headings. Opening a row produces a focused code-detail dialog and closing it returns to the unchanged list.
- Desktop width, outer frame, spacing rhythm, thin dividers, restrained teal accents, and 7-8px radii align with the existing product tokens and selected concepts.
- At 390px, summary metrics become a stable two-column grid, tables become readable stacked rows, filters stack without clipping, and the detail dialog remains inside the viewport.
- The concepts contain no photography or custom illustration. All controls use the project's Lucide icon library and existing design tokens.

**Functional Verification**

- [x] Three canonical routes render independently.
- [x] Assignment actions open the existing coding workspace.
- [x] Ranking uses completed-problem count and shared ranks, never runtime or submission speed.
- [x] Submission filters, pagination, and personal submission detail work.
- [x] Student authorization and class-bound assignment access are enforced by the backend.
- [x] Desktop and mobile light/dark captures have no horizontal overflow or page errors.
- [x] Browser smoke: 114 checks passed across three pages and two viewports; 24 focused checks passed again at the 1586 x 992 reference viewport.
- [x] Backend: 10 tests passed, including a full Spring/JPA context startup.

**Follow-up Polish**

- P3: assignment-level difficulty can replace the current hint-policy badge if the domain model later exposes that field.

final result: passed

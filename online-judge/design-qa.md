# Design QA — Student Problem Workbench

- source visual truth path: `C:\Users\Lenovo\AppData\Local\Temp\codex-clipboard-22561031-1621-48f2-b5b5-98c1dc356b6e.png`
- prior implementation reference: `C:\Users\Lenovo\AppData\Local\Temp\codex-clipboard-104f3ba0-3bac-4a2b-85c2-af577dc8aaf6.png`
- implementation screenshot path: `D:\WenCode\Code\online-judge\output\design-qa\student-problem-workbench.png`
- collapsed-state screenshot path: `D:\WenCode\Code\online-judge\output\design-qa\student-problem-workbench-collapsed.png`
- mobile screenshot path: `D:\WenCode\Code\online-judge\output\design-qa\student-problem-workbench-mobile.png`
- combined comparison path: `D:\WenCode\Code\online-judge\output\design-qa\student-problem-comparison.png`
- viewport: 1600 x 1000 desktop; 390 x 844 mobile
- state: signed-in student, first classroom task selected, editor open with starter code

## Full-view comparison evidence

The combined comparison places the selected reference and implementation in one frame. Both use a persistent primary rail, a secondary task list, and one full-height working canvas split between the statement and code editor. The implementation keeps the platform's existing top header and teal token system as intentional product-shell constraints.

## Focused interaction evidence

- The desktop capture shows the 50 / 50 default statement-editor split and visible drag handle.
- The collapsed-state capture shows the statement expanding across the canvas while a narrow, labeled code restore control remains available.
- The mobile capture shows primary navigation as a horizontal rail and the statement/editor stacking without horizontal overflow.
- The automated browser test drags the separator by 140 px and verifies the statement grows while the editor shrinks; it also collapses and restores the editor.

## Required fidelity surfaces

- Fonts and typography: the existing Chinese system font stack is retained. Page titles, problem headings, metadata, task labels, and editor chrome preserve a clear hierarchy without clipped text.
- Spacing and layout rhythm: the outer rail, task sidebar, statement, separator, and editor occupy one continuous full-height canvas. Borders replace the earlier detached-card gaps, matching the reference's denser workspace.
- Colors and visual tokens: the existing teal brand, pale active state, neutral dividers, dark code toolbar, and semantic task states closely match the reference direction.
- Image quality and asset fidelity: the reference contains no photographic or illustrative assets. Navigation, resize, collapse, and editor controls use the project's installed Lucide icon library consistently.
- Copy and content: navigation labels are concise; assignment title, task list, problem content, limits, language, starter code, and submission actions remain driven by application data.

## Findings

No actionable P0, P1, or P2 differences remain.

Accepted product constraints:

- The production platform header remains above the workbench so student/teacher switching, identity, language, and theme controls stay consistent across the app.
- The secondary sidebar lists this assignment's actual programming tasks rather than the reference site's mixed question-type categories.

## Comparison history

1. Initial implementation retained three detached cards and had no persistent primary rail, resize control, or collapse state. These were P1 structure and interaction mismatches.
2. The workbench was rebuilt with a persistent rail, secondary task list, border-based continuous canvas, resizable statement/editor split, and collapsible editor.
3. First automated resize check exposed a P1 layout constraint: later wide-screen CSS limited the working canvas, leaving the statement pinned at its minimum width. Final cascade rules now give the workbench the full viewport width; post-fix drag assertions pass.
4. Desktop, collapsed, and mobile states were recaptured. No clipping, overlap, missing primary action, or horizontal overflow remains in the tested states.

## Interaction and runtime checks

- Task switching links remain functional.
- Overview, task, submission, and ranking navigation remain functional.
- Drag separator supports pointer input and keyboard left/right adjustment.
- Code panel collapses and restores without losing source state.
- Language selection, editor, submit, and reset controls remain present.
- Browser console errors and page errors: none in the targeted workbench test.
- Automated checks passed: TypeScript typecheck, production build, 28 visual-smoke checks, problem workbench test, assignment overview test, six signed-in home tests, and guest layout test.

final result: passed

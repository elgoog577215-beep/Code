# Design QA — Student Problem Workbench

- source visual truth path: `C:\Users\Lenovo\AppData\Local\Temp\codex-clipboard-22561031-1621-48f2-b5b5-98c1dc356b6e.png`
- prior implementation reference: `C:\Users\Lenovo\AppData\Local\Temp\codex-clipboard-104f3ba0-3bac-4a2b-85c2-af577dc8aaf6.png`
- consistency reference — problem page: `C:\Users\Lenovo\AppData\Local\Temp\codex-clipboard-f0cbcd46-1169-4218-abda-d75fc5625802.png`
- consistency reference — assignment overview: `C:\Users\Lenovo\AppData\Local\Temp\codex-clipboard-98602bd7-aebd-4881-8fa3-9ec6c3df5123.png`
- edge-to-edge reference — problem page: `C:\Users\Lenovo\AppData\Local\Temp\codex-clipboard-e91d57d1-9da9-4b45-94c2-e5ca276b723e.png`
- edge-to-edge reference — assignment overview: `C:\Users\Lenovo\AppData\Local\Temp\codex-clipboard-88f922cc-b182-4724-9483-575fc25d58ef.png`
- implementation screenshot path: `D:\WenCode\Code\online-judge\output\design-qa\student-problem-workbench.png`
- final unified implementation screenshot path: `D:\WenCode\Code\online-judge\output\design-qa\student-problem-unified-final.png`
- final full-screen implementation screenshot path: `D:\WenCode\Code\online-judge\output\design-qa\student-problem-fullscreen.png`
- final chrome-alignment screenshot path: `D:\WenCode\Code\online-judge\output\design-qa\student-problem-chrome-final.png`
- collapsed-state screenshot path: `D:\WenCode\Code\online-judge\output\design-qa\student-problem-workbench-collapsed.png`
- mobile screenshot path: `D:\WenCode\Code\online-judge\output\design-qa\student-problem-workbench-mobile.png`
- combined comparison path: `D:\WenCode\Code\online-judge\output\design-qa\student-problem-comparison.png`
- viewport: 1600 x 1000 desktop; 390 x 844 mobile
- state: signed-in student, first classroom task selected, editor open with starter code

## Full-view comparison evidence

The combined comparison places the selected reference and implementation in one frame. Both use a persistent primary rail, a secondary task list, and one full-height working canvas split between the statement and code editor. The final consistency pass also verifies that the problem page now uses the exact shared assignment header component from the overview page: back action, assignment title, class, deadline, and student identity align in the same order and at the same height.

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
5. A consistency pass found a P1 shell mismatch: the overview had an assignment context header while the problem workbench started directly below the global header. Both pages now reuse `StudentAssignmentHeader`; the duplicate inner “返回学生端” action was removed and the workbench height was recalibrated for the added 72 px header. The final unified capture confirms matching hierarchy and alignment.
6. A final P1 canvas mismatch remained at wide viewports: the problem page inherited the general `main-shell` gutter, leaving 38 px of blank space on both sides and 21 px above the assignment header at 1600 px. The problem workbench now uses an edge-to-edge 100vw canvas and offsets the generic shell padding. Browser assertions confirm x = 0, width equals the viewport, and the assignment header starts immediately below the global header.
7. The final chrome pass removed the last subtle mismatch: problem-page rail icons were 21 px while the other assignment pages used 22 px. The problem page now uses the shared workspace and rail classes plus one shared dimension token set: 72 px assignment header, 84 px rail, 72 px rail items, identical padding and gap, and 22 px icons. Automated browser measurements compare both pages directly and require exact equality.

## Interaction and runtime checks

- Task switching links remain functional.
- Overview, task, submission, and ranking navigation remain functional.
- Drag separator supports pointer input and keyboard left/right adjustment.
- Code panel collapses and restores without losing source state.
- Language selection, editor, submit, and reset controls remain present.
- Browser console errors and page errors: none in the targeted workbench test.
- Automated checks passed: TypeScript typecheck, production build, 28 visual-smoke checks, problem workbench test, assignment overview test, six signed-in home tests, and guest layout test.

final result: passed

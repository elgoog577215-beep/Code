# Route Hub Design QA

- Source visual truth: `C:\Users\Lenovo\.codex\generated_images\019f41cc-0af4-7b10-b30d-0f51a0c34535\exec-d1dfd177-73b7-48dd-90cb-a0555d20ddca.png`
- Initial implementation screenshot: `D:\WenCode\Code\online-judge\output\design-qa\route-hub-desktop-light.png`
- Final implementation screenshot: `D:\WenCode\Code\online-judge\output\design-qa\route-hub-desktop-final.png`
- Mobile implementation screenshot: `D:\WenCode\Code\online-judge\output\playwright\app-entry-mobile-light.png`
- Final combined comparison: `D:\WenCode\Code\online-judge\output\design-qa\route-hub-comparison-final.png`
- Viewport: source 1487 x 1058; implementation desktop 1440 x 981 full-page capture; mobile 390 x 1734 full-page capture.
- State: light theme, public route hub, no authenticated role selected.

**Findings**

- No actionable P0, P1, or P2 differences remain.
- Fonts and typography: the implementation uses the existing PingFang SC/Microsoft YaHei stack and matches the source hierarchy with a two-line display headline, readable 14-16px interface copy, compact metadata, and zero negative letter spacing. Text remains unclipped on desktop and mobile.
- Spacing and layout rhythm: the same 35/65 desktop composition is present, with a full-height role introduction beside the code-and-feedback workspace. The learning loop uses three equally weighted evidence steps. Mobile collapses every region to one column with no horizontal overflow.
- Colors and visual tokens: the existing white, cool gray, charcoal, and muted teal tokens map closely to the concept. Green is limited to successful judge states and service health; borders and elevation remain restrained.
- Image and asset fidelity: the source contains no raster photography or custom illustration. All visible assets are operational UI and Lucide icons, matching the existing project icon system; no placeholder, custom SVG, CSS drawing, or generated decorative asset was introduced.
- Copy and content: the source headline, student/teacher role actions, three platform capabilities, live judge example, service status, and practice-judge-review loop are all represented with realistic product data.
- Interaction and accessibility: student and teacher actions retain canonical routes, focus-visible styles are present, semantic regions and labels describe the code demonstration, and theme/mobile browser checks pass without page errors.

**Open Questions**

- None blocking. The implementation intentionally omits the source mock's decorative save/menu controls inside the non-interactive demonstration so they are not mistaken for live commands.

**Full-view Evidence**

- `route-hub-comparison-final.png` places the selected concept and final implementation together at original scale. It confirms matching headline hierarchy, role actions, code workspace proportions, judge result hierarchy, and three-step learning loop.

**Focused-region Evidence**

- No separate crop is required: the original-resolution combined comparison keeps the headline, code editor, result panel, role actions, and learning-loop evidence readable. The mobile capture separately verifies responsive typography, controls, and stacking.

**Comparison History**

1. The previous production homepage was a sparse heading plus two empty role cards and did not express the selected concept's core learning experience.
2. The first implementation introduced the selected 35/65 experience, but the desktop headline wrapped to four lines and made the editor region excessively tall; the learning-loop steps also lacked concrete evidence. Result remained blocked by P2 hierarchy and density differences.
3. The headline size and hero padding were corrected to restore the source's two-line rhythm. Code, judge, and complexity evidence were added to the three learning-loop steps.
4. The final desktop/mobile captures pass all 45 focused browser checks with no horizontal overflow or page errors. No P0/P1/P2 mismatch remains.

**Implementation Checklist**

- [x] Headline and supporting copy match the selected concept.
- [x] Student and teacher entry actions point to existing canonical routes.
- [x] The first viewport demonstrates a realistic code submission and successful judge result.
- [x] The learning loop contains concrete practice, judge, and review evidence.
- [x] Desktop and mobile layouts have no horizontal overflow.
- [x] Light and dark themes remain readable.
- [x] Browser console has no page errors in focused checks.

**Follow-up Polish**

- P3: add the fourth score row and richer syntax highlighting only if exact screenshot parity is more important than keeping the homepage demonstration concise.

final result: passed

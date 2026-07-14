# 代码联动修正工作台 Design QA

- source visual truth path: `D:/WenCode/Code/docs/ui-mockups/feedback-workbench-concept.png`
- implementation screenshot path: `D:/WenCode/Code/docs/ui-mockups/feedback-workbench-implementation-desktop.png`
- mobile screenshot path: `D:/WenCode/Code/docs/ui-mockups/feedback-workbench-implementation-mobile.png`
- dark screenshot path: `D:/WenCode/Code/docs/ui-mockups/feedback-workbench-implementation-dark.png`
- English screenshot path: `D:/WenCode/Code/docs/ui-mockups/feedback-workbench-implementation-english.png`
- full-view comparison path: `D:/WenCode/Code/docs/ui-mockups/feedback-workbench-comparison.png`
- user scroll reference path: `D:/WenCode/Code/docs/ui-mockups/feedback-workbench-scroll-reference.png`
- user action reference path: `D:/WenCode/Code/docs/ui-mockups/feedback-workbench-action-reference.png`
- focused scroll comparison path: `D:/WenCode/Code/docs/ui-mockups/feedback-workbench-scroll-comparison.png`
- focused action comparison path: `D:/WenCode/Code/docs/ui-mockups/feedback-workbench-action-comparison.png`
- viewport: desktop `1487 × 1058`，mobile `390 × 844`
- state: 未通过提交；2 个修正问题、2 个提升建议；64 行长代码滚动到问题 2 的第 9 行

## Findings

最终对比没有剩余 P0、P1 或 P2 问题。

- 信息架构：实现保持方案三的左侧状态与问题导航、中间代码证据、右侧聚焦诊断三栏结构；修正工作台与成长仪表盘仍使用现有页签模式。
- 颜色联动：问题 1 使用橙色，问题 2 使用青色；颜色和编号同时出现在问题卡片、代码行、行号锚点、内联标签与右侧诊断中，不依赖颜色作为唯一识别方式。
- 代码定位：模型返回的 `evidenceSnippets` 或 `code:line:n` 会映射为真实代码行；范围会按源代码总行数收敛，缺少可靠证据时不会伪造定位。
- 代码滚动：桌面代码证据区保持在 500–620px 的可读窗口内，长代码使用独立纵向滚动；滚动不会带动左右问题导航和诊断一起离开视口。
- 信息密度：当前问题的原因、修改建议、验证方式、证据定位与知识路径集中在右侧；左侧只承担状态和切换，避免重复展示同一段诊断。
- 后续动作：只读代码不再出现“运行测试”或“运行并验证”；唯一下一步是右侧“返回代码修改”，避免重复执行未修改的提交。
- 响应式：桌面端三栏无横向溢出；移动端按“问题清单 → 代码 → 诊断”纵向排列，代码区域保留独立横向阅读能力。
- 主题和多语言：亮色、暗色、中英文界面均通过浏览器场景验证；动态诊断内容保留后端原文属于预期行为。
- 可访问性：问题卡、代码锚点和内联标签均为按钮；当前问题使用 `aria-pressed`，右侧诊断使用 `aria-live`，复选步骤可通过键盘操作。

## Full-view comparison evidence

组合图左侧为概念图，右侧为同尺寸浏览器实现。三栏比例、代码区主视觉、问题颜色映射和失败用例保持一致。根据用户最新反馈，实现将工作台总高度压缩到最多 760px，并让代码区独立滚动；概念图中的运行操作被右侧“返回代码修改”取代。这两处差异是对已验证使用问题的有意修正。

focused action comparison 左侧为用户标注的旧“运行测试”入口，右侧为实现：代码头只保留语言信息，下一步集中到右侧“返回代码修改”。focused scroll comparison 左侧为撑满页面的旧代码区，右侧为固定窗口并已滚动到第 9 行的实现。

## Comparison history

### Iteration 1

- 同尺寸对比确认概念图的核心三栏关系已落地；未发现需要返工的 P0/P1/P2 视觉差异。
- 以浏览器交互断言复核：点击第二个问题后，左侧、代码区、右侧均切换为青色；点击代码内联标签可反向切换聚焦问题；返回代码后页面滚动状态恢复。

### Iteration 2

- [P2] 长代码把工作台撑到 1680px，左右上下文难以同时保持在视野内；修复为桌面最大 760px 的工作台和独立代码滚动区。
- [P2] 只读代码上的“运行测试 / 运行并验证”会原样重跑旧提交；移除两个入口，只保留“返回代码修改”。
- [P2] 平板宽度下页签底部与内容有 12px 视觉重叠；收紧平板页头底部留白后通过回归。
- post-fix evidence: 浏览器实测代码区 `scrollHeight > clientHeight` 且 `scrollTop` 可独立变化；运行按钮数量为 0，返回修改按钮数量为 1。

## Primary interactions tested

- 左侧问题卡切换问题 1 / 问题 2。
- 中间代码锚点和内联标签反向聚焦右侧诊断。
- 代码证据独立纵向滚动、返回代码修改。
- 亮色 / 暗色切换。
- 中文 / 英文切换。
- 桌面、平板与手机视口。
- 关闭弹窗前后的页面滚动锁定与恢复。
- 浏览器回归：1087 项检查通过。

## Verification

- `npm.cmd run test:feedback-workbench`：2/2 通过。
- `npm.cmd run typecheck`：通过。
- `npm.cmd run build`：通过，仅保留既有的大 chunk 提示。
- `npm.cmd run smoke:visual`：28 项通过。
- `npm.cmd run smoke:browser`：1087 项通过。
- `npm.cmd audit --audit-level=high`：镜像源不实现 npm audit API，无法取得审计结果；未发现新增依赖。

final result: passed

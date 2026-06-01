## 1. 数据读取

- [x] 1.1 在教师页引入 `RecommendationEffectiveness` 类型并添加推荐效果状态、loading 和 error。
- [x] 1.2 在 `loadAll` 中读取 `api.recommendationEffectiveness()`，失败时只更新推荐效果错误态。

## 2. 推荐效果展示

- [x] 2.1 新增推荐效果摘要区域，展示曝光、点击、后续提交、后续通过、同类错因未解和教师介入建议。
- [x] 2.2 展示 feedback signals 的 summary、severity、evidence count 和 recommended action。
- [x] 2.3 展示推荐策略或焦点标签片段，包含点击、后续提交、通过和同类错因未解计数。
- [x] 2.4 补充推荐效果区域样式、移动端约束和暗色主题兼容。

## 3. 验证

- [x] 3.1 运行 OpenSpec 严格校验。
- [x] 3.2 运行前端 typecheck。
- [x] 3.3 使用浏览器 mock 数据检查教师端 AI 质量区推荐效果渲染、移动端布局和运行时错误。
- [x] 3.4 运行 diff 检查。

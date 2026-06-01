## 1. 数据读取

- [x] 1.1 在教师页引入 `AiQualityTrend` 类型并添加趋势状态、loading 和 error。
- [x] 1.2 在 `loadAll` 中读取 `api.aiQualityTrend()`，失败时只更新趋势错误态。

## 2. 趋势展示

- [x] 2.1 新增趋势摘要区域，展示跨作业样本、诊断 eval、课堂介入、still stuck 和 waiting followup。
- [x] 2.2 展示最近 assignment points 的介入候选、still stuck、waiting followup 和质量计数。
- [x] 2.3 展示 source segment 摘要，包含版本/状态、样本、校正、低置信和高泄题风险。
- [x] 2.4 补充趋势区域样式、移动端约束和暗色主题兼容。

## 3. 验证

- [x] 3.1 运行 OpenSpec 严格校验。
- [x] 3.2 运行前端 typecheck。
- [x] 3.3 使用浏览器或静态检查确认教师端 AI 质量区布局可读、无明显重叠。
- [x] 3.4 运行 diff 检查。

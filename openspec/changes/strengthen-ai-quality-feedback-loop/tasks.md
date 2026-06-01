## 1. OpenSpec

- [x] 1.1 写入 proposal、design、spec 和 tasks，明确质量反馈闭环的范围与验收标准。
- [x] 1.2 运行 OpenSpec 校验，确保新能力与修改能力格式有效。

## 2. 后端质量结构

- [x] 2.1 扩展 AI 质量 DTO，新增质量维度、改进优先级和评测就绪度字段。
- [x] 2.2 扩展质量指标计算，纳入模型运行、泄题风险、低置信、教师纠错、eval candidate 和学习动作证据。
- [x] 2.3 在 AI 质量概览服务中生成结构化维度、优先级、评测就绪度和兼容摘要。

## 3. 测试与验证

- [x] 3.1 补充 AI 质量概览测试，断言新增结构化字段和优先级排序。
- [x] 3.2 补充学习动作证据影响质量反馈的测试。
- [x] 3.3 运行后端 AI 相关测试和 OpenSpec strict validate。
- [x] 3.4 在教师工作台消费 AI 质量概览，展示维度状态、改进优先级和 eval readiness。
- [x] 3.5 运行前端类型检查和 OpenSpec strict validate，确认教师端消费链路可用。
- [x] 3.6 在教师工作台消费诊断 eval 候选样本，展示候选数量、错因修正、教师备注和代码预览。
- [x] 3.7 运行前端类型检查、相关后端测试、OpenSpec strict validate 和浏览器验证，确认 eval 候选队列可用。
- [x] 3.8 新增诊断 eval fixture draft 导出接口，按教师校正 fixture schema 生成可审查草稿。
- [x] 3.9 在教师工作台提供 fixture draft 预览入口，让候选样本可继续沉淀为回归评测资产。
- [x] 3.10 运行后端测试、前端类型检查、OpenSpec strict validate 和接口/浏览器验证。

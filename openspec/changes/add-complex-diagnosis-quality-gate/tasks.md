## 1. 计划与规格

- [x] 1.1 创建 `complex-diagnosis-quality-gate` OpenSpec 计划。
- [x] 1.2 写明复杂质量指标、报告聚合和 baseline regression 要求。

## 2. 评分器实现

- [x] 2.1 新增复杂诊断质量评分器，输出 6 个指标、失败原因和分数。
- [x] 2.2 将评分器接入 `ModelDiagnosisEvalTest` 的 live eval entry 构造。
- [x] 2.3 扩展 `LiveModelEvalReport` 的 entry 和 summary 聚合字段。

## 3. baseline 与回归

- [x] 3.1 扩展 quality baseline draft 生成逻辑，保留复杂质量信号。
- [x] 3.2 扩展 baseline regression gate，检查复杂质量信号不回退。

## 4. 测试与验证

- [x] 4.1 增加评分器单测，覆盖全通过和典型失败。
- [x] 4.2 增加 report summary / baseline regression 相关测试。
- [x] 4.3 运行相关后端测试。
- [x] 4.4 运行 OpenSpec strict validate、secret scan 和 `git diff --check`。

## 1. 诊断命中门禁

- [x] 1.1 调整正式 prompt，明确学生文案自然、命中元数据必须绑定 ID
- [x] 1.2 强化 `diagnosisDecision` 校验：`HIT` 必须有合法 ID，未知 ID 不再静默转库外

## 2. 标准库成长入口

- [x] 2.1 限制成长候选只来自 `PARTIAL / MISS / OUT_OF_LIBRARY`
- [x] 2.2 补充单元测试覆盖 `HIT` 不入成长池、`MISS` 可入成长池

## 3. 验证

- [x] 3.1 运行相关后端测试
- [x] 3.2 运行 `openspec validate harden-diagnosis-library-fit-gates --strict`

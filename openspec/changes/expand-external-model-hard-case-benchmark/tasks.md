## 1. OpenSpec 与边界

- [x] 1.1 运行 `openspec validate expand-external-model-hard-case-benchmark --strict`，确认新 proposal/design/spec/tasks 可用。
- [x] 1.2 确认本轮只升级复杂诊断评测库和相关测试，不修改默认 prompt、不跑强依赖额度的 live eval、不写入密钥。

## 2. 复杂 fixture 生成器升级

- [x] 2.1 扩展 `generate_complex_student_submission_cases.py`，将默认输出数量提高到至少 200 条，live candidate 提高到至少 36 条。
- [x] 2.2 提升生成代码体量：所有 buggy 程序至少 50 行，至少 80 条样本达到 100 行以上。
- [x] 2.3 保持每条样本有至少 3 个注入错误、2 个次要问题、2 个干扰信号、主错因 evidenceRef、first failed case evidenceRef 和防泄题禁区。
- [x] 2.4 重新生成 `complex-student-submission-cases.json`，确保生成器输出可复现。

## 3. 质量测试与评测入口升级

- [x] 3.1 更新 `ComplexStudentSubmissionEvalFixtureTest`，断言 200+ 数量、50/100 行门槛、36+ live candidate、14+ bug pattern、14+ primary fine tag 和 28+ 语义来源。
- [x] 3.2 更新 `ModelDiagnosisEvalTest` 中复杂 live candidate 数量或代表集相关断言，使其与扩容后的 benchmark 兼容。
- [x] 3.3 如 loader 或 scorer 需要读取新增质量字段，补充结构测试，确保 gold rubric 字段完整。

## 4. 验证

- [x] 4.1 运行复杂 fixture 生成器一致性测试。
- [x] 4.2 运行相关后端 Maven 测试。
- [x] 4.3 运行 `openspec validate expand-external-model-hard-case-benchmark --strict`。
- [x] 4.4 运行 `git diff --check`。
- [x] 4.5 运行精确 secret scan，确认没有 ModelScope token、Authorization 或 Bearer 泄露。

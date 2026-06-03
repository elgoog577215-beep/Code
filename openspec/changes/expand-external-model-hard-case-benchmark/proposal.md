## Why

当前外接模型链路已经具备 prompt 版本、标准库协议、原生 trace 和 live comparison，但评测基准仍主要是 100 条自动生成复杂样本加少量 live 代表集。下一阶段要先扩大并提升复杂诊断 benchmark 的权威性和难度，让后续 prompt/标准库优化围绕更硬的教育诊断样本，而不是继续局部打磨报告字段。

## What Changes

- 将复杂学生提交评测库扩展到至少 200 条确定性样本。
- 提升样本代码规模与复杂度：每条学生程序至少 50 行，其中至少 80 条达到 100 行以上。
- 保持每条样本都有可执行验证、正确解验证、主错因、干扰信号、证据引用、教师期望、下一步学习动作和安全禁区。
- 扩展生成器和测试门槛，要求 bug pattern、主错因标签和 live candidate 覆盖更广。
- 不在本轮修改默认 prompt，不把本地 fallback 或 report 归因优化当作模型能力提升。

## Capabilities

### New Capabilities

- `external-model-hard-case-benchmark`: 覆盖外接模型复杂教育诊断 hard-case benchmark 的数量、质量、gold rubric 和可复现生成要求。

### Modified Capabilities

无。

## Impact

- 影响 `online-judge/src/test/resources/diagnosis-eval-fixtures/generate_complex_student_submission_cases.py` 和生成的 `complex-student-submission-cases.json`。
- 影响复杂 fixture loader、fixture 质量测试、live eval case 选择或相关无模型结构测试。
- 不影响学生端 API 契约，不新增数据库迁移，不写入或提交任何外接模型密钥。

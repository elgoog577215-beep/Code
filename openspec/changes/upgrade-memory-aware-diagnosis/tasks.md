## 1. 证据策略与结构

- [x] 1.1 新增记忆证据校准结构，挂到现有 `ModelDiagnosisBrief`，保持旧字段兼容。
- [x] 1.2 新增轻量 `MemoryEvidencePolicy`，只判断记忆与当前候选证据关系，不重新推断错因。
- [x] 1.3 在 `ModelDiagnosisBriefBuilder` 中生成记忆校准摘要，并保留当前证据引用和记忆引用。

## 2. 外部模型输出校验

- [x] 2.1 扩展 `ModelOutputValidator`，识别 `memory:*` 证据与当前提交证据。
- [x] 2.2 拦截主错因只由记忆证据支撑的模型输出。
- [x] 2.3 将记忆冲突或过度依赖风险写入校验失败信息或 agent trace。

## 3. Prompt 与标准库

- [x] 3.1 更新 `StandardLibraryPackBuilder` 的决策协议，增加记忆校准摘要使用规则。
- [x] 3.2 更新 `PromptTemplateRegistry`，要求外部模型区分当前证据、记忆证据和教学调节用途。
- [x] 3.3 保持单次诊断和低预算 single-call 两条外部模型路径一致。

## 4. Agent 编排与可观测性

- [x] 4.1 在 `DiagnosticAgentService` trace 中记录记忆校准状态。
- [x] 4.2 确保历史干预无效或记忆冲突时，只影响教学粒度和教师关注，不覆盖当前诊断。

## 5. 测试与验证

- [x] 5.1 增加 brief 构建测试，覆盖记忆一致、记忆冲突和 teaching-only。
- [x] 5.2 增加 validator 测试，覆盖只引用 `memory:*` 的主诊断被拒绝。
- [x] 5.3 增加 agent 编排测试，覆盖 trace 记录记忆校准状态。
- [x] 5.4 运行 OpenSpec strict validate 和相关后端定向测试。

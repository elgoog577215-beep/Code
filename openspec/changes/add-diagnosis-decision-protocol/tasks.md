## 1. OpenSpec 文档

- [x] 1.1 写入 proposal、design、spec 和 tasks，沉淀排查结论与方案。

## 2. 标准库协议

- [x] 2.1 在 `StandardLibraryPack` 中新增 `DecisionProtocol`。
- [x] 2.2 在 `StandardLibraryPackBuilder` 中构建全局规则、证据优先级、标签选择、冲突处理和教学动作规则。
- [x] 2.3 保持标准库字段向后兼容。

## 3. Prompt 升级

- [x] 3.1 升级 diagnosis judge prompt，要求遵循 `standardLibrary.decisionProtocol`。
- [x] 3.2 升级 single-call prompt，要求遵循 `standardLibrary.decisionProtocol`。
- [x] 3.3 保持 teaching hint prompt 聚焦表达阶段，不混入重复裁决逻辑。

## 4. 测试与验证

- [x] 4.1 补充标准库协议测试。
- [x] 4.2 补充 prompt contract 测试。
- [x] 4.3 运行后端 AI targeted tests。
- [x] 4.4 运行节制真实外部模型 smoke eval。

## 5. 收束

- [x] 5.1 运行 OpenSpec strict validate。
- [x] 5.2 定向提交本轮改动。

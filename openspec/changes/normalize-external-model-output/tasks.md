## 1. OpenSpec 文档

- [x] 1.1 写入 proposal、design、spec 和 tasks，明确输出标准化范围。

## 2. 标准化组件

- [x] 2.1 新增 `ExternalModelOutputNormalizer`。
- [x] 2.2 支持诊断标签、细粒度标签、证据引用和教学动作标准化。
- [x] 2.3 保证未知标签、未知证据和安全风险不被绕过。

## 3. Runtime 接入

- [x] 3.1 在 staged 诊断阶段 parse 后、validate 前接入标准化。
- [x] 3.2 在 staged 教学阶段 parse 后、validate 前接入标准化。
- [x] 3.3 在 single-call combined output parse 后、validate 前接入标准化。

## 4. 测试与评测

- [x] 4.1 补充 normalizer 单元测试。
- [x] 4.2 补充 external runtime 集成测试，覆盖中文标签和大小写证据引用。
- [x] 4.3 运行后端 AI targeted tests。
- [x] 4.4 运行节制真实外部模型 smoke eval。

## 5. 收束

- [x] 5.1 运行 OpenSpec strict validate。
- [x] 5.2 定向提交本轮改动。

## 1. OpenSpec

- [x] 1.1 编写 proposal、design、tasks 和 spec delta。
- [x] 1.2 运行 `openspec validate add-coach-safety-eval-fixtures --strict`。

## 2. Fixture 资源与 loader

- [x] 2.1 新增 `coach-safety-rejection-cases.json`，覆盖完整答案、隐藏测试、英文直接修复和证据缺失风险。
- [x] 2.2 扩展 `CoachEvalFixtureLoader`，支持加载安全拒绝 fixture。
- [x] 2.3 为拒绝 fixture 提供统一的 unsafe model response JSON 序列化和 domain 转换。

## 3. 安全门与回归测试

- [x] 3.1 扩展 `CoachAgentServiceTest`，校验安全拒绝 fixture 结构完整。
- [x] 3.2 扩展 `CoachAgentServiceTest`，批量验证 unsafe fixture 会触发 `SAFETY_REJECTED` 和规则 fallback。
- [x] 3.3 补强 `CoachAgentService` 本地泄题短语识别，覆盖英文直接修复、完整答案和隐藏测试表达。
- [x] 3.4 扩展现有拒绝测试，断言模型风险元数据被保留或提升。

## 4. 验证

- [x] 4.1 运行 `openspec validate add-coach-safety-eval-fixtures --strict`。
- [x] 4.2 运行 `CoachAgentServiceTest`。
- [x] 4.3 运行后端编译和 `git diff --check`。

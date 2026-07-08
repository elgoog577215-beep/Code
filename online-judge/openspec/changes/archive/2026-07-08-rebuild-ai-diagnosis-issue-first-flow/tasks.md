## 1. Spec 门禁

- [x] 1.1 写出 `docs/ai-diagnosis-issue-first-spec.md`，复盘已抓到的真实请求/响应 trace，明确旧链路在哪一层失败。
- [x] 1.2 在 Spec 文档中定义 `issues[]`、逐层挂接 action、anchor 和 advice 输入的目标 schema。
- [x] 1.3 在 Spec 文档中画出后端控制逐层挂接状态机，包含 breadcrumb、当前层、终止条件和最大轮次。
- [x] 1.4 在 Spec 文档中列出降级矩阵，覆盖空标准库、空选择、非法 code、timeout、output truncation、NO_MATCH 和部分匹配。
- [x] 1.5 在 Spec 文档中列出旧链路删除/绕开清单和最少 3 个自动化测试断言；未完成这些产物不得进入主实现。

## 2. 自由诊断 issue-first 契约

- [x] 2.1 新增或调整自由诊断 DTO，使模型输出多个带 `issueId`、证据和置信度的 issue。
- [x] 2.2 更新自由诊断 prompt，只要求模型诊断当前提交并输出 issues，不要求标准库 ID。
- [x] 2.3 增加 issue 校验和局部降级：单个 issue 证据无效时不清空其他 issue。
- [x] 2.4 增加分层优惠最短路 fixture 测试，断言自由诊断能保留多个错误。

## 3. 后端控制标准库逐层挂接

- [x] 3.1 新增 layered attachment 服务或收束现有导航方法，由后端维护 issue 粒度的 current layer、breadcrumb 和 round。
- [x] 3.2 新增最小 action prompt，AI 只返回 `SELECT`、`DONE` 或 `NO_MATCH` 以及当前层 code。
- [x] 3.3 实现根目录为空时不调用 AI，直接标记 `LIBRARY_EMPTY` 并继续 advice generation。
- [x] 3.4 实现非法 code、空选择、轮次耗尽和 timeout 的 issue 级降级，不再返回整链 `MODEL_FAILED`。
- [x] 3.5 增加多 issue 独立挂接测试，断言一个 issue 失败不影响其他 issue。

## 4. advice generation 主链路改造

- [x] 4.1 将 advice generation 输入改为 `issues[] + optional anchors + evidence package`。
- [x] 4.2 更新 advice prompt，要求按多个 issue 输出多条基础建议和提高建议。
- [x] 4.3 调整 mapper、normalizer 和 validator，保留证据有效的多条建议，不因标准库 anchor 缺失清空建议。
- [x] 4.4 增加空标准库但 issues 有效时仍生成学生建议的回归测试。

## 5. Trace 与真实样本报告

- [x] 5.1 增加阶段化 trace 落盘，记录自由诊断、逐层挂接和 advice generation 的请求摘要、响应、后端判定和降级原因。
- [x] 5.2 更新 `DiagnosisReportV2RealSamplesSimulationTest` 报告，展示 trace 路径、阶段状态和多条建议。
- [x] 5.3 用第一道高级长代码样本跑一次 trace，确认报告能区分自由诊断成功、标准库挂接降级和 advice 结果。（2026-07-08 使用 `Qwen/Qwen3-Coder-30B-A3B-Instruct` 重跑五题 live，报告落盘到 `target/ai-simulation-reports/real-samples-website-vs-codex-20260708-231907.md`；5/5 `MODEL_COMPLETED`，每题均有多条基础建议和提高建议。）

## 6. 清理旧主路径

- [x] 6.1 将旧 `CONTINUE/selectedBranches/selectedPaths` 导航协议从默认主链路移除或降为兼容测试路径。
- [x] 6.2 删除导航阶段 evidenceRefs 硬要求，证据校验只保留在自由诊断和 advice 阶段。
- [x] 6.3 扫描并清理会把标准库导航失败映射为整链 `MODEL_FAILED` 的默认路径。

## 7. 验证与收束

- [x] 7.1 运行 OpenSpec 校验。
- [x] 7.2 运行 AI 诊断、标准库挂接、建议映射和真实样本报告相关测试。
- [x] 7.3 运行 `git diff --check`。
- [x] 7.4 更新 `docs/ai-memory/项目决策.md` 或 `错误经验.md`，记录 issue-first 和标准库挂接边界。
- [x] 7.5 提交并推送当前分支。

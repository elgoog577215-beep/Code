## Context

最近对 200 行以上高级样本的真实模型 trace 显示：

- 自由诊断阶段可以成功识别多个错误，例如分层最短路样本中同时识别优惠券未折半、全局剪枝忽略层数、最终答案只取恰好 k 层。
- 随后的标准库导航阶段在 H2 测试库中收到 `roots=[]` 和 `visibleKnowledgeNodeCodes=[]`，模型合理返回“无节点可选”，但后端将 `CONTINUE + selectedBranches=[]` 判为整链 `MODEL_FAILED`。
- 旧链路已经经历多轮补丁：增加导航轮数、修 evidenceRef 别名、修报告只展示首项、替换长代码样本；失败仍在不同前置阶段出现，说明当前架构把标准库协议放错了位置。

标准库的产品定位是教学参考规范包：提供章节、知识点、能力点、易错点和提升点的标准语言，不替代 AI 对当前提交的真实诊断，也不能成为学生建议生成的闸门。

## Goals / Non-Goals

**Goals:**

- 建立 issue-first 主链路：先诊断多个真实 issue，再做标准库挂接。
- 让后端控制逐层目录浏览：当前层目录、breadcrumb、轮次、可选 code 和扩展动作都由后端维护。
- 让 AI 在标准库阶段只做语义选择：选择当前层 code、结束或标记无匹配。
- 标准库为空、挂接失败、模型漏选、超时或输出异常时，保留 issue 并继续 advice generation。
- 学生端保留证据有效的多条基础建议和提高建议。
- 每次 live eval 能落盘请求/响应 trace，后续可直接复盘失败全过程。

**Non-Goals:**

- 不新增正式标准库内容，不用 seed 修测试空库。
- 不重做教师端标准库治理台。
- 不引入新的向量库、RAG 框架或外部依赖。
- 不恢复本地规则诊断作为 AI 内容兜底。
- 不要求第一版解决所有模型超时，只要求超时不清空已有 issue。

## Decisions

### 1. Spec 先行，且必须有硬产物

实现前先完成 `docs/ai-diagnosis-issue-first-spec.md`，内容必须包含：

- 旧链路真实 trace 复盘：至少覆盖自由诊断成功、导航空目录失败和 advice 未进入。
- 目标请求/响应 schema：`FreeDiagnosisIssueOutput`、`LayeredAttachmentAction`、`IssueLibraryAnchor`、`AdviceGenerationInput`。
- 后端状态机：每个 issue 的目录层级、breadcrumb、最大轮次、终止条件。
- 降级矩阵：空标准库、空选择、非法 code、timeout、output truncation、无匹配、部分匹配。
- 删除/绕开清单：旧 `selectedBranches/selectedPaths` 主路径、导航 evidenceRefs 硬要求、导航失败整链关闭。
- 自动化测试清单：至少 3 个会失败于旧链路、通过于新链路的测试。

没有这个 Spec 产物，不进入主实现。

### 2. 自由诊断输出变成 issues，而不是单次最终报告草稿

第一阶段模型只看题目、代码、判题事实和 evidenceRefs，输出：

```text
issues[]:
  issueId
  title
  explanation
  evidenceRefs[]
  confidence
  severity
```

理由：学生代码可能有多个错误，标准库挂接也必须按 issue 粒度进行。继续整题只选一个路径会天然压扁多条建议。

替代方案：继续让最终诊断同时生成诊断、标准库路径和建议。放弃原因是协议太重，长代码下容易漏字段、超时或被单条主因吞掉。

### 3. 标准库挂接由后端控制逐层浏览

后端为每个 issue 创建导航状态：

```text
issueId
breadcrumb
currentNodes
currentDiagnosticLayer
round
status
```

每轮只给 AI：

```text
issue 摘要
breadcrumb
当前层可选章节/知识点/诊断层内容
允许动作：SELECT / DONE / NO_MATCH
```

AI 只返回：

```text
action
codes[]
reason
confidence
```

后端负责校验 code、展开下一层、终止和降级。

替代方案：保留旧 `CONTINUE/DONE/NO_MATCH + selectedBranches + selectedPaths + evidenceRefs`。放弃原因是模型承担状态机和证据协议，正是当前失败根源。

### 4. 标准库挂接是可选辅助，不是 advice 闸门

advice generation 输入永远包含 `issues[]`。标准库 anchors 只作为可选字段：

```text
issues[] + anchors[]? + evidence package -> advice
```

当标准库不可用或挂接失败时：

```text
anchorStatus = LIBRARY_EMPTY / ATTACHMENT_FAILED / NO_MATCH
```

系统继续生成建议，只在 trace 和教师治理数据里记录挂接失败。

### 5. Trace 成为回归门禁

真实样本仿真和 live eval 必须落盘阶段化 trace：

- 阶段名。
- 请求摘要或完整请求体的安全版本。
- 模型响应。
- 后端解析结果。
- 降级原因。
- advice 数量。

理由：这次能定位根因，正是因为抓到了真实请求/响应。后续不能再依赖猜测。

## Risks / Trade-offs

- [Risk] 改动面触达 AI 主链路，容易一次改太多。Mitigation：先用 Spec 固定 schema 和测试，再按最小主路径替换。
- [Risk] issue-first 可能增加模型输出长度。Mitigation：自由诊断只输出 issue，不生成学生建议；建议阶段再生成学生文案。
- [Risk] 标准库为空时学生建议没有标准化路径。Mitigation：保留真实 issue 和证据，anchorStatus 明确标记 `LIBRARY_EMPTY`。
- [Risk] 新旧 telemetry 字段兼容复杂。Mitigation：保留旧字段读写兼容，但新运行状态使用新 anchor/advice 状态。

## Migration Plan

1. 完成 Spec 产物和失败复盘。
2. 新增 issue-first DTO、prompt 和解析/校验。
3. 新增后端控制的 layered attachment 服务，先支持知识树根、子节点和诊断层。
4. 改造 advice 输入，使其基于 issues 生成，多条建议不依赖标准库成功。
5. 将旧导航协议从默认主路径移除或降为兼容测试路径。
6. 更新真实样本仿真报告和 trace 输出。
7. 运行 OpenSpec、后端测试和单题 trace 验证。

Rollback：保留旧代码到单独兼容方法或历史提交；若新链路失败，可通过回退提交恢复，不提供运行时开关让旧协议继续作为默认主路径。

## Open Questions

- 第一版按每个 issue 最多选择几个目录分支：默认 1，最多 2，超过则记录为多候选但不展开。
- 是否在第一版生成标准库成长候选：保留已有成长流程入口，但不作为主验收。

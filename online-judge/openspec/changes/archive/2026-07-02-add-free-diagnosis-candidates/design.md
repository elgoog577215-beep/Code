# 设计

## 核心思路

保持实时链路简单：

```text
题目、代码、判题结果、证据
-> 本地召回树形标准库包
-> 单诊断 Agent 一次调用
-> 自由诊断候选
-> 标准库映射
-> 学生报告
-> 库外成长候选
```

本次只增加一个隐藏的结构字段：`diagnosisCandidates`。它不是学生报告，也不是完整思维链，而是可审计的诊断草稿摘要。

## 输出分层

### 1. `diagnosisCandidates`

模型先列出它自由判断出的真实问题候选。每个候选包含：

- `name`：问题名称。
- `layer`：`BASIC` 或 `IMPROVEMENT`。
- `libraryFit`：`HIT`、`PARTIAL`、`MISS`、`OUT_OF_LIBRARY`。
- `anchorId`：命中标准库时填写合法 ID；库外发现必须为 `null`。
- `anchorType`：标准库类型或 `OUT_OF_LIBRARY`。
- `role`：`PRIMARY`、`SECONDARY`、`CONTEXT`。
- `evidenceRefs`：必须来自输入证据。
- `reason`：简短说明。
- `confidence`：0 到 1。

### 2. `diagnosisDecision`

这是最终机器决策，继续复用现有结构。它必须从 `diagnosisCandidates` 中收束出主要锚点，不允许凭空生成另一套判断。

### 3. `studentReport`

继续作为学生端主输出。它写自然中文，不展示标准库评审过程。

### 4. `libraryGrowth`

只在 `PARTIAL`、`MISS` 或 `OUT_OF_LIBRARY` 时填写。它来自候选层里的库外发现，但仍只进入候选池，不能实时进入正式库。

## 后端校验

校验器新增对 `diagnosisCandidates` 的轻量硬门禁：

- 非库外候选如果填写 `anchorId`，必须存在于当前标准库包。
- `OUT_OF_LIBRARY` 候选的 `anchorId` 必须为空。
- 证据引用必须合法。
- 置信度必须在 0 到 1。
- `HIT` 的最终 `diagnosisDecision` 仍必须包含合法标准库 ID。
- `MISS` 的最终 `diagnosisDecision` 仍不得包含已知标准库 ID。

## 取舍

不新增复杂候选实体、不写数据库、不改前端。候选层先作为模型输出契约和后台校验字段存在，足够支撑下一轮评测和标准库成长。

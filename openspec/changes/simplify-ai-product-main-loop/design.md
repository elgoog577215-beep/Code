## Context

当前项目已经完成多轮 AI 能力扩张：单次诊断、轨迹分析、外接模型 runtime、复杂评测样本、live eval、baseline regression、教师质量概览和学生双通道反馈。能力本身有价值，但产品主线需要重新收束。

本轮选择“分层下沉”，而不是删除能力：

```text
学生主链路：学生看得懂、能行动、不泄题
教师洞察层：老师看班级共性、重复卡点、介入优先级
研发评测层：研发看模型完成率、fallback、baseline、runtime telemetry
```

## Goals / Non-Goals

**Goals:**

- 让 `studentFeedback` 成为学生端主展示契约。
- 明确后端 AI 主链路的 6 个步骤，减少后续分支扩散。
- 把 OpenSpec change 按产品层、教师层、评测层、runtime 层建立索引。
- 用测试保证 `StudentFeedbackAssembler` 对失败、隐藏失败、AC、低证据场景都有可用输出。

**Non-Goals:**

- 不删除现有 live eval、baseline、runtime telemetry。
- 不强行归档历史 OpenSpec change。
- 不新增学生端 schema。
- 不改数据库结构。
- 不默认跑完整 14 条 live eval。

## Decisions

- 学生端第一视觉层只读取 `studentFeedback.summary`、`blockingIssues`、`improvementOpportunities`、`nextLearningAction`。
- `issueTags`、`fineGrainedTags`、`aiInvocation`、`promptVersion`、`provider`、`latency`、`fallback`、`baseline` 只允许在折叠详情、教师端或研发报告中出现。
- `StudentFeedbackAssembler` 是本地学生反馈统一入口；未来新增本地反馈规则优先进入这里。
- 外接模型完整通过时可以提供 `studentFeedback`；模型 partial/fallback 时仍返回学生可用反馈，但不计入外接模型能力分。
- 产品目标文档只保留 4 个核心反馈质量指标：主错因命中、证据扎根、下一步可执行、不泄题。

## Main Loop

```text
学生提交
-> 本地判题结果
-> DiagnosisEvidencePackage 证据摘要
-> RuleSignalAnalyzer 本地候选信号
-> 外接模型尝试生成结构化诊断与 studentFeedback
-> ModelOutputValidator 安全与结构校验
-> StudentFeedbackAssembler 生成最终学生双通道反馈
```

## Layer Boundary

- 学生主链路：只服务“我错在哪里、下一步怎么做、通过后怎么提升”。
- 教师洞察层：只服务“谁需要帮助、班级共性是什么、模型是否可靠”。
- 研发评测层：只服务“模型和链路是否在变好、fallback 是否诚实、baseline 是否回退”。

## Risks / Trade-offs

- [Risk] 复杂能力被误删 -> 本轮只下沉和索引，不删除。
- [Risk] 文档无法约束后续开发 -> 用契约测试和前端展示规则固定关键边界。
- [Risk] 学生端旧数据没有 `studentFeedback` -> 继续保留旧字段兜底。

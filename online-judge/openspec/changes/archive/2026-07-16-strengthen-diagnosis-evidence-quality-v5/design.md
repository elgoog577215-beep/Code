## Context

生产 V4 发布后，知识树与标准库结构门禁全部通过，但现有诊断证据仍不能支撑效果判断：55 条事实中 33 条标记为 `PROVISIONAL`，其原始分析 JSON 均保存稳定 `provisionalNodeCode`，事实投影器却没有写入该字段；因此 53 条事实只能使用文本指纹，所有事实的 `library_fit` 都是 `UNKNOWN`。33 份分析中有 21 份包含建议，其中 7 份尚未投影事实，另有 12 份本来没有建议。

临时候选表已保存 38 个现有建议 code，并且 38/38 均能匹配有效父知识点；55 条现有事实也能按确定性的 `fact_key` 与分析 JSON 一一对应。这使无模型、无猜测的历史修复成为可能。

## Goals / Non-Goals

**Goals:**

- 保存诊断实际使用的正式或临时候选稳定身份，使标准库消费可以按 code 聚合。
- 用确定性历史数据补齐临时候选 code、library fit 和缺失事实投影。
- 保持问题生命周期身份、反馈版本和事件关联幂等，并提供发布级质量门禁。
- 明确区分“内容可用性”“事实投影完整性”“正式命中效果”三种口径。

**Non-Goals:**

- 不把临时候选自动晋升为正式标准库条目。
- 不重新调用模型，不为没有建议的历史分析生成事实。
- 不把 V4 发布前诊断当成 V4 效果样本，也不承诺本轮提高正式命中率。
- 不新增一套与现有知识树或标准库平行的证据分类结构。

## Decisions

### 1. 事实表直接保存 `provisional_node_code`

V5 在 `submission_diagnosis_facts` 增加可空 `provisional_node_code` 和索引。正式事实继续通过 `mistake_point_id`、`improvement_point_id`、`skill_unit_id` 表达正式身份；临时候选事实必须保存 `provisional_node_code`；未分类事实不保存任何库内 code。

不额外复制 `parent_knowledge_node_code` 或完整 code 路径。临时候选父节点已由 `ai_standard_library_growth_candidates` 以候选 code 唯一追溯，正式项可由规范表追溯主知识点；复制会产生第二份易漂移真源。

### 2. 问题身份采用正式优先、临时次之、文本最后的确定顺序

`IssuePointKeyFactory` 的身份优先级固定为：正式易错点或提升点 ID、临时候选 code、正式能力点 ID、文本指纹。临时候选使用 `provisional:point-key-v1:<code>` 和 `PROVISIONAL_ID`，不改变已有正式 ID 的 key 和版本。

`SubmissionIssueLifecycleService.normalizeFacts` 不再只修空 key，而是比较当前身份与按最新字段重新计算的身份；只有不一致时更新。随后现有回填流程按提交范围重建生命周期，避免事实身份已变而 transition 仍使用旧文本 key。

### 3. 历史修复只使用可确定来源

V5 SQL 通过分析 ID、事实类型、建议顺序和投影器既有 `fact_key` 规则，把历史事实与 `report_json` 中的 advice 一一连接，回填 33 条临时候选 code。迁移先验证所有 `PROVISIONAL` 事实均能获得 code、code 均存在于成长候选且父知识点有效；任一条件不满足则事务失败。

缺失的 26 条事实不在 SQL 中复制 Java 投影逻辑，而由现有 `SubmissionEvidenceBackfillService` 在新代码启动后生成。该流程同时补齐缺失反馈版本和事件关联，已具备预览、批次记录与幂等语义；没有建议的 12 份分析保持不可投影。

### 4. 缺失 library fit 按路径状态进行保守回填

当分析 `aiInvocation.diagnosisLibraryFit` 或 `libraryFit` 有合法值时沿用；历史值为空时，事实级回填固定为 `FORMAL -> HIT`、`PROVISIONAL -> PARTIAL`、`UNCLASSIFIED -> MISS`。这些映射只描述挂接结果，不表示诊断正确率。

### 5. 发布门禁同时检查结构完整性和统计可解释性

新增脚本把以下情况视为阻断项：临时候选事实缺 code、code 找不到成长候选或父节点、正式事实缺少或错挂正式 ID、建议行与事实投影数量不一致、非法 JSON、已投影事实仍为未知 library fit、身份来源与路径状态不一致。

正式命中率、临时候选率、未分类率、无建议分析量属于质量债务和效果基线，必须报告但不硬编码为一次性阻断阈值。V4 发布后的新分析数量不足时，报告必须明确“尚不能判断效果”。

## Risks / Trade-offs

- **[历史回填扩大事实和版本表计数]** → 在生产备份副本上先运行预览和正式回填，按表区分预期增长与业务事实不变项；不把证据派生表增长误判为业务污染。
- **[候选 code 与文本生命周期身份合并后改变历史连续性]** → 只通过身份工厂重新计算并按提交范围重建 transition，不直接批量改 transition key。
- **[FORMAL 比例仍然很低]** → 本轮将其作为后续导航与候选治理的可靠基线，不伪装为已经改善的结果。
- **[生产磁盘余量较低]** → 使用现有 PostgreSQL 镜像、流式传输候选镜像、保留一个明确回滚镜像并及时删除临时容器和传输文件。

## Migration Plan

1. 在生产 V4 上固化事实、分析、反馈版本、事件关联、生命周期和核心业务表计数，生成备份与 SHA-256。
2. 在空 PostgreSQL 验证 V1→V5、重复启动、baseline 与漂移阻断。
3. 在生产备份隔离恢复库运行候选应用 V5，执行证据回填预览和正式回填，验证第二次回填为幂等。
4. 验证临时候选 code、父节点、身份来源、library fit、投影覆盖和 lifecycle；同时确认题目、测试、提交、分析正文和反馈正文计数无非预期下降。
5. 保留 V4 应用镜像和 V4 数据库备份，发布 V5 后执行相同验证；失败时停止 V5 应用、恢复 V4 镜像，并在需要时从 V4 备份恢复数据库。

## Open Questions

- 正式命中率需要积累多少条 V5 发布后的新诊断，才足以进入下一轮内容或导航效果评估？本变更先报告样本量，不预设未经验证的固定阈值。

## Why

Flyway V4 已证明知识树与标准库内容可用，但生产 55 条诊断事实中只有 2 条正式挂接，33 条临时候选事实在投影时丢失 `provisionalNodeCode`，55 条 `library_fit` 全为 `UNKNOWN`，且 21 份包含建议的分析仍有 7 份没有事实投影。继续精修第四批内容无法回答“哪些知识真正被诊断使用”，因此必须先把诊断证据账本修成可追溯、可聚合、可复核的质量真源。

## What Changes

- 新增 Flyway V5，为诊断事实保存稳定 `provisional_node_code`，从已保存分析 JSON 无损回填历史临时候选 code，并把空字符串 ID 规范为 `NULL`。
- 让事实投影器和问题身份工厂保留临时候选稳定身份；正式项继续使用正式 ID，临时候选使用 `PROVISIONAL_ID`，未分类项才使用文本指纹。
- 当历史分析没有 `library_fit` 时，按已验证的路径状态回填 `FORMAL -> HIT`、`PROVISIONAL -> PARTIAL`、`UNCLASSIFIED -> MISS`，不再把已有挂接事实记成 `UNKNOWN`。
- 复用现有幂等证据回填流程，为有建议但无事实的 7 份历史分析补投影，并重建受影响的问题生命周期；没有建议的 12 份历史分析保持不可投影，不制造诊断事实。
- 新增诊断证据质量门禁，检查分析到事实覆盖、临时候选 code、候选父知识点、正式 ID、JSON 结构、library fit 和稳定身份来源，同时把正式命中率、临时候选率、未分类率作为质量债务披露。
- 增加 PostgreSQL 迁移、投影器、身份归一化和脚本安全回归；生产发布继续执行备份、独立恢复、应用级迁移、回填预演、正式回填和业务计数验证。

## Capabilities

### New Capabilities
- `diagnosis-evidence-quality`: 定义诊断事实稳定身份、投影覆盖、历史回填、质量门禁和效果口径边界。

### Modified Capabilities
- `standard-library-provisional-growth`: 临时候选被学生诊断使用后，稳定候选 code 必须进入诊断事实账本并可追溯到真实父知识点。

## Impact

- 数据库：`submission_diagnosis_facts` 新增列、索引与状态约束；Flyway 最新版本由 V4 升至 V5。
- 后端：`SubmissionDiagnosisFact`、`SubmissionDiagnosisFactProjector`、`IssuePointKeyFactory`、`SubmissionIssueLifecycleService`。
- 运维与质量：新增诊断证据门禁，更新 Schema readiness、PostgreSQL 迁移链、备份恢复与生产回填审计。
- 数据边界：历史题目、测试、提交、分析正文和学生反馈内容不改写；只补齐可由已保存 JSON 确定的事实身份、反馈版本和事件关联。

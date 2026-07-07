# 设计

## 主结构

标准库的主结构继续保持：

- `informatics_knowledge_nodes` 表达统一信息学知识树。
- `ai_standard_skill_units` 表达知识点下的能力点。
- `ai_standard_mistake_points` 表达能力点下的易错点。
- `ai_standard_improvement_points` 表达能力点或知识点下的提升点。

本次不合并成一张巨表。原因是知识树、能力点、易错点和提升点的生命周期、字段含义和诊断用途不同；真正需要合并的是主链路和写入口，而不是物理表数量。

## 写入策略

`AiStandardLibraryService.create/update/setEnabled` 先写规范化表，再同步旧平铺表快照。

- `SKILL_UNIT` 写入或更新 `AiStandardSkillUnit`。
- `MISTAKE_POINT` 和兼容 `BASIC_CAUSE` 写入或更新 `AiStandardMistakePoint`。
- `IMPROVEMENT_POINT` 写入或更新 `AiStandardImprovementPoint`。
- 旧 `AiStandardLibraryItem` 继续保存同一 `layer/code` 的快照，服务现有教师端 ID、列表、详情和 embedding 状态。

## 成长闭环

成长候选预检使用规范化存在性判断，避免只看旧表导致重复入库或误判。教师批准候选时，继续调用标准库服务写入正式库；由于服务已经规范化主写，批准后的候选会成为正式规范条目。

相似条目继承仍使用标准库统一查询能力，优先读规范化条目，必要时回退旧快照，保证候选可以继承能力点和知识点锚点。

## 兼容边界

教师端当前仍以数字 `id` 操作标准库条目，因此本次保留旧快照 ID。后续如要完全移除旧表，需要另起变更，把教师端 API 改为 `layer/code` 或规范化目标 ID。

Embedding 当前绑定旧 `AiStandardLibraryItem`，因此同步旧快照后继续标记该快照 embedding 为 stale。规范化 embedding 目标另行设计。

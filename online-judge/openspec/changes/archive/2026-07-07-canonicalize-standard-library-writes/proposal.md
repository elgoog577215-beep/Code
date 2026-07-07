# 规范化标准库写入主链路

## Why

当前标准库已经形成“知识点 -> 能力点 -> 易错点/提升点”的规范结构，但教师新增、编辑和成长候选批准仍主要写入旧的 `ai_standard_library_items` 平铺表。这样会让“知识树和标准库已经合并”的判断停留在召回层，数据库主写入口仍然像两套系统。

## What Changes

- 将正式标准库写入主链路切到规范化表：能力点写入 `ai_standard_skill_units`，易错点写入 `ai_standard_mistake_points`，提升点写入 `ai_standard_improvement_points`。
- 保留旧平铺表作为兼容快照，用于教师端现有数字 ID、历史接口和 embedding 失效标记。
- 成长候选预检、批准和快照对比以规范化表为正式库判断来源。
- 教师批准成长候选后，新条目必须同时具备规范化正式记录、兼容快照，并继续进入后续召回。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `standard-library-normalized-schema`: 正式标准库写入必须以规范化能力点、易错点和提升点为主结构，旧平铺条目只作为兼容快照。

## Impact

- 影响 `AiStandardLibraryService` 的新增、编辑、停用和正式库存在性判断。
- 影响 `AiStandardLibraryGrowthAgentService` 的预检、批准、入库审计和相似条目继承。
- 影响标准库相关测试和 OpenSpec 归档。
- 不改变教师端 API 路径，不新增前端页面，不拆分高中库和竞赛库。

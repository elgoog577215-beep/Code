## Context

数据审视结果显示：100 条复杂样本分布在 7 个 bugPattern，每个 pattern 14-15 条；去除 variant helper 后只有 7 个语义源码族。这意味着数量达标但实际测试面偏窄，容易高估模型能力。

## Goals / Non-Goals

**Goals:**

- 至少 14 个语义模板族。
- 至少 12 个不同 `bugPattern`。
- 至少 12 个不同 primary fine-grained tags。
- 保留 100 条样本和 24 条 live 候选。
- 继续由本地 Python 执行验证正确解法与错误提交。

**Non-Goals:**

- 不引入爬取代码或外部数据集。
- 不改生产诊断逻辑。
- 不扩大到 500 条。

## Decisions

- 通过新增模板族扩展覆盖，而不是仅修改 variant helper。模板族必须改变题意、代码结构、first failed case 和 root cause。
- 为新增模板选择现有 taxonomy 中未充分覆盖的细粒度错因，包括 `OFF_BY_ONE`、`DUPLICATE_CASE`、`INITIAL_STATE`、`BRUTE_FORCE_LIMIT`、`SAMPLE_OVERFIT`、`PARTIAL_FIX_REGRESSION`、`IN_PLACE_STATE_PROGRESS` 等。
- 测试层加入语义族门槛：按 `generatorSpecId` 的模板 slug 和源码结构双重统计，防止未来回退成同质变体。

## Risks / Trade-offs

- [Risk] 新增模板增加生成器维护成本 -> 每个模板仍保持同一 `Template` 结构，避免引入多套 schema。
- [Risk] 某些复杂错误可能不稳定复现 -> 所有新增模板都必须通过生成器运行验证，不通过不写入 fixture。
- [Risk] live 候选前 24 条覆盖顺序变化 -> 让前 24 条按模板轮转，保证 live smoke 先覆盖更多错因。

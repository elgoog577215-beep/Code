# 设计

## 内容密度

新增 `AiStandardLibraryV16DensityExpansionSeeds`，采用与 V15 一致的批次结构：

- 每个主题 1 个能力点。
- 每个能力点 3 个易错点。
- 每个能力点 1 个提升点。

V16 覆盖 8 个主题：

- 链表前驱后继与插入删除。
- 区间调度与区间合并。
- 前缀和区间查询。
- 计数排序与频次统计。
- 游程编码。
- 多数投票算法。
- 素数判断。
- 拓扑依赖。

## 成长闭环

成长候选正式入库时，保留 AI 诊断候选中的结构化字段：

- `errorSymptom` 进入正式条目描述与常见误解。
- `typicalCodePattern` 进入常见代码模式。
- `studentExplanation` 进入学生解释。
- `reason` 和结构化字段共同进入教师解释。
- `similarExistingItems` 优先用于继承已有能力点和知识点锚点。

## 召回可见性

当前召回链路优先读取 normalized 标准库。教师批准成长候选后生成的正式条目写入 legacy 标准库表，如果召回只返回 normalized 内容，新增条目会无法被 AI 使用。

因此 `enabledSearchLocationItems()` 应合并 normalized 条目与非 fallback 的 legacy 正式条目，并按 `layer/code` 去重，优先保留 normalized 条目。

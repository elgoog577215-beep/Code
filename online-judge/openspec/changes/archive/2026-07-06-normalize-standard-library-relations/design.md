# 设计

## 主结构
保留现有数据库主体，做兼容增强：

```text
informatics_knowledge_nodes
  code
  path

ai_standard_library_items
  layer = SKILL_UNIT
  code
  primaryKnowledgeNodeCode
  knowledgeNodeCodes          # 兼容/相关知识集合

ai_standard_library_items
  layer = MISTAKE_POINT
  code
  skillUnitCode               # 主归属
  primaryKnowledgeNodeCode    # 从所属能力点或种子首个知识点推导
  knowledgeNodeCodes          # 兼容/相关知识集合
```

规范表 `ai_standard_skill_units`、`ai_standard_mistake_points`、`ai_standard_improvement_points` 已有 `primaryKnowledgeNodeCode`，本次只让兼容表和运行包也表达相同语义。

## 兼容策略
- `primaryKnowledgeNodeCode` 是主知识点。
- `knowledgeNodeCodes` 继续保留，用于旧接口、检索和相关知识补充。
- `relatedKnowledgeNodeCodes` 在 DTO/运行包中作为更明确的别名输出，避免把旧字段误认为主结构。
- 旧请求如果没传 `primaryKnowledgeNodeCode`，后端从 `knowledgeNodeCodes` 第一项推导。

## 运行包策略
AI 候选包中：
- 能力点输出 `primaryKnowledgeNodeCode`。
- 易错点输出 `skillUnitCode` 作为主归属。
- 知识分组优先按 `primaryKnowledgeNodeCode` 分组。
- 旧 `knowledgeNodeCodes` 继续输出，保持测试和已有 prompt 兼容。

## 最小实现范围
- 扩展实体、请求、响应和运行包 DTO。
- 更新 seeder/service/selector 的字段映射。
- 增加测试确认主关系和相关关系不混淆。

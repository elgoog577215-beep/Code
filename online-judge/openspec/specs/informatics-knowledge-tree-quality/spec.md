# informatics-knowledge-tree-quality Specification

## Purpose
TBD - created by archiving change expand-knowledge-tree-standard-library-v8. Update Purpose after archive.
## Requirements
### Requirement: 知识树扩展必须细化既有教学路径
信息学知识树扩展 SHALL 复用现有领域、章节和 topic，并在其下补充更具体的小知识点，避免创建与既有路径重复或并行的新知识树。

#### Scenario: 新增细知识点
- **WHEN** 系统同步新增知识树 seed
- **THEN** 新知识点 SHALL 拥有合法父 topic
- **AND** 新知识点 SHALL 保留完整 path、description、learningObjectives 和 typicalProblems

#### Scenario: 不重复创建上层结构
- **WHEN** 某个新增内容可以归入既有 topic
- **THEN** 系统 SHALL NOT 为它新增重复领域、章节或 topic

### Requirement: 知识树扩展必须优先补强诊断薄弱主题
知识树扩展 SHALL 优先覆盖 AI 诊断高价值且手写标准库薄弱的主题，包括模拟边界、数组更新、字符串匹配、图存储、双指针、复杂度权衡、运行错误和提交检查。

#### Scenario: 薄弱主题获得更细节点
- **WHEN** 本轮 V8 知识树 seed 被加载
- **THEN** 至少 SHALL 为模拟、数组、字符串、图结构、复杂度或提交检查中的多个主题新增细知识点
- **AND** 新节点 SHALL 能被 V8 标准库条目引用

### Requirement: 知识树内容必须避免低信息模板表达
信息学知识树 seed SHALL 使用教学对象、操作、边界或验证方式描述节点，不得把正式节点描述写成“细颗粒知识点”“相关概念、方法和常见题型”等元描述。

#### Scenario: 校验知识点描述质量
- **WHEN** 系统加载知识树 seed
- **THEN** 每个知识点描述 SHALL 表达该知识点在读题、编码、调试或复盘中的作用
- **AND** 描述 SHALL NOT 依赖“细颗粒知识点”这类占位表达

#### Scenario: 校验 topic 学习目标质量
- **WHEN** 系统加载 topic seed
- **THEN** topic 的学习目标 SHALL 说明需要掌握的对象、边界或方法
- **AND** SHALL NOT 只写“掌握某某的基本用法”

## ADDED Requirements

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

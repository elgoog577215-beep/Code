## ADDED Requirements

### Requirement: 第四批知识点必须按高中课程与信奥能力矩阵精修
Flyway V6 SHALL 在不新增节点和不修改稳定 code 的前提下，精修 48 个仍使用占位描述的启用叶子知识点，并 SHALL 在 BASIC、MATH、ENG、CONTEST 四个领域各精修 12 个。

#### Scenario: 保存第四批知识点
- **WHEN** V6 在生产 V5 快照的隔离恢复库执行完成
- **THEN** `informatics-knowledge-discipline-v4` 知识点 SHALL 恰好为 48 个
- **AND** BASIC、MATH、ENG、CONTEST 各自 SHALL 恰好为 12 个
- **AND** 系统 SHALL NOT 创建高中专用或竞赛专用平行节点

#### Scenario: 精修文本仍是概念复述
- **WHEN** 第四批知识点不能说明概念对象、适用边界、状态不变量或验证方法
- **THEN** 该节点 SHALL NOT 计入第四批完成数
- **AND** 学习目标 SHALL 补成可观察动作，典型问题 SHALL 补成具体任务或错误情形

### Requirement: 第四批知识点必须保持统一术语和真实依赖
第四批节点 SHALL 优先使用高中课堂可理解的主名，并 SHALL 只把通用、信奥、英文或教师习惯叫法保存为别名；前置知识 SHALL 只引用真实存在的知识节点。

#### Scenario: 高中与竞赛术语重合
- **WHEN** 一个第四批概念同时有高中与信奥常用叫法
- **THEN** 系统 SHALL 复用同一个知识节点
- **AND** `aliases` SHALL 保存不同叫法而不是重复主名

#### Scenario: 前置知识无可靠依据
- **WHEN** 一个第四批节点没有明确学习前置
- **THEN** `prerequisites` SHALL 留空
- **AND** 系统 SHALL NOT 把 `parent_code` 机械复制为前置知识

## MODIFIED Requirements

### Requirement: 正式诊断上下文包必须保留完整代码和结构邻域
系统 SHALL 在最终诊断上下文包中稳定提供题目完整信息、学生完整代码、判题参考信号、初步诊断草稿和 AI 标准库导航确认的结构邻域。

#### Scenario: 构造最终诊断上下文
- **WHEN** 系统构造 `diagnosis-report-v3` 上下文
- **THEN** 上下文 SHALL 包含完整题目描述和完整学生代码
- **AND** 上下文 SHALL 包含判题结果或样例差异作为参考信号
- **AND** 上下文 SHALL 包含初步诊断草稿
- **AND** 上下文 SHALL 包含 AI 标准库导航确认的知识路径、能力点、易错点和提升点邻域

#### Scenario: 标准库上下文不是全库倾倒
- **WHEN** 标准库条目数量超过单次模型上下文预算
- **THEN** 系统 SHALL 让 AI 通过导航阶段逐层选择相关路径
- **AND** 系统 SHALL NOT 要求最终诊断模型从无关全库条目中筛选诊断答案

### Requirement: 提示词必须要求独立诊断并同步标注
系统 SHALL 在 `free-diagnosis-v1` 提示词中要求模型先依据题目、完整代码和运行结果判断真实错误；系统 SHALL 在 `diagnosis-report-v3` 提示词中要求模型基于初步诊断和 AI 导航结果完成标准库路径标注。

#### Scenario: 生成初步诊断提示词
- **WHEN** 系统构造 `free-diagnosis-v1` prompt
- **THEN** prompt SHALL NOT 包含标准库候选包
- **AND** prompt SHALL 要求模型输出真实错误原因假设、关键代码位置、行为差距和导航意图

#### Scenario: 生成最终诊断提示词
- **WHEN** 系统构造 `diagnosis-report-v3` prompt
- **THEN** prompt SHALL 明确要求模型保持初步诊断中由证据支持的真实问题
- **AND** prompt SHALL 明确要求模型使用 AI 导航确认的标准库路径统一术语和颗粒度
- **AND** prompt SHALL 要求模型输出命中状态和待审核库外候选

### Requirement: 提示词必须表达统一树和诊断层关系
正式 AI 诊断 prompt 和标准库导航 prompt SHALL 明确知识树只到知识点，能力点、易错点和提升点属于知识点以下的诊断层，并 SHALL 要求模型不要把知识树和标准库解释成两套平行数据库。

#### Scenario: 生成标准库导航 prompt
- **WHEN** 系统构造 `standard-library-navigation-v1` prompt
- **THEN** prompt SHALL 明确主链路是“知识点 -> 能力点 -> 易错点/提升点”
- **AND** prompt SHALL 明确知识点回答“学什么”，能力点回答“会做什么”，易错点回答“常错在哪里”
- **AND** prompt SHALL 要求 AI 优先使用标准库主名，高中术语和通用术语冲突时以主名为准

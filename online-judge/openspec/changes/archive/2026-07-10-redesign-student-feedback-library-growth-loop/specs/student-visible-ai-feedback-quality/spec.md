## ADDED Requirements

### Requirement: 每条学生建议必须使用独立信息卡
学生结果弹窗 SHALL 将每条修正建议和每条提升建议渲染为独立主卡片，并 SHALL 在卡片内建立标题、正文、知识路径和代码证据的清晰层级。

#### Scenario: 展示多条修正建议
- **WHEN** 学生反馈包含多条 repairItems
- **THEN** 每条 repair item SHALL 使用独立边框、背景和留白
- **AND** 一条建议的知识路径和代码证据 MUST 位于同一建议卡片内

#### Scenario: 展示多条提升建议
- **WHEN** 学生反馈包含多条 improvementItems
- **THEN** 每条 improvement item SHALL 使用独立提升卡片
- **AND** 系统 MUST NOT 仅用分隔线把多条提升建议压成一段连续文本

### Requirement: 知识路径必须作为可辨识子卡片展示
学生反馈 SHALL 使用独立知识路径子卡展示完整 breadcrumb，并 SHALL 区分正式、临时、推断和未归类状态。

#### Scenario: 展示正式知识路径
- **WHEN** 建议绑定正式标准库节点
- **THEN** 知识路径子卡 SHALL 展示全部路径段
- **AND** 最后一级 SHALL 具有明确视觉强调

#### Scenario: 展示临时知识路径
- **WHEN** 建议使用临时标准库节点
- **THEN** 子卡 SHALL 展示正式父 breadcrumb 与临时节点
- **AND** 临时节点 SHALL 使用“AI 新发现”或等价学生友好标记

#### Scenario: 没有有效父路径
- **WHEN** 建议没有正式或临时父路径
- **THEN** 前端 SHALL 显示“暂未归入知识树”或等价文案
- **AND** 前端 MUST NOT 把建议标题伪装成完整知识路径

### Requirement: 代码证据必须作为独立可操作子卡片展示
学生反馈 SHALL 将代码证据与知识路径分开呈现，并 SHALL 保持代码行跳转能力。

#### Scenario: 点击代码证据
- **WHEN** 建议包含 code line 或 range evidence snippet
- **THEN** 代码证据子卡 SHALL 展示行号和代码内容
- **AND** 点击后 SHALL 关闭结果弹窗并高亮编辑器对应行

#### Scenario: 暂无代码行
- **WHEN** 建议只有评测或题面证据
- **THEN** 证据子卡 SHALL 以次要样式说明暂无可跳转代码行
- **AND** 该说明 SHALL NOT 与知识路径混在同一行内

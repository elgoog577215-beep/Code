## ADDED Requirements

### Requirement: 本次结果不得包含独立 Coach 路线
学生本次提交结果 SHALL 使用结构化修复项、提高项和证据帮助学生理解提交，MUST NOT 再提供独立 Coach 生成、换题或回答链路。

#### Scenario: 学生查看非 AC 提交
- **WHEN** 学生反馈包含修复项、提高项或验证问题
- **THEN** 页面 SHALL 将这些内容作为本次反馈的一部分展示
- **AND** 页面 MUST NOT 展示“给我一问”“换一问”或 Coach 回答输入框

#### Scenario: 历史 Coach 数据存在
- **WHEN** 某提交存在历史 Coach 记录
- **THEN** 系统 MAY 保留该记录用于审计或历史兼容
- **AND** 学生主界面 MUST NOT 因该记录恢复独立 Coach 入口

### Requirement: 本次反馈不得垄断学生离开路径
结果页面 SHALL 同时提供继续修改、返回任务范围和查看学习记录的平行入口，不得把 AI 选择的下一题作为默认唯一主要按钮。

#### Scenario: 当前提交未通过
- **WHEN** 学生查看未通过结果
- **THEN** 页面 SHALL 提供继续修改和返回任务列表
- **AND** 两个入口 SHALL 使用客观动作名称

#### Scenario: 当前提交已通过
- **WHEN** 学生查看已通过结果
- **THEN** 页面 SHALL 提供留在本题、返回任务列表和查看学习记录
- **AND** 系统 MAY 展示其他题目但 MUST NOT 宣称其为用户唯一下一步

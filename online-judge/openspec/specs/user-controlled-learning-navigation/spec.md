# user-controlled-learning-navigation Specification

## Purpose
TBD - created by archiving change restore-user-agency-and-simplify-ai-surface. Update Purpose after archive.
## Requirements
### Requirement: 学生必须从完整任务范围中自主选择
学生首页 SHALL 展示用户当前可进入的课堂作业和自主练习范围，并 MUST NOT 使用 AI 推荐垄断首屏主要行动。

#### Scenario: 学生存在多个课堂作业
- **WHEN** 学生进入首页且存在多个可用作业
- **THEN** 系统 SHALL 展示这些作业及其客观状态
- **AND** 学生 SHALL 能自主选择任一作业
- **AND** 系统 MUST NOT 只展示一个由 AI 决定的“下一步”

#### Scenario: 学生没有课堂作业
- **WHEN** 学生进入首页且没有课堂作业
- **THEN** 系统 SHALL 展示自主练习入口和题目范围
- **AND** 系统 MUST NOT 伪造个性化推荐

### Requirement: AI 必须提供判断依据而不是行动命令
AI 可见内容 SHALL 描述当前提交的事实、归因、证据和可选学习方向，MUST NOT 把模型判断表达为用户必须执行的唯一行动。

#### Scenario: AI 反馈可用
- **WHEN** 学生查看一次提交的 AI 反馈
- **THEN** 系统 SHALL 展示基础修复、提高建议及其证据
- **AND** 学生 SHALL 同时拥有继续修改、返回任务列表或查看学习记录的自主入口

#### Scenario: AI 反馈不可用
- **WHEN** AI 反馈生成失败或尚未完成
- **THEN** 判题事实和任务导航 SHALL 保持可用
- **AND** 页面 MUST NOT 阻塞用户进入其他任务

### Requirement: 每个页面必须围绕一个用户对象组织
学生和教师页面 SHALL 以作业、题目、提交或学生证据中的一个明确对象作为主范围，MUST NOT 将跨对象洞察合并成没有边界的综合 AI 面板。

#### Scenario: 教师查看作业
- **WHEN** 教师进入某次作业
- **THEN** 页面 SHALL 只聚合该作业范围内的事实和 AI 归因
- **AND** 其他班级或其他作业的干预信号 MUST NOT 混入主面板

#### Scenario: 学生查看本次提交
- **WHEN** 学生打开本次提交结果
- **THEN** 主界面 SHALL 优先呈现本次判题、修复和提高内容
- **AND** 跨提交成长内容 SHALL 通过独立入口访问

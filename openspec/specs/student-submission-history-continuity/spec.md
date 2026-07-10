# student-submission-history-continuity Specification

## Purpose
TBD - created by archiving change build-student-submission-evidence-loop. Update Purpose after archive.
## Requirements
### Requirement: 学生重新进入题目必须恢复最近结果
系统 SHALL 在学生进入题目时恢复当前历史范围内最近一次提交的评测结果和对应 AI 反馈状态，不得只加载摘要后把结果留空。

#### Scenario: 课堂题目存在历史提交
- **WHEN** 已登录学生重新进入某次作业中的一道已提交题目
- **THEN** 系统 SHALL 展示该学生在该作业和题目范围内最近一次提交的评测结果
- **AND** 系统 SHALL 读取并展示该提交已有的 AI 反馈、生成中状态或失败状态

#### Scenario: 当前范围没有历史提交
- **WHEN** 学生进入当前作业和题目范围内从未提交过的题目
- **THEN** 系统 SHALL 展示尚无提交记录的状态
- **AND** 系统 MUST NOT 使用其他作业或其他学生的提交填充最近结果

### Requirement: 课堂历史必须按学生作业和题目隔离
课堂提交历史 SHALL 使用学生、作业和题目共同确定范围，同一道题在不同作业中的提交不得默认混合展示。

#### Scenario: 同一道题被多个作业复用
- **WHEN** 同一学生在两个作业中提交过同一道题并进入其中一个作业
- **THEN** 历史列表 SHALL 默认只包含当前作业中的提交
- **AND** 最近结果 SHALL 来自当前作业范围

#### Scenario: 公共题库历史
- **WHEN** 已登录学生进入公共题库中的题目
- **THEN** 系统 SHALL 展示该学生在无作业范围内的个人历史
- **AND** 系统 MUST NOT 混入课堂作业提交

### Requirement: 学生必须能重新打开历史提交结果
学生题目页 SHALL 提供按时间倒序排列的历史提交入口，并允许重新打开任意一条有权访问的提交详情与反馈。

#### Scenario: 打开成功反馈历史
- **WHEN** 学生选择一条状态为成功的历史提交
- **THEN** 系统 SHALL 展示该次评测结果、代码、测试点摘要和保存的学生可见反馈
- **AND** 展示内容 SHALL 对应该提交的反馈版本，不得替换为最新提交的反馈

#### Scenario: 打开失败反馈历史
- **WHEN** 学生选择一条 AI 反馈生成失败的历史提交
- **THEN** 系统 SHALL 展示当时保存的失败状态和学生友好原因
- **AND** 系统 MUST NOT 用本地模板伪装成成功反馈

### Requirement: 读取历史反馈不得无条件重新生成
系统 SHALL 优先复用已保存反馈；只有从未请求、生成状态过期或学生明确重试失败反馈时才允许重新入队。

#### Scenario: 读取已完成反馈
- **WHEN** 学生打开状态为 `READY` 的历史反馈
- **THEN** 系统 SHALL 直接返回保存结果
- **AND** 系统 MUST NOT 因展示字段缺失或页面刷新重新调用外部模型

#### Scenario: 恢复过期生成状态
- **WHEN** 历史反馈处于已超过过期阈值的生成中状态
- **THEN** 系统 SHALL 允许重新入队
- **AND** 页面 SHALL 保留原提交结果并显示新的生成状态

### Requirement: 反馈查看事件必须对应真实查看行为
系统 SHALL 仅在学生实际打开可见反馈时记录查看事件，不得因后台预取或历史摘要加载记录已查看。

#### Scenario: 后台恢复最近提交
- **WHEN** 页面在后台加载最近提交和反馈但学生未打开结果详情
- **THEN** 系统 MUST NOT 记录反馈已查看

#### Scenario: 学生打开反馈详情
- **WHEN** 学生打开包含成功模型反馈的结果详情
- **THEN** 系统 SHALL 记录该学生、提交和反馈版本的查看事件
- **AND** 重复打开的计数规则 SHALL 保持幂等并保留首次查看时间

### Requirement: 学生历史状态必须提供中英文文案
新增或修改的最近结果、历史列表、反馈状态和失败状态 SHALL 同时提供中文与英文文案，并在两种语言下保持可用布局。

#### Scenario: 切换英文模式
- **WHEN** 学生在有历史提交和 AI 反馈的题目页切换到英文
- **THEN** 历史入口、评测状态、反馈状态、空状态和错误提示 SHALL 使用英文
- **AND** 页面 MUST NOT 残留硬编码中文或出现文字遮挡

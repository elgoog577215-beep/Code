# single-problem-submission-growth Specification Delta

## ADDED Requirements

### Requirement: 单题成长必须标识真正改变结果的关键修改
系统 SHALL 从同一范围的可比较有效成长摘要中选择一组关键前后提交，优先使用问题恢复、可比测试点增加或未通过转为通过的证据；重复无变化和不可比较提交 MUST NOT 被选为关键修改。

#### Scenario: 某次修改恢复旧问题并增加通过测试点
- **WHEN** 同题多次提交中有一次有效修改同时形成恢复问题或通过测试点增加
- **THEN** 系统 SHALL 将该次提交及最近可比较基线标记为关键修改证据
- **AND** 页面 SHALL 展示具体测试点变化和恢复问题

#### Scenario: 失败后直接形成通过提交
- **WHEN** 学生此前未通过且后续有效提交通过，但问题生命周期证据不足
- **THEN** 系统 SHALL 使用前后判题形成最小关键修改证据
- **AND** 系统 MUST NOT 伪造已经恢复的具体问题

#### Scenario: 只有重复提交
- **WHEN** 学生只有重复无变化提交或没有可比较基线
- **THEN** 系统 SHALL 显示尚未观察到关键修改
- **AND** 页面 SHALL 保留原提交时间线

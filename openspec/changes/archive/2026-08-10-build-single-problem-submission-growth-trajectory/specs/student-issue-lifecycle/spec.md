## MODIFIED Requirements

### Requirement: 问题生命周期必须由不可覆盖事实投影
系统 SHALL 保留每次提交的完整问题集合，并以独立转换记录表达 `NEW`、`PERSISTED`、`NOT_OBSERVED`、`RECOVERED`、`RECURRED` 和 `UNCOMPARABLE`，不得回写覆盖历史事实；转换 SHALL 使用最近可比较有效提交作为基线，重复和不可比较记录不得错误打断问题生命周期。

#### Scenario: 旧问题持续并出现新问题
- **WHEN** 后续有效提交保留部分旧问题并新增一个规范化问题
- **THEN** 系统 SHALL 为保留问题记录持续状态
- **AND** 系统 SHALL 为新增问题记录新出现状态

#### Scenario: 诊断失败导致不可比较
- **WHEN** 当前或候选基线提交没有完整诊断事实
- **THEN** 系统 SHALL 记录不可比较或证据不足
- **AND** 系统 MUST NOT 把旧问题缺失解释为已恢复

#### Scenario: 问题消失后再次出现
- **WHEN** 某规范化问题曾在可比较提交中消失并形成恢复证据，之后又再次出现
- **THEN** 系统 SHALL 将其记录为复发
- **AND** 系统 SHALL 保留首次出现、消失和再次出现的提交引用

#### Scenario: 单次未观察到问题
- **WHEN** 某问题只在一次后续可比较提交中没有再次观察到且尚无更强验证
- **THEN** 系统 SHALL 将其标记为 `NOT_OBSERVED`
- **AND** 页面 MUST NOT 将其描述为已经彻底修复

#### Scenario: 重复提交位于两个有效提交之间
- **WHEN** 同一问题在一个有效提交后经过若干完全重复提交，并在下一次有效提交继续出现
- **THEN** 下一次有效提交 SHALL 与最近有效基线比较并标记问题持续存在
- **AND** 重复提交 MUST NOT 增加有效出现次数或重置连续次数

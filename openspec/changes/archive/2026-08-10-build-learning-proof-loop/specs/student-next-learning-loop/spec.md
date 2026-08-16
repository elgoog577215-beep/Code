# student-next-learning-loop Specification Delta

## ADDED Requirements

### Requirement: 可检查说明后只能进入真实关联题验证
学生形成修正说明后，系统 SHALL 只在同一作业存在未尝试且与来源题具有已保存知识关系的题目时提供“换一题验证”入口；没有安全目标时 SHALL 明确尚未安排，MUST NOT 构造任意题目。

#### Scenario: 存在同知识未做题
- **WHEN** 同一作业有一题与来源题共享已保存知识点且学生尚未尝试
- **THEN** 系统 SHALL 返回该题作为换题验证目标
- **AND** 入口 SHALL 保留学生、作业、来源提交和行动事件关系

#### Scenario: 没有真实关联题
- **WHEN** 同一作业不存在满足关系的未做题
- **THEN** 系统 SHALL 返回尚未安排换题验证
- **AND** 页面 MUST NOT 推荐任意公共题或其他作业题

### Requirement: 换题验证必须由后续正式提交回流
系统 SHALL 使用后续关联题的正式提交和现有后续验证分析返回换题状态；只有真实通过证据 SHALL 标记为换题已验证。

#### Scenario: 学生通过关联题
- **WHEN** 学生在关联题产生正式通过提交且关系可核验
- **THEN** 学习证明 SHALL 标记为换题已验证
- **AND** 返回目标问题和目标提交证据

#### Scenario: 学生尚未通过关联题
- **WHEN** 学生已尝试关联题但没有通过证据
- **THEN** 系统 SHALL 保留需要继续处理或等待证据状态
- **AND** 页面 MUST NOT 将进入题目或一次提交视为验证完成

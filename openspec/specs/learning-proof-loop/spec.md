# learning-proof-loop Specification

## Purpose
TBD - created by archiving change build-learning-proof-loop. Update Purpose after archive.
## Requirements
### Requirement: 学习证明必须由三个可追证阶段组成
系统 SHALL 为同一学生、题目和作业范围返回关键修改、学生说明和换题验证三个阶段；每个已完成阶段 SHALL 提供对应提交、回答或目标问题证据，前端 MUST NOT 自行推断完成状态。

#### Scenario: 三类证据均存在
- **WHEN** 学生存在有效关键修改、已保存可核验说明，并在关联题目形成后续通过提交
- **THEN** 系统 SHALL 将三个阶段分别标记为已有证据
- **AND** 每个阶段 SHALL 返回可打开的证据引用

#### Scenario: 某阶段没有证据
- **WHEN** 当前范围没有对应提交、回答或安全换题目标
- **THEN** 系统 SHALL 返回该阶段的尚未状态
- **AND** 页面 MUST NOT 展示低可信度结论或伪造完成原因

### Requirement: 修正说明必须绑定具体提交并幂等保存
系统 SHALL 为已经通过或已有关键修改证据的提交创建固定修正问题，并复用现有 Coach 存储保存学生回答；同一提交重复创建 MUST 返回同一条修正说明记录。

#### Scenario: 学生首次打开说明
- **WHEN** 合法学生为满足条件的提交请求修正说明
- **THEN** 系统 SHALL 创建一条 `LEARNING_REFLECTION` 记录
- **AND** 记录 SHALL 绑定该提交、固定问题和证据范围

#### Scenario: 学生重复打开说明
- **WHEN** 同一提交已经存在 `LEARNING_REFLECTION` 记录
- **THEN** 系统 SHALL 返回原记录
- **AND** 系统 MUST NOT 生成第二条重复问题

### Requirement: 学生回答只能形成可核验说明而非掌握结论
系统 SHALL 保存学生原始说明，并根据具体修改、失败原因、样例或边界等可检查内容返回说明状态；系统 MUST NOT 仅根据回答存在就宣称学生已经理解或掌握。

#### Scenario: 回答包含具体验证内容
- **WHEN** 学生说明包含具体修改与可验证样例、边界或代码证据
- **THEN** 系统 SHALL 将说明标记为可核验
- **AND** 教师和学生 SHALL 能查看同一原始回答

#### Scenario: 回答过于笼统
- **WHEN** 学生只提交无法对应修改或验证的笼统文本
- **THEN** 系统 SHALL 保留回答并标记为已提交
- **AND** 系统 MUST NOT 将其标记为已经掌握

### Requirement: 学生与教师必须共享同一学习证明投影
学生题目页、教师题目列表和教师学生单题详情 SHALL 消费同一后端学习证明状态；教师聚合 SHALL 按学生去重，并支持下钻到生成该人数的证据。

#### Scenario: 教师查看已说明学生
- **WHEN** 教师在题目页选择已说明阶段
- **THEN** 页面 SHALL 只展示后端聚合中具有保存说明的学生
- **AND** 打开学生详情 SHALL 展示与学生端相同的说明与绑定提交

### Requirement: 学习证明新增界面必须双语且保持正文优先
学生和教师新增的阶段名、状态、操作、反馈与空状态 SHALL 同时提供中文和英文；界面 SHALL 使用紧凑状态行与正文内面板，不得新增首屏解释性横幅或无证据统计卡。

#### Scenario: 英文窄屏查看学习证明
- **WHEN** 用户在英文模式和窄屏设备打开学生或教师题目页
- **THEN** 三个阶段 SHALL 使用英文并保持单列可读
- **AND** 页面主体 SHALL 在状态行后立即出现

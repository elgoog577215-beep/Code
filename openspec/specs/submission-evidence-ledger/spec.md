# submission-evidence-ledger Specification

## Purpose
TBD - created by archiving change build-student-submission-evidence-loop. Update Purpose after archive.
## Requirements
### Requirement: 课堂提交必须绑定合法教学上下文
系统 SHALL 由后端基于学生令牌校验并绑定学生、班级、作业和题目关系，课堂作业不得产生无法归属到学生的匿名提交。

#### Scenario: 合法课堂提交
- **WHEN** 已登录学生向所属班级作业中的题目提交代码
- **THEN** 系统 SHALL 保存学生、班级关联、作业和题目上下文
- **AND** 保存的学生身份 SHALL 来自已验证令牌

#### Scenario: 伪造其他学生身份
- **WHEN** 请求携带的学生 ID 与令牌身份不一致
- **THEN** 系统 SHALL 拒绝提交
- **AND** 系统 MUST NOT 创建提交或反馈记录

#### Scenario: 题目不属于作业
- **WHEN** 学生向某作业提交不属于该作业的题目
- **THEN** 系统 SHALL 拒绝提交并返回明确错误

#### Scenario: 匿名公共练习
- **WHEN** 未登录用户在公共题库提交代码
- **THEN** 系统 MAY 保存匿名练习记录和反馈状态
- **AND** 该记录 MUST NOT 进入课堂学生统计或个人画像

### Requirement: 每次提交必须建立完整持久化状态
系统 SHALL 在提交完成评测后保存提交、测试点和反馈状态；AI 生成失败不得删除或隐藏评测事实。

#### Scenario: AI 异步生成成功
- **WHEN** 提交完成评测并成功生成 AI 反馈
- **THEN** 系统 SHALL 保留提交和测试点记录
- **AND** 系统 SHALL 保存成功反馈当前快照和对应版本

#### Scenario: AI 异步生成失败
- **WHEN** 提交完成评测但 AI 调用失败
- **THEN** 系统 SHALL 保留提交和测试点记录
- **AND** 系统 SHALL 保存失败反馈状态、规范化失败原因和生成时间

#### Scenario: 进程在生成前退出
- **WHEN** 提交已完成评测但异步工作尚未开始
- **THEN** 数据库 SHALL 已存在可恢复的反馈待处理或生成中状态
- **AND** 后续恢复任务 SHALL 能继续处理该提交

### Requirement: 反馈重新生成必须保留版本
每次反馈生成终态 SHALL 形成不可覆盖的版本记录，当前反馈快照 SHALL 指向或反映最新可展示版本。

#### Scenario: 失败后重试成功
- **WHEN** 某提交第一次反馈失败后再次生成成功
- **THEN** 系统 SHALL 同时保留失败版本和成功版本
- **AND** 当前反馈 SHALL 展示成功版本

#### Scenario: 同一请求重复完成
- **WHEN** 异步重试重复提交同一生成结果
- **THEN** 系统 SHALL 通过幂等键避免创建重复版本

### Requirement: 每个诊断问题必须投影为结构化事实
系统 SHALL 将完整分析中的每个有效问题投影为独立诊断事实，并保留原始完整报告用于审计。

#### Scenario: 一次提交包含多个问题
- **WHEN** AI 分析返回多个具有独立问题 ID 的有效问题
- **THEN** 系统 SHALL 为每个问题保存独立事实记录
- **AND** 每条事实 SHALL 保留问题层级、主要问题状态、路径、标准库归属、证据引用和置信度

#### Scenario: 标准库正式命中
- **WHEN** 问题绑定正式知识点、能力点或易错点
- **THEN** 事实 SHALL 保存稳定标准库 ID、完整 breadcrumb 和 `FORMAL` 状态

#### Scenario: 标准库未命中
- **WHEN** 问题只有临时父路径、推断路径或无法归类
- **THEN** 事实 SHALL 分别保存 `PROVISIONAL`、`INFERRED` 或 `UNCLASSIFIED` 状态
- **AND** 系统 MUST NOT 用建议标题伪造正式路径

### Requirement: 教师校正必须叠加而不篡改原始事实
教师对错因、知识路径、证据或建议表达的校正 SHALL 作为独立校正层保存，并可形成教师端有效视图。

#### Scenario: 教师修正知识路径
- **WHEN** 教师对某诊断问题提交新的知识路径
- **THEN** 系统 SHALL 保留原始 AI 路径和教师修正路径
- **AND** 后续教师聚合 SHALL 能使用有效修正值并显示校正来源

### Requirement: 学习事件必须可关联到提交和反馈版本
反馈生成、失败、查看及后续同题提交的关联 SHALL 使用稳定提交 ID、学生 ID、作业 ID、题目 ID和反馈版本 ID。

#### Scenario: 查看后产生同题提交
- **WHEN** 学生查看反馈后再次提交同一作业中的同一道题
- **THEN** 系统 SHALL 关联反馈提交与时间上第一条后续提交
- **AND** 系统 SHALL 保留两次提交引用用于变化分析

#### Scenario: 尚无后续提交
- **WHEN** 学生已查看反馈但尚未再次提交同题
- **THEN** 系统 SHALL 保留等待后续状态
- **AND** 系统 MUST NOT 推断反馈有效或无效

### Requirement: 历史数据回填必须幂等且诚实表达缺失
系统 SHALL 通过可重复运行的回填任务投影现有提交、分析、反馈和事件，并记录无法可靠补齐的数据质量状态。

#### Scenario: 重复运行回填
- **WHEN** 管理员对同一历史范围重复运行回填
- **THEN** 诊断事实、反馈版本和事件关联计数 MUST NOT 重复增加

#### Scenario: 历史课堂提交缺少学生身份
- **WHEN** 历史提交带有作业但没有可验证学生身份
- **THEN** 系统 SHALL 标记身份缺失并保留原始记录
- **AND** 系统 MUST NOT 根据姓名、时间或相邻记录自动猜测学生

#### Scenario: 历史反馈缺少可靠知识路径
- **WHEN** 历史反馈无法从标准库 ID 或后端 breadcrumb 恢复路径
- **THEN** 系统 SHALL 标记为未归类或历史推断
- **AND** 系统 MUST NOT 把该记录计入正式路径命中

### Requirement: 每次提交必须保存完整诊断点集合
系统 SHALL 将 AI 返回的全部有效核心错误、基础修复/风险点和提升点投影为逐提交事实，不得按固定数量、主要问题或前端展示上限截断。

#### Scenario: 一次提交返回二十个有效点
- **WHEN** 完整分析返回十个错误/修复点和十个提升点且均通过证据校验
- **THEN** 系统 SHALL 保存二十条对应事实
- **AND** 当前反馈版本 SHALL 保留全部二十个学生可见项

#### Scenario: 单条无效不影响其他点
- **WHEN** 多个分析点中有一条未通过结构或证据校验
- **THEN** 系统 MAY 拒绝该无效点并记录质量状态
- **AND** 系统 MUST NOT 因单条无效而截断或删除其他有效点

### Requirement: 诊断事实必须保存生命周期投影输入
每条诊断事实 SHALL 保存规范化问题键、展示类别、匹配来源和算法版本；提交 SHALL 提供可用于判断重复代码的稳定源码指纹或等价输入。

#### Scenario: 新提交完成事实投影
- **WHEN** 合法课堂提交完成分析和事实投影
- **THEN** 每条事实 SHALL 具有可查询的规范化问题键和展示类别
- **AND** 后续生命周期投影 SHALL 能判断本次是否为有效尝试

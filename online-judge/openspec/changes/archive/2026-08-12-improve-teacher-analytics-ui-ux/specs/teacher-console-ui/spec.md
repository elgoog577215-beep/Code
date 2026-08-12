## ADDED Requirements

### Requirement: 教师分析入口必须显示可行动状态
教师班级和作业分析入口 SHALL 优先展示名册、已提交、未提交、一次通过和最终通过等无需额外解释即可行动的数据，不得用教师姓名、重复零值或难以理解的统计口径占据主要状态位置。

#### Scenario: 教师选择班级
- **WHEN** 教师打开班级分析入口且班级学情摘要可用
- **THEN** 每个班级行 SHALL 展示名册人数、已提交人数和未提交人数
- **AND** 教师 SHALL 能从整行进入对应班级分析

#### Scenario: 教师查看作业题目
- **WHEN** 教师打开作业分析页
- **THEN** 每道题 SHALL 展示已提交、一次通过、最终通过和未提交人数
- **AND** 页面 SHALL NOT 把有效修改中位数作为题目行主要指标

#### Scenario: 教师查看未提交学生
- **WHEN** 学生尚未提交当前题目
- **THEN** 学生行 SHALL 显示未提交状态
- **AND** 学生行 SHALL NOT 重复显示原始提交 0 和有效修改 0

### Requirement: 教师工作台窄屏必须完整可达且保持正文优先
教师端在 390px 可用宽度下 SHALL 在不横向截断功能的前提下完整显示教学分析、班级名单、题库管理、AI 标准库和系统状态入口，并 SHALL 压缩头部、对象路径和行内指标以让正文尽早出现。

#### Scenario: 教师在手机上打开工作台
- **WHEN** 视口宽度为 390px
- **THEN** 品牌、菜单与语言控制 SHALL 位于同一头部行
- **AND** 五个教师功能入口 SHALL 全部可见且可点击

#### Scenario: 教师在手机上下钻分析
- **WHEN** 教师进入作业、题目或学生详情
- **THEN** 对象路径 SHALL 只显式展示上一级与当前对象
- **AND** 行内核心指标 SHALL 在同一紧凑状态带中展示，而不是机械拆成多行

### Requirement: 学生提交证据工作区必须保留提交上下文
教师查看单个学生的题目过程时，成长轨迹、提交证据和 AI 分析 SHALL 共用当前提交选择，教师 SHALL 能在任一分类入口直接切换提交，且分类控件 SHALL 提供完整标签页语义和键盘操作。

#### Scenario: 教师跨分类查看同一次提交
- **WHEN** 教师选择一次历史提交并切换到提交证据或 AI 分析
- **THEN** 页面 SHALL 保持该提交为当前提交
- **AND** 当前提交编号、结果和时间 SHALL 保持可见

#### Scenario: 教师在证据分类切换提交
- **WHEN** 教师在提交证据或 AI 分析分类选择另一提交
- **THEN** 页面 SHALL 在当前分类内加载新提交对应的证据或已保存分析
- **AND** 教师 SHALL NOT 必须返回成长轨迹后再切回

#### Scenario: 教师使用键盘切换分类
- **WHEN** 焦点位于学生证据分类标签并按左右方向键
- **THEN** 焦点和当前面板 SHALL 移动到相邻分类
- **AND** 标签与面板 SHALL 通过 `aria-controls` 与 `aria-labelledby` 建立关联

### Requirement: 教师成长详情不得重复统计或生成空容器
教师学生详情 SHALL 使用紧凑的所选提交对比呈现测试点变化、未解决问题和真实问题信号，不得重复页头已有的提交次数与有效修改，也不得为无数据的知识点、问题变化或矩阵生成独立空卡。

#### Scenario: 当前提交没有问题信号
- **WHEN** 所选提交没有新增、持续、复发或已解决问题信号
- **THEN** 页面 SHALL 以单一紧凑结果说明当前没有可展示的问题变化
- **AND** 页面 SHALL NOT 展示多个无数据卡片

#### Scenario: 学生端查看成长仪表盘
- **WHEN** 学生进入现有成长分析
- **THEN** 学生端成长仪表盘结构 SHALL 保持不变

### Requirement: 教师学生详情正式地址必须可直接访问
服务端 SHALL 将班级分析路径下的学生题目详情 URL 转发到前端应用，使教师能够刷新、收藏或直接打开该页面，同时不得把未知 API 请求转发为 HTML。

#### Scenario: 教师直接打开学生详情
- **WHEN** 浏览器请求 `/code/teacher/classes/{classId}/assignments/{assignmentId}/problems/{problemId}/students/{studentProfileId}`
- **THEN** 服务端 SHALL 返回前端应用入口而不是 404

#### Scenario: 未知 API 保持 404
- **WHEN** 浏览器请求不存在的 `/code/api/...` 地址
- **THEN** 服务端 SHALL 保持 API 的资源不存在响应

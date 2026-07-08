## ADDED Requirements

### Requirement: 正式诊断 Agent 必须输出独立建议数组
系统 SHALL 要求正式诊断 Agent 将学生摘要、基础层建议、提高层建议、诊断锚点、标准库命中状态和标准库成长候选分开输出；基础层和提高层建议数量 SHALL 由当前提交中独立问题和真实提升方向决定。

#### Scenario: 多个独立错因
- **WHEN** 当前提交存在多个互相独立且有证据支持的基础问题
- **THEN** 正式诊断 Agent SHALL 在 `basicLayerAdvice` 中输出多条建议
- **AND** 后端 SHALL NOT 要求模型把多条建议合并成一段文字

#### Scenario: 没有真实提升方向
- **WHEN** 当前提交没有明确提升建议或提升建议会重复基础修正
- **THEN** 正式诊断 Agent MAY 返回空 `improvementLayerAdvice`
- **AND** 后端 SHALL NOT 为了凑数生成空泛提升项

### Requirement: 标准库成长候选必须默认进入人工候选池
系统 SHALL 将正式诊断 Agent 返回的 `libraryGrowth` 视为待审核候选线索；任何新错因、新提升点或新路径在教师审核前 MUST NOT 直接影响正式标准库。

#### Scenario: 模型发现库外错因
- **WHEN** `diagnosisDecision.libraryFit` 为 `PARTIAL`、`MISS` 或 `OUT_OF_LIBRARY`
- **AND** 模型返回标准库成长候选
- **THEN** 后端 SHALL 将候选保存为待审核或聚合到已有待审核候选
- **AND** 后端 SHALL NOT 直接写入正式标准库

#### Scenario: 命中已有标准库
- **WHEN** `diagnosisDecision.libraryFit` 为 `HIT`
- **THEN** 后端 SHALL NOT 因模型同时返回 `libraryGrowth` 而创建新成长候选

### Requirement: 正式诊断上下文必须优先保障当前提交事实
系统 SHALL 在正式诊断上下文中优先保留题目、学生代码、判题事实、失败样例、运行错误和规则信号；标准库参考包 SHALL 只作为命名、颗粒度和成长线索，不得替代当前提交证据。

#### Scenario: 标准库候选与提交证据冲突
- **WHEN** 标准库候选方向与当前提交证据不一致
- **THEN** 正式诊断 Agent SHALL 以当前提交证据为准
- **AND** MAY 将标准库关系标记为 `MISS` 或 `OUT_OF_LIBRARY`

#### Scenario: 代码较长
- **WHEN** 学生代码超过快反馈上下文预算
- **THEN** 教师深诊断上下文 SHALL 保留足够代码范围、失败点附近代码和可引用证据候选
- **AND** 不得只依赖第一条失败样例或第一条规则信号生成完整诊断

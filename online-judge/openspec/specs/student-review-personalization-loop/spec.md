# student-review-personalization-loop Specification

## Purpose

定义学生复盘报告、细颗粒画像、个性化推荐、教师班级洞察、评测闭环和标准库成长候选之间的主流程关系，确保实时链路保持简单，后续教学功能复用同一套诊断与画像数据，而不是各自重复判断。

## Requirements

### Requirement: 学生复盘报告优先展示

系统 SHALL 在学生提交后优先提供学生可读的复盘报告，而不是直接展示机器字段拼接内容。

#### Scenario: AI 复盘成功
- **WHEN** 学生提交产生判题结果且外部 AI 返回合法诊断
- **THEN** 系统返回包含基础层、提高层和下一步行动的 `studentReport`
- **AND** 学生端优先展示 `studentReport`
- **AND** 结构化字段仅用于校验、追踪、画像和评测

#### Scenario: AI 不可用
- **WHEN** 外部模型认证、额度、超时或服务不可用
- **THEN** 系统不得伪装成 AI 成功
- **AND** 学生端 SHALL 明确展示 AI 复盘暂不可用或规则反馈
- **AND** trace SHALL 记录失败原因

### Requirement: 默认实时链路保持单诊断 Agent

系统 SHALL 默认关闭实时搜索 Agent，只使用本地召回和一次外部诊断 Agent。

#### Scenario: 默认诊断
- **WHEN** 未显式开启 `AI_SEARCH_LOCATION_ENABLED`
- **THEN** 实时诊断 SHALL 只调用一次外部模型
- **AND** 输入 SHALL 包含本地召回的树形标准库候选包
- **AND** trace SHALL 标记召回状态为 `LOCAL_RECALL`

#### Scenario: 标准库未命中
- **WHEN** 本地召回候选不覆盖真实错因
- **THEN** 诊断 Agent SHALL 能返回 `OUT_OF_LIBRARY`
- **AND** 系统 SHALL 记录库外发现候选
- **AND** 不得把不匹配的标准库条目强行作为答案

### Requirement: 学生画像驱动推荐

系统 SHALL 使用同一套学生细颗粒画像驱动推荐，而不是在推荐模块重复解析诊断结果。

#### Scenario: 有未解决错因
- **WHEN** 学生画像存在最近失败题、错因标签或复盘卡片
- **THEN** 推荐 SHALL 优先生成复盘或同题重做建议
- **AND** 推荐理由 SHALL 说明与错因、能力点或复盘卡片的关系

#### Scenario: 基础问题较少
- **WHEN** 学生近期基础层问题较少或已有 AC
- **THEN** 推荐 MAY 转向同类新题、提高层拓展或迁移练习

### Requirement: 教师班级洞察复用画像

系统 SHALL 在教师端聚合班级高频错因和能力薄弱点，并复用学生画像和诊断元数据。

#### Scenario: 作业总览存在多名学生诊断结果
- **WHEN** 教师查看班级或作业总览
- **THEN** 系统 SHALL 展示高频细颗粒错因、能力薄弱点和建议讲评方向
- **AND** 每个洞察 SHALL 能回到代表题或代表学生证据

### Requirement: 评测闭环保存学生可见报告

系统 SHALL 在 AI 质量评测中保存学生实际看到的报告，并按固定失败类型归因。

#### Scenario: 运行典型题评测
- **WHEN** 评测样例执行完成
- **THEN** 输出 SHALL 包含 JSON 机器结果和 Markdown 人类报告
- **AND** 每题 SHALL 保存 `studentReport`
- **AND** 失败 SHALL 归入 `RECALL_MISS`、`MODEL_MISREAD`、`TEXT_BAD`、`ANSWER_LEAK`、`TOO_LONG`、`LIBRARY_GAP`、`VALIDATOR_TOO_STRICT` 或 `VALIDATOR_TOO_LOOSE`

### Requirement: 标准库成长候选不实时入库

系统 SHALL 将库外发现进入候选池，并等待教师审核后再进入正式标准库。

#### Scenario: 发现库外错因
- **WHEN** 诊断输出 `OUT_OF_LIBRARY`
- **THEN** 系统 SHALL 创建或聚合标准库成长候选
- **AND** 候选 SHALL 包含来源提交、建议路径、相似已有条目、证据摘要、出现次数和状态
- **AND** 正式标准库 SHALL 不被实时自动修改

#### Scenario: 教师批准候选
- **WHEN** 教师批准标准库成长候选
- **THEN** 系统 SHALL 将其转为正式标准库条目
- **AND** 标记对应 embedding 过期，等待后台重建

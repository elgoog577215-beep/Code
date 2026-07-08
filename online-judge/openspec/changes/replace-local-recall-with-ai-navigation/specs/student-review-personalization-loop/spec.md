## MODIFIED Requirements

### Requirement: 默认实时链路保持单诊断 Agent
系统 SHALL 默认使用“初步诊断 -> AI 标准库导航 -> 最终诊断”的实时链路，其中只有最终诊断 Agent 生成学生可见复盘报告；系统 SHALL NOT 使用本地召回作为默认标准库选择方式。

#### Scenario: 默认诊断
- **WHEN** 学生提交产生判题结果且外部 AI 可用
- **THEN** 实时诊断 SHALL 先生成初步诊断
- **AND** 实时诊断 SHALL 通过 AI 标准库导航选择知识点、能力点、易错点和提升点
- **AND** 实时诊断 SHALL 由最终诊断 Agent 返回 `studentReport`
- **AND** trace SHALL 标记标准库定位状态为 `AI_NAVIGATION`

#### Scenario: 默认链路不做本地召回
- **WHEN** 系统执行实时诊断
- **THEN** 输入 SHALL NOT 包含后端本地召回生成的树形候选包
- **AND** trace SHALL NOT 标记为 `LOCAL_RECALL`

#### Scenario: 标准库未命中
- **WHEN** AI 标准库导航或最终诊断判断现有标准库不覆盖真实错因
- **THEN** 诊断 Agent SHALL 能返回 `OUT_OF_LIBRARY`
- **AND** 系统 SHALL 记录库外发现候选
- **AND** 不得把不匹配的标准库条目强行作为答案

### Requirement: 标准库成长候选不实时入库
系统 SHALL 将库外发现进入候选池，并等待教师审核后再进入正式标准库；AI 标准库导航可以提出候选路径，但不得直接写入正式标准库。

#### Scenario: 发现库外错因
- **WHEN** 初步诊断、AI 标准库导航或最终诊断输出 `OUT_OF_LIBRARY`
- **THEN** 系统 SHALL 创建或聚合标准库成长候选
- **AND** 候选 SHALL 包含来源提交、建议路径、相似已有条目、证据摘要、出现次数和状态
- **AND** 正式标准库 SHALL 不被实时自动修改

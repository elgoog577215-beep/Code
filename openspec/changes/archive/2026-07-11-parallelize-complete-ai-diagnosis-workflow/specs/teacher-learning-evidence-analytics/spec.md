## ADDED Requirements

### Requirement: 教师统计投影必须以正式诊断事实为边界
教师个人、班级、作业和题目统计 SHALL 只消费已经保存并通过证据校验的规范诊断事实；正式诊断后的教师统计投影 SHALL 幂等执行并记录状态，模型临时输出或未完成 Run MUST NOT 直接进入统计分子。

#### Scenario: 正式诊断完成后更新统计
- **WHEN** 某提交的正式分析和规范问题事实保存成功
- **THEN** 系统 SHALL 触发教师统计投影并更新个人与班级证据范围
- **AND** 同一事实重复投影 MUST NOT 增加原始或加权次数

#### Scenario: Run 尚未完成
- **WHEN** 某提交仍停留在核心诊断、问题挂接或输出生成阶段
- **THEN** 教师数据完整性 SHALL 将其计为生成中或未完成
- **AND** 系统 MUST NOT 将模型阶段临时 JSON 计入错误统计

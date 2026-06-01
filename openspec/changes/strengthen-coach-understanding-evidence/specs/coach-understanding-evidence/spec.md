## ADDED Requirements

### Requirement: Coach answer quality must expose understanding evidence
系统 SHALL 对学生 Coach 回答输出结构化理解证据信号，包括理解层级、证据完整度、可验证性、行动状态和建议教学动作。

#### Scenario: Student answer contains multiple verifiable evidence types
- **WHEN** 学生回答同时包含最小样例、复杂度估算或输出对比等多个证据
- **THEN** Coach 回答质量信号 MUST 标记为可验证
- **AND** 理解层级 MUST 至少达到 `VERIFICATION`
- **AND** 下一步动作 MUST 引导学生做最小修改或预测评测现象

### Requirement: Vague answers must remain in evidence collection mode
系统 SHALL 将空泛确认或只有方向的回答保持在证据收集模式，不得进入更高提示层级。

#### Scenario: Student only says they understand
- **WHEN** 学生回答只表示“知道了”“我改一下”或类似空泛确认
- **THEN** Coach 回答质量信号 MUST 标记为不可验证
- **AND** 行动状态 MUST 为 `NEEDS_EVIDENCE`
- **AND** 下一步动作 MUST 要求补充最小样例、变量轨迹、输出对比或复杂度数量级

### Requirement: Answer-like content must be treated as safety risk
系统 SHALL 将疑似完整答案、代码或直接改法的学生回答标记为安全风险，并把教学动作拉回证据层。

#### Scenario: Student answer includes complete code or final answer wording
- **WHEN** 学生回答包含完整代码、最终答案、参考代码或可执行控制结构
- **THEN** Coach 回答质量信号 MUST 标记为 `SAFETY_RISK`
- **AND** needsTeacherAttention MUST 为 true
- **AND** 下一步动作 MUST 要求学生只描述触发问题的输入特征或证据

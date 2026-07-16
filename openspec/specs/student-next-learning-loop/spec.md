# student-next-learning-loop Specification

## Purpose
TBD - created by archiving change productize-student-next-learning-loop. Update Purpose after archive.
## Requirements
### Requirement: 学生首页必须展示单一主学习行动
系统 SHALL 在登录学生首页使用后端返回的推荐顺序展示一个最高优先级主行动，并将其他有效建议降为次级候选；前端 MUST NOT 根据标签文本、通过率或本地规则重新计算推荐优先级。

#### Scenario: 存在多条推荐
- **WHEN** 推荐接口返回两条或以上有效建议
- **THEN** 页面 SHALL 将第一条展示为本轮主行动
- **AND** 其余建议 SHALL 以较低视觉权重保留

### Requirement: 学习行动必须说明理由与完成标准
主行动 SHALL 展示行动目标和推荐理由，并在后端提供时展示关联能力、知识标签、学习假设、预期完成信号、风险和回退动作；页面 MUST NOT 为缺失字段编造已经掌握或完成的结论。

#### Scenario: 推荐包含完整学习契约
- **WHEN** 推荐包含理由、能力标签和预期完成信号
- **THEN** 学生 SHALL 能在不进入题目前理解为什么做以及怎样算完成

#### Scenario: 推荐缺少完成信号
- **WHEN** 推荐没有预期完成信号
- **THEN** 页面 MUST NOT 展示模板式虚假完成标准
- **AND** 行动入口仍可在其他必要字段完整时使用

### Requirement: 推荐行动必须进入既有学习证据链
可跳转行动 SHALL 携带学生身份和推荐令牌进入对应题目，并在点击、进入题目和再次提交时复用现有推荐事件链；无安全目标路由时系统 SHALL 只展示建议，不得构造错误入口。

#### Scenario: 推荐指向作业题
- **WHEN** 推荐同时包含作业 ID、题目 ID 和推荐令牌
- **THEN** 主按钮 SHALL 进入对应作业题目并携带学生身份与推荐令牌

#### Scenario: 推荐目标不完整
- **WHEN** 推荐缺少构造安全目标所需的题目或作业信息
- **THEN** 页面 SHALL 保留可读建议
- **AND** 页面 MUST NOT 展示指向错误题目的主按钮

### Requirement: 推荐状态必须诚实且不阻塞首页
推荐加载、空结果和请求失败 SHALL 使用不同状态表达，并与公共题库和作业入口独立；推荐失败 MUST NOT 阻塞学生继续访问原有学习资源，也不得被描述为学生已经掌握。

#### Scenario: 推荐请求失败
- **WHEN** 推荐接口暂时不可用
- **THEN** 页面 SHALL 明确说明暂时无法读取并提供重试
- **AND** 公共练习和课堂作业 SHALL 继续可用

#### Scenario: 暂无可靠推荐
- **WHEN** 推荐接口成功但没有有效建议
- **THEN** 页面 SHALL 说明当前暂无可靠的个性化行动
- **AND** 页面 SHALL 引导学生继续当前作业或自主练习

### Requirement: 下一步学习区域必须双语且可访问
新增标题、状态、按钮、标签和辅助说明 SHALL 同时提供中文和英文，并满足键盘操作、屏幕阅读顺序、非颜色状态表达和移动端单列布局。

#### Scenario: 英文移动端访问
- **WHEN** 学生在英文模式和窄屏设备打开首页
- **THEN** 下一步区域 SHALL 使用英文且保持单列可读
- **AND** 主行动入口 SHALL 可通过键盘聚焦并具有明确可访问名称

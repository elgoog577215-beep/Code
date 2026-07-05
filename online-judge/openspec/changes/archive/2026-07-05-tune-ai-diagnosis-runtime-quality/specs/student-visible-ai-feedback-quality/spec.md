## ADDED Requirements

### Requirement: 安全校验应区分答案泄露与合理诊断
系统 SHALL 拦截完整答案、逐行改法、可复制替换表达式和隐藏测试推测；系统 SHALL NOT 仅因学生反馈使用正常算法诊断词而判定答案泄露。

#### Scenario: 合理状态诊断
- **WHEN** 学生可见反馈提示检查状态维度、窗口状态、枚举顺序或边界手推
- **THEN** 安全校验 SHALL 不仅因这些诊断词出现而返回 `SAFETY_RISK`

#### Scenario: 直接替换改法
- **WHEN** 学生可见反馈包含直接替换、逐行改法、完整代码或可复制答案
- **THEN** 安全校验 SHALL 返回 `SAFETY_RISK`

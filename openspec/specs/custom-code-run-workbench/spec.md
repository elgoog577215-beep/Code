# custom-code-run-workbench Specification

## Purpose

为学生题目页定义安全、临时且不产生学习记录的单组自定义输入代码运行能力。
## Requirements
### Requirement: 学生可以使用单组自定义输入运行当前代码

题目页 SHALL 提供独立于正式提交的自定义代码运行工作台，并在当前页面展示标准输出、标准错误、状态和耗时。

#### Scenario: 使用自定义输入运行成功

- **GIVEN** 学生打开支持语言的题目并输入代码与标准输入
- **WHEN** 学生点击“运行代码”
- **THEN** 系统 SHALL 使用题目时间和内存限制执行当前代码
- **AND** 页面 SHALL 在编辑器下方展示标准输出、标准错误、运行状态和耗时
- **AND** 系统 SHALL NOT 创建提交、AI 诊断或学习证据

#### Scenario: 载入公开样例

- **GIVEN** 题目存在至少一个公开样例
- **WHEN** 学生点击“载入首个样例”
- **THEN** 输入框 SHALL 填入第一个公开样例的输入
- **AND** 系统 SHALL NOT 自动比较或判定期望输出
- **AND** 系统 SHALL NOT 读取或展示隐藏测试点

#### Scenario: 编辑内容后结果过期

- **GIVEN** 页面已有一次运行结果
- **WHEN** 学生修改代码、语言或自定义输入
- **THEN** 页面 SHALL 标记现有结果已经过期
- **AND** 学生 SHALL 可以重新运行获得新结果

### Requirement: 自定义运行必须与正式提交隔离

自定义运行 SHALL 使用独立 API 和服务，不得进入正式提交、历史、AI 或教师证据链路。

#### Scenario: 运行前后检查学习数据

- **WHEN** 任意公开或课堂题目完成一次自定义运行
- **THEN** 提交记录数量 SHALL 保持不变
- **AND** 提交历史、AI 诊断、反馈轮询和推荐事件 SHALL NOT 被触发

### Requirement: 自定义运行必须执行访问和资源保护

系统 SHALL 在启动进程前执行课堂授权、请求大小、频率、并发和输出捕获限制。

#### Scenario: 课堂作业身份不匹配

- **GIVEN** 请求带有班级作业 ID
- **WHEN** 学生令牌缺失、跨班或题目不属于该作业
- **THEN** 系统 SHALL 拒绝请求
- **AND** 代码执行器 SHALL NOT 被调用

#### Scenario: 公开匿名运行

- **GIVEN** 请求没有作业 ID
- **WHEN** 匿名学生运行公开题代码
- **THEN** 系统 MAY 执行代码
- **AND** 请求 SHALL 受匿名 IP 频率与并发限制

#### Scenario: 输出超过捕获上限

- **WHEN** 程序输出超过 stdout 或 stderr 捕获上限
- **THEN** 系统 SHALL 保持内存占用有界并继续排空进程流
- **AND** 响应 SHALL 返回已捕获文本和对应截断标记

#### Scenario: 执行环境不可用

- **WHEN** 所选语言的本机工具链或 Docker runner 未就绪
- **THEN** 响应 SHALL 返回 `ENVIRONMENT_UNAVAILABLE`
- **AND** 页面 SHALL 展示可理解的环境提示而不暴露内部路径或堆栈

### Requirement: 运行工作台必须保持课堂界面可用性

运行工作台 SHALL 在桌面、移动、浅色、深色和中英文模式下保持可发现、可操作且无横向溢出。

#### Scenario: 移动端运行代码

- **WHEN** 学生在 390px 宽度设备打开题目页
- **THEN** 输入、运行和输出区域 SHALL 纵向排列
- **AND** 主要控件 SHALL 可键盘与触控操作
- **AND** 页面 SHALL 无横向溢出

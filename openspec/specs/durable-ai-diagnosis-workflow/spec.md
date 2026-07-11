# durable-ai-diagnosis-workflow Specification

## Purpose
TBD - created by archiving change parallelize-complete-ai-diagnosis-workflow. Update Purpose after archive.
## Requirements
### Requirement: 每次正式生成必须建立持久化诊断任务
系统 SHALL 为每次新的正式 AI 生成建立可持久化 Run，并按提交保留递增版本、生成键、总状态、当前阶段、起止时间和最新正式标记；旧 Run 与阶段记录 MUST NOT 被新版本覆盖。

#### Scenario: 首次提交进入完整诊断
- **WHEN** 学生提交完成评测并进入 AI 诊断
- **THEN** 系统 SHALL 创建包含该 submission 与 generation key 的新 Run
- **AND** 系统 SHALL 为其分配该提交下递增的正式版本号

#### Scenario: 同一提交重新生成
- **WHEN** 用户对已有正式版本的提交发起新的全量诊断
- **THEN** 系统 SHALL 创建新的 Run 和版本号
- **AND** 系统 MUST 保留旧 Run、阶段输出和旧反馈修订

### Requirement: 完整链路必须只有一个核心诊断 Agent
系统 MUST 只使用一个核心诊断阶段读取完整题目、完整代码、判题事实、失败样例、学生学习历史和教师修正，并产出本次 Run 唯一的规范问题清单；后续阶段 MUST NOT 启动新的诊断 Agent 投票或改写问题集合。

#### Scenario: 核心诊断成功
- **WHEN** 核心诊断返回多个合法且有证据的问题
- **THEN** 系统 SHALL 持久化全部规范 issues 及其 issue ID、证据和置信度
- **AND** 标准库挂接、学生输出和教师输出 SHALL 引用同一问题清单

#### Scenario: 核心诊断失败
- **WHEN** 核心诊断返回空响应、非法证据或终态模型失败
- **THEN** 系统 SHALL 将 Run 标记为明确失败或待重试
- **AND** 系统 MUST NOT 使用规则模板或第二个诊断 Agent 伪造成功问题清单

### Requirement: 不同问题必须并行执行完整标准库挂接
系统 SHALL 在核心问题清单确定后为每个问题建立独立挂接阶段，并使用受控并发执行不同问题；每个问题内部 SHALL 保留完成知识树逐层导航所需的顺序模型调用，系统 MUST NOT 以固定问题数或 AI 调用总数跳过有效问题。

#### Scenario: 一次诊断包含多个问题
- **WHEN** 核心诊断返回两个或以上有效 issue
- **THEN** 系统 SHALL 为全部 issue 创建挂接阶段并允许并行运行
- **AND** 聚合结果 SHALL 按原 issue 顺序稳定返回

#### Scenario: 单个问题挂接失败
- **WHEN** 一个 issue 挂接超时、无匹配或输出非法
- **THEN** 系统 SHALL 保存该 issue 的 `NO_MATCH`、`PARTIAL`、`LIBRARY_EMPTY` 或 `ATTACHMENT_FAILED` 状态
- **AND** 其他 issue 的挂接 MUST 继续执行且成功结果 MUST 被复用

### Requirement: 学生反馈和教师洞察必须并行且共享事实
系统 SHALL 在问题清单与挂接结果确定后并行生成学生反馈和教师洞察；两个输出 MUST 引用同一批 issue ID 与证据，教师洞察不得新增或删除核心问题。

#### Scenario: 两个输出分支均成功
- **WHEN** 学生输出和教师输出均通过结构、证据与安全校验
- **THEN** 系统 SHALL 保存两个阶段输出并生成唯一正式分析版本
- **AND** 学生反馈与教师洞察中同一 issue SHALL 使用相同 issue ID

#### Scenario: 教师输出分支失败
- **WHEN** 学生反馈成功但教师洞察失败
- **THEN** 系统 SHALL 保留可查看的学生反馈并将教师分支标记为待重试或终态失败
- **AND** 系统 MUST NOT 使用模板教师结论伪装该阶段成功

### Requirement: 阶段执行必须可独立重试和重启续跑
系统 SHALL 持久化每个阶段的状态、尝试次数、输入指纹、输出、模型、prompt 版本、耗时和失败原因；恢复时 SHALL 直接复用成功阶段，只重新执行未完成或可重试失败阶段。

#### Scenario: 某个 issue 挂接重试
- **WHEN** 一个 issue 的挂接阶段失败而核心诊断和其他 issue 已成功
- **THEN** 系统 SHALL 只重试该 issue 的挂接阶段及其依赖的未完成下游阶段
- **AND** 系统 MUST NOT 重新调用核心诊断或成功 issue 的挂接

#### Scenario: 服务重启恢复任务
- **WHEN** 服务启动时发现处于进行中或可重试失败状态的 Run
- **THEN** 系统 SHALL 将遗留运行中阶段恢复为可重试并重新入队
- **AND** 已成功阶段 SHALL 从数据库输出恢复而不是重新调用模型

### Requirement: 正式诊断保存后的派生投影必须并行且幂等
系统 SHALL 在正式分析和反馈版本保存后，并行执行学生问题事实/生命周期、教师统计、标准库成长候选和 AI 运行质量投影；每个投影 MUST 幂等，非关键投影失败 MUST NOT 回滚正式诊断。

#### Scenario: 成长候选投影失败
- **WHEN** 正式诊断已经保存但标准库成长候选投影失败
- **THEN** 学生反馈和正式分析 SHALL 保持成功可读
- **AND** 系统 SHALL 记录该投影失败并允许独立重试

#### Scenario: 事实投影重复执行
- **WHEN** 恢复流程再次执行已部分完成的诊断事实投影
- **THEN** 系统 SHALL 根据稳定事实键避免重复记录
- **AND** 教师统计 MUST NOT 被重复放大

### Requirement: Run 完成状态必须反映完整路径
系统 SHALL 仅在全部必需 AI 阶段通过校验且正式分析、学生反馈版本已经保存后标记 Run 为 `COMPLETED`；任何阶段失败都必须有可观察终态，不得静默跳过。

#### Scenario: 完整路径完成
- **WHEN** 核心诊断、所有问题挂接、学生输出、教师输出和正式保存均成功
- **THEN** Run SHALL 标记为 `COMPLETED`
- **AND** 系统 SHALL 保留各阶段实际调用和耗时记录

#### Scenario: 非关键投影仍在重试
- **WHEN** 正式结果已保存且某个派生投影正在重试
- **THEN** 学生 SHALL 能读取已保存的完整反馈
- **AND** Run SHALL 明确区分正式结果完成与派生投影未完成状态

### Requirement: 学生进度只能暴露学习化阶段
学生反馈查询 SHALL 返回稳定的学习阶段、已完成数量、总数量、重试状态和更新时间；学生可见字段 MUST NOT 暴露 provider、API key、token、HTTP 状态、模型堆栈或内部失败代码。

#### Scenario: 多问题正在挂接
- **WHEN** 核心诊断已经完成且多个问题正在匹配标准库
- **THEN** 页面 SHALL 显示“正在匹配知识路径”及完成进度
- **AND** 页面 MUST NOT 显示底层模型调用轮次或供应商错误

#### Scenario: 阶段正在重试
- **WHEN** 某个可恢复阶段失败后重新入队
- **THEN** 页面 SHALL 显示学习化的“正在重试”状态并保留评测结果
- **AND** 运维细节 SHALL 只保存在后端阶段记录中

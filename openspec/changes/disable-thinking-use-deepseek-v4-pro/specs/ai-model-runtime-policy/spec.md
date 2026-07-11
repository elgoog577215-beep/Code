## ADDED Requirements

### Requirement: ModelScope请求必须统一关闭推理模式
系统 SHALL 对所有通过 ModelScope 兼容网关发出的 Chat Completions 请求显式设置 `enable_thinking=false`，并将结构化正文作为唯一可消费输出；系统 MUST NOT 依赖或展示 `reasoning_content`。

#### Scenario: V4 Pro生成结构化诊断
- **WHEN** 核心诊断、问题挂接、学生输出或教师输出调用 V4 Pro
- **THEN** 请求体 SHALL 包含 `enable_thinking=false`
- **AND** 阶段 SHALL 仅使用完整正文进入结构和证据校验

#### Scenario: 非ModelScope供应商
- **WHEN** AI 请求发送到非 ModelScope 兼容地址
- **THEN** 系统 MUST NOT 注入供应商专属的 `enable_thinking` 字段

### Requirement: V4 Pro必须作为默认主模型
本地、示例配置和学校部署 SHALL 使用 `deepseek-ai/DeepSeek-V4-Pro` 作为默认主模型；备用模型只在可分类的供应商失败时接续同一完整诊断路径，系统 MUST NOT 因模型切换减少诊断阶段或问题数量。

#### Scenario: 正常生成
- **WHEN** V4 Pro 请求成功且输出通过校验
- **THEN** 全部阶段 SHALL 使用 V4 Pro 完成本次诊断
- **AND** Run 阶段记录 SHALL 保存实际模型与耗时

#### Scenario: 主模型配额失败
- **WHEN** V4 Pro 返回可切换的配额或供应商错误
- **THEN** 系统 SHALL 按配置模型池继续同一完整阶段
- **AND** 已成功阶段 MUST 被复用而不是重新执行

### Requirement: 主模型切换必须通过完整链路门禁
系统 MUST 在切换生产主模型前验证非推理流式输出、结构化 JSON 完整性、必需阶段状态、真实困难样本和服务器 smoke；短文本成功 MUST NOT 单独作为上线依据。

#### Scenario: 真实困难样本通过
- **WHEN** V4 Pro 非推理模式完成真实困难题诊断
- **THEN** 核心诊断、全部问题挂接、学生输出和教师输出 SHALL 全部成功
- **AND** 学生反馈 SHALL 包含完整修正与提升建议

#### Scenario: 任一必需阶段失败
- **WHEN** 真实样本出现截断、非法 JSON、配额失败或必需阶段未完成
- **THEN** 系统 MUST NOT 将 V4 Pro 生产切换声明为完成

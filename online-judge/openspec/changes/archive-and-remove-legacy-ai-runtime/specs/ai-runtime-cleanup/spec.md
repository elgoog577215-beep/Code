## ADDED Requirements

### Requirement: 旧 AI 链路必须归档到程序外目录
系统 SHALL 将旧 AI 诊断链路资产归档到 `legacy-archive/ai-runtime-legacy/`，并确保该目录与程序编译、运行、测试解耦。

#### Scenario: 归档目录存在且不可编译
- **WHEN** 清理完成
- **THEN** `legacy-archive/ai-runtime-legacy/README.md`、`inventory.md`、`relationship-map.md` MUST 存在
- **AND** 归档目录 MUST NOT 包含可编译 `.java` 源文件

#### Scenario: 归档不参与运行
- **WHEN** 应用启动、编译或运行测试
- **THEN** Maven、Spring、资源加载和测试 MUST NOT 引用 `legacy-archive/ai-runtime-legacy/`

### Requirement: 主代码不得保留旧 AI 运行入口
系统 SHALL 从主代码删除 legacy long prompt、staged runtime、旧 single-call runtime 和旧 prompt version fallback。

#### Scenario: 旧 prompt 不可选
- **WHEN** 构建正式模型运行计划
- **THEN** 系统 MUST NOT 注册或选择 `diagnosis-and-teaching-v1/v2/v3/v4-lite`
- **AND** 系统 MUST NOT 注册或选择 `diagnosis-judge-v1/v2` 或 `teaching-hint-v1`

#### Scenario: 不再回退旧 prompt
- **WHEN** prompt version 为空或无效
- **THEN** 系统 MUST 使用正式 `diagnosis-and-advice-v1` 或显式失败
- **AND** 系统 MUST NOT 静默回退到 `diagnosis-and-teaching-v3`

### Requirement: 正式诊断链路唯一
系统 SHALL 只保留 search-location + advice-generation 作为提交后 AI 诊断正式链路。

#### Scenario: 默认诊断路径
- **WHEN** 外部 AI 可用且提交分析进入模型增强
- **THEN** 系统 MUST 先尝试 `search-location-v1`
- **AND** 完整诊断阶段 MUST 使用 `diagnosis-and-advice-v1`

#### Scenario: 搜索定位失败
- **WHEN** 搜索定位失败或回退
- **THEN** 系统 MAY 使用完整 `StandardLibraryPack`
- **AND** 完整诊断阶段 MUST 仍使用 `diagnosis-and-advice-v1`
- **AND** 系统 MUST NOT 调用旧 AI 链路

### Requirement: 新运行链路不得产生旧阶段名
系统 SHALL 防止新运行链路产生旧 prompt/stage 标识。

#### Scenario: 新阶段可观测
- **WHEN** 新 AI 链路记录 `AiInvocation`
- **THEN** failure stage MUST be `SEARCH_LOCATION`、`DIAGNOSIS_AND_ADVICE`、`EXTERNAL_MODEL` 或空值之一
- **AND** MUST NOT be `DIAGNOSIS_JUDGE`、`TEACHING_HINT` 或 `DIAGNOSIS_AND_TEACHING`

### Requirement: 旧模式配置必须移除
系统 SHALL 移除旧 runtime 模式配置，避免生产环境选择旧链路。

#### Scenario: 配置清理
- **WHEN** 查看主配置和 `.env.example`
- **THEN** MUST NOT expose `AI_EXTERNAL_RUNTIME_MODE`
- **AND** MUST NOT expose `AI_EXTERNAL_SINGLE_CALL_PROMPT_VERSION`

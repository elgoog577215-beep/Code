## 1. 规格与边界

- [x] 1.1 梳理当前本地召回链路和目标 AI 导航链路
- [x] 1.2 写入链路替换说明、模块命运表、输出形态和验收清单
- [x] 1.3 创建 OpenSpec 变更并明确移除默认本地召回

## 2. 标准库导航 API

- [x] 2.1 在标准库服务中提供一级目录读取接口
- [x] 2.2 提供知识节点子树分页展开接口
- [x] 2.3 提供知识点下能力点、易错点、提升点展开接口
- [x] 2.4 保证导航 API 复用现有规范化标准库，不新增高中/竞赛平行库

## 3. AI Prompt 与 DTO

- [x] 3.1 新增 `free-diagnosis-v1` prompt 和结构化输出 DTO
- [x] 3.2 新增 `standard-library-navigation-v1` prompt、导航轮次 DTO 和最终导航结果 DTO
- [x] 3.3 新增或升级 `diagnosis-report-v3` prompt，使其读取初步诊断和导航结果
- [x] 3.4 更新输出 normalizer/validator，校验导航锚点、证据引用和库外缺口

## 4. 后端编排替换

- [x] 4.1 调整 `ExternalModelAgentRuntime.prepare(...)`，不再提前调用 `StandardLibraryPackBuilder.build(brief)`
- [x] 4.2 调整学生实时诊断默认链路，移除 `applyLocalSearchLocationOnly(...)`
- [x] 4.3 调整教师/完整诊断链路，默认不再调用 `applySearchLocationIfAvailable(...)` 的本地召回路径
- [x] 4.4 用 AI 导航结果构造最终 `StandardLibraryPack`
- [x] 4.5 将默认 trace 从 `LOCAL_RECALL` 改为三阶段 AI 状态

## 5. 旧链路退出

- [x] 5.1 让 `SearchLocationRetrievalService` 不再参与学生实时诊断主链路
- [x] 5.2 退役 `search-location-v1` 在生产主链路中的使用
- [x] 5.3 清理或隔离 `SearchLocationPackSelector` 的默认调用入口
- [x] 5.4 更新配置说明，避免 `AI_SEARCH_LOCATION_ENABLED` 被理解成默认主链路开关

## 6. 测试与验证

- [x] 6.1 增加默认链路不调用本地召回的单元测试
- [x] 6.2 增加导航轮次、分支数和最终条目数上限测试
- [x] 6.3 增加导航失败不回退本地召回的测试
- [x] 6.4 增加最终学生报告结构保持不变的测试
- [x] 6.5 运行相关后端测试、OpenSpec validate 和必要 smoke

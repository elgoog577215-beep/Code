## 1. OpenSpec 与配置

- [x] 1.1 完成 proposal/design/spec/tasks 并通过 OpenSpec 校验
- [x] 1.2 新增搜索定位和 embedding 配置项，并更新 `.env.example` 与 `application.yml`
- [x] 1.3 将 Docker Compose Postgres 镜像调整为 pgvector 兼容镜像，并保留环境变量覆盖

## 2. 数据与标准库候选

- [x] 2.1 新增标准库 embedding 实体、仓储和内容 hash/过期状态模型
- [x] 2.2 扩展标准库服务，提供搜索定位候选读取和编辑后 embedding 过期标记
- [x] 2.3 扩展 `StandardLibraryPack` 兼容字段：知识锚点、能力点、易错点、搜索定位摘要

## 3. 本地混合召回

- [x] 3.1 实现文本/结构召回，对题目、代码、判题结果、rule signals 和标准库字段进行加权打分
- [x] 3.2 实现 embedding 客户端和向量召回接口，支持失败降级
- [x] 3.3 实现混合重排和候选包输出，默认限制在 80-120 个候选

## 4. LLM 搜索定位

- [x] 4.1 新增 `search-location-v1` prompt 和定位输入/输出 DTO
- [x] 4.2 新增定位输出 normalizer/validator，校验 ID、层级、证据和置信度
- [x] 4.3 根据定位结果构造精选 `StandardLibraryPack`，失败时回退旧 pack

## 5. 诊断链路接入

- [x] 5.1 将搜索定位接入 `ExternalModelAgentRuntime`/`AiReportService`，默认启用两次模型调用
- [x] 5.2 扩展 `AiInvocation` 和 diagnostic trace，记录定位状态、候选数量、精选数量、降级原因和 embedding 状态
- [x] 5.3 关闭开关时完全走旧链路，定位失败时继续完整诊断

## 6. 测试与验证

- [x] 6.1 增加文本召回、混合重排和向量降级单元测试
- [x] 6.2 增加 LLM 定位输出校验和精选 pack 构造测试
- [x] 6.3 增加链路测试：两次调用、定位失败回退、关闭开关旧链路
- [x] 6.4 运行相关后端测试和 OpenSpec validate

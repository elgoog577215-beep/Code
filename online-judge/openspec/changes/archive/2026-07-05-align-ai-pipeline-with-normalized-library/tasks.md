## 1. 结构化标准库候选包

- [x] 1.1 扩展 `StandardLibraryPack`，加入知识节点分组和能力点分组结构视图
- [x] 1.2 扩展 `SearchLocationCandidate`，加入主知识点、父能力、结构路径和邻域候选字段

## 2. 召回与选择链路

- [x] 2.1 调整 `SearchLocationRetrievalService`，用完整候选集合补全同能力易错和相关提升邻域
- [x] 2.2 调整 `SearchLocationPackSelector`，选中候选时补齐父能力、同层易错、相关提升并生成结构视图

## 3. Runtime 与 Prompt

- [x] 3.1 调整 `ExternalModelAgentRuntime` compact 逻辑，保留结构视图但限制体积
- [x] 3.2 调整搜索定位和诊断 prompt，明确标准库主结构和证据优先规则

## 4. 测试与验证

- [x] 4.1 增加搜索召回邻域字段测试
- [x] 4.2 增加选择器结构视图和父子补全测试
- [x] 4.3 增加 prompt/compact 结构视图测试
- [x] 4.4 运行 OpenSpec 校验和 AI 链路相关后端测试

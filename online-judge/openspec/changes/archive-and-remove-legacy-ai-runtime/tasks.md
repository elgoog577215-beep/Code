## 1. OpenSpec 与归档结构

- [x] 1.1 完成 proposal/design/spec/tasks 并通过 OpenSpec 校验
- [x] 1.2 建立 `legacy-archive/ai-runtime-legacy/` 目录结构

## 2. 旧链路归档

- [x] 2.1 归档旧运行入口和源码快照为不可编译文本
- [x] 2.2 归档旧 prompt、旧 DTO 契约和旧测试索引
- [x] 2.3 编写 `inventory.md`、`relationship-map.md`、`removal-notes.md`

## 3. 主代码清理

- [x] 3.1 删除 legacy long prompt 分支
- [x] 3.2 删除 staged runtime 分支
- [x] 3.3 删除旧 single-call runtime 和 `CombinedOutput` 路径
- [x] 3.4 删除旧 prompt 常量、模板注册和旧 prompt version fallback
- [x] 3.5 删除旧 runtime 配置项

## 4. 新链路保持可运行

- [x] 4.1 保留 search-location + advice-generation 正式链路
- [x] 4.2 移除 advice 对旧 DTO 的运行依赖，直接生成现有响应和学生反馈
- [x] 4.3 确保 AI 不可用或 advice 失败时只走规则兜底，不调用旧 AI 链路

## 5. 测试与验证

- [x] 5.1 更新或删除依赖旧 prompt/stage 的测试
- [x] 5.2 增加旧标识清洁检查
- [x] 5.3 运行 OpenSpec validate、后端编译/测试和旧标识扫描

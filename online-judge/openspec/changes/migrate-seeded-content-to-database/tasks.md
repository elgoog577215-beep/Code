## 1. 迁移准备与盘点

- [x] 1.1 盘点所有运行时 seed 入口、内容型 seed 文件和依赖它们的测试，输出迁移清单。
- [x] 1.2 明确正式数据库表映射：公共题库、知识树、标准库规范表和兼容快照。
- [x] 1.3 增加数据库内容备份和迁移前检查命令，确认目标库不是空写或误连本地库。

## 2. 一次性内容迁移

- [ ] 2.1 提供一次性迁移工具，把现有题库 seed 内容写入正式题库表，并保持题目标题/code 幂等。
- [ ] 2.2 提供一次性迁移工具，把现有知识树 seed 内容写入 `informatics_knowledge_nodes`。
- [ ] 2.3 提供一次性迁移工具，把现有标准库 seed 内容写入规范化能力点、易错点、提升点和兼容快照。
- [ ] 2.4 提供迁移后验证查询，覆盖题库数量、知识树路径、标准库三类条目、代表性 `if 条件` 路径和 AI 导航读取。

## 3. 移除运行时 seed 入口

- [x] 3.1 禁用或删除 `PublicProblemSeeder`、`InformaticsKnowledgeSeeder`、`AiStandardLibrarySeeder`、`AiStandardLibraryNormalizedSeeder` 的生产启动写入行为。
- [x] 3.2 调整 readiness / 健康检查，在正式内容缺失时报告数据库内容缺失，而不是自动补 seed。
- [ ] 3.3 删除或隔离 `public-problem-seeds` 资源读取路径，确保运行时不再从资源文件读取正式题库内容。

## 4. 删除内容型 seed 代码

- [ ] 4.1 删除公共题库内容型 seed 类和资源文件，保留必要的测试 fixture 到 `src/test`。
- [ ] 4.2 删除知识树内容型 seed 类，改为测试数据库 fixture 或正式数据库查询。
- [ ] 4.3 删除 AI 标准库内容型 seed catalog 和 V6-V16 扩库 seed 类，迁移旧测试到数据库行为测试。
- [ ] 4.4 保留或重写旧兜底识别逻辑，使它不依赖 seed catalog 枚举。

## 5. 验证、部署和收束

- [ ] 5.1 运行后端测试、数据库迁移 dry-run、脚本语法检查和 OpenSpec validate。
- [ ] 5.2 在本地或测试 Postgres 上执行迁移演练，确认重启不会重复写入内容。
- [ ] 5.3 更新项目记忆，说明正式内容数据库优先、seed 已退出主链路。
- [ ] 5.4 完成后归档 OpenSpec 变更并再次 `openspec validate --all`。

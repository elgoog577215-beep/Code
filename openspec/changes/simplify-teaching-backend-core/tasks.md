## 1. OpenSpec 与规则

- [x] 1.1 创建 `simplify-teaching-backend-core` proposal、design、spec、tasks。
- [x] 1.2 运行 OpenSpec strict validation。

## 2. 后端主链路审计

- [x] 2.1 审计 submission API、application、DTO，标记 keep-main、simplify、internal-only、remove-later。
- [x] 2.2 审计 classroom API、application、DTO，标记 keep-main、simplify、internal-only、remove-later。
- [x] 2.3 产出 `backend-core-audit.md`，列出分类理由、依赖风险和下一轮执行顺序。

## 3. 代码化边界

- [x] 3.1 新增轻量后端分类模型，用于表达教学主链路和内部支撑链路。
- [x] 3.2 新增单元测试，锁定分类语义：学生/教师主链路、内部 AI 质量/评测/运行诊断不能混为主链路。
- [x] 3.3 为第一批明显内部用途的教师端能力补充边界说明，保持接口兼容。

## 4. 验证与收束

- [x] 4.1 运行相关后端测试。
- [x] 4.2 如触及前端类型，运行 frontend typecheck。
- [x] 4.3 运行 `git diff --check`。
- [x] 4.4 确认本轮未删除 API、未改数据库、未改变模型调用语义。

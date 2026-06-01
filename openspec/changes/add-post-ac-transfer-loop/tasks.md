## 1. OpenSpec

- [x] 1.1 写入 `add-post-ac-transfer-loop` proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate add-post-ac-transfer-loop --strict`。

## 2. 后端结构化信号

- [x] 2.1 新增 AC 后迁移信号 DTO 字段，保持旧响应兼容。
- [x] 2.2 实现确定性 `PostAcTransferAnalyzer`，推导复盘阶段、证据、目标能力和下一步动作。
- [x] 2.3 在学生轨迹和任务轨迹中接入 `postAcTransferSignal`。
- [x] 2.4 在教师作业概览中输出待迁移学生数、迁移摘要和学生行迁移信号。

## 3. 推荐与质量闭环

- [x] 3.1 让推荐服务消费 AC 后迁移信号，生成复盘或迁移推荐。
- [x] 3.2 在 AI 质量概览中新增 `POST_AC_TRANSFER_LOOP` 维度和改进优先级。
- [x] 3.3 确保现有推荐、Coach 和教师介入字段不回退。

## 4. 前端展示

- [x] 4.1 更新共享 API 类型和格式化标签。
- [x] 4.2 学生端展示一个 AC 后复盘/迁移动作，避免覆盖题目列表主流程。
- [x] 4.3 教师端展示待迁移摘要和学生行迁移信号，AI 质量详情保持折叠。

## 5. 验证

- [x] 5.1 增加后端单元测试覆盖缺复盘、已有证据、迁移推荐和质量维度。
- [x] 5.2 运行相关 Maven 测试。
- [x] 5.3 运行前端 `tsc --noEmit`。
- [x] 5.4 按需运行构建或 browser smoke，确认 UI 没有溢出或选择器回归。

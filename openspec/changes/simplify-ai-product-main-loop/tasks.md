## 1. OpenSpec

- [x] 1.1 创建 `simplify-ai-product-main-loop` change 文档，写清产品北极星、三层架构和验收标准。
- [x] 1.2 新增 `ai-product-main-loop` spec，约束 `studentFeedback` 主展示、六步主链路和复杂能力下沉。

## 2. 架构文档与索引

- [x] 2.1 新增 AI 产品主链路架构文档，明确学生主链路、教师洞察层、研发评测层。
- [x] 2.2 新增 OpenSpec change 分层索引，覆盖当前 active changes。

## 3. 代码与测试收束

- [x] 3.1 增加 `StudentFeedbackAssembler` 契约测试，覆盖 WA 公开失败、隐藏失败、AC 和低证据场景。
- [x] 3.2 检查学生端反馈主区域，确保内部模型状态和评测指标不作为第一视觉层。

## 4. 验证

- [x] 4.1 运行 `openspec validate simplify-ai-product-main-loop --strict`。
- [x] 4.2 运行相关后端测试。
- [x] 4.3 运行前端 `tsc --noEmit`。
- [x] 4.4 运行 `git diff --check` 和精确 secret scan。

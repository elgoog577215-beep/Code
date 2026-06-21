## Context

旧 AI 运行链路来自多轮迭代，曾经承担单次诊断、分阶段判断、教学提示和旧学生反馈生成等职责。现在正式目标已经切换为两阶段教育 Agent：

```text
搜索定位 Agent -> 建议生成 Agent
```

旧链路继续留在主代码里会造成三个问题：

- 运行路径不唯一，排查线上行为时难以判断实际使用哪个 prompt。
- 测试语义混杂，旧 stage 名和旧 prompt 版本持续污染新链路质量判断。
- 配置可能静默回退旧链路，和“AI 不可用就明确不可用”的产品边界冲突。

## Goals / Non-Goals

**Goals:**

- 建立完整旧链路档案目录，并确保档案与编译、运行、测试解耦。
- 主程序只保留 search-location + diagnosis-and-advice-v1 正式链路。
- 删除旧 prompt、旧 staged runtime、旧 single-call runtime、旧 long prompt 和旧运行配置。
- 新运行链路不再产生旧 stage 名。
- 保持新链路可运行，并保持现有响应字段兼容。

**Non-Goals:**

- 不把旧 prompt 经验迁移到 `diagnosis-and-advice-v1`。
- 不增强标准库、搜索召回或 advice 质量。
- 不做数据库历史数据迁移。
- 不整体删除 `StudentAiFeedback` 或 `CoachAgent` 旁路功能。

## Decisions

1. **旧资产放入根目录 `legacy-archive/`。**
   - 原因：根目录清晰可见，且天然不被 Spring 扫描。
   - 约束：不得包含可编译 `.java` 文件，不得被 Maven、测试或资源配置引用。

2. **主代码不保留旧 prompt 选择开关。**
   - 原因：正式链路已经固定为 search-location + advice-generation。
   - 方案：删除 `external-runtime-mode` 和 `external-single-call-prompt-version` 配置，旧 prompt version 不再可选。

3. **先归档，后删除。**
   - 原因：保留可追溯性，避免删除后无法复盘旧逻辑。
   - 方案：归档快照使用 `.md` / `.txt` / `.java.txt`。

4. **最小必要重构允许存在。**
   - 原因：当前 advice 成功后仍桥接旧 `DiagnosisJudgeOutput` / `TeachingHintOutput`。
   - 方案：只为删除旧运行依赖而做直连映射，不做质量增强和旧经验迁移。

## Risks / Trade-offs

- [Risk] 旧测试删除过多导致覆盖下降 -> 保留新链路核心测试，旧测试只作为档案索引记录。
- [Risk] 旧 DTO 仍被新映射间接依赖 -> 用清洁扫描约束主代码不再出现旧 DTO 名。
- [Risk] classroom/eval 历史数据测试依赖旧 stage 名 -> 改为新 stage 测试或历史兼容命名，不让新运行链路产生旧值。
- [Trade-off] 本阶段不吸收旧 prompt 优点 -> 符合用户要求，后续如需提取再从 archive 取材。

## Migration Plan

- 不迁移数据库数据。
- 不迁移旧 prompt 到新 prompt。
- 不迁移旧 fixture 到新 fixture。
- 只迁移位置：旧运行资产从主代码上下文迁入 `legacy-archive`。

## 1. OpenSpec

- [x] 1.1 写入 `add-teacher-calibration-loop` proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate add-teacher-calibration-loop --strict`。

## 2. 后端校准结构

- [x] 2.1 在诊断响应和证据包中新增可选教师校准结构，保持旧 JSON 兼容。
- [x] 2.2 从现有教师校正记录构建校准模式、校准摘要和 evidenceRefs。
- [x] 2.3 在 `ModelDiagnosisBrief` 中接入教师校准摘要、候选信号、允许标签和 evidenceRefs。

## 3. 诊断反哺逻辑

- [x] 3.1 在诊断 agent 中应用教师校准支持状态，补充证据引用。
- [x] 3.2 在诊断 agent 中识别教师校准冲突，降低置信并生成教师复核提示。
- [x] 3.3 确保教师校准不覆盖当前提交事实、规则信号和已有提示安全逻辑。

## 4. 质量闭环

- [x] 4.1 在 `AiQualityMetrics` 中统计教师校准信号、应用数和冲突数。
- [x] 4.2 在 AI 质量概览中新增 `TEACHER_CALIBRATION_LOOP` 维度和改进建议。

## 5. 验证

- [x] 5.1 增加后端测试覆盖校准信号生成、brief 接入、诊断支持/冲突和质量维度。
- [x] 5.2 运行 OpenSpec strict validate。
- [x] 5.3 运行相关 Maven 测试和后端编译。

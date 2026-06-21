# 旧链路失败模式

本文件只记录历史经验，不在本阶段迁移到新链路。

## 旧失败模式

- `DIAGNOSIS_AND_TEACHING:OUTPUT_TRUNCATED`：旧 single-call 输出过长或流式截断。
- `DIAGNOSIS_AND_TEACHING:INVALID_TAG`：旧输出 tag 不在标准库范围内。
- `DIAGNOSIS_AND_TEACHING:SAFETY_RISK`：旧输出包含答案泄露风险。
- `DIAGNOSIS_JUDGE:*`：staged runtime 第一阶段失败。
- `TEACHING_HINT:*`：staged runtime 第二阶段失败。
- `legacy-long-prompt`：无 evidence/runtime 时进入旧长 prompt。

## 当前处理

新运行链路只允许：

- `SEARCH_LOCATION`
- `DIAGNOSIS_AND_ADVICE`
- `EXTERNAL_MODEL`

旧 stage 名只作为历史档案或历史数据展示存在。

# RuleSignalAnalyzer 归档说明

## 归档原因

`RuleSignalAnalyzer` 是早期没有稳定外部大模型时的本地规则信号器。它会把代码和判题结果粗略压成 `candidateSignals`、粗标签、细标签和证据引用。

现在学生 AI 主链路已经改为：

```text
题目 + 完整带行号代码 + 判题事实 + 树形标准库包
-> 外部大模型自由诊断
-> 后端只做结构校验和展示映射
```

继续把本地规则信号传给模型，会让模型被粗糙启发式牵引，削弱它自己阅读代码、定位多重问题、判断库外问题的能力。

## 已吸收价值

- 常见错误命名已经沉淀进标准库和诊断 taxonomy。
- 判题事实仍保留在 `judgeFacts`，包括 verdict、公开失败样例、隐藏失败边界。
- 代码证据改由模型从 `submission.sourceCodeWithLineNumbers` 直接定位，再由后端映射为可展示片段。
- 学习记忆和教师校准仍保留为摘要与可选标签，但不再伪装成当前提交的代码证据。

## 当前状态

正式学生 AI prompt、学生快反馈上下文、本地标准库召回加权都不再使用 `candidateSignals` 或 `RuleSignalAnalyzer` 输出。主代码中的 `RuleSignalAnalyzer`、`RuleSignalResult` 和 `ModelDiagnosisBrief.candidateSignals` 已删除；历史实现只保留在本归档目录的源码快照里。

源码快照：

- `source-snapshots/RuleSignalAnalyzer-legacy-local-signals.java.txt`
- `tests/RuleSignalAnalyzerTest-legacy-local-signals.java.txt`

状态：`ARCHIVED_ONLY`

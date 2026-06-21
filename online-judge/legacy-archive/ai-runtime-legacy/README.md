# AI Runtime Legacy Archive

本目录保存旧 AI 诊断运行链路的历史档案。它只用于人工查阅和后续复盘，不参与程序编译、运行、测试或资源加载。

## 边界

- 本目录不在 `src/`、`resources/`、`test/` 下。
- 本目录不得包含可编译 `.java` 源文件；源码快照使用 `.java.txt`。
- 主程序不得 import、读取或配置引用本目录。
- 本阶段只归档旧链路，不把旧 prompt、旧规则或旧 fixture 迁移进新链路。

## 归档内容

- `inventory.md`：旧资产清单和当前状态。
- `relationship-map.md`：旧运行入口、prompt、DTO、测试之间的关系。
- `source-snapshots/`：旧运行代码片段快照。
- `prompts/`：旧 prompt 文本。
- `contracts/`：旧输出契约。
- `tests/`：旧测试索引。
- `notes/`：失败模式和删除说明。

## 正式链路

主程序正式 AI 诊断链路固定为：

```text
DiagnosisEvidencePackage
-> ModelDiagnosisBrief
-> SearchLocationAgent
-> selected StandardLibraryPack
-> diagnosis-and-advice-v1
-> AdviceGenerationOutputValidator
-> StudentFeedback / StudentFeedbackView
```

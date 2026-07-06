# 设计

## 核心判断

这次不需要新链路。真正的断点是：标准库已经有主关系，但进入模型前的候选包和 prompt 没把这层语义讲清楚。

## 最小方案

```text
本地召回树形包
-> compact 时保留主知识点与相关知识点
-> 单诊断 Agent 读 knowledgeGroups
-> 按 primaryKnowledgeNodeCode 形成学生知识路径
-> relatedKnowledgeNodeCodes 只辅助区分，不单独当错因
```

## 取舍

- 只在运行包和 prompt 上补主路径规则，不新增后端推理器。
- 不强迫模型每条都填 ID；精确命中才填，半命中或库外发现保持可解释。
- 学生可见文字仍优先自然表达；知识路径和证据用于辅助展示、教师追踪和评测。

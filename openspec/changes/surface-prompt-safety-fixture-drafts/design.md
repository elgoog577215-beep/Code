## Context

`ClassroomService.exportDiagnosisEvalFixtureDraft` 已经返回三类草稿：诊断校正、课堂介入和提示安全。前端 `DiagnosisEvalFixtureDraft` 类型也包含 `safetyFixtureCount` 与 `safetyFixtures`，但教师页的预览只统计并序列化前两类。

本变更只补齐教师端可见性。它不改变安全判定、fixture 生成、后端 API 或 eval 资源格式。

## Goals / Non-Goals

**Goals:**

- 让教师在 Fixture 草稿预览入口直接看到提示安全草稿数量。
- 展示安全草稿的最高风险、待审查提交和主要 blocked reasons。
- 将 `safetyFixtures` 放入现有 JSON 预览，便于教师复制、审查和沉淀。
- 保持布局克制，符合教师工作台高密度、可扫描的使用场景。

**Non-Goals:**

- 不新增导出接口或后端 DTO 字段。
- 不新增 fixture 编辑器、下载按钮或持久化流程。
- 不改变现有诊断草稿和课堂介入草稿的数据结构。

## Decisions

### 复用现有预览容器

继续使用 `teacher-fixture-draft-preview` 的 details 区域，而不是新增一个独立面板。这样教师仍在“导出 eval 草稿”的同一工作流里看到安全草稿，不需要理解新的入口。

### 添加简短安全概览而不是完整卡片列表

安全草稿本身包含大量 JSON 字段。界面上只展示最高风险、草稿数量、提交号和 blocked reasons，完整内容仍在 JSON 预览中。这能避免教师页变得拥挤，同时让高风险样本不会被隐藏。

### 状态徽标计入安全草稿

Fixture 预览 summary 的数量从“诊断 + 课堂介入”扩展为“诊断 + 课堂介入 + 提示安全”。当只有安全草稿时，入口也应呈现信息态，而不是中性态。

## Risks / Trade-offs

- 安全草稿只以预览形式展示，仍需要人工把样本整理进静态资源。→ 保持本轮范围小，后续可再做一键下载或审查流。
- blocked reasons 可能较长。→ 展示时限制前几条，完整内容保留在 JSON。
- 现有教师页较大，局部逻辑可能继续膨胀。→ 本轮只新增小型 helper，不引入新的全局状态。

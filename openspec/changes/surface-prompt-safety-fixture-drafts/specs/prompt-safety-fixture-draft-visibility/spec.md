## ADDED Requirements

### Requirement: 教师端必须展示提示安全 fixture 草稿计数

教师端 Fixture 草稿预览 SHALL 将提示安全草稿纳入入口计数和统计区，使只有安全草稿时教师也能发现可审查样本。

#### Scenario: 草稿响应包含提示安全草稿

- **WHEN** 教师端收到 `DiagnosisEvalFixtureDraft.safetyFixtureCount` 大于 0 的响应
- **THEN** Fixture 草稿预览入口 SHALL 显示包含提示安全草稿的总览数量
- **AND** 统计区 SHALL 显示提示安全草稿数量
- **AND** 入口状态 SHALL 使用非中性的可见状态

#### Scenario: 草稿响应没有提示安全草稿

- **WHEN** 教师端收到的 `safetyFixtureCount` 为空或为 0
- **THEN** Fixture 草稿预览 SHALL 保持现有诊断草稿和课堂介入草稿展示兼容

### Requirement: 教师端必须提供安全草稿审查摘要

教师端 Fixture 草稿预览 SHALL 为提示安全草稿展示可扫描摘要，帮助教师判断是否需要沉淀为 eval 资源。

#### Scenario: 存在中高风险安全草稿

- **WHEN** `safetyFixtures` 中存在风险等级为 `MEDIUM` 或 `HIGH` 的草稿
- **THEN** 预览 SHALL 展示最高风险级别
- **AND** 预览 SHALL 展示至少一个待审查提交标识
- **AND** 预览 SHALL 展示主要 blocked reasons 或 expected safety action

### Requirement: 教师端 JSON 预览必须包含安全 fixture 草稿

教师端 Fixture 草稿预览的 JSON 内容 MUST 包含 `safetyFixtures`，以便教师人工审查风险来源、禁止短语和证据引用。

#### Scenario: 展开 Fixture 草稿预览

- **WHEN** 教师展开 Fixture 草稿预览
- **THEN** JSON 预览 SHALL 包含 `diagnosisFixtures`
- **AND** JSON 预览 SHALL 包含 `interventionFixtures`
- **AND** JSON 预览 SHALL 包含 `safetyFixtures`

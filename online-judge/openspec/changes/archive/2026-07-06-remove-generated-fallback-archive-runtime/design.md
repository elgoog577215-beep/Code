## Context

自动兜底层最初用于没有外部模型时保证知识树覆盖，后来已被多轮手写标准库和 V10/V11/V12 吸收批次替代。目前后端仍保留 `archivedGeneratedFallbackSeeds()` 和兜底价值分类类，这使兜底库虽然不进入活跃 seed，但仍在代码内可枚举。用户现在希望彻底删除这层运行时代码，只在备份目录保留快照。

## Goals / Non-Goals

**Goals:**

- 删除后端代码中的自动兜底 seed 枚举入口。
- 删除兜底存档价值分类运行时代码和测试。
- 在仓库备份目录保存删除前快照，供 Git/云端同步。
- 保留旧兜底 code 识别逻辑，以便启动时禁用历史数据库记录。
- 更新测试，证明备份目录不参与活跃 seed、候选包、质量报告或前后端。

**Non-Goals:**

- 不删除已经人工吸收的 V10/V11/V12 智能条目。
- 不删除历史 OpenSpec archive 记录。
- 不做数据库迁移；历史兜底记录继续由 Seeder 识别并禁用。

## Decisions

1. 备份目录只放静态文件，不放 Java/TS 可 import 代码。

   备份放在 `backups/standard-library/generated-fallback-archive-2026-07-06/`，其中包含快照和说明。该目录可以被 Git 同步，但不会被 Maven、Spring、前端构建或 seed catalog 扫描。

2. 删除枚举入口，保留识别入口。

   `archivedGeneratedFallbackSeeds()` 和 `generatedFullCoverage(...)` 提供了完整兜底库枚举，应删除；`isGeneratedFallbackCode(...)` 仍用于禁用旧数据库记录，应保留。

3. V12 吸收批次保留，兜底分类类删除。

   V12 已经把选中的 A/B 价值转为手写条目，正式库不需要继续运行时分类 C 类 archive-only。后续扩库只基于手写主题包和备份材料人工参考。

## Risks / Trade-offs

- [Risk] 删除枚举入口后不能在代码里直接统计剩余兜底缺口 → 备份快照保留删除前数据，后续需要人工参考或重新设计离线工具。
- [Risk] 历史数据库仍可能存在兜底记录 → 保留 `isGeneratedFallbackCode(...)`，Seeder 继续识别并禁用历史记录。
- [Risk] 备份目录被误接入运行时 → 测试断言活跃 seed 和质量报告不依赖备份，规格明确禁止前后端读取备份目录。

## Why

仓库虽然已经把学校启动脚本改为 `--no-build`，但 `main` 每次推送仍会通过 GitHub Actions SSH 到生产服务器，并由 `/usr/local/bin/deploy-online-judge` 执行 `docker compose up -d --build`。本次数据库工程发布中，该自动构建与外部镜像受控部署并发，终止了已验收应用并造成短暂不可达，说明现有发布门禁没有覆盖真实自动化入口。

## What Changes

- **BREAKING**：取消 `main` push 自动部署，GitHub Actions 只保留人工 `workflow_dispatch`，代码推送不再自动改动生产运行态。
- 新增仓库内可审查的服务器部署脚本，要求 `--confirm-build`，先调用显式构建入口，再调用固定 `--no-build` 的安全启动入口。
- GitHub Actions 的人工发布必须传入显式确认参数，不再调用无门禁的服务器构建命令。
- 扩展静态安全测试，覆盖 GitHub Actions 触发条件和服务器部署入口，阻止自动 push 构建、直接 `compose up --build` 或缺少确认参数回归。
- 在生产服务器备份旧入口，并让 `/usr/local/bin/deploy-online-judge` 指向仓库内受版本控制的脚本。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `production-release-safety`：把“生产启动不得隐式构建”的约束扩展到 GitHub Actions 和服务器部署命令，禁止代码 push 自动触发生产构建，并要求人工构建显式确认。

## Impact

- 仓库：`.github/workflows/deploy-online-judge.yml`、`online-judge/scripts/deploy-online-judge.sh`、部署安全静态测试和运维文档。
- 服务器：`/usr/local/bin/deploy-online-judge` 将从未版本化脚本改为仓库脚本链接；旧文件保留时间戳备份。
- 发布流程：合并 `main` 只更新代码仓库，不再自动部署；需要生产发布时，由运维人员明确触发人工工作流，服务器才允许构建和启动。
- 数据库：不修改 PostgreSQL 容器、镜像、Schema 或 Volume。

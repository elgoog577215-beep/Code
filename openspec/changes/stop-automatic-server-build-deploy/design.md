## Context

项目已通过 `separate-production-build-release` 将 `start-school.sh` 和 PowerShell 入口固定为 `docker compose up --no-build -d`，但真实 GitHub Actions 工作流仍在每次 `main` push 后 SSH 执行服务器全局命令。该命令直接运行 `docker compose up -d --build`，绕开了仓库内的确认门禁和安全启动脚本。

本次生产发布的 Docker events 与 GitHub Actions 运行记录给出了完整证据：外部构建镜像已经部署并通过 readiness 后，仍在运行的 push 工作流终止并替换了应用容器；下一次 push 又取消前一任务并启动新的服务器构建。生产数据库没有被替换，但应用在构建窗口内短暂不可达，磁盘也被新镜像和 runner 层推高。

## Goals / Non-Goals

**Goals:**

- 代码 push 不再自动改变生产运行态或触发服务器构建。
- 所有服务器镜像构建必须由人工工作流和 `--confirm-build` 双重显式授权。
- 服务器部署入口进入版本控制，并复用既有“显式构建”和“安全启动”脚本。
- 静态测试覆盖工作流、服务器入口、Linux 与 PowerShell 脚本的完整发布边界。
- 部署入口切换过程中不重建 PostgreSQL 容器或 Volume。

**Non-Goals:**

- 本轮不引入镜像仓库、签名、制品保留或跨环境晋级。
- 本轮不把服务器构建改成 GitHub runner 构建；这仍是下一阶段发布工程工作。
- 本轮不修改应用业务代码、数据库 Schema 或运行密钥。

## Decisions

### 1. GitHub Actions 只保留人工触发

删除 `push.branches: [main]`，只保留 `workflow_dispatch`。这是有意的行为变化：合并和推送只更新 Git 历史，不再自动 SSH 到生产服务器。相比增加 paths 过滤，彻底取消 push 触发可以覆盖代码、文档和 OpenSpec 等所有提交，避免新路径再次漏出自动构建。

### 2. 服务器入口复用两段式安全脚本

新增 `online-judge/scripts/deploy-online-judge.sh`，必须收到唯一参数 `--confirm-build` 才继续。脚本保留部署锁和 Git fast-forward 更新，但不直接写 `docker compose up --build`；它依次调用 `build-school-images.sh --confirm-build` 和 `start-school.sh`。这样构建与启动仍是两个独立阶段，启动阶段固定 `--no-build`，现有静态门禁可以继续生效。

服务器 `/usr/local/bin/deploy-online-judge` 改为指向仓库脚本的符号链接，原脚本先做时间戳备份。选择链接而不是复制，是为了让入口实现与仓库版本保持一致，避免下一次只修仓库而漏改服务器。

### 3. 人工工作流也必须传递确认参数

GitHub Actions 的 SSH 命令固定为 `deploy-online-judge --confirm-build`。`workflow_dispatch` 表示人工发起，参数门禁表示服务器再次确认调用者选择了构建路径；任何直接执行无参数命令都会在 Git、Docker 和容器变化前失败。

### 4. 静态测试覆盖真实绕行入口

扩展 `SchoolDeploymentScriptSafetyTest`：读取仓库根工作流和新的服务器脚本，验证工作流只有 `workflow_dispatch`、不存在 `push:`、SSH 命令带确认参数；验证服务器脚本有确认门禁，只调用受控构建与安全启动脚本，且不包含直接 `docker compose up ... --build`。文本门禁简单、无需 Docker daemon，并能直接锁定本次复发路径。

## Risks / Trade-offs

- [合并 main 后不再自动上线] → README 和工作流名称明确改为人工发布；只有已经准备好生产窗口时才触发。
- [人工工作流仍在小规格服务器构建] → 双重确认、flock 和现有资源预检降低误触概率；外部制品发布作为后续独立变更，不在本轮临时引入半套镜像仓库。
- [仓库脚本更新后服务器链接立即指向新内容] → 工作流只允许人工触发，脚本自身继续要求确认；发布前静态测试和 OpenSpec 严格校验必须通过。
- [切换全局入口失败] → 保留时间戳备份，可立即恢复原文件；切换不触碰数据库容器和 Volume。

## Migration Plan

1. 提交工作流、仓库部署脚本、静态测试和文档，先在本地完成定向测试与 OpenSpec 严格校验。
2. 在服务器备份 `/usr/local/bin/deploy-online-judge`，安装指向仓库脚本的符号链接，并验证无参数调用在任何 Git/Docker 变化前失败。
3. 推送包含“取消 push 触发”的提交；GitHub 应不创建新的 push 部署运行。
4. 观察现有应用、数据库容器、Flyway、readiness 和题库接口，确认切换没有改变运行态。
5. 若入口切换异常，恢复备份文件；若工作流配置异常，通过 Git 回退修正，但不恢复自动 push 构建。

## Open Questions

无。镜像仓库或 GitHub runner 外部构建将在后续发布工程变更中单独设计。

## Context

当前 `start-school.sh` 与 `start-school.ps1` 无条件执行 `docker compose up --build -d`。这使启动、配置重载和生产发布都会隐式进入 Node/Maven/GCC 构建流程。服务器历史记录表明该命令被反复用于生产发布，累计出约 10GB Build Cache；在 3.5GB 内存、30GB 系统盘的 ECS 上，一次 Maven 层失效即可造成严重资源压力。

## Goals / Non-Goals

**Goals:**

- 生产启动默认不构建，缺少镜像时明确失败。
- 镜像构建必须使用独立命令并显式确认。
- 保留现有 Compose 服务、数据卷和首次安装能力。
- 用自动化测试阻止生产脚本重新引入 `--build`。

**Non-Goals:**

- 本变更不引入镜像仓库或 CI 平台。
- 不改动 PostgreSQL 数据结构、Volume、业务代码或模型配置。
- 不自动清理镜像、容器、Volume 或数据库备份。

## Decisions

### 1. 启动脚本默认使用 `--no-build`

Linux 与 PowerShell 启动脚本统一执行 `docker compose up --no-build -d`。如果应用镜像不存在，命令失败并提示先在受控环境显式构建或加载镜像。相比保留默认 `--build` 再增加生产参数，这一选择把安全行为设为默认值，避免调用者遗漏参数。

### 2. 构建脚本要求显式确认

新增 `build-school-images.sh` 与 `build-school-images.ps1`。Linux 入口要求 `--confirm-build`，PowerShell 入口要求 `-ConfirmBuild`，然后只执行 `docker compose build app cpp17-runner`，不启动或替换运行容器。

### 3. 生产发布仍复用 Compose

外部构建或加载目标镜像后，继续通过启动脚本调用 Compose。Compose 会保留既有 `postgres-data` Volume；脚本不得调用 `down -v`、`system prune`、`volume prune` 或任何数据库初始化命令。

### 4. 静态测试锁定危险命令边界

新增 JUnit 测试读取四个脚本：启动脚本必须包含 `--no-build` 且不得包含 `--build`；构建脚本必须包含显式确认和限定服务的 `docker compose build`。该测试无需 Docker daemon，可在完整后端测试中稳定执行。

## Risks / Trade-offs

- [首次安装时没有本地镜像，启动脚本会失败] → 输出明确的构建/加载镜像指引，并在 README 中提供首次安装顺序。
- [外部镜像没有版本标签或加载错误] → 文档要求生产发布保留旧镜像作为回滚，不继续依赖隐式构建兜底。
- [用户仍可手工执行 `docker compose up --build`] → 脚本和文档只能建立项目默认门禁；服务器权限治理和 CI 镜像发布作为后续独立工作。

## Migration Plan

1. 合并新脚本和文档，不自动重启现有容器。
2. 后续首次发布在本地或 CI 显式构建并验证 `linux/amd64` 镜像。
3. 服务器加载镜像后运行安全启动脚本。
4. 若新镜像异常，重新标记并启动保留的旧镜像；PostgreSQL Volume 始终保持挂载。

## Open Questions

无。镜像仓库与 CI 自动发布将在确认具体平台后单独设计。

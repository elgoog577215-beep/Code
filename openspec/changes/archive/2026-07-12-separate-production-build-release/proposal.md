## Why

现有学校启动脚本把“启动服务”和“现场构建镜像”绑定为同一个 `docker compose up --build` 命令。生产服务器在构建缓存膨胀、磁盘空间紧张和源码层失效时，会重新下载依赖并编译前后端，可能耗尽小规格 ECS 的内存与 I/O，进而造成整机失去响应。

## What Changes

- **BREAKING**：学校启动脚本默认只启动已有镜像，不再隐式构建。
- 新增显式镜像构建入口，首次安装或受控构建环境必须主动选择构建操作。
- 新增生产发布入口，只允许使用已存在的镜像并重建应用容器，禁止触碰 PostgreSQL 数据卷。
- 在部署文档中区分首次安装、生产发布和纯配置重载，并补充备份、磁盘检查、回滚与线上验收门禁。
- 为 shell 与 PowerShell 脚本补充静态回归检查，防止生产路径重新引入 `--build`。

## Capabilities

### New Capabilities

- `production-release-safety`: 约束生产发布、镜像构建、配置重载、数据保护和发布验收之间的边界。

### Modified Capabilities

无。

## Impact

- 影响 `online-judge/scripts/start-school.sh`、`start-school.ps1`、新增的构建/发布脚本和学校部署文档。
- 不修改应用业务代码、数据库结构、Docker Volume 或 AI 诊断协议。
- 运维行为改变：首次安装需要显式构建；生产服务器默认拒绝现场构建。

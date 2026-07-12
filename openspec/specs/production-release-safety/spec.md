# production-release-safety Specification

## Purpose
TBD - created by archiving change separate-production-build-release. Update Purpose after archive.
## Requirements
### Requirement: 生产启动不得隐式构建镜像
系统 SHALL 为 Linux 与 PowerShell 提供默认不构建镜像的学校启动入口，生产启动过程中 MUST 使用已有镜像且不得执行 Compose build。

#### Scenario: 使用已有镜像启动学校服务
- **WHEN** 运维人员运行学校启动脚本且所需镜像已经存在
- **THEN** 系统使用 `docker compose up --no-build -d` 启动或更新服务
- **AND** 不执行 Node、Maven 或 GCC 构建

#### Scenario: 应用镜像不存在
- **WHEN** 运维人员运行学校启动脚本但应用镜像尚未构建或加载
- **THEN** 启动流程失败并提示使用显式构建入口或加载发布镜像
- **AND** 不自动回退到服务器现场构建

### Requirement: 镜像构建必须显式授权
系统 SHALL 将镜像构建放在独立脚本中，调用者 MUST 提供显式确认参数后才能执行构建。

#### Scenario: 未确认构建
- **WHEN** 调用者直接运行镜像构建脚本但未提供确认参数
- **THEN** 脚本以非零状态退出
- **AND** 不执行 `docker compose build`

#### Scenario: 已确认构建
- **WHEN** 调用者在受控构建环境提供确认参数
- **THEN** 脚本只构建应用镜像与 C++17 runner 镜像
- **AND** 不启动、不替换运行中的应用或数据库容器

### Requirement: 生产发布保护数据库资产
生产启动与镜像构建脚本 MUST NOT 删除或重建 PostgreSQL Volume，MUST NOT 调用广域 Docker 清理命令，并 SHALL 在部署文档中明确数据库备份和回滚边界。

#### Scenario: 更新应用镜像
- **WHEN** 运维人员加载新应用镜像并运行学校启动脚本
- **THEN** PostgreSQL 服务继续使用既有 `postgres-data` Volume
- **AND** 脚本不调用 `down -v`、`system prune` 或 `volume prune`

### Requirement: 部署脚本安全边界必须自动验证
项目 SHALL 提供无需 Docker daemon 的自动化测试，验证生产启动脚本与显式构建脚本的命令边界。

#### Scenario: 回归测试发现生产脚本重新构建
- **WHEN** 启动脚本包含 `--build` 或缺少 `--no-build`
- **THEN** 自动化测试失败

#### Scenario: 回归测试发现构建入口缺少确认
- **WHEN** 构建脚本缺少显式确认参数或构建了非限定服务
- **THEN** 自动化测试失败

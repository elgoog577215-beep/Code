## MODIFIED Requirements

### Requirement: 生产启动不得隐式构建镜像
系统 SHALL 为 Linux、PowerShell、GitHub Actions 和服务器命令提供默认不构建镜像的生产启动入口；普通代码 push MUST NOT 触发生产镜像构建或替换运行容器，生产启动过程中 MUST 使用已有镜像且不得隐式执行 Compose build。

#### Scenario: 使用已有镜像启动学校服务
- **WHEN** 运维人员运行学校启动脚本且所需镜像已经存在
- **THEN** 系统使用 `docker compose up --no-build -d` 启动或更新服务
- **AND** 不执行 Node、Maven 或 GCC 构建

#### Scenario: 应用镜像不存在
- **WHEN** 运维人员运行学校启动脚本但应用镜像尚未构建或加载
- **THEN** 启动流程失败并提示使用显式构建入口或加载发布镜像
- **AND** 不自动回退到服务器现场构建

#### Scenario: 代码推送到 main
- **WHEN** 提交被推送或合并到 `main`
- **THEN** GitHub Actions SHALL NOT 自动 SSH 到生产服务器
- **AND** SHALL NOT 构建镜像、替换容器或修改生产运行态

### Requirement: 镜像构建必须显式授权
系统 SHALL 将镜像构建放在独立脚本中；GitHub Actions MUST 只允许人工触发生产构建，服务器部署入口 MUST 要求显式确认参数后才能执行构建。

#### Scenario: 未确认构建
- **WHEN** 调用者直接运行镜像构建脚本或服务器部署入口但未提供确认参数
- **THEN** 脚本以非零状态退出
- **AND** 不执行 Git 更新、`docker compose build` 或容器替换

#### Scenario: 已确认构建
- **WHEN** 调用者人工触发生产工作流且服务器入口收到显式确认参数
- **THEN** 部署入口 SHALL 调用独立构建脚本只构建应用镜像与 C++17 runner 镜像
- **AND** SHALL 在构建完成后调用使用 `--no-build` 的安全启动脚本

### Requirement: 部署脚本安全边界必须自动验证
项目 SHALL 提供无需 Docker daemon 的自动化测试，验证生产启动脚本、显式构建脚本、GitHub Actions 工作流和服务器部署入口的命令边界。

#### Scenario: 回归测试发现生产脚本重新构建
- **WHEN** 启动脚本包含 `--build` 或缺少 `--no-build`
- **THEN** 自动化测试失败

#### Scenario: 回归测试发现构建入口缺少确认
- **WHEN** 构建脚本或服务器部署入口缺少显式确认参数，或构建了非限定服务
- **THEN** 自动化测试失败

#### Scenario: 回归测试发现 push 自动部署
- **WHEN** GitHub Actions 部署工作流包含 `push` 触发器，或 SSH 部署命令缺少显式确认参数
- **THEN** 自动化测试失败

#### Scenario: 回归测试发现服务器入口绕过安全脚本
- **WHEN** 服务器部署入口直接执行带 `--build` 的 Compose 启动，或没有调用受控构建与安全启动脚本
- **THEN** 自动化测试失败

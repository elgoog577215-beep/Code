# 温中编程学习平台

这是一个面向课堂和信息竞赛训练的在线判题系统。学生可以用 Windows 电脑、安卓平板或其他浏览器设备打开网站写代码；C++17 代码在后端服务器的 Docker runner 中编译运行，学生设备不需要安装 C++ 编译器。

## 学校开箱部署

服务器需要安装 Docker Desktop、OrbStack 或兼容 Docker Engine。

先复制配置并替换真实口令和密钥：

```bash
cp .env.example .env
```

必须修改：

- `POSTGRES_PASSWORD`
- `TEACHER_PASSWORD`
- `TEACHER_SESSION_SECRET`
- `STUDENT_TOKEN_SECRET`

部署前先跑一次环境自检：

```bash
bash scripts/doctor-school.sh
```

### 首次安装或受控构建

镜像构建与生产启动已经分离。只允许在本地、CI 或确认资源充足的受控环境显式构建；构建脚本不会启动或替换任何容器。

macOS / Linux：

```bash
bash scripts/build-school-images.sh --confirm-build
bash scripts/start-school.sh
```

Windows PowerShell：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-school-images.ps1 -ConfirmBuild
powershell -ExecutionPolicy Bypass -File scripts/start-school.ps1
```

### 生产发布

生产服务器不得执行 `docker compose up --build`，也不应运行镜像构建脚本。先在外部构建并验证与服务器架构一致的镜像，再推送到镜像仓库或用 `docker save` / `docker load` 传入服务器。

替换应用前必须：

1. 备份 `.env` 和 PostgreSQL，并验证备份可读。
2. 检查磁盘、内存、运行容器与数据卷。
3. 为当前应用镜像保留带时间戳的回滚标签。
4. 加载新镜像后运行安全启动脚本；该脚本固定使用 `--no-build`。

macOS / Linux 生产启动或配置重载：

```bash
bash scripts/start-school.sh
```

Windows PowerShell 生产启动或配置重载：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-school.ps1
```

若镜像不存在，启动脚本会直接失败并提示先构建或加载镜像，不会回退到服务器现场构建。生产发布不得使用 `docker system prune`、`docker volume prune` 或 `docker compose down -v`；PostgreSQL 的 `postgres-data` Volume 不参与应用镜像替换。

新镜像上线后必须检查页面、readiness、判题和 AI smoke。若验证失败，重新指向保留的旧镜像并再次运行安全启动脚本，数据库 Volume 保持不变。

启动后访问：

```text
http://localhost:8081/app/
```

如果要在局域网给学生访问，把 `localhost` 换成运行服务器的局域网 IP。

教师端第一次进入会要求输入 `.env` 中的 `TEACHER_PASSWORD`。教师可在 `/app/teacher-management` 查看“开课状态”，包括 Docker、C++17 runner、数据库、教师口令、学生令牌密钥和 AI smoke 状态。

如果学校网络无法访问 Docker Hub，请先让网管配置 Docker 镜像加速，或在 `.env` 中把基础镜像变量改成学校内网仓库里的兼容镜像。应用镜像构建使用的 Node、Maven、JRE、Docker CLI 镜像和 C++17 runner 的 GCC 镜像都可以替换。本机已验证 C++17 runner 可用备用源：

```env
OJ_CPP17_BASE_IMAGE=public.ecr.aws/docker/library/gcc:13-bookworm
```

本机也验证了应用构建基础镜像可从 `public.ecr.aws/docker/library` 读取：`node:24-bookworm-slim`、`maven:3.9.9-eclipse-temurin-17`、`eclipse-temurin:17-jre`、`docker:29-cli`。Python runner 是否切换镜像源，建议按学校网络和内网仓库实际情况配置。

## 默认能力

- 后端 Java 17。
- 前端 React/Vite，Docker 构建阶段自动安装 Node 依赖并打包。
- 学校部署默认使用 Postgres；H2 仅用于本机开发。
- 教师端使用单校共享教师口令和 HttpOnly cookie 会话。
- 学生端登录后使用访问令牌隔离个人轨迹、推荐和提交反馈。
- C++17 runner 镜像：`wenzhong-oj-cpp17-runner:13`。
- Python 3 runner 镜像：`python:3.12-slim`。
- Docker 沙箱默认关闭网络，并限制 CPU、内存和进程数。
- 应用容器通过 Docker socket 调用 runner，源码和输入以 Java 内置 tar 流传入 runner，不依赖宿主机临时目录路径或系统 `tar` 命令。

## 配置

复制 `.env.example` 为 `.env` 后可以调整：

```bash
cp .env.example .env
```

常用变量：

- `SERVER_PORT`: 对外端口，默认 `8081`。
- `APP_PROFILE`: 学校部署使用 `school`。
- `EXECUTOR_MODE`: 学校部署建议保持 `docker`。
- `POSTGRES_PASSWORD`: 学校部署数据库密码。
- `FLYWAY_BASELINE_ON_MIGRATE`: 正式运行固定为 `false`；只有受控的一次性旧库基线允许临时启用。
- `TEACHER_PASSWORD`: 教师端共享口令。
- `TEACHER_SESSION_SECRET`: 教师会话签名密钥。
- `STUDENT_TOKEN_SECRET`: 学生访问令牌签名密钥。
- `OJ_APP_IMAGE`: 应用 Docker 镜像名。

学校与生产 PostgreSQL 使用 Flyway 管理 Schema，Hibernate 只执行结构校验。第一次把已有非空数据库接入 Flyway 时，不要直接启动新应用，先阅读并执行 [数据库迁移与恢复指南](docs/database-migration-guide.md)。
- `OJ_CPP17_DOCKER_IMAGE`: C++17 runner 镜像名。
- `OJ_CPP17_BASE_IMAGE`: 构建 C++17 runner 使用的基础镜像，默认 `gcc:13-bookworm`。
- `OJ_PYTHON3_DOCKER_IMAGE`: Python 3 runner 镜像名。
- `OJ_NODE_BASE_IMAGE`: 构建前端使用的 Node 基础镜像。
- `OJ_MAVEN_BASE_IMAGE`: 构建后端使用的 Maven/JDK 基础镜像。
- `OJ_JRE_BASE_IMAGE`: 运行应用使用的 Java 17 JRE 基础镜像。
- `OJ_DOCKER_CLI_IMAGE`: 提供 Docker CLI 的基础镜像，应用容器会从该镜像复制 `docker` 命令。
- `AI_ENABLED`: 是否启用模型反馈。
- `OJ_MODELSCOPE_API_KEY`: 模型 API Key，推荐用于学校 Docker 部署；旧名 `MODELSCOPE_API_KEY` 仍兼容。
- `AI_READINESS_BLOCKING`: 是否要求 AI smoke 通过才允许 readiness 为 `READY`。

更多投用要求见 [高中单校内网投用清单](docs/school-readiness-checklist.md)。

## 本机开发

需要 Java 17 和 Node/npm：

```bash
cd frontend
npm ci
npm run build
cd ..
./mvnw spring-boot:run
```

如果只跑后端测试、不重建前端：

```bash
./mvnw -Dskip.frontend=true test
```

## C++17 环境检查

学校部署预检：

```bash
bash scripts/doctor-school.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/doctor-school.ps1
```

单独构建并烟测 runner：

```bash
bash scripts/build-cpp17-runner.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-cpp17-runner.ps1
```

教师端系统状态会检查 Docker 和 C++17 runner 是否就绪。如果未就绪，学生仍能打开网站写代码，但 C++17 提交会提示联系老师完成服务器部署配置。

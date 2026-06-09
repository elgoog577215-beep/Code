# 温中编程学习平台

这是一个面向课堂和信息竞赛训练的在线判题系统。学生可以用 Windows 电脑、安卓平板或其他浏览器设备打开网站写代码；C++17 代码在后端服务器的 Docker runner 中编译运行，学生设备不需要安装 C++ 编译器。

## 学校开箱部署

服务器需要安装 Docker Desktop、OrbStack 或兼容 Docker Engine。

部署前可以先跑一次环境自检：

```bash
bash scripts/doctor-school.sh
```

macOS / Linux:

```bash
bash scripts/start-school.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-school.ps1
```

启动后访问：

```text
http://localhost:8081/app/
```

如果要在局域网给学生访问，把 `localhost` 换成运行服务器的局域网 IP。

如果学校网络无法访问 Docker Hub，请先让网管配置 Docker 镜像加速，或在 `.env` 中把基础镜像变量改成学校内网仓库里的兼容镜像。应用镜像构建使用的 Node、Maven、JRE、Docker CLI 镜像和 C++17 runner 的 GCC 镜像都可以替换。本机已验证 C++17 runner 可用备用源：

```env
OJ_CPP17_BASE_IMAGE=public.ecr.aws/docker/library/gcc:13-bookworm
```

本机也验证了应用构建基础镜像可从 `public.ecr.aws/docker/library` 读取：`node:24-bookworm-slim`、`maven:3.9.9-eclipse-temurin-17`、`eclipse-temurin:17-jre`、`docker:29-cli`。Python runner 是否切换镜像源，建议按学校网络和内网仓库实际情况配置。

## 默认能力

- 后端 Java 17。
- 前端 React/Vite，Docker 构建阶段自动安装 Node 依赖并打包。
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
- `EXECUTOR_MODE`: 学校部署建议保持 `docker`。
- `OJ_APP_IMAGE`: 应用 Docker 镜像名。
- `OJ_CPP17_DOCKER_IMAGE`: C++17 runner 镜像名。
- `OJ_CPP17_BASE_IMAGE`: 构建 C++17 runner 使用的基础镜像，默认 `gcc:13-bookworm`。
- `OJ_PYTHON3_DOCKER_IMAGE`: Python 3 runner 镜像名。
- `OJ_NODE_BASE_IMAGE`: 构建前端使用的 Node 基础镜像。
- `OJ_MAVEN_BASE_IMAGE`: 构建后端使用的 Maven/JDK 基础镜像。
- `OJ_JRE_BASE_IMAGE`: 运行应用使用的 Java 17 JRE 基础镜像。
- `OJ_DOCKER_CLI_IMAGE`: 提供 Docker CLI 的基础镜像，应用容器会从该镜像复制 `docker` 命令。
- `AI_ENABLED`: 是否启用模型反馈。
- `OJ_MODELSCOPE_API_KEY`: 模型 API Key。

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

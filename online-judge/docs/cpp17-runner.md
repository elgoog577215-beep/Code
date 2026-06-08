# C++17 判题环境部署说明

Windows、安卓平板或其他学生电脑只是通过浏览器访问网站，不在学生设备上编译 C++。C++17 代码在网站后端服务器的 runner 中编译和运行。

## 推荐部署方式

课堂或竞赛试点推荐使用 Docker 沙箱：

1. 在运行后端服务的服务器上安装并启动 Docker。
2. 构建项目自带 C++17 runner 镜像。

macOS / Linux:

```bash
bash scripts/build-cpp17-runner.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-cpp17-runner.ps1
```

3. 启动后端时使用 Docker 执行模式：

```bash
EXECUTOR_MODE=docker ./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
$env:EXECUTOR_MODE = "docker"
.\mvnw.cmd spring-boot:run
```

默认 runner 镜像名为 `wenzhong-oj-cpp17-runner:13`。如需改名，构建和启动时保持同一个环境变量：

```bash
OJ_CPP17_DOCKER_IMAGE=my-oj-cpp17:13 bash scripts/build-cpp17-runner.sh
EXECUTOR_MODE=docker OJ_CPP17_DOCKER_IMAGE=my-oj-cpp17:13 ./mvnw spring-boot:run
```

## 本机执行模式

本机模式只适合开发调试。服务器必须安装可编译 `#include <bits/stdc++.h>` 的 GNU g++，必要时设置：

```bash
OJ_CPP17_COMPILER=g++-13 ./mvnw spring-boot:run
```

macOS 自带的 `/usr/bin/g++` 通常是 Apple clang，不能作为信息竞赛 C++17 环境。

## 教师端状态

教师端系统状态会检查：

- Docker 是否启动。
- `wenzhong-oj-cpp17-runner:13` 镜像是否存在。
- C++17 runner 是否可用于后端判题。

如果状态显示未就绪，学生仍可以打开网站和写代码，但 C++17 提交会被后端拦截并提示联系老师完成部署配置。

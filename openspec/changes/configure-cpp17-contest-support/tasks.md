## 1. OpenSpec 与现状确认

- [x] 1.1 完成 `configure-cpp17-contest-support` proposal、design、spec、tasks。
- [x] 1.2 运行 OpenSpec strict validation。
- [x] 1.3 复核当前 C++17 执行器、判题白名单、系统状态和前端入口。

## 2. 后端 C++17 配置

- [x] 2.1 新增或收敛后端语言配置模型，集中维护 C++17 的 ID、名称、文件名、扩展名和可用性语义。
- [x] 2.2 更新本机 C++17 编译命令为 `g++ -std=c++17 -O2 -pipe`。
- [x] 2.3 更新 Docker C++17 配置和不支持语言文案，保持 C++17 语义清晰。
- [x] 2.4 更新 `JudgeService` 使用集中语言配置，并保持 C++17 环境未就绪错误可解释。
- [x] 2.5 更新执行环境状态服务与 DTO，使教师端能明确看到 C++17 可用性。
- [x] 2.6 增加项目自带 C++17 runner 镜像配置，并让 Docker 模式检查 runner 镜像是否存在。
- [x] 2.7 增加 macOS/Linux 与 Windows 的 runner 构建烟测脚本。
- [x] 2.8 增加应用 Dockerfile、docker-compose、环境变量样例、学校一键启动脚本和部署预检脚本。
- [x] 2.9 将 Docker 判题源码/输入传输改为 tar 流，避免应用容器内路径与宿主机路径不一致。

## 3. 前端 C++17 入口

- [x] 3.1 抽出学生端语言选项和模板配置，消除散落的 `languageId === 54` 判断。
- [x] 3.2 确保 C++17 下拉项、默认模板、文件名、编辑器语法和恢复模板一致。
- [x] 3.3 更新教师端执行环境展示，使用 C++17 文案。
- [x] 3.4 如 API 类型变化，更新前端类型定义和调用。

## 4. 测试与验证

- [x] 4.1 新增后端测试覆盖 C++17 语言配置和环境状态语义。
- [x] 4.2 新增或更新后端判题测试，覆盖 C++17 AC、编译错误、环境不可用。
- [x] 4.3 运行相关 Maven 测试。
- [x] 4.4 运行 frontend typecheck。
- [x] 4.5 运行 `git diff --check` 并确认无无关改动。
- [x] 4.6 验证本机 JDK 17、Node/npm、Docker daemon 可用。
- [x] 4.7 构建 C++17 runner 镜像并完成 Docker smoke。（Docker Hub 当前不可达，本机已用 `public.ecr.aws/docker/library/gcc:13-bookworm` 完成构建和 smoke）

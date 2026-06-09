## Context

当前平台已经有基础多语言判题能力，但 C++17 的产品化链路不完整：

- `LocalCodeExecutor` 支持 `54` 对应 C++，但编译参数只有 `g++ -o`，没有固定 C++17 标准和竞赛常用优化参数；macOS 自带 `/usr/bin/g++` 实际是 Apple clang，不能编译信息竞赛常用的 `#include <bits/stdc++.h>`。
- `DockerCodeExecutor` 支持 C++17，参数为 `-std=c++17 -O2 -pipe`，但语言配置与本地执行器重复。
- `JudgeService` 只在本服务内维护语言白名单，教师端系统状态只暴露 `cppAvailable`。
- 学生端只暴露 Python 3 和 C++17，但默认模板、文件名和恢复模板仍以二选一判断散落在页面中。

信息竞赛课堂需要的是稳定、可解释、可部署的 C++17 主语言能力。学生关心能不能按竞赛习惯提交；老师关心环境是否就绪；后续 AI 诊断关心判题事实是否可靠。

## Goals / Non-Goals

**Goals:**

- 将 C++17 定义为一等竞赛语言，并稳定进入提交白名单。
- 统一本机与 Docker 的 C++17 编译参数，默认使用 `-std=c++17 -O2 -pipe`。
- 集中后端语言元数据，避免语言 ID、显示名、扩展名和可用性判断继续散落。
- 学生端 C++17 入口、模板、文件名、编辑器语法和恢复模板保持一致。
- 学生端默认语言使用 C++17，贴合信息竞赛训练习惯。
- 教师端执行环境展示能明确说明 C++17 是否可用。
- 补充 C++17 判题测试，覆盖 AC、编译错误、环境不可用等核心路径。

**Non-Goals:**

- 本轮不开放 C 语言。
- 本轮不新增语言管理后台。
- 本轮不引入完整沙箱编排系统。
- 本轮不改变 AI 诊断字段结构。
- 本轮不改变数据库结构。

## Decisions

### Decision 1: 固定 C++17 为信息竞赛默认标准

C++ 提交统一按 C++17 编译，显示名固定为 `C++17`，语言 ID 继续使用现有 `54`，避免破坏历史提交和前端草稿 key。

理由：

- C++17 是当前信息竞赛训练最稳妥的默认标准，兼容常用 STL、结构化绑定、`auto`、`constexpr` 等学生常见写法。
- 保留语言 ID 可以减少数据库和历史提交迁移。
- 不把 C++20 作为默认，避免部署环境和竞赛平台兼容性不一致。

替代方案：使用 C++20 或允许老师选择标准。暂不采用，因为会增加配置复杂度，且信息竞赛课堂首先需要稳定默认值。

### Decision 2: 集中语言配置，不再在服务中散落魔法数字

新增轻量语言配置模型，统一表达：

- `languageId`
- `displayName`
- `sourceFileName`
- `editorKind`
- 是否需要编译器检测
- 本地/Docker 编译参数

`JudgeService`、`ExecutorStatusService` 和前端语言选项都围绕这套语义收敛。

理由：

- 后续扩展 C、Java 或更多竞赛语言时，只需要新增配置和测试，不会继续复制判断。
- 能把学生端、教师端和后端判题看到的语言名称统一起来。

替代方案：继续在每个文件里写 `languageId === 54`。暂不采用，因为这会让 C++17 支持停留在“能跑”，而不是平台级能力。

### Decision 3: 正式课堂优先项目自带 Docker runner，本机执行必须通过 GNU C++17 烟测

Docker 沙箱仍是正式课堂推荐执行方式；项目内提供 `docker/cpp17-runner/Dockerfile` 和构建脚本，默认镜像名为 `wenzhong-oj-cpp17-runner:13`。本机执行器保留，用于本地开发和缺 Docker 的演示环境。两者都使用 C++17 参数，但只有 Docker 能提供更强隔离。本机模式不能只检测 `g++ --version`，必须实际编译包含 `bits/stdc++.h` 的 C++17 代码，避免 macOS Apple clang 被误判为可用信息竞赛环境。

理由：

- C/C++ 是不可信代码执行风险较高的语言，本机执行不适合作为正式开放环境。
- 当前 Docker 执行器已有无网络、内存、进程数和超时控制，适合作为第一阶段沙箱。
- 把 runner 镜像定义放进项目后，Windows 学生电脑只需要浏览器，服务器部署者按项目脚本构建镜像即可。
- 应用容器通过 Docker socket 调用 runner，源码和输入以 Java 内置 tar 流传入 runner，避免容器内路径和宿主机路径不一致导致挂载失败，也避免应用运行环境额外依赖系统 `tar` 命令。
- 本机模式保留可降低开发调试门槛，但必须优先寻找 `OJ_CPP17_COMPILER`、`g++-14/g++-13/g++-12/g++-11/g++` 中能通过 GNU 竞赛模板烟测的编译器。

替代方案：只支持 Docker C++17。暂不采用，因为当前开发和演示环境可能还依赖本机模式。

### Decision 4: C++17 不改变 AI 诊断结构，但必须提供可靠判题事实

本轮不新增 AI 字段，不改变错因分析结构；C++17 的价值先体现在提交结果、编译输出、运行错误和测试点事实稳定进入现有诊断链路。

理由：

- AI 诊断的输入事实可靠，比新增语言特定提示词更基础。
- 避免把语言支持变成一次 AI pipeline 重构。

## Risks / Trade-offs

- [Risk] 本机 `g++` 可能是 Apple clang，缺少 `bits/stdc++.h`。
  → Mitigation: 系统状态使用真实 GNU 竞赛模板烟测，不通过则标记 C++17 未就绪；正式课堂推荐项目 C++17 runner 镜像。

- [Risk] Docker 镜像首次构建或基础镜像拉取慢。
  → Mitigation: 提供项目内 `scripts/build-cpp17-runner.sh` 和 `scripts/build-cpp17-runner.ps1`；课堂部署前构建 `wenzhong-oj-cpp17-runner:13`。

- [Risk] 学校网络或本机网络暂时无法访问 Docker Hub。
  → Mitigation: `OJ_CPP17_BASE_IMAGE`、`OJ_NODE_BASE_IMAGE`、`OJ_MAVEN_BASE_IMAGE`、`OJ_JRE_BASE_IMAGE`、`OJ_DOCKER_CLI_IMAGE` 均可切换为学校内网镜像仓库里的兼容镜像；compose 和构建脚本读取这些变量，并提供 `doctor-school` 预检脚本。

- [Risk] Docker 已启动但 C++17 runner 镜像未构建。
  → Mitigation: Docker 模式下系统状态检查 `wenzhong-oj-cpp17-runner:13` 镜像是否存在，不只检查 Docker daemon。

- [Risk] 只支持 C++17，部分学生想用 C 或 Java。
  → Mitigation: 本轮先保证信息竞赛主语言稳定；C/Java 后续按同一语言配置模型扩展。

- [Risk] 前端语言列表若继续写死，会与后端配置漂移。
  → Mitigation: 本轮至少抽出前端语言常量；后续可升级为后端语言列表 API。

## Migration Plan

1. 创建 OpenSpec proposal/design/spec/tasks。
2. 新增或收敛后端语言配置模型，保留现有语言 ID。
3. 更新本机与 Docker C++17 编译参数和错误文案。
4. 增加项目自带 C++17 runner Dockerfile、构建烟测脚本和部署说明。
5. 增加应用 Dockerfile、docker-compose、环境变量样例、学校一键启动脚本和学校部署预检脚本。
6. 更新判题服务和执行环境状态逻辑。
7. 更新学生端语言模板、文件名和编辑器模式。
8. 更新教师端环境展示文案。
9. 添加后端 C++17 判题测试和前端 typecheck。
10. 运行 OpenSpec strict validation、相关后端测试、前端 typecheck、`git diff --check`。

## Open Questions

无需要用户继续决策的问题。本轮默认信息竞赛主语言为 C++17；C 语言和更多语言留到后续变更。

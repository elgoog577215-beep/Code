## ADDED Requirements

### Requirement: 系统必须将 C++17 作为信息竞赛主语言

系统 SHALL 将 C++17 作为正式提交语言开放，语言标识、显示名称、源文件名、编辑器语法和判题白名单必须保持一致。

#### Scenario: 信息竞赛题目页默认 C++17

- **WHEN** 学生打开题目页代码区
- **THEN** 系统 SHALL 默认选择 C++17
- **AND** 系统 SHALL 默认展示 C++17 模板

#### Scenario: 学生选择 C++17 提交

- **WHEN** 学生在题目页选择 C++17
- **THEN** 系统 SHALL 使用 `54` 作为提交语言 ID
- **AND** 系统 SHALL 将语言显示为 `C++17`
- **AND** 系统 SHALL 使用 `.cpp` 源文件和 C++ 编辑器语法
- **AND** 系统 SHALL 使用 C++17 默认模板恢复代码

#### Scenario: 后端记录 C++17 提交语言

- **WHEN** 后端接收 `languageId=54` 的提交
- **THEN** 系统 SHALL 将提交记录的 `languageName` 保存为 `C++17`
- **AND** 系统 SHALL 允许该提交进入判题流程

### Requirement: C++17 编译参数必须适合信息竞赛

系统 SHALL 在本机执行器和 Docker 沙箱中使用一致的 C++17 竞赛编译参数，并且本机执行环境必须能编译信息竞赛常用的 GNU C++ 头文件。

#### Scenario: 本机执行器编译 C++17

- **WHEN** 本机执行器运行 C++17 提交
- **THEN** 系统 SHALL 使用通过 GNU C++17 烟测的编译器
- **AND** 编译命令 SHALL include `-std=c++17`
- **AND** 编译命令 SHALL include `-O2`
- **AND** 编译命令 SHALL include `-pipe`

#### Scenario: macOS clang 不得误报为信息竞赛 C++17 可用

- **WHEN** 本机 `g++` 无法编译 `#include <bits/stdc++.h>` 的 C++17 代码
- **THEN** 执行环境状态 SHALL 标记 C++17 未就绪
- **AND** 错误信息 SHALL explain missing GNU contest toolchain

#### Scenario: Docker 沙箱编译 C++17

- **WHEN** Docker 执行器运行 C++17 提交
- **THEN** 系统 SHALL 使用项目配置的 C++17 runner 镜像
- **AND** 系统 SHALL 使用 C++17 编译命令
- **AND** 编译命令 SHALL include `-std=c++17`
- **AND** 编译命令 SHALL include `-O2`
- **AND** 编译命令 SHALL include `-pipe`

#### Scenario: Windows 学生电脑通过网站提交 C++17

- **WHEN** 学生使用 Windows 电脑浏览器打开网站并提交 C++17
- **THEN** 系统 SHALL 在后端服务器 C++17 runner 中编译运行代码
- **AND** 系统 SHALL NOT 要求学生 Windows 电脑安装 C++ 编译器

#### Scenario: 项目提供 C++17 runner 部署资产

- **WHEN** 部署者准备课堂或竞赛试点环境
- **THEN** 项目 SHALL provide C++17 runner Dockerfile
- **AND** 项目 SHALL provide macOS/Linux 构建烟测脚本
- **AND** 项目 SHALL provide Windows PowerShell 构建烟测脚本

### Requirement: C++17 执行环境状态必须可解释

系统 SHALL 检测 C++17 执行环境是否可用，并向教师端和提交链路输出可理解的课堂提示。

#### Scenario: C++17 编译器可用

- **WHEN** 系统检测到 Docker 沙箱和项目 C++17 runner 镜像可用，或本机 `g++` 可用
- **THEN** 执行环境状态 SHALL 标记 C++17 可用
- **AND** 教师端 SHALL 展示 C++17 已就绪

#### Scenario: Docker 已启动但 C++17 runner 未构建

- **WHEN** 系统处于 Docker 执行模式
- **AND** Docker daemon 可用
- **AND** 项目 C++17 runner 镜像不存在
- **THEN** 执行环境状态 SHALL 标记 C++17 未就绪
- **AND** 错误信息 SHALL 提醒运行项目内构建脚本

#### Scenario: C++17 编译器不可用

- **WHEN** 学生提交 C++17 代码但执行环境未就绪
- **THEN** 系统 SHALL 返回 `INTERNAL_ERROR`
- **AND** 错误信息 SHALL 明确说明 C++17 执行环境未就绪
- **AND** 错误信息 SHALL 提醒联系老师完成部署配置

### Requirement: C++17 判题事实必须进入现有诊断链路

系统 SHALL 对 C++17 提交产生与其他语言一致的判题事实，包括编译输出、运行输出、测试点结果和最终 verdict。

#### Scenario: C++17 正确提交通过样例

- **WHEN** 学生提交能够通过所有测试点的 C++17 代码
- **THEN** 系统 SHALL 返回 `ACCEPTED`
- **AND** 系统 SHALL 保存测试点通过结果
- **AND** 后续 AI 诊断 SHALL 能读取该提交的语言、源码和判题事实

#### Scenario: C++17 编译错误被记录

- **WHEN** 学生提交存在编译错误的 C++17 代码
- **THEN** 系统 SHALL 返回 `COMPILATION_ERROR`
- **AND** 系统 SHALL 保存编译输出
- **AND** 编译输出 SHALL 可被学生结果页和后续 AI 诊断读取

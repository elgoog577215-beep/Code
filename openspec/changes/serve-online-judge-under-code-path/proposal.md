## Why

服务器入口已从 Nginx 切换为 Caddy，主站同时占用 `/app/` 与根级 `/api/`。Online Judge 当前虽然对外宣称使用 `/code/`，但页面、React 路由和静态资源仍原生位于 `/app/`，只能依赖代理层改写响应内容或跳转到旧子域，导致正式主域入口失效并与平台路由冲突。

## What Changes

- **BREAKING**：将 Online Judge 的前端页面、SPA 路由和构建资源基路径从 `/app/` 迁移到 `/code/`。
- 浏览器端 API 统一通过 `/code/api/` 访问，应用使用固定前缀适配器复用容器内部既有 `/api/` 控制器，不修改后端 API 合同。
- Spring Boot 将构建产物原生发布到 `/code/`，并为 `/code/` 深层 SPA 路由提供入口回退。
- 部署脚本与回归测试改为验证 Caddy、`/code/` 页面、`/code/assets/` 静态资源、`/code/api/` readiness 和 SPA 深链，不再依赖 Nginx 或旧子域。
- 保留根级 `/api/` 作为容器内部与后端测试的接口路径；主域平台的根级 `/api/` 继续归平台所有。
- 新增机器可读的路由所有权合同，统一管理正式域名、OJ 页面/API/资源前缀、平台保留路径与旧入口策略；构建、前端路由、后端常量和部署门禁必须消费或校验该合同。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `production-release-safety`：正式发布必须保证 Online Judge 原生使用 `/code/` 页面路径，并通过标准 Caddy 路由与主平台共存。

## Impact

- 前端：Vite `base`、React Router `basename`、应用内相对导航、浏览器 API 基路径、Playwright/smoke 脚本。
- 后端：集中路径常量、静态资源目录与映射、SPA forward、缓存头、相关 MVC/部署回归测试。
- 部署：`deploy-online-judge.sh`、Caddy 主域配置、生产探针、README 与项目决策。
- 不涉及数据库迁移，不改变 Java 控制器的根级 `/api/` 合同，不需要 Caddy 插件，也不切回 Nginx。

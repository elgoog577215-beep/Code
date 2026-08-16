## Context

Online Judge 当前将 Vite 资源输出到 `static/app`，React 路由和所有前端链接使用 `/app/...`，浏览器 API 使用根级 `/api/...`。2026-07-20 的 Nginx 方案通过响应与请求改写，把主域 `/code/` 映射到这些内部路径；2026-08-04 服务器已切换为 Caddy，主平台占用 `/app/` 与根级 `/api/`，现有 Caddy 因此只能把 `/code/` 跳转到旧子域。

Caddy 已具备 `handle`、`uri strip_prefix` 和 `reverse_proxy`，不需要第三方插件。关键约束是同时处理页面、静态资源、SPA 深链和 API，不能只修改 Vite `base`。

## Goals / Non-Goals

**Goals:**

- 正式主域唯一入口为 `https://tuotuzju.com/code/`，不占用平台 `/app/` 或根级 `/api/`。
- 应用构建产物原生生成 `/code/assets/...`，React 内部导航始终留在 `/code/...`。
- 浏览器通过 `/code/api/...` 调用 OJ；后端控制器继续使用 `/api/...`，由标准前缀适配处理公开路径。
- Caddy 配置只使用官方内置指令，并通过 `caddy validate`、真实 HTTPS 页面、资源、API 和 SPA 深链验收。
- 旧 `/app/...` 请求获得可预测的 `/code/...` 重定向，避免已有书签静默落到主平台页面。

**Non-Goals:**

- 不修改数据库、业务 API 语义、鉴权模型或 AI 流程。
- 不安装 Caddy 插件，不切回 Nginx，不恢复旧子域为正式入口。
- 不让 OJ 接管主域根级 `/api/`、`/app/` 或 `/download/`。

## Decisions

### 1. 应用原生前端基路径统一为 `/code/`

Vite `base`、输出目录、React 路由、导航链接、Spring 静态资源映射与 SPA forward 同步迁移。相比仅在 Caddy 中把 `/code/` 改写为 `/app/`，该方案不会让 HTML 继续生成冲突的 `/app/assets/...`，因此无需响应体替换插件。

### 2. 公开 API 使用 `/code/api/`，内部 API 合同保留 `/api/`

前端请求层统一把既有 `/api/...` 请求解析为 `/code/api/...`。Spring 增加受限的 `/code/api/**` 前缀适配，使应用直连也可完成端到端验证；Caddy 生产配置直接反向代理 `/code/*`，不要求更改全部 Java 控制器。适配器只处理固定 `/code/api` 前缀，不接受任意重写目标。

选择该方案而不是让 Caddy 根据 Referer 分流根级 `/api/`，因为 Referer 不可靠且会把两个应用的 API 所有权混在一起。

### 3. 旧 `/app/` 只保留重定向兼容，不继续提供正式静态内容

Spring 对旧页面入口返回到等价 `/code/...` 的重定向。生产 Caddy 不代理主域 `/app/` 给 OJ，因此平台路由所有权不会被破坏；旧子域可通过重定向继续引导到新路径。

### 4. 部署门禁改为 Caddy 与四链路探针

部署脚本在容器启动后先检查直连 `/code/` 与内部 `/api/system/readiness`，再运行 `caddy validate --config /etc/caddy/Caddyfile`，最后检查正式 HTTPS 的 `/code/`、从 HTML 解析出的 `/code/assets/...`、`/code/api/system/readiness` 和 `/code/student`。不再调用 `nginx -t`。

### 5. 路径所有权使用机器合同统一管理

`config/route-ownership.json` 统一声明正式域名、Online Judge 页面/API/资源前缀、upstream 标记、平台保留路径和旧入口兼容策略。Vite 与构建清理直接读取合同；React Router 使用 Vite `BASE_URL` 作为 `basename`，业务组件只维护应用内部相对路由；Java 端只从 `OnlineJudgeWebPaths` 使用集中常量，并由合同测试确保常量与 JSON 一致；部署脚本通过 `jq` 读取合同，动态生成正式探针和平台保留路径检查。

服务器的稳定命令使用指向仓库脚本的符号链接，不保留独立脚本副本。部署脚本在拉取后比较自身哈希；若脚本刚被更新，则在持有同一部署锁的前提下重新执行新版本，避免发布门禁落后一版。

## Risks / Trade-offs

- [旧链接仍指向 `/app/...`] → 后端仅在请求实际到达 OJ 时保留等价重定向；主域 `/app/` 属于平台，正式文档和外链统一改为 `/code/...`。
- [API 前缀适配绕过鉴权过滤器] → 使用请求路径包装后继续原过滤器链，而不是在适配器内自行调用控制器；增加受保护 API 回归测试。
- [Caddy 配置错误导致主站受影响] → 修改前保留时间戳备份，先 `caddy validate`，再原子替换并 reload；失败立即恢复备份。
- [服务器仓库有本地改动与环境备份] → 部署脚本现有 `pull --ff-only` 可能被阻塞；部署前只读核对差异，不覆盖本地安全配置，必要时仅在确认无冲突后执行受控部署。

## Migration Plan

1. 本地完成 `/code/` 前端、静态资源、API 适配与回归脚本迁移，运行前端构建和相关后端测试。
2. 提交并推送代码；保留服务器运行中的旧容器作为回滚对象。
3. 备份 `/etc/caddy/Caddyfile`，把主域 `/code*` 从旧子域跳转改为 `127.0.0.1:8081` 反向代理，验证后 reload。
4. 通过受控部署入口构建并启动新镜像，依次验证页面、资源、API、SPA 和平台 `/app/`、`/download/` 未回归。
5. 若应用验证失败，恢复旧 Caddyfile 并重载；若镜像失败，重新指向保留的旧镜像，PostgreSQL Volume 不变。

## Open Questions

无。当前服务器、Caddy 能力、端口与正式域名均已通过只读检查确认。

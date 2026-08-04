## ADDED Requirements

### Requirement: Online Judge 正式入口必须原生位于主域 `/code/`
系统 SHALL 将 Online Judge 的页面、SPA 路由和静态资源原生发布在 `/code/` 前缀下；主域 `/app/`、`/download/` 与根级 `/api/` MUST 继续归主平台所有，正式入口 MUST NOT 跳转到旧子域。

#### Scenario: 访问正式入口
- **WHEN** 用户访问 `https://tuotuzju.com/code/`
- **THEN** 系统直接返回 Online Judge 页面且最终 URL 仍位于主域 `/code/`
- **AND** 页面引用的脚本与样式使用 `/code/assets/` 前缀

#### Scenario: 访问 SPA 深层路由
- **WHEN** 用户直接访问 `/code/student` 或任一受支持的 `/code/...` 页面路由
- **THEN** 系统返回前端入口并由客户端路由渲染目标页面
- **AND** 不跳转到 `/app/` 或旧子域

#### Scenario: 主平台路径保持独立
- **WHEN** 用户访问主域 `/app/`、`/download/` 或根级 `/api/`
- **THEN** Caddy 继续返回主平台内容
- **AND** Online Judge 不接管这些请求

### Requirement: Online Judge 公开 API 必须使用 `/code/api/` 命名空间
浏览器端 Online Judge SHALL 通过 `/code/api/...` 发起接口请求；代理或应用适配层 MUST 将该固定前缀映射到既有后端 `/api/...` 控制器，并 MUST 保留请求方法、查询参数、请求体、鉴权头与响应语义。

#### Scenario: 正式 readiness 请求
- **WHEN** 浏览器或发布探针请求 `https://tuotuzju.com/code/api/system/readiness`
- **THEN** 请求由 Online Judge 后端处理并返回 readiness JSON
- **AND** 主平台根级 `/api/` 不参与该请求

#### Scenario: 受保护 API 经前缀适配
- **WHEN** 未授权客户端通过 `/code/api/teacher/...` 请求受保护接口
- **THEN** 系统执行与对应 `/api/teacher/...` 相同的鉴权检查
- **AND** 前缀适配不得绕过既有安全过滤器

### Requirement: Caddy 发布不得依赖第三方响应改写插件
生产 Caddy 配置 SHALL 仅使用已安装的标准匹配、URI 处理和反向代理能力发布 `/code/`；发布流程 MUST 在 reload 前验证配置，并 MUST 检查页面、静态资源、API 与 SPA 深链。

#### Scenario: 部署合法配置
- **WHEN** 运维流程准备加载新的 Caddy 配置
- **THEN** 流程先执行 `caddy validate --config /etc/caddy/Caddyfile`
- **AND** 验证成功后才允许 reload

#### Scenario: 生产路由回归
- **WHEN** 新应用与 Caddy 配置完成发布
- **THEN** `/code/` 页面、`/code/assets/` 资源、`/code/api/system/readiness` 与 `/code/student` 均通过 HTTPS 探针
- **AND** 合同声明的 `/app/`、`/download/` 与根级 `/api/` 均未被 Online Judge 接管

### Requirement: 跨层路由所有权必须由统一合同管理
系统 SHALL 使用机器可读合同声明正式域名、Online Judge 页面/API/资源前缀、代理 upstream 标记、平台保留路径和旧入口兼容策略；前端构建、客户端路由、后端路径常量与部署探针 MUST 消费该合同或通过自动化测试证明一致。业务页面 MUST NOT 重复嵌入公开部署前缀。

#### Scenario: 修改 Online Judge 公开前缀
- **WHEN** 维护者修改统一路由所有权合同中的 Online Judge 公开前缀
- **THEN** Vite 构建路径、React Router basename、浏览器 API、后端常量和部署探针必须同步通过合同校验
- **AND** 任一层仍保留旧前缀时，构建或测试必须失败

#### Scenario: 平台新增保留路径
- **WHEN** 平台在统一合同中新增保留路径
- **THEN** 生产发布自动将该路径纳入未被 Online Judge 接管的探针
- **AND** 无需再修改部署脚本中的硬编码路径列表

#### Scenario: 部署入口脚本升级
- **WHEN** 服务器仓库拉取后部署脚本本身发生变化
- **THEN** 发布流程在继续构建前重新执行仓库中的新脚本
- **AND** 稳定命令不得长期运行脱离仓库的旧脚本副本

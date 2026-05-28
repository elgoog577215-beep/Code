## 1. 规格

- [x] 1.1 新增 OpenSpec 规格，描述健康接口、脱敏和风险等级。

## 2. 实现

- [x] 2.1 新增 `AiRouteHealthResponse` DTO。
- [x] 2.2 新增 `AiRouteHealthService`，读取 `ai.*` 配置并生成路由健康画像。
- [x] 2.3 在 `SystemController` 暴露 `/api/system/ai-route-health`。
- [x] 2.4 将 AI 路由健康接入教师管理页系统状态区，展示健康等级、可用路由数、备用路由、路由池和脱敏路由明细。

## 3. 验证

- [x] 3.1 单元测试覆盖单路由风险、多路由就绪、无路由、缺字段和密钥不泄露。
- [x] 3.2 运行相关 Maven 测试、前端类型检查和 OpenSpec 严格校验。

## 4. 沉淀

- [x] 4.1 将“路由健康预检不等于真实质量评测”的边界写入项目记忆。

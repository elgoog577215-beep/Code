## 1. 请求与配置

- [x] 1.1 为 ModelScope 兼容请求加入可配置 `enable_thinking`，生产默认 `false`，并覆盖开关单元测试
- [x] 1.2 将应用默认主模型和模型池首位切换为 `deepseek-ai/DeepSeek-V4-Pro`
- [x] 1.3 同步 `.env.example`、Docker Compose、本地 `.env` 的推理开关和主模型配置

## 2. 回归验证

- [x] 2.1 运行请求工厂、AI runtime、持久化阶段和完整后端测试
- [x] 2.2 使用 V4 Pro 非推理模式运行真实困难样本并确认全部必需阶段成功
- [x] 2.3 运行 OpenSpec 校验、构建检查和 `git diff --check`

## 3. 服务器切换

- [ ] 3.1 备份并同步服务器模型配置，只重建应用容器
- [ ] 3.2 运行服务器 readiness、AI smoke 和真实提交验证
- [ ] 3.3 归档 OpenSpec 变更并提交推送最终配置

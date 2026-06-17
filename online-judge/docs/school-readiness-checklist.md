# 高中单校内网投用清单

这份清单用于一所学校内网试点。目标是能安全上课，不覆盖公网 SaaS、多校租户或商业合规场景。

## 1. 部署前

1. 复制配置。

```bash
cp .env.example .env
```

2. 修改 `.env` 中的真实值：

- `APP_PROFILE=school`
- `EXECUTOR_MODE=docker`
- `POSTGRES_PASSWORD`
- `TEACHER_PASSWORD`
- `TEACHER_SESSION_SECRET`
- `STUDENT_TOKEN_SECRET`
- 如需外部 AI：`AI_ENABLED=true`、`OJ_MODELSCOPE_API_KEY`

3. 运行预检。

```bash
bash scripts/doctor-school.sh
```

预检失败时不要开课。先按提示处理 Docker、镜像源、Postgres 密码或密钥配置。

## 2. 启动

```bash
bash scripts/start-school.sh
```

访问：

```text
http://服务器局域网IP:8081/app/
```

教师进入 `/app/teacher-management`，输入教师口令，查看“开课状态”。只有 `READY` 或明确接受 `DEGRADED` 时才上课。

## 3. AI 数据外发

启用外部 AI 后，系统会把学生提交代码、题目信息、评测结果、测试点摘要和诊断证据发送到配置的模型服务，用于生成学生反馈。

不发送教师口令、学生访问令牌、数据库密码或系统密钥。

如果外部 AI 返回 401、429、超时或格式错误，系统仍可判题，但不能宣称 AI 能力可用，也不建议作为高中 AI 试点版开课。

如果学校不允许学生代码外发，保持：

```env
AI_ENABLED=false
```

## 4. 数据备份

学校部署使用 Postgres。建议每天课后备份一次：

```bash
bash scripts/backup-postgres.sh
```

恢复：

```bash
bash scripts/restore-postgres.sh backups/onlinejudge-YYYYMMDD-HHMMSS.sql
```

备份文件默认在 `backups/`，不要提交到 Git。

## 5. 常见故障

- `Docker daemon is not available`：启动 Docker Desktop、OrbStack 或 Docker Engine。
- `C++17 runner image is not built yet`：运行 `bash scripts/build-cpp17-runner.sh` 或重新 `bash scripts/start-school.sh`。
- AI smoke 显示 `INSUFFICIENT_QUOTA`：ModelScope 额度不足，充值或关闭 AI。
- AI smoke 显示认证失败：检查 `OJ_MODELSCOPE_API_KEY`。
- 教师端无法进入：检查 `.env` 中 `TEACHER_PASSWORD` 是否已设置，并重新启动服务。

## 6. 最低验收

- `bash scripts/doctor-school.sh` 通过。
- 教师端能登录。
- `/api/system/readiness` 返回 `READY`，或只剩外部 AI 降级项。
- Python 提交可 AC/WA。
- C++17 提交在 runner 就绪后可 AC。
- AI smoke 通过，或教师明确接受规则诊断降级。

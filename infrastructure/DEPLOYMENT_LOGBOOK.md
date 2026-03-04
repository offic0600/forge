# Forge 部署记录

## 环境

- 服务器: Rocky Linux / 2 CPU / 内网 IP 10.3.0.16
- 部署方式: 生产环境 (docker-compose.production-single.yml)
- 日期: 2026-03-04

## 步骤与耗时

| 步骤 | 操作 | 耗时 |
|------|------|------|
| 1 | 安装 git, docker, jdk21, nodejs20 | ~5min |
| 2 | 克隆仓库 + 子模块 | ~1min |
| 3 | 构建后端 JAR | 4min |
| 4 | 构建 MCP Server | 1min |
| 5 | 构建前端 + 控制台 | 2min |
| 6 | 配置环境变量 + nginx ssl | ~1min |
| 7 | 启动容器 | ~3min |

## 问题

1. **SSH 未配置**: 克隆仓库时认证失败 → 配置 GitHub SSH 公钥解决
2. **CPU 限制过高**: backend 配置 `cpus: 3`，机器只有 2 核 → 改为 `cpus: 2`
3. **外部访问异常**: DNS 解析到公网 IP 13.248.243.5，机器只有内网 IP 10.3.0.16 → 需检查云平台网络配置

## 最终端口

- HTTP: 80, HTTPS: 443
- Keycloak: 8180, Backend: 8080

## 访问地址

- Web IDE: https://forge.delivery
- 控制台: https://forge.delivery/console
- Keycloak: https://forge.delivery/auth

## 命令

```bash
cd /root/forge/infrastructure/docker
docker compose -f docker-compose.production-single.yml --env-file .env.production up -d
docker compose -f docker-compose.production-single.yml logs -f
```

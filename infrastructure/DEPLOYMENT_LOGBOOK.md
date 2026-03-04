# Forge 部署记录

## 环境

- 服务器: Rocky Linux / 2 CPU / 4G RAM / 内网 IP 10.3.0.16
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

## 问题汇总

### 1. Git SSH 认证失败
- **现象**: 克隆仓库时 `git@github.com: Permission denied (publickey)`
- **原因**: 未配置 GitHub SSH 公钥
- **解决**: 生成 SSH 密钥并将公钥添加到 GitHub 账户

### 2. Docker CPU 限制过高
- **现象**: `Error: Range of CPUs is from 0.01 to 2.00`
- **原因**: docker-compose.yml 中 backend 配置 `cpus: 3`，机器只有 2 核
- **解决**: 改为 `cpus: 2`

### 3. 机器内存不足导致 OOM
- **现象**: 机器宕机
- **原因**: backend 配置 `mem_limit: 5g`，机器只有 4G 内存
- **解决**: 改为 `mem_limit: 2g`，JVM 堆 `-Xmx1536m`

### 4. SSO 登录跳转到 localhost (重点!)
- **现象**: 登录跳转 `http://localhost:8180/realms/forge/...`
- **原因**: 多处配置问题
  1. Keycloak `KC_HOSTNAME_URL` 写死为 `https://forge.delivery/auth`
  2. realm-export.json 中的 client redirectUris 包含 localhost
  3. 前端构建时环境变量未正确传入
  4. 前端 .next/standalone 缓存了旧配置
- **解决**:
  1. Keycloak 环境变量改为 `${KEYCLOAK_PUBLIC_URL}`
  2. 重建 Keycloak 数据库重新导入 realm
  3. 前端构建时传入环境变量
  4. 前端容器需要重建才能加载新的构建产物

### 5. /auth/callback 404
- **现象**: 登录成功后跳转 `/auth/callback` 页面不存在
- **原因**: nginx 配置缺少 `/auth/callback` 路由，被 `/auth/` 规则匹配到 keycloak
- **解决**: 添加 `location = /auth/callback` 规则代理到 frontend

### 6. 外部访问异常
- **现象**: DNS 解析到公网 IP 13.248.243.5 但机器只有内网 IP
- **原因**: 云平台网络配置问题，需检查安全组和 NAT 转发

## 最终修改的文件

- `infrastructure/docker/docker-compose.production-single.yml`
  - backend: cpus 3→2, mem_limit 5g→2g
  - nginx: 添加 443 端口和 SSL 挂载
  - keycloak: KC_HOSTNAME_URL 改为环境变量
- `infrastructure/docker/nginx-production.conf`
  - 添加 HTTPS 配置
  - 添加 /auth/callback 路由
- `web-ide/frontend/`
  - 需重新构建才能加载正确的环境变量

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

# 重建前端（修改环境变量后）
cd ../../web-ide/frontend
NEXT_PUBLIC_KEYCLOAK_URL=https://forge.delivery/auth npm run build
cd ../../infrastructure/docker
docker compose -f docker-compose.production-single.yml up -d --build frontend
```

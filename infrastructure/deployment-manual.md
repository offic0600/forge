# Forge 部署手册

> 两套 compose 文件，按场景选择：
>
> | 文件 | 用途 | 容器数 | SSO |
> |------|------|--------|-----|
> | `docker-compose.trial.yml` | 试用 / 开发环境 | **8 个**（含 Keycloak） | ✅ 开启 |
> | `docker-compose.production-single.yml` | 8C16G 单机生产 | **8 个**（含 Keycloak） | ✅ 开启 |

---

## 一、环境准备

### 1.1 安装 Docker + Compose

**Ubuntu 22.04+：**

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker
```

**Rocky Linux / RHEL / CentOS Stream：**

```bash
sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl start docker
sudo systemctl enable docker
```

**验证：**

```bash
docker --version         # 需要 24+
docker compose version   # 需要 v2.20+
```

### 1.2 配置国内镜像加速（国内服务器必须）

```bash
mkdir -p /etc/docker
cat > /etc/docker/daemon.json << 'EOF'
{
  "registry-mirrors": ["https://docker.m.daocloud.io"]
}
EOF
systemctl daemon-reload
systemctl restart docker
```

### 1.3 安装构建工具

**Ubuntu：**

```bash
sudo apt install -y openjdk-21-jdk
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs git
```

**Rocky Linux：**

```bash
sudo dnf install -y java-21-openjdk-devel git
curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -
sudo dnf install -y nodejs
```

**验证：**

```bash
java -version   # 必须 21+
node -v         # 需要 20+
npm -v
git --version
```

### 1.4 创建数据目录（生产模式必须）

```bash
mkdir -p /data/forge/{postgres,backend}
```

---

## 二、获取代码

```bash
cd /opt
git clone git@github.com:pan94u/forge.git
cd forge
git submodule update --init --recursive
```

> SSH 未配置时用 HTTPS：`git clone https://github.com/pan94u/forge.git`

---

## 三、构建产物

> 原则：**宿主机构建 + Docker 只打包**。不在容器内编译（避免网络/依赖问题）。

```bash
cd /opt/forge

# 后端 JAR（约 10 分钟，首次需下载依赖）
./gradlew :web-ide:backend:clean :web-ide:backend:bootJar -x test

# MCP Server JAR
./gradlew :mcp-servers:forge-knowledge-mcp:shadowJar
./gradlew :mcp-servers:forge-database-mcp:shadowJar

# Web IDE 前端
cd web-ide/frontend && npm ci && npm run build && cd ../..

# 企业控制台
cd enterprise-console && npm ci && npm run build && cd ..
```

**验证产物：**

```bash
ls -lh web-ide/backend/build/libs/backend-*.jar
ls -lh mcp-servers/forge-knowledge-mcp/build/libs/*-all.jar
ls -lh mcp-servers/forge-database-mcp/build/libs/*-all.jar
ls -d web-ide/frontend/.next/standalone
ls -d enterprise-console/.next/standalone
```

---

## 四、试用环境部署（docker-compose.trial.yml）

适合：开发调试、功能演示。数据存 Docker volume（非 bind mount）。

### 4.1 配置环境变量

```bash
cd /opt/forge/infrastructure/docker
cp .env.trial.example .env
```

使用 echo 逐行写入（SSH 粘贴长命令不换行）：

```bash
f=/opt/forge/infrastructure/docker/.env
echo 'MODEL_PROVIDER=minimax' > $f
echo 'MINIMAX_API_KEY=<your-key>' >> $f
echo 'MINIMAX_API_URL=https://api.minimaxi.com/anthropic' >> $f
echo "FORGE_ENCRYPTION_KEY=$(openssl rand -base64 32)" >> $f
```

### 4.2 启动

```bash
cd /opt/forge/infrastructure/docker
docker compose -f docker-compose.trial.yml up --build -d
docker compose -f docker-compose.trial.yml logs -f
```

### 4.3 访问地址（试用）

| 服务 | 地址 |
|------|------|
| Web IDE | http://localhost:19000 |
| 企业控制台 | http://localhost:19001 |
| Keycloak Admin | http://localhost:8180 |

---

## 五、生产环境部署（docker-compose.production-single.yml）

适合：8C16G 单机生产。数据 bind mount 到 `/data/forge/`，资源有明确 CPU/内存限制。

### 5.1 生成密钥

```bash
echo "DB_PASSWORD=$(openssl rand -base64 16)"
echo "FORGE_ENCRYPTION_KEY=$(openssl rand -base64 32)"
echo "NEXTAUTH_SECRET=$(openssl rand -base64 32)"
```

记录以上输出，下一步用到。

### 5.2 配置环境变量

> **注意**：SSH 会话中 heredoc 和 vim 容易因终端折行出错，推荐用 echo 逐行写入。

```bash
f=/opt/forge/infrastructure/docker/.env.production
echo 'MODEL_PROVIDER=minimax' > $f
echo 'MINIMAX_API_KEY=<your-minimax-key>' >> $f
echo 'MINIMAX_API_URL=https://api.minimaxi.com/anthropic' >> $f
echo 'DB_PASSWORD=<生成的密码>' >> $f
echo 'FORGE_ENCRYPTION_KEY=<生成的密钥>' >> $f
echo 'FORGE_FRONTEND_URL=https://<your-domain>' >> $f
echo 'FORGE_CORS_ALLOWED_ORIGINS=https://<your-domain>' >> $f
echo 'KEYCLOAK_PUBLIC_URL=https://<your-domain>/auth' >> $f
echo 'KEYCLOAK_ADMIN_PASSWORD=<强密码>' >> $f
echo 'KEYCLOAK_ENTERPRISE_SECRET=enterprise-secret' >> $f
echo 'CONSOLE_PUBLIC_URL=https://<your-domain>/console' >> $f
echo 'NEXTAUTH_SECRET=<生成的密钥>' >> $f

# 验证
cat $f
```

**必填项说明：**

| 变量 | 说明 | 示例 |
|------|------|------|
| `MODEL_PROVIDER` | 模型提供商 | `minimax` |
| `MINIMAX_API_KEY` | MiniMax API Key | `sk-cp-...` |
| `DB_PASSWORD` | PostgreSQL 密码 | `openssl rand -base64 16` |
| `FORGE_ENCRYPTION_KEY` | 数据加密密钥 | `openssl rand -base64 32` |
| `FORGE_FRONTEND_URL` | Web IDE 访问地址 | `https://forge.haier.net` |
| `FORGE_CORS_ALLOWED_ORIGINS` | CORS 白名单 | `https://forge.haier.net` |
| `KEYCLOAK_PUBLIC_URL` | Keycloak 浏览器地址 | `https://forge.haier.net/auth` |
| `KEYCLOAK_ADMIN_PASSWORD` | Keycloak 管理员密码 | 自定义强密码 |
| `CONSOLE_PUBLIC_URL` | 企业控制台访问地址 | `https://forge.haier.net/console` |
| `NEXTAUTH_SECRET` | NextAuth 会话密钥 | `openssl rand -base64 32` |

> **域名说明**：Keycloak 挂载在 `/auth/` 路径下，所有服务通过同一域名 + 端口 9000 访问（由公司统一 nginx 转发，无需关心 HTTPS 配置）。

### 5.3 首次部署（全新机器）

> 建议在 tmux 中执行，防止 SSH 断开中断：

```bash
dnf install -y tmux     # Rocky Linux
# apt install -y tmux   # Ubuntu

tmux new -s forge
```

```bash
cd /opt/forge/infrastructure/docker

# 首次启动约 8-12 分钟（构建镜像 + Keycloak 初始化 + realm 导入）
docker compose -f docker-compose.production-single.yml --env-file .env.production up --build -d

# 查看进度
docker compose -f docker-compose.production-single.yml --env-file .env.production logs -f
```

> **Keycloak 启动说明**：
> - 首次启动（含 realm 导入）约 60-90s
> - 后续重启约 30-40s
> - `start_period` 已设为 180s，正常情况无需手动干预

SSH 断开后恢复：`tmux attach -t forge`

### 5.4 数据已存在时升级部署

```bash
# 确认 keycloak 数据库是否存在
docker exec forge-postgres psql -U forge -l | grep keycloak

# 不存在则手动创建（一次性操作）
docker exec forge-postgres psql -U forge -c "CREATE DATABASE keycloak;"
docker exec forge-postgres psql -U forge -c "GRANT ALL PRIVILEGES ON DATABASE keycloak TO forge;"
```

### 5.5 访问地址（生产 + 域名模式）

| 服务 | 地址 |
|------|------|
| Web IDE | `https://forge.haier.net` |
| 企业控制台 | `https://forge.haier.net/console` |
| Keycloak Admin | `https://forge.haier.net/auth` |
| PostgreSQL | `127.0.0.1:5432`（仅本机） |

---

## 六、验证部署

### 6.1 容器状态

```bash
docker compose -f /opt/forge/infrastructure/docker/docker-compose.production-single.yml \
  --env-file /opt/forge/infrastructure/docker/.env.production ps
```

预期：9 个容器全部 `Up`，其中 5 个带 `(healthy)`

```
NAME                        STATUS
forge-keycloak              Up (healthy)
forge-postgres              Up (healthy)
forge-knowledge-mcp         Up (healthy)
forge-database-mcp          Up (healthy)
forge-backend               Up (healthy)
forge-frontend              Up
forge-enterprise-console    Up
forge-nginx                 Up
```

### 6.2 逐服务验证

```bash
SERVER=<server-ip>

# Keycloak 就绪
curl -sf http://${SERVER}:8180/auth/health/ready | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])"
# 预期: UP

# Keycloak realm 已导入
curl -sf http://${SERVER}:8180/auth/realms/forge | python3 -c "import sys,json; print(json.load(sys.stdin)['realm'])"
# 预期: forge

# Web IDE（未登录跳转 Keycloak）
curl -s -o /dev/null -w "%{http_code}" http://${SERVER}:9000/
# 预期: 307

# 企业控制台（未登录跳转 Keycloak）
curl -s -o /dev/null -w "%{http_code}" http://${SERVER}:9001/
# 预期: 307
```

### 6.3 SSO 登录验证

1. 浏览器访问 Web IDE，自动跳转 Keycloak 登录页
2. 使用内置账号登录

**内置账号：**

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `admin` | admin + developer |
| `dev1` | `dev1` | developer |
| `viewer1` | `viewer1` | viewer |

> **生产环境**：请立即在 Keycloak Admin 控制台修改默认密码并删除测试账号。

### 6.4 资源使用

```bash
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}"
```

---

## 七、更新部署

```bash
cd /opt/forge

# 1. 拉取最新代码
git pull && git submodule update --recursive

# 2. 按需重新构建（只构建有变更的模块）
./gradlew :web-ide:backend:clean :web-ide:backend:bootJar -x test
# cd web-ide/frontend && npm ci && npm run build && cd ../..
# cd enterprise-console && npm ci && npm run build && cd ..

# 3. 重建并启动容器
docker compose -f infrastructure/docker/docker-compose.production-single.yml \
  --env-file infrastructure/docker/.env.production \
  up --build -d

# 4. 清理旧镜像
docker image prune -f
```

---

## 八、日常运维

### 8.1 查看日志

```bash
docker logs forge-backend --tail 100 -f
docker logs forge-keycloak --tail 50 -f

# 全部容器
docker compose -f /opt/forge/infrastructure/docker/docker-compose.production-single.yml \
  --env-file /opt/forge/infrastructure/docker/.env.production logs -f --tail 30
```

### 8.2 重启服务

```bash
# 重启单个容器
docker compose -f /opt/forge/infrastructure/docker/docker-compose.production-single.yml \
  --env-file /opt/forge/infrastructure/docker/.env.production restart backend

# 全部重启
docker compose -f /opt/forge/infrastructure/docker/docker-compose.production-single.yml \
  --env-file /opt/forge/infrastructure/docker/.env.production restart
```

### 8.3 数据库备份

```bash
mkdir -p /data/forge/backups

# 手动备份
docker exec forge-postgres pg_dump -U forge forge | \
  gzip > /data/forge/backups/forge_$(date +%Y%m%d_%H%M%S).sql.gz
docker exec forge-postgres pg_dump -U forge keycloak | \
  gzip > /data/forge/backups/keycloak_$(date +%Y%m%d_%H%M%S).sql.gz

# 每日自动备份（crontab -e）
# 0 3 * * * docker exec forge-postgres pg_dump -U forge forge | gzip > /data/forge/backups/forge_$(date +\%Y\%m\%d).sql.gz
# 0 3 * * * docker exec forge-postgres pg_dump -U forge keycloak | gzip > /data/forge/backups/keycloak_$(date +\%Y\%m\%d).sql.gz
# 0 4 * * * find /data/forge/backups -name "*.sql.gz" -mtime +7 -delete
```

### 8.4 Keycloak 用户管理

```bash
# 添加用户
docker exec forge-keycloak /opt/keycloak/bin/kcadm.sh \
  create users -r forge \
  -s username=newuser -s enabled=true \
  --server http://localhost:8080/auth --realm master \
  --user admin --password ${KEYCLOAK_ADMIN_PASSWORD}

# 设置密码
docker exec forge-keycloak /opt/keycloak/bin/kcadm.sh \
  set-password -r forge --username newuser --new-password newpass \
  --server http://localhost:8080/auth --realm master \
  --user admin --password ${KEYCLOAK_ADMIN_PASSWORD}

# 分配角色（developer / viewer / admin）
docker exec forge-keycloak /opt/keycloak/bin/kcadm.sh \
  add-roles -r forge --uusername newuser --rolename developer \
  --server http://localhost:8080/auth --realm master \
  --user admin --password ${KEYCLOAK_ADMIN_PASSWORD}
```

---

## 九、故障排查

### 健康检查失败排查

```bash
# 查看具体失败原因
docker inspect <container-name> --format '{{json .State.Health.Log}}' | python3 -m json.tool | tail -20
```

### 常见问题速查

| 现象 | 检查命令 | 常见原因 |
|------|---------|---------|
| Keycloak unhealthy | `docker inspect forge-keycloak --format '{{json .State.Health.Log}}'` | 镜像内无 curl，已用 bash tcp 替代；或启动超时（正常等待即可） |
| Keycloak 启动慢 | `docker logs forge-keycloak \| tail -30` | 首次 60-90s（含 realm 导入），重启 30-40s，属正常 |
| Keycloak DB 连接失败 | `docker logs forge-keycloak \| grep "FATAL\|ERROR"` | `keycloak` 数据库未创建，见 5.4 节 |
| Backend unhealthy | `docker inspect forge-backend --format '{{json .State.Health.Log}}'` | 安全模式下 `/api/knowledge/search` 返回 401，healthcheck 已配置接受 401 |
| Backend 401 | `docker logs forge-backend \| grep -i "jwt\|401"` | `KEYCLOAK_PUBLIC_URL` 与 token iss 不符 |
| EC 登录循环重定向 | `docker logs forge-enterprise-console` | `NEXTAUTH_SECRET` 未设置或 `KEYCLOAK_ISSUER` 地址错误 |
| 内存 OOM | `docker stats --no-stream` | backend mem_limit 需 5G |
| 磁盘满 | `df -h /data/forge` | 清理备份和旧镜像：`docker image prune -f` |

---

## 十、文件清单

```
infrastructure/
├── docker/
│   ├── docker-compose.trial.yml              # 试用环境
│   ├── docker-compose.production-single.yml  # 8C16G 生产
│   ├── nginx-trial.conf                      # 试用 nginx
│   ├── nginx-production.conf                 # 生产 nginx（含 /auth/ Keycloak 代理）
│   ├── keycloak/
│   │   ├── realm-export.json                 # Keycloak realm（客户端/角色/账号）
│   │   └── init-keycloak-db.sql              # PG 首次初始化时创建 keycloak 数据库
│   ├── .env.trial.example                    # 试用环境变量模板
│   ├── .env.production.example               # 生产环境变量模板
│   └── .env / .env.production                # 实际环境变量（不入库）
├── deployment-architecture.md                # 部署架构文档
└── deployment-manual.md                      # 本文件
```

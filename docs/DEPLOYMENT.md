# AI Studio 部署文档

> 最后更新：2026-01-20

## 目录

- [环境要求](#环境要求)
- [本地开发部署](#本地开发部署)
- [Docker 部署](#docker-部署)
- [生产环境部署](#生产环境部署)
- [数据库迁移](#数据库迁移)
- [故障排查](#故障排查)

---

## 环境要求

### 软件要求

- Java 21+
- Maven 3.9+
- PostgreSQL 15+ (with PGVector extension)
- Docker 24+ (可选)
- Docker Compose 2.20+ (可选)

### 硬件要求

**最低配置:**
- CPU: 2 cores
- 内存: 4GB
- 磁盘: 20GB

**推荐配置:**
- CPU: 4 cores
- 内存: 8GB
- 磁盘: 50GB

---

## 本地开发部署

### 1. 克隆代码

```bash
git clone https://github.com/mydotey-ai/ai-studio.git
cd ai-studio
```

### 2. 配置数据库

**安装 PostgreSQL 15+**

Ubuntu/Debian:
```bash
sudo apt update
sudo apt install postgresql-15 postgresql-contrib-15
```

macOS:
```bash
brew install postgresql@15
```

**安装 PGVector 扩展**

```bash
# Clone pgvector
git clone --branch v0.5.1 https://github.com/pgvector/pgvector.git
cd pgvector

# Build and install
make
sudo make install
```

**创建数据库**

```bash
# 切换到 postgres 用户
sudo -u postgres psql

# 创建数据库和用户
CREATE DATABASE ai_studio;
CREATE USER ai_studio WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE ai_studio TO ai_studio;

# 启用 PGVector 扩展
\c ai_studio
CREATE EXTENSION vector;
\q
```

### 3. 配置环境变量

复制示例配置文件:
```bash
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
```

编辑 `application-dev.yml`,配置数据库连接:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_studio
    username: ai_studio
    password: your_password
```

设置环境变量:
```bash
export JWT_SECRET=your-jwt-secret-key
export EMBEDDING_API_KEY=your-embedding-api-key
export LLM_API_KEY=your-llm-api-key
```

### 4. 运行应用

```bash
# 编译项目
mvn clean package

# 运行应用
java -jar target/ai-studio-1.0.0.jar --spring.profiles.active=dev
```

或使用 Maven:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 5. 验证部署

访问健康检查端点:
```bash
curl http://localhost:8080/actuator/health
```

访问 API 文档:
```
http://localhost:8080/swagger-ui.html
```

---

## Docker 部署

### 1. 准备环境文件

```bash
cp .env.example .env
```

编辑 `.env` 文件,设置必要的环境变量:
```env
POSTGRES_PASSWORD=your_secure_password
JWT_SECRET=your-jwt-secret-key
EMBEDDING_API_KEY=your-embedding-api-key
LLM_API_KEY=your-llm-api-key
```

### 2. 启动服务

**开发环境:**
```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

**生产环境:**
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### 3. 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f app
docker-compose logs -f postgres
```

### 4. 停止服务

```bash
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

### 5. 数据库迁移

Flyway 迁移会在应用启动时自动执行。查看迁移状态:
```bash
docker-compose exec app curl http://localhost:8080/actuator/flyway
```

---

## 生产环境部署

### 1. 服务器准备

**更新系统:**
```bash
sudo apt update && sudo apt upgrade -y
```

**安装 Docker:**
```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
```

**安装 Docker Compose:**
```bash
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. 配置防火墙

```bash
# 允许 SSH
sudo ufw allow 22/tcp

# 允许 HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# 启用防火墙
sudo ufw enable
```

### 3. 配置 Nginx 反向代理 (可选)

创建 Nginx 配置 `/etc/nginx/sites-available/ai-studio`:

```nginx
upstream ai_studio_backend {
    server localhost:8080;
}

server {
    listen 80;
    server_name api.example.com;

    client_max_body_size 100M;

    location / {
        proxy_pass http://ai_studio_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket support (for SSE)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location /swagger-ui {
        proxy_pass http://ai_studio_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

启用配置:
```bash
sudo ln -s /etc/nginx/sites-available/ai-studio /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 4. 配置 SSL (使用 Let's Encrypt)

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d api.example.com
```

### 5. 部署应用

```bash
# 克隆代码
git clone https://github.com/mydotey-ai/ai-studio.git
cd ai-studio

# 配置环境
cp .env.example .env
nano .env  # 编辑配置

# 启动服务
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### 6. 配置自动重启

创建 systemd service `/etc/systemd/system/ai-studio.service`:

```ini
[Unit]
Description=AI Studio Application
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/ai-studio
ExecStart=/usr/local/bin/docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
ExecStop=/usr/local/bin/docker-compose -f docker-compose.yml -f docker-compose.prod.yml down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
```

启用服务:
```bash
sudo systemctl enable ai-studio.service
sudo systemctl start ai-studio.service
```

---

## 数据库迁移

### Flyway 迁移

迁移文件位于 `src/main/resources/db/migration/`.

**手动执行迁移:**
```bash
docker-compose exec app mvn flyway:migrate
```

**查看迁移历史:**
```bash
docker-compose exec postgres psql -U postgres -d ai_studio -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"
```

**回滚迁移:**
Flyway 社区版不支持自动回滚。需要手动编写回滚脚本:
```bash
docker-compose exec postgres psql -U postgres -d ai_studio -f your_rollback_script.sql
```

### 数据库备份

**备份:**
```bash
docker-compose exec postgres pg_dump -U postgres ai_studio > backup_$(date +%Y%m%d_%H%M%S).sql
```

**恢复:**
```bash
docker-compose exec -T postgres psql -U postgres ai_studio < backup_20260120_120000.sql
```

---

## 故障排查

### 应用无法启动

**检查日志:**
```bash
docker-compose logs app
```

**检查数据库连接:**
```bash
docker-compose exec postgres psql -U postgres -c "SELECT version();"
```

**检查环境变量:**
```bash
docker-compose exec app env | grep SPRING
```

### 数据库连接失败

**验证 PGVector 扩展:**
```bash
docker-compose exec postgres psql -U postgres -d ai_studio -c "SELECT * FROM pg_extension WHERE extname = 'vector';"
```

**检查数据库监听:**
```bash
docker-compose exec postgres psql -U postgres -c "SELECT * FROM pg_stat_activity;"
```

### 内存不足

**调整 JVM 内存:**
```yaml
services:
  app:
    environment:
      JAVA_OPTS: "-Xms512m -Xmx1024m"
```

### 性能优化

**数据库索引:**
```sql
-- 查看慢查询
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

-- 分析表
ANALYZE document_chunks;
```

**应用监控:**
```bash
# 查看指标
curl http://localhost:8080/actuator/metrics

# Prometheus 指标
curl http://localhost:8080/actuator/prometheus
```

---

## 更新部署

### 滚动更新

```bash
# 拉取最新代码
git pull

# 重新构建镜像
docker-compose build

# 重启服务
docker-compose up -d

# 清理旧镜像
docker image prune -f
```

### 零停机更新 (使用健康检查)

```bash
# 启动新容器
docker-compose up -d --no-deps --scale app=2 app

# 等待新容器健康
sleep 30

# 停止旧容器
docker-compose up -d --no-deps --scale app=1 app
```

---

## 监控和日志

### 应用日志

```bash
# 实时日志
docker-compose logs -f app

# 最近 100 行
docker-compose logs --tail=100 app
```

### Prometheus 指标

访问 `http://localhost:8080/actuator/prometheus` 获取指标。

配置 Prometheus 抓取:

```yaml
scrape_configs:
  - job_name: 'ai-studio'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### 健康检查

```bash
curl http://localhost:8080/actuator/health
```

---

## 安全建议

1. **使用强密码**: 修改 `.env` 中的默认密码
2. **限制数据库访问**: 不要暴露 PostgreSQL 端口到公网
3. **启用 HTTPS**: 使用 Let's Encrypt 配置 SSL
4. **定期更新**: 保持系统和依赖包最新
5. **备份策略**: 设置定期数据库备份
6. **监控告警**: 配置 Prometheus AlertManager
7. **日志审计**: 启用审计日志并定期审查

---

## 联系支持

如有问题,请通过以下方式联系:
- Email: support@mydotey.ai
- GitHub Issues: https://github.com/mydotey-ai/ai-studio/issues

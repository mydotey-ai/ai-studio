# AI Studio 运维手册

> 最后更新：2026-01-20

## 目录

- [日常运维](#日常运维)
- [监控告警](#监控告警)
- [备份恢复](#备份恢复)
- [性能调优](#性能调优)
- [故障处理](#故障处理)
- [安全加固](#安全加固)

---

## 日常运维

### 服务状态检查

**检查服务运行状态:**
```bash
docker-compose ps
```

**检查服务健康状态:**
```bash
curl http://localhost:8080/actuator/health
```

**检查容器资源使用:**
```bash
docker stats
```

### 日志查看

**应用日志:**
```bash
# 实时日志
docker-compose logs -f app

# 最近 100 行
docker-compose logs --tail=100 app

# 带时间戳
docker-compose logs -t app
```

**数据库日志:**
```bash
docker-compose logs -f postgres
```

**按级别过滤:**
```bash
docker-compose exec app grep "ERROR" logs/ai-studio-error.log
```

### 数据库维护

**数据库大小检查:**
```sql
SELECT
    pg_database.datname,
    pg_size_pretty(pg_database_size(pg_database.datname)) AS size
FROM pg_database;
```

**表大小检查:**
```sql
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

**清理死锁:**
```sql
SELECT * FROM pg_stat_activity WHERE state = 'active';
-- 如果需要终止某个进程
-- SELECT pg_terminate_backend(pid);
```

**定期 VACUUM:**
```bash
docker-compose exec postgres psql -U postgres -d ai_studio -c "VACUUM ANALYZE;"
```

### 日志轮转

日志轮转已在 `logback-spring.xml` 中配置:

- 普通日志: 按天和大小 (100MB) 轮转,保留 30 天
- 错误日志: 按天和大小 (100MB) 轮转,保留 30 天

手动清理旧日志:
```bash
find logs/ -name "*.log" -mtime +30 -delete
```

---

## 监控告警

### Prometheus 监控

**关键指标:**

1. **HTTP 请求指标**
   - `http.server.requests` - HTTP 请求总数
   - `http.server.requests.latency` - 请求延迟

2. **JVM 指标**
   - `jvm.memory.used` - JVM 内存使用
   - `jvm.gc.pause` - GC 暂停时间
   - `jvm.threads.live` - 活跃线程数

3. **自定义业务指标**
   - `method.execution.time` - 方法执行时间
   - `errors.total` - 错误总数

**Prometheus 配置示例:**

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'ai-studio'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

### Grafana 仪表盘

推荐面板:

1. **应用概览**
   - 请求速率 (requests/sec)
   - 错误率 (%)
   - P50/P95/P99 延迟

2. **JVM 监控**
   - 堆内存使用
   - GC 时间
   - 线程数

3. **业务指标**
   - 文档上传数量
   - RAG 查询次数
   - Agent 执行次数

### 告警规则

**Prometheus AlertManager 规则:**

```yaml
groups:
  - name: ai-studio-alerts
    rules:
      # 应用不健康
      - alert: ApplicationDown
        expr: up{job="ai-studio"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "AI Studio 应用 down"
          description: "{{ $labels.instance }} 已经 down 超过 1 分钟"

      # 高错误率
      - alert: HighErrorRate
        expr: rate(errors_total[5m]) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "错误率过高"
          description: "错误率: {{ $value }} errors/sec"

      # 高内存使用
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "堆内存使用率过高"
          description: "堆内存使用率: {{ $value | humanizePercentage }}"

      # 高 GC 时间
      - alert: HighGcTime
        expr: rate(jvm_gc_pause_seconds_sum[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "GC 时间过长"
          description: "GC 时间占比: {{ $value | humanizePercentage }}"
```

---

## 备份恢复

### 自动备份脚本

创建 `scripts/backup.sh`:

```bash
#!/bin/bash

# 配置
BACKUP_DIR="/backup"
POSTGRES_CONTAINER="ai-studio-postgres"
DB_NAME="ai_studio"
DB_USER="postgres"
RETENTION_DAYS=7

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份文件名
BACKUP_FILE="$BACKUP_DIR/ai_studio_$(date +%Y%m%d_%H%M%S).sql"

# 执行备份
docker exec $POSTGRES_CONTAINER pg_dump -U $DB_USER $DB_NAME > $BACKUP_FILE

# 压缩备份
gzip $BACKUP_FILE

# 删除旧备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete

echo "Backup completed: ${BACKUP_FILE}.gz"
```

设置定时任务 (每天凌晨 2 点):
```bash
# 编辑 crontab
crontab -e

# 添加定时任务
0 2 * * * /path/to/scripts/backup.sh >> /var/log/ai-studio-backup.log 2>&1
```

### 数据恢复

**从备份恢复:**
```bash
# 解压备份文件
gunzip backup_file.sql.gz

# 停止应用
docker-compose stop app

# 恢复数据库
docker-compose exec -T postgres psql -U postgres -d ai_studio < backup_file.sql

# 重启应用
docker-compose start app
```

### 文件备份

备份上传文件:
```bash
tar -czf uploads_backup_$(date +%Y%m%d).tar.gz /path/to/uploads
```

---

## 性能调优

### JVM 调优

**推荐 JVM 参数:**

```yaml
environment:
  JAVA_OPTS: >
    -XX:+UseG1GC
    -XX:MaxRAMPercentage=75.0
    -XX:+UseStringDeduplication
    -XX:+ExitOnOutOfMemoryError
    -Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags
```

**参数说明:**
- `-XX:+UseG1GC`: 使用 G1 垃圾收集器
- `-XX:MaxRAMPercentage=75.0`: 使用容器内存的 75%
- `-XX:+UseStringDeduplication`: 字符串去重,减少内存
- `-XX:+ExitOnOutOfMemoryError`: OOM 时退出
- `-Xlog:gc*`: GC 日志

### 数据库调优

**PostgreSQL 配置优化 (`postgresql.conf`):**

```ini
# 连接设置
max_connections = 100
shared_buffers = 256MB
effective_cache_size = 1GB

# WAL 设置
wal_buffers = 16MB
checkpoint_completion_target = 0.9

# 查询优化
random_page_cost = 1.1
effective_io_concurrency = 200

# 日志设置
log_min_duration_statement = 1000  # 记录超过 1 秒的查询
```

**索引优化:**

```sql
-- 查看未使用的索引
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
AND indexname NOT LIKE '%_pkey';

-- 创建向量搜索索引 (如果不存在)
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding
ON document_chunks
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
```

### 应用调优

**数据库连接池配置:**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**异步配置:**

```yaml
spring:
  task:
    execution:
      pool:
        core-size: 8
        max-size: 16
        queue-capacity: 100
```

---

## 故障处理

### 常见问题

**1. 应用无法启动**

症状: `docker-compose up` 后容器立即退出

排查:
```bash
# 查看日志
docker-compose logs app

# 检查配置
docker-compose config

# 检查环境变量
docker-compose exec app env | grep SPRING
```

解决:
- 检查数据库连接
- 验证环境变量配置
- 查看应用日志

**2. 数据库连接池耗尽**

症状: 日志显示 "Connection pool exhausted"

解决:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30  # 增加连接池大小
      connection-timeout: 60000  # 增加超时时间
```

**3. 内存溢出 (OOM)**

症状: 容器被杀死,日志显示 "OutOfMemoryError"

解决:
```yaml
services:
  app:
    deploy:
      resources:
        limits:
          memory: 2G  # 增加内存限制
    environment:
      JAVA_OPTS: "-XX:MaxRAMPercentage=75.0"
```

**4. 响应缓慢**

排查:
```bash
# 查看慢查询
docker-compose exec postgres psql -U postgres -d ai_studio -c "
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
"

# 查看方法执行时间
curl http://localhost:8080/actuator/metrics/method.execution.time
```

**5. 磁盘空间不足**

排查:
```bash
# 查看磁盘使用
df -h

# 查看 Docker 占用
docker system df

# 清理未使用的镜像
docker system prune -a
```

### 紧急恢复流程

**应用完全 down:**
```bash
# 1. 尝试重启服务
docker-compose restart app

# 2. 如果失败,查看日志
docker-compose logs --tail=500 app

# 3. 检查数据库
docker-compose exec postgres psql -U postgres -c "SELECT 1;"

# 4. 从备份恢复 (如果需要)
./scripts/restore.sh
```

**数据库损坏:**
```bash
# 1. 停止应用
docker-compose stop app

# 2. 尝试修复
docker-compose exec postgres psql -U postgres -d ai_studio -c "REINDEX DATABASE ai_studio;"

# 3. 如果修复失败,从备份恢复
./scripts/restore.sh
```

---

## 安全加固

### 系统安全

**1. 限制容器权限:**

```yaml
services:
  app:
    security_opt:
      - no-new-privileges:true
    read_only: true
    tmpfs:
      - /tmp
```

**2. 使用非 root 用户:**

已在 Dockerfile 中配置 `spring:spring` 用户。

**3. 网络隔离:**

```yaml
networks:
  ai-studio-network:
    driver: bridge
    internal: true  # 禁止访问外网
```

### 应用安全

**1. 环境变量管理:**

使用 Docker Secrets 或环境变量文件,不要在代码中硬编码密钥。

**2. 定期更新依赖:**

```bash
# 检查过期依赖
mvn versions:display-dependency-updates

# 更新依赖
mvn versions:use-latest-releases
```

**3. 启用审计日志:**

审计日志已启用,定期检查:
```sql
SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 100;
```

### 数据库安全

**1. 定期备份:** 每天自动备份

**2. 限制远程访问:**
```bash
# 编辑 pg_hba.conf
host    all    all    0.0.0.0/0    reject
host    all    all    172.16.0.0/12    scram-sha-256
```

**3. 启用 SSL 连接:**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_studio?sslmode=require
```

---

## 联系支持

如有运维问题,请通过以下方式联系:
- Email: ops@mydotey.ai
- GitHub Issues: https://github.com/mydotey-ai/ai-studio/issues

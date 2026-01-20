# Docker Compose Deployment Guide

This guide explains how to use Docker Compose to deploy AI Studio in different environments.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- At least 4GB of available RAM
- 10GB of available disk space

## Quick Start

### 1. Environment Setup

Copy the example environment file and configure it:

```bash
cp .env.example .env
```

Edit `.env` and update the following variables:
- `POSTGRES_PASSWORD`: Set a strong password for PostgreSQL
- `JWT_SECRET`: Set a secure secret key (minimum 256 bits)
- `EMBEDDING_API_KEY`: Add your embedding service API key
- `LLM_API_KEY`: Add your LLM service API key

### 2. Development Environment

Start all services in development mode:

```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

View logs:

```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs -f
```

Stop services:

```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml down
```

### 3. Production Environment

For production deployment:

```bash
# Build the application image
docker build -t ai-studio:latest .

# Start production services
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## Services

The Docker Compose setup includes the following services:

### 1. PostgreSQL Database
- **Port**: 5432
- **Default Database**: ai_studio
- **Default User**: postgres
- **Volume**: ai-studio-postgres-data

### 2. AI Studio Application
- **Port**: 8080
- **Health Check**: http://localhost:8080/actuator/health
- **API Docs**: http://localhost:8080/swagger-ui.html
- **Metrics**: http://localhost:8080/actuator/prometheus

### 3. Prometheus
- **Port**: 9090
- **Web UI**: http://localhost:9090
- **Volume**: ai-studio-prometheus-data

### 4. Grafana
- **Port**: 3000
- **Web UI**: http://localhost:3000
- **Default Credentials**: admin/admin (change on first login)
- **Volume**: ai-studio-grafana-data

## Common Operations

### View Service Status

```bash
docker-compose ps
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f ai-studio
```

### Restart Services

```bash
docker-compose restart
```

### Scale Application (Production)

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --scale ai-studio=3
```

### Clean Up Everything

```bash
# Stop and remove containers, networks
docker-compose down

# Stop and remove containers, networks, and volumes
docker-compose down -v
```

## Health Checks

Check application health:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

## Monitoring

### Prometheus Metrics

Access Prometheus at http://localhost:9090

Example queries:
- JVM Memory: `jvm_memory_used_bytes`
- HTTP Requests: `http_server_requests_seconds_count`
- Custom Metrics: Check application-specific metrics

### Grafana Dashboards

Access Grafana at http://localhost:3000

1. Login with admin/admin
2. Change password on first login
3. Explore pre-configured dashboards
4. Create custom dashboards using Prometheus as datasource

## Troubleshooting

### Container Won't Start

1. Check logs:
   ```bash
   docker-compose logs [service-name]
   ```

2. Verify environment variables in `.env`

3. Ensure ports are not already in use:
   ```bash
   netstat -tulpn | grep -E '(8080|5432|9090|3000)'
   ```

### Database Connection Issues

1. Verify PostgreSQL is healthy:
   ```bash
   docker-compose exec postgres pg_isready -U postgres
   ```

2. Check database logs:
   ```bash
   docker-compose logs postgres
   ```

### Out of Memory

1. Check resource usage:
   ```bash
   docker stats
   ```

2. Adjust memory limits in `docker-compose.prod.yml`

### Data Persistence

All data is stored in named Docker volumes:
- `ai-studio-postgres-data`: Database files
- `ai-studio-upload-data`: Uploaded files
- `ai-studio-log-data`: Application logs
- `ai-studio-prometheus-data`: Metrics data
- `ai-studio-grafana-data`: Dashboard configurations

Backup volumes:

```bash
docker run --rm -v ai-studio-postgres-data:/data -v $(pwd):/backup alpine tar czf /backup/postgres-backup.tar.gz -C /data .
```

## Production Considerations

### Security

1. Change all default passwords in `.env`
2. Use strong, random JWT secrets
3. Enable HTTPS/TLS for external access
4. Restrict network access using firewall rules
5. Regular security updates for Docker images

### Performance

1. Configure appropriate resource limits in production compose file
2. Enable and configure connection pooling
3. Use external managed database services if needed
4. Implement backup and disaster recovery procedures

### Monitoring

1. Set up alerting in Prometheus/Grafana
2. Monitor JVM metrics, database connections, and HTTP endpoints
3. Configure log aggregation (e.g., ELK stack)
4. Regular performance testing

## Environment-Specific Configurations

### Development
- Hot reload enabled
- Debug port exposed (5005)
- Verbose logging
- Lower resource limits
- Local volumes for easy cleanup

### Production
- Resource limits configured
- Health checks enabled
- Logging configured with rotation
- Multiple replicas supported
- Persistent volume configurations
- Optimized JVM settings

## Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)

# Quick Start - Docker Compose

## Development Environment

```bash
# 1. Copy environment file
cp .env.example .env

# 2. Edit .env with your configuration
# Important: Change JWT_SECRET, EMBEDDING_API_KEY, LLM_API_KEY

# 3. Start services
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# 4. Check status
docker-compose ps

# 5. View logs
docker-compose logs -f

# 6. Access services
# Application: http://localhost:8080
# API Docs: http://localhost:8080/swagger-ui.html
# Health: http://localhost:8080/actuator/health
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (admin/admin)

# 7. Stop services
docker-compose -f docker-compose.yml -f docker-compose.dev.yml down
```

## Production Environment

```bash
# 1. Build image
docker build -t ai-studio:latest .

# 2. Copy and configure environment
cp .env.example .env
# Edit .env with production values

# 3. Start services
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 4. Check health
curl http://localhost:8080/actuator/health
```

## Useful Commands

```bash
# View logs
docker-compose logs -f [service-name]

# Restart service
docker-compose restart [service-name]

# Scale application
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --scale ai-studio=3

# Remove all (including volumes)
docker-compose down -v

# Validate configuration
docker-compose config
```

## Troubleshooting

```bash
# Check container logs
docker-compose logs ai-studio

# Check database connection
docker-compose exec postgres pg_isready -U postgres

# Access container shell
docker-compose exec ai-studio sh

# View resource usage
docker stats
```

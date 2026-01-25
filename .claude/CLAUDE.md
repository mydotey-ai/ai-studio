# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## AI Studio Codebase Overview

AI Studio is an AI Development Studio for Knowledge Base, RAG, Agent, and Chatbot development. It's a full-stack application with a Spring Boot backend and Vue 3 frontend.

## Repository Structure

```
ai-studio/
├── src/                     # Backend source code (Java/Spring Boot)
│   ├── main/java/com/mydotey/ai/studio/
│   │   ├── controller/     # API controllers
│   │   ├── service/        # Business logic services
│   │   ├── entity/         # Data models
│   │   ├── dto/            # Data transfer objects
│   │   ├── config/         # Configuration classes
│   │   ├── security/       # Security and authentication
│   │   └── ...
│   └── test/               # Test files
├── frontend/                # Frontend source code (Vue 3 + TypeScript)
│   ├── src/
│   │   ├── api/            # API calls
│   │   ├── components/     # Reusable Vue components
│   │   ├── views/          # Page components
│   │   ├── router/         # Routing configuration
│   │   ├── stores/         # Pinia state management
│   │   └── ...
│   ├── package.json        # Frontend dependencies
│   └── vite.config.ts      # Vite configuration
├── pom.xml                 # Maven project configuration
├── docker-compose.yml      # Docker Compose base configuration
├── docker-compose.dev.yml  # Development environment configuration
├── docker-compose.prod.yml # Production environment configuration
├── start.sh                # One-click startup script
├── start-local.sh          # Local development script (alternative to Docker)
├── .env.example            # Environment variables example
└── README.md               # Project documentation
```

## Key Technologies

### Backend (Spring Boot 3.5.0)
- **Java 21** - Programming language
- **Spring Boot** - Application framework
- **PostgreSQL** - Database
- **MyBatis Plus** - ORM framework
- **JWT** - Authentication
- **Redis** - Caching
- **Flyway** - Database migrations
- **Prometheus** - Metrics collection
- **Grafana** - Monitoring dashboards

### Frontend (Vue 3)
- **Vue 3** - UI framework
- **TypeScript** - Type safety
- **Element Plus** - UI component library
- **Pinia** - State management
- **Vue Router** - Routing
- **Vite** - Build tool
- **Axios** - HTTP client
- **ECharts** - Data visualization

## Testing

### Test Configuration
- **Test Database**: PostgreSQL (configured in `application-test.yml`)
  - Test Database URL: jdbc:postgresql://localhost:5432/ai_studio_test
  - Test Username: postgres
  - Test Password: xx123456XX
- **Test Frameworks**: JUnit 5, Mockito, Spring Test
- **Test Location**: `src/test/java/` for backend tests

### Running Tests
```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=ClassNameTest

# Run tests with coverage report
mvn jacoco:report
```

### Test Best Practices
- Write unit tests for all business logic
- Use @DataJpaTest for repository layer tests
- Use @SpringBootTest for integration tests
- Follow Given-When-Then pattern in test names
- Mock external dependencies in unit tests

## Development Commands

### Backend (Maven)
```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run

# Run tests (covered in Testing section)
# mvn test

# Check for dependency updates
mvn versions:display-dependency-updates
```

### Frontend (npm)
```bash
# Install dependencies
cd frontend && npm install

# Start development server (hot reload)
cd frontend && npm run dev

# Build for production
cd frontend && npm run build

# Preview production build
cd frontend && npm run preview

# Lint and fix files
cd frontend && npm run lint

# Format files with Prettier
cd frontend && npm run format
```

### Docker Compose (Quick Start)
```bash
# One-click startup (development environment)
./start.sh start

# One-click startup (production environment)
./start.sh start prod

# Stop all services
./start.sh stop

# Restart services
./start.sh restart

# Check service status
./start.sh status

# View logs (all services)
./start.sh logs

# View logs (specific service)
./start.sh logs ai-studio

# Build production image
./start.sh build

# Display help
./start.sh help
```

### Local Development (start-local.sh)
```bash
# 启动开发环境（前后端分离）
./start-local.sh start

# 只启动后端服务
./start-local.sh start-backend

# 只启动前端开发服务器
./start-local.sh start-frontend

# 重启所有服务
./start-local.sh restart

# 重启后端服务
./start-local.sh restart-backend

# 重启前端服务
./start-local.sh restart-frontend

# 停止所有服务
./start-local.sh stop

# 检查服务状态
./start-local.sh status

# 查看日志
./start-local.sh logs backend    # 查看后端日志（实时）
./start-local.sh logs frontend   # 查看前端日志（实时）
./start-local.sh logs all        # 查看所有日志

# 编译后端（跳过测试）
./start-local.sh build

# 显示帮助信息
./start-local.sh help
```

**注意事项：**
- 开发测试时使用 `start-local.sh` 脚本
- 前后端分离架构：后端运行在 8080 端口，前端开发服务器运行在 3000 端口
- 前端开发服务器会自动将 `/api` 请求代理到后端 8080 端口
- 访问应用时使用 http://localhost:3000
- 前端支持热重载，代码修改后会自动刷新

### Docker Compose (Direct Commands)
```bash
# Start development environment
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Start production environment
docker build -t ai-studio:latest .
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f

# Check container status
docker-compose ps
```

## Build and Deployment

### Local Development (推荐)
1. 确保已安装 Java 21、Maven、Node.js、npm、PostgreSQL 和 Redis
2. 确保 PostgreSQL 和 Redis 服务正在运行
3. 运行 `./start-local.sh start` 启动开发环境
4. 访问应用：http://localhost:3000
5. 默认登录账号：admin / 123456

### Docker Compose Development
1. Ensure Docker and Docker Compose are installed
2. Copy `.env.example` to `.env` and configure environment variables
3. Run `./start.sh start` to start the development environment
4. Access the application at http://localhost:8080

### Environment Variables
Key variables to configure in `.env`:
- `POSTGRES_PASSWORD`: PostgreSQL password
- `JWT_SECRET`: JWT signing secret
- `EMBEDDING_API_KEY`: Embedding service API key
- `LLM_API_KEY`: LLM service API key

### PostgreSQL Test Configuration
PostgreSQL test database configuration is located in `application-test.yml`:
- Test Database URL: jdbc:postgresql://localhost:5432/ai_studio_test
- Test Username: postgres
- Test Password: xx123456XX

## API Documentation

API documentation is available at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Configuration Files

### Application Configuration
- `application.yml` - Main application configuration
- `application-dev.yml` - Development environment configuration
- `application-test.yml` - Test environment configuration (includes PostgreSQL test settings)
- `application-prod.yml` - Production environment configuration

### Docker Configuration
- `docker-compose.yml` - Base Docker Compose configuration
- `docker-compose.dev.yml` - Development environment overrides
- `docker-compose.prod.yml` - Production environment overrides

## Monitoring and Health Checks

- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/prometheus
- **Prometheus UI**: http://localhost:9090
- **Grafana Dashboards**: http://localhost:3000 (admin/admin)

## Services Architecture

### Core Backend Services
- **KnowledgeBaseService**: Manage knowledge bases and document storage
- **DocumentProcessingService**: Parse and process uploaded documents
- **TextChunkingService**: Split documents into text chunks for indexing
- **VectorSearchService**: Vector search and retrieval
- **RagService**: RAG (Retrieval-Augmented Generation) operations
- **LlmGenerationService**: LLM (Large Language Model) integration
- **ChatService**: Chat and conversation management
- **AgentService**: Agent and workflow management
- **UserService**: User authentication and authorization

### Frontend Pages/Views
- Dashboard: Analytics and system overview
- Knowledge Base: Manage knowledge bases and documents
- Chat: Interactive chat interface
- Agents: Agent configuration and management
- Settings: System configuration

## Data Storage

### Database (PostgreSQL)
- Stores application data (users, knowledge bases, documents, etc.)
- Uses Flyway for schema migrations

### File Storage
- **Local storage** (default): Stores files on local disk
- **AWS S3**: Cloud storage integration
- **Aliyun OSS**: Alibaba Cloud OSS integration
- Files are automatically processed and indexed

## Key Features

1. **Knowledge Base Management**: Create and manage knowledge bases
2. **Document Upload**: Support for PDF, Word, Excel, and other formats
3. **Text Chunking**: Automatic text chunking for optimal retrieval
4. **Vector Search**: Semantic search using embeddings
5. **RAG (Retrieval-Augmented Generation)**: LLM responses with context from knowledge base
6. **Chat Interface**: Interactive chat with knowledge base content
7. **Agents**: Configurable agents with workflows
8. **Monitoring**: Prometheus + Grafana for system monitoring
9. **Security**: JWT authentication, rate limiting, CORS configuration

## Development Workflow

1. **Branch**: Create a new branch for each feature or bug fix
2. **Code**: Implement changes following existing patterns
3. **Test**: Write unit tests for backend changes (see Testing section)
4. **Lint**: Run frontend linting and formatting
5. **Commit**: Use meaningful commit messages
6. **Push**: Push changes to remote repository
7. **PR**: Create a pull request for review

## Debugging

### Backend Debugging
- Debug port: 5005 (exposed in development environment)
- Add remote debugging configuration in IDE
- Connect to localhost:5005

### Logging
- Logs are written to `logs/` directory
- Log levels: DEBUG, INFO, WARN, ERROR
- Structured logging in JSON format (Logstash)

### Frontend Debugging
- Use browser DevTools (F12)
- Vue DevTools browser extension for Vue debugging
- Network tab to inspect API calls

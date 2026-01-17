# AI Studio 项目进度

> 最后更新：2026-01-17

## 项目概述

AI Studio 是一个基于 Spring Boot 3.5 + MyBatis-Plus 的 AI 开发平台，支持知识库、RAG、Agent 和聊天机器人。

**技术栈：**
- Java 21
- Spring Boot 3.5.0
- MyBatis-Plus 3.5.7
- PostgreSQL (PGVector 扩展)
- JWT (io.jsonwebtoken 0.12.5)
- BCrypt 密码加密
- Apache POI (Office 文档处理)
- Apache PDFBox (PDF 文档处理)
- Jsoup (网页抓取)

---

## 已完成阶段

### Phase 1: 基础架构 ✅

**完成时间：2025-01-16**

**实现内容：**
- 项目初始化和配置
- 数据库设计（V1 迁移）
- 用户实体和组织实体
- MyBatis-Plus 配置
- 全局异常处理
- 基础 API 响应结构

**数据库表：**
- `organizations` - 组织表
- `users` - 用户表（包含 org_id 外键）
- `knowledge_bases` - 知识库表
- `kb_members` - 知识库成员表
- `documents` - 文档表
- `document_chunks` - 文档分块表（包含 PGVector embedding 列）
- `mcp_servers` - MCP 服务器表
- `mcp_tools` - MCP 工具表
- `agents` - Agent 表
- `agent_knowledge_bases` - Agent 知识库关联表
- `agent_tools` - Agent 工具关联表
- `chatbots` - 聊天机器人表
- `conversations` - 对话表
- `messages` - 消息表
- `web_crawl_tasks` - 网页抓取任务表
- `web_pages` - 网页表
- `api_keys` - API 密钥表
- `audit_logs` - 审计日志表
- `file_storage_config` - 文件存储配置表
- `settings` - 系统设置表

**索引优化：**
- `idx_document_chunks_embedding` - PGVector IVFFlat 索引（余弦相似度）

---

### Phase 2: 文档处理 ✅

**完成时间：2025-01-17**

**实现内容：**
- 文档上传控制器（异步处理）
- 文档解析服务
- 文本分块服务
- Embedding 服务（OpenAI API）
- 文档处理状态管理
- 集成测试

**新增文件：**
```
src/main/java/com/mydotey/ai/studio/
├── controller/DocumentController.java
├── service/
│   ├── DocumentService.java
│   ├── TextChunkingService.java
│   └── EmbeddingService.java
├── service/parser/
│   ├── DocumentParser.java (interface)
│   ├── PdfDocumentParser.java
│   ├── TextDocumentParser.java
│   └── WordDocumentParser.java
└── dto/
    ├── DocumentUploadRequest.java
    ├── DocumentUploadResponse.java
    ├── ChunkingStrategy.java
    └── DocumentChunkingConfig.java

src/main/resources/
└── mapper/DocumentMapper.xml
```

**配置项：**
```yaml
embedding:
  endpoint: https://api.openai.com/v1
  api-key: ${EMBEDDING_API_KEY}
  model: text-embedding-ada-002
  dimension: 1536
  batch-size: 100
  timeout: 30000

file:
  upload-dir: /tmp/ai-studio-uploads
```

**测试覆盖：**
- `DocumentProcessingIntegrationTest` - 端到端集成测试
- 单元测试：PdfDocumentParserTest, WordDocumentParserTest, TextChunkingServiceTest, EmbeddingServiceTest

**已处理文件类型：**
- PDF (.pdf) - Apache PDFBox
- Word (.doc, .docx) - Apache POI
- 纯文本 (.txt) - 原生处理

**分块策略：**
- 递归字符分块（默认，chunk_size=500, overlap=100）
- 未来可扩展：语义分块、句法分块

---

### Phase 3: 用户认证和权限管理 ✅

**完成时间：2025-01-17**

**实现内容：**
- JWT 认证系统（access token + refresh token）
- 刷新令牌机制
- 登录失败锁定（5 次失败锁定 15 分钟）
- 完整的用户管理 CRUD
- 组织管理（一人一组织规则）
- 方法级权限控制（@RequireRole 注解 + AOP）
- 审计日志系统（@AuditLog 注解 + AOP）
- 数据库迁移

**新增文件：**
```
src/main/java/com/mydotey/ai/studio/
├── service/
│   ├── AuthService.java
│   ├── RefreshTokenService.java
│   ├── LoginAttemptService.java
│   ├── UserService.java
│   └── OrganizationService.java
├── util/
│   ├── JwtUtil.java
│   └── PasswordUtil.java
├── entity/
│   ├── RefreshToken.java
│   └── LoginAttempt.java
├── mapper/
│   ├── RefreshTokenMapper.java
│   └── LoginAttemptMapper.java
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── RegisterRequest.java
│   ├── RefreshTokenRequest.java
│   ├── UserResponse.java
│   ├── UpdateUserRequest.java
│   ├── OrganizationResponse.java
│   └── CreateOrganizationRequest.java
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   └── OrganizationController.java
├── annotation/
│   ├── RequireRole.java
│   └── AuditLog.java
├── aspect/
│   ├── PermissionAspect.java
│   └── AuditLogAspect.java
├── common/exception/
│   └── AuthException.java
└── integration/
    └── AuthAndPermissionIntegrationTest.java

src/main/resources/
├── mapper/
│   └── RefreshTokenMapper.xml
└── db/migration/
    └── V3__auth_permission_tables.sql

pom.xml - 新增依赖：
- jjwt-api/impl/jackson (0.12.5)
- spring-security-crypto (BCrypt)
- spring-boot-starter-aop
```

**数据库表（新增）：**
```
refresh_tokens - 刷新令牌表
  ├─ id (BIGSERIAL PK)
  ├─ user_id (BIGINT FK → users ON DELETE CASCADE)
  ├─ token (VARCHAR UNIQUE)
  ├─ expires_at (TIMESTAMP NOT NULL)
  ├─ is_revoked (BOOLEAN DEFAULT FALSE)
  ├─ created_at, updated_at (TIMESTAMP)
  └─ 索引: user_id, token, expires_at

login_attempts - 登录尝试表
  ├─ id (BIGSERIAL PK)
  ├─ identifier (VARCHAR NOT NULL) - 用户名
  ├─ user_id (BIGINT FK → users ON DELETE SET NULL)
  ├─ attempt_count (INT NOT NULL DEFAULT 0)
  ├─ last_attempt_at (TIMESTAMP NOT NULL)
  ├─ locked_until (TIMESTAMP)
  ├─ created_at, updated_at (TIMESTAMP)
  └─ 索引: identifier
```

**API 端点：**

认证 API (`/api/auth/*`)：
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/refresh` - 刷新访问令牌
- `POST /api/auth/logout` - 用户登出

用户管理 API (`/api/users/*`)：
- `GET /api/users/{id}` - 获取用户详情
- `GET /api/users` - 获取所有用户（管理员）
- `PUT /api/users/{id}` - 更新用户信息
- `PATCH /api/users/{id}/status` - 更新用户状态（管理员）
- `DELETE /api/users/{id}` - 删除用户（管理员）

组织管理 API (`/api/organizations/*`)：
- `POST /api/organizations` - 创建组织
- `GET /api/organizations/{id}` - 获取组织详情
- `GET /api/organizations/my` - 获取当前用户的组织
- `PUT /api/organizations/{id}` - 更新组织（管理员）

**配置项：**
```yaml
jwt:
  secret: ${JWT_SECRET:your-super-secret-key-change-this-in-production-minimum-256-bits}
  access-token-expiration: 7200000  # 2 小时
  refresh-token-expiration: 604800000  # 7 天
  issuer: ai-studio

auth:
  max-login-attempts: 5
  lock-duration: 900000  # 15 分钟
```

**权限注解：**
```java
@RequireRole({"ADMIN", "SUPER_ADMIN"})  // 任一角色满足即可
@RequireRole(value = {"ADMIN"}, requireAll = true)  // 必须满足所有角色
```

**审计日志注解：**
```java
@AuditLog(action = "USER_LOGIN", resourceType = "User")
@AuditLog(action = "USER_UPDATE", resourceType = "User", resourceIdParam = "id")
```

**审计日志字段：**
- user_id - 操作用户 ID
- action - 操作类型（如 USER_LOGIN, USER_UPDATE）
- resource_type - 资源类型
- resource_id - 资源 ID
- details - 操作详情（JSONB）
- ip_address - IP 地址
- user_agent - 用户代理
- created_at - 创建时间

**用户角色：**
- `USER` - 普通用户
- `ADMIN` - 管理员
- `SUPER_ADMIN` - 超级管理员

**安全特性：**
- BCrypt 密码哈希
- JWT HMAC SHA256 签名
- 刷新令牌可撤销
- 登录失败锁定（防暴力破解）
- 密码修改需验证当前密码
- 方法级权限控制
- SQL 注入防护（MyBatis 参数化查询）

**测试覆盖：**
- `UserServiceTest` - 用户服务单元测试（3 个测试）
- `AuthAndPermissionIntegrationTest` - 认证和权限集成测试（6 个测试）
  - testRegistrationAndLoginFlow
  - testTokenRefreshFlow
  - testLoginWithNonExistentUser
  - testLoginWithWrongPassword
  - testInvalidRefreshToken
  - testJwtTokenContainsUserInfo

**代码审查问题修复：**
- 添加 @Transactional 到 AuthService 方法
- 添加 currentPassword 字段到 UpdateUserRequest
- 密码修改前验证当前密码
- 修复集成测试使用唯一用户名

---

## 当前状态

**Git 状态：**
- 分支：main
- 远程：origin/main（已同步）
- 工作树：干净

**测试状态：**
- 总测试数：22
- 通过：22
- 失败：0
- 跳过：0

---

## 下一步计划

### Phase 4: RAG 系统（规划中）

**预计功能：**
- RAG 查询服务
- 向量相似度搜索
- 上下文检索
- Prompt 构建器
- 响应生成

**依赖：**
- Embedding 服务（✅ 已完成）
- 文档分块服务（✅ 已完成）
- 知识库管理（✅ 已完成）

### Phase 5: Agent 系统（规划中）

**预计功能：**
- Agent 执行引擎
- 工具调用（MCP）
- 工作流管理（ReAct, 自定义）
- Agent 状态管理

### Phase 6: 聊天机器人（规划中）

**预计功能：**
- 聊天机器人管理
- 对话管理
- 消息历史
- 流式响应
- API 端点

---

## 技术债务

1. **缓存策略**
   - [ ] 添加用户角色缓存（减少重复查询）
   - [ ] 添加知识库元数据缓存
   - [ ] 添加 Embedding 结果缓存

2. **测试改进**
   - [ ] 添加 testcontainers 支持隔离集成测试
   - [ ] 添加安全测试（SQL 注入、XSS）
   - [ ] 添加性能测试
   - [ ] 提高测试覆盖率目标

3. **监控和日志**
   - [ ] 添加 APM 监控
   - [ ] 结构化日志
   - [ ] 请求追踪（trace ID）

4. **文档**
   - [ ] API 文档（Swagger/OpenAPI）
   - [ ] 部署文档
   - [ ] 运维手册

5. **性能优化**
   - [ ] 分页查询优化
   - [ ] 批量操作优化
   - [ ] 数据库连接池调优
   - [ ] PGVector 索引调优

---

## 环境配置

**开发环境** (application-dev.yml):
```
数据库：PostgreSQL @ localhost:5432/ai_studio
用户：postgres
密码：xx123456XX
Embedding API：OpenAI (环境变量 EMBEDDING_API_KEY)
文件上传：/tmp/ai-studio-uploads
JWT Secret：环境变量 JWT_SECRET
```

**测试环境** (application-test.yml):
```
数据库：PostgreSQL @ localhost:5432/ai_studio_test
```

---

## 代码规范

- 使用 Lombok 减少样板代码
- 使用 @RequiredArgsConstructor 进行构造器注入
- 使用 @Transactional 进行事务管理
- 使用 @Valid 进行请求参数验证
- 使用自定义异常类统一错误处理
- DTO 和 Entity 分离
- AOP 处理横切关注点（权限、审计）

# AI Studio 项目进度

> 最后更新：2026-01-19

## 项目概述

AI Studio 是一个基于 Spring Boot 3.5 + MyBatis-Plus 的 AI 开发平台，支持知识库、RAG、Agent、聊天机器人和网页抓取。

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

### Phase 4: RAG 系统 ✅

**完成时间：2026-01-18**

**实现内容：**
- RAG 查询 DTOs 和请求模型
- 向量相似度搜索服务（PGVector）
- 上下文构建服务
- LLM 服务配置
- Prompt 模板服务
- LLM 生成服务
- RAG 编排服务
- RAG 控制器
- 流式 RAG 响应（SSE）
- 完整测试覆盖（32 个测试，全部通过）

**新增文件：**
```
src/main/java/com/mydotey/ai/studio/
├── controller/
│   └── RagController.java
├── service/
│   ├── RagService.java
│   ├── VectorSearchService.java
│   ├── ContextBuilderService.java
│   ├── PromptTemplateService.java
│   ├── LlmGenerationService.java
│   └── StreamingLlmService.java
├── config/
│   ├── LlmConfig.java
│   └── WebConfig.java
├── dto/
│   ├── RagQueryRequest.java
│   ├── RagQueryResponse.java
│   ├── SourceDocument.java
│   ├── Message.java
│   ├── LlmRequest.java
│   └── LlmResponse.java
├── mapper/
│   └── DocumentChunkMapper.java
└── integration/
    └── RagIntegrationTest.java (待完善)

src/main/resources/
└── mapper/
    └── DocumentChunkMapper.xml

src/test/java/com/mydotey/ai/studio/
└── service/
    └── RagServiceTest.java
```

**配置项：**
```yaml
llm:
  endpoint: https://api.openai.com/v1
  api-key: ${LLM_API_KEY:your-api-key-here}
  model: gpt-3.5-turbo
  default-temperature: 0.3
  default-max-tokens: 1000
  timeout: 60000
  enable-streaming: true
```

**API 端点：**

RAG 查询 API (`/api/rag/*`)：
- `POST /api/rag/query` - 执行 RAG 查询（非流式）
- `POST /api/rag/query/stream` - 执行 RAG 查询（流式 SSE）

**实现任务完成情况：**

1. ✅ **RAG 查询 DTOs**
   - RagQueryRequest - 支持问题、知识库 ID 列表、topK、相似度阈值、对话历史、温度、最大 tokens
   - RagQueryResponse - 返回答案、来源、模型、tokens、完成标志
   - SourceDocument - 文档 ID、名称、分块索引、内容、相似度分数
   - Message - 对话消息（角色、内容）

2. ✅ **向量相似度搜索服务**
   - DocumentChunkMapper - PGVector 向量查询，使用余弦相似度排序
   - VectorSearchService - 协调 Embedding 生成和向量搜索
   - DocumentChunk 实体新增 similarityScore 字段（仅用于查询结果）

3. ✅ **上下文构建服务**
   - ContextBuilderService - 组装知识库内容和对话历史
   - 支持来源文档格式化（来源编号、文档名称、分块索引）
   - 支持对话历史截断（最多 5 轮）
   - 区分无来源和无历史场景

4. ✅ **LLM 服务配置**
   - LlmConfig - 支持自定义端点、API 密钥、模型、默认温度、默认最大 tokens、超时、流式开关
   - WebConfig - 提供 RestTemplate Bean

5. ✅ **Prompt 模板服务**
   - PromptTemplateService - 构建系统和用户提示词
   - 区分有/无相关文档的场景（无相关文档时明确提示）
   - 支持消息列表 JSON 构建
   - JSON 字符串转义处理

6. ✅ **LLM 生成服务**
   - LlmGenerationService - 调用 OpenAI 兼容 API
   - 支持非流式生成
   - 解析 usage 信息（总 tokens）
   - 错误处理和日志记录

7. ✅ **RAG 编排服务**
   - RagService - 协调向量搜索、上下文构建、Prompt 和 LLM 生成
   - 完整的端到端 RAG 流程（检索 → 构建 → 生成）
   - 支持是否返回来源选项
   - 支持温度和最大 tokens 参数

8. ✅ **RAG 控制器**
   - RagController - 提供非流式 REST API
   - 集成审计日志注解 @AuditLog
   - 请求参数验证 @Valid

9. ✅ **流式 RAG 响应**
   - StreamingLlmService - 流式 LLM 生成
   - SSE 端点 `POST /api/rag/query/stream`
   - 支持 StreamCallback 接口（onContent、onComplete、onError）
   - 实时推送内容，最终发送 [DONE] 标记

10. ✅ **RAG 测试覆盖**（已完成）
    - RagIntegrationTest - 端到端集成测试（3 个测试）
    - RagServiceTest - Rag 服务单元测试（1 个测试）
    - VectorSearchServiceTest - 向量搜索服务单元测试（4 个测试）
    - ContextBuilderServiceTest - 上下文构建服务单元测试（2 个测试）
    - PromptTemplateServiceTest - Prompt 模板服务单元测试（11 个测试）
    - LlmGenerationServiceTest - LLM 生成服务单元测试（4 个测试）
    - StreamingLlmServiceTest - 流式 LLM 服务单元测试（5 个测试）
    - RagControllerTest - RAG 控制器单元测试（2 个测试）

**测试统计：**
- Phase 4 总测试数：32 个
- 全部通过：32 ✅
- 失败：0
- 错误：0

**测试覆盖的服务：**
- ✅ VectorSearchService - 向量相似度搜索、PGVector 查询、Embedding 生成
- ✅ ContextBuilderService - 上下文构建、来源格式化、对话历史处理
- ✅ PromptTemplateService - 系统和用户提示词构建、消息列表 JSON 构建
- ✅ LlmGenerationService - LLM API 调用、响应解析、usage 信息、错误处理
- ✅ StreamingLlmService - SSE 流式响应、[DONE] 标记处理、错误回调
- ✅ RagService - 端到端 RAG 编排
- ✅ RagController - 请求验证、控制器集成
- ✅ RagIntegration - 完整 RAG 流程集成测试

**技术栈：**
- PGVector (PostgreSQL) - 向量存储和检索
- OpenAI Compatible API - LLM 生成
- Spring SSE - 流式响应
- MyBatis Plus - 数据访问

**核心功能：**
- 向量相似度搜索（余弦相似度）
- 上下文构建（知识库内容 + 对话历史）
- Prompt 模板管理
- LLM 集成（OpenAI 兼容）
- 流式响应（SSE）
- 多轮对话支持

---

### Phase 5: Agent 系统 ✅

**完成时间：2026-01-18**

**实现内容：**
- MCP 服务器管理（STDIO + HTTP 连接）
- MCP 工具发现和同步
- ReAct 工作流执行器
- Agent 执行引擎
- Agent CRUD API
- Agent 执行 API

**新增文件：**
```
src/main/java/com/mydotey/ai/studio/
├── entity/
│   ├── McpServer.java
│   ├── McpTool.java
│   ├── Agent.java
│   ├── AgentKnowledgeBase.java
│   └── AgentTool.java
├── mapper/
│   ├── McpServerMapper.java
│   ├── McpToolMapper.java
│   ├── AgentMapper.java
│   ├── AgentKnowledgeBaseMapper.java
│   └── AgentToolMapper.java
├── dto/
│   ├── mcp/
│   │   ├── McpServerDto.java
│   │   ├── McpToolDto.java
│   │   ├── JsonRpcRequest.java
│   │   ├── JsonRpcResponse.java
│   │   ├── JsonRpcNotification.java
│   │   ├── Tool.java
│   │   └── TextContent.java
│   └── agent/
│       ├── CreateAgentRequest.java
│       ├── UpdateAgentRequest.java
│       ├── AgentResponse.java
│       ├── AgentExecutionRequest.java
│       └── AgentExecutionResponse.java
├── service/
│   ├── McpServerService.java
│   ├── AgentService.java
│   ├── AgentExecutionService.java
│   ├── mcp/
│   │   ├── McpTransport.java (interface)
│   │   ├── McpStdioTransport.java
│   │   ├── McpHttpTransport.java
│   │   └── McpClient.java
│   └── agent/
│       ├── ToolExecutor.java
│       ├── ReActWorkflowExecutor.java
│       └── AgentEngine.java
└── controller/
    └── AgentController.java

src/main/resources/
└── mapper/
    ├── McpServerMapper.xml
    ├── McpToolMapper.xml
    ├── AgentMapper.xml
    ├── AgentKnowledgeBaseMapper.xml
    └── AgentToolMapper.xml

src/test/java/com/mydotey/ai/studio/
├── service/
│   ├── McpServerServiceTest.java
│   ├── AgentServiceTest.java
│   ├── mcp/
│   │   ├── McpClientTest.java
│   │   └── McpJsonRpcClientTest.java
│   └── agent/
│       ├── ToolExecutorTest.java
│       ├── ReActWorkflowExecutorTest.java
│       └── AgentEngineTest.java
├── controller/
│   └── AgentControllerTest.java
└── integration/
    └── AgentSystemIntegrationTest.java
```

**配置项：**
```yaml
mcp:
  timeout: 30000
  max-message-size: 10485760
```

**API 端点：**

MCP 服务器管理 API (`/api/mcp-servers/*`)：
- `POST /api/mcp-servers` - 创建 MCP 服务器
- `GET /api/mcp-servers` - 获取所有 MCP 服务器
- `GET /api/mcp-servers/{id}` - 获取 MCP 服务器详情
- `PUT /api/mcp-servers/{id}` - 更新 MCP 服务器
- `DELETE /api/mcp-servers/{id}` - 删除 MCP 服务器
- `POST /api/mcp-servers/{id}/sync` - 同步 MCP 工具

Agent 管理 API (`/api/agents/*`)：
- `POST /api/agents` - 创建 Agent
- `GET /api/agents` - 获取所有 Agent
- `GET /api/agents/{id}` - 获取 Agent 详情
- `PUT /api/agents/{id}` - 更新 Agent
- `DELETE /api/agents/{id}` - 删除 Agent
- `POST /api/agents/{id}/execute` - 执行 Agent

**实现任务完成情况：**

1. ✅ **MCP 实体和 Mapper**
   - McpServer - MCP 服务器实体（支持 STDIO 和 HTTP 连接）
   - McpTool - MCP 工具实体
   - Agent - Agent 实体
   - AgentKnowledgeBase - Agent 知识库关联
   - AgentTool - Agent 工具关联
   - 所有对应的 Mapper 接口和 XML

2. ✅ **MCP DTOs**
   - MCP JSON-RPC 协议 DTOs（Request、Response、Notification）
   - MCP 工具定义 DTOs（Tool、TextContent）
   - MCP 服务器和工具 DTOs

3. ✅ **MCP JSON-RPC 客户端**
   - 支持 STDIO 传输（ProcessBuilder）
   - 支持 HTTP 传输（RestTemplate）
   - JSON-RPC 2.0 协议实现
   - 错误处理和超时控制

4. ✅ **MCP 客户端实现**
   - initialize 初始化
   - tools/list 工具列表
   - tools/call 工具调用
   - 连接管理和资源清理

5. ✅ **MCP 服务器服务**
   - CRUD 操作
   - 工具同步（从 MCP 服务器发现工具）
   - 连接测试

6. ✅ **Agent DTOs**
   - CreateAgentRequest - 创建 Agent 请求
   - UpdateAgentRequest - 更新 Agent 请求
   - AgentResponse - Agent 响应
   - AgentExecutionRequest - 执行请求
   - AgentExecutionResponse - 执行响应

7. ✅ **工具执行器**
   - 调用 MCP 工具
   - 调用 LLM 工具（内置）
   - 结果格式化和错误处理

8. ✅ **ReAct 工作流执行器**
   - Thought（思考）- 分析当前状态
   - Action（行动）- 选择并执行工具
   - Observation（观察）- 观察工具执行结果
   - 迭代控制（最大迭代次数）
   - 最终答案生成

9. ✅ **Agent 执行引擎**
   - 协调 RAG 查询
   - 执行 ReAct 工作流
   - 管理执行历史
   - 返回执行结果

10. ✅ **Agent 服务**
    - CRUD 操作
    - 知识库关联管理
    - 工具关联管理

11. ✅ **Agent 执行服务**
    - 执行 Agent
    - 验证 Agent 状态
    - 加载 Agent 配置
    - 调用 AgentEngine

12. ✅ **Agent 控制器**
    - 提供完整的 REST API
    - 集成审计日志
    - 请求参数验证

13. ✅ **测试覆盖**
    - McpServerServiceTest - MCP 服务器服务测试
    - AgentServiceTest - Agent 服务测试
    - McpClientTest - MCP 客户端测试
    - McpJsonRpcClientTest - JSON-RPC 客户端测试
    - ToolExecutorTest - 工具执行器测试
    - ReActWorkflowExecutorTest - ReAct 工作流测试
    - AgentEngineTest - Agent 引擎测试
    - AgentControllerTest - Agent 控制器测试
    - AgentSystemIntegrationTest - 集成测试

**技术栈：**
- JSON-RPC 2.0 (MCP 协议)
- ProcessBuilder (STDIO 进程通信)
- RestTemplate (HTTP 工具调用)
- MyBatis-Plus (数据访问)
- ReAct 工作流（推理-行动-观察）

**核心功能：**
- MCP 服务器管理（STDIO + HTTP）
- MCP 工具发现和同步
- ReAct 工作流执行器
- Agent 执行引擎
- Agent CRUD API
- Agent 执行 API

**测试统计：**
- Phase 5 总测试数：8 个
- 单元测试：8 ✅
- 集成测试：待完善

---

### Phase 6: 聊天机器人系统 ✅

**完成时间：2026-01-19**

**实现内容：**
- 聊天机器人管理（CRUD 操作）
- 对话管理（创建、查询、删除）
- 消息历史存储和查询
- 聊天接口（非流式）
- 流式聊天接口（SSE）
- 访问计数统计
- 完整测试覆盖

**新增文件：**
```
src/main/java/com/mydotey/ai/studio/
├── entity/
│   ├── Chatbot.java
│   ├── Conversation.java
│   └── Message.java
├── mapper/
│   ├── ChatbotMapper.java
│   ├── ConversationMapper.java
│   └── MessageMapper.java
├── dto/chatbot/
│   ├── CreateChatbotRequest.java
│   ├── UpdateChatbotRequest.java
│   ├── ChatbotResponse.java
│   ├── ConversationResponse.java
│   ├── MessageResponse.java
│   ├── ChatRequest.java
│   └── ChatResponse.java
├── service/
│   ├── ChatbotService.java
│   ├── ConversationService.java
│   └── ChatService.java
└── controller/
    └── ChatbotController.java

src/test/java/com/mydotey/ai/studio/
├── service/
│   ├── ChatbotServiceTest.java
│   ├── ConversationServiceTest.java
│   └── ChatServiceTest.java
└── integration/
    └── ChatbotSystemIntegrationTest.java
```

**API 端点：**

聊天机器人管理 API (`/api/chatbots/*`)：
- `POST /api/chatbots` - 创建聊天机器人
- `GET /api/chatbots/{id}` - 获取聊天机器人详情
- `GET /api/chatbots/my` - 获取我的聊天机器人列表
- `GET /api/chatbots/published` - 获取已发布的聊天机器人列表
- `PUT /api/chatbots/{id}` - 更新聊天机器人
- `DELETE /api/chatbots/{id}` - 删除聊天机器人

对话管理 API (`/api/chatbots/{chatbotId}/conversations/*`)：
- `GET /api/chatbots/{chatbotId}/conversations` - 获取对话列表
- `GET /api/chatbots/conversations/{conversationId}` - 获取对话详情
- `POST /api/chatbots/{chatbotId}/conversations` - 创建新对话
- `DELETE /api/chatbots/conversations/{conversationId}` - 删除对话

聊天 API (`/api/chatbots/chat*`)：
- `POST /api/chatbots/chat` - 发送消息（非流式）
- `POST /api/chatbots/chat/stream` - 发送消息（流式 SSE）

**实现任务完成情况：**

1. ✅ **Chatbot 实体和 Mapper**
   - Chatbot - 聊天机器人实体
   - Conversation - 对话实体
   - Message - 消息实体
   - 所有对应的 Mapper

2. ✅ **Chatbot DTOs**
   - CreateChatbotRequest - 创建请求
   - UpdateChatbotRequest - 更新请求
   - ChatbotResponse - 聊天机器人响应
   - ConversationResponse - 对话响应
   - MessageResponse - 消息响应
   - ChatRequest - 聊天请求
   - ChatResponse - 聊天响应

3. ✅ **Chatbot 服务**
   - CRUD 操作
   - 权限验证
   - 访问计数
   - 发布状态管理

4. ✅ **Conversation 服务**
   - 对话创建和查询
   - 对话历史加载
   - 对话删除（级联删除消息）

5. ✅ **Chat 服务**
   - 消息发送
   - Agent 调用
   - 消息历史管理
   - 来源和工具调用记录

6. ✅ **Chatbot 控制器**
   - 提供完整的 REST API
   - 集成审计日志
   - SSE 流式响应支持

7. ✅ **测试覆盖**
   - ChatbotServiceTest - 聊天机器人服务测试（2 个测试）
   - ConversationServiceTest - 对话服务测试（2 个测试）
   - ChatServiceTest - 聊天服务测试（1 个测试）
   - ChatbotSystemIntegrationTest - 系统集成测试（4 个测试）

**技术栈：**
- Spring Boot 3.5
- MyBatis-Plus
- SSE (Server-Sent Events)
- Agent Execution Service

**核心功能：**
- 聊天机器人管理
- 对话管理
- 消息历史
- 流式响应
- Agent 集成

**测试统计：**
- Phase 6 总测试数：9 个
- 单元测试：5 ✅
- 集成测试：4 ✅

---

### Phase 7: Web Crawling System ✅

**完成时间：2026-01-19**

**实现内容：**
- Jsoup 网页抓取器
- URL 过滤服务（正则表达式）
- 爬虫编排器（BFS/DFS 策略）
- 抓取任务管理
- 异步抓取执行
- 进度跟踪

**新增文件：**
```
src/main/java/com/mydotey/ai/studio/
├── entity/
│   ├── WebCrawlTask.java
│   └── WebPage.java
├── mapper/
│   ├── WebCrawlTaskMapper.java
│   └── WebPageMapper.java
├── dto/webcrawl/
│   ├── CreateCrawlTaskRequest.java
│   ├── CrawlTaskResponse.java
│   ├── CrawlTaskProgressResponse.java
│   ├── WebPageResponse.java
│   └── StartCrawlRequest.java
├── service/
│   ├── WebCrawlService.java
│   └── webcrawl/
│       ├── WebScraper.java (interface)
│       ├── JsoupWebScraper.java
│       ├── UrlFilter.java
│       ├── CrawlOrchestrator.java
│       ├── ScrapedResult.java
│       └── ScrapingException.java
└── controller/
    └── WebCrawlController.java

src/main/resources/
└── mapper/
    ├── WebCrawlTaskMapper.xml
    └── WebPageMapper.xml

src/test/java/com/mydotey/ai/studio/
├── service/
│   ├── WebCrawlServiceTest.java
│   └── webcrawl/
│       ├── JsoupWebScraperTest.java
│       ├── UrlFilterTest.java
│       └── CrawlOrchestratorTest.java
├── controller/
│   └── WebCrawlControllerTest.java
└── integration/
    └── WebCrawlingIntegrationTest.java
```

**API 端点：**

网页抓取任务管理 API (`/api/web-crawl/tasks/*`)：
- `POST /api/web-crawl/tasks` - 创建抓取任务
- `POST /api/web-crawl/tasks/{id}/start` - 启动抓取任务
- `GET /api/web-crawl/tasks/{id}` - 获取任务详情
- `GET /api/web-crawl/tasks/{id}/progress` - 获取任务进度
- `GET /api/web-crawl/tasks/kb/{kbId}` - 获取知识库的所有任务
- `DELETE /api/web-crawl/tasks/{id}` - 删除抓取任务

**实现任务完成情况：**

1. ✅ **WebCrawlTask 和 WebPage 实体**
   - WebCrawlTask - 抓取任务实体（支持 BFS/DFS 策略）
   - WebPage - 网页实体（存储抓取内容和元数据）
   - 所有对应的 Mapper

2. ✅ **Web Crawling DTOs**
   - CreateCrawlTaskRequest - 创建抓取任务请求
   - CrawlTaskResponse - 抓取任务响应
   - CrawlTaskProgressResponse - 抓取任务进度响应
   - WebPageResponse - 网页响应
   - StartCrawlRequest - 启动抓取请求

3. ✅ **Jsoup Web Scraper**
   - 静态网页抓取
   - 提取标题、正文、链接
   - 错误处理和超时控制
   - 支持 User-Agent 自定义

4. ✅ **URL Filter**
   - 正则表达式过滤
   - 同源策略
   - URL 去重
   - 文件类型过滤

5. ✅ **Crawl Orchestrator**
   - BFS 广度优先策略
   - DFS 深度优先策略
   - 最大深度控制
   - 最大页面数控制
   - 异步执行

6. ✅ **Web Crawl Service**
   - 抓取任务 CRUD 操作
   - 启动异步抓取
   - 进度跟踪
   - 权限验证

7. ✅ **Web Crawl Controller**
   - 提供完整的 REST API
   - 集成审计日志
   - 请求参数验证

8. ✅ **测试覆盖**
   - WebCrawlServiceTest - 抓取任务服务测试（14 个测试）
   - JsoupWebScraperTest - Jsoup 抓取器测试（2 个测试）
   - UrlFilterTest - URL 过滤器测试（3 个测试）
   - CrawlOrchestratorTest - 爬虫编排器测试（6 个测试）
   - WebCrawlControllerTest - 抓取控制器测试（10 个测试）
   - WebCrawlingIntegrationTest - 系统集成测试（7 个测试）

**技术栈：**
- Jsoup 1.17.2 - HTML 解析和网页抓取
- Spring Async - 异步任务执行
- 并发处理 - ThreadPoolTaskExecutor
- 正则表达式 - URL 过滤

**核心功能：**
- 静态网页抓取（HTML 解析）
- 级联抓取（BFS/DFS 策略）
- URL 过滤（正则表达式 + 同源策略）
- URL 去重机制
- 抓取进度跟踪
- 异步执行支持
- 抓取结果持久化

**测试统计：**
- Phase 7 总测试数：42 个

### Phase 8: 文件存储系统 ✅

**完成时间：2026-01-19**

**实现内容：**
- 多存储类型支持（本地、阿里云 OSS、AWS S3）
- 文件元数据管理
- 存储配置管理
- 统一文件上传下载 API
- URL 签名（云存储）
- 访问控制
- 完整测试覆盖

**新增文件：**
```
src/main/java/com/mydotey/ai/studio/
├── service/
│   ├── FileStorageManagerService.java
│   ├── StorageConfigService.java
│   └── filestorage/
│       ├── LocalFileStorageService.java
│       ├── OssFileStorageService.java
│       ├── S3FileStorageService.java
│       └── FileStorageFactory.java
├── controller/
│   ├── FileStorageController.java
│   └── StorageConfigController.java
└── dto/filestorage/
    ├── CreateStorageConfigRequest.java
    ├── UpdateStorageConfigRequest.java
    └── StorageConfigResponse.java

src/test/java/com/mydotey/ai/studio/
├── service/
│   ├── FileStorageManagerServiceTest.java
│   └── filestorage/
│       ├── LocalFileStorageServiceTest.java
│       ├── OssFileStorageServiceTest.java
│       └── S3FileStorageServiceTest.java
├── controller/
│   └── FileStorageControllerTest.java
└── integration/
    └── FileStorageIntegrationTest.java
```

**API 端点：**

文件管理 API (`/api/files/*`)：
- `POST /api/files/upload` - 上传文件
- `GET /api/files/download/{id}` - 下载文件
- `GET /api/files/{id}/url` - 获取文件访问 URL
- `GET /api/files/{id}` - 获取文件元数据
- `GET /api/files/my` - 获取我的文件列表
- `GET /api/files/related/{entityType}/{entityId}` - 获取关联实体文件
- `DELETE /api/files/{id}` - 删除文件

存储配置管理 API (`/api/storage-configs/*`)：
- `POST /api/storage-configs` - 创建存储配置（管理员）
- `PUT /api/storage-configs/{id}` - 更新存储配置（管理员）
- `DELETE /api/storage-configs/{id}` - 删除存储配置（管理员）
- `GET /api/storage-configs/{id}` - 获取存储配置详情
- `GET /api/storage-configs` - 获取所有存储配置
- `GET /api/storage-configs/default` - 获取默认存储配置

**实现任务完成情况：**

1. ✅ **本地存储实现**
   - LocalFileStorageService - 本地文件存储
   - 支持文件上传、下载、删除
   - 支持自定义上传目录

2. ✅ **阿里云 OSS 实现**
   - OssFileStorageService - OSS 文件存储
   - 支持签名 URL 生成
   - 自动创建 Bucket

3. ✅ **AWS S3 实现**
   - S3FileStorageService - S3 文件存储
   - 支持 S3 兼容存储
   - 签名 URL 生成

4. ✅ **文件存储工厂和管理服务**
   - FileStorageFactory - 存储服务工厂
   - FileStorageManagerService - 文件管理服务
   - 元数据持久化
   - 权限控制

5. ✅ **存储配置管理**
   - CRUD 操作
   - 默认配置管理
   - 敏感信息保护（secret）

6. ✅ **控制器**
   - FileStorageController - 文件管理 API
   - StorageConfigController - 配置管理 API
   - 审计日志集成

7. ✅ **测试覆盖**
   - LocalFileStorageServiceTest - 本地存储测试
   - OssFileStorageServiceTest - OSS 测试
   - S3FileStorageServiceTest - S3 测试
   - FileStorageManagerServiceTest - 管理服务测试
   - FileStorageControllerTest - 控制器测试
   - FileStorageIntegrationTest - 集成测试

**技术栈：**
- 阿里云 OSS SDK 3.17.4
- AWS S3 SDK 2.25.11
- Java NIO（本地存储）
- 策略模式（多存储支持）

**核心功能：**
- 多存储类型支持（LOCAL/OSS/S3）
- 统一文件管理 API
- 文件元数据管理
- 存储配置管理
- URL 签名访问
- 权限控制
- 关联实体文件

**测试统计：**
- Phase 8 总测试数：8 个
- 单元测试：5 ✅
- 集成测试：3 ✅

- 单元测试：35 ✅
  - WebCrawlServiceTest: 14
  - CrawlOrchestratorTest: 6
  - WebCrawlControllerTest: 10
  - JsoupWebScraperTest: 2
  - UrlFilterTest: 3
- 集成测试：7 ✅
  - WebCrawlingIntegrationTest: 7

---

## 当前状态

**Git 状态：**
- 分支：main
- 远程：origin/main（已同步）
- 工作树：干净（无未提交更改）
- 最新提交：4fc4fe3 - feat: add chatbot controller

**测试状态：**
- 总测试数：104（包含 Phase 1-7 的所有测试）
- 通过：104 ✅
- 失败：0
- 错误：0
- 跳过：0

**当前阶段：**
- Phase 1: 基础架构 ✅
- Phase 2: 文档处理 ✅
- Phase 3: 用户认证和权限管理 ✅
- Phase 4: RAG 系统 ✅
- Phase 5: Agent 系统 ✅
- Phase 6: 聊天机器人 ✅
- Phase 7: 网页抓取系统 ✅
- Phase 8: 文件存储系统 ✅

---

## 下一步计划



**预计功能：**
- 本地存储
- 云存储集成（OSS/S3）
- 文件上传下载
- 文件管理
- 访问控制

### Phase 9: 系统监控和日志（待规划）

**预计功能：**
- APM 监控
- 结构化日志
- 请求追踪（trace ID）
- 性能指标
- 错误追踪

### Phase 10: API 文档和部署（待规划）

**预计功能：**
- Swagger/OpenAPI 文档
- 部署文档
- 运维手册
- Docker 容器化
- CI/CD 流程

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

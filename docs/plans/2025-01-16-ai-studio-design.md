# AI Studio 设计方案

## 项目概述

面向企业用户（非程序员）的 AI 开发工作室，提供知识库（RAG）、工具配置（MCP）、AI Agent 和 Chatbot 开发能力。

### 核心特性
- 知识库管理：文档上传、网页抓取、向量化检索
- MCP 工具集成：连接外部工具和数据源
- Agent 构建：配置 AI 代理完成复杂任务
- Chatbot 部署：创建对话机器人服务终端用户
- 多用户权限：支持组织协作和权限控制

### 技术栈
- **前端**：Vue 3 + TypeScript + Vite + Element Plus
- **后端**：Spring Boot 3.5 + JDK 21
- **数据库**：PostgreSQL 15+ (PGVector)
- **持久化**：MyBatis Plus
- **部署**：Docker

---

## 一、整体架构

### 前端 (Vue 3)
- 框架：Vue 3 + TypeScript + Vite
- UI：Element Plus
- 状态管理：Pinia
- 主要模块：知识库管理、工具配置、Agent 构建、Chatbot 对话界面

### 后端 (Spring Boot 3.5)
- 分层架构：Controller → Service → Repository
- 向量存储：PGVector (PostgreSQL 扩展)
- 文档解析：Apache PDFBox、Apache POI、Jsoup
- API：RESTful 风格 + SSE 流式响应

### 核心服务模块
1. 知识库服务：文档上传、解析、向量化、检索
2. RAG 服务：知识检索 + AI 生成
3. Agent 服务：工作流编排、工具调用
4. Chatbot 服务：对话管理、历史记录
5. MCP 工具服务：工具注册和调用
6. 网页抓取服务：Playwright 级联抓取

---

## 二、数据库设计

### 核心表结构

#### 1. organizations 组织表（可选）
```sql
CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 2. users 用户表
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT REFERENCES organizations(id),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    avatar_url VARCHAR(500),
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 3. knowledge_bases 知识库表
```sql
CREATE TABLE knowledge_bases (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT REFERENCES organizations(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    embedding_model VARCHAR(100) NOT NULL,
    chunk_size INT DEFAULT 500,
    chunk_overlap INT DEFAULT 100,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 4. kb_members 知识库成员权限关联表
```sql
CREATE TABLE kb_members (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL, -- OWNER, MEMBER, VIEWER
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(kb_id, user_id)
);
```

#### 5. documents 文档表
```sql
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    chunk_count INT DEFAULT 0,
    source_type VARCHAR(20),
    source_url VARCHAR(500),
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 6. document_chunks 文档分块表
```sql
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536),
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, chunk_index)
);
```

#### 7. mcp_servers MCP 服务器配置表
```sql
CREATE TABLE mcp_servers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    connection_type VARCHAR(20) NOT NULL,
    command VARCHAR(500),
    working_dir VARCHAR(500),
    endpoint_url VARCHAR(500),
    headers JSONB DEFAULT '{}',
    auth_type VARCHAR(20),
    auth_config JSONB DEFAULT '{}',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 8. mcp_tools MCP 工具定义表
```sql
CREATE TABLE mcp_tools (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES mcp_servers(id) ON DELETE CASCADE,
    tool_name VARCHAR(255) NOT NULL,
    description TEXT,
    input_schema JSONB NOT NULL,
    output_schema JSONB,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(server_id, tool_name)
);
```

#### 9. agents Agent 配置表
```sql
CREATE TABLE agents (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT REFERENCES organizations(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    system_prompt TEXT NOT NULL,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    model_config JSONB NOT NULL,
    workflow_type VARCHAR(20) DEFAULT 'REACT',
    workflow_config JSONB DEFAULT '{}',
    max_iterations INT DEFAULT 10,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 10. agent_knowledge_bases Agent 知识库关联表
```sql
CREATE TABLE agent_knowledge_bases (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
    kb_id BIGINT NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(agent_id, kb_id)
);
```

#### 11. agent_tools Agent 工具关联表
```sql
CREATE TABLE agent_tools (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
    tool_id BIGINT NOT NULL REFERENCES mcp_tools(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(agent_id, tool_id)
);
```

#### 12. chatbots Chatbot 配置表
```sql
CREATE TABLE chatbots (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL REFERENCES agents(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    welcome_message TEXT DEFAULT '你好，有什么可以帮助你的吗？',
    avatar_url VARCHAR(500),
    owner_id BIGINT NOT NULL REFERENCES users(id),
    settings JSONB DEFAULT '{}',
    style_config JSONB DEFAULT '{}',
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    access_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 13. conversations 对话记录表
```sql
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    chatbot_id BIGINT NOT NULL REFERENCES chatbots(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 14. messages 消息表
```sql
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    sources JSONB DEFAULT '[]',
    tool_calls JSONB DEFAULT '[]',
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 15. web_crawl_tasks 网页抓取任务表
```sql
CREATE TABLE web_crawl_tasks (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    start_url VARCHAR(500) NOT NULL,
    url_pattern VARCHAR(500),
    max_depth INT DEFAULT 2,
    crawl_strategy VARCHAR(20) DEFAULT 'BFS',
    concurrent_limit INT DEFAULT 3,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_pages INT DEFAULT 0,
    success_pages INT DEFAULT 0,
    failed_pages INT DEFAULT 0,
    error_message TEXT,
    created_by BIGINT REFERENCES users(id),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 16. web_pages 已抓取页面表
```sql
CREATE TABLE web_pages (
    id BIGSERIAL PRIMARY KEY,
    crawl_task_id BIGINT NOT NULL REFERENCES web_crawl_tasks(id) ON DELETE CASCADE,
    document_id BIGINT REFERENCES documents(id),
    url VARCHAR(500) NOT NULL,
    title VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    depth INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(crawl_task_id, url)
);
```

#### 17. api_keys API Key 管理表
```sql
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    key_hash VARCHAR(255) NOT NULL,
    permissions JSONB DEFAULT '{}',
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 18. audit_logs 审计日志表
```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50),
    resource_id BIGINT,
    details JSONB DEFAULT '{}',
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 19. file_storage_config 文件存储配置表
```sql
CREATE TABLE file_storage_config (
    id BIGSERIAL PRIMARY KEY,
    storage_type VARCHAR(20) NOT NULL,
    endpoint VARCHAR(500),
    access_key VARCHAR(255),
    secret_key VARCHAR(255),
    bucket_name VARCHAR(255),
    region VARCHAR(50),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 20. settings 系统配置表
```sql
CREATE TABLE settings (
    id BIGSERIAL PRIMARY KEY,
    key VARCHAR(100) NOT NULL UNIQUE,
    value TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 三、知识库服务

### 文档上传流程
1. 前端上传文件（multipart/form-data）
2. 验证文件类型和大小
3. 保存到对象存储（本地/MinIO/OSS）
4. 异步处理队列：解析 → 文本提取 → 分块 → 向量化 → 存储

### 文档解析
- PDF：Apache PDFBox
- Word：Apache POI
- Excel：Apache POI
- PPT：Apache POI

### 分块策略
- 按段落/标题智能分块
- 块大小可配置（默认 500-1000 字符）
- 块重叠 20% 避免信息丢失

### 向量化
- 调用 OpenAI 兼容 API 的 embeddings 接口
- Batch 处理提升效率
- 缓存向量减少重复调用

### 检索策略
- 余弦相似度检索 Top-K 相关块
- 支持 hybrid 检索（关键词 + 向量混合）
- 引用高亮显示原文位置

---

## 四、网页抓取服务

### 网页抓取流程
1. 用户输入 URL，配置：
   - URL pattern（正则表达式）
   - 级联深度 depth（默认 2）
2. Playwright 启动无头浏览器加载页面
3. 等待页面完全渲染（包括 JavaScript）
4. 导出为 PDF 文件
5. 如果 depth > 0，提取页面内符合 pattern 的链接
6. 将新 URL 加入抓取队列（depth - 1）
7. 递归处理直到队列为空

### 级联抓取配置
- **URL pattern**：正则表达式筛选要抓取的链接
- **Max depth**：最大抓取深度
- **Breadth-first/Depth-first**：抓取策略选择
- **Concurrent limit**：并发抓取数量限制（默认 3）

### 去重机制
- Redis 或数据库记录已抓取的 URL
- 支持设置过期时间

### 进度跟踪
- 显示已抓取/待抓取/失败的页面数量
- 支持暂停和恢复抓取任务

---

## 五、RAG 服务

### RAG 流程
1. 用户提问
2. 查询向量数据库检索相关知识块（Top-K，默认 5）
3. 构造 Prompt：系统提示 + 检索到的知识 + 用户问题
4. 调用 OpenAI 兼容 API 生成回答
5. 返回回答 + 知识来源引用

### Prompt 模板（可配置）
```
你是一个专业的助手。请根据以下知识回答用户的问题。

知识内容：
{knowledge}

用户问题：{question}

如果知识中没有相关信息，请明确说明不要凭空编造。
```

### 检索增强配置
- Top-K：返回的相关块数量（默认 5）
- Score threshold：相似度阈值（默认 0.7）
- Temperature：生成温度（默认 0.3）
- Max tokens：最大生成长度

### 引用显示
- 回答中标注知识来源（文档名 + 页码/段落）
- 前端点击引用可跳转到原文位置
- 支持多来源聚合显示

### 流式响应
- 使用 Server-Sent Events (SSE) 实现流式输出
- 逐字显示回答

---

## 六、Agent 服务

### Agent 概念
Agent 是一个具备推理能力的 AI 实体，可以配置知识库、工具和工作流，自主完成复杂任务。

### Agent 配置结构
- **基本信息**：名称、描述、头像
- **系统提示词**：定义 Agent 的角色和行为
- **知识库绑定**：关联一个或多个知识库
- **工具集**：从 MCP 工具列表中选择
- **参数配置**：temperature、max_tokens、top_p 等

### Agent 执行流程
1. 接收用户输入
2. 根据系统提示词和知识库信息构造上下文
3. AI 推理是否需要调用工具
4. 如需调用：解析参数 → 执行工具 → 获取结果 → 继续推理
5. 多轮循环直到任务完成或达到最大迭代次数
6. 返回最终结果

### 工作流支持
- **Linear Flow**：顺序执行预定义的步骤
- **ReAct Loop**：Thought → Action → Observation 循环
- **DAG Flow**：有向无环图，支持分支和并行

---

## 七、Chatbot 服务

### Chatbot 概念
Chatbot 是面向终端用户的对话界面，绑定一个 Agent 作为对话大脑。

### Chatbot 配置结构
- **基本信息**：名称、描述、头像、欢迎语
- **绑定 Agent**：选择配置好的 Agent
- **对话设置**：
  - 对话历史保留轮数（默认 10 轮）
  - 是否显示引用来源
  - 流式输出开关
- **外观设置**：主题颜色、Logo、标题等

### 对话流程
1. 用户访问 Chatbot 页面
2. 发送消息
3. 查询或创建会话（conversations）
4. 加载对话历史作为上下文
5. 调用绑定的 Agent 处理消息
6. 保存消息到数据库
7. 流式返回回答

### 会话管理
- 每个用户可以有多个独立会话
- 会话标题自动生成
- 支持重命名、删除会话
- 会话历史可搜索和导出

### 前端聊天界面
- 类似 ChatGPT 的对话框布局
- 消息气泡区分用户和助手
- 引用来源高亮可点击
- 支持 Markdown 渲染
- 移动端响应式适配

---

## 八、MCP 工具服务

### MCP 概念
MCP (Model Context Protocol) 是标准化协议，用于连接 AI 模型与外部工具和数据源。

### MCP 工具配置结构
- **基本信息**：工具名称、描述、图标
- **连接方式**：
  - **Local**：本地可执行文件
  - **HTTP**：通过 HTTP API 调用
  - **SSE**：通过 Server-Sent Events 通信
- **连接参数**：
  - Local：可执行文件路径、工作目录、环境变量
  - HTTP：endpoint URL、headers、认证方式
- **Schema**：工具的输入输出 JSON Schema

### 工具注册流程
1. 用户选择连接方式
2. 填写连接参数
3. 后端连接 MCP 服务器并验证
4. 自动获取工具列表和 Schema
5. 解析并保存工具定义到数据库

### 工具调用流程
1. Agent 决定调用某个 MCP 工具
2. 后端根据工具配置找到连接方式
3. 构造调用请求，传递参数
4. 等待工具执行结果
5. 将结果返回给 Agent 继续推理

### 内置工具
- 网页搜索（可配置搜索引擎 API）
- 文件读写（限沙盒目录）
- 数据库查询（限配置的数据源）
- HTTP 请求（限白名单域名）

---

## 九、用户认证和权限管理

### 认证方式
- 用户名 + 密码登录
- JWT Token 认证（Access Token 2 小时，Refresh Token 7 天）
- 支持记住登录状态

### 角色权限体系
- **Super Admin**：系统最高权限，管理所有用户、配置全局设置
- **Admin**：管理本组织内的知识库、Agent、Chatbot、用户
- **User**：创建自己的知识库、Agent、Chatbot，使用他人共享的资源

### 权限控制维度
- **知识库权限**：Owner / Member / Viewer
- **Agent 权限**：Owner / Viewer（可复制但不可编辑）
- **Chatbot 权限**：Owner（完全控制）/ User（仅对话）

### 组织架构（可选）
- 支持多租户/多部门
- 资源按组织隔离
- 跨组织资源需要显式共享

### 安全措施
- 密码使用 BCrypt 加密存储
- API Key 用于外部集成
- 操作日志记录关键操作
- 限流防止暴力破解（5 次失败锁定 15 分钟）

---

## 十、错误处理和日志

### 统一异常处理
- 全局异常处理器 `@RestControllerAdvice`
- 异常分类：
  - BusinessException（400）
  - AuthException（401/403）
  - ResourceNotFoundException（404）
  - RateLimitException（429）
  - SystemException（500）
- 统一返回格式：`{code, message, data, timestamp}`

### 日志设计
- **操作日志**：记录用户关键操作
- **访问日志**：记录 API 请求
- **错误日志**：记录异常堆栈和上下文
- **性能日志**：记录慢查询、慢接口（>3s）
- 日志级别：ERROR、WARN、INFO、DEBUG

### 日志存储
- 开发环境：输出到控制台和文件
- 生产环境：输出到文件 + 可选 ELK/Loki 集中收集
- 日志文件按天轮转，保留 30 天

### 错误监控
- 集成健康检查端点 `/actuator/health`
- 关键指标：文档处理成功率、API 响应时间、错误率
- 可选集成 Sentry 进行错误追踪

---

## 十一、测试策略

### 单元测试
- Service 层核心业务逻辑测试（JUnit 5 + Mockito）
- 目标覆盖率：70%+
- 测试用例包括：正常流程、边界条件、异常情况

### 集成测试
- API 接口测试（TestContainers + PostgreSQL）
- 测试完整流程：用户操作 → 数据库持久化 → 响应
- Mock 外部依赖（AI 模型 API、文件存储）

### 关键测试场景
- 文档上传和解析流程
- 知识库检索准确性
- Agent 工具调用链路
- MCP 工具连接和执行
- 用户认证和权限验证

### E2E 测试（可选）
- Playwright 或 Cypress 测试前端完整用户流程
- 场景：注册 → 创建知识库 → 上传文档 → 创建 Agent → 对话测试

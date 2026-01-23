# MCP 服务器配置 UI 设计文档

**日期**: 2026-01-22
**功能**: MCP 服务器配置和管理界面
**设计者**: Claude (AI Studio Frontend Team)

---

## 一、概述

### 目标
为 AI Studio 平台创建 MCP (Model Context Protocol) 服务器配置和管理界面，允许用户配置 STDIO 或 HTTP 类型的 MCP 服务器，同步服务器提供的工具，并测试连接。

### 设计原则
1. **一致性**: 与现有 UI (Chatbot、Agent) 保持一致的设计模式
2. **简洁性**: 动态表单减少用户认知负担
3. **易用性**: 提供测试连接功能，方便验证配置
4. **完整性**: 覆盖 MCP 服务器的完整生命周期

---

## 二、页面架构

### 2.1 页面层级

**列表页** (`/mcp-servers`)
- 标准列表布局
- 顶部标题栏："MCP 服务器" + "创建服务器"按钮
- 服务器列表表格（无分页）
- 点击行进入详情页

**详情页** (`/mcp-servers/:id`)
- 多标签页设计
- 返回按钮 + 操作按钮
- 三个标签页：信息、工具列表、测试日志

### 2.2 路由配置

```typescript
{
  path: 'mcp-servers',
  name: 'McpServers',
  component: () => import('@/views/mcp/McpServerListView.vue'),
  meta: { title: 'MCP工具', icon: 'Connection' }
},
{
  path: 'mcp-servers/:id',
  name: 'McpServerDetail',
  component: () => import('@/views/mcp/McpServerDetailView.vue'),
  meta: { title: 'MCP服务器详情', hidden: true }
}
```

---

## 三、类型定义

### 3.1 McpServer 接口

```typescript
export interface McpServer {
  id: number
  name: string
  description?: string
  connectionType: 'STDIO' | 'HTTP'
  command?: string
  workingDir?: string
  endpointUrl?: string
  headers?: string
  authType?: 'NONE' | 'API_KEY' | 'BASIC'
  status: 'ACTIVE' | 'INACTIVE' | 'ERROR'
  createdBy: number
  createdAt: string
  updatedAt: string
}
```

### 3.2 请求接口

```typescript
export interface CreateMcpServerRequest {
  name: string
  description?: string
  connectionType: 'STDIO' | 'HTTP'
  command?: string
  workingDir?: string
  endpointUrl?: string
  headers?: string
  authType?: 'NONE' | 'API_KEY' | 'BASIC'
  authConfig?: string
}

export interface UpdateMcpServerRequest {
  name?: string
  description?: string
  connectionType?: 'STDIO' | 'HTTP'
  command?: string
  workingDir?: string
  endpointUrl?: string
  headers?: string
  authType?: 'NONE' | 'API_KEY' | 'BASIC'
  authConfig?: string
}
```

### 3.3 工具接口

```typescript
export interface McpTool {
  id: number
  serverId: number
  toolName: string
  description?: string
  inputSchema: Record<string, unknown>
  outputSchema?: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface TestConnectionResult {
  success: boolean
  message?: string
}
```

---

## 四、API 函数

### 4.1 服务器 CRUD

```typescript
export function getMcpServers(): Promise<McpServer[]>
export function getMcpServer(id: number): Promise<McpServer>
export function createMcpServer(data: CreateMcpServerRequest): Promise<McpServer>
export function updateMcpServer(id: number, data: UpdateMcpServerRequest): Promise<void>
export function deleteMcpServer(id: number): Promise<void>
```

### 4.2 工具管理

```typescript
export function syncTools(serverId: number): Promise<void>
export function testConnection(serverId: number): Promise<TestConnectionResult>
```

---

## 五、列表页面设计

### 5.1 McpServerListView 组件

**布局结构**:
- 顶部标题栏
- 服务器列表表格
- 创建对话框

**表格列**:
1. 名称 (150px)
2. 描述 (200px)
3. 连接类型 (120px) - Tag 组件
4. 状态 (100px) - Tag 组件（绿色/灰色/红色）
5. 创建时间 (180px)
6. 操作 (280px) - 详情、同步工具、编辑、删除

**状态标签**:
```typescript
function getStatusType(status: string): 'success' | 'info' | 'danger' {
  switch (status) {
    case 'ACTIVE': return 'success'
    case 'INACTIVE': return 'info'
    case 'ERROR': return 'danger'
    default: return 'info'
  }
}

function getStatusLabel(status: string): string {
  switch (status) {
    case 'ACTIVE': return '活动'
    case 'INACTIVE': return '未激活'
    case 'ERROR': return '错误'
    default: return status
  }
}
```

---

## 六、表单组件设计

### 6.1 McpServerForm 组件

**动态表单字段**:

**基本信息**:
- 名称 (必填)
- 描述 (可选)
- 连接类型 (必填) - Radio: STDIO / HTTP

**STDIO 配置** (connectionType === 'STDIO' 时显示):
- 命令 (必填) - Input: "npx -y @modelcontextprotocol/server-filesystem"
- 工作目录 (可选) - Input: "/home/user/projects"

**HTTP 配置** (connectionType === 'HTTP' 时显示):
- 端点 URL (必填) - Input: "https://api.example.com/mcp"
- 请求头 (可选) - Textarea: JSON 格式

**认证配置**:
- 认证类型 - Select: NONE / API_KEY / BASIC

**API Key 认证** (authType === 'API_KEY' 时显示):
- API Key - Input (show-password)
- Header 名称 - Input: "x-api-key"

**Basic Auth 认证** (authType === 'BASIC' 时显示):
- 用户名 - Input
- 密码 - Input (show-password)

**提交逻辑**:
```typescript
function buildAuthConfig(): string {
  if (form.authType === 'API_KEY') {
    return JSON.stringify({
      key: apiKey.value,
      header: apiKeyHeader.value || 'x-api-key'
    })
  } else if (form.authType === 'BASIC') {
    return JSON.stringify({
      username: basicUsername.value,
      password: basicPassword.value
    })
  }
  return '{}'
}
```

---

## 七、详情页面设计

### 7.1 McpServerDetailView 组件

**页面头部**:
- 返回按钮
- 服务器名称 + 状态标签
- 操作按钮: 同步工具、测试连接、编辑、删除

**标签页结构**:

**信息标签页**:
- 使用 el-descriptions 组件
- 显示所有配置信息
- 根据连接类型动态显示 STDIO 或 HTTP 配置

**工具列表标签页**:
- McpToolList 组件
- 显示该服务器的所有工具
- 提供同步按钮
- 查看输入/输出 Schema

**测试日志标签页**:
- 使用 el-timeline 组件
- 显示历史测试记录
- 成功/失败状态
- 错误信息展示

---

## 八、工具列表组件

### 8.1 McpToolList 组件

**布局**:
- 顶部: "工具列表" 标题 + "同步工具" 按钮
- 工具表格

**表格列**:
1. 工具名称 (200px)
2. 描述 (300px)
3. 输入 Schema (100px) - "查看" 按钮
4. 输出 Schema (100px) - "查看" 按钮 (如果有)

**Schema 查看对话框**:
- 使用 el-dialog
- 显示格式化的 JSON Schema
- 代码块样式展示

---

## 九、测试连接功能

### 9.1 实现

```typescript
async function handleTestConnection() {
  const loading = ElLoading.service({
    lock: true,
    text: '正在测试连接...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    const result = await testConnection(serverId.value)

    testLogs.value.unshift({
      id: Date.now(),
      timestamp: new Date().toISOString(),
      success: result.success,
      message: result.success ? '连接成功' : '连接失败',
      error: result.message
    })

    if (result.success) {
      ElMessage.success('连接测试成功')
    } else {
      ElMessage.error(`连接测试失败: ${result.message}`)
    }
  } catch (error) {
    ElMessage.error('测试连接时出错')
  } finally {
    loading.close()
  }
}
```

---

## 十、错误处理

### 10.1 API 错误处理

所有 API 调用都包含 try-catch：
- 显示友好的错误消息
- 记录详细错误到控制台
- 清理加载状态

### 10.2 表单验证

**动态验证规则**:
- STDIO 类型: command 必填
- HTTP 类型: endpointUrl 必填
- 名称必填
- 连接类型必填

### 10.3 删除确认

使用 ElMessageBox 确认删除操作：
- 显示服务器名称
- 警告样式
- 确认/取消按钮

---

## 十一、样式设计

### 11.1 SCSS 样式

**列表页样式**:
```scss
.mcp-server-list {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h2 {
      margin: 0;
      font-size: 24px;
      color: #303133;
    }
  }
}
```

**详情页样式**:
```scss
.mcp-server-detail {
  .header {
    margin-bottom: 20px;

    .title {
      font-size: 20px;
      font-weight: 500;
    }
  }

  .tabs {
    :deep(.el-descriptions) {
      margin-top: 20px;
    }

    .test-logs {
      padding: 20px;

      .error {
        color: #f56c6c;
        margin-top: 8px;
        font-size: 12px;
      }
    }
  }
}
```

**工具列表样式**:
```scss
.mcp-tool-list {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 500;
    }
  }

  .schema-content {
    background-color: #f5f5f5;
    padding: 16px;
    border-radius: 4px;
    overflow-x: auto;
    font-family: 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.6;
  }
}
```

---

## 十二、文件清单

### 新增文件
1. `frontend/src/types/mcp.ts` - MCP 类型定义
2. `frontend/src/api/mcp.ts` - MCP API 函数
3. `frontend/src/views/mcp/McpServerListView.vue` - 列表页
4. `frontend/src/views/mcp/McpServerDetailView.vue` - 详情页
5. `frontend/src/components/mcp/McpServerForm.vue` - 表单组件
6. `frontend/src/components/mcp/McpToolList.vue` - 工具列表组件

### 修改文件
1. `frontend/src/router/index.ts` - 添加 MCP 路由

---

## 十三、实现计划

### 阶段 1: 基础功能
1. 创建类型定义和 API 函数
2. 实现列表页面
3. 实现创建/编辑表单

### 阶段 2: 详情和工具
4. 实现详情页面
5. 实现工具列表组件
6. 实现工具同步功能

### 阶段 3: 高级功能
7. 实现测试连接功能
8. 添加测试日志展示
9. 完善错误处理

---

## 十四、测试要点

### 功能测试
- [ ] 创建 STDIO 服务器
- [ ] 创建 HTTP 服务器
- [ ] 编辑服务器配置
- [ ] 删除服务器
- [ ] 同步工具
- [ ] 查看工具 Schema
- [ ] 测试连接

### 边界情况
- [ ] 连接类型切换时字段重置
- [ ] 认证类型切换时字段重置
- [ ] 空列表状态
- [ ] 同步失败处理
- [ ] 网络错误处理

---

**设计状态**: ✅ 完成
**下一步**: 创建实现计划

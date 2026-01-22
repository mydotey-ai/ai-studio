# 系统设置界面 (System Settings UI) 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 为 AI Studio 平台构建完整的系统设置管理界面,包括用户管理、组织设置、审计日志、存储配置等模块。

**架构:** 基于 Vue 3 + TypeScript + Element Plus 的前端架构,使用标签页(Tabs)布局组织多个设置模块,每个模块独立管理。调用后端现有的 REST API 进行数据交互。

**技术栈:** Vue 3.5+ (Composition API), TypeScript 5.3+, Vite 5.4+, Element Plus 2.13+, Pinia 2.3+, Axios 1.13+

---

## 后端 API 上下文

### 现有后端 API 端点

**用户管理 API** (`/api/users/*`):
- `GET /api/users` - 获取所有用户 (需要 ADMIN 或 SUPER_ADMIN 角色)
- `GET /api/users/{id}` - 获取用户详情
- `PUT /api/users/{id}` - 更新用户信息
- `PATCH /api/users/{id}/status?status=ACTIVE` - 更新用户状态 (需要管理员)
- `DELETE /api/users/{id}` - 删除用户 (需要管理员)

**组织管理 API** (`/api/organizations/*`):
- `GET /api/organizations/my` - 获取当前用户的组织
- `GET /api/organizations/{id}` - 获取组织详情
- `PUT /api/organizations/{id}` - 更新组织信息 (需要管理员)
- `POST /api/organizations` - 创建组织

**存储配置 API** (`/api/storage-configs/*`):
- `GET /api/storage-configs` - 获取所有存储配置
- `GET /api/storage-configs/{id}` - 获取存储配置详情
- `GET /api/storage-configs/default` - 获取默认存储配置
- `POST /api/storage-configs` - 创建存储配置 (需要管理员)
- `PUT /api/storage-configs/{id}` - 更新存储配置 (需要管理员)
- `DELETE /api/storage-configs/{id}` - 删除存储配置 (需要管理员)

**审计日志实体** (`AuditLog`):
- 字段: id, userId, action, resourceType, resourceId, details (JSONB), ipAddress, userAgent, createdAt
- 注意: 后端暂无审计日志查询 API,需要添加

### 前端现有文件

**已存在:**
- `frontend/src/views/settings/SettingsView.vue` - 占位文件
- `frontend/src/types/user.ts` - 用户类型定义
- `frontend/src/api/auth.ts` - 认证 API

**需要创建:**
- 系统设置相关 API 函数
- 审计日志类型定义
- 存储配置类型定义

---

## 实施任务

### Task 1: 添加后端审计日志查询 API

**文件:**
- Modify: `src/main/java/com/mydotey/ai/studio/mapper/AuditLogMapper.java`
- Modify: `src/main/resources/mapper/AuditLogMapper.xml`
- Create: `src/main/java/com/mydotey/ai/studio/dto/audit/AuditLogQueryRequest.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/audit/AuditLogResponse.java`
- Modify: `src/main/java/com/mydotey/ai/studio/service/AuditLogService.java`
- Create: `src/main/java/com/mydotey/ai/studio/controller/AuditLogController.java`

**Step 1: 添加 DTO 定义**

创建文件: `src/main/java/com/mydotey/ai/studio/dto/audit/AuditLogQueryRequest.java`

```java
package com.mydotey.ai.studio.dto.audit;

import lombok.Data;

import java.time.Instant;

@Data
public class AuditLogQueryRequest {
    private Long userId;
    private String action;
    private String resourceType;
    private Long resourceId;
    private Instant startDate;
    private Instant endDate;
    private Integer page = 1;
    private Integer pageSize = 20;
}
```

创建文件: `src/main/java/com/mydotey/ai/studio/dto/audit/AuditLogResponse.java`

```java
package com.mydotey.ai.studio.dto.audit;

import lombok.Data;

import java.time.Instant;

@Data
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String details;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
}
```

**Step 2: 添加 Mapper 查询方法**

修改文件: `src/main/java/com/mydotey/ai/studio/mapper/AuditLogMapper.java`

```java
package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mydotey.ai.studio.dto.audit.AuditLogQueryRequest;
import com.mydotey.ai.studio.dto.audit.AuditLogResponse;
import com.mydotey.ai.studio.entity.AuditLog;
import org.apache.ibatis.annotations.Param;

public interface AuditLogMapper extends BaseMapper<AuditLog> {

    IPage<AuditLogResponse> queryAuditLogs(
            Page<AuditLogResponse> page,
            @Param("query") AuditLogQueryRequest query
    );
}
```

修改文件: `src/main/resources/mapper/AuditLogMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.mydotey.ai.studio.mapper.AuditLogMapper">

    <select id="queryAuditLogs" resultType="com.mydotey.ai.studio.dto.audit.AuditLogResponse">
        SELECT
            al.id,
            al.user_id,
            u.username,
            al.action,
            al.resource_type,
            al.resource_id,
            al.details,
            al.ip_address,
            al.user_agent,
            al.created_at
        FROM audit_logs al
        LEFT JOIN users u ON al.user_id = u.id
        <where>
            <if test="query.userId != null">
                AND al.user_id = #{query.userId}
            </if>
            <if test="query.action != null and query.action != ''">
                AND al.action LIKE CONCAT('%', #{query.action}, '%')
            </if>
            <if test="query.resourceType != null and query.resourceType != ''">
                AND al.resource_type = #{query.resourceType}
            </if>
            <if test="query.resourceId != null">
                AND al.resource_id = #{query.resourceId}
            </if>
            <if test="query.startDate != null">
                AND al.created_at &gt;= #{query.startDate}
            </if>
            <if test="query.endDate != null">
                AND al.created_at &lt;= #{query.endDate}
            </if>
        </where>
        ORDER BY al.created_at DESC
    </select>

</mapper>
```

**Step 3: 添加 Service 方法**

修改文件: `src/main/java/com/mydotey/ai/studio/service/AuditLogService.java`

```java
package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mydotey.ai.studio.dto.audit.AuditLogQueryRequest;
import com.mydotey.ai.studio.dto.audit.AuditLogResponse;
import com.mydotey.ai.studio.entity.AuditLog;
import com.mydotey.ai.studio.mapper.AuditLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public void logAudit(Long userId, String action, String resourceType, Long resourceId, Object details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);

            if (details != null) {
                auditLog.setDetails(objectMapper.writeValueAsString(details));
            } else {
                auditLog.setDetails("{}");
            }

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }

            auditLog.setCreatedAt(Instant.now());
            auditLogMapper.insert(auditLog);

        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    public IPage<AuditLogResponse> queryAuditLogs(AuditLogQueryRequest query) {
        Page<AuditLogResponse> page = new Page<>(query.getPage(), query.getPageSize());
        return auditLogMapper.queryAuditLogs(page, query);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

**Step 4: 创建 Controller**

创建文件: `src/main/java/com/mydotey/ai/studio/controller/AuditLogController.java`

```java
package com.mydotey.ai.studio.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mydotey.ai.studio.annotation.RequireRole;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.audit.AuditLogQueryRequest;
import com.mydotey.ai.studio.dto.audit.AuditLogResponse;
import com.mydotey.ai.studio.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "审计日志", description = "审计日志查询相关接口")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @Operation(summary = "查询审计日志", description = "分页查询审计日志")
    public ApiResponse<IPage<AuditLogResponse>> queryAuditLogs(@ModelAttribute AuditLogQueryRequest query) {
        IPage<AuditLogResponse> result = auditLogService.queryAuditLogs(query);
        return ApiResponse.success(result);
    }
}
```

**Step 5: 提交后端更改**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio
git add .
git commit -m "feat: add audit log query API for system settings"
```

---

### Task 2: 创建前端类型定义

**文件:**
- Create: `frontend/src/types/audit.ts`
- Create: `frontend/src/types/storage.ts`

**Step 1: 创建审计日志类型**

创建文件: `frontend/src/types/audit.ts`

```typescript
export interface AuditLog {
  id: number
  userId: number
  username: string
  action: string
  resourceType: string
  resourceId?: number
  details?: string
  ipAddress?: string
  userAgent?: string
  createdAt: string
}

export interface AuditLogQuery {
  userId?: number
  action?: string
  resourceType?: string
  resourceId?: number
  startDate?: string
  endDate?: string
  page?: number
  pageSize?: number
}

export interface AuditLogListResponse {
  records: AuditLog[]
  total: number
  current: number
  size: number
}
```

**Step 2: 创建存储配置类型**

创建文件: `frontend/src/types/storage.ts`

```typescript
export enum StorageType {
  LOCAL = 'LOCAL',
  OSS = 'OSS',
  S3 = 'S3'
}

export interface StorageConfig {
  id: number
  name: string
  type: StorageType
  isDefault: boolean
  endpoint?: string
  bucket?: string
  region?: string
  accessKey?: string
  secret?: string
  uploadPath?: string
  createdAt: string
  updatedAt: string
}

export interface CreateStorageConfigRequest {
  name: string
  type: StorageType
  endpoint?: string
  bucket?: string
  region?: string
  accessKey?: string
  secret?: string
  uploadPath?: string
  isDefault?: boolean
}

export interface UpdateStorageConfigRequest {
  name?: string
  endpoint?: string
  bucket?: string
  region?: string
  accessKey?: string
  secret?: string
  uploadPath?: string
}
```

**Step 3: 提交类型定义**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio/frontend
git add src/types/audit.ts src/types/storage.ts
git commit -m "feat: add audit log and storage config type definitions"
```

---

### Task 3: 创建前端 API 函数

**文件:**
- Create: `frontend/src/api/user.ts`
- Create: `frontend/src/api/organization.ts`
- Create: `frontend/src/api/audit.ts`
- Create: `frontend/src/api/storage.ts`

**Step 1: 创建用户管理 API**

创建文件: `frontend/src/api/user.ts`

```typescript
import request from './request'
import type { User, UserStatus } from '@/types/user'

export interface UpdateUserRequest {
  username?: string
  email?: string
  currentPassword?: string
  newPassword?: string
}

export const userApi = {
  // 获取所有用户 (管理员)
  getAllUsers() {
    return request<User[], never>({
      url: '/users',
      method: 'get'
    })
  },

  // 获取用户详情
  getUserById(id: number) {
    return request<User, never>({
      url: `/users/${id}`,
      method: 'get'
    })
  },

  // 更新用户信息
  updateUser(id: number, data: UpdateUserRequest) {
    return request<void, UpdateUserRequest>({
      url: `/users/${id}`,
      method: 'put',
      data
    })
  },

  // 更新用户状态 (管理员)
  updateUserStatus(id: number, status: UserStatus) {
    return request<void, never>({
      url: `/users/${id}/status?status=${status}`,
      method: 'patch'
    })
  },

  // 删除用户 (管理员)
  deleteUser(id: number) {
    return request<void, never>({
      url: `/users/${id}`,
      method: 'delete'
    })
  }
}
```

**Step 2: 创建组织管理 API**

创建文件: `frontend/src/api/organization.ts`

```typescript
import request from './request'
import type { Organization } from '@/types/user'

export interface CreateOrganizationRequest {
  name: string
  description?: string
}

export const organizationApi = {
  // 获取当前用户的组织
  getMyOrganization() {
    return request<Organization, never>({
      url: '/organizations/my',
      method: 'get'
    })
  },

  // 获取组织详情
  getOrganizationById(id: number) {
    return request<Organization, never>({
      url: `/organizations/${id}`,
      method: 'get'
    })
  },

  // 更新组织信息 (管理员)
  updateOrganization(id: number, data: CreateOrganizationRequest) {
    return request<void, CreateOrganizationRequest>({
      url: `/organizations/${id}`,
      method: 'put',
      data
    })
  }
}
```

**Step 3: 创建审计日志 API**

创建文件: `frontend/src/api/audit.ts`

```typescript
import request from './request'
import type { AuditLogQuery, AuditLogListResponse } from '@/types/audit'

export const auditApi = {
  // 查询审计日志 (管理员)
  queryAuditLogs(query: AuditLogQuery) {
    return request<AuditLogListResponse, AuditLogQuery>({
      url: '/audit-logs',
      method: 'get',
      params: query
    })
  }
}
```

**Step 4: 创建存储配置 API**

创建文件: `frontend/src/api/storage.ts`

```typescript
import request from './request'
import type {
  StorageConfig,
  CreateStorageConfigRequest,
  UpdateStorageConfigRequest
} from '@/types/storage'

export const storageApi = {
  // 获取所有存储配置
  getAllConfigs() {
    return request<StorageConfig[], never>({
      url: '/storage-configs',
      method: 'get'
    })
  },

  // 获取存储配置详情
  getConfigById(id: number) {
    return request<StorageConfig, never>({
      url: `/storage-configs/${id}`,
      method: 'get'
    })
  },

  // 获取默认存储配置
  getDefaultConfig() {
    return request<StorageConfig, never>({
      url: '/storage-configs/default',
      method: 'get'
    })
  },

  // 创建存储配置 (管理员)
  createConfig(data: CreateStorageConfigRequest) {
    return request<StorageConfig, CreateStorageConfigRequest>({
      url: '/storage-configs',
      method: 'post',
      data
    })
  },

  // 更新存储配置 (管理员)
  updateConfig(id: number, data: UpdateStorageConfigRequest) {
    return request<StorageConfig, UpdateStorageConfigRequest>({
      url: `/storage-configs/${id}`,
      method: 'put',
      data
    })
  },

  // 删除存储配置 (管理员)
  deleteConfig(id: number) {
    return request<void, never>({
      url: `/storage-configs/${id}`,
      method: 'delete'
    })
  }
}
```

**Step 5: 提交 API 函数**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio/frontend
git add src/api/user.ts src/api/organization.ts src/api/audit.ts src/api/storage.ts
git commit -m "feat: add system settings API functions"
```

---

### Task 4: 实现用户管理组件

**文件:**
- Create: `frontend/src/views/settings/components/UserManagement.vue`

**Step 1: 创建用户管理组件**

创建文件: `frontend/src/views/settings/components/UserManagement.vue`

```vue
<template>
  <div class="user-management">
    <div class="header">
      <h3>用户管理</h3>
      <el-text type="info" size="small">管理系统用户和权限</el-text>
    </div>

    <el-table
      :data="users"
      :loading="loading"
      stripe
      v-loading="loading"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column label="角色" width="150">
        <template #default="{ row }">
          <el-tag :type="getRoleType(row.role)" size="small">
            {{ getRoleLabel(row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            size="small"
            @click="handleEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="row.status === 'ACTIVE'"
            link
            type="warning"
            size="small"
            @click="handleUpdateStatus(row, 'INACTIVE')"
          >
            禁用
          </el-button>
          <el-button
            v-else
            link
            type="success"
            size="small"
            @click="handleUpdateStatus(row, 'ACTIVE')"
          >
            启用
          </el-button>
          <el-button
            link
            type="danger"
            size="small"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑用户对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑用户"
      width="500px"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { userApi } from '@/api/user'
import type { User, UserStatus } from '@/types/user'
import dayjs from 'dayjs'

const users = ref<User[]>([])
const loading = ref(false)
const saving = ref(false)
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()

const editForm = ref<Partial<User>>({
  id: 0,
  username: '',
  email: ''
})

const editRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const loadUsers = async () => {
  loading.value = true
  try {
    const data = await userApi.getAllUsers()
    users.value = data.data
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = (user: User) => {
  editForm.value = { ...user }
  editDialogVisible.value = true
}

const handleSave = async () => {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async (valid) => {
    if (!valid) return

    saving.value = true
    try {
      await userApi.updateUser(editForm.value.id!, {
        email: editForm.value.email
      })
      ElMessage.success('更新成功')
      editDialogVisible.value = false
      await loadUsers()
    } catch (error) {
      ElMessage.error('更新失败')
    } finally {
      saving.value = false
    }
  })
}

const handleUpdateStatus = async (user: User, status: UserStatus) => {
  try {
    await ElMessageBox.confirm(
      `确定要${status === 'ACTIVE' ? '启用' : '禁用'}用户 ${user.username} 吗?`,
      '确认操作',
      { type: 'warning' }
    )

    await userApi.updateUserStatus(user.id, status)
    ElMessage.success('操作成功')
    await loadUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleDelete = async (user: User) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 ${user.username} 吗?此操作不可恢复!`,
      '确认删除',
      { type: 'error' }
    )

    await userApi.deleteUser(user.id)
    ElMessage.success('删除成功')
    await loadUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const getRoleType = (role: string) => {
  const map: Record<string, any> = {
    'USER': '',
    'ADMIN': 'warning',
    'SUPER_ADMIN': 'danger'
  }
  return map[role] || ''
}

const getRoleLabel = (role: string) => {
  const map: Record<string, string> = {
    'USER': '普通用户',
    'ADMIN': '管理员',
    'SUPER_ADMIN': '超级管理员'
  }
  return map[role] || role
}

const getStatusType = (status: string) => {
  const map: Record<string, any> = {
    'ACTIVE': 'success',
    'INACTIVE': 'info',
    'LOCKED': 'danger'
  }
  return map[status] || ''
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    'ACTIVE': '正常',
    'INACTIVE': '禁用',
    'LOCKED': '锁定'
  }
  return map[status] || status
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
}

.header h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
}
</style>
```

**Step 2: 提交用户管理组件**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio/frontend
git add src/views/settings/components/UserManagement.vue
git commit -m "feat: add user management component"
```

---

### Task 5: 实现组织设置组件

**文件:**
- Create: `frontend/src/views/settings/components/OrganizationSettings.vue`

**Step 1: 创建组织设置组件**

创建文件: `frontend/src/views/settings/components/OrganizationSettings.vue`

```vue
<template>
  <div class="organization-settings">
    <div class="header">
      <h3>组织设置</h3>
      <el-text type="info" size="small">管理组织基本信息</el-text>
    </div>

    <el-card v-loading="loading">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        :disabled="!isEditable"
      >
        <el-form-item label="组织名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入组织名称" />
        </el-form-item>

        <el-form-item label="组织描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入组织描述"
          />
        </el-form-item>

        <el-form-item label="创建时间">
          <el-text>{{ organization?.createdAt ? formatDate(organization.createdAt) : '-' }}</el-text>
        </el-form-item>

        <el-form-item>
          <el-button
            v-if="!isEditable"
            type="primary"
            @click="isEditable = true"
          >
            编辑
          </el-button>
          <template v-else>
            <el-button type="primary" @click="handleSave" :loading="saving">
              保存
            </el-button>
            <el-button @click="handleCancel">取消</el-button>
          </template>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { organizationApi } from '@/api/organization'
import type { Organization } from '@/types/user'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const organization = ref<Organization>()
const loading = ref(false)
const saving = ref(false)
const isEditable = ref(false)

const form = ref<Partial<Organization>>({
  name: '',
  description: ''
})

const originalForm = ref<Partial<Organization>>({
  name: '',
  description: ''
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入组织名称', trigger: 'blur' },
    { min: 2, max: 50, message: '组织名称长度为 2-50 个字符', trigger: 'blur' }
  ]
}

const loadOrganization = async () => {
  loading.value = true
  try {
    const data = await organizationApi.getMyOrganization()
    organization.value = data.data
    form.value = {
      name: data.data.name,
      description: data.data.description
    }
    originalForm.value = { ...form.value }
  } catch (error) {
    ElMessage.error('加载组织信息失败')
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    saving.value = true
    try {
      await organizationApi.updateOrganization(organization.value!.id, {
        name: form.value.name!,
        description: form.value.description
      })
      ElMessage.success('保存成功')
      isEditable.value = false
      await loadOrganization()
    } catch (error) {
      ElMessage.error('保存失败')
    } finally {
      saving.value = false
    }
  })
}

const handleCancel = () => {
  form.value = { ...originalForm.value }
  isEditable.value = false
}

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

onMounted(() => {
  loadOrganization()
})
</script>

<style scoped>
.organization-settings {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
}

.header h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
}
</style>
```

**Step 2: 提交组织设置组件**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio/frontend
git add src/views/settings/components/OrganizationSettings.vue
git commit -m "feat: add organization settings component"
```

---

### Task 6: 实现审计日志组件

**文件:**
- Create: `frontend/src/views/settings/components/AuditLogs.vue`

**Step 1: 创建审计日志组件**

创建文件: `frontend/src/views/settings/components/AuditLogs.vue`

```vue
<template>
  <div class="audit-logs">
    <div class="header">
      <h3>审计日志</h3>
      <el-text type="info" size="small">查看系统操作记录</el-text>
    </div>

    <el-card class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="用户">
          <el-input
            v-model="query.username"
            placeholder="用户名"
            clearable
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-input
            v-model="query.action"
            placeholder="操作类型"
            clearable
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select
            v-model="query.resourceType"
            placeholder="资源类型"
            clearable
            @clear="handleSearch"
          >
            <el-option label="用户" value="User" />
            <el-option label="组织" value="Organization" />
            <el-option label="知识库" value="KnowledgeBase" />
            <el-option label="文档" value="Document" />
            <el-option label="Agent" value="Agent" />
            <el-option label="聊天机器人" value="Chatbot" />
            <el-option label="MCP服务器" value="McpServer" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table
      :data="logs"
      :loading="loading"
      stripe
      v-loading="loading"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="action" label="操作类型" width="180">
        <template #default="{ row }">
          <el-tag size="small">{{ formatAction(row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="resourceType" label="资源类型" width="120" />
      <el-table-column prop="resourceId" label="资源ID" width="100" />
      <el-table-column prop="ipAddress" label="IP地址" width="140" />
      <el-table-column prop="createdAt" label="时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            size="small"
            @click="handleViewDetails(row)"
          >
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadLogs"
        @size-change="loadLogs"
      />
    </div>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="日志详情"
      width="600px"
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item label="ID">{{ selectedLog?.id }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ selectedLog?.username }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ formatAction(selectedLog?.action || '') }}</el-descriptions-item>
        <el-descriptions-item label="资源类型">{{ selectedLog?.resourceType }}</el-descriptions-item>
        <el-descriptions-item label="资源ID">{{ selectedLog?.resourceId }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ selectedLog?.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="User Agent">
          <el-text truncated>{{ selectedLog?.userAgent }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="时间">
          {{ selectedLog ? formatDate(selectedLog.createdAt) : '' }}
        </el-descriptions-item>
        <el-descriptions-item label="详情">
          <pre class="json-details">{{ formatDetails(selectedLog?.details) }}</pre>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { auditApi } from '@/api/audit'
import type { AuditLog, AuditLogQuery } from '@/types/audit'
import dayjs from 'dayjs'

const logs = ref<AuditLog[]>([])
const loading = ref(false)
const detailDialogVisible = ref(false)
const selectedLog = ref<AuditLog>()
const dateRange = ref<[string, string]>([])

const query = ref<AuditLogQuery>({
  page: 1,
  pageSize: 20
})

const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0
})

const loadLogs = async () => {
  loading.value = true
  try {
    const data = await auditApi.queryAuditLogs(query.value)
    logs.value = data.data.records
    pagination.value.total = data.data.total
    pagination.value.page = data.data.current
    pagination.value.pageSize = data.data.size
  } catch (error) {
    ElMessage.error('加载审计日志失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.value.page = 1
  loadLogs()
}

const handleReset = () => {
  query.value = {
    page: 1,
    pageSize: 20
  }
  dateRange.value = []
  loadLogs()
}

const handleDateChange = (values: [string, string] | null) => {
  if (values) {
    query.value.startDate = values[0]
    query.value.endDate = values[1]
  } else {
    delete query.value.startDate
    delete query.value.endDate
  }
}

const handleViewDetails = (log: AuditLog) => {
  selectedLog.value = log
  detailDialogVisible.value = true
}

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const formatAction = (action: string) => {
  const map: Record<string, string> = {
    'USER_LOGIN': '用户登录',
    'USER_LOGOUT': '用户登出',
    'USER_CREATE': '创建用户',
    'USER_UPDATE': '更新用户',
    'USER_DELETE': '删除用户',
    'USER_STATUS_UPDATE': '更新用户状态',
    'ORGANIZATION_CREATE': '创建组织',
    'ORGANIZATION_UPDATE': '更新组织',
    'KB_CREATE': '创建知识库',
    'KB_UPDATE': '更新知识库',
    'KB_DELETE': '删除知识库',
    'DOC_UPLOAD': '上传文档',
    'DOC_DELETE': '删除文档',
    'AGENT_CREATE': '创建Agent',
    'AGENT_UPDATE': '更新Agent',
    'AGENT_DELETE': '删除Agent',
    'AGENT_EXECUTE': '执行Agent',
    'CHATBOT_CREATE': '创建聊天机器人',
    'CHATBOT_UPDATE': '更新聊天机器人',
    'CHATBOT_DELETE': '删除聊天机器人',
    'MCP_CREATE': '创建MCP服务器',
    'MCP_UPDATE': '更新MCP服务器',
    'MCP_DELETE': '删除MCP服务器',
    'STORAGE_CONFIG_CREATE': '创建存储配置',
    'STORAGE_CONFIG_UPDATE': '更新存储配置',
    'STORAGE_CONFIG_DELETE': '删除存储配置'
  }
  return map[action] || action
}

const formatDetails = (details?: string) => {
  if (!details) return '{}'
  try {
    return JSON.stringify(JSON.parse(details), null, 2)
  } catch {
    return details
  }
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.audit-logs {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
}

.header h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
}

.filter-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.json-details {
  margin: 0;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 4px;
  font-size: 12px;
  max-height: 300px;
  overflow-y: auto;
}
</style>
```

**Step 2: 提交审计日志组件**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio/frontend
git add src/views/settings/components/AuditLogs.vue
git commit -m "feat: add audit logs component"
```

---

### Task 7: 实现存储配置组件

**文件:**
- Create: `frontend/src/views/settings/components/StorageConfig.vue`

**Step 1: 创建存储配置组件**

创建文件: `frontend/src/views/settings/components/StorageConfig.vue`

```vue
<template>
  <div class="storage-config">
    <div class="header">
      <h3>存储配置</h3>
      <el-text type="info" size="small">管理文件存储配置</el-text>
    </div>

    <div class="actions">
      <el-button type="primary" :icon="Plus" @click="handleCreate">
        新建配置
      </el-button>
    </div>

    <el-table
      :data="configs"
      :loading="loading"
      stripe
      v-loading="loading"
    >
      <el-table-column prop="name" label="名称" width="200" />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          <el-tag :type="getTypeType(row.type)" size="small">
            {{ getTypeLabel(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="endpoint" label="端点" min-width="200" show-overflow-tooltip />
      <el-table-column prop="bucket" label="Bucket" width="150" />
      <el-table-column prop="region" label="区域" width="120" />
      <el-table-column label="默认" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault" type="success" size="small">是</el-tag>
          <el-text v-else type="info">否</el-text>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">
            编辑
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑存储配置' : '新建存储配置'"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="配置名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入配置名称" />
        </el-form-item>

        <el-form-item label="存储类型" prop="type">
          <el-select
            v-model="form.type"
            placeholder="请选择存储类型"
            :disabled="isEdit"
            @change="handleTypeChange"
          >
            <el-option label="本地存储" value="LOCAL" />
            <el-option label="阿里云 OSS" value="OSS" />
            <el-option label="AWS S3" value="S3" />
          </el-select>
        </el-form-item>

        <template v-if="form.type !== 'LOCAL'">
          <el-form-item label="端点" prop="endpoint">
            <el-input v-model="form.endpoint" placeholder="请输入端点地址" />
          </el-form-item>

          <el-form-item label="Bucket" prop="bucket">
            <el-input v-model="form.bucket" placeholder="请输入 Bucket 名称" />
          </el-form-item>

          <el-form-item label="区域" prop="region">
            <el-input v-model="form.region" placeholder="请输入区域" />
          </el-form-item>

          <el-form-item label="Access Key" prop="accessKey">
            <el-input v-model="form.accessKey" placeholder="请输入 Access Key" />
          </el-form-item>

          <el-form-item label="Secret" prop="secret">
            <el-input
              v-model="form.secret"
              type="password"
              placeholder="请输入 Secret"
              show-password
            />
          </el-form-item>
        </template>

        <el-form-item v-if="form.type === 'LOCAL'" label="上传路径" prop="uploadPath">
          <el-input v-model="form.uploadPath" placeholder="请输入上传路径" />
        </el-form-item>

        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { storageApi } from '@/api/storage'
import type { StorageConfig, CreateStorageConfigRequest, StorageType } from '@/types/storage'
import dayjs from 'dayjs'

const configs = ref<StorageConfig[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const form = ref<CreateStorageConfigRequest>({
  name: '',
  type: 'LOCAL' as StorageType,
  uploadPath: '',
  isDefault: false
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入配置名称', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择存储类型', trigger: 'change' }
  ],
  endpoint: [
    { required: true, message: '请输入端点地址', trigger: 'blur' }
  ],
  bucket: [
    { required: true, message: '请输入 Bucket 名称', trigger: 'blur' }
  ],
  accessKey: [
    { required: true, message: '请输入 Access Key', trigger: 'blur' }
  ],
  secret: [
    { required: true, message: '请输入 Secret', trigger: 'blur' }
  ]
}

const loadConfigs = async () => {
  loading.value = true
  try {
    const data = await storageApi.getAllConfigs()
    configs.value = data.data
  } catch (error) {
    ElMessage.error('加载存储配置失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  isEdit.value = false
  form.value = {
    name: '',
    type: 'LOCAL' as StorageType,
    uploadPath: '',
    isDefault: false
  }
  dialogVisible.value = true
}

const handleEdit = (config: StorageConfig) => {
  isEdit.value = true
  form.value = {
    name: config.name,
    type: config.type,
    endpoint: config.endpoint,
    bucket: config.bucket,
    region: config.region,
    accessKey: config.accessKey,
    secret: '', // 不回显 secret
    uploadPath: config.uploadPath,
    isDefault: config.isDefault
  }
  dialogVisible.value = true
}

const handleTypeChange = () => {
  // 清空类型特定字段
  if (form.value.type === 'LOCAL') {
    form.value.endpoint = undefined
    form.value.bucket = undefined
    form.value.region = undefined
    form.value.accessKey = undefined
    form.value.secret = undefined
  } else {
    form.value.uploadPath = undefined
  }
}

const handleSave = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    saving.value = true
    try {
      if (isEdit.value) {
        // 编辑模式 - 需要获取当前编辑的配置 ID
        const currentConfig = configs.value.find(c => c.name === form.value.name)
        if (currentConfig) {
          await storageApi.updateConfig(currentConfig.id, form.value)
          ElMessage.success('更新成功')
        }
      } else {
        await storageApi.createConfig(form.value)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      await loadConfigs()
    } catch (error) {
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = async (config: StorageConfig) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除存储配置 "${config.name}" 吗?`,
      '确认删除',
      { type: 'error' }
    )

    await storageApi.deleteConfig(config.id)
    ElMessage.success('删除成功')
    await loadConfigs()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const getTypeType = (type: string) => {
  const map: Record<string, any> = {
    'LOCAL': 'info',
    'OSS': 'success',
    'S3': 'warning'
  }
  return map[type] || ''
}

const getTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    'LOCAL': '本地',
    'OSS': 'OSS',
    'S3': 'S3'
  }
  return map[type] || type
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.storage-config {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
}

.header h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
}

.actions {
  margin-bottom: 20px;
}
</style>
```

**Step 2: 提交存储配置组件**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio/frontend
git add src/views/settings/components/StorageConfig.vue
git commit -m "feat: add storage config component"
```

---

### Task 8: 实现系统设置主视图

**文件:**
- Modify: `frontend/src/views/settings/SettingsView.vue`

**Step 1: 更新系统设置视图**

修改文件: `frontend/src/views/settings/SettingsView.vue`

```vue
<template>
  <div class="settings-view">
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="用户管理" name="users">
        <UserManagement />
      </el-tab-pane>

      <el-tab-pane label="组织设置" name="organization">
        <OrganizationSettings />
      </el-tab-pane>

      <el-tab-pane label="审计日志" name="audit">
        <AuditLogs />
      </el-tab-pane>

      <el-tab-pane label="存储配置" name="storage">
        <StorageConfig />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import UserManagement from './components/UserManagement.vue'
import OrganizationSettings from './components/OrganizationSettings.vue'
import AuditLogs from './components/AuditLogs.vue'
import StorageConfig from './components/StorageConfig.vue'

const activeTab = ref('users')
</script>

<style scoped>
.settings-view {
  height: 100%;
}

.settings-view :deep(.el-tabs__content) {
  height: calc(100vh - 200px);
  overflow-y: auto;
}
</style>
```

**Step 2: 提交系统设置视图**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio/frontend
git add src/views/settings/SettingsView.vue
git commit -m "feat: implement settings view with tabs"
```

---

### Task 9: 验证和测试

**文件:**
- None (运行测试)

**Step 1: TypeScript 类型检查**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio/frontend
npm run type-check
```

预期输出: 无类型错误

**Step 2: ESLint 代码检查**

```bash
npm run lint
```

预期输出: 无 ESLint 错误

**Step 3: 生产构建测试**

```bash
npm run build
```

预期输出: 构建成功,无错误

**Step 4: 启动开发服务器**

```bash
npm run dev
```

预期输出: 开发服务器成功启动在 http://localhost:5173

**Step 5: 手动功能测试**

在浏览器中访问 http://localhost:5173 并进行以下测试:

1. **用户管理测试**:
   - 访问 /settings 路由
   - 切换到"用户管理"标签页
   - 验证用户列表正确加载
   - 测试编辑用户功能
   - 测试启用/禁用用户功能
   - 测试删除用户功能

2. **组织设置测试**:
   - 切换到"组织设置"标签页
   - 验证组织信息正确加载
   - 测试编辑组织信息功能
   - 验证保存后数据更新

3. **审计日志测试**:
   - 切换到"审计日志"标签页
   - 验证日志列表正确加载
   - 测试筛选功能(用户、操作类型、资源类型、时间范围)
   - 测试分页功能
   - 测试查看日志详情

4. **存储配置测试**:
   - 切换到"存储配置"标签页
   - 验证配置列表正确加载
   - 测试创建本地存储配置
   - 测试创建 OSS/S3 配置
   - 测试编辑配置功能
   - 测试删除配置功能

**Step 6: 提交验证结果**

```bash
cd /home/koqizhao/Projects/mydotey-ai/ai-studio
git add frontend/
git commit -m "feat: complete system settings UI implementation"
```

---

## 总结

完成此计划后,系统设置界面将包含:

1. ✅ **用户管理** - 完整的用户列表、编辑、状态管理、删除功能
2. ✅ **组织设置** - 组织信息查看和编辑
3. ✅ **审计日志** - 分页查询、多维度筛选、详情查看
4. ✅ **存储配置** - 本地/OSS/S3 配置的创建、编辑、删除

**新增文件统计:**
- 后端: 5 个新文件 (DTOs、Controller、Mapper XML)
- 前端: 8 个新文件 (类型、API、组件)

**测试覆盖:**
- TypeScript 类型检查
- ESLint 代码规范检查
- 生产构建测试
- 手动功能测试

**符合规范:**
- 遵循 Vue 3 Composition API 最佳实践
- 100% TypeScript 类型安全
- Element Plus UI 一致性
- 与现有前端架构保持一致

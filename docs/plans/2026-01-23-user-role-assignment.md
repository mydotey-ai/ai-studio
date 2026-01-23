# 用户角色分配功能实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 为用户管理界面添加角色分配功能，允许管理员修改用户角色（USER/ADMIN/SUPER_ADMIN）。

**架构:** 后端添加角色更新 API，前端在用户编辑对话框中添加角色选择下拉框，支持角色变更的权限验证。

**Tech Stack:** Spring Boot 3.5, MyBatis-Plus, Vue 3.5, TypeScript, Element Plus

---

## 实施任务概览

本计划包含以下任务组:

1. **后端增强** - 在 UpdateUserRequest 添加角色字段，UserService 添加角色更新逻辑
2. **API 更新** - 确保 UserController 支持角色更新
3. **前端类型** - 更新 User 类型定义
4. **前端 API** - 更新 userApi 调用
5. **UI 增强** - 在 UserManagement 组件添加角色选择功能
6. **权限验证** - 确保只有超级管理员可以提升为超级管理员
7. **测试验证** - 功能测试和代码质量检查

---

## Task Group 1: 后端增强

### Task 1.1: 修改 UpdateUserRequest 添加角色字段

**Files:**
- Modify: `src/main/java/com/mydotey/ai/studio/dto/UpdateUserRequest.java`

**Step 1: 添加角色字段**

在 `UpdateUserRequest.java` 中添加角色字段：

```java
package com.mydotey.ai.studio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Email(message = "Invalid email format")
    private String email;

    private String avatarUrl;

    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    private String newPassword;

    private String currentPassword;

    // 新增：角色字段（仅管理员可修改）
    private String role;
}
```

**Step 2: 提交修改**

```bash
git add src/main/java/com/mydotey/ai/studio/dto/UpdateUserRequest.java
git commit -m "feat: add role field to UpdateUserRequest"
```

---

### Task 1.2: 增强 UserService 角色更新逻辑

**Files:**
- Modify: `src/main/java/com/mydotey/ai/studio/service/UserService.java`

**Step 1: 在 updateUser 方法中添加角色更新逻辑**

在 UserService.java 的 updateUser 方法中（约第 86 行后）添加角色更新逻辑：

```java
// 更新角色 - 需要管理员权限
if (request.getRole() != null && !request.getRole().isEmpty()) {
    // 验证角色值
    if (!"USER".equals(request.getRole()) &&
        !"ADMIN".equals(request.getRole()) &&
        !"SUPER_ADMIN".equals(request.getRole())) {
        throw new BusinessException("Invalid role value");
    }

    // 只有超级管理员可以分配/修改超级管理员角色
    if ("SUPER_ADMIN".equals(request.getRole())) {
        String operatorRole = getUserRole(currentUserId);
        if (!"SUPER_ADMIN".equals(operatorRole)) {
            throw new BusinessException("Only super admin can assign super admin role");
        }
    }

    // 管理员不能将自己的角色降级
    if (userId.equals(currentUserId) && !"SUPER_ADMIN".equals(getUserRole(currentUserId))) {
        throw new BusinessException("Cannot modify your own role");
    }

    user.setRole(request.getRole());
}
```

插入位置：在 `userMapper.updateById(user);` 之前（约第 106 行）

**Step 2: 提交修改**

```bash
git add src/main/java/com/mydotey/ai/studio/service/UserService.java
git commit -m "feat: add role update logic to UserService with permission checks"
```

---

### Task 1.3: 验证 UserController 支持

**Files:**
- Check: `src/main/java/com/mydotey/ai/studio/controller/UserController.java`

**Step 1: 确认 updateUser 端点**

检查 UserController.java 的 updateUser 方法（约第 35-43 行）：

```java
@PutMapping("/{id}")
@AuditLog(action = "USER_UPDATE", resourceType = "User", resourceIdParam = "id")
public ApiResponse<Void> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UpdateUserRequest request,
        @RequestAttribute("userId") Long currentUserId) {
    userService.updateUser(id, request, currentUserId);
    return ApiResponse.success("User updated successfully", null);
}
```

确认：✅ 端点已存在，会自动支持 role 字段更新

**Step 3: 添加权限注解（可选）**

如果需要限制只有管理员才能更新角色，可以添加注解：

```java
@PutMapping("/{id}")
@RequireRole({"ADMIN", "SUPER_ADMIN"})
@AuditLog(action = "USER_UPDATE", resourceType = "User", resourceIdParam = "id")
public ApiResponse<Void> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody UpdateUserRequest request,
        @RequestAttribute("userId") Long currentUserId) {
    userService.updateUser(id, request, currentUserId);
    return ApiResponse.success("User updated successfully", null);
}
```

**Step 4: 如果添加了注解，提交修改**

```bash
git add src/main/java/com/mydotey/ai/studio/controller/UserController.java
git commit -m "feat: add ADMIN role requirement to updateUser endpoint"
```

---

## Task Group 2: 前端类型和 API

### Task 2.1: 更新前端 UpdateUserRequest 类型

**Files:**
- Modify: `frontend/src/api/user.ts`

**Step 1: 添加 role 字段**

修改 frontend/src/api/user.ts 中的 UpdateUserRequest 接口：

```typescript
export interface UpdateUserRequest {
  username?: string
  email?: string
  avatarUrl?: string
  role?: 'USER' | 'ADMIN' | 'SUPER_ADMIN'  // 新增
  newPassword?: string
  currentPassword?: string
}
```

**Step 2: 提交修改**

```bash
git add frontend/src/api/user.ts
git commit -m "feat: add role field to UpdateUserRequest type"
```

---

### Task 2.2: 更新 User 类型定义

**Files:**
- Check: `frontend/src/types/user.ts`

**Step 1: 确认 User 类型已有 role 字段**

检查 frontend/src/types/user.ts 的 User 接口（约第 13-23 行）：

```typescript
export interface User {
  id: number
  username: string
  email: string
  role: UserRole  // ✅ 已存在
  status: UserStatus
  avatarUrl?: string
  orgId?: number
  createdAt: string
  updatedAt: string
}
```

确认：✅ User 类型已有 role 字段

---

## Task Group 3: UI 增强

### Task 3.1: 修改 UserManagement 组件添加角色选择

**Files:**
- Modify: `frontend/src/views/settings/components/UserManagement.vue`

**Step 1: 在编辑表单中添加角色选择**

在 UserManagement.vue 的编辑表单中（约第 59-66 行），添加角色选择：

```vue
<el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
  <el-form-item label="用户名" prop="username">
    <el-input v-model="editForm.username" disabled />
  </el-form-item>
  <el-form-item label="邮箱" prop="email">
    <el-input v-model="editForm.email" />
  </el-form-item>
  <el-form-item label="角色" prop="role">
    <el-select v-model="editForm.role" placeholder="请选择角色" style="width: 100%">
      <el-option label="普通用户" value="USER" />
      <el-option label="管理员" value="ADMIN" />
      <el-option label="超级管理员" value="SUPER_ADMIN" />
    </el-select>
  </el-form-item>
</el-form>
```

**Step 2: 更新 editForm 初始值**

修改 editForm 的初始值（约第 90-94 行）：

```typescript
const editForm = ref<Partial<User>>({
  id: 0,
  username: '',
  email: '',
  role: 'USER'  // 新增默认值
})
```

**Step 3: 更新 handleSave 方法**

修改 handleSave 方法（约第 120-140 行），添加角色更新：

```typescript
const handleSave = async () => {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async valid => {
    if (!valid) return

    saving.value = true
    try {
      await userApi.updateUser(editForm.value.id!, {
        email: editForm.value.email,
        role: editForm.value.role  // 新增
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
```

**Step 4: 添加权限验证提示**

在 script 部分添加当前用户角色检查（约第 82 行后）：

```typescript
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 检查是否可以修改为超级管理员
const canAssignSuperAdmin = computed(() => {
  return userStore.user?.role === 'SUPER_ADMIN'
})

// 检查是否可以修改角色
const canModifyRole = computed(() => {
  const currentUserRole = userStore.user?.role
  return currentUserRole === 'ADMIN' || currentUserRole === 'SUPER_ADMIN'
})
```

**Step 5: 更新角色选择 UI，添加权限控制**

修改角色选择表单项（Step 1 中的代码），添加禁用状态：

```vue
<el-form-item label="角色" prop="role">
  <el-select
    v-model="editForm.role"
    placeholder="请选择角色"
    :disabled="!canModifyRole"
    style="width: 100%"
  >
    <el-option label="普通用户" value="USER" />
    <el-option label="管理员" value="ADMIN" />
    <el-option
      label="超级管理员"
      value="SUPER_ADMIN"
      :disabled="!canAssignSuperAdmin"
    />
  </el-select>
  <el-text v-if="!canModifyRole" type="info" size="small">
    只有管理员才能修改角色
  </el-text>
</el-form-item>
```

**Step 6: 添加样式**

在 style 部分添加提示样式（约第 221 行后）：

```css
.el-text {
  margin-top: 4px;
  display: block;
}
```

**Step 7: 提交修改**

```bash
git add frontend/src/views/settings/components/UserManagement.vue
git commit -m "feat: add role selection to user edit dialog with permission checks"
```

---

## Task Group 4: 测试验证

### Task 4.1: 后端编译和测试

**Step 1: 后端编译**

```bash
mvn clean compile
```

预期输出: `BUILD SUCCESS`

**Step 2: 运行测试（如果有）**

```bash
mvn test -Dtest=UserServiceTest
```

---

### Task 4.2: 前端构建测试

**Step 1: TypeScript 类型检查**

```bash
cd frontend
npm run build
```

预期输出: 构建成功，无类型错误

**Step 2: ESLint 检查**

```bash
npm run lint
```

预期输出: 无新的错误或警告

---

### Task 4.3: 手动功能测试

**Step 1: 启动后端服务器**

```bash
mvn spring-boot:run
```

**Step 2: 启动前端开发服务器**

```bash
cd frontend
npm run dev
```

**Step 3: 测试角色分配功能**

登录后访问设置页面，测试以下场景：

**场景 1: 超级管理员修改角色**
- 登录超级管理员账号
- 进入用户管理
- 编辑一个用户
- 修改角色为 USER/ADMIN/SUPER_ADMIN
- 保存并验证角色已更新

**场景 2: 普通管理员修改角色**
- 登录普通管理员账号
- 编辑一个用户
- 尝试选择 SUPER_ADMIN（应该禁用）
- 只能选择 USER/ADMIN
- 保存并验证角色已更新

**场景 3: 普通用户**
- 登录普通用户账号
- 角色选择应该禁用
- 不能修改角色

**场景 4: 不能修改自己的角色**
- 尝试修改当前登录用户的角色
- 应该显示错误提示

---

## 最终提交和文档更新

### Task 5.1: 最终代码提交

**Step 1: 检查所有修改**

```bash
git status
```

**Step 2: 提交所有前端更改**

```bash
git add frontend/
git commit -m "feat: implement user role assignment feature"
```

---

### Task 5.2: 更新项目进度文档

**Files:**
- Modify: `docs/PROJECT_PROGRESS.md`

**Step 1: 在 Phase 11 前端实现部分添加记录**

在 Phase 11 的"已完成任务"部分，第 10 项后添加：

```markdown
11. ✅ **用户角色分配功能** (Commits: XXX, XXX)
   - 后端 UpdateUserRequest 添加角色字段
   - UserService 角色更新逻辑和权限验证
   - 前端用户编辑对话框添加角色选择
   - 超级管理员专属权限控制
   - 防止修改自己角色的保护机制
```

**Step 2: 更新下一步计划**

从"下一步计划"中移除"用户权限界面增强"：

```markdown
**下一步计划:**
- 性能优化和缓存
- 文件管理界面
- 数据导入导出功能
```

**Step 3: 提交进度更新**

```bash
git add docs/PROJECT_PROGRESS.md
git commit -m "docs: mark user role assignment feature as completed"
```

---

## 总结

完成此计划后，用户角色分配功能将包含：

✅ **后端实现:**
- UpdateUserRequest 支持角色字段
- UserService 角色更新逻辑
- 权限验证（只有超级管理员可以分配超级管理员角色）
- 防止修改自己角色的保护机制

✅ **前端实现:**
- 用户编辑对话框添加角色选择下拉框
- 基于当前用户角色的权限控制
- 超级管理员选项禁用状态
- 角色修改提示信息

✅ **安全特性:**
- 角色值验证（USER/ADMIN/SUPER_ADMIN）
- 超级管理员专属权限
- 防止权限提升攻击
- 审计日志记录

**新增文件统计:**
- 后端: 2 个文件修改（约 40 行新增）
- 前端: 2 个文件修改（约 60 行新增）

**总计:** 约 100 行代码，提供完整的用户角色分配功能。

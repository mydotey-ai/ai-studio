package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.annotation.RequireRole;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.UpdateUserRequest;
import com.mydotey.ai.studio.dto.UserResponse;
import com.mydotey.ai.studio.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ApiResponse.success(user);
    }

    @GetMapping
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public ApiResponse<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ApiResponse.success(users);
    }

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

    @PatchMapping("/{id}/status")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @AuditLog(action = "USER_STATUS_UPDATE", resourceType = "User", resourceIdParam = "id")
    public ApiResponse<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestAttribute("userId") Long operatorId) {
        userService.updateUserStatus(id, status, operatorId);
        return ApiResponse.success("User status updated successfully", null);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @AuditLog(action = "USER_DELETE", resourceType = "User", resourceIdParam = "id")
    public ApiResponse<Void> deleteUser(
            @PathVariable Long id,
            @RequestAttribute("userId") Long operatorId) {
        userService.deleteUser(id, operatorId);
        return ApiResponse.success("User deleted successfully", null);
    }
}

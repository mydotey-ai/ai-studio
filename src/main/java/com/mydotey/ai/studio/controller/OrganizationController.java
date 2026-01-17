package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.annotation.RequireRole;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.CreateOrganizationRequest;
import com.mydotey.ai.studio.dto.OrganizationResponse;
import com.mydotey.ai.studio.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @AuditLog(action = "ORGANIZATION_CREATE", resourceType = "Organization")
    public ApiResponse<OrganizationResponse> create(
            @Valid @RequestBody CreateOrganizationRequest request,
            @RequestAttribute("userId") Long userId) {
        OrganizationResponse org = organizationService.create(request, userId);
        return ApiResponse.success(org);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrganizationResponse> getById(@PathVariable Long id) {
        OrganizationResponse org = organizationService.getById(id);
        return ApiResponse.success(org);
    }

    @GetMapping("/my")
    public ApiResponse<OrganizationResponse> getMyOrganization(@RequestAttribute("userId") Long userId) {
        OrganizationResponse org = organizationService.getUserOrganization(userId);
        return ApiResponse.success(org);
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @AuditLog(action = "ORGANIZATION_UPDATE", resourceType = "Organization", resourceIdParam = "id")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateOrganizationRequest request) {
        organizationService.update(id, request);
        return ApiResponse.success("Organization updated successfully", null);
    }
}

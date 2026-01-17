package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.CreateOrganizationRequest;
import com.mydotey.ai.studio.dto.OrganizationResponse;
import com.mydotey.ai.studio.entity.Organization;
import com.mydotey.ai.studio.entity.User;
import com.mydotey.ai.studio.mapper.OrganizationMapper;
import com.mydotey.ai.studio.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationMapper organizationMapper;
    private final UserMapper userMapper;

    /**
     * 创建组织
     */
    @Transactional
    public OrganizationResponse create(CreateOrganizationRequest request, Long userId) {
        // 检查用户是否已有组织（简化版：每个用户只能创建一个组织）
        User user = userMapper.selectById(userId);
        if (user != null && user.getOrgId() != null) {
            throw new BusinessException("User already belongs to an organization");
        }

        Organization org = new Organization();
        org.setName(request.getName());
        org.setDescription(request.getDescription());
        org.setSettings("{}");

        organizationMapper.insert(org);

        // 将创建者关联到组织
        if (user != null) {
            user.setOrgId(org.getId());
            userMapper.updateById(user);
        }

        return toResponse(org);
    }

    /**
     * 获取组织信息
     */
    public OrganizationResponse getById(Long orgId) {
        Organization org = organizationMapper.selectById(orgId);
        if (org == null) {
            throw new BusinessException("Organization not found");
        }
        return toResponse(org);
    }

    /**
     * 获取用户的组织信息
     */
    public OrganizationResponse getUserOrganization(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getOrgId() == null) {
            throw new BusinessException("User organization not found");
        }
        return getById(user.getOrgId());
    }

    /**
     * 更新组织信息
     */
    @Transactional
    public void update(Long orgId, CreateOrganizationRequest request) {
        Organization org = organizationMapper.selectById(orgId);
        if (org == null) {
            throw new BusinessException("Organization not found");
        }

        org.setName(request.getName());
        org.setDescription(request.getDescription());

        organizationMapper.updateById(org);
    }

    /**
     * 转换为响应对象
     */
    private OrganizationResponse toResponse(Organization org) {
        OrganizationResponse response = new OrganizationResponse();
        response.setId(org.getId());
        response.setName(org.getName());
        response.setDescription(org.getDescription());
        response.setSettings(org.getSettings());
        response.setCreatedAt(org.getCreatedAt());
        response.setUpdatedAt(org.getUpdatedAt());
        return response;
    }
}

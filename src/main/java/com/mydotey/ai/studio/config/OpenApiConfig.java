package com.mydotey.ai.studio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 配置类
 * 配置 API 文档信息和 JWT Bearer 认证方案
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aiStudioOpenAPI() {
        // Security scheme for JWT
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        // Server configuration
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8080");
        devServer.setDescription("Development server");

        Server prodServer = new Server();
        prodServer.setUrl("https://api.example.com");
        prodServer.setDescription("Production server");

        // API Information
        Contact contact = new Contact()
                .name("AI Studio Team")
                .email("support@mydotey.ai")
                .url("https://mydotey.ai");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("AI Studio API")
                .version("1.0.0")
                .description("""
                        AI Studio 是一个基于 Spring Boot 3.5 的 AI 开发平台。

                        ## 主要功能

                        * **知识库管理** - 创建和管理知识库
                        * **文档处理** - 上传和处理文档 (PDF, Word, TXT)
                        * **RAG 查询** - 基于知识库的检索增强生成
                        * **Agent 系统** - MCP 工具调用和 ReAct 工作流
                        * **聊天机器人** - 创建和管理聊天机器人
                        * **网页抓取** - 自动抓取和处理网页内容
                        * **文件存储** - 多存储类型支持 (本地/OSS/S3)
                        * **用户认证** - JWT 认证和权限管理

                        ## 认证方式

                        大部分 API 需要 JWT Bearer Token 认证。使用 `/api/auth/login` 登录获取 token。

                        ## 错误码

                        * `400` - 请求参数错误
                        * `401` - 未认证
                        * `403` - 权限不足
                        * `404` - 资源不存在
                        * `500` - 服务器内部错误
                        """)
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer))
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement);
    }
}

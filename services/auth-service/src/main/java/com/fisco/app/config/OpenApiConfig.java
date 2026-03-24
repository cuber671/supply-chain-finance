package com.fisco.app.config;

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

import java.util.Arrays;

/**
 * OpenAPI 3.0 全局配置
 *
 * 配置项：
 * - API 基本信息（标题、版本、描述）
 * - 服务器地址
 * - JWT Bearer 认证安全方案
 * - 全局 Tag 分类
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI supplyChainFinanceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("供应链金融平台 API - 认证服务")
                        .version("1.0.0")
                        .description("FISCO BCOS 供应链金融平台 - 认证服务模块 API 文档\n\n" +
                                "## 功能模块\n" +
                                "- **用户认证**：登录、登出、Token刷新、Token验证\n" +
                                "- **用户管理**：注册、信息查询、状态变更、角色变更\n" +
                                "- **企业管理**：员工管理、审核、注销流程\n\n" +
                                "## 认证说明\n" +
                                "除登录/刷新Token等公开接口外，所有业务接口均需要 JWT Bearer Token 认证：\n" +
                                "```\n" +
                                "Authorization: Bearer {access_token}\n" +
                                "```")
                        .contact(new Contact()
                                .name("FISCO BCOS 团队")
                                .email("support@fisco.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:8081")
                                .description("本地开发环境"),
                        new Server()
                                .url("http://172.26.0.11:8081")
                                .description("Docker 环境")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer Token 认证，登录接口获取 Token 后在此处填写")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}

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
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI supplyChainFinanceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("供应链金融平台 API - 信用服务")
                        .version("1.0.0")
                        .description("FISCO BCOS 供应链金融平台 - 信用服务模块 API 文档\n\n" +
                                "## 功能模块\n" +
                                "- **信用画像**：企业信用画像查询、信用评分查询\n" +
                                "- **信用事件**：信用事件上报、物流偏航扣分\n" +
                                "- **授信额度**：额度设置、额度校验、额度锁定、可用额度查询\n" +
                                "- **信用评分**：信用等级重算、批量重算\n" +
                                "- **信用黑名单**：黑名单检查、触发、移除\n\n" +
                                "## 认证说明\n" +
                                "所有业务接口均需要 JWT Bearer Token 认证：\n" +
                                "```\n" +
                                "Authorization: Bearer {access_token}\n" +
                                "```\n\n" +
                                "## 角色说明\n" +
                                "- 部分接口需要 ADMIN 角色权限（如上报信用事件、设置额度、黑名单操作等）")
                        .contact(new Contact()
                                .name("FISCO BCOS 团队")
                                .email("support@fisco.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:8086")
                                .description("本地开发环境"),
                        new Server()
                                .url("http://172.26.0.16:8086")
                                .description("Docker 环境")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer Token 认证")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}

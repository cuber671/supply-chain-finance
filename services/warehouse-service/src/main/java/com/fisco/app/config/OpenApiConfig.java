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
                        .title("供应链金融平台 API - 仓储服务")
                        .version("1.0.0")
                        .description("FISCO BCOS 供应链金融平台 - 仓储服务模块 API 文档\n\n" +
                                "## 功能模块\n" +
                                "- **入库管理**：入库申请、确认、取消、查询\n" +
                                "- **仓单管理**：仓单签发、查询、质押锁定、还款解押、核销出库\n" +
                                "- **背书转让**：仓单背书发起、确认、撤回、记录查询\n" +
                                "- **拆分合并**：仓单拆分申请、合并申请、执行/驳回\n" +
                                "- **仓库管理**：创建仓库、查询仓库列表\n" +
                                "- **溯源查询**：仓单全路径溯源\n\n" +
                                "## 认证说明\n" +
                                "所有业务接口均需要 JWT Bearer Token 认证：\n" +
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
                                .url("http://localhost:8083")
                                .description("本地开发环境"),
                        new Server()
                                .url("http://172.26.0.13:8083")
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

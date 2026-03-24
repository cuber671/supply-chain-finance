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
                        .title("供应链金融平台 API - FISCO网关服务")
                        .version("1.0.0")
                        .description("FISCO BCOS 供应链金融平台 - FISCO区块链网关服务 API 文档\n\n" +
                                "## 功能模块\n" +
                                "- **区块链基础服务**：区块/交易查询、合约调用、账户管理\n" +
                                "- **企业上链**：企业注册、状态更新、信用评级、授信额度\n" +
                                "- **仓单上链**：签发、背书、拆分合并、质押锁定、核销\n" +
                                "- **物流上链**：委托单创建、提货确认、到货确认、交付\n" +
                                "- **贷款上链**：创建、审批、放款、还款、逾期/违约处理\n" +
                                "- **应收款上链**：创建、确认、调整、融资、结算\n\n" +
                                "## 认证说明\n" +
                                "所有接口均需要 JWT Bearer Token 认证：\n" +
                                "```\n" +
                                "Authorization: Bearer {access_token}\n" +
                                "```\n\n" +
                                "## 角色说明\n" +
                                "- 大多数区块链写入操作需要 ADMIN 角色\n" +
                                "- 部分操作（签发仓单等）需要 ADMIN 或 WAREHOUSE 角色")
                        .contact(new Contact()
                                .name("FISCO BCOS 团队")
                                .email("support@fisco.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:8087")
                                .description("本地开发环境"),
                        new Server()
                                .url("http://172.26.0.17:8087")
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

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
                        .title("供应链金融平台 API - 物流服务")
                        .version("1.0.0")
                        .description("FISCO BCOS 供应链金融平台 - 物流服务模块 API 文档\n\n" +
                                "## 功能模块\n" +
                                "- **委派单管理**：创建、查询委派单\n" +
                                "- **物流指派**：司机指派、提货确认\n" +
                                "- **物流追踪**：状态追踪、轨迹查询、偏航记录\n" +
                                "- **轨迹上报**：承运方上报物流轨迹\n" +
                                "- **状态更新**：物流状态更新、确认交付、使委派单失效\n\n" +
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
                                .url("http://localhost:8084")
                                .description("本地开发环境"),
                        new Server()
                                .url("http://172.26.0.14:8084")
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

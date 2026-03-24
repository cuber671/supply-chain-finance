package com.fisco.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * FISCO Gateway Service Application
 *
 * 区块链网关服务 - 提供统一的区块链操作入口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Tag(name = "FISCO区块链网关服务")
@SpringBootApplication
@ComponentScan(basePackages = {"com.fisco.app"})
@MapperScan("com.fisco.app.mapper")
public class FiscoGatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FiscoGatewayServiceApplication.class, args);
    }
}

package com.fisco.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 仓储服务启动类
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.fisco.app"})
@MapperScan("com.fisco.app.mapper")
@EnableFeignClients(basePackages = {"com.fisco.app.feign"})
public class WarehouseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WarehouseServiceApplication.class, args);
    }
}

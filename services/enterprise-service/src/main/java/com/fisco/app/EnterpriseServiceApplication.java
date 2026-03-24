package com.fisco.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 企业服务启动类
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.fisco.app"})
@MapperScan("com.fisco.app.mapper")
@EnableFeignClients(basePackages = {"com.fisco.app.feign"})
public class EnterpriseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseServiceApplication.class, args);
    }
}

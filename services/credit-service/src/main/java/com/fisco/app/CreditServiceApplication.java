package com.fisco.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 信用服务启动类
 *
 * 提供企业信用档案管理、信用事件上报、信用评分计算等服务
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@SpringBootApplication(scanBasePackages = {"com.fisco.app"})
@MapperScan("com.fisco.app.mapper")
@EnableFeignClients(basePackages = "com.fisco.app.feign")
public class CreditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreditServiceApplication.class, args);
    }
}

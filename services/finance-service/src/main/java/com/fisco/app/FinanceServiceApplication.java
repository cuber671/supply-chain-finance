package com.fisco.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 金融服务启动类
 *
 * 提供应收款管理和质押贷款服务
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@SpringBootApplication(scanBasePackages = {"com.fisco.app"})
@MapperScan("com.fisco.app.mapper")
@EnableFeignClients(basePackages = "com.fisco.app.feign")
public class FinanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceServiceApplication.class, args);
    }
}

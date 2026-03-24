package com.fisco.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 物流服务启动类
 *
 * 提供DPDO数字提货单全生命周期管理
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@SpringBootApplication(scanBasePackages = {"com.fisco.app"})
@MapperScan("com.fisco.app.mapper")
@EnableFeignClients(basePackages = "com.fisco.app.feign")
public class LogisticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsServiceApplication.class, args);
    }
}

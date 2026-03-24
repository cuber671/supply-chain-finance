package com.fisco.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Logger;
import feign.Retryer;

/**
 * Feign 全局配置
 */
@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100L, 1000L, 3);
    }
}

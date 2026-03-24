package com.fisco.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT配置类
 * 从环境变量读取JWT相关配置
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Configuration
public class JwtConfig {

    /**
     * JWT签名密钥
     */
    @Getter
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Access Token 过期时间（毫秒）
     * 默认2小时：7200000
     */
    @Getter
    @Value("${jwt.expiration:7200000}")
    private Long accessTokenExpiration;

    /**
     * Refresh Token 过期时间（毫秒）
     * 默认7天：604800000
     */
    @Getter
    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshTokenExpiration;

    public JwtConfig() {
        log.info("JWT配置加载完成");
    }
}

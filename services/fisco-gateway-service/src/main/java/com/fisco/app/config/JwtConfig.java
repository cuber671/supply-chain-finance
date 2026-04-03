package com.fisco.app.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.fisco.app.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * FISCO Gateway JWT配置
 * 初始化JwtUtil的静态密钥
 */
@Slf4j
@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:7200000}")
    private Long accessTokenExpiration;

    /**
     * 初始化JwtUtil的静态密钥
     */
    @PostConstruct
    public void init() {
        log.info("正在初始化JWT密钥...");
        JwtUtil.init(secret, accessTokenExpiration, 604800000L);
    }
}

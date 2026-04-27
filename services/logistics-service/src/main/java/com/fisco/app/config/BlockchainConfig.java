package com.fisco.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * FISCO BCOS 区块链配置类
 *
 * 注意：logistics-service 已重构为通过 Feign Client 调用 fisco-gateway-service
 * 此配置类仅用于禁用 FISCO SDK 相关功能，不再直接连接区块链
 */
@Configuration
public class BlockchainConfig {

    @Value("${fisco.enabled:false}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }
}

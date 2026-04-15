package com.fisco.app.config;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fisco.app.filter.BaseJwtAuthenticationFilter;

/**
 * FISCO Gateway JWT 过滤器
 * 继承BaseJwtAuthenticationFilter，复用公共认证逻辑
 *
 * 注意：fisco-gateway-service不连接Redis，不检查Token黑名单
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtAuthenticationFilter extends BaseJwtAuthenticationFilter {

    private static final String[] EXCLUDE_PATTERNS = {
            // 区块链基础查询（公开）
            "/api/v1/blockchain/status",
            "/api/v1/blockchain/health",
            "/api/v1/blockchain/blockNumber",
            "/api/v1/blockchain/block/",
            "/api/v1/blockchain/blockHash/",
            "/api/v1/blockchain/receipt/**",
            "/api/v1/blockchain/account",
            "/api/v1/blockchain/balance/",
            "/api/v1/blockchain/group",
            "/api/v1/blockchain/groups",
            // 健康检查
            "/health",
            "/error",
            "/",
            // Swagger
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**"
    };

    @Override
    protected String[] getExcludePatterns() {
        return EXCLUDE_PATTERNS;
    }

    @Override
    protected void onSkipAuthentication(HttpServletRequest request) {
        // 跳过认证的路径（如区块链查询），设置管理员属性以便 AOP @RequireRole(adminBypass=true) 通过
        request.setAttribute(ATTR_ROLE, "ADMIN");
        request.setAttribute(ATTR_SCOPE, 1);
    }
}

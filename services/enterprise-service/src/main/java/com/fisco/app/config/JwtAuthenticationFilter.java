package com.fisco.app.config;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.fisco.app.filter.BaseJwtAuthenticationFilter;
import com.fisco.app.util.AuditContext;
import com.fisco.app.util.JwtUtil;

/**
 * 企业服务 JWT 过滤器
 * 继承BaseJwtAuthenticationFilter，复用公共认证逻辑
 */
@Component
public class JwtAuthenticationFilter extends BaseJwtAuthenticationFilter {

    private static final String[] EXCLUDE_PATTERNS = {
            "/api/v1/auth/login",
            "/api/v1/auth/admin/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/validate",
            "/api/v1/auth/users/register",
            "/api/v1/enterprise/register",
            "/api/v1/enterprise/login",
            "/api/v1/enterprise/invitation/validate",
            "/api/v1/enterprise/invite-codes/use",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**",
            "/error",
            "/",
            "/health",
            "/actuator/health"
    };

    @Override
    protected String[] getExcludePatterns() {
        return EXCLUDE_PATTERNS;
    }

    @Override
    protected void onAuthenticationSuccess(HttpServletRequest request) {
        // 记录审计上下文
        AuditContext.set(
                getUserId(request),
                getEntId(request),
                getRole(request),
                getScope(request),
                JwtUtil.getJti(getClaims(request))
        );
    }

    @Override
    protected void cleanup() {
        AuditContext.clear();
    }
}

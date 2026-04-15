package com.fisco.app.config;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.fisco.app.filter.BaseJwtAuthenticationFilter;

/**
 * 认证服务 JWT 过滤器
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
            "/api/v1/auth/enterprise/token",
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
        // 设置 Spring Security 上下文
        String role = getRole(request);
        if (role != null) {
            GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(getUserId(request), null, Collections.singletonList(authority));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
}

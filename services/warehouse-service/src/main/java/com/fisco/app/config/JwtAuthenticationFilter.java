package com.fisco.app.config;

import com.fisco.app.filter.BaseJwtAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationFilter extends BaseJwtAuthenticationFilter {

    private static final String[] EXCLUDE_PATTERNS = {
            "/api/v1/warehouse/auth/**",
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
}

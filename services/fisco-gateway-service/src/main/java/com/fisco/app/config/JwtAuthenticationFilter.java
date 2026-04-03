package com.fisco.app.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.util.JwtUtil;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;

/**
 * FISCO Gateway JWT认证过滤器
 * 解析JWT Token，提取用户信息并存入请求属性，供RoleAuthorizationInterceptor使用
 *
 * 注意：fisco-gateway-service不连接Redis，因此不检查Token黑名单
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String ATTR_CLAIMS = "jwt_claims";
    public static final String ATTR_USER_ID = "user_id";
    public static final String ATTR_ENT_ID = "ent_id";
    public static final String ATTR_ROLE = "role";
    public static final String ATTR_SCOPE = "scope";
    public static final String ATTR_ENT_ROLE = "ent_role";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 不需要拦截的URL模式（公开端点）
     */
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
            // 区块链查询类接口（需要认证但作为后备）
            "/api/v1/blockchain/enterprise/list",
            "/api/v1/blockchain/enterprise/**",
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
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            if (shouldSkipAuthentication(request)) {
                // 跳过认证的路径（如区块链查询），设置管理员属性以便 AOP @RequireRole(adminBypass=true) 通过
                request.setAttribute(ATTR_ROLE, "ADMIN");
                request.setAttribute(ATTR_SCOPE, 1);
                filterChain.doFilter(request, response);
                return;
            }

            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                log.warn("请求未携带有效的Authorization头: {}", request.getRequestURI());
                sendUnauthorizedResponse(response, "Missing or invalid Authorization header");
                return;
            }

            String token = authHeader.substring(BEARER_PREFIX.length());

            Claims claims = JwtUtil.parseToken(token);
            if (claims == null) {
                log.warn("Token解析失败: {}", request.getRequestURI());
                sendUnauthorizedResponse(response, "Invalid token");
                return;
            }

            if (!JwtUtil.isAccessToken(claims)) {
                log.warn("非Access Token访问: {}", request.getRequestURI());
                sendUnauthorizedResponse(response, "Invalid token type, expected access token");
                return;
            }

            if (!JwtUtil.validateToken(token)) {
                log.warn("Token已过期: {}", request.getRequestURI());
                sendUnauthorizedResponse(response, "Token has expired");
                return;
            }

            // 将用户信息存入请求属性，供CurrentUser和RoleAuthorizationInterceptor使用
            request.setAttribute(ATTR_CLAIMS, claims);
            request.setAttribute(ATTR_USER_ID, JwtUtil.getSubId(claims));
            request.setAttribute(ATTR_ENT_ID, JwtUtil.getEntId(claims));
            request.setAttribute(ATTR_ROLE, JwtUtil.getRole(claims));
            request.setAttribute(ATTR_SCOPE, JwtUtil.getScope(claims));
            request.setAttribute(ATTR_ENT_ROLE, JwtUtil.getEntRole(claims));

            log.debug("JWT认证成功，用户ID: {}, 角色: {}, URI: {}",
                    JwtUtil.getSubId(claims), JwtUtil.getRole(claims), request.getRequestURI());

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("JWT认证异常: {}", e.getMessage(), e);
            sendUnauthorizedResponse(response, "Authentication failed: " + e.getMessage());
        }
    }

    private boolean shouldSkipAuthentication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String pattern : EXCLUDE_PATTERNS) {
            if (matchPattern(uri, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchPattern(String uri, String pattern) {
        if (pattern.contains("**")) {
            String prefix = pattern.replace("/**", "");
            return uri.startsWith(prefix);
        }
        if (pattern.contains("*")) {
            String prefix = pattern.substring(0, pattern.lastIndexOf('/'));
            return uri.startsWith(prefix);
        }
        return uri.equals(pattern);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> error = new HashMap<>();
        error.put("code", 401);
        error.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}

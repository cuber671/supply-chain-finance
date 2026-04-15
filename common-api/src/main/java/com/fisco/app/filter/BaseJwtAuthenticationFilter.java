package com.fisco.app.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.util.JwtUtil;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT认证过滤器基类 - 抽取公共逻辑到common-api
 *
 * 各服务继承此类，只需提供 excludePatterns 列表，
 * 认证逻辑统一在基类中维护。
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
public abstract class BaseJwtAuthenticationFilter extends OncePerRequestFilter {

    protected static final String BEARER_PREFIX = "Bearer ";
    protected static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String ATTR_CLAIMS = "jwt_claims";
    public static final String ATTR_USER_ID = "user_id";
    public static final String ATTR_ENT_ID = "ent_id";
    public static final String ATTR_ROLE = "role";
    public static final String ATTR_SCOPE = "scope";
    public static final String ATTR_ENT_ROLE = "ent_role";

    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 提供需要跳过认证的路径模式列表
     * 子类实现，返回该服务独有的公开路径
     */
    protected abstract String[] getExcludePatterns();

    /**
     * 可选：Token黑名单检查回调。
     * 默认实现返回 false（不检查黑名单）。
     * 需要黑名单检查的服务可覆盖此方法。
     *
     * @return true 表示 token 已被吊销，应拒绝访问
     */
    protected boolean isTokenBlacklisted(String jti) {
        return false;
    }

    /**
     * 跳过认证时的回调钩子，子类可覆盖
     * fisco-gateway-service 在此设置 ATTR_ROLE=ADMIN
     */
    protected void onSkipAuthentication(HttpServletRequest request) {
        // 默认空实现
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            if (shouldSkipAuthentication(request)) {
                onSkipAuthentication(request);
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

            // 黑名单检查（子类可选覆盖）
            String jti = JwtUtil.getJti(claims);
            if (isTokenBlacklisted(jti)) {
                log.warn("Token已被吊销: {}, JTI: {}", request.getRequestURI(), jti);
                sendUnauthorizedResponse(response, "Token has been revoked");
                return;
            }

            // 设置请求属性
            request.setAttribute(ATTR_CLAIMS, claims);
            request.setAttribute(ATTR_USER_ID, JwtUtil.getSubId(claims));
            request.setAttribute(ATTR_ENT_ID, JwtUtil.getEntId(claims));
            request.setAttribute(ATTR_ROLE, JwtUtil.getRole(claims));
            request.setAttribute(ATTR_SCOPE, JwtUtil.getScope(claims));
            request.setAttribute(ATTR_ENT_ROLE, JwtUtil.getEntRole(claims));

            log.debug("JWT认证成功，用户ID: {}, 角色: {}, URI: {}",
                    JwtUtil.getSubId(claims), JwtUtil.getRole(claims), request.getRequestURI());

            // 认证成功后回调，子类可覆盖（如设置Spring Security上下文）
            onAuthenticationSuccess(request);
            filterChain.doFilter(request, response);
        } finally {
            // 清理审计上下文（ThreadLocal，防止内存泄漏）
            cleanup();
        }
    }

    /**
     * 认证成功后的回调钩子，子类可覆盖
     * enterprise-service 在此设置 Spring Security 上下文
     */
    protected void onAuthenticationSuccess(HttpServletRequest request) {
        // 默认空实现
    }

    /**
     * 请求结束后清理资源，子类可覆盖
     */
    protected void cleanup() {
        // 默认空实现
    }

    protected boolean shouldSkipAuthentication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String pattern : getExcludePatterns()) {
            if (matchPattern(uri, pattern)) {
                return true;
            }
        }
        return false;
    }

    protected boolean matchPattern(String uri, String pattern) {
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

    protected void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> error = new HashMap<>();
        error.put("code", 401);
        error.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    // ==================== 静态工具方法 ====================

    public static Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(ATTR_USER_ID);
        return userId != null ? (Long) userId : null;
    }

    public static Long getEntId(HttpServletRequest request) {
        Object entId = request.getAttribute(ATTR_ENT_ID);
        return entId != null ? (Long) entId : null;
    }

    public static String getRole(HttpServletRequest request) {
        return (String) request.getAttribute(ATTR_ROLE);
    }

    public static Integer getScope(HttpServletRequest request) {
        Object scope = request.getAttribute(ATTR_SCOPE);
        return scope != null ? (Integer) scope : null;
    }

    public static Integer getEntRole(HttpServletRequest request) {
        Object entRole = request.getAttribute(ATTR_ENT_ROLE);
        return entRole != null ? (Integer) entRole : null;
    }

    public static Claims getClaims(HttpServletRequest request) {
        return (Claims) request.getAttribute(ATTR_CLAIMS);
    }
}

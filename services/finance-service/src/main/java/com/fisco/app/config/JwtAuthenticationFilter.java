package com.fisco.app.config;

import com.fisco.app.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String ATTR_CLAIMS = "jwt_claims";
    public static final String ATTR_USER_ID = "user_id";
    public static final String ATTR_ENT_ID = "ent_id";
    public static final String ATTR_ROLE = "role";
    public static final String ATTR_SCOPE = "scope";
    public static final String ATTR_ENT_ROLE = "ent_role";

    private static final String[] EXCLUDE_PATTERNS = {
            "/api/v1/finance/auth/**",
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
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            if (shouldSkipAuthentication(request)) {
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
            log.error("JWT认证异常: {}", e.getMessage());
            sendUnauthorizedResponse(response, "Authentication failed");
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
        return uri.equals(pattern);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> error = new HashMap<>();
        error.put("code", 401);
        error.put("message", message);
        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(error));
    }
}

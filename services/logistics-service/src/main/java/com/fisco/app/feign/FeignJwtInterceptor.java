package com.fisco.app.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Feign 请求拦截器 - 将当前请求的 JWT 传递到下游服务
 *
 * 从当前 HTTP 请求上下文中提取 Authorization 头（JWT），
 * 并添加到所有 Feign 远程调用的请求头中，
 * 确保下游服务能够获取到调用方的身份信息。
 */
@Component
public class FeignJwtInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization != null && !authorization.isEmpty()) {
            template.header(AUTHORIZATION_HEADER, authorization);
        }
    }
}

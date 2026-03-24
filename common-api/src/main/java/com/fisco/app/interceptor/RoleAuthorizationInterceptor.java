package com.fisco.app.interceptor;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fisco.app.annotation.RequireRole;
import com.fisco.app.util.CurrentUser;

import java.util.Arrays;

/**
 * 角色权限校验拦截器
 * 用于处理 @RequireRole 注解，在方法执行前进行角色权限校验
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class RoleAuthorizationInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RoleAuthorizationInterceptor.class);

    /**
     * 在标注了 @RequireRole 注解的方法执行前进行权限校验
     */
    @Before("@annotation(com.fisco.app.annotation.RequireRole)")
    public void checkRole(JoinPoint joinPoint) {
        try {
            // 获取目标方法
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            // 获取注解信息
            RequireRole requireRole = method.getAnnotation(RequireRole.class);
            if (requireRole == null) {
                return;
            }

            String[] allowedRoles = requireRole.value();
            boolean adminBypass = requireRole.adminBypass();

            // 系统管理员可绕过校验
            if (adminBypass && CurrentUser.isAdmin()) {
                logger.debug("系统管理员绕过角色校验，允许访问: {}.{}", joinPoint.getTarget().getClass().getSimpleName(), method.getName());
                return;
            }

            // 获取当前用户角色
            String currentRole = CurrentUser.getRole();
            if (currentRole == null) {
                logger.warn("角色校验失败：无法获取当前用户角色");
                throw new AuthorizationException("未登录或Token无效");
            }

            // 检查是否具有所需角色之一
            boolean hasPermission = Arrays.asList(allowedRoles).contains(currentRole);
            if (!hasPermission) {
                logger.warn("角色校验失败：用户角色 {} 不在允许列表 {} 中，方法: {}.{}",
                        currentRole, Arrays.toString(allowedRoles),
                        joinPoint.getTarget().getClass().getSimpleName(), method.getName());
                throw new AuthorizationException("权限不足，需要角色: " + Arrays.toString(allowedRoles));
            }

            logger.debug("角色校验通过：用户角色 {} 在允许列表 {} 中",
                    currentRole, Arrays.toString(allowedRoles));

        } catch (AuthorizationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("角色校验异常", e);
            throw new AuthorizationException("权限校验失败: " + e.getMessage());
        }
    }

    /**
     * 授权异常，用于在AOP中中断请求
     */
    public static class AuthorizationException extends RuntimeException {
        public AuthorizationException(String message) {
            super(message);
        }
    }
}

package com.fisco.app.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限校验注解
 * 用于标记需要特定角色才能访问的接口
 *
 * 使用示例：
 * <pre>
 * @RequireRole({"ADMIN", "FINANCE"})
 * @PostMapping("/issue")
 * public Result<?> issueBill(...) { ... }
 * </pre>
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 允许访问的角色列表
     * 如果用户角色在列表中，则允许访问
     */
    String[] value();

    /**
     * 是否需要系统管理员权限
     * 如果为true，系统管理员(scope=1)可绕过角色校验
     */
    boolean adminBypass() default true;
}

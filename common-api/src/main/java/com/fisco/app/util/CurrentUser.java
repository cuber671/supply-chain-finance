package com.fisco.app.util;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前用户信息工具类
 *
 * 用于从请求中获取当前登录用户的信息
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public class CurrentUser {

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        return getAttribute("user_id", Long.class);
    }

    /**
     * 获取当前企业ID
     */
    public static Long getEntId() {
        return getAttribute("ent_id", Long.class);
    }

    /**
     * 获取当前用户角色
     */
    public static String getRole() {
        return getAttribute("role", String.class);
    }

    /**
     * 获取当前用户权限范围 (1=系统管理员, 2=企业管理员, 3=普通用户)
     */
    public static Integer getScope() {
        return getAttribute("scope", Integer.class);
    }

    /**
     * 获取当前企业角色（用于仓单权限校验，6=金融机构，9=仓储方等）
     */
    public static Integer getEntRole() {
        return getAttribute("ent_role", Integer.class);
    }

    /**
     * 判断是否为系统管理员
     */
    public static boolean isAdmin() {
        Integer scope = getScope();
        return scope != null && scope == 1;
    }

    /**
     * 判断是否为企业管理员
     */
    public static boolean isEnterpriseAdmin() {
        Integer scope = getScope();
        return scope != null && (scope == 1 || scope == 2);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getAttribute(String name, Class<T> clazz) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        Object value = request.getAttribute(name);
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
}

package com.fisco.app.util;

/**
 * 审计上下文 - 存储当前请求的用户信息
 * 用于在业务层自动获取当前登录用户ID填充operator_id字段
 *
 * 工作原理：
 * 1. JwtAuthenticationFilter解析JWT后，将userId/entId/role存入ThreadLocal
 * 2. 业务代码通过AuditContext.getUserId()获取当前用户ID
 * 3. AOP切面在数据库更新时自动注入operator_id
 * 4. 请求结束后清理ThreadLocal
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public class AuditContext {

    /**
     * 线程本地变量 - 存储当前请求的用户信息
     */
    private static final ThreadLocal<AuditInfo> AUDIT_INFO_HOLDER = new ThreadLocal<>();

    /**
     * 审计信息内部类
     */
    private static class AuditInfo {
        Long userId;       // 用户ID (sub)
        Long entId;        // 企业ID
        String role;       // 角色
        Integer scope;     // 权限范围
        String jti;        // JWT唯一标识

        AuditInfo(Long userId, Long entId, String role, Integer scope, String jti) {
            this.userId = userId;
            this.entId = entId;
            this.role = role;
            this.scope = scope;
            this.jti = jti;
        }
    }

    /**
     * 设置当前审计信息
     */
    public static void set(Long userId, Long entId, String role, Integer scope, String jti) {
        AUDIT_INFO_HOLDER.set(new AuditInfo(userId, entId, role, scope, jti));
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        AuditInfo info = AUDIT_INFO_HOLDER.get();
        return info != null ? info.userId : null;
    }

    /**
     * 获取当前企业ID
     */
    public static Long getEntId() {
        AuditInfo info = AUDIT_INFO_HOLDER.get();
        return info != null ? info.entId : null;
    }

    /**
     * 获取当前用户角色
     */
    public static String getRole() {
        AuditInfo info = AUDIT_INFO_HOLDER.get();
        return info != null ? info.role : null;
    }

    /**
     * 获取当前权限范围
     */
    public static Integer getScope() {
        AuditInfo info = AUDIT_INFO_HOLDER.get();
        return info != null ? info.scope : null;
    }

    /**
     * 获取当前JWT唯一标识
     */
    public static String getJti() {
        AuditInfo info = AUDIT_INFO_HOLDER.get();
        return info != null ? info.jti : null;
    }

    /**
     * 检查是否已登录
     */
    public static boolean isPresent() {
        return AUDIT_INFO_HOLDER.get() != null;
    }

    /**
     * 清理当前线程的审计信息
     * 必须在请求结束时调用，防止内存泄漏
     */
    public static void clear() {
        AUDIT_INFO_HOLDER.remove();
    }
}

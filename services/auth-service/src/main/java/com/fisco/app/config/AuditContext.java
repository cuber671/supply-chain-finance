package com.fisco.app.config;

/**
 * 审计上下文
 * 用于在线程中存储当前用户的审计信息
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public class AuditContext {

    private static final ThreadLocal<AuditInfo> CONTEXT = new ThreadLocal<>();

    public static void set(Long userId, Long entId, String role, Integer scope, String jti) {
        AuditInfo info = new AuditInfo(userId, entId, role, scope, jti);
        CONTEXT.set(info);
    }

    public static AuditInfo get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static class AuditInfo {
        private final Long userId;
        private final Long entId;
        private final String role;
        private final Integer scope;
        private final String jti;

        public AuditInfo(Long userId, Long entId, String role, Integer scope, String jti) {
            this.userId = userId;
            this.entId = entId;
            this.role = role;
            this.scope = scope;
            this.jti = jti;
        }

        public Long getUserId() { return userId; }
        public Long getEntId() { return entId; }
        public String getRole() { return role; }
        public Integer getScope() { return scope; }
        public String getJti() { return jti; }
    }
}

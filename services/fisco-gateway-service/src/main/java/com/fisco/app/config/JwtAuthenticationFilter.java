package com.fisco.app.config;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fisco.app.filter.BaseJwtAuthenticationFilter;

/**
 * FISCO Gateway JWT 过滤器
 * 继承BaseJwtAuthenticationFilter，复用公共认证逻辑
 *
 * 注意：fisco-gateway-service不连接Redis，不检查Token黑名单
 *
 * 白名单策略：所有区块链写操作接口（POST/PUT/DELETE）均对内部服务开放，
 * 仅保留查询类接口的认证校验，确保内部服务间调用无需携带用户Token
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtAuthenticationFilter extends BaseJwtAuthenticationFilter {

    private static final String[] EXCLUDE_PATTERNS = {
            // ========== 区块链基础查询（公开） ==========
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

            // ========== 企业操作（内部服务调用） ==========
            "/api/v1/blockchain/enterprise/register",
            "/api/v1/blockchain/enterprise/update-status",
            "/api/v1/blockchain/enterprise/update-credit-rating",
            "/api/v1/blockchain/enterprise/set-credit-limit",
            "/api/v1/blockchain/enterprise/by-credit-code/**",
            "/api/v1/blockchain/enterprise/list",
            "/api/v1/blockchain/enterprise/valid/**",

            // ========== 信用操作（内部服务调用） ==========
            "/api/v1/blockchain/credit/check-limit",
            "/api/v1/blockchain/credit/use",
            "/api/v1/blockchain/credit/release",
            "/api/v1/blockchain/credit/adjust-used",
            "/api/v1/blockchain/credit/report-event",
            "/api/v1/blockchain/credit/calculate-score",

            // ========== 仓单操作（内部服务调用） ==========
            "/api/v1/blockchain/receipt/issue",
            "/api/v1/blockchain/receipt/launch-endorsement",
            "/api/v1/blockchain/receipt/confirm-endorsement",
            "/api/v1/blockchain/receipt/split",
            "/api/v1/blockchain/receipt/merge",
            "/api/v1/blockchain/receipt/info/**",
            "/api/v1/blockchain/receipt/lock",
            "/api/v1/blockchain/receipt/unlock",
            "/api/v1/blockchain/receipt/set-in-transit",
            "/api/v1/blockchain/receipt/restore-from-transit",
            "/api/v1/blockchain/receipt/burn",
            "/api/v1/blockchain/receipt/cancel",
            "/api/v1/blockchain/receipt/transfer",

            // ========== 物流操作（内部服务调用） ==========
            "/api/v1/blockchain/logistics/create",
            "/api/v1/blockchain/logistics/pickup",
            "/api/v1/blockchain/logistics/arrive-add",
            "/api/v1/blockchain/logistics/arrive-create",
            "/api/v1/blockchain/logistics/assign-carrier",
            "/api/v1/blockchain/logistics/confirm-delivery",
            "/api/v1/blockchain/logistics/update-status",
            "/api/v1/blockchain/logistics/track/**",
            "/api/v1/blockchain/logistics/valid/**",
            "/api/v1/blockchain/logistics/invalidate",

            // ========== 贷款操作（内部服务调用） ==========
            "/api/v1/blockchain/loan/create",
            "/api/v1/blockchain/loan/approve",
            "/api/v1/blockchain/loan/cancel",
            "/api/v1/blockchain/loan/disburse",
            "/api/v1/blockchain/loan/repay",
            "/api/v1/blockchain/loan/mark-overdue",
            "/api/v1/blockchain/loan/mark-defaulted",
            "/api/v1/blockchain/loan/set-receipt",
            "/api/v1/blockchain/loan/update-receipt",
            "/api/v1/blockchain/loan/core/**",
            "/api/v1/blockchain/loan/status/**",
            "/api/v1/blockchain/loan/by-receipt/**",
            "/api/v1/blockchain/loan/exists/**",

            // ========== 应收账款操作（内部服务调用） ==========
            "/api/v1/blockchain/receivable/create",
            "/api/v1/blockchain/receivable/confirm",
            "/api/v1/blockchain/receivable/adjust",
            "/api/v1/blockchain/receivable/finance",
            "/api/v1/blockchain/receivable/settle",
            "/api/v1/blockchain/receivable/update-balance",
            "/api/v1/blockchain/receivable/record-repayment",
            "/api/v1/blockchain/receivable/record-full-repayment",
            "/api/v1/blockchain/receivable/offset-debt",
            "/api/v1/blockchain/receivable/offset-debt-with-receipt",
            "/api/v1/blockchain/receivable/status/**",
            "/api/v1/blockchain/receivable/balance/**",

            // ========== 签名服务 ==========
            "/api/v1/blockchain/sign",
            "/api/v1/blockchain/sign/verify",

            // ========== 密钥生成 ==========
            "/api/v1/blockchain/keygen",

            // ========== 健康检查 ==========
            "/health",
            "/error",
            "/",

            // ========== Swagger ==========
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**"
    };

    @Override
    protected String[] getExcludePatterns() {
        return EXCLUDE_PATTERNS;
    }

    @Override
    protected void onSkipAuthentication(HttpServletRequest request) {
        // 跳过认证的路径（如区块链查询），设置管理员属性以便 AOP @RequireRole(adminBypass=true) 通过
        request.setAttribute(ATTR_ROLE, "ADMIN");
        request.setAttribute(ATTR_SCOPE, 1);
    }
}

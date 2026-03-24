package com.fisco.app.constant;

/**
 * 企业角色常量
 */
public class EntRoleConstant {

    public static final int CORE_ENTERPRISE = 1;
    public static final int SPOT_PLATFORM = 2;
    public static final int SUPPLIER = 3;
    public static final int FINANCIAL_INSTITUTION = 6;
    public static final int WAREHOUSE = 9;
    public static final int LOGISTICS = 12;

    public static boolean isWarehouse(Integer entRole) {
        return entRole != null && entRole == WAREHOUSE;
    }

    public static boolean isFinancialInstitution(Integer entRole) {
        return entRole != null && entRole == FINANCIAL_INSTITUTION;
    }

    public static boolean isReceiptOwner(Integer entRole) {
        return entRole != null && (entRole == CORE_ENTERPRISE || entRole == SUPPLIER || entRole == SPOT_PLATFORM);
    }

    public static String getRoleName(Integer entRole) {
        if (entRole == null) return "未知";
        switch (entRole) {
            case CORE_ENTERPRISE: return "核心企业";
            case SPOT_PLATFORM: return "现货交易平台";
            case SUPPLIER: return "供应商";
            case FINANCIAL_INSTITUTION: return "金融机构";
            case WAREHOUSE: return "仓储方";
            case LOGISTICS: return "物流方";
            default: return "未知";
        }
    }
}

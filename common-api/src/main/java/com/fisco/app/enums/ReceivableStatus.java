package com.fisco.app.enums;

/**
 * 应收款状态枚举
 * 对应实体 t_receivable.status 字段
 * 注意：链上 ReceivableCore.ReceivableStatus 值不同，转换时需使用 fromChainStatus/toChainStatus
 */
public enum ReceivableStatus {
    PENDING(1, "待确认"),              // 对应实体 STATUS_PENDING
    ACTIVE(2, "生效中"),               // 对应实体 STATUS_ACTIVE
    PARTIAL_REPAYMENT(3, "部分还款"),  // 对应实体 STATUS_PARTIAL_REPAYMENT
    SETTLED(4, "已结清"),              // 对应实体 STATUS_SETTLED
    OVERDUE(5, "逾期");                // 对应实体 STATUS_OVERDUE

    private final int value;
    private final String description;

    ReceivableStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 从数据库状态值转换为枚举（实体用）
     * @param dbStatus 数据库状态值
     * @return 对应的枚举，如果未找到则返回 null
     */
    public static ReceivableStatus fromDbStatus(int dbStatus) {
        for (ReceivableStatus status : values()) {
            if (status.value == dbStatus) {
                return status;
            }
        }
        return null;
    }

    /**
     * 转换为数据库状态值
     * @return 数据库状态值
     */
    public int toDbStatus() {
        return value;
    }

    /**
     * 从链上状态值转换为枚举
     * 链上状态: None(0), Created(1), Confirmed(2), Financing(3),
     *          Repaying(4), Settled(5), Overdue(6), Defaulted(7)
     * @param chainStatus 链上状态值
     * @return 对应的枚举，如果未找到则返回 null
     */
    public static ReceivableStatus fromChainStatus(int chainStatus) {
        switch (chainStatus) {
            case 0: return null;  // None - 不存在
            case 1: return PENDING;      // Created -> 待确认
            case 2: return ACTIVE;       // Confirmed -> 生效中
            case 3: return ACTIVE;       // Financing -> 生效中(融资中也是活跃状态)
            case 4: return PARTIAL_REPAYMENT;  // Repaying -> 部分还款
            case 5: return SETTLED;       // Settled -> 已结清
            case 6: return OVERDUE;      // Overdue -> 逾期
            case 7: return OVERDUE;      // Defaulted -> 逾期(严重逾期)
            default: return null;
        }
    }

    /**
     * 转换为链上状态值
     * @return 链上状态值
     */
    public int toChainStatus() {
        switch (this) {
            case PENDING: return 1;           // Created
            case ACTIVE: return 2;            // Confirmed
            case PARTIAL_REPAYMENT: return 4; // Repaying
            case SETTLED: return 5;            // Settled
            case OVERDUE: return 6;           // Overdue
            default: return 0;
        }
    }
}
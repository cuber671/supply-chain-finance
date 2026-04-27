package com.fisco.app.enums;

import lombok.Getter;

/**
 * 质押贷款状态枚举
 */
@Getter
public enum LoanStatus {

    PENDING(1, "待审批"),
    REJECTED(2, "已拒绝"),
    CANCELLED(3, "已取消"),
    PENDING_DISBURSE(4, "待放款"),
    DISBURSED(5, "已放款"),
    REPAYING(6, "还款中"),
    SETTLED(7, "已结清"),
    OVERDUE(8, "逾期"),
    DEFAULTED(9, "已违约");

    private final int code;
    private final String description;

    LoanStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static LoanStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (LoanStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public boolean isFinalStatus() {
        return this == SETTLED || this == DEFAULTED;
    }

    public boolean canCancel() {
        return this == PENDING || this == PENDING_DISBURSE;
    }

    public boolean canApprove() {
        return this == PENDING;
    }

    public boolean canDisburse() {
        return this == PENDING_DISBURSE;
    }

    /**
     * 从链上状态值转换为枚举
     * 链上状态: None(0), Pending(1), Approved(2), Disbursed(3), Repaying(4),
     *          Settled(5), Overdue(6), Defaulted(7), Cancelled(8), Disposed(9)
     * @param chainStatus 链上状态值
     * @return 对应的枚举，如果未找到则返回 null
     */
    public static LoanStatus fromChainStatus(int chainStatus) {
        switch (chainStatus) {
            case 1: return PENDING;
            case 2: return REJECTED;  // 链上无REJECTED，用REJECTED表示审批拒绝
            case 3: return DISBURSED;
            case 4: return REPAYING;
            case 5: return SETTLED;
            case 6: return OVERDUE;
            case 7: return DEFAULTED;
            case 8: return CANCELLED;
            default: return null;
        }
    }

    /**
     * 转换为链上状态值
     * @return 链上状态值
     */
    public int toChainStatus() {
        switch (this) {
            case PENDING: return 1;
            case REJECTED: return 2;   // 链上无REJECTED状态，上链时可能用其他状态替代
            case CANCELLED: return 8;
            case PENDING_DISBURSE: return 2;  // 链上Approved(2)最接近待放款
            case DISBURSED: return 3;
            case REPAYING: return 4;
            case SETTLED: return 5;
            case OVERDUE: return 6;
            case DEFAULTED: return 7;
            default: return 0;
        }
    }

    public boolean canRepay() {
        return this == DISBURSED || this == REPAYING || this == OVERDUE;
    }
}

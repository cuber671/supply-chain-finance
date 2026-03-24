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

    public boolean canRepay() {
        return this == DISBURSED || this == REPAYING || this == OVERDUE;
    }
}

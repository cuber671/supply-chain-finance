package com.fisco.app.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

/**
 * 企业状态枚举
 */
@Getter
public enum EnterpriseStatusEnum implements IEnum<Integer> {

    PENDING(0, "待审核"),
    NORMAL(1, "正常"),
    FROZEN(2, "已冻结"),
    CANCELLING(3, "申请注销中"),
    PENDING_CANCEL(5, "注销待审核"),
    CANCELLED(4, "已注销");

    private final int value;
    private final String desc;

    EnterpriseStatusEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }

    public boolean canExecuteTransaction() {
        return this == NORMAL;
    }

    public boolean canApplyCancellation() {
        return this == NORMAL;
    }
}

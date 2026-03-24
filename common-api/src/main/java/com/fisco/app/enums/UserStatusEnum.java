package com.fisco.app.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

/**
 * 用户状态枚举
 */
@Getter
public enum UserStatusEnum implements IEnum<Integer> {

    PENDING(1, "注册中（待审核）"),
    NORMAL(2, "正常"),
    FROZEN(3, "冻结"),
    CANCELLING(4, "注销中"),
    PENDING_CANCEL(6, "注销待审核"),
    CANCELLED(5, "已注销");

    private final int value;
    private final String desc;

    UserStatusEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }

    public boolean canLogin() {
        return this == NORMAL;
    }

    public boolean isLocked() {
        return this == CANCELLING || this == CANCELLED || this == FROZEN || this == PENDING_CANCEL;
    }

    public boolean canApplyCancellation() {
        return this == NORMAL;
    }
}

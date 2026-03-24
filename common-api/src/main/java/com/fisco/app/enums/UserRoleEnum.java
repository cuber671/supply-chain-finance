package com.fisco.app.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

/**
 * 用户职能角色枚举
 */
@Getter
public enum UserRoleEnum implements IEnum<String> {

    ADMIN("ADMIN", "管理员"),
    FINANCE("FINANCE", "财务"),
    OPERATOR("OPERATOR", "业务员");

    private final String value;
    private final String desc;

    UserRoleEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    public boolean hasApprovalPower() {
        return this == ADMIN || this == FINANCE;
    }
}

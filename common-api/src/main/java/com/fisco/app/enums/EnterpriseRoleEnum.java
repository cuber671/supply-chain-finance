package com.fisco.app.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

import lombok.Getter;

/**
 * 企业角色枚举
 */
@Getter
public enum EnterpriseRoleEnum implements IEnum<Integer> {

    CORE(1, "核心企业"),
    TRADING(2, "现货交易平台"),
    SUPPLIER(3, "供应商"),
    INSTITUTION(6, "金融机构"),
    WAREHOUSE(9, "仓储方"),
    LOGISTICS(12, "物流方");

    private final int value;
    private final String desc;

    EnterpriseRoleEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }
}

package com.fisco.app.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 仓单权限校验注解
 * 用于校验当前用户是否具有操作仓单的权限（基于企业角色）
 *
 * 企业角色说明：
 * - 1: 核心企业
 * - 2: 现货交易平台
 * - 3: 供应商
 * - 6: 金融机构
 * - 9: 仓储方
 * - 12: 物流方
 *
 * 使用场景：
 * - 仓单签发、拆分/合并执行、核销确认需要仓储方(9)
 * - 质押锁定/解押需要金融机构(6)
 * - 仓单转让需要当前持有人
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WarehousePermissionCheck {

    /**
     * 允许操作的企业角色
     * 空数组表示不校验角色
     */
    int[] allowedRoles() default {};

    /**
     * 是否要求企业必须是仓单的监管方（warehouse_ent_id匹配）
     * 适用于核销等需要监管方确认的操作
     */
    boolean requireWarehouseOwner() default false;

    /**
     * 是否要求企业必须是仓单的当前持有人（owner_ent_id匹配）
     * 适用于转让、拆分等需要持有人操作的动作
     */
    boolean requireReceiptOwner() default false;

    /**
     * 是否允许系统管理员(scope=1)绕过校验
     * 系统管理员可以操作所有仓单
     */
    boolean adminBypass() default true;

    /**
     * 当使用 requireWarehouseOwner 或 requireReceiptOwner 时，
     * 需要从请求中获取的仓单ID参数名
     */
    String receiptIdParam() default "receiptId";

    /**
     * 校验失败时的错误消息
     */
    String errorMessage() default "";
}
package com.fisco.app.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 物流指派历史记录实体类
 *
 * 对应数据库表: t_logistics_assign_history
 *
 * 【P2-4修复】记录委派单的指派变更历史，防止无限次更换司机导致的管理混乱
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_logistics_assign_history")
public class LogisticsAssignHistory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("voucher_no")
    private String voucherNo;

    @TableField("driver_id")
    private String driverId;

    @TableField("driver_name")
    private String driverName;

    @TableField("vehicle_no")
    private String vehicleNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime assignTime;

    /**
     * 指派类型：1=首次指派, 2=变更指派, 3=取消指派
     */
    @TableField("assign_type")
    private Integer assignType;

    // 指派类型常量
    public static final int TYPE_FIRST_ASSIGN = 1;    // 首次指派
    public static final int TYPE_CHANGE_ASSIGN = 2;    // 变更指派
    public static final int TYPE_CANCEL_ASSIGN = 3;    // 取消指派

    public String getAssignTypeDesc() {
        if (assignType == null) return "未知";
        switch (this.assignType) {
            case TYPE_FIRST_ASSIGN: return "首次指派";
            case TYPE_CHANGE_ASSIGN: return "变更指派";
            case TYPE_CANCEL_ASSIGN: return "取消指派";
            default: return "未知";
        }
    }
}
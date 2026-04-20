package com.fisco.app.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 电子物流委派单实体类
 *
 * 对应数据库表: t_logistics_delegate
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_logistics_delegate")
public class LogisticsDelegate {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("voucher_no")
    private String voucherNo;

    @NotNull(message = "业务场景不能为空，1=直接移库，2=转让后移库，3=发货入库")
    @TableField("business_scene")
    private Integer businessScene;

    /**
     * 关联仓单ID
     * 场景一(直接移库)必填；场景二/三不填
     */
    @TableField("receipt_id")
    private Long receiptId;

    /**
     * 关联背书记录ID
     * 场景二(转让后移库)必填；场景一/三不填
     */
    @TableField("endorse_id")
    private Long endorseId;

    @NotNull(message = "运输数量不能为空")
    @DecimalMin(value = "0", message = "运输数量不能为负数")
    @TableField("transport_quantity")
    private BigDecimal transportQuantity;

    @NotBlank(message = "计量单位不能为空")
    @TableField("unit")
    private String unit;

    /**
     * 货主企业ID（创建时自动设置为当前登录企业）
     */
    @TableField("owner_ent_id")
    private Long ownerEntId;

    @NotNull(message = "承运企业ID不能为空")
    @TableField("carrier_ent_id")
    private Long carrierEntId;

    @TableField("source_wh_id")
    private Long sourceWhId;

    @TableField("target_wh_id")
    private Long targetWhId;

    @TableField("target_receipt_id")
    private Long targetReceiptId;

    @TableField("driver_id")
    private String driverId;

    @TableField("driver_name")
    private String driverName;

    @TableField("vehicle_no")
    private String vehicleNo;

    @TableField("auth_code")
    private String authCode;

    @TableField("pickup_qr_code")
    private String pickupQrCode;

    @TableField("auth_signature")
    private String authSignature;

    @TableField("status")
    private Integer status;

    @TableField("valid_until")
    private LocalDateTime validUntil;

    @TableField("chain_tx_hash")
    private String chainTxHash;

    @TableField("remark")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 业务场景常量 ====================

    public static final int SCENE_DIRECT_TRANSFER = 1;
    public static final int SCENE_TRANSFER_THEN_TRANSFER = 2;
    public static final int SCENE_DELIVERY_TO_WAREHOUSE = 3;

    // ==================== 状态常量 ====================

    public static final int STATUS_PENDING = 1;
    public static final int STATUS_ASSIGNED = 2;
    public static final int STATUS_IN_TRANSIT = 3;
    public static final int STATUS_DELIVERED = 4;
    public static final int STATUS_INVALID = 5;

    // ==================== 到货处理动作常量 ====================

    public static final int ACTION_CREATE_NEW_RECEIPT = 1;
    public static final int ACTION_MERGE_EXISTING_RECEIPT = 2;

    // ==================== 便捷方法 ====================

    public boolean isPending() {
        return STATUS_PENDING == this.status;
    }

    public boolean isInTransit() {
        return STATUS_IN_TRANSIT == this.status;
    }

    public boolean isDelivered() {
        return STATUS_DELIVERED == this.status;
    }

    public boolean isInvalid() {
        return STATUS_INVALID == this.status;
    }

    public boolean isValid() {
        return this.validUntil != null && LocalDateTime.now().isBefore(this.validUntil);
    }

    public String getStatusDesc() {
        if (status == null) return "未知";
        switch (this.status) {
            case STATUS_PENDING: return "待指派";
            case STATUS_ASSIGNED: return "已调度";
            case STATUS_IN_TRANSIT: return "运输中";
            case STATUS_DELIVERED: return "已交付";
            case STATUS_INVALID: return "已失效";
            default: return "未知";
        }
    }

    public String getBusinessSceneDesc() {
        if (businessScene == null) return "未知";
        switch (this.businessScene) {
            case SCENE_DIRECT_TRANSFER: return "直接移库";
            case SCENE_TRANSFER_THEN_TRANSFER: return "转让后移库";
            case SCENE_DELIVERY_TO_WAREHOUSE: return "发货入库";
            default: return "未知";
        }
    }
}

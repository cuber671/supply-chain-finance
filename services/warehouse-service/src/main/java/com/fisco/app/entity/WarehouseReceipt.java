package com.fisco.app.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 电子仓单实体类
 *
 * 对应数据库表: t_warehouse_receipt
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_warehouse_receipt")
public class WarehouseReceipt {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("warehouse_id")
    private Long warehouseId;

    @TableField("on_chain_id")
    private String onChainId;

    @TableField("owner_ent_id")
    private Long ownerEntId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("warehouse_ent_id")
    private Long warehouseEntId;

    @TableField("warehouse_user_id")
    private Long warehouseUserId;

    @TableField("goods_name")
    private String goodsName;

    private BigDecimal weight;

    private String unit;

    @TableField("parent_id")
    private Long parentId;

    @TableField("root_id")
    private Long rootId;

    @TableField("is_locked")
    private Boolean isLocked;

    @TableField("loan_id")
    private String loanId;

    @TableField("stock_order_id")
    private Long stockOrderId;

    private Integer status;

    @TableField("on_chain_status")
    private Integer onChainStatus;

    private Long version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String remark;

    // 状态常量
    public static final int STATUS_IN_STOCK = 1;
    public static final int STATUS_PENDING_TRANSFER = 2;
    public static final int STATUS_SPLIT_MERGED = 3;
    public static final int STATUS_BURNED = 4;
    public static final int STATUS_IN_TRANSIT = 5;
    public static final int STATUS_VOID = 6;  // 已作废
    public static final int STATUS_WAIT_LOGISTICS = 7;  // 待物流（物流委派单创建中，禁止拆分/转让/再次创建物流）

    // 链上状态常量
    public static final int ON_CHAIN_STATUS_PENDING = 0;  // 待上链
    public static final int ON_CHAIN_STATUS_SYNCED = 1;   // 已上链
    public static final int ON_CHAIN_STATUS_FAILED = 2;   // 上链失败
}

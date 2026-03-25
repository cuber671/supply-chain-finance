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
 * 入库单实体类
 *
 * 对应数据库表: t_stock_order
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_stock_order")
public class StockOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("warehouse_id")
    private Long warehouseId;

    @TableField("ent_id")
    private Long entId;

    @TableField("user_id")
    private Long userId;

    @TableField("receipt_id")
    private Long receiptId;

    @TableField("goods_name")
    private String goodsName;

    private BigDecimal weight;

    private String unit;

    @TableField("attachment_url")
    private String attachmentUrl;

    @TableField("stock_no")
    private String stockNo;

    @TableField("data_hash")
    private String dataHash;

    @TableField("chain_tx_hash")
    private String chainTxHash;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 状态常量
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_CONFIRMED = 2;
    public static final int STATUS_CANCELLED = 3;
    public static final int STATUS_COMPLETED = 4; // 已完成出库
}

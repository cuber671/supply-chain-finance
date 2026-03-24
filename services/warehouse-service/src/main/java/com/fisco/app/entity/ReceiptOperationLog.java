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
 * 仓单拆分/合并记录实体类
 *
 * 对应数据库表: t_receipt_operation_log
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
@TableName("t_receipt_operation_log")
public class ReceiptOperationLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Integer opType;

    @TableField("source_receipt_ids")
    private String sourceReceiptIds;

    @TableField("target_receipt_ids")
    private String targetReceiptIds;

    @TableField("total_weight")
    private BigDecimal totalWeight;

    @TableField("apply_ent_id")
    private Long applyEntId;

    @TableField("apply_user_id")
    private Long applyUserId;

    @TableField("execute_ent_id")
    private Long executeEntId;

    @TableField("execute_user_id")
    private Long executeUserId;

    @TableField("tx_hash")
    private String txHash;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("finish_time")
    private LocalDateTime finishTime;

    // 操作类型常量
    public static final int OP_TYPE_SPLIT = 1;
    public static final int OP_TYPE_MERGE = 2;

    // 状态常量
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_REJECTED = 3;
}

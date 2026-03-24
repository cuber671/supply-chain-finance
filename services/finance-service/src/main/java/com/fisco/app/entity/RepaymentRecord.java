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
 * 还款记录实体类
 *
 * 对应数据库表: t_repayment_record
 */
@Data
@TableName("t_repayment_record")
public class RepaymentRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("receivable_id")
    private Long receivableId;

    @TableField("repayment_no")
    private String repaymentNo;

    @TableField("repayment_type")
    private Integer repaymentType;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("currency")
    private String currency;

    @TableField("payment_voucher")
    private String paymentVoucher;

    @TableField("receipt_id")
    private Long receiptId;

    @TableField("offset_price")
    private BigDecimal offsetPrice;

    @TableField("signature_hash")
    private String signatureHash;

    @TableField("repayment_time")
    private LocalDateTime repaymentTime;

    @TableField("chain_tx_hash")
    private String chainTxHash;

    @TableField("remark")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

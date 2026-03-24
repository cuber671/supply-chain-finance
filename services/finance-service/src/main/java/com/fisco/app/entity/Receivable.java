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
 * 电子应收款项实体类
 *
 * 对应数据库表: t_receivable
 *
 * 账款状态流转：
 * 1-待确认 -> 2-生效中 -> 3-部分还款 -> 4-已结清
 * 或 2-生效中 -> 5-逾期
 */
@Data
@TableName("t_receivable")
public class Receivable {

    // ==================== 状态常量 ====================

    /** 待确认 */
    public static final int STATUS_PENDING = 1;

    /** 生效中 */
    public static final int STATUS_ACTIVE = 2;

    /** 部分还款 */
    public static final int STATUS_PARTIAL_REPAYMENT = 3;

    /** 已结清 */
    public static final int STATUS_SETTLED = 4;

    /** 逾期 */
    public static final int STATUS_OVERDUE = 5;

    // ==================== 业务场景常量 ====================

    /** 入库生成 */
    public static final int SCENE_STOCK_IN = 1;

    /** 转让配送签收生成 */
    public static final int SCENE_TRANSFER_ACCEPT = 2;

    /**
     * 账款记录唯一主键ID - 雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账单编号
     */
    @TableField("receivable_no")
    private String receivableNo;

    /**
     * 来源场景：1-入库生成；2-转让配送签收生成
     */
    @TableField("business_scene")
    private Integer businessScene;

    /**
     * 关联物流单ID
     */
    @TableField("source_voucher_id")
    private Long sourceVoucherId;

    /**
     * 债权人ID
     */
    @TableField("creditor_ent_id")
    private Long creditorEntId;

    /**
     * 债务人ID
     */
    @TableField("debtor_ent_id")
    private Long debtorEntId;

    /**
     * 原始金额
     */
    @TableField("initial_amount")
    private BigDecimal initialAmount;

    /**
     * 结算金额
     */
    @TableField("adjusted_amount")
    private BigDecimal adjustedAmount;

    /**
     * 已回收金额
     */
    @TableField("collected_amount")
    private BigDecimal collectedAmount;

    /**
     * 待还余额
     */
    @TableField("balance_unpaid")
    private BigDecimal balanceUnpaid;

    /**
     * 币种
     */
    @TableField("currency")
    private String currency;

    /**
     * 最后还款日
     */
    @TableField("due_date")
    private LocalDateTime dueDate;

    /**
     * 账款状态：1-待确认；2-生效中；3-部分还款；4-已结清；5-逾期
     */
    @TableField("status")
    private Integer status;

    /**
     * 融资标识
     */
    @TableField("is_financed")
    private Integer isFinanced;

    /**
     * 父级ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 区块链交易哈希
     */
    @TableField("chain_tx_hash")
    private String chainTxHash;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最后修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

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
 * 贷款还款记录实体类
 *
 * 对应数据库表: t_loan_repayment
 *
 * 支持以下还款方式：
 * 1. CASH - 现金还款
 * 2. RECEIPT_OFFSET - 仓单抵债
 * 3. RECEIVABLE_OFFSET - 应收账款抵债
 * 4. COLLATERAL_DISPOSAL - 质押物处置
 * 5. PARTIAL - 部分还款
 */
@Data
@TableName("t_loan_repayment")
public class LoanRepayment {

    // ==================== 还款类型常量 ====================

    /** 现金还款 */
    public static final String TYPE_CASH = "CASH";

    /** 仓单抵债 */
    public static final String TYPE_RECEIPT_OFFSET = "RECEIPT_OFFSET";

    /** 应收账款抵债 */
    public static final String TYPE_RECEIVABLE_OFFSET = "RECEIVABLE_OFFSET";

    /** 质押物处置 */
    public static final String TYPE_COLLATERAL_DISPOSAL = "COLLATERAL_DISPOSAL";

    /** 部分还款 */
    public static final String TYPE_PARTIAL = "PARTIAL";

    // ==================== 状态常量 ====================

    /** 待确认 */
    public static final int STATUS_PENDING = 1;

    /** 已确认 */
    public static final int STATUS_CONFIRMED = 2;

    /** 已拒绝 */
    public static final int STATUS_REJECTED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("loan_id")
    private Long loanId;

    @TableField("repayment_no")
    private String repaymentNo;

    // ==================== 还款金额 ====================

    @TableField("principal_amount")
    private BigDecimal principalAmount;

    @TableField("interest_amount")
    private BigDecimal interestAmount;

    @TableField("penalty_amount")
    private BigDecimal penaltyAmount;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    // ==================== 还款方式 ====================

    @TableField("repayment_type")
    private String repaymentType;

    // ==================== 现金还款信息 ====================

    @TableField("payment_voucher")
    private String paymentVoucher;

    @TableField("payment_account")
    private String paymentAccount;

    // ==================== 仓单抵债信息 ====================

    @TableField("offset_receipt_id")
    private Long offsetReceiptId;

    @TableField("offset_receipt_no")
    private String offsetReceiptNo;

    // ==================== 应收账款抵债信息 ====================

    @TableField("offset_receivable_id")
    private Long offsetReceivableId;

    @TableField("offset_receivable_no")
    private String offsetReceivableNo;

    // ==================== 处置信息 ====================

    @TableField("disposal_method")
    private String disposalMethod;

    @TableField("disposal_amount")
    private BigDecimal disposalAmount;

    // ==================== 签名与时间 ====================

    @TableField("signature_hash")
    private String signatureHash;

    @TableField("repayment_time")
    private LocalDateTime repaymentTime;

    // ==================== 状态 ====================

    @TableField("status")
    private Integer status;

    // ==================== 区块链 ====================

    @TableField("chain_tx_hash")
    private String chainTxHash;

    // ==================== 审计字段 ====================

    @TableField("remark")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 辅助方法 ====================

    public String getRepaymentTypeName() {
        if (repaymentType == null) return "未知";
        switch (repaymentType) {
            case TYPE_CASH: return "现金还款";
            case TYPE_RECEIPT_OFFSET: return "仓单抵债";
            case TYPE_RECEIVABLE_OFFSET: return "应收账款抵债";
            case TYPE_COLLATERAL_DISPOSAL: return "质押物处置";
            case TYPE_PARTIAL: return "部分还款";
            default: return "未知";
        }
    }

    public String getStatusName() {
        if (status == null) return "未知";
        switch (status) {
            case STATUS_PENDING: return "待确认";
            case STATUS_CONFIRMED: return "已确认";
            case STATUS_REJECTED: return "已拒绝";
            default: return "未知";
        }
    }
}

package com.fisco.app.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 质押贷款实体类
 *
 * 对应数据库表: t_loan
 *
 * 贷款状态流转：
 * 1-待审批 -> 4-待放款 -> 5-已放款 -> 6-还款中 -> 7-已结清
 *                  -> 2-已拒绝
 *                  -> 3-已取消
 * 5-已放款 -> 8-逾期 -> 9-已违约
 */
@Data
@TableName("t_loan")
public class Loan {

    // ==================== 状态常量 ====================

    /** 待审批 */
    public static final int STATUS_PENDING = 1;

    /** 已拒绝 */
    public static final int STATUS_REJECTED = 2;

    /** 已取消 */
    public static final int STATUS_CANCELLED = 3;

    /** 待放款 */
    public static final int STATUS_PENDING_DISBURSE = 4;

    /** 已放款 */
    public static final int STATUS_DISBURSED = 5;

    /** 还款中 */
    public static final int STATUS_REPAYING = 6;

    /** 已结清 */
    public static final int STATUS_SETTLED = 7;

    /** 逾期 */
    public static final int STATUS_OVERDUE = 8;

    /** 已违约 */
    public static final int STATUS_DEFAULTED = 9;

    // ==================== 业务字段 ====================

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("loan_no")
    private String loanNo;

    @TableField("borrower_ent_id")
    private Long borrowerEntId;

    @TableField("borrower_ent_name")
    private String borrowerEntName;

    @TableField("finance_ent_id")
    private Long financeEntId;

    @TableField("finance_ent_name")
    private String financeEntName;

    @TableField("receipt_id")
    private Long receiptId;

    @TableField("receipt_no")
    private String receiptNo;

    @TableField("goods_name")
    private String goodsName;

    @TableField("warehouse_name")
    private String warehouseName;

    // ==================== 贷款金额信息 ====================

    @TableField("applied_amount")
    private BigDecimal appliedAmount;

    @TableField("approved_amount")
    private BigDecimal approvedAmount;

    @TableField("loan_amount")
    private BigDecimal loanAmount;

    // ==================== 利率与期限 ====================

    @TableField("applied_interest_rate")
    private BigDecimal appliedInterestRate;

    @TableField("approved_interest_rate")
    private BigDecimal approvedInterestRate;

    @TableField("loan_interest_rate")
    private BigDecimal loanInterestRate;

    @TableField("loan_days")
    private Integer loanDays;

    // ==================== 质押物评估 ====================

    @TableField("collateral_value")
    private BigDecimal collateralValue;

    @TableField("pledge_rate")
    private BigDecimal pledgeRate;

    @TableField("max_loan_amount")
    private BigDecimal maxLoanAmount;

    // ==================== 时间信息 ====================

    @TableField("applied_time")
    private LocalDateTime appliedTime;

    @TableField("approved_time")
    private LocalDateTime approvedTime;

    @TableField("disbursed_time")
    private LocalDateTime disbursedTime;

    @TableField("loan_start_date")
    private LocalDate loanStartDate;

    @TableField("loan_end_date")
    private LocalDate loanEndDate;

    // ==================== 还款信息 ====================

    @TableField("repaid_principal")
    private BigDecimal repaidPrincipal;

    @TableField("repaid_interest")
    private BigDecimal repaidInterest;

    @TableField("repaid_penalty")
    private BigDecimal repaidPenalty;

    @TableField("outstanding_principal")
    private BigDecimal outstandingPrincipal;

    @TableField("outstanding_interest")
    private BigDecimal outstandingInterest;

    @TableField("outstanding_penalty")
    private BigDecimal outstandingPenalty;

    // ==================== 状态与关联 ====================

    @TableField("status")
    private Integer status;

    @TableField("receivable_id")
    private Long receivableId;

    // ==================== 审批/拒绝信息 ====================

    @TableField("approve_remark")
    private String approveRemark;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("reject_time")
    private LocalDateTime rejectTime;

    @TableField("cancel_reason")
    private String cancelReason;

    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    // ==================== 区块链 ====================

    @TableField("chain_tx_hash")
    private String chainTxHash;

    /**
     * 放款凭证
     */
    @TableField("disbursement_voucher")
    private String disbursementVoucher;

    // ==================== 审计字段 ====================

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 辅助方法 ====================

    public String getStatusName() {
        if (status == null) return "未知";
        switch (status) {
            case STATUS_PENDING: return "待审批";
            case STATUS_REJECTED: return "已拒绝";
            case STATUS_CANCELLED: return "已取消";
            case STATUS_PENDING_DISBURSE: return "待放款";
            case STATUS_DISBURSED: return "已放款";
            case STATUS_REPAYING: return "还款中";
            case STATUS_SETTLED: return "已结清";
            case STATUS_OVERDUE: return "逾期";
            case STATUS_DEFAULTED: return "已违约";
            default: return "未知";
        }
    }

    public BigDecimal getTotalRepayable() {
        BigDecimal principal = loanAmount != null ? loanAmount : BigDecimal.ZERO;
        BigDecimal interest = outstandingInterest != null ? outstandingInterest : BigDecimal.ZERO;
        return principal.add(interest);
    }

    public BigDecimal getTotalRepaid() {
        BigDecimal principal = repaidPrincipal != null ? repaidPrincipal : BigDecimal.ZERO;
        BigDecimal interest = repaidInterest != null ? repaidInterest : BigDecimal.ZERO;
        BigDecimal penalty = repaidPenalty != null ? repaidPenalty : BigDecimal.ZERO;
        return principal.add(interest).add(penalty);
    }
}

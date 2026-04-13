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
 * 贷款分期计划实体类
 *
 * 对应数据库表: t_loan_installment
 */
@Data
@TableName("t_loan_installment")
public class LoanInstallment {

    // ==================== 状态常量 ====================

    /** 待还 */
    public static final int STATUS_PENDING = 1;

    /** 已还 */
    public static final int STATUS_PAID = 2;

    /** 逾期 */
    public static final int STATUS_OVERDUE = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("loan_id")
    private Long loanId;

    @TableField("installment_no")
    private Integer installmentNo;

    @TableField("due_date")
    private LocalDate dueDate;

    @TableField("principal_amount")
    private BigDecimal principalAmount;

    @TableField("interest_amount")
    private BigDecimal interestAmount;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("repaid_amount")
    private BigDecimal repaidAmount;

    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public String getStatusName() {
        if (status == null) return "未知";
        switch (status) {
            case STATUS_PENDING: return "待还";
            case STATUS_PAID: return "已还";
            case STATUS_OVERDUE: return "逾期";
            default: return "未知";
        }
    }

    public BigDecimal getOutstandingAmount() {
        BigDecimal total = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        BigDecimal repaid = repaidAmount != null ? repaidAmount : BigDecimal.ZERO;
        return total.subtract(repaid);
    }

    public boolean isOverdue() {
        if (status != null && status == STATUS_PAID) {
            return false;
        }
        return dueDate != null && LocalDate.now().isAfter(dueDate);
    }
}

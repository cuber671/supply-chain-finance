package com.fisco.app.dto;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

/**
 * 质押贷款申请请求
 */
@Data
public class LoanApplyRequest {

    @NotNull(message = "仓单ID不能为空")
    private Long receiptId;

    @NotNull(message = "申请金额不能为空")
    @Positive(message = "申请金额必须大于0")
    private BigDecimal appliedAmount;

    @NotNull(message = "贷款期限不能为空")
    @Positive(message = "贷款期限必须大于0")
    private Integer loanDays;

    private BigDecimal appliedInterestRate;

    private String contactPerson;

    private String remark;
}

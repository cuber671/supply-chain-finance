package com.fisco.app.dto;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

/**
 * 质押贷款审批请求
 */
@Data
public class LoanApproveRequest {

    @NotNull(message = "审批金额不能为空")
    @Positive(message = "审批金额必须大于0")
    private BigDecimal approvedAmount;

    @NotNull(message = "利率不能为空")
    @Positive(message = "利率必须大于0")
    private BigDecimal interestRate;

    @NotNull(message = "贷款期限不能为空")
    @Positive(message = "贷款期限必须大于0")
    private Integer loanDays;

    private String remark;
}

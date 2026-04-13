package com.fisco.app.dto;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 质押贷款审批请求
 */
@Data
@Schema(description = "质押贷款审批请求")
public class LoanApproveRequest {

    @NotNull(message = "审批金额不能为空")
    @Positive(message = "审批金额必须大于0")
    @Schema(description = "审批金额", example = "95000.00")
    private BigDecimal approvedAmount;

    @NotNull(message = "利率不能为空")
    @Positive(message = "利率必须大于0")
    @Schema(description = "年利率（%）", example = "5.0")
    private BigDecimal interestRate;

    @NotNull(message = "贷款期限不能为空")
    @Positive(message = "贷款期限必须大于0")
    @Schema(description = "贷款期限（天）", example = "30")
    private Integer loanDays;

    @Schema(description = "审批备注（可选）", example = "资料齐全，同意放款")
    private String remark;
}

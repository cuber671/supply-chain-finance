package com.fisco.app.dto;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 质押贷款申请请求
 */
@Data
@Schema(description = "质押贷款申请请求")
public class LoanApplyRequest {

    @NotNull(message = "仓单ID不能为空")
    @Schema(description = "仓单ID", example = "1")
    private Long receiptId;

    @NotNull(message = "申请金额不能为空")
    @Positive(message = "申请金额必须大于0")
    @Schema(description = "申请金额", example = "100000.00")
    private BigDecimal appliedAmount;

    @NotNull(message = "贷款期限不能为空")
    @Positive(message = "贷款期限必须大于0")
    @Schema(description = "贷款期限（天）", example = "30")
    private Integer loanDays;

    @Schema(description = "申请利率（可选）", example = "5.5")
    private BigDecimal appliedInterestRate;

    @Schema(description = "联系人（可选）", example = "张三")
    private String contactPerson;

    @Schema(description = "备注（可选）", example = "用于采购原材料")
    private String remark;
}
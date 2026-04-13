package com.fisco.app.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 质押贷款试算请求
 */
@Data
@Schema(description = "贷款试算请求")
public class LoanCalculatorRequest {

    @Schema(description = "仓单ID（可选）", example = "1")
    private Long receiptId;

    @Schema(description = "贷款金额（可选）", example = "100000.00")
    private BigDecimal loanAmount;

    @Schema(description = "贷款期限（天，可选）", example = "30")
    private Integer loanDays;

    @Schema(description = "企业ID（可选）", example = "1")
    private Long enterpriseId;

    @Schema(description = "质押物价值（可选）", example = "150000.00")
    private BigDecimal collateralValue;

    @Schema(description = "质押率（可选，单位：%）", example = "70")
    private BigDecimal pledgeRate;

    @Data
    @Schema(description = "贷款试算结果")
    public static class LoanCalculatorResult {

        @Schema(description = "仓单信息", example = "See ReceiptInfo")
        private ReceiptInfo receiptInfo;

        @Schema(description = "贷款试算信息", example = "See LoanInfo")
        private LoanInfo loanCalculator;

        @Schema(description = "质押率（%）", example = "70")
        private BigDecimal pledgeRate;

        @Schema(description = "最大可贷金额", example = "105000.00")
        private BigDecimal maxLoanAmount;

        @Schema(description = "企业信用信息", example = "See CreditInfo")
        private CreditInfo creditInfo;
    }

    @Data
    @Schema(description = "仓单信息")
    public static class ReceiptInfo {
        @Schema(description = "仓单ID", example = "1")
        private Long receiptId;
        @Schema(description = "仓单编号", example = "RCP1234567890")
        private String receiptNo;
        @Schema(description = "货物名称", example = "钢材")
        private String goodsName;
        @Schema(description = "货物重量", example = "100.5")
        private BigDecimal weight;
        @Schema(description = "计量单位", example = "吨")
        private String unit;
        @Schema(description = "质押物价值", example = "150000.00")
        private BigDecimal collateralValue;
    }

    @Data
    @Schema(description = "贷款试算信息")
    public static class LoanInfo {
        @Schema(description = "贷款金额", example = "100000.00")
        private BigDecimal loanAmount;
        @Schema(description = "贷款期限（天）", example = "30")
        private Integer loanDays;
        @Schema(description = "年利率（%）", example = "5.0")
        private BigDecimal interestRate;
        @Schema(description = "预估利息", example = "416.67")
        private BigDecimal estimatedInterest;
        @Schema(description = "罚息利率（%）", example = "0.05")
        private BigDecimal penaltyRate;
        @Schema(description = "每日罚息", example = "50.00")
        private BigDecimal overduePenaltyPerDay;
        @Schema(description = "到期总还款额", example = "100416.67")
        private BigDecimal totalRepayable;
    }

    @Data
    @Schema(description = "企业信用信息")
    public static class CreditInfo {
        @Schema(description = "企业ID", example = "1")
        private Long enterpriseId;
        @Schema(description = "信用等级", example = "AA")
        private String creditLevel;
        @Schema(description = "建议利率（%）", example = "5.0")
        private BigDecimal suggestedRate;
    }
}

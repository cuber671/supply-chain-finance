package com.fisco.app.dto;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 质押贷款试算请求
 */
@Data
public class LoanCalculatorRequest {

    private Long receiptId;

    private BigDecimal loanAmount;

    private Integer loanDays;

    private Long enterpriseId;

    private BigDecimal collateralValue;

    private BigDecimal pledgeRate;

    @Data
    public static class LoanCalculatorResult {
        private ReceiptInfo receiptInfo;
        private LoanInfo loanCalculator;
        private BigDecimal pledgeRate;
        private BigDecimal maxLoanAmount;
        private CreditInfo creditInfo;

        @Data
        public static class ReceiptInfo {
            private Long receiptId;
            private String receiptNo;
            private String goodsName;
            private BigDecimal weight;
            private String unit;
            private BigDecimal collateralValue;
        }

        @Data
        public static class LoanInfo {
            private BigDecimal loanAmount;
            private Integer loanDays;
            private BigDecimal interestRate;
            private BigDecimal estimatedInterest;
            private BigDecimal penaltyRate;
            private BigDecimal overduePenaltyPerDay;
            private BigDecimal totalRepayable;
        }

        @Data
        public static class CreditInfo {
            private Long enterpriseId;
            private String creditLevel;
            private BigDecimal suggestedRate;
        }
    }
}

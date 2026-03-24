package com.fisco.app.dto;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * 质押贷款还款请求
 */
@Data
public class RepaymentRequest {

    @NotNull(message = "还款类型不能为空")
    private String repaymentType;

    @NotNull(message = "本金金额不能为空")
    private BigDecimal principalAmount;

    @NotNull(message = "利息金额不能为空")
    private BigDecimal interestAmount;

    private BigDecimal penaltyAmount;

    private String paymentVoucher;

    private String paymentAccount;

    private Long offsetReceiptId;

    private BigDecimal offsetPrice;

    private Long offsetReceivableId;

    private String signatureHash;

    private String remark;
}

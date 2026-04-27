package com.fisco.app.dto;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 质押贷款还款请求
 */
@Schema(description = "质押贷款还款请求")
@Data
public class RepaymentRequest {

    @NotNull(message = "还款类型不能为空")
    @Schema(description = "还款类型", example = "NORMAL", allowableValues = {"NORMAL", "EARLY", "OVERDUE"})
    private String repaymentType;

    @NotNull(message = "本金金额不能为空")
    @Schema(description = "本金金额", example = "10000.00")
    private BigDecimal principalAmount;

    @NotNull(message = "利息金额不能为空")
    @Schema(description = "利息金额", example = "500.00")
    private BigDecimal interestAmount;

    @Schema(description = "罚息金额", example = "0.00")
    private BigDecimal penaltyAmount;

    @Schema(description = "支付凭证（图片URL或Base64）", example = "https://example.com/voucher.jpg")
    private String paymentVoucher;

    @Schema(description = "支付账户", example = "6217********1234")
    private String paymentAccount;

    @Schema(description = "偏移仓单ID（部分还款时使用）", example = "12345")
    private Long offsetReceiptId;

    @Schema(description = "偏移价格（部分还款时使用）", example = "5000.00")
    private BigDecimal offsetPrice;

    @Schema(description = "偏移应收账款ID（部分还款时使用）", example = "12345")
    private Long offsetReceivableId;

    @Schema(description = "签名哈希", example = "0xabc123...")
    private String signatureHash;

    @Schema(description = "备注", example = "提前还款")
    private String remark;
}
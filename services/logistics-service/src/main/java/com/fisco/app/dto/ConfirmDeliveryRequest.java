package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 确认交付请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "确认交付请求")
public class ConfirmDeliveryRequest {

    @Parameter(description = "委派单编号", required = true, example = "DPDO2026041703FBCD46")
    @NotBlank(message = "委派单编号不能为空")
    private String voucherNo;

    @Parameter(description = "动作类型：1-全量交付，2-部分交付（需指定目标仓单ID）", example = "1")
    private Integer action;

    @Parameter(description = "目标仓单ID（部分交付时必填）", example = "2042829397927989200")
    private String targetReceiptId;
}

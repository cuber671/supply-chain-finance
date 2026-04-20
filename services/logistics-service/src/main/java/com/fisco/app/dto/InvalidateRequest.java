package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 使委派单失效请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "使委派单失效请求")
public class InvalidateRequest {

    @Parameter(description = "委派单编号", required = true, example = "DPDO2026041703FBCD46")
    @NotBlank(message = "委派单编号不能为空")
    private String voucherNo;
}

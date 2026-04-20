package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 仓库提货确认请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仓库提货确认请求")
public class ConfirmPickupRequest {

    @Parameter(description = "委派单编号", required = true, example = "DPDO2026041703FBCD46")
    @NotBlank(message = "委派单编号不能为空")
    private String voucherNo;

    @Parameter(description = "提货授权码", required = true, example = "QY56WH7E")
    @NotBlank(message = "提货授权码不能为空")
    private String authCode;

    @Parameter(description = "司机当前位置纬度", example = "22.543102")
    private Double driverLatitude;

    @Parameter(description = "司机当前位置经度", example = "114.057865")
    private Double driverLongitude;
}

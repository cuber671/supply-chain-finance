package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 物流指派请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "物流指派请求")
public class AssignDriverRequest {

    @Parameter(description = "委派单编号", required = true, example = "DPDO2026041703FBCD46")
    @NotBlank(message = "委派单编号不能为空")
    private String voucherNo;

    @Parameter(description = "司机ID", required = true, example = "D001")
    @NotBlank(message = "司机ID不能为空")
    private String driverId;

    @Parameter(description = "司机姓名", required = true, example = "张三")
    @NotBlank(message = "司机姓名不能为空")
    private String driverName;

    @Parameter(description = "车牌号", required = true, example = "粤B12345")
    @NotBlank(message = "车牌号不能为空")
    private String vehicleNo;
}

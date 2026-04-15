package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema(description = "审核注销申请请求")
public class AuditCancellationRequestDTO {

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果，true=通过，false=拒绝", example = "true")
    private Boolean approved;
}

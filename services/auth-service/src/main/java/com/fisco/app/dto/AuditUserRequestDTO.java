package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 审核用户请求 DTO
 */
@Data
@Schema(description = "审核用户请求")
public class AuditUserRequestDTO {

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果，true=通过，false=拒绝", example = "true")
    private Boolean approved;

    @Schema(description = "拒绝原因（审核拒绝时填写）", example = "企业信息与邀请码不匹配")
    private String rejectReason;
}

package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "申请注销请求")
public class CancelApplyRequestDTO {

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码（验证身份）", example = "********")
    private String password;

    @Schema(description = "注销原因", example = "不再使用该账号")
    private String reason;
}

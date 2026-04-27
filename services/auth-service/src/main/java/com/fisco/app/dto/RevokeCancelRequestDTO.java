package com.fisco.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "撤回注销申请请求")
public class RevokeCancelRequestDTO {

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码（验证身份）", example = "********")
    private String password;
}